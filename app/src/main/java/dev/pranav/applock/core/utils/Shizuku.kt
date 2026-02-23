package dev.pranav.applock.core.utils

import android.os.Process
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

val pm = SystemServiceHelper.getSystemService("package")
    .let(::ShizukuBinderWrapper)
    .let(android.content.pm.IPackageManager.Stub::asInterface)

fun blockUninstallForUser(packageName: String) {
    pm.setBlockUninstallForUser(packageName, true, Process.myUserHandle().describeContents())
}

fun unblockUninstallForUser(packageName: String) {
    pm.setBlockUninstallForUser(packageName, false, Process.myUserHandle().describeContents())
}
