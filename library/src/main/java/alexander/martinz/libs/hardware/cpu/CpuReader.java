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

package alexander.martinz.libs.hardware.cpu;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Collections;
import java.util.List;

import alexander.martinz.libs.execution.Command;
import alexander.martinz.libs.hardware.Constants;
import alexander.martinz.libs.hardware.utils.IoUtils;
import alexander.martinz.libs.hardware.utils.Utils;
import alexander.martinz.libs.logger.Logger;

public class CpuReader {
    private static final String TAG = CpuReader.class.getSimpleName();

    private static final String PATH_BASE = "/sys/devices/system/cpu/";
    private static final String PATH_COUNT = PATH_BASE + "present";

    //private static final String PATH_CORE_ONLINE = PATH_BASE + "cpu%s/online";
    private static final String PATH_CORE_FREQ_AVAIL = PATH_BASE + "cpu%s/cpufreq/scaling_available_frequencies";
    private static final String PATH_CORE_FREQ_CUR = PATH_BASE + "cpu%s/cpufreq/scaling_cur_freq";
    private static final String PATH_CORE_FREQ_MAX = PATH_BASE + "cpu%s/cpufreq/scaling_max_freq";
    private static final String PATH_CORE_FREQ_MIN = PATH_BASE + "cpu%s/cpufreq/scaling_min_freq";

    // TODO: own file?
    //public static final String PATH_FREQ_TIME_IN_STATE = PATH_BASE + "cpu0/cpufreq/stats/time_in_state";

    private static final String PATH_TEMPERATURE = "/sys/class/thermal/thermal_zone0/temp";

    private CpuReader() { }

    public static void getCpuInformation(Context context, CpuInformationListener listener) {
        final ReadCpuInformationThread thread = new ReadCpuInformationThread(context, listener);
        thread.start();
    }

    public static CpuInformation getCpuInformationBlocking() {
        final CpuInformation cpuInformation = new CpuInformation();

        cpuInformation.coreCount = readAvailableCores();
        cpuInformation.freqAvail = readAvailableFrequency(0);
        cpuInformation.freqCur = readCurrentFrequency(0);
        cpuInformation.freqMax = readMaxFrequency(0);
        cpuInformation.freqMin = readMinFrequency(0);

        cpuInformation.temperature = readTemperature();

        return cpuInformation;
    }

    private static int readTemperature() {
        return IoUtils.readSysfsIntValue(PATH_TEMPERATURE);
    }

    private static int readAvailableCores() {
        // example value: 0-3 -> 0, 1, 2, 3 -> we have 4 cores
        return readAvailableCores(IoUtils.readFile(PATH_COUNT));
    }

    private static int readAvailableCores(final String rawString) {
        if (!TextUtils.isEmpty(rawString)) {
            final int length = rawString.length();
            // 0-3 -> 3
            final String coreCountString = rawString.substring(length - 1, length);
            final Integer coreCount = Utils.tryParseIntRaw(coreCountString);
            if (coreCount != null) {
                // 3 + 1 = 4 cores (yes, i got these math skills in school)
                return (coreCount + 1);
            } else {
                Logger.w(TAG, "Could not get core count!");
            }
        }
        return Constants.INVALID;
    }

    @Nullable private static List<Integer> readAvailableFrequency(int cpuCore) {
        final String freqString = IoUtils.readFile(getPathCoreFreqAvail(cpuCore));
        if (TextUtils.isEmpty(freqString)) {
            return null;
        }
        return Utils.stringToListInteger(freqString);
    }

    @Nullable private static List<Integer> readAvailableFrequency(@Nullable final String freqString) {
        if (TextUtils.isEmpty(freqString)) {
            return null;
        }
        return Utils.stringToListInteger(freqString);
    }

    private static int readCurrentFrequency(int cpuCore) {
        return IoUtils.readSysfsIntValue(getPathCoreFreqCur(cpuCore));
    }

    private static int readMaxFrequency(int cpuCore) {
        return IoUtils.readSysfsIntValue(getPathCoreFreqMax(cpuCore));
    }

    private static int readMinFrequency(int cpuCore) {
        return IoUtils.readSysfsIntValue(getPathCoreFreqMin(cpuCore));
    }

    private static String getPathCoreFreqAvail(int cpuCore) {
        return String.format(PATH_CORE_FREQ_AVAIL, cpuCore);
    }

    private static String getPathCoreFreqCur(int cpuCore) {
        return String.format(PATH_CORE_FREQ_CUR, cpuCore);
    }

    private static String getPathCoreFreqMax(int cpuCore) {
        return String.format(PATH_CORE_FREQ_MAX, cpuCore);
    }

