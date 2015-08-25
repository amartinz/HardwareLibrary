package alexander.martinz.libs.hardware.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateFormat;

import java.util.Calendar;
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
                    if (s.contains(state)) return true;
                } else {
                    if (s.equals(state)) return true;
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

}
