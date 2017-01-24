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
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import at.amartinz.hardware.Constants;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.hardware.utils.HwUtils;

/**
 * Provides information about the device's memory
 */
public class MemoryInfo {
    public static final int TYPE_B = 0;
    public static final int TYPE_KB = 1;
    public static final int TYPE_MB = 2;

    private static final String MEMTOTAL = "MemTotal:";
    private static final String MEMCACHED = "Cached:";
    private static final String MEMFREE = "MemFree:";

    public int type;
    public long total;
    public long cached;
    public long free; // TODO: rework as android calculates free memory differently

    public MemoryInfo() { }

    public static void feedWithInformation(final int type, final Device.MemoryInfoListener listener) {
        AsyncTask.execute(new Runnable() {
            @Override public void run() {
                feedWithInformationBlocking(type, listener);
            }
        });
    }

    @WorkerThread public static void feedWithInformationBlocking(int type, Device.MemoryInfoListener listener) {
        if (listener != null) {
            listener.onMemoryInfoAvailable(readMemory(type));
        }
    }

    private static MemoryInfo readMemory(final int type) {
        final MemoryInfo memoryInfo = new MemoryInfo();
        memoryInfo.type = type;

        final String input = HwIoUtils.INSTANCE.readFile("/proc/meminfo");
        if (!TextUtils.isEmpty(input)) {
            final String[] parts = input.split("\n");
            for (final String s : parts) {
                memoryInfo.checkMemory(s);
            }
        } else {
            memoryInfo.total = 0;
            memoryInfo.free = 0;
            memoryInfo.cached = 0;
        }

        // Ensure we don't get garbage
        if (memoryInfo.total < 0) {
            memoryInfo.total = 0;
        }
        if (memoryInfo.free < 0) {
            memoryInfo.free = 0;
        }
        if (memoryInfo.cached < 0) {
            memoryInfo.cached = 0;
        }

        // default is kb
        switch (type) {
            default:
            case TYPE_KB:
                break;
            case TYPE_B:
                memoryInfo.total *= 1024;
                memoryInfo.free *= 1024;
                memoryInfo.cached *= 1024;
                break;
            case TYPE_MB:
                memoryInfo.total /= 1024;
                memoryInfo.free /= 1024;
                memoryInfo.cached /= 1024;
                break;
        }

        return memoryInfo;
    }

    public static String getAsMb(final long data) {
        return String.format("%s MB", data);
    }

    private long checkMemory(String content) {
        if (!TextUtils.isEmpty(content)) {
            content = content.replace("kB", "");

            if (content.startsWith(MEMTOTAL)) {
                content = content.replace(MEMTOTAL, "").trim();
                total = HwUtils.INSTANCE.tryParseLong(content);
                return total;
            } else if (content.startsWith(MEMCACHED)) {
                content = content.replace(MEMCACHED, "").trim();
                cached = HwUtils.INSTANCE.tryParseLong(content);
                return cached;
            } else if (content.startsWith(MEMFREE)) {
                content = content.replace(MEMFREE, "").trim();
                free = HwUtils.INSTANCE.tryParseLong(content);
                return free;
            }
        }

        return Constants.INSTANCE.getINVALID();
    }

}
