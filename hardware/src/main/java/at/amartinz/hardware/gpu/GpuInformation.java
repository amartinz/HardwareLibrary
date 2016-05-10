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

package at.amartinz.hardware.gpu;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import at.amartinz.hardware.Constants;
import at.amartinz.hardware.utils.HwUtils;

public class GpuInformation {
    private static final String TAG = GpuInformation.class.getSimpleName();

    public List<Integer> freqAvailable = Collections.emptyList();
    public int freqCur = Constants.NOT_INITIALIZED;
    public int freqMax = Constants.NOT_INITIALIZED;
    public int freqMin = Constants.NOT_INITIALIZED;

    public void resetInvalid() {
        if (freqCur == Constants.INVALID) {
            freqCur = Constants.NOT_INITIALIZED;
        }
        if (freqMax == Constants.INVALID) {
            freqMax = Constants.NOT_INITIALIZED;
        }
        if (freqMin == Constants.INVALID) {
            freqMin = Constants.NOT_INITIALIZED;
        }
    }

    public boolean isInitializing() {
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
        return false;
    }

    public boolean isValid() {
        return (((freqCur != Constants.NOT_INITIALIZED) && (freqCur != Constants.INVALID)) &&
                ((freqMax != Constants.NOT_INITIALIZED) && (freqMax != Constants.INVALID)) &&
                ((freqMin != Constants.NOT_INITIALIZED) && (freqMin != Constants.INVALID)));
    }

    public String freqAsMhzReadable(final int frequency) {
        return String.format("%s (%s)", frequency, freqAsMhz(frequency));
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
                value = HwUtils.tryParseInt(mhzString) / 1000000;
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
            sb.append(GpuInformation.toMhz(frequency.toString()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString().trim();
    }
}
