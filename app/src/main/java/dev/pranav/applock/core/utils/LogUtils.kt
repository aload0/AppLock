package dev.pranav.applock.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object LogUtils {
    private const val TAG = "LogUtils"
    private const val FILE_NAME = "app_logs.txt"

    fun exportLogs(context: Context): Uri? {
        val file = File(context.cacheDir, FILE_NAME)
        try {
            // Clear previous logs if file exists
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()

            // Run logcat command to get logs
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = process.inputStream.bufferedReader()
            val writer = FileWriter(file)

            // Write logs to file
            bufferedReader.use { reader ->
                writer.use { out ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        out.write(line + "\n")
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
