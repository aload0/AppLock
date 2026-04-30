package android.app

import android.Manifest
import android.app.ActivityManager.RunningAppProcessInfo.Importance
import android.app.WindowConfiguration.ActivityType
import android.app.WindowConfiguration.WindowingMode
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ActivityInfo
import android.content.pm.ConfigurationInfo
import android.content.pm.IPackageDataObserver
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.*
import androidx.annotation.*
import androidx.annotation.IntRange
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.Locale
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * 
 * 
 * This class gives information about, and interacts
 * with, activities, services, and the containing
 * process.
 * 
 * 
 * 
 * 
 * A number of the methods in this class are for
 * debugging or informational purposes and they should
 * not be used to affect any runtime behavior of
 * your app. These methods are called out as such in
 * the method level documentation.
 * 
 * 
 * 
 * 
 * Most application developers should not have the need to
 * use this class, most of whose methods are for specialized
 * use cases. However, a few methods are more broadly applicable.
 * For instance, isLowRamDevice()
 * enables your app to detect whether it is running on a low-memory device,
 * and behave accordingly.
 * clearApplicationUserData()
 * is for apps with reset-data functionality.
 * 
 * 
 * 
 * 
 * In some special use cases, where an app interacts with
 * its Task stack, the app may use the
 * AppTask and
 * RecentTaskInfo inner
 * classes. However, in general, the methods in this class should
 * be used for testing and debugging purposes only.
 * 
 */
class ActivityManager {
    val memoryClass: Int
        get() = 0

    val largeMemoryClass: Int
        get() = 0

    val isLowRamDevice: Boolean
        get() = false

    val totalRam: Long
        get() = 0

    var frontActivityScreenCompatMode: Int
        get() = 0
        set(mode) {
        }

    fun getPackageScreenCompatMode(packageName: String?): Int {
        return 0
    }

    fun setPackageScreenCompatMode(packageName: String?, mode: Int) {
    }

    fun getPackageAskScreenCompat(packageName: String?): Boolean {
        return false
    }

    fun setPackageAskScreenCompat(packageName: String?, ask: Boolean) {
    }

    fun getMemoryInfo(outInfo: MemoryInfo?) {
    }

    fun clearApplicationUserData(packageName: String?, observer: IPackageDataObserver?): Boolean {
        return false
    }

    fun clearApplicationUserData(): Boolean {
        return false
    }

    @Deprecated("")
    fun clearGrantedUriPermissions(packageName: String?) {
    }

    @Deprecated("")
    @Throws(SecurityException::class)
    fun getRecentTasks(maxNum: Int, flags: Int): MutableList<RecentTaskInfo?>? {
        return null
    }

    val appTasks: MutableList<AppTask?>?
        get() = null

    val appTaskThumbnailSize: Size?
        get() = null

    fun addAppTask(
        activity: Activity,
        intent: Intent,
        description: TaskDescription?,
        thumbnail: Bitmap
    ): Int {
        return 0
    }

    @Deprecated("")
    @Throws(SecurityException::class)
    fun getRunningTasks(maxNum: Int): MutableList<RunningTaskInfo?>? {
        return null
    }

    @RequiresPermission(Manifest.permission.REORDER_TASKS)
    fun moveTaskToFront(taskId: Int, @MoveTaskFlags flags: Int) {
    }

    @RequiresPermission(Manifest.permission.REORDER_TASKS)
    fun moveTaskToFront(taskId: Int, @MoveTaskFlags flags: Int, options: Bundle?) {
    }

    fun isActivityStartAllowedOnDisplay(context: Context, displayId: Int, intent: Intent): Boolean {
        return false
    }

    @Deprecated("")
    @Throws(SecurityException::class)
    fun getRunningServices(maxNum: Int): List<RunningServiceInfo> {
        return emptyList()
    }

    @Throws(SecurityException::class)
    fun getRunningServiceControlPanel(service: ComponentName?): PendingIntent? {
        return null
    }

    fun getRunningServiceConnections(service: ComponentName): MutableList<ConnectionInfo>? {
        return null
    }

    val isBackgroundRestricted: Boolean
        get() = false

    fun setProcessMemoryTrimLevel(process: String?, userId: Int, level: Int): Boolean {
        return false
    }

    val runningAppProcesses: MutableList<RunningAppProcessInfo?>?
        get() = null

    fun getHistoricalProcessStartReasons(@IntRange(from = 0) maxNum: Int): MutableList<ApplicationStartInfo?>? {
        return null
    }

    fun getExternalHistoricalProcessStartReasons(
        packageName: String,
        @IntRange(from = 0) maxNum: Int
    ): MutableList<ApplicationStartInfo?>? {
        return null
    }

    fun addApplicationStartInfoCompletionListener(
        executor: Executor,
        listener: Consumer<ApplicationStartInfo?>
    ) {
    }

    fun removeApplicationStartInfoCompletionListener(listener: Consumer<ApplicationStartInfo?>) {
    }

    fun addStartInfoTimestamp(
        @IntRange(
            from = ApplicationStartInfo.START_TIMESTAMP_RESERVED_RANGE_DEVELOPER_START.toLong(),
            to = ApplicationStartInfo.START_TIMESTAMP_RESERVED_RANGE_DEVELOPER.toLong()
        ) key: Int, timestampNs: Long
    ) {
    }

    fun getHistoricalProcessExitReasons(
        packageName: String?,
        @IntRange(from = 0) pid: Int,
        @IntRange(from = 0) maxNum: Int
    ): MutableList<ApplicationExitInfo?>? {
        return null
    }

    fun setProcessStateSummary(state: ByteArray?) {
    }

    fun getUidProcessState(uid: Int): Int {
        return 0
    }

