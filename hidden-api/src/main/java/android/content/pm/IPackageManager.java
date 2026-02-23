/*
 **
 ** Copyright 2007, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager.ComponentEnabledSetting;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;

import java.util.List;
import java.util.Map;

/**
 * See {@link PackageManager} for documentation on most of the APIs
 * here.
 *
 * @hide
 */
public interface IPackageManager {
    void checkPackageStartable(String packageName, int userId);

    boolean isPackageAvailable(String packageName, int userId);

    PackageInfo getPackageInfo(String packageName, long flags, int userId);

    PackageInfo getPackageInfoVersioned(VersionedPackage versionedPackage,
                                        long flags, int userId);

    int getPackageUid(String packageName, long flags, int userId);

    int[] getPackageGids(String packageName, long flags, int userId);

    String[] currentToCanonicalPackageNames(String[] names);

    String[] canonicalToCurrentPackageNames(String[] names);

    ApplicationInfo getApplicationInfo(String packageName, long flags, int userId);

    /**
     * @return the target SDK for the given package name, or -1 if it cannot be retrieved
     */
    int getTargetSdkVersion(String packageName);

    ActivityInfo getActivityInfo(ComponentName className, long flags, int userId);

    boolean activitySupportsIntentAsUser(ComponentName className, Intent intent,
                                         String resolvedType, int userId);

    ActivityInfo getReceiverInfo(ComponentName className, long flags, int userId);

    ServiceInfo getServiceInfo(ComponentName className, long flags, int userId);

    ProviderInfo getProviderInfo(ComponentName className, long flags, int userId);

    boolean isProtectedBroadcast(String actionName);

    int checkSignatures(String pkg1, String pkg2, int userId);

    int checkUidSignatures(int uid1, int uid2);

    List<String> getAllPackages();

    String[] getPackagesForUid(int uid);

    String getNameForUid(int uid);

    String[] getNamesForUids(int[] uids);

    int getUidForSharedUser(String sharedUserName);

    int getFlagsForUid(int uid);

    int getPrivateFlagsForUid(int uid);

    boolean isUidPrivileged(int uid);

    ResolveInfo resolveIntent(Intent intent, String resolvedType, long flags, int userId);

    ResolveInfo findPersistentPreferredActivity(Intent intent, int userId);

    boolean canForwardTo(Intent intent, String resolvedType, int sourceUserId, int targetUserId);

    ParceledListSlice queryIntentActivities(Intent intent,
                                            String resolvedType, long flags, int userId);

    ParceledListSlice queryIntentActivityOptions(
            ComponentName caller, Intent[] specifics,
            String[] specificTypes, Intent intent,
            String resolvedType, long flags, int userId);

    ParceledListSlice queryIntentReceivers(Intent intent,
                                           String resolvedType, long flags, int userId);

    ResolveInfo resolveService(Intent intent,
                               String resolvedType, long flags, int userId);

    ParceledListSlice queryIntentServices(Intent intent,
                                          String resolvedType, long flags, int userId);

    ParceledListSlice queryIntentContentProviders(Intent intent,
                                                  String resolvedType, long flags, int userId);

    /**
     * This implements getInstalledPackages via a "last returned row"
     * mechanism that is not exposed the API. This is to get around the IPC
     * limit that kicks when flags are included that bloat up the data
     * returned.
     */
    ParceledListSlice getInstalledPackages(long flags, int userId);

    ParcelFileDescriptor getAppMetadataFd(String packageName,
                                          int userId);

    /**
     * This implements getPackagesHoldingPermissions via a "last returned row"
     * mechanism that is not exposed the API. This is to get around the IPC
     * limit that kicks when flags are included that bloat up the data
     * returned.
     */
    ParceledListSlice getPackagesHoldingPermissions(String[] permissions,
                                                    long flags, int userId);

    /**
     * This implements getInstalledApplications via a "last returned row"
     * mechanism that is not exposed the API. This is to get around the IPC
     * limit that kicks when flags are included that bloat up the data
     * returned.
     */
    ParceledListSlice getInstalledApplications(long flags, int userId);

    /**
     * Retrieve all applications that are marked as persistent.
     *
     * @return A List<ApplicationInfo> containing one entry for each persistent
     * application.
     */
    ParceledListSlice getPersistentApplications(int flags);

    ProviderInfo resolveContentProvider(String name, long flags, int userId);

