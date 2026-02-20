package dev.pranav.applock.features.applist.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object AppIconCache {
    private const val MAX_CACHE_SIZE = 200

    private val iconCache = LruCache<String, ImageBitmap>(MAX_CACHE_SIZE)
    private val labelCache = LruCache<String, String>(MAX_CACHE_SIZE)

    fun getIcon(context: Context, appInfo: ApplicationInfo): ImageBitmap? {
        val cached = iconCache.get(appInfo.packageName)
        if (cached != null) return cached

        val icon = appInfo.loadIcon(context.packageManager)?.toBitmap()?.asImageBitmap()
        icon?.let { iconCache.put(appInfo.packageName, it) }
        return icon
    }

    fun getLabel(context: Context, appInfo: ApplicationInfo): String {
        val cached = labelCache.get(appInfo.packageName)
        if (cached != null) return cached

        val label = appInfo.loadLabel(context.packageManager).toString()
        labelCache.put(appInfo.packageName, label)
        return label
    }

    fun clear() {
        iconCache.evictAll()
        labelCache.evictAll()
    }
}

