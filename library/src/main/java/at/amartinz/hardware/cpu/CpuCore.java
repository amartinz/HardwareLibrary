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

import android.text.TextUtils;

import at.amartinz.hardware.utils.HwUtils;

public class CpuCore {
    public int core;
    public int max;
    public int current;
    public String governor;

    public CpuCore(int core, String current, String max, String governor) {
        setCore(core);
        setCurrent(current);
        setMax(max);
        setGovernor(governor);
    }

    public CpuCore setCore(int core) {
        this.core = core;
        return this;
    }

    public CpuCore setCurrent(String current) {
        this.current = HwUtils.tryParseInt(current, 0);
        return this;
    }

    public CpuCore setMax(String max) {
        this.max = HwUtils.tryParseInt(max, 0);
        return this;
    }

    public CpuCore setGovernor(String governor) {
        this.governor = (!TextUtils.isEmpty(governor) ? governor : "0");
        return this;
    }

    @Override public String toString() {
        return String.format("core: %s | max: %s | current: %s | gov: %s", core, max, current, governor);
    }
}
