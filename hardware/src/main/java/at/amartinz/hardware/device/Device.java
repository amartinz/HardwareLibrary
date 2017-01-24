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

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import at.amartinz.hardware.Constants;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.hardware.utils.HwUtils;
import at.amartinz.execution.BusyBox;
import at.amartinz.execution.RootCheck;

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
        platformBuildType = HwUtils.INSTANCE.getDate(Build.TIME);

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
        hasRoot = RootCheck.INSTANCE.isRooted();
        suVersion = RootCheck.INSTANCE.getSuVersion(false);

        // check busybox
        hasBusyBox = BusyBox.INSTANCE.isAvailable(mContext);

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
            runtime = Constants.INSTANCE.getUNAVAILABLE();
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
            final int enforcingState = HwIoUtils.INSTANCE.readSysfsIntValue("/sys/fs/selinux/enforce");

            // 4.4+ builds (should) be enforcing by default
            if (enforcingState == Constants.INSTANCE.getINVALID()) {
                isSELinuxEnforcing = (Build.VERSION.SDK_INT >= 19);
            } else {
                isSELinuxEnforcing = HwUtils.INSTANCE.isEnabled(Integer.toString(enforcingState), false);
            }
        }

        return isSELinuxEnforcing;
    }

    public static String getAndroidId(@NonNull Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override public String toString() {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("platform_version", platformVersion);
            jsonObject.put("platform_id", platformId);
            jsonObject.put("platform_type", platformType);
            jsonObject.put("platform_tags", platformTags);
            jsonObject.put("platform_buildtype", platformBuildType);

            jsonObject.put("vm_library", vmLibrary);
            jsonObject.put("vm_version", vmVersion);

            jsonObject.put("screen_width", screenWidth);
            jsonObject.put("screen_height", screenHeight);

            jsonObject.put("device_android_id", androidId);
            jsonObject.put("device_manufacturer", manufacturer);
            jsonObject.put("device_model", model);
            jsonObject.put("device_device", device);
            jsonObject.put("device_product", product);
            jsonObject.put("device_board", board);
            jsonObject.put("device_bootloader", bootloader);
            jsonObject.put("device_radio", radio);

            jsonObject.put("has_busybox", hasBusyBox);
            jsonObject.put("has_root", hasRoot);
            jsonObject.put("su_version", suVersion);
            jsonObject.put("is_selinux_enforcing", isSELinuxEnforcing);
            return jsonObject.toString();
        } catch (Exception ignored) { }

        // something bad happened
        return super.toString();
    }
}
