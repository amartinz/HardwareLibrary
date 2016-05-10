/*
 * The MIT License
 *
 * Copyright (c) 2016 Alexander Martinz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package at.amartinz.hardware.cpu;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import at.amartinz.execution.Command;
import at.amartinz.execution.Shell;
import at.amartinz.execution.ShellManager;

public class CpuCoreMonitor {
    private static CpuCoreMonitor cpuFrequencyMonitor;

    private final Handler handler;
    private final int cpuCount;
    private final List<CpuCore> coreList = new ArrayList<>();

    private Shell shell;

    private CoreListener listener;
    private int interval;

    private boolean isStarted = false;

    public interface CoreListener {
        void onCores(@NonNull final List<CpuCore> cores);
    }

    private CpuCoreMonitor(final Handler handler) {
        this.handler = handler;
        this.cpuCount = CpuReader.readAvailableCores();
        getShell();

        for (int i = 0; i < cpuCount; i++) {
            coreList.add(new CpuCore(i, "0", "0", "0"));
        }
    }

    public static CpuCoreMonitor getInstance(@NonNull final Handler handler) {
        if (cpuFrequencyMonitor == null) {
            cpuFrequencyMonitor = new CpuCoreMonitor(handler);
        }
        return cpuFrequencyMonitor;
    }

    public CpuCoreMonitor start(final CoreListener listener) {
        return start(listener, 2000);
    }

    public CpuCoreMonitor start(final CoreListener listener, final int interval) {
        this.listener = listener;
        this.interval = interval;
        if (!isStarted) {
            isStarted = true;
            handler.post(mUpdater);
        }

        return cpuFrequencyMonitor;
    }

    public CpuCoreMonitor stop() {
        if (isStarted) {
            isStarted = false;
            listener = null;
            handler.removeCallbacks(mUpdater);
        }
        return cpuFrequencyMonitor;
    }

    public void destroy() {
        stop();
        cpuFrequencyMonitor = null;
    }

    private final Runnable mUpdater = new Runnable() {
        @Override public void run() {
            updateStates();
        }
    };

    private Shell getShell() {
        if (shell == null || shell.isClosed() || shell.shouldClose()) {
            final boolean shouldUseRoot = shouldUseRoot();
            if (shouldUseRoot) {
                shell = ShellManager.get().getRootShell();
            } else {
                shell = ShellManager.get().getNormalShell();
            }
        }
        return shell;
    }

    private boolean shouldUseRoot() {
        for (int i = 0; i < cpuCount; i++) {
            final String[] paths = new String[]{
                    CpuReader.getPathCoreFreqCur(i),
                    CpuReader.getPathCoreFreqMax(i),
                    CpuReader.getPathCoreFreqMin(i),
                    CpuReader.getPathCoreGov(i)
            };
            for (final String path : paths) {
                if (!(new File(path).canRead())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateStates() {
        if (getShell() == null) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cpuCount; i++) {
            // if cpufreq directory exists ...
            sb.append("if [ -d \"/sys/devices/system/cpu/cpu").append(String.valueOf(i)).append("/cpufreq\" ]; then\n");
            // cat /path/to/cpu/frequency
            sb.append(String.format("(cat \"%s\") 2> /dev/null;\n", CpuReader.getPathCoreFreqCur(i)));
            sb.append("echo -n \" \";");
            // cat /path/to/cpu/frequency_max
            sb.append(String.format("(cat \"%s\") 2> /dev/null;\n", CpuReader.getPathCoreFreqMax(i)));
            sb.append("echo -n \" \";");
            // cat /path/to/cpu/governor
            sb.append(String.format("(cat \"%s\") 2> /dev/null;\n", CpuReader.getPathCoreGov(i)));
            // ... else echo 0 for them
            sb.append("else echo \"0 0 0\";fi;");
            // ... and append a space on the end
            sb.append("echo -n \" \";");
        }

        // example output: 162000 1890000 interactive
        final String cmd = sb.toString();
        final Command command = new Command(cmd) {
            @Override public void onCommandCompleted(int id, int exitCode) {
                super.onCommandCompleted(id, exitCode);

                String output = getOutput();
                if (output == null) {
                    return;
                }
                output = output.replace("\n", " ");

                final String[] parts = output.split(" ");
                int mult = 0;
                for (int i = 0; i < cpuCount; i++) {
                    CpuCore cpuCore;
                    try {
                        cpuCore = coreList.get(i);
                    } catch (IndexOutOfBoundsException iobe) {
                        cpuCore = new CpuCore(i, "0", "0", "0");
                    }
                    try {
                        cpuCore.setCurrent(parts[i + mult])
                                .setMax(parts[i + mult + 1])
                                .setGovernor(parts[i + mult + 2]);
                    } catch (IndexOutOfBoundsException iob) {
                        cpuCore.setCurrent("0").setMax("0").setGovernor("0");
                    }
                    mult += 2;
                }

                if (listener != null) {
                    handler.post(new Runnable() {
                        @Override public void run() {
                            if (listener != null) {
                                listener.onCores(coreList);
                            }
                        }
                    });
                }

                handler.removeCallbacks(mUpdater);
                handler.postDelayed(mUpdater, interval);
            }
        };
        command.setOutputType(Command.OUTPUT_STRING);
        getShell().add(command);
    }

}