    private static String getPathCoreFreqMin(int cpuCore) {
        return String.format(PATH_CORE_FREQ_MIN, cpuCore);
    }

    private static class ReadCpuInformationThread extends Thread {
        private static final String T_PATH_COUNT = PATH_COUNT;
        private static final String T_PATH_FREQ_AVAIL = getPathCoreFreqAvail(0);
        private static final String T_PATH_FREQ_CUR = getPathCoreFreqCur(0);
        private static final String T_PATH_FREQ_MAX = getPathCoreFreqMax(0);
        private static final String T_PATH_FREQ_MIN = getPathCoreFreqMin(0);
        private static final String T_PATH_TEMPERATURE = PATH_TEMPERATURE;

        private final Context context;

        private final CpuInformationListener listener;

        private CpuInformation cpuInformation;
        private boolean hasFinished;

        public ReadCpuInformationThread(Context context, CpuInformationListener listener) {
            super();
            this.context = context;
            this.listener = listener;
        }

        @Override public void run() {
            cpuInformation = getCpuInformationBlocking();
            // if the cpu information contains an invalid value AND we are not using root, finish
            if (cpuInformation.isValid() || !Constants.USE_ROOT) {
                hasFinished = true;
            } else {
                cpuInformation.resetInvalid();
            }

            while (!hasFinished) {
                if (cpuInformation.coreCount == Constants.NOT_INITIALIZED) {
                    Command cmd = IoUtils.readFileRoot(context, T_PATH_COUNT, readFileListener);
                    if (cmd == null) {
                        Logger.e(this, "Could not read file with root!");
                        break;
                    } else {
                        cpuInformation.coreCount = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.freqAvail == null) {
                    Command cmd = IoUtils.readFileRoot(context, T_PATH_COUNT, readFileListener);
                    if (cmd == null) {
                        Logger.e(this, "Could not read file with root!");
                        break;
                    } else {
                        cpuInformation.freqAvail = Collections.emptyList();
                    }
                }
                if (cpuInformation.freqCur == Constants.NOT_INITIALIZED) {
                    Command cmd = IoUtils.readFileRoot(context, T_PATH_FREQ_CUR, readFileListener);
                    if (cmd == null) {
                        Logger.e(this, "Could not read file with root!");
                        break;
                    } else {
                        cpuInformation.freqCur = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.freqMax == Constants.NOT_INITIALIZED) {
                    Command cmd = IoUtils.readFileRoot(context, T_PATH_FREQ_MAX, readFileListener);
                    if (cmd == null) {
                        Logger.e(this, "Could not read file with root!");
                        break;
                    } else {
                        cpuInformation.freqMax = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.freqMin == Constants.NOT_INITIALIZED) {
                    Command cmd = IoUtils.readFileRoot(context, T_PATH_FREQ_MIN, readFileListener);
                    if (cmd == null) {
                        Logger.e(this, "Could not read file with root!");
                        break;
                    } else {
                        cpuInformation.freqMin = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.temperature == Constants.NOT_INITIALIZED) {
                    Command cmd =
                            IoUtils.readFileRoot(context, T_PATH_TEMPERATURE, readFileListener);
                    if (cmd == null) {
                        Logger.e(this, "Could not read file with root!");
                        break;
                    } else {
                        cpuInformation.temperature = Constants.INITIALIZATION_STARTED;
                    }
                }

                // if we have read all values, we are done
                hasFinished = !cpuInformation.isInitializing();
            }

            if (listener != null) {
                listener.onCpuInformation(cpuInformation);
            }
        }

        private final IoUtils.ReadFileListener readFileListener = new IoUtils.ReadFileListener() {
            @Override public void onFileRead(String path, String content) {
                if (T_PATH_COUNT.equals(path)) {
                    cpuInformation.coreCount = Utils.tryParseInt(content);
                } else if (T_PATH_FREQ_AVAIL.equals(path)) {
                    cpuInformation.freqAvail = readAvailableFrequency(content);
                } else if (T_PATH_FREQ_CUR.equals(path)) {
                    cpuInformation.freqCur = Utils.tryParseInt(content);
                } else if (T_PATH_FREQ_MAX.equals(path)) {
                    cpuInformation.freqMax = Utils.tryParseInt(content);
                } else if (T_PATH_FREQ_MIN.equals(path)) {
                    cpuInformation.freqMin = Utils.tryParseInt(content);
                } else if (T_PATH_TEMPERATURE.equals(path)) {
                    cpuInformation.temperature = Utils.tryParseInt(content);
                }
            }
        };
    }
}
