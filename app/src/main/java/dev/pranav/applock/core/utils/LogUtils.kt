package dev.pranav.applock.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.concurrent.thread

@SuppressLint("StaticFieldLeak")
object LogUtils {
    private const val TAG = "LogUtils"
    private const val FILE_NAME = "app_logs.txt"
    private const val SECURITY_LOGS = "audit_log.txt"
    private lateinit var context: Context
    private var loggingEnabled = false

    fun initialize(application: Context) {
        context = application
    }

    fun setLoggingEnabled(enabled: Boolean) {
        loggingEnabled = enabled
    }

    fun d(tag: String, message: String) {
        if (!loggingEnabled) return

        val file = File(context.filesDir, SECURITY_LOGS)

        if (!file.exists()) {
            file.createNewFile()
        }

        file.appendText(Instant.now().toString() + " D " + tag + ": " + message + "\n")

        Log.d(tag, message)
    }

    fun exportAuditLogs(): Uri? {
        val file = File(context.filesDir, SECURITY_LOGS)
        return if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            null
        }
    }

    fun exportLogs(): Uri? {
        val file = File(context.cacheDir, FILE_NAME)
        try {
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()

            val process = Runtime.getRuntime().exec("logcat -d")

            process.inputStream.bufferedReader().use { reader ->
                file.writer().use { writer ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        reader.transferTo(writer)
                    } else {
                        reader.forEachLine { line ->
                            writer.write(line + "\n")
                        }
                    }
                }
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting logs", e)
            return null
        }
    }

    /**
     * Clear all security and audit logs.
     * Called when the app is updated.
     */
    fun clearAllLogs() {
        try {
            val securityLogFile = File(context.filesDir, SECURITY_LOGS)
            if (securityLogFile.exists()) {
                securityLogFile.delete()
                Log.d(TAG, "Cleared security logs")
            }
            
            val appLogFile = File(context.cacheDir, FILE_NAME)
            if (appLogFile.exists()) {
                appLogFile.delete()
                Log.d(TAG, "Cleared app logs")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logs", e)
        }
    }

    /**
     * Purge log entries older than 3 days from both audit and app log files.
     * This prevents logs from growing indefinitely.
     * Runs asynchronously to avoid blocking the main thread.
     */
    fun purgeOldLogs() {
        thread(name = "LogPurgeThread", isDaemon = true) {
            purgeOldLogsFromFile(File(context.filesDir, SECURITY_LOGS), "audit")
            purgeOldLogsFromFile(File(context.cacheDir, FILE_NAME), "app")
        }
    }

    private fun purgeOldLogsFromFile(logFile: File, logType: String) {
        try {
            if (!logFile.exists()) {
                return
            }

            val tempDir = context.cacheDir
            val tempLogFile = File(tempDir, logFile.name + ".processing")
            val backupFile = File(tempDir, logFile.name + ".backup")

            try {
                logFile.copyTo(backupFile, overwrite = true)

                val threeDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
                var purgedCount = 0
                var keptCount = 0

                backupFile.bufferedReader().use { reader ->
                    tempLogFile.bufferedWriter().use { writer ->
                        reader.forEachLine { line ->
                            try {
                                val timestampStr = line.substringBefore(" ")
                                val timestamp = Instant.parse(timestampStr)

                                if (timestamp.isAfter(threeDaysAgo)) {
                                    writer.write(line)
                                    writer.newLine()
                                    keptCount++
                                } else {
                                    purgedCount++
                                }
                            } catch (_: Exception) {
                                writer.write(line)
                                writer.newLine()
                                keptCount++
                            }
                        }
                    }
                }

                if (keptCount == 0) {
                    logFile.delete()
                    tempLogFile.delete()
                    Log.d(TAG, "Deleted $logType log file - all entries were older than 3 days")
                } else if (purgedCount > 0) {
                    tempLogFile.copyTo(logFile, overwrite = true)
                    tempLogFile.delete()
                    Log.d(TAG, "Purged $purgedCount old $logType log entries")
                } else {
                    tempLogFile.delete()
                }
            } finally {
                backupFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error purging old $logType logs", e)
        }
    }
}
