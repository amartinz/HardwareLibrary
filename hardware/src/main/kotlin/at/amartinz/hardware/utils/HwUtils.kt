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

package at.amartinz.hardware.utils

import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Log
import at.amartinz.hardware.Constants
import java.util.*

object HwUtils {
    private val TAG = HwUtils::class.java.simpleName

    @JvmOverloads fun tryParseInt(toParse: String?, defInt: Int = Constants.INVALID): Int {
        val integer = tryParseIntRaw(toParse) ?: return defInt
        return integer
    }

    fun tryParseIntRaw(toParse: String?): Int? {
        var parse = toParse
        if (parse != null) {
            parse = parse.trim { it <= ' ' }
        }
        if (parse.isNullOrBlank()) {
            return null
        }
        try {
            return parse?.toInt()
        } catch (nfe: NumberFormatException) {
            if (Constants.DEBUG) {
                Log.e(TAG, "Could not parse as Integer", nfe)
            }
        }
        return null
    }

    fun tryParseLong(toParse: String): Long {
        val longRaw = tryParseLongRaw(toParse) ?: return Constants.INVALID.toLong()
        return longRaw
    }

    fun tryParseLongRaw(toParse: String?): Long? {
        var toParse = toParse
        if (toParse != null) {
            toParse = toParse.trim { it <= ' ' }
        }
        if (TextUtils.isEmpty(toParse)) {
            return null
        }
        try {
            return toParse?.toLong()
        } catch (nfe: NumberFormatException) {
            if (Constants.DEBUG) {
                Log.e(TAG, "Could not parse as Long", nfe)
            }
        }

        return null
    }

    fun isEnabled(string: String?, contains: Boolean): Boolean {
        var s = string
        if (s != null) {
            s = s.trim { it <= ' ' }.toUpperCase()
            for (state in Constants.ENABLED_STATES) {
                if (contains) {
                    if (s.contains(state)) {
                        return true
                    }
                } else {
                    if (s == state) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun getDate(time: Long): String {
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = time
        return DateFormat.format("dd-MM-yyyy", cal).toString()
    }

    fun stringToList(arrayString: String?): List<String> {
        if (TextUtils.isEmpty(arrayString)) {
            return emptyList()
        }
        val splitted = arrayString!!.trim { it <= ' ' }.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        val list = ArrayList(Arrays.asList(*splitted))
        Collections.sort(list)
        return list
    }

    fun stringToListInteger(arrayString: String?): List<Int> {
        if (TextUtils.isEmpty(arrayString)) {
            return emptyList()
        }
        val splitted = arrayString!!.trim { it <= ' ' }.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        val list = splitted.map { part -> tryParseInt(part.trim { it <= ' ' }) }

        Collections.sort(list)
        return list
    }

}
