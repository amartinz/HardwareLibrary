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

import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.util.ArrayList;

import alexander.martinz.libs.hardware.utils.IoUtils;

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
        cid = IoUtils.readOneLine("/sys/class/block/mmcblk0/device/cid");
        date = IoUtils.readOneLine("/sys/class/block/mmcblk0/device/date");
        mid = IoUtils.readOneLine("/sys/class/block/mmcblk0/device/manfid");
        name = IoUtils.readOneLine("/sys/class/block/mmcblk0/device/name");
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
