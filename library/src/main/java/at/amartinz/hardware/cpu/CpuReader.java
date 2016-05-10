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

package at.amartinz.hardware.cpu;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.List;

import at.amartinz.hardware.Constants;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.hardware.utils.HwUtils;
import at.amartinz.execution.Command;
import at.amartinz.execution.RootCheck;

public class CpuReader {
    private static final String TAG = CpuReader.class.getSimpleName();

    private static final String PATH_BASE = "/sys/devices/system/cpu/";
    private static final String PATH_COUNT = PATH_BASE + "present";

    //private static final String PATH_CORE_ONLINE = PATH_BASE + "cpu%s/online";
    private static final String PATH_CORE_BASE = PATH_BASE + "cpu%s/";
    private static final String PATH_CORE_FREQ_AVAIL = PATH_CORE_BASE + "cpufreq/scaling_available_frequencies";
    private static final String PATH_CORE_FREQ_CUR = PATH_CORE_BASE + "cpufreq/scaling_cur_freq";
    private static final String PATH_CORE_FREQ_MAX = PATH_CORE_BASE + "cpufreq/scaling_max_freq";
    private static final String PATH_CORE_FREQ_MIN = PATH_CORE_BASE + "cpufreq/scaling_min_freq";

    private static final String PATH_CORE_GOV_AVAIL = PATH_CORE_BASE + "cpufreq/scaling_available_governors";
    private static final String PATH_CORE_GOV = PATH_CORE_BASE + "cpufreq/scaling_governor";

    // TODO: own file?
    //public static final String PATH_FREQ_TIME_IN_STATE = PATH_BASE + "cpu0/cpufreq/stats/time_in_state";

    private static final String PATH_TEMPERATURE = "/sys/class/thermal/thermal_zone0/temp";

    private CpuReader() { }

    public static void getCpuInformation(CpuInformationListener listener) {
        AsyncTask.execute(new ReadCpuInformationRunnable(listener));
    }

    @WorkerThread public static CpuInformation getCpuInformationBlocking() {
        final CpuInformation cpuInformation = new CpuInformation();

        cpuInformation.coreCount = readAvailableCores();
        cpuInformation.isOctaCore = cpuInformation.coreCount > 4;

        // some octa core cpus are buggy and need special treatment
        if (cpuInformation.isOctaCore) {
            if (Constants.DEBUG) {
                Log.i(TAG, "using special octa core treatment");
            }
            int cpuToReadFrom = 0;
            for (; cpuToReadFrom < 4; cpuToReadFrom++) {
                final File cpuFreqDir = new File(getPathCoreBase(cpuToReadFrom), "cpufreq");
                if (cpuFreqDir.exists()) {
                    break;
                }
            }
            if (Constants.DEBUG) {
                Log.v(TAG, String.format("Using cpu%s to read from", cpuToReadFrom));
            }

            cpuInformation.freqAvail = readFreqAvail(cpuToReadFrom);
            cpuInformation.freqCur = readFreqCur(cpuToReadFrom);
            cpuInformation.freqMax = readFreqMax(cpuToReadFrom);
            cpuInformation.freqMin = readFreqMin(cpuToReadFrom);

            cpuInformation.govAvail = readGovAvail(cpuToReadFrom);
            cpuInformation.govCur = readGovernor(cpuToReadFrom);
        } else {
            cpuInformation.freqAvail = readFreqAvail(0);
            cpuInformation.freqCur = readFreqCur(0);
            cpuInformation.freqMax = readFreqMax(0);
            cpuInformation.freqMin = readFreqMin(0);

            cpuInformation.govAvail = readGovAvail(0);
            cpuInformation.govCur = readGovernor(0);
        }

        cpuInformation.temperature = readTemperature();

        return cpuInformation;
    }

    private static int readTemperature() {
        return HwIoUtils.readSysfsIntValue(PATH_TEMPERATURE);
    }

