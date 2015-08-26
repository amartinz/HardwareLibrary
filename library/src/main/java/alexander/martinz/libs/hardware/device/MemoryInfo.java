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
import android.text.TextUtils;

import com.google.gson.Gson;

import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.hardware.utils.IoUtils;
import alexander.martinz.libs.hardware.utils.Utils;

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

    @Override public String toString() {
        return new Gson().toJson(this, MemoryInfo.class);
    }

    public static void feedWithInformation(final Context context, final int type, final Device.MemoryInfoListener listener) {
        final Thread feedThread = new Thread(new Runnable() {
            @Override public void run() {
                feedWithInformationBlocking(context, type, listener);
            }
        });
        feedThread.start();
    }

    public static void feedWithInformationBlocking(Context context, int type, Device.MemoryInfoListener listener) {
        if (listener != null) {
            listener.onMemoryInfoAvailable(readMemory(type));
        }
    }

    private static MemoryInfo readMemory(final int type) {
        final MemoryInfo memoryInfo = new MemoryInfo();
        memoryInfo.type = type;

        final String input = IoUtils.readFile("/proc/meminfo");
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
                total = Utils.tryParseLong(content);
                return total;
            } else if (content.startsWith(MEMCACHED)) {
                content = content.replace(MEMCACHED, "").trim();
                cached = Utils.tryParseLong(content);
                return cached;
            } else if (content.startsWith(MEMFREE)) {
                content = content.replace(MEMFREE, "").trim();
                free = Utils.tryParseLong(content);
                return free;
            }
        }

        return Constants.INVALID;
    }

}
