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
package at.amartinz.hardware;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;

import at.amartinz.hardware.utils.HwIoUtils;

/**
 * A class for interacting with the device's emmc
 */
public class Emmc {
    public static final String BRICK_INFO_URL = "http://wiki.cyanogenmod.org/w/EMMC_Bugs";

    private String cid = null;
    private String date = null;
    private String mid = null;
    private String name = null;
    private String rev = null;

    private static final ArrayList<EmmcBugged> EMMC_BUGGED_LIST = new ArrayList<>();

    static {
        EMMC_BUGGED_LIST.add(new EmmcBugged("KYL00M", "15", "25", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("M8G2FA", "15", "0", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("MAG2GA", "15", "0", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("MAG4FA", "15", "25", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("MBG8FA", "15", "0", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("MCGAFA", "15", "0", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("VAL00M", "15", "0", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("VTU001", "15", "f1", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("VYL00M", "15", "0", EmmcBugged.IMPACT_BRICK));
        EMMC_BUGGED_LIST.add(new EmmcBugged("VZL00M", "15", "0", EmmcBugged.IMPACT_BRICK));
    }

    private static Emmc sInstance;

    private Emmc() { }

    public static Emmc get() {
        if (sInstance == null) {
            sInstance = new Emmc();
        }
        return sInstance;
    }

    public static boolean isSupported() {
        return new File("/sys/class/block/mmcblk0/device/").exists();
    }

    @Nullable public String getCid() {
        if (cid == null) {
            cid = HwIoUtils.readOneLine("/sys/class/block/mmcblk0/device/cid");
        }
        return cid;
    }

    @Nullable public String getDate() {
        if (date == null) {
            date = HwIoUtils.readOneLine("/sys/class/block/mmcblk0/device/date");
        }
        return date;
    }

    @Nullable public String getMid() {
        if (mid == null) {
            mid = HwIoUtils.readOneLine("/sys/class/block/mmcblk0/device/manfid");
        }
        return mid;
    }

    @Nullable public String getName() {
        if (name == null) {
            name = HwIoUtils.readOneLine("/sys/class/block/mmcblk0/device/name");
        }
        return name;
    }

    @Nullable public String getRev() {
        if (rev == null) {
            rev = (getCid() != null && getCid().length() > 20)
                    ? getCid().substring(18, 20)
                    : "-";
        }
        return rev;
    }

    public boolean canBrick() {
        final EmmcBugged emmc = new EmmcBugged(getName(), getMid(), getRev());
        for (final EmmcBugged bugged : EMMC_BUGGED_LIST) {
            if (bugged != null
                && TextUtils.equals(emmc.name, bugged.name)
                && TextUtils.equals(emmc.mid, bugged.mid)
                && !TextUtils.isEmpty(bugged.rev)
                && (TextUtils.equals(bugged.rev, "0") || TextUtils.equals(emmc.rev, bugged.rev))) {
                return bugged.impact == EmmcBugged.IMPACT_BRICK;
            }
        }

        return false;
    }

    public static class EmmcBugged {
        public static final int IMPACT_NONE = 0;
        public static final int IMPACT_CORRUPTION = 1;
        public static final int IMPACT_BRICK = 2;

        public final String name;
        public final String mid;
        public final String rev;

        public int impact;

        public EmmcBugged(final String name, final String mid, final String rev) {
            this(name, mid, rev, IMPACT_NONE);
        }

        public EmmcBugged(final String name, final String mid, final String rev, final int impact) {
            this.name = name;
            this.mid = mid;
            this.rev = rev;
            this.impact = impact;
        }
    }

}