    public static int readAvailableCores() {
        // example value: 0-3 -> 0, 1, 2, 3 -> we have 4 cores
        return readAvailableCores(HwIoUtils.readFile(PATH_COUNT));
    }

    private static int readAvailableCores(final String rawString) {
        if (!TextUtils.isEmpty(rawString)) {
            final int length = rawString.length();
            // 0-3 -> 3
            final String coreCountString = rawString.substring(length - 1, length);
            final Integer coreCount = HwUtils.tryParseIntRaw(coreCountString);
            if (coreCount != null) {
                // 3 + 1 = 4 cores (yes, i got these math skills in school)
                return (coreCount + 1);
            } else if (Constants.DEBUG) {
                Log.w(TAG, "Could not get core count!");
            }
        }
        return Constants.INVALID;
    }

    @NonNull public static List<Integer> readFreqAvail(int cpuCore) {
        final String freqString = HwIoUtils.readFile(getPathCoreFreqAvail(cpuCore));
        if (TextUtils.isEmpty(freqString)) {
            return Collections.emptyList();
        }
        return HwUtils.stringToListInteger(freqString);
    }

    @NonNull private static List<Integer> readFreqAvail(@Nullable final String freqString) {
        if (TextUtils.isEmpty(freqString)) {
            return Collections.emptyList();
        }
        return HwUtils.stringToListInteger(freqString);
    }

    @NonNull public static List<String> readGovAvail(int cpuCore) {
        final String freqString = HwIoUtils.readFile(getPathCoreGovAvail(cpuCore));
        if (TextUtils.isEmpty(freqString)) {
            return Collections.emptyList();
        }
        return HwUtils.stringToList(freqString);
    }

    @NonNull private static List<String> readGovAvail(@Nullable final String govString) {
        if (TextUtils.isEmpty(govString)) {
            return Collections.emptyList();
        }
        return HwUtils.stringToList(govString);
    }

    private static int readFreqCur(int cpuCore) {
        return HwIoUtils.readSysfsIntValue(getPathCoreFreqCur(cpuCore));
    }

    private static int readFreqMax(int cpuCore) {
        return HwIoUtils.readSysfsIntValue(getPathCoreFreqMax(cpuCore));
    }

    private static int readFreqMin(int cpuCore) {
        return HwIoUtils.readSysfsIntValue(getPathCoreFreqMin(cpuCore));
    }

    private static String readGovernor(int cpuCore) {
        return HwIoUtils.readSysfsStringValue(getPathCoreGov(cpuCore));
    }

    public static String getPathCoreBase(int cpuCore) {
        return String.format(PATH_CORE_BASE, cpuCore);
    }

    public static String getPathCoreFreqAvail(int cpuCore) {
        return String.format(PATH_CORE_FREQ_AVAIL, cpuCore);
    }

    public static String getPathCoreFreqCur(int cpuCore) {
        return String.format(PATH_CORE_FREQ_CUR, cpuCore);
    }

    public static String getPathCoreFreqMax(int cpuCore) {
        return String.format(PATH_CORE_FREQ_MAX, cpuCore);
    }

    public static String getPathCoreFreqMin(int cpuCore) {
        return String.format(PATH_CORE_FREQ_MIN, cpuCore);
    }

    public static String getPathCoreGovAvail(int cpuCore) {
        return String.format(PATH_CORE_GOV_AVAIL, cpuCore);
    }

    public static String getPathCoreGov(int cpuCore) {
        return String.format(PATH_CORE_GOV, cpuCore);
    }

    private static class ReadCpuInformationRunnable implements Runnable {
        private static final String T_PATH_COUNT = PATH_COUNT;
        private static final String T_PATH_FREQ_AVAIL = getPathCoreFreqAvail(0);
        private static final String T_PATH_FREQ_CUR = getPathCoreFreqCur(0);
        private static final String T_PATH_FREQ_MAX = getPathCoreFreqMax(0);
        private static final String T_PATH_FREQ_MIN = getPathCoreFreqMin(0);
        private static final String T_PATH_GOV_AVAIL = getPathCoreGovAvail(0);
        private static final String T_PATH_GOV = getPathCoreGov(0);
        private static final String T_PATH_TEMPERATURE = PATH_TEMPERATURE;

