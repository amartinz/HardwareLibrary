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

package at.amartinz.hardware.device;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import at.amartinz.hardware.Constants;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.execution.Command;
import at.amartinz.execution.RootCheck;

/**
 * A class which parses /proc/cpuinfo and prepares information ready for usage
 */
public class ProcessorInfo {
    private static final String TAG = ProcessorInfo.class.getSimpleName();

    private static final String PATH_PROC_CPU = "/proc/cpuinfo";
    public String processor;
    public String bogomips;
    public String features;
    public String hardware;

    public final boolean is64Bit;
    public final String supportedAbis;

    private ProcessorInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            is64Bit = Build.SUPPORTED_64_BIT_ABIS.length != 0;
            String abis = "";
            int length = Build.SUPPORTED_ABIS.length;
            for (int i = 0; i < length; i++) {
                abis += Build.SUPPORTED_ABIS[i];
                if (i + 1 != length) {
                    abis += ", ";
                }
            }
            supportedAbis = abis;
        } else {
            is64Bit = false;
            supportedAbis = String.format("%s, %s", Build.CPU_ABI, Build.CPU_ABI2);
        }
    }

    public static void feedWithInformation(final Device.ProcessorInfoListener processorInfoListener) {
        AsyncTask.execute(new Runnable() {
            @Override public void run() {
                feedWithInformationBlocking(processorInfoListener);
            }
        });
    }

    @WorkerThread public static void feedWithInformationBlocking(final Device.ProcessorInfoListener procInfoListener) {
        final String content = HwIoUtils.readFile(PATH_PROC_CPU);
        if (!TextUtils.isEmpty(content)) {
            feedWithInformation(content, procInfoListener);
            return;
        }

        // If we could not read the file and we do not have root, then we can not read it...
        if (!RootCheck.isRooted()) {
            return;
        }

        final Command cmd = HwIoUtils.readFileRoot(PATH_PROC_CPU, new HwIoUtils.ReadFileListener() {
            @Override public void onFileRead(String path, String content) {
                feedWithInformation(content, procInfoListener);
            }
        });
        if (cmd == null) {
            if (Constants.DEBUG) {
                Log.e(TAG, "Could not read file with root!");
            }
        }
    }

    private static void feedWithInformation(String content, Device.ProcessorInfoListener listener) {
        if (TextUtils.isEmpty(content)) {
            return;
        }

        final ProcessorInfo processorInfo = new ProcessorInfo();
        final String[] list = content.split("\n");

        for (final String s : list) {
            if (s.contains("Processor")) {
                processorInfo.processor = getData(s);
            } else if (processorInfo.bogomips == null && s.contains("BogoMIPS")) {
                processorInfo.bogomips = getData(s);
            } else if (s.contains("Features")) {
                processorInfo.features = getData(s);
            } else if (s.contains("Hardware")) {
                processorInfo.hardware = getData(s);
            }
        }

        if (listener != null) {
            listener.onProcessorInfoAvailable(processorInfo);
        }
    }

    @NonNull private static String getData(final String data) {
        if (data == null) {
            return "";
        }

        final String[] splitted = data.split(":");
        if (splitted.length < 2) {
            return "";
        }

        return (splitted[1] != null ? splitted[1].trim() : "");
    }

    public List<String> abisAsList() {
        final ArrayList<String> list = new ArrayList<>();
        final String[] abis = supportedAbis.split(",");
        for (final String abi : abis) {
            list.add(abi.trim());
        }
        return list;
    }

    public static boolean is64BitStatic() {
        return new ProcessorInfo().is64Bit;
    }

}