    @ProcessCapability
    fun getUidProcessCapabilities(uid: Int): Int {
        return 0
    }

    @Importance
    fun getPackageImportance(packageName: String?): Int {
        return RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    @Importance
    fun getUidImportance(uid: Int): Int {
        return RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    @Importance
    fun getBindingUidImportance(uid: Int): Int {
        return RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    fun addOnUidImportanceListener(
        listener: OnUidImportanceListener?,
        @Importance importanceCutpoint: Int
    ) {
    }

    fun addOnUidImportanceListener(
        listener: OnUidImportanceListener,
        @Importance importanceCutpoint: Int,
        uids: IntArray
    ) {
    }

    fun removeOnUidImportanceListener(listener: OnUidImportanceListener?) {
    }

    fun getProcessMemoryInfo(pids: IntArray?): Array<Debug.MemoryInfo?>? {
        return null
    }

    @Deprecated("")
    fun restartPackage(packageName: String?) {
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    fun killBackgroundProcesses(packageName: String?) {
    }

    fun killUid(uid: Int, reason: String?) {
    }

    fun forceStopPackageAsUser(packageName: String?, userId: Int) {
    }

    fun forceStopPackage(packageName: String?) {
    }

    fun forceStopPackageAsUserEvenWhenStopping(packageName: String?, userId: Int) {
    }

    fun stopPackageForUser(packageName: String?) {
    }

    fun setDeviceLocales(locales: LocaleList) {
    }

    val supportedLocales: MutableCollection<Locale>?
        get() = null

    val deviceConfigurationInfo: ConfigurationInfo?
        get() = null

    val launcherLargeIconDensity: Int
        get() = 0

    val launcherLargeIconSize: Int
        get() = 0

    fun alwaysShowUnsupportedCompileSdkWarning(activity: ComponentName?) {
    }

    fun switchUser(userid: Int): Boolean {
        return false
    }

    fun switchUser(user: UserHandle): Boolean {
        return false
    }

    fun logoutUser(userId: Int): Boolean {
        return false
    }

    fun startUserInBackgroundVisibleOnDisplay(userId: Int, displayId: Int): Boolean {
        return false
    }

    val displayIdsForStartingVisibleBackgroundUsers: IntArray?
        get() = null

    fun getSwitchingFromUserMessage(userId: Int): String? {
        return null
    }

    fun getSwitchingToUserMessage(userId: Int): String? {
        return null
    }

    fun setStopUserOnSwitch(@StopUserOnSwitch value: Int) {
    }

    fun startProfile(userHandle: UserHandle): Boolean {
        return false
    }

    fun stopProfile(userHandle: UserHandle): Boolean {
        return false
    }

    fun updateMccMncConfiguration(mcc: String, mnc: String): Boolean {
        return false
    }

    fun stopUser(userId: Int): Boolean {
        return false
    }

    fun isUserRunning(userId: Int): Boolean {
        return false
    }

    fun isVrModePackageEnabled(component: ComponentName?): Boolean {
        return false
    }

    @RequiresPermission(Manifest.permission.DUMP)
    fun dumpPackageState(fd: FileDescriptor?, packageName: String?) {
    }

    fun setWatchHeapLimit(pssSize: Long) {
    }

    fun clearWatchHeapLimit() {
    }

    @get:Deprecated("")
    val isInLockTaskMode: Boolean
        get() = false

    val lockTaskModeState: Int
        get() = 0

    fun scheduleApplicationInfoChanged(packages: MutableList<String?>?, userId: Int) {
    }

    fun isProfileForeground(userHandle: UserHandle): Boolean {
        return false
    }

    fun killProcessesWhenImperceptible(pids: IntArray, reason: String) {
    }

    val bugreportWhitelistedPackages: MutableList<String?>?
        get() = null

    fun appNotResponding(reason: String) {
    }

    fun addHomeVisibilityListener(executor: Executor, listener: HomeVisibilityListener) {
    }

    fun removeHomeVisibilityListener(listener: HomeVisibilityListener) {
    }

    fun setThemeOverlayReady(userId: Int) {
    }

    fun resetAppErrors() {
    }

    fun holdLock(token: IBinder?, durationMs: Int) {
    }

    fun waitForBroadcastIdle() {
    }

    fun forceDelayBroadcastDelivery(
        targetPackage: String,
        @IntRange(from = 0) delayedDurationMs: Long
    ) {
    }

    fun isProcessFrozen(pid: Int): Boolean {
        return false
    }

    @Throws(SecurityException::class)
    fun noteForegroundResourceUseBegin(@ForegroundServiceApiType apiType: Int, uid: Int, pid: Int) {
    }

    @Throws(SecurityException::class)
    fun noteForegroundResourceUseEnd(@ForegroundServiceApiType apiType: Int, uid: Int, pid: Int) {
    }

    fun getBackgroundRestrictionExemptionReason(uid: Int): Int {
        return 0
    }

    fun noteAppRestrictionEnabled(
        packageName: String,
        uid: Int,
        @RestrictionLevel restrictionLevel: Int,
        enabled: Boolean,
        @RestrictionReason reason: Int,
        subReason: String?,
        @RestrictionSource source: Int,
        threshold: Long
    ) {
    }

    fun notifySystemPropertiesChanged() {
    }

    /**
     * Process states, describing the kind of state a particular process is in.
     */
    @IntDef(
        flag = false,
        value = [PROCESS_STATE_UNKNOWN, PROCESS_STATE_PERSISTENT, PROCESS_STATE_PERSISTENT_UI, PROCESS_STATE_TOP, PROCESS_STATE_BOUND_TOP, PROCESS_STATE_FOREGROUND_SERVICE, PROCESS_STATE_BOUND_FOREGROUND_SERVICE, PROCESS_STATE_IMPORTANT_FOREGROUND, PROCESS_STATE_IMPORTANT_BACKGROUND, PROCESS_STATE_TRANSIENT_BACKGROUND, PROCESS_STATE_BACKUP, PROCESS_STATE_SERVICE, PROCESS_STATE_RECEIVER, PROCESS_STATE_TOP_SLEEPING, PROCESS_STATE_HEAVY_WEIGHT, PROCESS_STATE_HOME, PROCESS_STATE_LAST_ACTIVITY, PROCESS_STATE_CACHED_ACTIVITY, PROCESS_STATE_CACHED_ACTIVITY_CLIENT, PROCESS_STATE_CACHED_RECENT, PROCESS_STATE_CACHED_EMPTY
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ProcessState

    /**
     * The set of flags for process capability.
     */
    //@IntDef(
    //    flag = true,
    //    value = [PROCESS_CAPABILITY_NONE, PROCESS_CAPABILITY_FOREGROUND_LOCATION, PROCESS_CAPABILITY_FOREGROUND_CAMERA, PROCESS_CAPABILITY_FOREGROUND_MICROPHONE, PROCESS_CAPABILITY_POWER_RESTRICTED_NETWORK, PROCESS_CAPABILITY_BFSL, PROCESS_CAPABILITY_USER_RESTRICTED_NETWORK, PROCESS_CAPABILITY_FOREGROUND_AUDIO_CONTROL, PROCESS_CAPABILITY_CPU_TIME, PROCESS_CAPABILITY_IMPLICIT_CPU_TIME
    //    ]
    //)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ProcessCapability

    /**
     * Constants used to denote what API type is creating an API event for logging.
     */
    @IntDef(
        flag = false,
        value = [FOREGROUND_SERVICE_API_TYPE_CAMERA, FOREGROUND_SERVICE_API_TYPE_BLUETOOTH, FOREGROUND_SERVICE_API_TYPE_LOCATION, FOREGROUND_SERVICE_API_TYPE_MEDIA_PLAYBACK, FOREGROUND_SERVICE_API_TYPE_AUDIO, FOREGROUND_SERVICE_API_TYPE_MICROPHONE, FOREGROUND_SERVICE_API_TYPE_PHONE_CALL, FOREGROUND_SERVICE_API_TYPE_USB, FOREGROUND_SERVICE_API_TYPE_CDM
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ForegroundServiceApiType

    @IntDef(
        flag = false, value = [FOREGROUND_SERVICE_API_EVENT_BEGIN, FOREGROUND_SERVICE_API_EVENT_END
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ForegroundServiceApiEvent

    /**
     * Restriction level constants.
     */
    @IntDef(
        value = [RESTRICTION_LEVEL_UNKNOWN, RESTRICTION_LEVEL_UNRESTRICTED, RESTRICTION_LEVEL_EXEMPTED, RESTRICTION_LEVEL_ADAPTIVE_BUCKET, RESTRICTION_LEVEL_RESTRICTED_BUCKET, RESTRICTION_LEVEL_BACKGROUND_RESTRICTED, RESTRICTION_LEVEL_FORCE_STOPPED, RESTRICTION_LEVEL_USER_LAUNCH_ONLY, RESTRICTION_LEVEL_CUSTOM, RESTRICTION_LEVEL_MAX
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class RestrictionLevel

    @IntDef(
        value = [RESTRICTION_REASON_DEFAULT, RESTRICTION_REASON_DORMANT, RESTRICTION_REASON_USAGE, RESTRICTION_REASON_USER, RESTRICTION_REASON_SYSTEM_HEALTH, RESTRICTION_REASON_POLICY, RESTRICTION_REASON_OTHER
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class RestrictionReason

    @IntDef(
        value = [RESTRICTION_SOURCE_USER, RESTRICTION_SOURCE_USER_NUDGED, RESTRICTION_SOURCE_SYSTEM, RESTRICTION_SOURCE_COMMAND_LINE, RESTRICTION_SOURCE_REMOTE_TRIGGER
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class RestrictionSource

    @IntDef(
        flag = true, value = [MOVE_TASK_WITH_HOME, MOVE_TASK_NO_USER_ACTION
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class MoveTaskFlags

    //@IntDef(
    //    value = [STOP_USER_ON_SWITCH_DEFAULT, STOP_USER_ON_SWITCH_TRUE, STOP_USER_ON_SWITCH_FALSE
    //    ]
    //)
    @Retention(AnnotationRetention.SOURCE)
    annotation class StopUserOnSwitch

    /**
     * Callback to get reports about changes to the importance of a uid.
     */
    interface OnUidImportanceListener {
        fun onUidImportance(uid: Int, @Importance importance: Int)
    }

    /**
     * Information you can retrieve about a particular Task.
     */
    open class TaskInfo {
        val configuration: Configuration = Configuration()
        var userId: Int = 0
        var taskId: Int = 0
        var effectiveUid: Int = 0
        var isRunning: Boolean = false
        var baseIntent: Intent? = null
        var baseActivity: ComponentName? = null
        var topActivity: ComponentName? = null
        var origActivity: ComponentName? = null
        var realActivity: ComponentName? = null
        var numActivities: Int = 0
        var lastActiveTime: Long = 0
        var displayId: Int = 0
        var displayAreaFeatureId: Int = 0
        var taskDescription: TaskDescription? = null
        var mTopActivityLocusId: LocusId? = null
        var supportsMultiWindow: Boolean = false
        var resizeMode: Int = 0
        var pictureInPictureParams: PictureInPictureParams? = null
        var shouldDockBigOverlays: Boolean = false
        var launchIntoPipHostTaskId: Int = 0
        var lastParentTaskIdBeforePip: Int = 0
        var displayCutoutInsets: Rect? = null

        @ActivityType
        var topActivityType: Int = 0
        var topActivityInfo: ActivityInfo? = null
        var isResizeable: Boolean = false
        var minWidth: Int = 0
        var minHeight: Int = 0
        var defaultMinSize: Int = 0
        var positionInParent: Point? = null
        var launchCookies: ArrayList<IBinder?> = ArrayList<IBinder?>()
        var parentTaskId: Int = 0
        var isFocused: Boolean = false
        var isVisible: Boolean = false
        var isVisibleRequested: Boolean = false
        var isTopActivityNoDisplay: Boolean = false
        var isSleeping: Boolean = false
        var isTopActivityTransparent: Boolean = false
        var isActivityStackTransparent: Boolean = false
        var lastNonFullscreenBounds: Rect? = null
        var capturedLink: Uri? = null
        var capturedLinkTimestamp: Long = 0
        var requestedVisibleTypes: Int = 0
        var topActivityRequestOpenInBrowserEducationTimestamp: Long = 0
        var isAppBubble: Boolean = false
        var topActivityMainWindowFrame: Rect? = null
        var isInteractivePictureInPicture: Boolean = false

        fun shouldDockBigOverlays(): Boolean {
            return false
        }

        @get:WindowingMode
        val windowingMode: Int
            get() = WindowConfiguration.WINDOWING_MODE_UNDEFINED

        @get:ActivityType
        val activityType: Int
            get() = WindowConfiguration.ACTIVITY_TYPE_UNDEFINED

        fun addLaunchCookie(cookie: IBinder?) {
        }

        fun containsLaunchCookie(cookie: IBinder): Boolean {
            return false
        }

        fun hasParentTask(): Boolean {
            return false
        }

        fun equalsForTaskOrganizer(that: TaskInfo?): Boolean {
            return false
        }

        fun equalsForCompatUi(that: TaskInfo?): Boolean {
            return false
        }

        override fun toString(): String {
            return "TaskInfo{...}"
        }
    }

    /**
     * Information you can retrieve about tasks that the user has most recently started or visited.
     */
    class RecentTaskInfo: TaskInfo() {
        @Deprecated("")
        var id: Int = 0

        @Deprecated("")
        var persistentId: Int = 0

        @Deprecated("")
        var description: CharSequence? = null

        @Deprecated("")
        var affiliatedTaskId: Int = 0

        fun dump(pw: PrintWriter?, indent: String?) {
        }
    }

    /**
     * Information you can retrieve about a particular task that is currently "running" in the system.
     */
    class RunningTaskInfo: TaskInfo() {
        @Deprecated("")
        var id: Int = 0

        @Deprecated("")
        var thumbnail: Bitmap? = null

        @Deprecated("")
        var description: CharSequence? = null

        @Deprecated("")
        var numRunning: Int = 0

        fun isFreeform(): Boolean = false
    }

    /**
     * Information you can retrieve about a particular Service that is currently running in the system.
     */
    class RunningServiceInfo {
        var service: ComponentName
        var pid: Int = 0
        var uid: Int = 0
        var process: String? = null
        var foreground: Boolean = false
        var activeSince: Long = 0
        var started: Boolean = false
        var clientCount: Int = 0
        var crashCount: Int = 0
        var lastActivityTime: Long = 0
        var restarting: Long = 0
        var flags: Int = 0
        var clientPackage: String? = null
        var clientLabel: Int = 0

        companion object {
            val FLAG_STARTED: Int = 1 shl 0
            val FLAG_FOREGROUND: Int = 1 shl 1
            val FLAG_SYSTEM_PROCESS: Int = 1 shl 2
            val FLAG_PERSISTENT_PROCESS: Int = 1 shl 3
        }
    }

    /**
     * Information you can retrieve about a particular connection to a Service.
     */
    class ConnectionInfo {
        val flags: Long
            get() = 0

        val processName: String
            get() = ""

        val packageName: String
            get() = ""
    }

    /**
     * Information you can retrieve about the available memory.
     */
    class MemoryInfo {
        var advertisedMem: Long = 0
        var availMem: Long = 0
        var freeMem: Long = 0
        var totalMem: Long = 0
        var threshold: Long = 0
        var lowMemory: Boolean = false
        var hiddenAppThreshold: Long = 0
        var secondaryServerThreshold: Long = 0
        var visibleAppThreshold: Long = 0
        var foregroundAppThreshold: Long = 0

        fun copyTo(other: MemoryInfo?) {
        }
    }

    /**
     * Information you can retrieve about any processes that are in an error condition.
     */
    class ProcessErrorStateInfo {
        var condition: Int = 0
        var processName: String? = null
        var pid: Int = 0
        var uid: Int = 0
        var tag: String? = null
        var shortMsg: String? = null
        var longMsg: String? = null
        var stackTrace: String? = null

        companion object {
            const val NO_ERROR: Int = 0
            const val CRASHED: Int = 1
            const val NOT_RESPONDING: Int = 2
        }
    }

    /**
     * Information you can retrieve about a running process.
     */
    class RunningAppProcessInfo {
        var processName: String? = null
        var pid: Int = 0
        var uid: Int = 0
        var pkgList: Array<String>? = null
        var pkgDeps: Array<String>? = null
        var flags: Int = 0
        var lastTrimLevel: Int = 0

        @Importance
        var importance: Int = 0
        var lru: Int = 0
        var importanceReasonCode: Int = 0
        var importanceReasonPid: Int = 0
        var importanceReasonComponent: ComponentName? = null
        var importanceReasonImportance: Int = 0
        var processState: Int = 0
        var isFocused: Boolean = false
        var lastActivityTime: Long = 0

        constructor()
        constructor(pProcessName: String?, pPid: Int, pArr: Array<String?>?)

        fun copyTo(other: RunningAppProcessInfo?) {
        }

        @IntDef(
            value = [IMPORTANCE_FOREGROUND, IMPORTANCE_FOREGROUND_SERVICE, IMPORTANCE_TOP_SLEEPING, IMPORTANCE_VISIBLE, IMPORTANCE_PERCEPTIBLE, IMPORTANCE_CANT_SAVE_STATE, IMPORTANCE_SERVICE, IMPORTANCE_CACHED, IMPORTANCE_GONE
            ]
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class Importance
        companion object {
            const val IMPORTANCE_FOREGROUND: Int = 100
            const val IMPORTANCE_FOREGROUND_SERVICE: Int = 125

            @Deprecated("")
            const val IMPORTANCE_TOP_SLEEPING_PRE_28: Int = 150
            const val IMPORTANCE_VISIBLE: Int = 200
            const val IMPORTANCE_PERCEPTIBLE_PRE_26: Int = 130
            const val IMPORTANCE_PERCEPTIBLE: Int = 230
            const val IMPORTANCE_CANT_SAVE_STATE_PRE_26: Int = 170
            const val IMPORTANCE_SERVICE: Int = 300
            const val IMPORTANCE_TOP_SLEEPING: Int = 325
            const val IMPORTANCE_CANT_SAVE_STATE: Int = 350
            const val IMPORTANCE_CACHED: Int = 400
            val IMPORTANCE_BACKGROUND: Int = IMPORTANCE_CACHED

            @Deprecated("")
            const val IMPORTANCE_EMPTY: Int = 500
            const val IMPORTANCE_GONE: Int = 1000
            const val REASON_UNKNOWN: Int = 0
            const val REASON_PROVIDER_IN_USE: Int = 1
            const val REASON_SERVICE_IN_USE: Int = 2

            @Importance
            fun procStateToImportance(procState: Int): Int {
                return IMPORTANCE_FOREGROUND
            }

            @Importance
            fun procStateToImportanceForClient(procState: Int, clientContext: Context?): Int {
                return IMPORTANCE_BACKGROUND
            }

            @Importance
            fun procStateToImportanceForTargetSdk(procState: Int, targetSdkVersion: Int): Int {
                return IMPORTANCE_FOREGROUND
            }

            fun importanceToProcState(@Importance importance: Int): Int {
                return 0
            }
        }
    }

    /**
     * Information you can set and retrieve about the current activity within the recent task list.
     */
    class TaskDescription {
        @Deprecated("")
        constructor(label: String?, @DrawableRes iconRes: Int, colorPrimary: Int)

        @Deprecated("")
        constructor(label: String?, @DrawableRes iconRes: Int)

        @Deprecated("")
        constructor(label: String?)

        @Deprecated("")
        constructor()

        @Deprecated("")
        constructor(label: String?, icon: Bitmap?, colorPrimary: Int)

        @Deprecated("")
        constructor(label: String?, icon: Bitmap?)

        constructor(
            label: String?, icon: Icon?, colorPrimary: Int, colorBackground: Int,
            statusBarColor: Int, navigationBarColor: Int, systemBarsAppearance: Int,
            topOpaqueSystemBarsAppearance: Int, ensureStatusBarContrastWhenTransparent: Boolean,
            ensureNavigationBarContrastWhenTransparent: Boolean, resizeMode: Int, minWidth: Int,
            minHeight: Int, colorBackgroundFloating: Int
        )

        constructor(td: TaskDescription?)

        fun copyFrom(other: TaskDescription?) {
        }

        fun copyFromPreserveHiddenFields(other: TaskDescription?) {
        }

        var label: String?
            get() = null
            set(label) {
            }

        fun loadIcon(): Icon? {
            return null
        }

        @get:Deprecated("")
        val icon: Bitmap?
            get() = null

        fun setIcon(icon: Icon?) {
        }

        val rawIcon: Icon?
            get() = null

        val iconResourcePackage: String?
            get() = null

        val iconResource: Int
            get() = 0

        var iconFilename: String?
            get() = null
            set(iconFilename) {
            }

        val inMemoryIcon: Bitmap?
            get() = null

        @get:ColorInt
        var primaryColor: Int
            get() = 0
            set(primaryColor) {
            }

        @get:ColorInt
        var backgroundColor: Int
            get() = 0
            set(backgroundColor) {
            }

        var backgroundColorFloating: Int
            get() = 0
            set(backgroundColor) {
            }

        @get:ColorInt
        var statusBarColor: Int
            get() = 0
            set(statusBarColor) {
            }

        @get:ColorInt
        var navigationBarColor: Int
            get() = 0
            set(navigationBarColor) {
            }

        var ensureStatusBarContrastWhenTransparent: Boolean
            get() = false
            set(ensureStatusBarContrastWhenTransparent) {
            }

        var systemBarsAppearance: Int
            get() = 0
            set(systemBarsAppearance) {
            }

        var topOpaqueSystemBarsAppearance: Int
            get() = 0
            set(topOpaqueSystemBarsAppearance) {
            }

        var ensureNavigationBarContrastWhenTransparent: Boolean
            get() = false
            set(ensureNavigationBarContrastWhenTransparent) {
            }

        var resizeMode: Int
            get() = 0
            set(resizeMode) {
            }

        var minWidth: Int
            get() = 0
            set(minWidth) {
            }

        var minHeight: Int
            get() = 0
            set(minHeight) {
            }

        override fun hashCode(): Int {
            return 0
        }

        override fun equals(obj: Any?): Boolean {
            return false
        }

        class Builder {
            fun setLabel(label: String?): Builder {
                return this
            }

            fun setIcon(@DrawableRes iconRes: Int): Builder {
                return this
            }

            fun setPrimaryColor(@ColorInt color: Int): Builder {
                return this
            }

            fun setBackgroundColor(@ColorInt color: Int): Builder {
                return this
            }

            fun setStatusBarColor(@ColorInt color: Int): Builder {
                return this
            }

            fun setNavigationBarColor(@ColorInt color: Int): Builder {
                return this
            }

            fun build(): TaskDescription {
                return TaskDescription()
            }
        }

        companion object {
            const val ATTR_TASKDESCRIPTION_PREFIX: String = "task_description_"

            fun equals(td1: TaskDescription?, td2: TaskDescription?): Boolean {
                return false
            }
        }
    }

    /**
     * The AppTask allows you to manage your own application's tasks.
     */
    class AppTask {
        fun finishAndRemoveTask() {
        }

        val taskInfo: RecentTaskInfo?
            get() = null

        fun moveToFront() {
        }

        fun startActivity(context: Context?, intent: Intent?, options: Bundle?) {
        }

        fun setExcludeFromRecents(exclude: Boolean) {
        }

        @IntDef(
            value = [WINDOWING_LAYER_UNDEFINED, WINDOWING_LAYER_NORMAL_APP, WINDOWING_LAYER_PINNED
            ]
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class WindowingLayer

        companion object {
            const val WINDOWING_LAYER_UNDEFINED: Int = 0
            const val WINDOWING_LAYER_NORMAL_APP: Int = 1
            const val WINDOWING_LAYER_PINNED: Int = 2
        }
    }

    /**
     * Callback for home visibility changes.
     */
    abstract class HomeVisibilityListener {
        abstract fun onHomeVisibilityChanged(isVisible: Boolean)
    }

    /**
     * Subset of immutable pending intent information.
     */
    class PendingIntentInfo(
        creatorPackage: String?,
        creatorUid: Int,
        immutable: Boolean,
        intentSenderType: Int
    ) {
        val creatorPackage: String?
            get() = null

        val creatorUid: Int
            get() = 0

        val isImmutable: Boolean
            get() = false

        val intentSenderType: Int
            get() = 0
    }

    companion object {
        /**
         * Disable hidden API checks for the newly started instrumentation.
         */
        val INSTR_FLAG_DISABLE_HIDDEN_API_CHECKS: Int = 1 shl 0
        val INSTR_FLAG_DISABLE_ISOLATED_STORAGE: Int = 1 shl 1
        val INSTR_FLAG_DISABLE_TEST_API_CHECKS: Int = 1 shl 2
        val INSTR_FLAG_NO_RESTART: Int = 1 shl 3
        val INSTR_FLAG_ALWAYS_CHECK_SIGNATURE: Int = 1 shl 4
        val INSTR_FLAG_INSTRUMENT_SDK_SANDBOX: Int = 1 shl 5
        val INSTR_FLAG_INSTRUMENT_SDK_IN_SANDBOX: Int = 1 shl 6

        /**
         * Intent ACTION_CLOSE_SYSTEM_DIALOGS is too powerful to be unrestricted.
         */
        const val DROP_CLOSE_SYSTEM_DIALOGS: Long = 174664120L
        const val LOCK_DOWN_CLOSE_SYSTEM_DIALOGS: Long = 174664365L
        const val PROCESS_STATE_UNKNOWN: Int = -1
        const val PROCESS_STATE_PERSISTENT: Int = 0
        const val PROCESS_STATE_PERSISTENT_UI: Int = 1
        const val PROCESS_STATE_TOP: Int = 2
        const val PROCESS_STATE_BOUND_TOP: Int = 3
        const val PROCESS_STATE_FOREGROUND_SERVICE: Int = 4
        const val PROCESS_STATE_BOUND_FOREGROUND_SERVICE: Int = 5
        const val PROCESS_STATE_IMPORTANT_FOREGROUND: Int = 6
        const val PROCESS_STATE_IMPORTANT_BACKGROUND: Int = 7
        const val PROCESS_STATE_TRANSIENT_BACKGROUND: Int = 8
        const val PROCESS_STATE_BACKUP: Int = 9
        const val PROCESS_STATE_SERVICE: Int = 10
        const val PROCESS_STATE_RECEIVER: Int = 11
        const val PROCESS_STATE_TOP_SLEEPING: Int = 12
        const val PROCESS_STATE_HEAVY_WEIGHT: Int = 13
        const val PROCESS_STATE_HOME: Int = 14
        const val PROCESS_STATE_LAST_ACTIVITY: Int = 15
        const val PROCESS_STATE_CACHED_ACTIVITY: Int = 16
        const val PROCESS_STATE_CACHED_ACTIVITY_CLIENT: Int = 17
        const val PROCESS_STATE_CACHED_RECENT: Int = 18
        const val PROCESS_STATE_CACHED_EMPTY: Int = 19
        const val PROCESS_STATE_NONEXISTENT: Int = 20
        val MIN_PROCESS_STATE: Int = PROCESS_STATE_PERSISTENT
        val MAX_PROCESS_STATE: Int = PROCESS_STATE_NONEXISTENT
        const val PROCESS_CAPABILITY_NONE: Int = 0
        val PROCESS_CAPABILITY_FOREGROUND_LOCATION: Int = 1 shl 0
        val PROCESS_CAPABILITY_FOREGROUND_CAMERA: Int = 1 shl 1
        val PROCESS_CAPABILITY_FOREGROUND_MICROPHONE: Int = 1 shl 2
        val PROCESS_CAPABILITY_POWER_RESTRICTED_NETWORK: Int = 1 shl 3
        val PROCESS_CAPABILITY_BFSL: Int = 1 shl 4
        val PROCESS_CAPABILITY_USER_RESTRICTED_NETWORK: Int = 1 shl 5
        val PROCESS_CAPABILITY_FOREGROUND_AUDIO_CONTROL: Int = 1 shl 6
        val PROCESS_CAPABILITY_CPU_TIME: Int = 1 shl 7
        val PROCESS_CAPABILITY_IMPLICIT_CPU_TIME: Int = 1 shl 8
        val PROCESS_CAPABILITY_ALL: Int = -1
        val PROCESS_CAPABILITY_ALL_IMPLICIT: Int =
            PROCESS_CAPABILITY_FOREGROUND_CAMERA or PROCESS_CAPABILITY_FOREGROUND_MICROPHONE
        const val FOREGROUND_SERVICE_API_TYPE_CAMERA: Int = 1
        const val FOREGROUND_SERVICE_API_TYPE_BLUETOOTH: Int = 2
        const val FOREGROUND_SERVICE_API_TYPE_LOCATION: Int = 3
        const val FOREGROUND_SERVICE_API_TYPE_MEDIA_PLAYBACK: Int = 4
        const val FOREGROUND_SERVICE_API_TYPE_AUDIO: Int = 5
        const val FOREGROUND_SERVICE_API_TYPE_MICROPHONE: Int = 6
        const val FOREGROUND_SERVICE_API_TYPE_PHONE_CALL: Int = 7
        const val FOREGROUND_SERVICE_API_TYPE_USB: Int = 8
        const val FOREGROUND_SERVICE_API_TYPE_CDM: Int = 9
        const val FOREGROUND_SERVICE_API_EVENT_BEGIN: Int = 1
        const val FOREGROUND_SERVICE_API_EVENT_END: Int = 2
        const val RESTRICTION_LEVEL_UNKNOWN: Int = 0
        const val RESTRICTION_LEVEL_UNRESTRICTED: Int = 10
        const val RESTRICTION_LEVEL_EXEMPTED: Int = 20
        const val RESTRICTION_LEVEL_ADAPTIVE_BUCKET: Int = 30
        const val RESTRICTION_LEVEL_RESTRICTED_BUCKET: Int = 40
        const val RESTRICTION_LEVEL_BACKGROUND_RESTRICTED: Int = 50
        const val RESTRICTION_LEVEL_FORCE_STOPPED: Int = 60
        const val RESTRICTION_LEVEL_USER_LAUNCH_ONLY: Int = 70
        const val RESTRICTION_LEVEL_CUSTOM: Int = 90
        const val RESTRICTION_LEVEL_MAX: Int = 100
        const val RESTRICTION_SUBREASON_MAX_LENGTH: Int = 16
        const val RESTRICTION_REASON_DEFAULT: Int = 1
        const val RESTRICTION_REASON_DORMANT: Int = 2
        const val RESTRICTION_REASON_USAGE: Int = 3
        const val RESTRICTION_REASON_USER: Int = 4
        const val RESTRICTION_REASON_SYSTEM_HEALTH: Int = 5
        const val RESTRICTION_REASON_POLICY: Int = 6
        const val RESTRICTION_REASON_OTHER: Int = 7
        const val RESTRICTION_SOURCE_USER: Int = 1
        const val RESTRICTION_SOURCE_USER_NUDGED: Int = 2
        const val RESTRICTION_SOURCE_SYSTEM: Int = 3
        const val RESTRICTION_SOURCE_COMMAND_LINE: Int = 4
        const val RESTRICTION_SOURCE_REMOTE_TRIGGER: Int = 5

        /**
         * Start result constants.
         */
        val START_VOICE_HIDDEN_SESSION: Int = -100
        val START_VOICE_NOT_ACTIVE_SESSION: Int = -99
        val START_NOT_CURRENT_USER_ACTIVITY: Int = -98
        val START_NOT_VOICE_COMPATIBLE: Int = -97
        val START_CANCELED: Int = -96
        val START_NOT_ACTIVITY: Int = -95
        val START_PERMISSION_DENIED: Int = -94
        val START_FORWARD_AND_REQUEST_CONFLICT: Int = -93
        val START_CLASS_NOT_FOUND: Int = -92
        val START_INTENT_NOT_RESOLVED: Int = -91
        val START_ASSISTANT_HIDDEN_SESSION: Int = -90
        val START_ASSISTANT_NOT_ACTIVE_SESSION: Int = -89
        const val START_SUCCESS: Int = 0
        const val START_RETURN_INTENT_TO_CALLER: Int = 1
        const val START_TASK_TO_FRONT: Int = 2
        const val START_DELIVERED_TO_TOP: Int = 3
        const val START_SWITCHES_CANCELED: Int = 100
        const val START_RETURN_LOCK_TASK_MODE_VIOLATION: Int = 101
        const val START_ABORTED: Int = 102
        val START_FLAG_ONLY_IF_NEEDED: Int = 1 shl 0
        val START_FLAG_DEBUG: Int = 1 shl 1
        val START_FLAG_TRACK_ALLOCATION: Int = 1 shl 2
        val START_FLAG_NATIVE_DEBUGGING: Int = 1 shl 3
        val START_FLAG_DEBUG_SUSPEND: Int = 1 shl 4
        const val BROADCAST_SUCCESS: Int = 0
        val BROADCAST_STICKY_CANT_HAVE_PERMISSION: Int = -1
        val BROADCAST_FAILED_USER_STOPPED: Int = -2
        const val INTENT_SENDER_UNKNOWN: Int = 0
        const val INTENT_SENDER_BROADCAST: Int = 1
        const val INTENT_SENDER_ACTIVITY: Int = 2
        const val INTENT_SENDER_ACTIVITY_RESULT: Int = 3
        const val INTENT_SENDER_SERVICE: Int = 4
        const val INTENT_SENDER_FOREGROUND_SERVICE: Int = 5
        const val USER_OP_SUCCESS: Int = 0
        val USER_OP_UNKNOWN_USER: Int = -1
        val USER_OP_IS_CURRENT: Int = -2
        val USER_OP_ERROR_IS_SYSTEM: Int = -3
        val USER_OP_ERROR_RELATED_USERS_CANNOT_STOP: Int = -4
        val COMPAT_MODE_ALWAYS: Int = -1
        val COMPAT_MODE_NEVER: Int = -2
        val COMPAT_MODE_UNKNOWN: Int = -3
        const val COMPAT_MODE_DISABLED: Int = 0
        const val COMPAT_MODE_ENABLED: Int = 1
        const val COMPAT_MODE_TOGGLE: Int = 2
        const val LOCK_TASK_MODE_NONE: Int = 0
        const val LOCK_TASK_MODE_LOCKED: Int = 1
        const val LOCK_TASK_MODE_PINNED: Int = 2
        const val RECENT_WITH_EXCLUDED: Int = 0x0001
        const val RECENT_IGNORE_UNAVAILABLE: Int = 0x0002
        const val MOVE_TASK_WITH_HOME: Int = 0x00000001
        const val MOVE_TASK_NO_USER_ACTION: Int = 0x00000002
        const val META_HOME_ALTERNATE: String = "android.app.home.alternate"
        const val ACTION_REPORT_HEAP_LIMIT: String = "android.app.action.REPORT_HEAP_LIMIT"
        val FLAG_OR_STOPPED: Int = 1 shl 0
        val FLAG_AND_LOCKED: Int = 1 shl 1
        val FLAG_AND_UNLOCKED: Int = 1 shl 2
        val FLAG_AND_UNLOCKING_OR_UNLOCKED: Int = 1 shl 3
        val STOP_USER_ON_SWITCH_DEFAULT: Int = -1
        const val STOP_USER_ON_SWITCH_TRUE: Int = 1
        const val STOP_USER_ON_SWITCH_FALSE: Int = 0

        fun isStartResultSuccessful(result: Int): Boolean {
            return false
        }

        fun isStartResultFatalError(result: Int): Boolean {
            return false
        }

        fun restrictionLevelToName(@RestrictionLevel level: Int): String? {
            return null
        }

        fun isProcStateBackground(procState: Int): Boolean {
            return false
        }

        fun isProcStateCached(procState: Int): Boolean {
            return false
        }

        fun isProcStateJankPerceptible(procState: Int): Boolean {
            return false
        }

        fun isFloating(@WindowingMode windowingMode: Int): Boolean {
            return false
        }

        fun inMultiWindowMode(@WindowingMode windowingMode: Int): Boolean {
            return false
        }

        val isLowRamDeviceStatic: Boolean
            get() = false

        val isSmallBatteryDevice: Boolean
            get() = false

        val isHighEndGfx: Boolean
            get() = false

        val maxRecentTasksStatic: Int
            get() = 0

        fun printCapabilitiesSummary(pw: PrintWriter?, @ProcessCapability caps: Int) {
        }

        fun printCapabilitiesSummary(sb: StringBuilder?, @ProcessCapability caps: Int) {
        }

        fun printCapabilitiesFull(pw: PrintWriter?, @ProcessCapability caps: Int) {
        }

        fun getCapabilitiesSummary(@ProcessCapability caps: Int): String? {
            return null
        }

        fun processCapabilityAmToProto(amInt: Int): Int {
            return 0
        }

        fun processStateAmToProto(amInt: Int): Int {
            return 0
        }

        fun isProcStateConsideredInteraction(@ProcessState procState: Int): Boolean {
            return false
        }

        fun procStateToString(procState: Int): String? {
            return null
        }

        val isUserAMonkey: Boolean
            get() = false

        val isRunningInTestHarness: Boolean
            get() = false

        val isRunningInUserTestHarness: Boolean
            get() = false

        fun canAccessUnexportedComponents(uid: Int): Boolean {
            return false
        }

        fun checkComponentPermission(
            permission: String?,
            uid: Int,
            owningUid: Int,
            exported: Boolean
        ): Int {
            return 0
        }

        fun checkComponentPermission(
            permission: String?,
            uid: Int,
            deviceId: Int,
            owningUid: Int,
            exported: Boolean
        ): Int {
            return 0
        }

        fun checkUidPermission(permission: String?, uid: Int): Int {
            return 0
        }

        val currentUser: Int
            get() = 0

        fun resumeAppSwitches() {
        }

        fun noteWakeupAlarm(
            ps: PendingIntent?,
            workSource: WorkSource?,
            sourceUid: Int,
            sourcePkg: String?,
            tag: String?
        ) {
        }

        fun noteAlarmStart(
            ps: PendingIntent?,
            workSource: WorkSource?,
            sourceUid: Int,
            tag: String?
        ) {
        }

        fun noteAlarmFinish(
            ps: PendingIntent?,
            workSource: WorkSource?,
            sourceUid: Int,
            tag: String?
        ) {
        }

        fun setVrThread(tid: Int) {
        }

        fun setPersistentVrThread(tid: Int) {
        }

        fun dumpPackageStateStatic(fd: FileDescriptor?, packageName: String?) {
        }

        val isSystemReady: Boolean
            get() = false

        fun staticGetMemoryClass(): Int {
            return 0
        }

        fun staticGetLargeMemoryClass(): Int {
            return 0
        }

        val isLowMemoryKillReportSupported: Boolean
            get() = false

        fun getMyMemoryState(outState: RunningAppProcessInfo?) {
        }
    }
}