    /**
     * Resolve content providers with a given authority, for a specific
     * callingUid.
     *
     * @param authority  Authority of the content provider
     * @param flags      Additional option flags to modify the data returned.
     * @param userId     Current user ID
     * @param callingUid UID of the caller who's access to the content provider
     *                   is to be checked
     * @return ProviderInfo of the resolved content provider. May return null
     */
    ProviderInfo resolveContentProviderForUid(String authority, long flags,
                                              int userId, int callingUid);

    /**
     * Retrieve sync information for all content providers.
     *
     * @param outNames Filled with a list of the root names of the content
     *                 providers that can sync.
     * @param outInfo  Filled with a list of the ProviderInfo for each
     *                 name 'outNames'.
     */
    void querySyncProviders(List<String> outNames,
                            List<ProviderInfo> outInfo);

    ParceledListSlice queryContentProviders(
            String processName, int uid, long flags, String metaDataKey);

    InstrumentationInfo getInstrumentationInfoAsUser(
            ComponentName className, int flags, int userId);

    ParceledListSlice queryInstrumentationAsUser(
            String targetPackage, int flags, int userId);

    void finishPackageInstall(int token, boolean didLaunch);

    void setInstallerPackageName(String targetPackage, String installerPackageName);

    void relinquishUpdateOwnership(String targetPackage);

    void setApplicationCategoryHint(String packageName, int categoryHint, String callerPackageName);

    String getInstallerPackageName(String packageName);

    InstallSourceInfo getInstallSourceInfo(String packageName, int userId);

    void resetApplicationPreferences(int userId);

    ResolveInfo getLastChosenActivity(Intent intent,
                                      String resolvedType, int flags);

    void setLastChosenActivity(Intent intent, String resolvedType, int flags,
                               IntentFilter filter, int match, ComponentName activity);

    void addPreferredActivity(IntentFilter filter, int match,
                              ComponentName[] set, ComponentName activity, int userId, boolean removeExisting);

    void replacePreferredActivity(IntentFilter filter, int match,
                                  ComponentName[] set, ComponentName activity, int userId);

    void clearPackagePreferredActivities(String packageName);

    int getPreferredActivities(List<IntentFilter> outFilters,
                               List<ComponentName> outActivities, String packageName);

    void addPersistentPreferredActivity(IntentFilter filter, ComponentName activity, int userId);

    void clearPackagePersistentPreferredActivities(String packageName, int userId);

    void clearPersistentPreferredActivity(IntentFilter filter, int userId);

    void addCrossProfileIntentFilter(IntentFilter intentFilter, String ownerPackage,
                                     int sourceUserId, int targetUserId, int flags);

    boolean removeCrossProfileIntentFilter(IntentFilter intentFilter, String ownerPackage,
                                           int sourceUserId, int targetUserId, int flags);

    void clearCrossProfileIntentFilters(int sourceUserId, String ownerPackage);

    String[] setDistractingPackageRestrictionsAsUser(String[] packageNames, int restrictionFlags,
                                                     int userId);

    String[] getUnsuspendablePackagesForUser(String[] packageNames, int userId);

    boolean isPackageSuspendedForUser(String packageName, int userId);

    boolean isPackageQuarantinedForUser(String packageName, int userId);

    boolean isPackageStoppedForUser(String packageName, int userId);

    Bundle getSuspendedPackageAppExtras(String packageName, int userId);

    String getSuspendingPackage(String packageName, int userId);

    /**
     * Backup/restore support - only the system uid may use these.
     */
    byte[] getPreferredActivityBackup(int userId);

    void restorePreferredActivities(byte[] backup, int userId);

    byte[] getDefaultAppsBackup(int userId);

    void restoreDefaultApps(byte[] backup, int userId);

    byte[] getDomainVerificationBackup(int userId);

    void restoreDomainVerification(byte[] backup, int userId);

    /**
     * Report the set of 'Home' activity candidates, plus (if any) which of them
     * is the current "always use this one" setting.
     */
    ComponentName getHomeActivities(List<ResolveInfo> outHomeCandidates);

    void setHomeActivity(ComponentName className, int userId);

    /**
     * Overrides the label and icon of the component specified by the component name. The component
     * must belong to the calling app.
     * <p>
     * These changes will be reset on the next boot and whenever the package is updated.
     * <p>
     * Only the app defined as com.android.internal.R.config_overrideComponentUiPackage is allowed
     * to call this.
     *
     * @param componentName     The component name to override the label/icon of.
     * @param nonLocalizedLabel The label to be displayed.
     * @param icon              The icon to be displayed.
     * @param userId            The user id.
     */
    void overrideLabelAndIcon(ComponentName componentName, String nonLocalizedLabel,
                              int icon, int userId);

