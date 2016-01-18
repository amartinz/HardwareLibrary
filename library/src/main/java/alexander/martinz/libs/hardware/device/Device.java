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
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

import alexander.martinz.libs.execution.RootCheck;
import alexander.martinz.libs.execution.binaries.BusyBox;
import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.hardware.utils.IoUtils;
import alexander.martinz.libs.hardware.utils.Utils;

public class Device {
    public final String platformVersion;
    public final String platformId;
    public final String platformType;
    public final String platformTags;
    public final String platformBuildType;

    public final String vmLibrary;
    public final String vmVersion;

    public final int screenWidth;
    public final int screenHeight;

    public final String androidId;
    public final String manufacturer;
    public final String model;
    public final String device;
    public final String product;
    public final String board;
    public final String bootloader;
    public final String radio;

    public boolean hasBusyBox;
    public boolean hasRoot;
    public String suVersion;
    public boolean isSELinuxEnforcing;

    private static Device sInstance;
    protected Context mContext;

    public interface EmmcInfoListener {
        void onEmmcInfoAvailable(@NonNull EmmcInfo emmcInfo);
    }

    public interface KernelInfoListener {
        void onKernelInfoAvailable(@NonNull KernelInfo kernelInfo);
    }

    public interface MemoryInfoListener {
        void onMemoryInfoAvailable(@NonNull MemoryInfo memoryInfo);
    }

    public interface ProcessorInfoListener {
        void onProcessorInfoAvailable(@NonNull ProcessorInfo processorInfo);
    }

    protected Device(@NonNull Context context) {
        mContext = context;

        platformVersion = Build.VERSION.RELEASE;
        platformId = Build.DISPLAY;
        platformType = Build.VERSION.CODENAME + " " + Build.TYPE;
        platformTags = Build.TAGS;
        platformBuildType = Utils.getDate(Build.TIME);

        vmVersion = System.getProperty("java.vm.version", "-");
        vmLibrary = getRuntime();

        final Resources res = context.getResources();
        screenWidth = res.getDisplayMetrics().widthPixels;
        screenHeight = res.getDisplayMetrics().heightPixels;

        androidId = getAndroidId(context);
        manufacturer = Build.MANUFACTURER;
        model = Build.MODEL;
        device = Build.DEVICE;
        product = Build.PRODUCT;
        board = Build.BOARD;
        bootloader = Build.BOOTLOADER;
        radio = Build.getRadioVersion();

        // initialize defaults
        hasBusyBox = false;
        hasRoot = false;
        suVersion = "-";
        isSELinuxEnforcing = isSELinuxEnforcing(); // ehm, alright, if you say so...
    }

    public static Device get(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new Device(context);
        }
        return sInstance;
    }

    public Device update() {
        hasRoot = RootCheck.isRooted();
        suVersion = RootCheck.getSuVersion();

        // check busybox
        hasBusyBox = BusyBox.isAvailable();

        // selinux can be toggled when in development mode, so do not cache it
        isSELinuxEnforcing = isSELinuxEnforcing(); // ehm, alright, if you say so...

        return this;
    }

    @NonNull private String getRuntime() {
        String tmp = (vmVersion.startsWith("1") ? "libdvm.so" : "libart.so");
        final boolean isDalvik = "libdvm.so".equals(tmp);
        final boolean isArt = "libart.so".equals(tmp);

        final String runtime;
        if (isArt) {
            runtime = "ART";
        } else if (isDalvik) {
            runtime = "Dalvik";
        } else {
            runtime = Constants.UNAVAILABLE;
        }
        tmp = String.format("%s (%s)", runtime, tmp);

        return tmp;
    }

    /**
     * @return A string, formatted as - manufacturer model (device) <br/>
     * Example: OPPO Find7 (find7)
     */
    public String getModelString() {
        return String.format("%s %s (%s)", manufacturer, model, device);
    }

    /**
     * @return A string, formatted as - manufacturer model <br/>
     * Example: OPPO Find7
     */
    public String getModelStringShort() {
        return String.format("%s %s", manufacturer, model);
    }

    private boolean isSELinuxEnforcing() {
        // We know about a 4.2 release, which has enforcing selinux
        if (Build.VERSION.SDK_INT >= 17) {
            final int enforcingState = IoUtils.readSysfsIntValue("/sys/fs/selinux/enforce");

            // 4.4+ builds (should) be enforcing by default
            if (enforcingState == Constants.INVALID) {
                isSELinuxEnforcing = (Build.VERSION.SDK_INT >= 19);
            } else {
                isSELinuxEnforcing = Utils.isEnabled(Integer.toString(enforcingState), false);
            }
        }

        return isSELinuxEnforcing;
    }

    public static String getAndroidId(@NonNull Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
