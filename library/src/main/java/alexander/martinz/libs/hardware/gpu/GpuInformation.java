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

package alexander.martinz.libs.hardware.gpu;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.hardware.utils.Utils;

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
                value = Utils.tryParseInt(mhzString) / 1000000;
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
