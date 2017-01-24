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

import java.util.ArrayList;

import at.amartinz.hardware.utils.HwIoUtils;

/**
 * A class for interacting with the device's emmc
 */
public class EmmcInfo {
    public static final String BRICK_INFO_URL = "http://wiki.cyanogenmod.org/w/EMMC_Bugs";

    public String cid;
    public String date;
    public String mid;
    public String name;
    public String rev;

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

    private EmmcInfo() {
        cid = HwIoUtils.INSTANCE.readOneLine("/sys/class/block/mmcblk0/device/cid");
        date = HwIoUtils.INSTANCE.readOneLine("/sys/class/block/mmcblk0/device/date");
        mid = HwIoUtils.INSTANCE.readOneLine("/sys/class/block/mmcblk0/device/manfid");
        name = HwIoUtils.INSTANCE.readOneLine("/sys/class/block/mmcblk0/device/name");
        rev = ((cid != null && cid.length() > 20) ? cid.substring(18, 20) : "-");
    }

    public static void feedWithInformation(final Device.EmmcInfoListener emmcInfoListener) {
        AsyncTask.execute(new Runnable() {
            @Override public void run() {
                feedWithInformationBlocking(emmcInfoListener);
            }
        });
    }

    @WorkerThread public static void feedWithInformationBlocking(final Device.EmmcInfoListener emmcInfoListener) {
        if (emmcInfoListener != null) {
            emmcInfoListener.onEmmcInfoAvailable(new EmmcInfo());
        }
    }

    public boolean canBrick() {
        final EmmcBugged emmc = new EmmcBugged(name, mid, rev);
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
