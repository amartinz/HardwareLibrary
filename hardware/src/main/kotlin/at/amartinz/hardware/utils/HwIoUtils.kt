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

package at.amartinz.hardware.utils

import android.content.Context
import android.support.annotation.ArrayRes
import android.support.annotation.WorkerThread
import android.text.TextUtils
import android.util.Log

import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.net.Socket
import java.util.ArrayList
import java.util.Random

import at.amartinz.hardware.Constants
import at.amartinz.execution.Command
import at.amartinz.execution.RootCheck
import at.amartinz.execution.RootShell
import at.amartinz.execution.ShellManager
import hugo.weaving.DebugLog

object HwIoUtils {
    private val TAG = HwIoUtils::class.java.simpleName

    private val sRandom = Random(System.nanoTime())

    fun closeQuietly(o: Any?) {
        if (o is Socket) {
            try {
                o.close()
            } catch (ignored: Exception) {
            }
        } else if (o is Closeable) {
            try {
                o.close()
            } catch (ignored: Exception) {
            }
        }
    }

    interface ReadFileListener {
        fun onFileRead(path: String, content: String)
    }

    @JvmOverloads fun getPath(context: Context, @ArrayRes filePathResId: Int, prefix: String? = null): String {
        val hasPrefix = !TextUtils.isEmpty(prefix)
        val paths = context.resources.getStringArray(filePathResId)

        var basePath: String? = null
        for (pathOriginal in paths) {
            var path = pathOriginal
            if (hasPrefix) {
                path = prefix!! + path
            }
            if (HwIoUtils.fileExists(path)) {
                basePath = path
                break
            }
        }
        if (basePath.isNullOrBlank()) {
            return ""
        }
        return basePath ?: ""
    }

    fun canExecute(filePath: String?): Boolean {
        return !TextUtils.isEmpty(filePath) && File(filePath!!).canExecute()
    }

    fun canRead(filePath: String?): Boolean {
        return !TextUtils.isEmpty(filePath) && File(filePath!!).canRead()
    }

    fun canWrite(filePath: String?): Boolean {
        return !TextUtils.isEmpty(filePath) && File(filePath!!).canWrite()
    }

    fun fileExists(filePath: String?): Boolean {
        return !TextUtils.isEmpty(filePath) && File(filePath!!.trim { it <= ' ' }).exists()
    }

    fun fileExists(file: File?): Boolean {
        return file != null && file.exists()
    }

    fun fileExists(files: Array<String>): Boolean {
        return files.any { fileExists(it) }
    }

    fun checkPaths(paths: Array<String>): String? {
        return paths.firstOrNull { fileExists(it) }
    }

    fun checkPath(path: String): String? {
        if (fileExists(path)) {
            return path
        }
        return null
    }

    fun listFiles(pathToDirectory: String): List<String> {
        return listFiles(File(pathToDirectory))
    }

    fun listFiles(directory: File): List<String> {
        return listFiles(directory, false)
    }

    @WorkerThread @DebugLog fun listFiles(directory: File, withRootFallback: Boolean): List<String> {
        val files = ArrayList<String>()
        val listedFiles = directory.listFiles()
        if (listedFiles != null) {
            for (file in listedFiles) {
                files.add(file.name)
            }
        } else if (withRootFallback) {
            val rootShell = ShellManager.get().getRootShell()
            if (rootShell == null) {
                if (Constants.DEBUG) {
                    Log.w(TAG, "could not obtain root shell")
                }
            } else {
                val command = object : Command(String.format("ls %s", directory.absolutePath)) {
                    override fun onCommandOutput(id: Int, line: String) {
                        var ourLine = line
                        super.onCommandOutput(id, line)
                        ourLine = ourLine.trim { it <= ' ' }
                        if (!ourLine.isBlank()) {
                            files.add(ourLine.trim { it <= ' ' })
                        }
                    }
                }
                rootShell.add(command)
                command.waitFor()
            }
        } else if (Constants.DEBUG) {
            Log.w(TAG, "could not list files")
        }
        return files
    }

    @WorkerThread fun readSysfsIntValue(path: String): Int {
        val rawString = HwIoUtils.readFile(path)
        if (!TextUtils.isEmpty(rawString)) {
            return HwUtils.tryParseInt(rawString)
        }
        return Constants.INVALID
    }

    @WorkerThread fun readSysfsStringValue(path: String): String {
        val rawString = HwIoUtils.readFile(path)
        if (!TextUtils.isEmpty(rawString)) {
            return rawString!!.trim { it <= ' ' }
        }
        return Constants.INVALID_STR
    }

    @WorkerThread fun readStringArray(path: String): Array<String>? {
        val line = readOneLine(path)
        if (line != null) {
            return line.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        }
        return null
    }

    @WorkerThread fun readFile(path: String): String? {
        val content = readFileInternal(path, false)
        return content?.trim { it <= ' ' }
    }

