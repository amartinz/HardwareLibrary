/*
 *  Copyright (C) 2013 - 2014 Alexander "Evisceration" Martinz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package alexander.martinz.libs.hardware.io;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import alexander.martinz.libs.execution.Command;
import alexander.martinz.libs.execution.RootShell;
import alexander.martinz.libs.execution.ShellManager;
import alexander.martinz.libs.hardware.utils.IoUtils;

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
    @Nullable public static String[] getAvailableIoSchedulers() {
        String[] schedulers = null;
        final String[] aux = IoUtils.readStringArray(IO_SCHEDULER_PATH[0]);
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

    public static void getIoScheduler(final IoSchedulerListener listener) {
        getIoScheduler(listener, new Handler(Looper.getMainLooper()));
    }

    public static void getIoScheduler(final IoSchedulerListener listener, final Handler handler) {
        final String content = IoUtils.readFile(IO_SCHEDULER_PATH[0]);
        if (!TextUtils.isEmpty(content)) {
            processIoSchedulerContent(content, listener, handler);
            return;
        }

        final RootShell rootShell = ShellManager.get().getRootShell();
        if (rootShell != null) {
            final StringBuilder outputCollector = new StringBuilder();
            final Command command = new Command(String.format("cat %s", IO_SCHEDULER_PATH[0])) {
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

    private static void processIoSchedulerContent(@NonNull String content, final IoSchedulerListener listener, Handler handler) {
        final List<String> result = Arrays.asList(content.split(" "));
        final List<String> tmpList = new ArrayList<>();
        String tmpString = "";

        if (result.size() <= 0) {
            return;
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

        final String scheduler = tmpString;
        final String[] availableSchedulers = tmpList.toArray(new String[tmpList.size()]);
        handler.post(new Runnable() {
            @Override public void run() {
                listener.onIoScheduler(new IoScheduler(availableSchedulers, scheduler));
            }
        });
    }
}
