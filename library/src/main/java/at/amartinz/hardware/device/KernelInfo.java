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

package at.amartinz.hardware.device;

import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.amartinz.hardware.Constants;
import at.amartinz.hardware.utils.HwIoUtils;
import at.amartinz.execution.Command;
import at.amartinz.execution.RootCheck;

/**
 * A class which parses /proc/version and prepares information ready for usage
 */
public class KernelInfo {
    private static final String TAG = KernelInfo.class.getSimpleName();

    private static final String PATH_PROC_VERSION = "/proc/version";

    public String version;
    public String host;
    public String toolchain;
    public String revision;
    public String extras;
    public String date;

    private KernelInfo() { }

    public static void feedWithInformation(final Device.KernelInfoListener kernelInfoListener) {
        AsyncTask.execute(new Runnable() {
            @Override public void run() {
                feedWithInformationBlocking(kernelInfoListener);
            }
        });
    }

    @WorkerThread public static void feedWithInformationBlocking(final Device.KernelInfoListener kernelInfoListener) {
        final String content = HwIoUtils.readFile(PATH_PROC_VERSION);
        if (!TextUtils.isEmpty(content)) {
            feedWithInformation(content, kernelInfoListener);
            return;
        }

        // If we could not read the file and we do not have root, then we can not read it...
        if (!RootCheck.isRooted()) {
            return;
        }

        final Command cmd = HwIoUtils.readFileRoot(PATH_PROC_VERSION, new HwIoUtils.ReadFileListener() {
            @Override public void onFileRead(String path, String content) {
                feedWithInformation(content, kernelInfoListener);
            }
        });
        if (cmd == null) {
            if (Constants.DEBUG) {
                Log.e(TAG, "Could not read file with root!");
            }
        }
    }

    private static void feedWithInformation(String content, Device.KernelInfoListener listener) {
        if (TextUtils.isEmpty(content)) {
            return;
        }

        // replace new lines as the readFile method appends a new line
        content = content.replace("\n", "").trim();

        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (toolchain version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
                "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
                "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
                "(\\(gcc.+? \\)) " +    /* group 3: GCC version information */
                "(#\\d+) " +              /* group 4: "#1" */
                "(.*?)?" +              /* group 5: optional SMP, PREEMPT, and any CONFIG_FLAGS */
                "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 6: "Thu Jun 28 11:02:39 PDT 2012" */

        final Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(content);
        if (!m.matches() || m.groupCount() < 6) {
            if (Constants.DEBUG) {
                Log.e(TAG, "Regex does not match!");
            }
            return;
        }

        final KernelInfo info = new KernelInfo();

        info.version = m.group(1);
        if (info.version != null) {
            info.version = info.version.trim();
        }

        info.host = m.group(2);
        if (info.host != null) {
            info.host = info.host.trim();
        }

        info.toolchain = m.group(3);
        if (info.toolchain != null) {
            info.toolchain = info.toolchain.substring(1, info.toolchain.length() - 2).trim();
        }

        info.revision = m.group(4);
        if (info.revision != null) {
            info.revision = info.revision.trim();
        }

        info.extras = m.group(5);
        if (info.extras != null) {
            info.extras = info.extras.trim();
        }

        info.date = m.group(6);
        if (info.date != null) {
            info.date = info.date.trim();
        }

        if (listener != null) {
            listener.onKernelInfoAvailable(info);
        }
    }

}
