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
package at.amartinz.hardware.cpu

import android.os.Handler

import java.io.File
import java.util.ArrayList

import at.amartinz.execution.Command
import at.amartinz.execution.Shell
import at.amartinz.execution.ShellManager

class CpuCoreMonitor private constructor(private val handler: Handler) {
    private val cpuCount: Int
    private val coreList = ArrayList<CpuCore>()

    private var shell: Shell? = null

    private var listener: CoreListener? = null
    private var interval: Int = 0

    private var isStarted = false

    interface CoreListener {
        fun onCores(cores: List<CpuCore>)
    }

    companion object {
        private var cpuFrequencyMonitor: CpuCoreMonitor? = null

        fun getInstance(handler: Handler): CpuCoreMonitor {
            if (cpuFrequencyMonitor == null) {
                cpuFrequencyMonitor = CpuCoreMonitor(handler)
            }
            return (cpuFrequencyMonitor as CpuCoreMonitor)
        }
    }

    init {
        this.cpuCount = CpuReader.readAvailableCores()
        getShell()

        (0..cpuCount - 1).mapTo(coreList) { CpuCore(it, "0", "0", "0") }
    }

    @JvmOverloads fun start(listener: CoreListener, interval: Int = 2000): CpuCoreMonitor {
        this.listener = listener
        this.interval = interval
        if (!isStarted) {
            isStarted = true
            handler.post(mUpdater)
        }

        return (cpuFrequencyMonitor as CpuCoreMonitor)
    }

    fun stop(): CpuCoreMonitor {
        if (isStarted) {
            isStarted = false
            listener = null
            handler.removeCallbacks(mUpdater)
        }
        return (cpuFrequencyMonitor as CpuCoreMonitor)
    }

    fun destroy() {
        stop()
        cpuFrequencyMonitor = null
    }

    private val mUpdater = Runnable { updateStates() }

    private fun getShell(): Shell? {
        if (shell == null || shell!!.isClosed || shell!!.shouldClose) {
            val shouldUseRoot = shouldUseRoot()
            if (shouldUseRoot) {
                shell = ShellManager.get().getRootShell()
            } else {
                shell = ShellManager.get().getNormalShell()
            }
        }
        return shell
    }

    private fun shouldUseRoot(): Boolean {
        (0..cpuCount - 1)
                .map {
                    arrayOf(CpuReader.getPathCoreFreqCur(it), CpuReader.getPathCoreFreqMax(it), CpuReader.getPathCoreFreqMin(it), CpuReader.getPathCoreGov(it))
                }
                .forEach { paths -> paths.filterNot { File(it).canRead() }.forEach { return true } }
        return false
    }

    private fun updateStates() {
        if (getShell() == null) {
            return
        }

        val sb = StringBuilder()
        for (i in 0..cpuCount - 1) {
            // if cpufreq directory exists ...
            sb.append("if [ -d \"/sys/devices/system/cpu/cpu").append(i.toString()).append("/cpufreq\" ]; then\n")
            // cat /path/to/cpu/frequency
            sb.append(String.format("(cat \"%s\") 2> /dev/null;\n", CpuReader.getPathCoreFreqCur(i)))
            sb.append("echo -n \" \";")
            // cat /path/to/cpu/frequency_max
            sb.append(String.format("(cat \"%s\") 2> /dev/null;\n", CpuReader.getPathCoreFreqMax(i)))
            sb.append("echo -n \" \";")
            // cat /path/to/cpu/governor
            sb.append(String.format("(cat \"%s\") 2> /dev/null;\n", CpuReader.getPathCoreGov(i)))
            // ... else echo 0 for them
            sb.append("else echo \"0 0 0\";fi;")
            // ... and append a space on the end
            sb.append("echo -n \" \";")
        }

        // example output: 162000 1890000 interactive
        val cmd = sb.toString()
        val command = object : Command(cmd) {
            override fun onCommandCompleted(id: Int, exitCode: Int) {
                super.onCommandCompleted(id, exitCode)

                var output: String = output ?: return
                output = output.replace("\n", " ")

                val parts = output.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                var mult = 0
                for (i in 0..cpuCount - 1) {
                    var cpuCore: CpuCore
                    try {
                        cpuCore = coreList[i]
                    } catch (iobe: IndexOutOfBoundsException) {
                        cpuCore = CpuCore(i, "0", "0", "0")
                    }

                    try {
                        cpuCore.setCurrent(parts[i + mult])
                                .setMax(parts[i + mult + 1])
                                .setGovernor(parts[i + mult + 2])
                    } catch (iob: IndexOutOfBoundsException) {
                        cpuCore.setCurrent("0").setMax("0").setGovernor("0")
                    }

                    mult += 2
                }

                if (listener != null) {
                    handler.post {
                        if (listener != null) {
                            listener!!.onCores(coreList)
                        }
                    }
                }

                handler.removeCallbacks(mUpdater)
                handler.postDelayed(mUpdater, interval.toLong())
            }
        }
        command.setOutputType(Command.OUTPUT_STRING)
        getShell()!!.add(command)
    }
}
