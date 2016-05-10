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

import at.amartinz.hardware.R;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.hardware.utils.HwUtils;
import at.amartinz.execution.RootShell;

public class DisplayGammaCalibration {
    public static final String TAG = DisplayGammaCalibration.class.getSimpleName();

    private String[] paths;
    private String[] descriptors;
    private int max;
    private int min;

    public DisplayGammaCalibration(Context context) {
        final Resources res = context.getResources();
        final String[] paths = res.getStringArray(R.array.hardware_display_gamma_calibration_paths);
        final String[] descs = res.getStringArray(R.array.hardware_display_gamma_calibration_descs);
        final String[] maxs = res.getStringArray(R.array.hardware_display_gamma_calibration_max);
        final String[] mins = res.getStringArray(R.array.hardware_display_gamma_calibration_min);

        String[] splitted;
        final int length = paths.length;
        for (int i = 0; i < length; i++) {
            // split it
            splitted = paths[i].split(",");
            boolean exists = false;
            for (final String path : splitted) {
                // if the file exists, set up the values
                if (HwIoUtils.fileExists(path)) {
                    // and get out of here to continue
                    exists = true;
                    break;
                }
            }

            // if the controls exist, set up the values and end searching
            if (exists) {
                this.paths = splitted;
                // maximum and minimum
                max = HwUtils.tryParseInt(maxs[i]);
                min = HwUtils.tryParseInt(mins[i]);
                // descriptors
                descriptors = descs[i].split(",");
                // get out of here finally
                break;
            }
        }
    }

    public boolean isSupported() { return paths != null && HwIoUtils.fileExists(paths[0]); }

    public int getMaxValue(final int control) { return max; }

    public int getMinValue(final int control) { return min; }

    public String getCurGamma(final int control) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(HwIoUtils.readOneLine(paths[i]));
        }
        return sb.toString();
    }

    public void setGamma(final int control, final String gamma) {
        if (paths.length <= 0) {
            return;
        }

        final String[] split = gamma.split(" ");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            sb.append(String.format("echo \"%s\" > %s;", split[i], paths[i]));
        }
        RootShell.fireAndForget(sb.toString());
    }

    public String[] getDescriptors() { return descriptors; }

    // TODO: update if needed, return 1 for now
    public int getNumberOfControls() {
        return 1;
    }

    @Nullable public String[] getPaths(final int control) { return paths; }
}
