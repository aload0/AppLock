package dev.pranav.applock.core.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.security.SecureRandom

object RecoveryKeyManager {
    private const val APP_FOLDER = "APP Lock by AP"
    private const val FILE_PREFIX = "recovery-key-"
    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val random = SecureRandom()

    fun generateRecoveryKey(length: Int = 64): String = buildString(length) {
        repeat(length) {
            append(CHARSET[random.nextInt(CHARSET.length)])
        }
    }

    fun saveRecoveryKeyToDownloads(context: Context, recoveryKey: String): Result<String> = runCatching {
        val fileName = "$FILE_PREFIX${System.currentTimeMillis()}.txt"
        val body = "Recovery key for APP Lock by AP\n\n$recoveryKey\n"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$APP_FOLDER")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create download file")
            resolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(body) }
                ?: error("Could not open download output stream")
        } else {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads, APP_FOLDER).apply { mkdirs() }
            File(dir, fileName).writeText(body)
        }
        "Downloads/$APP_FOLDER/$fileName"
    }
}
