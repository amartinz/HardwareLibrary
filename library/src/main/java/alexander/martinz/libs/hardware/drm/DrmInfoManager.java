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

package alexander.martinz.libs.hardware.drm;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;

/**
 * Created by amartinz on 27.04.16.
 *
 * UUID's obtained from <a href="http://dashif.org/identifiers/protection/">DASH Industry Forum</a>.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class DrmInfoManager {
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    @NonNull public static BaseDrmInfo getWidevineDrmInfo() {
        return new WidevineDrmInfo();
    }

    @NonNull public static BaseDrmInfo getPlayReadyDrmInfo() {
        return new PlayReadyDrmInfo();
    }
}
