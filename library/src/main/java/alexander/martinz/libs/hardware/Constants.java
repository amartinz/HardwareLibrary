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

package alexander.martinz.libs.hardware;

public class Constants {
    public static boolean DEBUG;

    public static final int API_NOT_SUPPORTED = -100;

    public static final int NOT_INITIALIZED = -3;
    public static final int INITIALIZATION_STARTED = -2;
    public static final int INVALID = -1;

    public static final String NOT_INITIALIZED_STR = "---3";
    public static final String INITIALIZATION_STARTED_STR = "---2";
    public static final String INVALID_STR = "---1";

    public static final String UNAVAILABLE = "Unavailable";
    public static final String UNKNOWN = "Unknown";

    public static final String[] ENABLED_STATES = { "Y", "TRUE", "1", "255" };
}
