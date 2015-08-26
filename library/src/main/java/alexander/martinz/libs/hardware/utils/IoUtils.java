package alexander.martinz.libs.hardware.utils;

import android.content.Context;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

import alexander.martinz.libs.execution.Command;
import alexander.martinz.libs.execution.RootShell;
import alexander.martinz.libs.execution.ShellManager;
import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.hardware.device.Device;
import alexander.martinz.libs.logger.Logger;

public class IoUtils {
    private static final String TAG = IoUtils.class.getSimpleName();

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
            if (IoUtils.fileExists(path)) {
                basePath = path;
                break;
            }
        }
        if (TextUtils.isEmpty(basePath)) {
            return "";
        }
        return basePath;
    }

    public static boolean fileExists(@Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) && new File(filePath.trim()).exists();
    }

    public static boolean fileExists(@Nullable File file) {
        return file != null && file.exists();
    }

    public static int readSysfsIntValue(final String path) {
        final String rawString = IoUtils.readFile(path);
        if (!TextUtils.isEmpty(rawString)) {
            return Utils.tryParseInt(rawString);
        }
        return Constants.INVALID;
    }

    @Nullable public static String readFile(final String path) {
        final String content = readFileInternal(path, false);
        return ((content != null) ? content.trim() : null);
    }

    @Nullable public static String readOneLine(final String path) {
        final String content = readFileInternal(path, true);
        return ((content != null) ? content.trim() : null);
    }

    @Nullable public static Command readFileRoot(@NonNull final Context context, @Nullable final String path,
            @Nullable final ReadFileListener readFileListener) {
        if (TextUtils.isEmpty(path) || readFileListener == null || !Device.isRooted()) {
            return null;
        }

        final Command cmd = new Command(String.format("cat %s", path)) {
            private final StringBuilder sb = new StringBuilder();

            @Override public void onCommandCompleted(int id, int exitCode) {
                readFileListener.onFileRead(path, sb.toString());
                super.onCommandCompleted(id, exitCode);
            }

            @Override public void onCommandTerminated(int id, String reason) {
                Logger.e(this, "File reading terminated -> %s", reason);
                super.onCommandTerminated(id, reason);
            }

            @Override public void onCommandOutput(int id, String line) {
                sb.append(line).append('\n');
                super.onCommandOutput(id, line);
            }
        };

        final RootShell rootShell = ShellManager.get(context).getRootShell();
        if (rootShell != null) {
            rootShell.add(cmd);
            return cmd;
        }
        return null;
    }

    @Nullable private static String readFileInternal(final String path, final boolean oneLine) {
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
                Logger.e(TAG, "Could not read file -> %s", path);
                if (Logger.getEnabled()) {
                    ioe.printStackTrace();
                }
            } finally {
                closeQuietly(bufferedReader);
                closeQuietly(fileReader);
            }
        } else {
            Logger.w(TAG, "Can not read file -> %s", path);
        }
        return null;
    }

    public static boolean writeToFile(@NonNull Context context, @NonNull String path, @NonNull String content) {
        return writeToFile(context, path, content, true);
    }

    public static boolean writeToFile(@NonNull Context context, @NonNull String path, @NonNull String content,
            boolean useRootAsFallback) {
        return writeToFile(context, new File(path), content, useRootAsFallback);
    }

    public static boolean writeToFile(@NonNull Context context, @NonNull File file, @NonNull String content) {
        return writeToFile(context, file, content, true);
    }

    public static boolean writeToFile(@NonNull Context context, @NonNull File file, @NonNull String content,
            boolean useRootAsFallback) {
        final boolean useRoot = useRootAsFallback && (!file.canWrite() && Device.isRooted());
        if (useRoot) {
            final RootShell rootShell = ShellManager.get(context).getRootShell();
            if (rootShell == null) {
                return false;
            }

            final int id = sRandom.nextInt(1000);
            final String cmd = String.format("echo \'%s\' > %s", content, file.getAbsolutePath());
            final Command writeCommand = new Command(id, cmd);
            rootShell.add(writeCommand);

            final int exitCode = writeCommand.waitFor().getExitCode();
            Logger.v(TAG, "write command \"%s\" ended with exit code -> %s", id, exitCode);
            return (exitCode == 0);
        } else {
            FileWriter fw = null;
            try {
                fw = new FileWriter(file);
                fw.write(content);
            } catch (IOException ioe) {
                Logger.e(TAG, "could not write to file %s", file.getAbsolutePath());
                if (Logger.getEnabled()) {
                    ioe.printStackTrace();
                }
                return false;
            } finally {
                closeQuietly(fw);
            }
        }
        return true;
    }
}
