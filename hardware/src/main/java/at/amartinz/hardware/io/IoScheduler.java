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
package at.amartinz.hardware.io;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.execution.Command;
import at.amartinz.execution.RootShell;
import at.amartinz.execution.ShellManager;

public class IoScheduler {

    public static final String[] IO_SCHEDULER_PATH = {
            "/sys/block/mmcblk0/queue/scheduler",
            "/sys/block/mmcblk1/queue/scheduler"
    };
    public static final String[] READ_AHEAD_PATH = {
            "/sys/block/mmcblk0/queue/read_ahead_kb",
            "/sys/block/mmcblk1/queue/read_ahead_kb"
    };

    public interface IoSchedulerListener {
        void onIoScheduler(final IoScheduler ioScheduler);
    }

    public final String[] available;
    public final String current;

    public IoScheduler(final String[] availableIoSchedulers, final String ioScheduler) {
        available = availableIoSchedulers;
        current = ioScheduler;
    }

    /**
     * Gets available schedulers from file
     *
     * @return available schedulers
     */
    @WorkerThread @Nullable public static String[] getAvailableIoSchedulers() {
        String[] schedulers = null;
        final String[] aux = HwIoUtils.INSTANCE.readStringArray(IO_SCHEDULER_PATH[0]);
        if (aux != null) {
            schedulers = new String[aux.length];
            for (int i = 0; i < aux.length; i++) {
                if (aux[i].charAt(0) == '[') {
                    schedulers[i] = aux[i].substring(1, aux[i].length() - 1);
                } else {
                    schedulers[i] = aux[i];
                }
            }
        }
        return schedulers;
    }

    @WorkerThread @Nullable public static IoScheduler getIoSchedulerBlocking() {
        final String content = RootShell.Companion.fireAndBlockString(String.format("cat %s", IO_SCHEDULER_PATH[0]));
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        return processIoSchedulerContent(content, null, null);
    }

    public static void getIoScheduler(final IoSchedulerListener listener) {
        getIoScheduler(listener, new Handler(Looper.getMainLooper()));
    }

    public static void getIoScheduler(final IoSchedulerListener listener, final Handler handler) {
        final String content = HwIoUtils.INSTANCE.readFile(IO_SCHEDULER_PATH[0]);
        if (!TextUtils.isEmpty(content)) {
            processIoSchedulerContent(content, listener, handler);
            return;
        }

        final RootShell rootShell = ShellManager.Companion.get().getRootShell();
        if (rootShell != null) {
            final StringBuilder outputCollector = new StringBuilder();
            final Command command = new Command(new String[]{String.format("cat %s", IO_SCHEDULER_PATH[0])}, 0, 5000) {
                @Override public void onCommandOutput(int id, String line) {
                    outputCollector.append(line);
                    super.onCommandOutput(id, line);
                }

                @Override public void onCommandCompleted(int id, int exitCode) {
                    processIoSchedulerContent(outputCollector.toString(), listener, handler);
                    super.onCommandCompleted(id, exitCode);
                }
            };
            rootShell.add(command);
        }
    }

    @Nullable private static IoScheduler processIoSchedulerContent(@NonNull String content,
            @Nullable final IoSchedulerListener listener, @Nullable Handler handler) {
        final List<String> result = Arrays.asList(content.split(" "));
        final List<String> tmpList = new ArrayList<>();
        String tmpString = "";

        if (result.size() <= 0) {
            return null;
        }

        for (final String s : result) {
            if (TextUtils.isEmpty(s)) {
                continue;
            }
            if (s.charAt(0) == '[') {
                tmpString = s.substring(1, s.length() - 1);
                tmpList.add(tmpString);
            } else {
                tmpList.add(s);
            }
        }

        final IoScheduler ioScheduler = new IoScheduler(tmpList.toArray(new String[tmpList.size()]), tmpString);
        if (handler != null && listener != null) {
            handler.post(new Runnable() {
                @Override public void run() {
                    listener.onIoScheduler(ioScheduler);
                }
            });
        }
        return ioScheduler;
    }
}
