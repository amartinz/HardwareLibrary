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

package alexander.martinz.libs.hardware.gpu;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

import alexander.martinz.libs.execution.Command;
import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.hardware.R;
import alexander.martinz.libs.execution.RootCheck;
import alexander.martinz.libs.hardware.utils.HwIoUtils;
import alexander.martinz.libs.hardware.utils.HwUtils;

public class GpuReader {
    private static final String TAG = GpuReader.class.getSimpleName();

    private static String basePath;
    private static String freqAvailPath;
    private static String freqCurPath;

    private GpuReader() { }

    public static void getGpuInformation(Context context, GpuInformationListener listener) {
        AsyncTask.execute(new ReadGpuInformationRunnable(context, listener));
    }

    @WorkerThread public static GpuInformation getGpuInformationBlocking(Context context) {
        final GpuInformation gpuInformation = new GpuInformation();

        gpuInformation.freqAvailable = readAvailableFrequencies(context);
        if (gpuInformation.freqAvailable.isEmpty()) {
            gpuInformation.freqMax = Constants.INVALID;
            gpuInformation.freqMin = Constants.INVALID;
        } else {
            gpuInformation.freqMax = gpuInformation.freqAvailable.get(gpuInformation.freqAvailable.size() - 1);
            gpuInformation.freqMin = gpuInformation.freqAvailable.get(0);
        }

        gpuInformation.freqCur = HwIoUtils.readSysfsIntValue(getFreqCurPath(context));

        return gpuInformation;
    }

    @Nullable public static String getBasePath(Context context) {
        if (basePath == null) {
            basePath = HwIoUtils.getPath(context, R.array.hardware_gpu_base);
        }
        return basePath;
    }

    @Nullable public static String getFreqAvailPath(Context context) {
        if (freqAvailPath == null) {
            freqAvailPath = HwIoUtils.getPath(context, R.array.hardware_gpu_freqs_avail, getBasePath(context));
        }
        return freqAvailPath;
    }

    @Nullable public static String getFreqCurPath(Context context) {
        if (freqCurPath == null) {
            freqCurPath = HwIoUtils.getPath(context, R.array.hardware_gpu_freqs_cur, getBasePath(context));
        }
        return freqCurPath;
    }

    @NonNull private static ArrayList<Integer> readAvailableFrequencies(Context context) {
        final String freqAvailPath = getFreqAvailPath(context);
        return readAvailableFrequencies(HwIoUtils.readFile(freqAvailPath));
    }

    @NonNull private static ArrayList<Integer> readAvailableFrequencies(final String freqString) {
        final ArrayList<Integer> availableFreqs = new ArrayList<>();
        if (!TextUtils.isEmpty(freqString)) {
            final String[] splitted = freqString.split(" ");
            for (final String s : splitted) {
                availableFreqs.add(HwUtils.tryParseInt(s));
            }
        }
        if (!availableFreqs.isEmpty()) {
            Collections.sort(availableFreqs);
        }
        return availableFreqs;
    }

    private static class ReadGpuInformationRunnable implements Runnable {
        private final Context context;
        private final GpuInformationListener listener;

        private GpuInformation gpuInformation;
        private boolean hasFinished;

        public ReadGpuInformationRunnable(Context context, GpuInformationListener listener) {
            super();
            this.context = context;
            this.listener = listener;
        }

        @Override public void run() {
            gpuInformation = getGpuInformationBlocking(context);
            // if the gpu information contains an invalid value AND we are not using root, finish
            if (gpuInformation.isValid() || !RootCheck.isRooted()) {
                hasFinished = true;
            } else {
                gpuInformation.resetInvalid();
            }

            while (!hasFinished) {
                if (gpuInformation.freqCur == Constants.NOT_INITIALIZED) {
                    Command cmd = HwIoUtils.readFileRoot(getFreqCurPath(context), readFileListener);
                    if (cmd == null) {
                        if (Constants.DEBUG) {
                            Log.e(TAG, "Could not read file with root!");
                        }
                        break;
                    } else {
                        gpuInformation.freqCur = Constants.INITIALIZATION_STARTED;
                    }
                }
                if ((gpuInformation.freqMax == Constants.NOT_INITIALIZED) ||
                    (gpuInformation.freqMin == Constants.NOT_INITIALIZED)) {
                    Command cmd = HwIoUtils.readFileRoot(getFreqAvailPath(context), readFileListener);
                    if (cmd == null) {
                        if (Constants.DEBUG) {
                            Log.e(TAG, "Could not read file with root!");
                        }
                        break;
                    } else {
                        gpuInformation.freqMin = Constants.INITIALIZATION_STARTED;
                        gpuInformation.freqMax = Constants.INITIALIZATION_STARTED;
                    }
                }

                // if we have read all values, we are done
                hasFinished = !gpuInformation.isInitializing();
            }

            if (listener != null) {
                listener.onGpuInformation(gpuInformation);
            }
        }

        private final HwIoUtils.ReadFileListener readFileListener = new HwIoUtils.ReadFileListener() {
            @Override public void onFileRead(String path, String content) {
                if (TextUtils.isEmpty(path)) {
                    return;
                }
                if (TextUtils.equals(getFreqAvailPath(context), path)) {
                    final ArrayList<Integer> availableFreqs = readAvailableFrequencies(content);
                    if (availableFreqs.isEmpty()) {
                        gpuInformation.freqMax = Constants.INVALID;
                        gpuInformation.freqMin = Constants.INVALID;
                    } else {
                        gpuInformation.freqMax = availableFreqs.get(availableFreqs.size() - 1);
                        gpuInformation.freqMin = availableFreqs.get(0);
                    }
                } else if (TextUtils.equals(getFreqCurPath(context), path)) {
                    gpuInformation.freqCur = HwUtils.tryParseInt(content);
                }
            }
        };
    }


}
