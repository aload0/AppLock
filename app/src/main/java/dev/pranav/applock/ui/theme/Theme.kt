package dev.pranav.applock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.AppThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val appLockRepository = remember(context) { AppLockRepository(context) }

    val themeMode = appLockRepository.getAppThemeMode()
    val resolvedDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> darkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val amoledModeEnabled = appLockRepository.isAmoledModeEnabled()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !amoledModeEnabled -> {
            if (resolvedDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        resolvedDarkTheme -> {
            darkColorScheme().run {
                if (amoledModeEnabled) copy(primary = Color.Black) else this
            }
        }

        else -> expressiveLightColorScheme()
    }
    val shapes = Shapes(largeIncreased = RoundedCornerShape(36.0.dp))

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content,
        motionScheme = MotionScheme.expressive()
    )
}
