package dev.pranav.applock

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import dev.pranav.applock.core.utils.BiometricStatus
import dev.pranav.applock.core.utils.getBiometricStatus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.rememberNavController
import dev.pranav.applock.core.navigation.AppNavHost
import dev.pranav.applock.core.navigation.NavigationManager
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.ui.theme.AppLockTheme

class MainActivity : FragmentActivity() {

    private lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        navigationManager = NavigationManager(this)

        val biometricStatus = getBiometricStatus(this)
        setContent {
            AppLockTheme {
                if (biometricStatus is BiometricStatus.Unavailable) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(biometricStatus.message)
                    }
                } else {
                    val navController = rememberNavController()
                    val startDestination = navigationManager.determineStartDestination()

                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination
                    )

                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        handleOnResume(navController)
                    }
                }
            }
        }
    }

    private fun handleOnResume(navController: androidx.navigation.NavHostController) {
        val currentRoute = navController.currentDestination?.route

        if (navigationManager.shouldSkipPasswordCheck(currentRoute)) {
            return
        }

        if (currentRoute != Screen.PasswordOverlay.route && currentRoute != Screen.SetPassword.route && currentRoute != Screen.SetPasswordPattern.route && currentRoute != Screen.SetPasswordText.route) {
            navController.navigate(Screen.PasswordOverlay.route)
        }
    }
}


