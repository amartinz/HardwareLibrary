/*
 * Copyright (C) 2013 - 2015 Alexander Martinz
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
 */

package alexander.martinz.libs.hardware.utils;

import android.content.Context;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import alexander.martinz.libs.hardware.Constants;
import at.amartinz.execution.Command;
import at.amartinz.execution.RootCheck;
import at.amartinz.execution.RootShell;
import at.amartinz.execution.ShellManager;
import hugo.weaving.DebugLog;

public class HwIoUtils {
    private static final String TAG = HwIoUtils.class.getSimpleName();

    private static final Random sRandom = new Random(System.nanoTime());

    public static void closeQuietly(final Object o) {
        if (o instanceof Socket) {
            try {
                ((Socket) o).close();
            } catch (Exception ignored) { }
        } else if (o instanceof Closeable) {
            try {
                ((Closeable) o).close();
            } catch (Exception ignored) { }
        }
    }

    public interface ReadFileListener {
        void onFileRead(String path, String content);
    }

    public static String getPath(@NonNull Context context, @ArrayRes int filePathResId) {
        return getPath(context, filePathResId, null);
    }

    public static String getPath(@NonNull Context context, @ArrayRes int filePathResId, @Nullable String prefix) {
        final boolean hasPrefix = !TextUtils.isEmpty(prefix);
        final String[] paths = context.getResources().getStringArray(filePathResId);

        String basePath = null;
        for (String path : paths) {
            if (hasPrefix) {
                path = prefix + path;
            }
            if (HwIoUtils.fileExists(path)) {
                basePath = path;
                break;
            }
        }
        if (TextUtils.isEmpty(basePath)) {
            return "";
        }
        return basePath;
    }