    /**
     * Restores the label and icon of the activity specified by the component name if either has
     * been overridden. The component must belong to the calling app.
     * <p>
     * Only the app defined as com.android.internal.R.config_overrideComponentUiPackage is allowed
     * to call this.
     *
     * @param componentName The component name.
     * @param userId        The user id.
     */
    void restoreLabelAndIcon(ComponentName componentName, int userId);

    /**
     * As per {@link android.content.pm.PackageManager#setComponentEnabledSetting}.
     */
    void setComponentEnabledSetting(ComponentName componentName,
                                    int newState, int flags, int userId, String callingPackage);

    /**
     * As per {@link android.content.pm.PackageManager#setComponentEnabledSettings}.
     */
    void setComponentEnabledSettings(List<ComponentEnabledSetting> settings, int userId,
                                     String callingPackage);

    /**
     * As per {@link android.content.pm.PackageManager#getComponentEnabledSetting}.
     */
    int getComponentEnabledSetting(ComponentName componentName, int userId);

    /**
     * As per {@link android.content.pm.PackageManager#setApplicationEnabledSetting}.
     */
    void setApplicationEnabledSetting(String packageName, int newState, int flags,
                                      int userId, String callingPackage);

    /**
     * As per {@link android.content.pm.PackageManager#getApplicationEnabledSetting}.
     */
    int getApplicationEnabledSetting(String packageName, int userId);

    /**
     * Logs process start information (including APK hash) to the security log.
     */
    void logAppProcessStartIfNeeded(String packageName, String processName, int uid, String seinfo, String apkFile, int pid);

    /**
     * Set whether the given package should be considered stopped, making
     * it not visible to implicit intents that filter out stopped packages.
     */
    void setPackageStoppedState(String packageName, boolean stopped, int userId);

    /**
     * Free storage by deleting LRU sorted list of cache files across
     * all applications. If the currently available free storage
     * on the device is greater than or equal to the requested
     * free storage, no cache files are cleared. If the currently
     * available storage on the device is less than the requested
     * free storage, some or all of the cache files across
     * all applications are deleted (based on last accessed time)
     * to increase the free storage space on the device to
     * the requested value. There is no guarantee that clearing all
     * the cache files from all applications will clear up
     * enough storage to achieve the desired value.
     *
     * @param freeStorageSize The number of bytes of storage to be
     *                        freed by the system. Say if freeStorageSize is XX,
     *                        and the current free storage is YY,
     *                        if XX is less than YY, just return. if not free XX-YY number
     *                        of bytes if possible.
     * @param observer        call back used to notify when
     *                        the operation is completed
     */
    void freeStorageAndNotify(String volumeUuid, long freeStorageSize,
                              int storageFlags, IPackageDataObserver observer);

    /**
     * Free storage by deleting LRU sorted list of cache files across
     * all applications. If the currently available free storage
     * on the device is greater than or equal to the requested
     * free storage, no cache files are cleared. If the currently
     * available storage on the device is less than the requested
     * free storage, some or all of the cache files across
     * all applications are deleted (based on last accessed time)
     * to increase the free storage space on the device to
     * the requested value. There is no guarantee that clearing all
     * the cache files from all applications will clear up
     * enough storage to achieve the desired value.
     *
     * @param freeStorageSize The number of bytes of storage to be
     *                        freed by the system. Say if freeStorageSize is XX,
     *                        and the current free storage is YY,
     *                        if XX is less than YY, just return. if not free XX-YY number
     *                        of bytes if possible.
     * @param pi              IntentSender call back used to
     *                        notify when the operation is completed.May be null
     *                        to indicate that no call back is desired.
     */
    void freeStorage(String volumeUuid, long freeStorageSize,
                     int storageFlags, IntentSender pi);

    /**
     * Delete all the cache files an applications cache directory
     *
     * @param packageName The package name of the application whose cache
     *                    files need to be deleted
     * @param observer    a callback used to notify when the deletion is finished.
     */
    void deleteApplicationCacheFiles(String packageName, IPackageDataObserver observer);

    /**
     * Delete all the cache files an applications cache directory
     *
     * @param packageName The package name of the application whose cache
     *                    files need to be deleted
     * @param userId      the user to delete application cache for
     * @param observer    a callback used to notify when the deletion is finished.
     */
    void deleteApplicationCacheFilesAsUser(String packageName, int userId, IPackageDataObserver observer);

