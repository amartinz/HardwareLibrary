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

package at.amartinz.hardware

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.WorkerThread
import at.amartinz.execution.Command
import at.amartinz.execution.ShellManager
import java.util.*

/**
 * @author Jared Rummler http://stackoverflow.com/a/32366476
 */
object ProcessManager {
    private val APP_ID_PATTERN: String

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Android 4.2 (JB-MR1) changed the UID name of apps for multiple user account support.
            APP_ID_PATTERN = "u\\d+_a\\d+"
        } else {
            APP_ID_PATTERN = "app_\\d+"
        }
    }

    val runningProcesses: List<Process>
        @WorkerThread get() {
            val processes = ArrayList<Process>()

            val normalShell = ShellManager.get().getNormalShell()
            if (normalShell != null) {
                val command = object : Command("toolbox ps -p -P -x -c") {
                    override fun onCommandOutput(id: Int, line: String) {
                        super.onCommandOutput(id, line)
                        try {
                            processes.add(Process(line))
                        } catch (ignored: Exception) {
                        }

                    }
                }
                normalShell.add(command)
                command.waitFor()
            }
            return processes
        }

    // skip the processes we created to get the running apps.
    val runningApps: List<Process>
        @WorkerThread get() {
            val processes = ArrayList<Process>()

            val normalShell = ShellManager.get().getNormalShell()
            if (normalShell != null) {
                val myPid = android.os.Process.myPid()
                val command = object : Command("toolbox ps -p -P -x -c") {
                    override fun onCommandOutput(id: Int, line: String) {
                        super.onCommandOutput(id, line)
                        val process: Process
                        try {
                            process = Process(line)
                        } catch (ignored: Exception) {
                            return
                        }

                        if (process.user.matches(APP_ID_PATTERN.toRegex())) {
                            if (process.ppid == myPid || process.name == "toolbox") {
                                return
                            }
                            processes.add(process)
                        }
                    }
                }
                normalShell.add(command)
                command.waitFor()
            }

            return processes
        }

    class Process : Parcelable {
        /** User name  */
        val user: String

        /** User ID  */
        val uid: Int

        /** Processes ID  */
        val pid: Int

        /** Parent processes ID  */
        val ppid: Int

        /** virtual memory size of the process in KiB (1024-byte units).  */
        val vsize: Long

        /** resident set size, the non-swapped physical memory that a task has used (in kiloBytes).  */
        val rss: Long

        val cpu: Int

        /** The priority  */
        val priority: Int

        /** The priority, [niceness](https://en.wikipedia.org/wiki/Nice_(Unix)) level  */
        val niceness: Int

        /** Real time priority  */
        val realTimePriority: Int

        /** 0 (sched_other), 1 (sched_fifo), and 2 (sched_rr).  */
        val schedulingPolicy: Int

        /** The scheduling policy. Either "bg", "fg", "un", "er", or ""  */
        val policy: String

        /** address of the kernel function where the process is sleeping  */
        val wchan: String

        val pc: String

        /**
         * Possible states:
         *
         *
         * "D" uninterruptible sleep (usually IO)
         *
         *
         * "R" running or runnable (on run queue)
         *
         *
         * "S" interruptible sleep (waiting for an event to complete)
         *
         *
         * "T" stopped, either by a job control signal or because it is being traced
         *
         *
         * "W" paging (not valid since the 2.6.xx kernel)
         *
         *
         * "X" dead (should never be seen)
         *
         * "Z" defunct ("zombie") process, terminated but not reaped by its parent
         */
        val state: String

        /** The process name  */
        val name: String

        /** user time in milliseconds  */
        val userTime: Long

        /** system time in milliseconds  */
        val systemTime: Long

        // Much dirty. Much ugly.
        @Throws(Exception::class)
        constructor(line: String) {
            val fields = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            user = fields[0]
            uid = android.os.Process.getUidForName(user)
            pid = Integer.parseInt(fields[1])
            ppid = Integer.parseInt(fields[2])
            vsize = (Integer.parseInt(fields[3]) * 1024).toLong()
            rss = (Integer.parseInt(fields[4]) * 1024).toLong()
            cpu = Integer.parseInt(fields[5])
            priority = Integer.parseInt(fields[6])
            niceness = Integer.parseInt(fields[7])
            realTimePriority = Integer.parseInt(fields[8])
            schedulingPolicy = Integer.parseInt(fields[9])
            if (fields.size == 16) {
                policy = ""
                wchan = fields[10]
                pc = fields[11]
                state = fields[12]
                name = fields[13]
                userTime = (Integer.parseInt(fields[14].split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[1].replace(",", "")) * 1000).toLong()
                systemTime = (Integer.parseInt(fields[15].split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[1].replace(")", "")) * 1000).toLong()
            } else {
                policy = fields[10]
                wchan = fields[11]
                pc = fields[12]
                state = fields[13]
                name = fields[14]
                userTime = (Integer.parseInt(fields[15].split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[1].replace(",", "")) * 1000).toLong()
                systemTime = (Integer.parseInt(fields[16].split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[1].replace(")", "")) * 1000).toLong()
            }
        }

        constructor(`in`: Parcel) {
            user = `in`.readString()
            uid = `in`.readInt()
            pid = `in`.readInt()
            ppid = `in`.readInt()
            vsize = `in`.readLong()
            rss = `in`.readLong()
            cpu = `in`.readInt()
            priority = `in`.readInt()
            niceness = `in`.readInt()
            realTimePriority = `in`.readInt()
            schedulingPolicy = `in`.readInt()
            policy = `in`.readString()
            wchan = `in`.readString()
            pc = `in`.readString()
            state = `in`.readString()
            name = `in`.readString()
            userTime = `in`.readLong()
            systemTime = `in`.readLong()
        }

        // this process is not an application
        // background service running in another process than the main app process
        val packageName: String?
            get() {
                if (!user.matches(APP_ID_PATTERN.toRegex())) {
                    return null
                } else if (name.contains(":")) {
                    return name.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[0]
                }
                return name
            }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(user)
            dest.writeInt(uid)
            dest.writeInt(pid)
            dest.writeInt(ppid)
            dest.writeLong(vsize)
            dest.writeLong(rss)
            dest.writeInt(cpu)
            dest.writeInt(priority)
            dest.writeInt(niceness)
            dest.writeInt(realTimePriority)
            dest.writeInt(schedulingPolicy)
            dest.writeString(policy)
            dest.writeString(wchan)
            dest.writeString(pc)
            dest.writeString(state)
            dest.writeString(name)
            dest.writeLong(userTime)
            dest.writeLong(systemTime)
        }

        companion object {
            val CREATOR: Parcelable.Creator<Process> = object : Parcelable.Creator<Process> {
                override fun createFromParcel(source: Parcel): Process {
                    return Process(source)
                }

                override fun newArray(size: Int): Array<Process?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

}
