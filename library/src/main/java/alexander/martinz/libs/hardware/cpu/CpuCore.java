/*
 *  Copyright (C) 2013 - 2016 Alexander "Evisceration" Martinz
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
 *
 */
package alexander.martinz.libs.hardware.cpu;

import android.text.TextUtils;

import alexander.martinz.libs.hardware.utils.Utils;

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
        this.current = Utils.tryParseInt(current, 0);
        return this;
    }

    public CpuCore setMax(String max) {
        this.max = Utils.tryParseInt(max, 0);
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
