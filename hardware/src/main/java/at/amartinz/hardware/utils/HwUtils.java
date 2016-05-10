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

package at.amartinz.hardware.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import at.amartinz.hardware.Constants;

public class HwUtils {
    private static final String TAG = HwUtils.class.getSimpleName();

    private HwUtils() { }

    public static int tryParseInt(final String toParse) {
        return tryParseInt(toParse, Constants.INVALID);
    }

    public static int tryParseInt(final String toParse, final int defInt) {
        final Integer integer = tryParseIntRaw(toParse);
        if (integer == null) {
            return defInt;
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
            if (Constants.DEBUG) {
                Log.e(TAG, "Could not parse as Integer", nfe);
            }
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
            if (Constants.DEBUG) {
                Log.e(TAG, "Could not parse as Long", nfe);
            }
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

    public static List<String> stringToList(@Nullable String arrayString) {
        if (TextUtils.isEmpty(arrayString)) {
            return Collections.emptyList();
        }
        final String[] splitted = arrayString.trim().split(" ");
        final ArrayList<String> list = new ArrayList<>(Arrays.asList(splitted));
        Collections.sort(list);
        return list;
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
