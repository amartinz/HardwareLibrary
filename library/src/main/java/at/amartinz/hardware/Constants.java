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

package at.amartinz.hardware;

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
