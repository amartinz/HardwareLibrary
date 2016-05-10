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

package at.amartinz.hardware.display;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import at.amartinz.hardware.Constants;
import alexander.martinz.libs.hardware.R;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.hardware.utils.HwUtils;
import at.amartinz.execution.RootShell;

public class DisplayColorCalibration {
    public static final String TAG = DisplayColorCalibration.class.getSimpleName();

    private String path;
    private String ctrl;
    private String def;
    private int max;
    private int min;

    public DisplayColorCalibration(Context context) {
        final Resources res = context.getResources();
        final String[] paths = res.getStringArray(R.array.hardware_display_color_calibration_paths);
        final String[] ctrls = res.getStringArray(R.array.hardware_display_color_calibration_ctrls);
        final String[] defs = res.getStringArray(R.array.hardware_display_color_calibration_defs);
        final String[] maxs = res.getStringArray(R.array.hardware_display_color_calibration_max);
        final String[] mins = res.getStringArray(R.array.hardware_display_color_calibration_min);

        final int length = paths.length;
        for (int i = 0; i < length; i++) {
            // if the file exists, set up the values
            if (HwIoUtils.fileExists(paths[i])) {
                // our existing path
                path = paths[i];

                // our control path, optional
                ctrl = ctrls[i];
                if (TextUtils.isEmpty(ctrl)
                    // check if we disabled it
                    || TextUtils.equals(ctrl, "-")
                    // check if it exists
                    || !HwIoUtils.fileExists(ctrl)) {
                    ctrl = null;
                }

                // maximum
                max = HwUtils.tryParseInt(maxs[i]);
                if (Constants.DEBUG) {
                    Log.i(TAG, String.format("max --> %s", max));
                }

                // minimum
                min = HwUtils.tryParseInt(mins[i]);
                if (Constants.DEBUG) {
                    Log.i(TAG, String.format("min --> %s", min));
                }

                // get default value
                def = defs[i];
                if (TextUtils.equals("max", def)) {
                    def = String.valueOf(max);
                } else if (TextUtils.equals("min", def)) {
                    def = String.valueOf(min);
                }

                // and get out of here
                break;
            }
        }
    }

    public boolean isSupported() { return HwIoUtils.fileExists(path); }

    public int getMaxValue() { return max; }

    public int getMinValue() { return min; }

    public int getDefValue() { return HwUtils.tryParseInt(def); }

    @Nullable public String getCurColors() { return HwIoUtils.readOneLine(path); }

    public void setColors(final String colors) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("echo \"%s\" > %s;", colors, path));
        if (!TextUtils.isEmpty(ctrl)) {
            sb.append(String.format("echo \"%s\" > %s;", "1", ctrl));
        }
        RootShell.fireAndForget(sb.toString());
    }

    @Nullable public String getPath() {
        return path;
    }
}
