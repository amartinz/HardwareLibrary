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

    public int coreCount = Constants.NOT_INITIALIZED;

    public List<Integer> freqAvail;
    public int freqCur = Constants.NOT_INITIALIZED;
    public int freqMax = Constants.NOT_INITIALIZED;
    public int freqMin = Constants.NOT_INITIALIZED;

    public List<String> govAvail;
    public String govCur = Constants.NOT_INITIALIZED_STR;

    public int temperature = Constants.NOT_INITIALIZED;

    public void resetInvalid() {
        if (coreCount == Constants.INVALID) {
            coreCount = Constants.NOT_INITIALIZED;
        }
        if (freqCur == Constants.INVALID) {
            freqCur = Constants.NOT_INITIALIZED;
        }
        if (freqMax == Constants.INVALID) {
            freqMax = Constants.NOT_INITIALIZED;
        }
        if (freqMin == Constants.INVALID) {
            freqMin = Constants.NOT_INITIALIZED;
        }
        if (Constants.INVALID_STR.equals(govCur)) {
            govCur = Constants.NOT_INITIALIZED_STR;
        }
        if (temperature == Constants.INVALID) {
            temperature = Constants.NOT_INITIALIZED;
        }
    }

    public boolean isInitializing() {
        if (((coreCount == Constants.NOT_INITIALIZED) ||
             (coreCount == Constants.INITIALIZATION_STARTED))) {
            return true;
        }
        if (freqAvail == null) {
            return true;
        }
        if (((freqCur == Constants.NOT_INITIALIZED) ||
             (freqCur == Constants.INITIALIZATION_STARTED))) {
            return true;
        }
        if (((freqMax == Constants.NOT_INITIALIZED) ||
             (freqMax == Constants.INITIALIZATION_STARTED))) {
            return true;
        }
        if (((freqMin == Constants.NOT_INITIALIZED) ||
             (freqMin == Constants.INITIALIZATION_STARTED))) {
            return true;
        }
        if (govAvail == null) {
            return true;
        }
        if (((Constants.NOT_INITIALIZED_STR.equals(govCur)) ||
             (Constants.INITIALIZATION_STARTED_STR.equals(govCur)))) {
            return true;
        }
        if (((temperature == Constants.NOT_INITIALIZED) ||
             (temperature == Constants.INITIALIZATION_STARTED))) {
            return true;
        }
        return false;
    }

    public boolean isValid() {
        return (((coreCount != Constants.NOT_INITIALIZED) && (coreCount != Constants.INVALID)) &&
                ((freqCur != Constants.NOT_INITIALIZED) && (freqCur != Constants.INVALID)) &&
                ((freqMax != Constants.NOT_INITIALIZED) && (freqMax != Constants.INVALID)) &&
                ((freqMin != Constants.NOT_INITIALIZED) && (freqMin != Constants.INVALID)) &&
                ((!Constants.NOT_INITIALIZED_STR.equals(govCur)) && (!Constants.INVALID_STR.equals(govCur))) &&
                ((temperature != Constants.NOT_INITIALIZED) && (temperature != Constants.INVALID)));
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
    @Nullable public static String toMhz(final String mhzString) {
        int value = Constants.INVALID;
        if (!TextUtils.isEmpty(mhzString)) {
            try {
                value = HwUtils.tryParseInt(mhzString) / 1000;
            } catch (NumberFormatException exc) {
                if (Constants.DEBUG) {
                    Log.e(TAG, "toMhz", exc);
                }
                value = Constants.INVALID;
            }
        }

        if (value != Constants.INVALID) {
            return String.valueOf(value) + " MHz";
        }
        return null;
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
            return HwUtils.tryParseInt(value.trim(), -1);
        }
        value = value.replace("MHz", "").trim();

        final int intValue = HwUtils.tryParseInt(value, -1);
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