    /**
     * Clear the user data directory of an application.
     *
     * @param packageName The package name of the application whose cache
     *                    files need to be deleted
     * @param observer    a callback used to notify when the operation is completed.
     */
    void clearApplicationUserData(String packageName, IPackageDataObserver observer, int userId);

    /**
     * Clear the profile data of an application.
     *
     * @param packageName The package name of the application whose profile data
     *                    need to be deleted
     */
    void clearApplicationProfileData(String packageName);

    /**
     * Get a list of shared libraries that are available on the system.
     *
     * @deprecated use getSystemSharedLibraryNamesAndPaths() instead
     */
    String[] getSystemSharedLibraryNames();

    /**
     * Get a list of shared library names (key) and paths (values).
     */
    Map<String, String> getSystemSharedLibraryNamesAndPaths();

    /**
     * Get a list of features that are available on the system.
     */
    ParceledListSlice getSystemAvailableFeatures();

    boolean hasSystemFeature(String name, int version);

    List<String> getInitialNonStoppedSystemPackages();

    void enterSafeMode();

    boolean isSafeMode();

    boolean hasSystemUidErrors();

    /**
     * Notify the package manager that a package is going to be used and why.
     * <p>
     * See PackageManager.NOTIFY_PACKAGE_USE_* for reasons.
     */
    void notifyPackageUse(String packageName, int reason);

    /**
     * Notify the package manager that a list of dex files have been loaded.
     *
     * @param loadingPackageName    the name of the package who performs the load
     * @param classLoaderContextMap a map from file paths to dex files that have been loaded to
     *                              the class loader context that was used to load them.
     * @param loaderIsa             the ISA of the loader process
     */
    void notifyDexLoad(String loadingPackageName,
                       Map<String, String> classLoaderContextMap, String loaderIsa);

    int getMoveStatus(int moveId);

    int movePackage(String packageName, String volumeUuid);

    int movePrimaryStorage(String volumeUuid);

    boolean setInstallLocation(int loc);

    int getInstallLocation();

    int installExistingPackageAsUser(String packageName, int userId, int installFlags,
                                     int installReason, List<String> whiteListedPermissions);

    void verifyPendingInstall(int id, int verificationCode);