    public static boolean canExecute(@Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) && new File(filePath).canExecute();
    }

    public static boolean canRead(@Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) && new File(filePath).canRead();
    }

    public static boolean canWrite(@Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) && new File(filePath).canWrite();
    }

    public static boolean fileExists(@Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) && new File(filePath.trim()).exists();
    }

    public static boolean fileExists(@Nullable File file) {
        return file != null && file.exists();
    }

    public static boolean fileExists(final String[] files) {
        for (final String s : files) {
            if (fileExists(s)) {
                return true;
            }
        }
        return false;
    }

    @Nullable public static String checkPaths(final String[] paths) {
        for (final String s : paths) {
            if (fileExists(s)) {
                return s;
            }
        }
        return null;
    }

    @Nullable public static String checkPath(final String path) {
        if (fileExists(path)) {
            return path;
        }
        return null;
    }

    @NonNull public static List<String> listFiles(@NonNull String pathToDirectory) {
        return listFiles(new File(pathToDirectory));
    }

    @NonNull public static List<String> listFiles(@NonNull File directory) {
        return listFiles(directory, false);
    }

    @WorkerThread @DebugLog @NonNull public static List<String> listFiles(@NonNull File directory, boolean withRootFallback) {
        final ArrayList<String> files = new ArrayList<>();
        final File[] listedFiles = directory.listFiles();
        if (listedFiles != null) {
            for (final File file : listedFiles) {
                files.add(file.getName());
            }
        } else if (withRootFallback) {
            final RootShell rootShell = ShellManager.get().getRootShell();
            if (rootShell == null) {
                if (Constants.DEBUG) {
                    Log.w(TAG, "could not obtain root shell");
                }
            } else {
                final Command command = new Command(String.format("ls %s", directory.getAbsolutePath())) {
                    @Override public void onCommandOutput(int id, String line) {
                        super.onCommandOutput(id, line);
                        if (line != null && !(line = line.trim()).isEmpty()) {
                            files.add(line.trim());
                        }
                    }
                };
                rootShell.add(command);
                command.waitFor();
            }
        } else if (Constants.DEBUG) {
            Log.w(TAG, "could not list files");
        }
        return files;
    }

    @WorkerThread public static int readSysfsIntValue(final String path) {
        final String rawString = HwIoUtils.readFile(path);
        if (!TextUtils.isEmpty(rawString)) {
            return HwUtils.tryParseInt(rawString);
        }
        return Constants.INVALID;
    }

    @WorkerThread public static String readSysfsStringValue(final String path) {
        final String rawString = HwIoUtils.readFile(path);
        if (!TextUtils.isEmpty(rawString)) {
            return rawString.trim();
        }
        return Constants.INVALID_STR;
    }

    @WorkerThread @Nullable public static String[] readStringArray(final String path) {
        final String line = readOneLine(path);
        if (line != null) {
            return line.split(" ");
        }
        return null;
    }

    @WorkerThread @Nullable public static String readFile(final String path) {
        final String content = readFileInternal(path, false);
        return ((content != null) ? content.trim() : null);
    }

    @WorkerThread @Nullable public static String readOneLine(final String path) {
        final String content = readFileInternal(path, true);
        return ((content != null) ? content.trim() : null);
    }

    @WorkerThread @Nullable private static String readFileInternal(final String path, final boolean oneLine) {
        final File f = new File(path);
        if (f.canRead()) {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                fileReader = new FileReader(f);
                bufferedReader = new BufferedReader(fileReader);
                if (oneLine) {
                    return bufferedReader.readLine();
                }
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append('\n');
                }
                return stringBuilder.toString();
            } catch (IOException ioe) {
                if (Constants.DEBUG) {
                    Log.e(TAG, String.format("Could not read file -> %s", path), ioe);
                }
            } finally {
                closeQuietly(bufferedReader);
                closeQuietly(fileReader);
            }
        } else if (Constants.DEBUG) {
            Log.w(TAG, String.format("Can not read file, because it is not readable -> %s", path));
        }
        return null;
    }

    @WorkerThread
    @Nullable public static Command readFileRoot(@Nullable final String path, @Nullable final ReadFileListener readFileListener) {
        if (TextUtils.isEmpty(path) || readFileListener == null || !RootCheck.isRooted()) {
            return null;
        }

        final Command cmd = new Command(String.format("cat %s", path)) {
            private final StringBuilder sb = new StringBuilder();

            @Override public void onCommandCompleted(int id, int exitCode) {
                readFileListener.onFileRead(path, sb.toString());
                super.onCommandCompleted(id, exitCode);
            }

            @Override public void onCommandOutput(int id, String line) {
                sb.append(line).append('\n');
                super.onCommandOutput(id, line);
            }
        };

        final RootShell rootShell = ShellManager.get().getRootShell();
        if (rootShell != null) {
            rootShell.add(cmd);
            return cmd;
        }
        return null;
    }

    @WorkerThread public static boolean writeToFile(@NonNull String path, @NonNull String content) {
        return writeToFile(path, content, true);
    }

    @WorkerThread public static boolean writeToFile(@NonNull String path, @NonNull String content, boolean useRootAsFallback) {
        return writeToFile(new File(path), content, useRootAsFallback);
    }

    @WorkerThread public static boolean writeToFile(@NonNull File file, @NonNull String content) {
        return writeToFile(file, content, true);
    }

    @WorkerThread public static boolean writeToFile(@NonNull File file, @NonNull String content, boolean useRootAsFallback) {
        final boolean useRoot = useRootAsFallback && (!file.canWrite() && RootCheck.isRooted());
        if (useRoot) {
            if (Constants.DEBUG) {
                Log.v(TAG, String.format("writing to %s as root", file.getAbsolutePath()));
            }
            final RootShell rootShell = ShellManager.get().getRootShell();
            if (rootShell == null) {
                if (Constants.DEBUG) {
                    Log.w(TAG, "could not obtain root shell!");
                }
                return false;
            }

            final int id = sRandom.nextInt(10000);
            final String cmd = String.format("echo \'%s\' > %s", content, file.getAbsolutePath());
            final Command writeCommand = new Command(id, cmd);
            rootShell.add(writeCommand);

            final int exitCode = writeCommand.waitFor().getExitCode();
            if (Constants.DEBUG) {
                Log.v(TAG, String.format("write command \"%s\" ended with exit code -> %s", id, exitCode));
            }
            return (exitCode == 0);
        } else {
            if (Constants.DEBUG) {
                Log.v(TAG, String.format("writing to %s", file.getAbsolutePath()));
            }
            FileWriter fw = null;
            try {
                fw = new FileWriter(file);
                fw.write(content);
            } catch (IOException ioe) {
                if (Constants.DEBUG) {
                    Log.e(TAG, String.format("could not write to file %s", file.getAbsolutePath()));
                    Log.e(TAG, String.format("exists: %s | can read: %s | can write: %s",
                            file.exists(), file.canRead(), file.canWrite()), ioe);
                }
                return false;
            } finally {
                closeQuietly(fw);
            }
        }
        return true;
    }
}
