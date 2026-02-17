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

@SuppressLint("StaticFieldLeak")
object LogUtils {
    private const val TAG = "LogUtils"
    private const val FILE_NAME = "app_logs.txt"
    private const val SECURITY_LOGS = "audit_log.txt"
    private lateinit var context: Context

    fun initialize(application: Context) {
        context = application
    }

    fun d(tag: String, message: String) {
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
     * Purge log entries older than 3 days from the audit log file.
     * This prevents logs from growing indefinitely.
     */
    fun purgeOldLogs() {
        try {
            val securityLogFile = File(context.filesDir, SECURITY_LOGS)
            if (!securityLogFile.exists()) {
                return
            }

            val threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS)
            val lines = securityLogFile.readLines()
            val recentLines = mutableListOf<String>()

            for (line in lines) {
                try {
                    // Extract timestamp from log line format: "[ISO-8601 timestamp] D [TAG]: [message]"
                    val timestampStr = line.substringBefore(" ")
                    val timestamp = Instant.parse(timestampStr)
                    
                    if (timestamp.isAfter(threeDaysAgo)) {
                        recentLines.add(line)
                    }
                } catch (e: Exception) {
                    // If we can't parse the timestamp, keep the line to avoid data loss
                    recentLines.add(line)
                }
            }

            // Rewrite the file with only recent logs
            if (recentLines.size < lines.size) {
                if (recentLines.isEmpty()) {
                    // Delete the file if no recent logs remain
                    securityLogFile.delete()
                    Log.d(TAG, "Deleted log file - all entries were older than 3 days")
                } else {
                    securityLogFile.writeText(recentLines.joinToString("\n") + "\n")
                    Log.d(TAG, "Purged ${lines.size - recentLines.size} old log entries")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error purging old logs", e)
        }
    }
}