        private final CpuInformationListener listener;

        private CpuInformation cpuInformation;
        private boolean hasFinished;

        public ReadCpuInformationRunnable(CpuInformationListener listener) {
            super();
            this.listener = listener;
        }

        @Override public void run() {
            cpuInformation = getCpuInformationBlocking();
            // if the cpu information contains an invalid value AND we are not using root, finish
            if (cpuInformation.isValid() || !RootCheck.isRooted()) {
                hasFinished = true;
            } else {
                cpuInformation.resetInvalid();
            }

            while (!hasFinished) {
                if (cpuInformation.coreCount == Constants.NOT_INITIALIZED) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_COUNT, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.coreCount = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.freqAvail == null) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_FREQ_AVAIL, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.freqAvail = Collections.emptyList();
                    }
                }
                if (cpuInformation.freqCur == Constants.NOT_INITIALIZED) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_FREQ_CUR, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.freqCur = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.freqMax == Constants.NOT_INITIALIZED) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_FREQ_MAX, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.freqMax = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.freqMin == Constants.NOT_INITIALIZED) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_FREQ_MIN, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.freqMin = Constants.INITIALIZATION_STARTED;
                    }
                }
                if (cpuInformation.govAvail == null) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_GOV_AVAIL, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.govAvail = Collections.emptyList();
                    }
                }
                if (Constants.NOT_INITIALIZED_STR.equals(cpuInformation.govCur)) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_GOV, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.govCur = Constants.INITIALIZATION_STARTED_STR;
                    }
                }
                if (cpuInformation.temperature == Constants.NOT_INITIALIZED) {
                    Command cmd = HwIoUtils.readFileRoot(T_PATH_TEMPERATURE, readFileListener);
                    if (cmd == null) {
                        break;
                    } else {
                        cpuInformation.temperature = Constants.INITIALIZATION_STARTED;
                    }
                }

                // if we have read all values, we are done
                hasFinished = !cpuInformation.isInitializing();
            }

            if (cpuInformation.freqAvail != null && !cpuInformation.freqAvail.isEmpty()) {
                if (cpuInformation.freqMin == -1) {
                    cpuInformation.freqMin = cpuInformation.freqAvail.get(0);
                }
                if (cpuInformation.freqMax == -1) {
                    final int maxPos = cpuInformation.freqAvail.size() - 1;
                    cpuInformation.freqMax = cpuInformation.freqAvail.get(maxPos);
                }
            }

            if (listener != null) {
                listener.onCpuInformation(cpuInformation);
            }
        }

        private final HwIoUtils.ReadFileListener readFileListener = new HwIoUtils.ReadFileListener() {
            @Override public void onFileRead(String path, String content) {
                if (T_PATH_COUNT.equals(path)) {
                    cpuInformation.coreCount = HwUtils.tryParseInt(content);
                } else if (T_PATH_FREQ_AVAIL.equals(path)) {
                    cpuInformation.freqAvail = readFreqAvail(content);
                } else if (T_PATH_FREQ_CUR.equals(path)) {
                    cpuInformation.freqCur = HwUtils.tryParseInt(content);
                } else if (T_PATH_FREQ_MAX.equals(path)) {
                    cpuInformation.freqMax = HwUtils.tryParseInt(content);
                } else if (T_PATH_FREQ_MIN.equals(path)) {
                    cpuInformation.freqMin = HwUtils.tryParseInt(content);
                } else if (T_PATH_GOV_AVAIL.equals(path)) {
                    cpuInformation.govAvail = readGovAvail(content);
                } else if (T_PATH_GOV.equals(path)) {
                    cpuInformation.govCur = content;
                } else if (T_PATH_TEMPERATURE.equals(path)) {
                    cpuInformation.temperature = HwUtils.tryParseInt(content);
                }
            }
        };
    }
}
