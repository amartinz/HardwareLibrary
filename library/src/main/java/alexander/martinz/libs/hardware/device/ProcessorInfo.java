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

package alexander.martinz.libs.hardware.device;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import alexander.martinz.libs.execution.Command;
import alexander.martinz.libs.hardware.utils.IoUtils;
import alexander.martinz.libs.logger.Logger;

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

    @Override public String toString() {
        return new Gson().toJson(this, ProcessorInfo.class);
    }

    public static void feedWithInformation(final Device.ProcessorInfoListener processorInfoListener) {
        AsyncTask.execute(new Runnable() {
            @Override public void run() {
                feedWithInformationBlocking(processorInfoListener);
            }
        });
    }

    @WorkerThread public static void feedWithInformationBlocking(final Device.ProcessorInfoListener procInfoListener) {
        final String content = IoUtils.readFile(PATH_PROC_CPU);
        if (!TextUtils.isEmpty(content)) {
            feedWithInformation(content, procInfoListener);
            return;
        }

        // If we could not read the file and we do not have root, then we can not read it...
        if (!RootCheck.isRooted()) {
            return;
        }

        final Command cmd = IoUtils.readFileRoot(PATH_PROC_CPU, new IoUtils.ReadFileListener() {
            @Override public void onFileRead(String path, String content) {
                feedWithInformation(content, procInfoListener);
            }
        });
        if (cmd == null) {
            Logger.e(TAG, "Could not read file with root!");
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