    void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay);

    /**
     * @deprecated
     */
    void verifyIntentFilter(int id, int verificationCode, List<String> failedDomains);

    /**
     * @deprecated
     */
    int getIntentVerificationStatus(String packageName, int userId);

    /**
     * @deprecated
     */
    boolean updateIntentVerificationStatus(String packageName, int status, int userId);

    /**
     * @deprecated
     */
    ParceledListSlice getIntentFilterVerifications(String packageName);

    ParceledListSlice getAllIntentFilters(String packageName);

    boolean isFirstBoot();

    boolean isDeviceUpgrading();

    /**
     * Reflects current DeviceStorageMonitorService state
     */
    boolean isStorageLow();

    boolean setApplicationHiddenSettingAsUser(String packageName, boolean hidden, int userId);

    boolean getApplicationHiddenSettingAsUser(String packageName, int userId);

    void setSystemAppHiddenUntilInstalled(String packageName, boolean hidden);

    boolean setSystemAppInstallState(String packageName, boolean installed, int userId);

    boolean setBlockUninstallForUser(String packageName, boolean blockUninstall, int userId);

    boolean getBlockUninstallForUser(String packageName, int userId);

    String getPermissionControllerPackageName();

    String getSdkSandboxPackageName();

    ParceledListSlice getInstantApps(int userId);

    byte[] getInstantAppCookie(String packageName, int userId);

    boolean setInstantAppCookie(String packageName, byte[] cookie, int userId);

    Bitmap getInstantAppIcon(String packageName, int userId);

    boolean isInstantApp(String packageName, int userId);

    boolean setRequiredForSystemUser(String packageName, boolean systemUserApp);

    /**
     * Sets whether or not an update is available. Ostensibly for instant apps
     * to force external resolution.
     */
    void setUpdateAvailable(String packageName, boolean updateAvailable);

    String getServicesSystemSharedLibraryPackageName();

    String getSharedSystemSharedLibraryPackageName();

    ChangedPackages getChangedPackages(int sequenceNumber, int userId);

    boolean isPackageDeviceAdminOnAnyUser(String packageName);

    int getInstallReason(String packageName, int userId);

    ParceledListSlice getSharedLibraries(String packageName, long flags, int userId);

    ParceledListSlice getDeclaredSharedLibraries(String packageName, long flags, int userId);

    boolean canRequestPackageInstalls(String packageName, int userId);

    void deletePreloadsFileCache();

    ComponentName getInstantAppResolverComponent();

    ComponentName getInstantAppResolverSettingsComponent();

    ComponentName getInstantAppInstallerComponent();

    String getInstantAppAndroidId(String packageName, int userId);

    void setHarmfulAppWarning(String packageName, CharSequence warning, int userId);

    CharSequence getHarmfulAppWarning(String packageName, int userId);

    boolean hasSigningCertificate(String packageName, byte[] signingCertificate, int flags);

    boolean hasUidSigningCertificate(int uid, byte[] signingCertificate, int flags);

    String getDefaultTextClassifierPackageName();

    String getSystemTextClassifierPackageName();

    String getAttentionServicePackageName();

    String getRotationResolverPackageName();

    String getWellbeingPackageName();

    String getAppPredictionServicePackageName();

    String getSystemCaptionsServicePackageName();

    String getSetupWizardPackageName();

    String getIncidentReportApproverPackageName();

    boolean isPackageStateProtected(String packageName, int userId);

    void sendDeviceCustomizationReadyBroadcast();

    List<ModuleInfo> getInstalledModules(int flags);

    ModuleInfo getModuleInfo(String packageName, int flags);

    int getRuntimePermissionsVersion(int userId);

    void setRuntimePermissionsVersion(int version, int userId);

    void notifyPackagesReplacedReceived(String[] packages);

    IntentSender getLaunchIntentSenderForPackage(String packageName, String callingPackage,
                                                 String featureId, int userId);

    //------------------------------------------------------------------------
    //
    // The following binder interfaces have been moved to IPermissionManager
    //
    //------------------------------------------------------------------------

    //------------------------------------------------------------------------
    // We need to keep these IPackageManager for app compatibility
    //------------------------------------------------------------------------ 
    String[] getAppOpPermissionPackages(String permissionName, int userId);

    PermissionGroupInfo getPermissionGroupInfo(String name, int flags);

    boolean addPermission(PermissionInfo info);

    boolean addPermissionAsync(PermissionInfo info);

    void removePermission(String name);

    int checkPermission(String permName, String pkgName, int userId);

    void grantRuntimePermission(String packageName, String permissionName, int userId);

    //------------------------------------------------------------------------
    // We need to keep these IPackageManager for convenience splitting
    // out the permission manager. This should be cleaned up, but, will require
    // a large change that modifies many repos.
    //------------------------------------------------------------------------
    int checkUidPermission(String permName, int uid);

    void setMimeGroup(String packageName, String group, List<String> mimeTypes);

    String getSplashScreenTheme(String packageName, int userId);

    void setSplashScreenTheme(String packageName, String themeName, int userId);

    int getUserMinAspectRatio(String packageName, int userId);

    void setUserMinAspectRatio(String packageName, int userId, int aspectRatio);

    List<String> getMimeGroup(String packageName, String group);

    boolean isAutoRevokeWhitelisted(String packageName);

    void makeProviderVisible(int recipientAppId, String visibleAuthority);

    void makeUidVisible(int recipientAppId, int visibleUid);

    IBinder getHoldLockToken();

    void holdLock(IBinder token, int durationMs);

    PackageManager.Property getPropertyAsUser(String propertyName, String packageName,
                                              String className, int userId);

    ParceledListSlice queryProperty(String propertyName, int componentType);

    void setKeepUninstalledPackages(List<String> packageList);

    boolean[] canPackageQuery(String sourcePackageName, String[] targetPackageNames, int userId);

    boolean waitForHandler(long timeoutMillis, boolean forBackgroundHandler);

    Bitmap getArchivedAppIcon(String packageName, UserHandle user, String callingPackageName);

    boolean isAppArchivable(String packageName, UserHandle user);

    int getAppMetadataSource(String packageName, int userId);

    ComponentName getDomainVerificationAgent(int userId);

    void setPageSizeAppCompatFlagsSettingsOverride(String packageName, boolean enabled);

    boolean isPageSizeCompatEnabled(String packageName);

    String getPageSizeCompatWarningMessage(String packageName);

    List<String> getAllApexDirectories();

    abstract class Stub extends Binder implements IPackageManager {
        public static IPackageManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
