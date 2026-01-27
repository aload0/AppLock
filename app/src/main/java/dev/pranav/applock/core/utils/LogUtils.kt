package dev.pranav.applock.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant

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

    fun exportLogs(context: Context): Uri? {
        val file = File(context.cacheDir, FILE_NAME)
        try {
            // Clear previous logs if file exists
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()

            val pid = android.os.Process.myPid()
            val process = Runtime.getRuntime().exec("logcat -d --pid $pid")

            val excludedTags = listOf("ApkAssets", "SystemServiceRegistry")

            process.inputStream.bufferedReader().use { reader ->
                file.writer().use { writer ->
                    reader.forEachLine { line ->
                        // Check if the line contains ANY of our excluded words
                        val isNoise = excludedTags.any { line.contains(it) }

                        if (!isNoise) {
                            writer.write(line + "\n")
                        }
                    }
                }
            }

            // Return Uri using FileProvider
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
}
