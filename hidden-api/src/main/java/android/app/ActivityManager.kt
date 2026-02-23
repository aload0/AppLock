package android.app

import android.content.ComponentName

object ActivityManager {
    class RunningTaskInfo {
        fun getActivityType() = 0

        fun isVisible() = false

        val topActivity: ComponentName? = null
    }
}
