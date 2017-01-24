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

package at.amartinz.hardware.cpu;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

import at.amartinz.hardware.Constants;
import at.amartinz.hardware.utils.HwUtils;

public class CpuInformation {
    private static final String TAG = CpuInformation.class.getSimpleName();

    // TODO: improve octa core support
    public boolean isOctaCore;

    public int coreCount = Constants.INSTANCE.getNOT_INITIALIZED();

    public List<Integer> freqAvail;
    public int freqCur = Constants.INSTANCE.getNOT_INITIALIZED();
    public int freqMax = Constants.INSTANCE.getNOT_INITIALIZED();
    public int freqMin = Constants.INSTANCE.getNOT_INITIALIZED();

    public List<String> govAvail;
    public String govCur = Constants.INSTANCE.getNOT_INITIALIZED_STR();

    public int temperature = Constants.INSTANCE.getNOT_INITIALIZED();

    public void resetInvalid() {
        if (coreCount == Constants.INSTANCE.getINVALID()) {
            coreCount = Constants.INSTANCE.getNOT_INITIALIZED();
        }
        if (freqCur == Constants.INSTANCE.getINVALID()) {
            freqCur = Constants.INSTANCE.getNOT_INITIALIZED();
        }
        if (freqMax == Constants.INSTANCE.getINVALID()) {
            freqMax = Constants.INSTANCE.getNOT_INITIALIZED();
        }
        if (freqMin == Constants.INSTANCE.getINVALID()) {
            freqMin = Constants.INSTANCE.getNOT_INITIALIZED();
        }
        if (Constants.INSTANCE.getINVALID_STR().equals(govCur)) {
            govCur = Constants.INSTANCE.getNOT_INITIALIZED_STR();
        }
        if (temperature == Constants.INSTANCE.getINVALID()) {
            temperature = Constants.INSTANCE.getNOT_INITIALIZED();
        }
    }

    public boolean isInitializing() {
        if (((coreCount == Constants.INSTANCE.getNOT_INITIALIZED()) ||
             (coreCount == Constants.INSTANCE.getINITIALIZATION_STARTED()))) {
            return true;
        }
        if (freqAvail == null) {
            return true;
        }
        if (((freqCur == Constants.INSTANCE.getNOT_INITIALIZED()) ||
             (freqCur == Constants.INSTANCE.getINITIALIZATION_STARTED()))) {
            return true;
        }
        if (((freqMax == Constants.INSTANCE.getNOT_INITIALIZED()) ||
             (freqMax == Constants.INSTANCE.getINITIALIZATION_STARTED()))) {
            return true;
        }
        if (((freqMin == Constants.INSTANCE.getNOT_INITIALIZED()) ||
             (freqMin == Constants.INSTANCE.getINITIALIZATION_STARTED()))) {
            return true;
        }
        if (govAvail == null) {
            return true;
        }
        if (((Constants.INSTANCE.getNOT_INITIALIZED_STR().equals(govCur)) ||
             (Constants.INSTANCE.getINITIALIZATION_STARTED_STR().equals(govCur)))) {
            return true;
        }
        if (((temperature == Constants.INSTANCE.getNOT_INITIALIZED()) ||
             (temperature == Constants.INSTANCE.getINITIALIZATION_STARTED()))) {
            return true;
        }
        return false;
    }

    public boolean isValid() {
        return (((coreCount != Constants.INSTANCE.getNOT_INITIALIZED()) && (coreCount != Constants.INSTANCE.getINVALID())) &&
                ((freqCur != Constants.INSTANCE.getNOT_INITIALIZED()) && (freqCur != Constants.INSTANCE.getINVALID())) &&
                ((freqMax != Constants.INSTANCE.getNOT_INITIALIZED()) && (freqMax != Constants.INSTANCE.getINVALID())) &&
                ((freqMin != Constants.INSTANCE.getNOT_INITIALIZED()) && (freqMin != Constants.INSTANCE.getINVALID())) &&
                ((!Constants.INSTANCE.getNOT_INITIALIZED_STR().equals(govCur)) && (!Constants.INSTANCE.getINVALID_STR().equals(govCur))) &&
                ((temperature != Constants.INSTANCE.getNOT_INITIALIZED()) && (temperature != Constants.INSTANCE.getINVALID())));
    }

    public String freqAsMhz(final int frequency) {
        return toMhz(String.valueOf(frequency));
    }

    /**
     * Convert to MHz and append a tag (MHz)
     *
     * @param mhzString The string to convert to MHz
     * @return The tagged and converted string OR null if it can not be converted
     */
    @NonNull public static String toMhz(final String mhzString) {
        int value = Constants.INSTANCE.getINVALID();
        if (!TextUtils.isEmpty(mhzString)) {
            try {
                value = HwUtils.INSTANCE.tryParseInt(mhzString) / 1000;
            } catch (NumberFormatException exc) {
                if (Constants.INSTANCE.getDEBUG()) {
                    Log.e(TAG, "toMhz", exc);
                }
                value = Constants.INSTANCE.getINVALID();
            }
        }

        if (value != Constants.INSTANCE.getINVALID()) {
            return String.valueOf(value) + " MHz";
        }
        return "";
    }

    /**
     * Convert from MHz
     *
     * @param value The string in MHz format (eg. "2457 MHz")
     * @return The original value as integer (eg. 2457000)
     */
    public static int fromMhz(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return -1;
        }
        if (!value.contains("MHz")) {
            // no MHz, maybe we have passed in a correct frequency already...
            return HwUtils.INSTANCE.tryParseInt(value.trim(), -1);
        }
        value = value.replace("MHz", "").trim();

        final int intValue = HwUtils.INSTANCE.tryParseInt(value, -1);
        if (intValue != -1) {
            return intValue * 1000;
        }
        return -1;
    }

    public static String listFrequenciesFormatted(@Nullable final List<Integer> freqAvail) {
        if (freqAvail == null || freqAvail.isEmpty()) {
            return "-";
        }

        final StringBuilder sb = new StringBuilder();
        final Iterator<Integer> iterator = freqAvail.iterator();
        while (iterator.hasNext()) {
            final Integer frequency = iterator.next();
            if (frequency == null) {
                continue;
            }
            sb.append(CpuInformation.toMhz(frequency.toString()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString().trim();
    }
}
