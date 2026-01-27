package dev.pranav.applock.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
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
}