    @WorkerThread fun readOneLine(path: String): String? {
        val content = readFileInternal(path, true)
        return content?.trim { it <= ' ' }
    }

    /**
     * Read one line from a file with root credentials.
     * Use blocking reading.
     * @param path of the file to read
     * *
     * @return Line read.
     */
    @WorkerThread fun readOneLineRoot(path: String): String {
        val content = readFileInternalRoot(path, true)
        return content.trim { it <= ' ' }
    }

    /**
     * Read file with root permissions.
     * @param path Path of the file to read
     * *
     * @param oneLine read only one line if true
     * *
     * @return content read
     */
    @WorkerThread private fun readFileInternalRoot(path: String, oneLine: Boolean): String {
        if (path.isNullOrBlank() || !RootCheck.isRooted()) {
            return ""
        }
        val output: String?
        if (oneLine) {
            output = RootShell.fireAndBlockStringNewline(Command(String.format("head -n 1 %s", path)))
        } else {
            output = RootShell.fireAndBlockStringNewline(Command(String.format("cat %s", path)))
        }
        return output ?: ""
    }

    @WorkerThread private fun readFileInternal(path: String, oneLine: Boolean): String? {
        val f = File(path)
        if (f.canRead()) {
            var fileReader: FileReader? = null
            var bufferedReader: BufferedReader? = null
            try {
                fileReader = FileReader(f)
                bufferedReader = BufferedReader(fileReader)
                if (oneLine) {
                    return bufferedReader.readLine()
                }
                val stringBuilder = StringBuilder()
                while (true) {
                    val line = bufferedReader.readLine() ?: break
                    stringBuilder.append(line).append('\n')
                }
                return stringBuilder.toString()
            } catch (ioe: IOException) {
                if (Constants.DEBUG) {
                    Log.e(TAG, String.format("Could not read file -> %s", path), ioe)
                }
            } finally {
                closeQuietly(bufferedReader)
                closeQuietly(fileReader)
            }
        } else if (Constants.DEBUG) {
            Log.w(TAG, String.format("Can not read file, because it is not readable -> %s", path))
        }
        return null
    }

    @WorkerThread fun readFileRoot(path: String, readFileListener: ReadFileListener?): Command? {
        if (TextUtils.isEmpty(path) || readFileListener == null || !RootCheck.isRooted()) {
            return null
        }

        val cmd = object : Command(String.format("cat %s", path)) {
            private val sb = StringBuilder()

            override fun onCommandCompleted(id: Int, exitCode: Int) {
                readFileListener.onFileRead(path, sb.toString())
                super.onCommandCompleted(id, exitCode)
            }

            override fun onCommandOutput(id: Int, line: String) {
                sb.append(line).append('\n')
                super.onCommandOutput(id, line)
            }
        }

        val rootShell = ShellManager.get().getRootShell()
        if (rootShell != null) {
            rootShell.add(cmd)
            return cmd
        }
        return null
    }

    @WorkerThread @JvmOverloads fun writeToFile(path: String, content: String, useRootAsFallback: Boolean = true): Boolean {
        return writeToFile(File(path), content, useRootAsFallback)
    }

    @WorkerThread @JvmOverloads fun writeToFile(file: File, content: String, useRootAsFallback: Boolean = true): Boolean {
        val useRoot = useRootAsFallback && !file.canWrite() && RootCheck.isRooted()
        if (useRoot) {
            if (Constants.DEBUG) {
                Log.v(TAG, String.format("writing to %s as root", file.absolutePath))
            }
            val rootShell = ShellManager.get().getRootShell()
            if (rootShell == null) {
                if (Constants.DEBUG) {
                    Log.w(TAG, "could not obtain root shell!")
                }
                return false
            }

            val id = sRandom.nextInt(10000)
            val cmd = String.format("echo \'%s\' > %s", content, file.absolutePath)
            val writeCommand = Command(cmd, id = id)
            rootShell.add(writeCommand)

            val exitCode = writeCommand.waitFor().getExitCode()
            if (Constants.DEBUG) {
                Log.v(TAG, String.format("write command \"%s\" ended with exit code -> %s", id, exitCode))
            }
            return exitCode == 0
        } else {
            if (Constants.DEBUG) {
                Log.v(TAG, String.format("writing to %s", file.absolutePath))
            }
            var fw: FileWriter? = null
            try {
                fw = FileWriter(file)
                fw.write(content)
            } catch (ioe: IOException) {
                if (Constants.DEBUG) {
                    Log.e(TAG, String.format("could not write to file %s", file.absolutePath))
                    Log.e(TAG, String.format("exists: %s | can read: %s | can write: %s",
                            file.exists(), file.canRead(), file.canWrite()), ioe)
                }
                return false
            } finally {
                closeQuietly(fw)
            }
        }
        return true
    }
}
