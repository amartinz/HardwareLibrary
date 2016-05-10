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

package at.amartinz.hardware.gpu;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

import at.amartinz.hardware.Constants;
import alexander.martinz.libs.hardware.R;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.hardware.utils.HwUtils;
import at.amartinz.execution.Command;
import at.amartinz.execution.RootCheck;

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
                    gpuInformation.freqAvailable = availableFreqs;
                } else if (TextUtils.equals(getFreqCurPath(context), path)) {
                    gpuInformation.freqCur = HwUtils.tryParseInt(content);
                }
            }
        };
    }


}
