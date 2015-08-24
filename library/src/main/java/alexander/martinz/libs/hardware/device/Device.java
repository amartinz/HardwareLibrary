/*
 *  Copyright (C) 2014 Alexander "Evisceration" Martinz
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
 *
 */
package alexander.martinz.libs.hardware.device;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;

import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.hardware.utils.IoUtils;
import alexander.martinz.libs.hardware.utils.Utils;

public class Device {
    @SerializedName("platform_version") public final String platformVersion;
    @SerializedName("platform_id") public final String platformId;
    @SerializedName("platform_type") public final String platformType;
    @SerializedName("platform_tags") public final String platformTags;
    @SerializedName("platform_build_date") public final String platformBuildType;

    @SerializedName("vm_library") public final String vmLibrary;
    @SerializedName("vm_version") public final String vmVersion;

    @SerializedName("screen_width") public final int screenWidth;
    @SerializedName("screen_height") public final int screenHeight;

    @SerializedName("android_id") public final String androidId;
    @SerializedName("manufacturer") public final String manufacturer;
    @SerializedName("model") public final String model;
    @SerializedName("device") public final String device;
    @SerializedName("product") public final String product;
    @SerializedName("board") public final String board;
    @SerializedName("bootloader") public final String bootloader;
    @SerializedName("radio_version") public final String radio;

    @SerializedName("has_busybox") public boolean hasBusyBox;
    @SerializedName("has_root") public boolean hasRoot;
    @SerializedName("su_version") public String suVersion;
    @SerializedName("selinux_enforcing") public boolean isSELinuxEnforcing;

    private static Device sInstance;
    private Context mContext;

    public interface EmmcInfoListener {
        void onEmmcInfoAvailable(@NonNull EmmcInfo emmcInfo);
    }

    public interface KernelInfoListener {
        void onKernelInfoAvailable(@NonNull KernelInfo kernelInfo);
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

    public static boolean isRooted() {
        return new File("/system/bin/su").exists()
               || new File("/system/xbin/su").exists()
               || new File("/system/bin/.ext/.su").exists()
               || new File("/system/xbin/sugote").exists();
    }

    public Device update() {
        hasRoot = isRooted();

        // get su version
        //suVersion = hasRoot ? Utils.getCommandResult("su -v", "-") : "-";

        // check busybox
        //hasBusyBox = RootTools.isBusyboxAvailable();

        // selinux can be toggled when in development mode, so do not cache it
        isSELinuxEnforcing = isSELinuxEnforcing(); // ehm, alright, if you say so...

        return this;
    }

    @NonNull private String getRuntime() {
        String tmp = (vmVersion.startsWith("1") ? "libdvm.so" : "libart.so");

        final String runtime = "libdvm.so".equals(tmp)
                ? "Dalvik"
                : "libart.so".equals(tmp) ? "ART" : Constants.UNAVAILABLE;
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

    @Override public String toString() {
        return new Gson().toJson(this, Device.class);
    }

    public static String getAndroidId(@NonNull Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
