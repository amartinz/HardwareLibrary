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

package alexander.martinz.libs.hardware.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.logger.Logger;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    private Utils() { }

    public static int tryParseInt(final String toParse) {
        final Integer integer = tryParseIntRaw(toParse);
        if (integer == null) {
            return Constants.INVALID;
        }
        return integer;
    }

    @Nullable public static Integer tryParseIntRaw(String toParse) {
        if (toParse != null) {
            toParse = toParse.trim();
        }
        if (TextUtils.isEmpty(toParse)) {
            return null;
        }
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException nfe) {
            Logger.e(TAG, "Could not parse as Integer", nfe);
        }
        return null;
    }

    public static long tryParseLong(final String toParse) {
        final Long longRaw = tryParseLongRaw(toParse);
        if (longRaw == null) {
            return Constants.INVALID;
        }
        return longRaw;
    }

    @Nullable public static Long tryParseLongRaw(String toParse) {
        if (toParse != null) {
            toParse = toParse.trim();
        }
        if (TextUtils.isEmpty(toParse)) {
            return null;
        }
        try {
            return Long.parseLong(toParse);
        } catch (NumberFormatException nfe) {
            Logger.e(TAG, "Could not parse as Long", nfe);
        }
        return null;
    }

    public static boolean isEnabled(String s, final boolean contains) {
        if (s != null) {
            s = s.trim().toUpperCase();
            for (final String state : Constants.ENABLED_STATES) {
                if (contains) {
                    if (s.contains(state)) {
                        return true;
                    }
                } else {
                    if (s.equals(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getDate(final long time) {
        final Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        return DateFormat.format("dd-MM-yyyy", cal).toString();
    }

    public static List<Integer> stringToListInteger(@Nullable String arrayString) {
        if (TextUtils.isEmpty(arrayString)) {
            return Collections.emptyList();
        }
        final ArrayList<Integer> list = new ArrayList<>();
        final String[] splitted = arrayString.trim().split(" ");
        for (final String part : splitted) {
            list.add(tryParseInt(part.trim()));
        }

        Collections.sort(list);
        return list;
    }

}
