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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.AppThemeMode

// Purple/Lavender Palette
private val PurplePrimary = Color(0xFF7B50A1)
private val PurpleOnPrimary = Color(0xFFFFFFFF)
private val PurplePrimaryContainer = Color(0xFFF3E8FF)
private val PurpleOnPrimaryContainer = Color(0xFF2D0050)
private val PurpleSecondary = Color(0xFF655A70)
private val PurpleOnSecondary = Color(0xFFFFFFFF)
private val PurpleSecondaryContainer = Color(0xFFEBDFF7)
private val PurpleOnSecondaryContainer = Color(0xFF20182B)
private val PurpleBackground = Color(0xFFFFF7FF)
private val PurpleOnBackground = Color(0xFF1D1B1F)
private val PurpleSurface = Color(0xFFFFF7FF)
private val PurpleOnSurface = Color(0xFF1D1B1F)

private val PurplePrimaryDark = Color(0xFFE1B6FF)
private val PurpleOnPrimaryDark = Color(0xFF491E6F)
private val PurplePrimaryContainerDark = Color(0xFF613787)
private val PurpleOnPrimaryContainerDark = Color(0xFFF3E8FF)
private val PurpleSecondaryDark = Color(0xFFD0C1DA)
private val PurpleOnSecondaryDark = Color(0xFF362D3F)
private val PurpleSecondaryContainerDark = Color(0xFF4D4357)
private val PurpleOnSecondaryContainerDark = Color(0xFFEBDFF7)
private val PurpleBackgroundDark = Color(0xFF1D1B1F)
private val PurpleOnBackgroundDark = Color(0xFFE6E1E6)
private val PurpleSurfaceDark = Color(0xFF1D1B1F)
private val PurpleOnSurfaceDark = Color(0xFFE6E1E6)

private val CustomLightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,
    secondary = PurpleSecondary,
    onSecondary = PurpleOnSecondary,
    secondaryContainer = PurpleSecondaryContainer,
    onSecondaryContainer = PurpleOnSecondaryContainer,
    background = PurpleBackground,
    onBackground = PurpleOnBackground,
    surface = PurpleSurface,
    onSurface = PurpleOnSurface,
    surfaceContainer = Color(0xFFF7F2FA),
    surfaceContainerLow = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
)

private val CustomDarkColorScheme = darkColorScheme(
    primary = PurplePrimaryDark,
    onPrimary = PurpleOnPrimaryDark,
    primaryContainer = PurplePrimaryContainerDark,
    onPrimaryContainer = PurpleOnPrimaryContainerDark,
    secondary = PurpleSecondaryDark,
    onSecondary = PurpleOnSecondaryDark,
    secondaryContainer = PurpleSecondaryContainerDark,
    onSecondaryContainer = PurpleOnSecondaryContainerDark,
    background = PurpleBackgroundDark,
    onBackground = PurpleOnBackgroundDark,
    surface = PurpleSurfaceDark,
    onSurface = PurpleOnSurfaceDark,
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainerHigh = Color(0xFF2B2930),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to prioritize our custom palette
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val appLockRepository = remember(context) { AppLockRepository(context) }

    val themeMode by appLockRepository.appThemeModeFlow()
        .collectAsState(initial = appLockRepository.getAppThemeMode())
    val amoledModeEnabled by appLockRepository.amoledModeFlow()
        .collectAsState(initial = appLockRepository.isAmoledModeEnabled())

    val resolvedDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> darkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !amoledModeEnabled -> {
            if (resolvedDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        resolvedDarkTheme -> {
            if (amoledModeEnabled) {
                CustomDarkColorScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212),
                    surfaceContainer = Color(0xFF0F0F0F),
                    surfaceContainerLow = Color(0xFF080808),
                    surfaceContainerHigh = Color(0xFF252525), // Increased contrast for cards
                )
            } else {
                CustomDarkColorScheme
            }
        }

        else -> CustomLightColorScheme
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
