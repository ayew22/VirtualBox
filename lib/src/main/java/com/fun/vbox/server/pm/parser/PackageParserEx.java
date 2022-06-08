package com.fun.vbox.server.pm.parser;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;

import com.fun.vbox.GmsSupport;
import com.fun.vbox.client.core.SettingConfig;
import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.env.Constants;
import com.fun.vbox.client.fixer.ComponentFixer;
import com.fun.vbox.helper.collection.ArrayMap;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.helper.compat.NativeLibraryHelperCompat;
import com.fun.vbox.helper.compat.PackageParserCompat;
import com.fun.vbox.helper.utils.FileUtils;
import com.fun.vbox.helper.utils.VLog;
import com.fun.vbox.os.VEnvironment;
import com.fun.vbox.remote.InstalledAppInfo;
import com.fun.vbox.server.pm.PackageCacheManager;
import com.fun.vbox.server.pm.PackageSetting;
import com.fun.vbox.server.pm.PackageUserState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import mirror.vbox.content.pm.ApplicationInfoL;
import mirror.vbox.content.pm.ApplicationInfoN;

/**
 * @author Lody
 */

public class PackageParserEx {

    private static final String TAG = PackageParserEx.class.getSimpleName();

    private static final ArrayMap<String, String[]> sSharedLibCache = new ArrayMap<>();

    public static VPackage parsePackage(File packageFile) throws Throwable {
        PackageParser parser = PackageParserCompat.createParser(packageFile);
        PackageParser.Package p = PackageParserCompat.parsePackage(parser, packageFile, 0);
        if (p.requestedPermissions.contains("android.permission.FAKE_PACKAGE_SIGNATURE")
                && p.mAppMetaData != null
                && p.mAppMetaData.containsKey("fake-signature")) {
            String sig = p.mAppMetaData.getString("fake-signature");
            p.mSignatures = new Signature[]{new Signature(sig)};
            VLog.d(TAG, "Using fake-signature feature on : " + p.packageName);
        } else {
            try {
                int flag = 0;
                if (BuildCompat.isPie()) {
                    flag |= PackageParser.PARSE_IS_SYSTEM_DIR;
                } else {
                    flag |= PackageParser.PARSE_IS_SYSTEM;
                }
                PackageParserCompat.collectCertificates(parser, p, flag);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return buildPackageCache(p);
    }

    public static VPackage readPackageCache(String packageName) {
        Parcel p = Parcel.obtain();
        try {
            File cacheFile = VEnvironment.getPackageCacheFile(packageName);
            FileInputStream is = new FileInputStream(cacheFile);
            byte[] bytes = FileUtils.toByteArray(is);
            is.close();
            p.unmarshall(bytes, 0, bytes.length);
            p.setDataPosition(0);
            if (p.readInt() != 4) {
                throw new IllegalStateException("Invalid version.");
            }
            VPackage pkg = new VPackage(p);
            addOwner(pkg);
            return pkg;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            p.recycle();
        }
        return null;
    }

    public static void readSignature(VPackage pkg) {
        File signatureFile = VEnvironment.getSignatureFile(pkg.packageName);
        if (!signatureFile.exists()) {
            return;
        }
        Parcel p = Parcel.obtain();
        try {
            FileInputStream fis = new FileInputStream(signatureFile);
            byte[] bytes = FileUtils.toByteArray(fis);
            fis.close();
            p.unmarshall(bytes, 0, bytes.length);
            p.setDataPosition(0);
            pkg.mSignatures = p.createTypedArray(Signature.CREATOR);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            p.recycle();
        }
    }

    public static void savePackageCache(VPackage pkg) {
        final String packageName = pkg.packageName;
        File cacheFile = VEnvironment.getPackageCacheFile(packageName);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
        File signatureFile = VEnvironment.getSignatureFile(packageName);
        if (signatureFile.exists()) {
            signatureFile.delete();
        }
        Parcel p = Parcel.obtain();

        try {
            p.writeInt(4);
            pkg.writeToParcel(p, 0);
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(p.marshall());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            p.recycle();
        }
        Signature[] signatures = pkg.mSignatures;
        if (signatures != null) {
            if (signatureFile.exists() && !signatureFile.delete()) {
                VLog.w(TAG, "Unable to delete the signatures of " + packageName);
            }
            p = Parcel.obtain();
            try {
                p.writeTypedArray(signatures, 0);
                FileUtils.writeParcelToFile(p, signatureFile);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                p.recycle();
            }
        }
    }

    private static VPackage buildPackageCache(PackageParser.Package p) {
        VPackage cache = new VPackage();
        cache.activities = new ArrayList<>(p.activities.size());
        cache.services = new ArrayList<>(p.services.size());
        cache.receivers = new ArrayList<>(p.receivers.size());
        cache.providers = new ArrayList<>(p.providers.size());
        cache.instrumentation = new ArrayList<>(p.instrumentation.size());
        cache.permissions = new ArrayList<>(p.permissions.size());
        cache.permissionGroups = new ArrayList<>(p.permissionGroups.size());

        for (PackageParser.Activity activity : p.activities) {
            cache.activities.add(new VPackage.ActivityComponent(activity));
        }
        for (PackageParser.Service service : p.services) {
            cache.services.add(new VPackage.ServiceComponent(service));
        }
        for (PackageParser.Activity receiver : p.receivers) {
            cache.receivers.add(new VPackage.ActivityComponent(receiver));
        }
        for (PackageParser.Provider provider : p.providers) {
            cache.providers.add(new VPackage.ProviderComponent(provider));
        }
        for (PackageParser.Instrumentation instrumentation : p.instrumentation) {
            cache.instrumentation.add(new VPackage.InstrumentationComponent(instrumentation));
        }
        for (PackageParser.Permission permission : p.permissions) {
            cache.permissions.add(new VPackage.PermissionComponent(permission));
        }
        for (PackageParser.PermissionGroup permissionGroup : p.permissionGroups) {
            cache.permissionGroups.add(new VPackage.PermissionGroupComponent(permissionGroup));
        }
        cache.requestedPermissions = new ArrayList<>(p.requestedPermissions.size());
        cache.requestedPermissions.addAll(p.requestedPermissions);
        if (mirror.vbox.content.pm.PackageParser.Package.protectedBroadcasts != null) {
            List<String> protectedBroadcasts =
                    mirror.vbox.content.pm.PackageParser.Package.protectedBroadcasts.get(p);
            if (protectedBroadcasts != null) {
                cache.protectedBroadcasts = new ArrayList<>(protectedBroadcasts);
                cache.protectedBroadcasts.addAll(protectedBroadcasts);
            }
        }
        cache.applicationInfo = p.applicationInfo;
        cache.mSignatures = getSignature(p);
        cache.mAppMetaData = p.mAppMetaData;
        cache.packageName = p.packageName;
        cache.mPreferredOrder = p.mPreferredOrder;
        cache.mVersionName = p.mVersionName;
        cache.mSharedUserId = p.mSharedUserId;
        cache.mSharedUserLabel = p.mSharedUserLabel;
        cache.usesLibraries = p.usesLibraries;
        cache.usesOptionalLibraries = p.usesOptionalLibraries;
        cache.mVersionCode = p.mVersionCode;
        cache.configPreferences = p.configPreferences;
        cache.reqFeatures = p.reqFeatures;
        addOwner(cache);

//        updatePackageApache(cache);
        return cache;
    }



    private static Signature[] getSignature(PackageParser.Package p) {
        if (BuildCompat.isPie()) {
            return p.mSigningDetails.signatures;
        } else {
            return p.mSignatures;
        }
    }

    public static void initApplicationInfoBase(PackageSetting ps, VPackage p) {
        ApplicationInfo ai = p.applicationInfo;
        if (TextUtils.isEmpty(ai.processName)) {
            ai.processName = ai.packageName;
        }
        ai.enabled = true;
        ai.uid = ps.appId;
        ai.name = ComponentFixer.fixComponentClassName(ps.packageName, ai.name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ApplicationInfoL.scanSourceDir.set(ai, ai.dataDir);
            ApplicationInfoL.scanPublicSourceDir.set(ai, ai.dataDir);
            String hostPrimaryCpuAbi = ApplicationInfoL.primaryCpuAbi
                    .get(VCore.get().getContext().getApplicationInfo());
            ApplicationInfoL.primaryCpuAbi.set(ai, hostPrimaryCpuAbi);
        }
        String[] sharedLibraryFiles = sSharedLibCache.get(ps.packageName);
        if (sharedLibraryFiles == null) {
            List<String> sharedLibraryFileList = new LinkedList<>();
            if (ps.appMode == InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK) {
                PackageManager hostPM = VCore.get().getUnHookPackageManager();
                try {
                    ApplicationInfo hostInfo = hostPM.getApplicationInfo(ps.packageName,
                            PackageManager.GET_SHARED_LIBRARY_FILES);
                    if (hostInfo.sharedLibraryFiles != null) {
                        Collections.addAll(sharedLibraryFileList, hostInfo.sharedLibraryFiles);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // ignore
                }
            }
            if (Build.VERSION.SDK_INT >= 28 && ai.targetSdkVersion < 28) {
                String APACHE_LEGACY_JAR = Constants.APACHE_LEGACY_P;
                if (!FileUtils.isExist(APACHE_LEGACY_JAR)) {
                    APACHE_LEGACY_JAR = Constants.APACHE_LEGACY_Q;
                }
                if (!sharedLibraryFileList.contains(APACHE_LEGACY_JAR)) {
                    sharedLibraryFileList.add(APACHE_LEGACY_JAR);
                }
            }
            sharedLibraryFiles = sharedLibraryFileList.toArray(new String[0]);
            sSharedLibCache.put(ps.packageName, sharedLibraryFiles);
        }
        ai.sharedLibraryFiles = sharedLibraryFiles;
    }

    private static void initApplicationAsUser(ApplicationInfo ai, int userId) {
        PackageSetting ps = PackageCacheManager.getSetting(ai.packageName);
        if (ps == null) {
            throw new IllegalStateException();
        }
        boolean is64bit = ps.isRunOn64BitProcess();
        String apkPath = ps.getApkPath(is64bit);
        ai.publicSourceDir = apkPath;
        ai.sourceDir = apkPath;
        SettingConfig config = VCore.getConfig();
        SettingConfig.AppLibConfig libConfig = config.getAppLibConfig(ai.packageName);
        if (is64bit) {
            ai.nativeLibraryDir = VEnvironment.getAppLibDirectory64(ai.packageName).getPath();
        } else {
            ai.nativeLibraryDir = VEnvironment.getAppLibDirectory(ai.packageName).getPath();
        }
        ApplicationInfo outside = null;
        try {
            outside = VCore.get().getUnHookPackageManager().getApplicationInfo(ai.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        if (ps.appMode == InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK) {
            if (libConfig == SettingConfig.AppLibConfig.UseRealLib && outside == null) {
                libConfig = SettingConfig.AppLibConfig.UseOwnLib;
            }
            if (GmsSupport.isGoogleAppOrService(ai.packageName)) {
                libConfig = SettingConfig.AppLibConfig.UseOwnLib;
            }
            if (outside != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ai.splitNames = outside.splitNames;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ai.splitPublicSourceDirs = outside.splitPublicSourceDirs;
                    ai.splitSourceDirs = outside.splitSourceDirs;
                }
                if (libConfig == SettingConfig.AppLibConfig.UseRealLib) {
                    String outsideNativeLib = chooseOutsideNativeLib(outside, is64bit);
                    if (outsideNativeLib != null) {
                        ai.nativeLibraryDir = outsideNativeLib;
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (is64bit) {
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    ApplicationInfoL.primaryCpuAbi.set(ai, Build.SUPPORTED_64_BIT_ABIS[0]);
                }
                if (ps.flag == PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT) {
                    ApplicationInfoL.secondaryCpuAbi.set(ai, Build.SUPPORTED_32_BIT_ABIS[0]);
                }
            } else {
                ApplicationInfoL.primaryCpuAbi.set(ai, Build.SUPPORTED_32_BIT_ABIS[0]);
                if (ps.flag == PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT) {
                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                        ApplicationInfoL.secondaryCpuAbi.set(ai, Build.SUPPORTED_64_BIT_ABIS[0]);
                    }
                }
            }
        }
        if (is64bit) {
            ai.dataDir =
                    VEnvironment.getDataUserPackageDirectory64(userId, ai.packageName).getPath();
        } else {
            ai.dataDir = VEnvironment.getDataUserPackageDirectory(userId, ai.packageName).getPath();
        }
        String scanSourceDir = new File(apkPath).getParent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ApplicationInfoL.scanSourceDir.set(ai, scanSourceDir);
            ApplicationInfoL.scanPublicSourceDir.set(ai, scanSourceDir);
            if (libConfig == SettingConfig.AppLibConfig.UseRealLib && outside != null) {
                ApplicationInfoL.splitPublicSourceDirs.set(ai, outside.splitPublicSourceDirs);
                ApplicationInfoL.splitSourceDirs.set(ai, outside.splitSourceDirs);
            }
            if (Build.VERSION.SDK_INT >= 26 && outside != null) {
                ai.splitNames = outside.splitNames;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String deDataDir;
            if (is64bit) {
                deDataDir = VEnvironment.getDeDataUserPackageDirectory64(userId, ai.packageName)
                        .getPath();
            } else {
                deDataDir = VEnvironment.getDeDataUserPackageDirectory(userId, ai.packageName)
                        .getPath();
            }
            if (ApplicationInfoN.deviceEncryptedDataDir != null) {
                ApplicationInfoN.deviceEncryptedDataDir.set(ai, deDataDir);
            }
            if (ApplicationInfoN.credentialEncryptedDataDir != null) {
                ApplicationInfoN.credentialEncryptedDataDir.set(ai, ai.dataDir);
            }
            if (ApplicationInfoN.deviceProtectedDataDir != null) {
                ApplicationInfoN.deviceProtectedDataDir.set(ai, deDataDir);
            }
            if (ApplicationInfoN.credentialProtectedDataDir != null) {
                ApplicationInfoN.credentialProtectedDataDir.set(ai, ai.dataDir);
            }
        }
        if (config.isEnableIORedirect()) {
            if (config.isUseRealDataDir(ai.packageName)) {
                ai.dataDir = "/data/data/" + ai.packageName + "/";
            }
            if (config.isUseRealLibDir(ai.packageName)) {
                ai.nativeLibraryDir = "/data/data/" + ai.packageName + "/lib/";
            }
        }
    }

    private static String chooseOutsideNativeLib(ApplicationInfo ai, boolean is64bit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                String primaryCpuAbi = ApplicationInfoL.primaryCpuAbi.get(ai);
                String secondaryCpuAbi = ApplicationInfoL.secondaryCpuAbi.get(ai);
                if (primaryCpuAbi == null) {
                    return null;
                }
                boolean matchPrimary = is64bit
                        ? NativeLibraryHelperCompat.is64bitAbi(primaryCpuAbi)
                        : NativeLibraryHelperCompat.is32bitAbi(primaryCpuAbi);
                if (matchPrimary) {
                    return ai.nativeLibraryDir;
                } else {
                    if (secondaryCpuAbi != null) {
                        return ApplicationInfoL.secondaryNativeLibraryDir.get(ai);
                    }
                    return null;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return ai.nativeLibraryDir;
    }

    private static void addOwner(VPackage p) {
        for (VPackage.ActivityComponent activity : p.activities) {
            activity.owner = p;
            for (VPackage.ActivityIntentInfo info : activity.intents) {
                info.activity = activity;
            }
        }
        for (VPackage.ServiceComponent service : p.services) {
            service.owner = p;
            for (VPackage.ServiceIntentInfo info : service.intents) {
                info.service = service;
            }
        }
        for (VPackage.ActivityComponent receiver : p.receivers) {
            receiver.owner = p;
            for (VPackage.ActivityIntentInfo info : receiver.intents) {
                info.activity = receiver;
            }
        }
        for (VPackage.ProviderComponent provider : p.providers) {
            provider.owner = p;
            for (VPackage.ProviderIntentInfo info : provider.intents) {
                info.provider = provider;
            }
        }
        for (VPackage.InstrumentationComponent instrumentation : p.instrumentation) {
            instrumentation.owner = p;
        }
        for (VPackage.PermissionComponent permission : p.permissions) {
            permission.owner = p;
        }
        for (VPackage.PermissionGroupComponent group : p.permissionGroups) {
            group.owner = p;
        }
        int flags = ApplicationInfo.FLAG_HAS_CODE;
        if (GmsSupport.isGoogleService(p.packageName)) {
            flags |= ApplicationInfo.FLAG_PERSISTENT;
        }
        p.applicationInfo.flags |= flags;
    }

    public static PackageInfo generatePackageInfo(VPackage p, int flags,
                                                  int appMode, long firstInstallTime,
                                                  long lastUpdateTime, PackageUserState state,
                                                  int userId) {
        if (!checkUseInstalledOrHidden(state, flags)) {
            return null;
        }
        if (p.mSignatures == null) {
            readSignature(p);
        }
        PackageInfo pi = new PackageInfo();
        pi.packageName = p.packageName;
        pi.versionCode = p.mVersionCode;
        pi.sharedUserLabel = p.mSharedUserLabel;
        pi.versionName = p.mVersionName;
        pi.sharedUserId = p.mSharedUserId;
        pi.applicationInfo = generateApplicationInfo(p, flags, state, userId);
        if (Build.VERSION.SDK_INT >= 21 && appMode == InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK) {
            PackageInfo packageInfo;
            try {
                packageInfo =
                        VCore.get().getUnHookPackageManager().getPackageInfo(p.packageName, 0);
            } catch (PackageManager.NameNotFoundException unused) {
                packageInfo = null;
            }
            if (packageInfo != null) {
                pi.splitNames = packageInfo.splitNames;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageInfo pkgInfo = null;
            try {
                pkgInfo = VCore.get().getUnHookPackageManager()
                        .getPackageInfo(pi.packageName, PackageManager.GET_SIGNING_CERTIFICATES);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore
            }
            if (pkgInfo != null) {
                pi.signingInfo = pkgInfo.signingInfo;
            }
        }
        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if (p.requestedPermissions != null && !p.requestedPermissions.isEmpty()) {
            String[] requestedPermissions = new String[p.requestedPermissions.size()];
            p.requestedPermissions.toArray(requestedPermissions);
            pi.requestedPermissions = requestedPermissions;
        }
        if ((flags & PackageManager.GET_GIDS) != 0) {
            pi.gids = PackageParserCompat.GIDS;
        }
        if ((flags & PackageManager.GET_CONFIGURATIONS) != 0) {
            int N = p.configPreferences != null ? p.configPreferences.size() : 0;
            if (N > 0) {
                pi.configPreferences = new ConfigurationInfo[N];
                p.configPreferences.toArray(pi.configPreferences);
            }
            N = p.reqFeatures != null ? p.reqFeatures.size() : 0;
            if (N > 0) {
                pi.reqFeatures = new FeatureInfo[N];
                p.reqFeatures.toArray(pi.reqFeatures);
            }
        }
        if ((flags & PackageManager.GET_ACTIVITIES) != 0) {
            final int N = p.activities.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final VPackage.ActivityComponent a = p.activities.get(i);
                    res[num++] = generateActivityInfo(a, flags, state, userId);
                }
                pi.activities = res;
            }
        }
        if ((flags & PackageManager.GET_RECEIVERS) != 0) {
            final int N = p.receivers.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final VPackage.ActivityComponent a = p.receivers.get(i);
                    res[num++] = generateActivityInfo(a, flags, state, userId);
                }
                pi.receivers = res;
            }
        }
        if ((flags & PackageManager.GET_SERVICES) != 0) {
            final int N = p.services.size();
            if (N > 0) {
                int num = 0;
                final ServiceInfo[] res = new ServiceInfo[N];
                for (int i = 0; i < N; i++) {
                    final VPackage.ServiceComponent s = p.services.get(i);
                    res[num++] = generateServiceInfo(s, flags, state, userId);
                }
                pi.services = res;
            }
        }
        if ((flags & PackageManager.GET_PROVIDERS) != 0) {
            final int N = p.providers.size();
            if (N > 0) {
                int num = 0;
                final ProviderInfo[] res = new ProviderInfo[N];
                for (int i = 0; i < N; i++) {
                    final VPackage.ProviderComponent pr = p.providers.get(i);
                    res[num++] = generateProviderInfo(pr, flags, state, userId);
                }
                pi.providers = res;
            }
        }
        if ((flags & PackageManager.GET_INSTRUMENTATION) != 0) {
            int N = p.instrumentation.size();
            if (N > 0) {
                pi.instrumentation = new InstrumentationInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.instrumentation[i] = generateInstrumentationInfo(
                            p.instrumentation.get(i), flags);
                }
            }
        }
        if ((flags & PackageManager.GET_PERMISSIONS) != 0) {
            int N = p.permissions.size();
            if (N > 0) {
                pi.permissions = new PermissionInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.permissions[i] = generatePermissionInfo(p.permissions.get(i), flags);
                }
            }
            N = p.requestedPermissions == null ? 0 : p.requestedPermissions.size();
            if (N > 0) {
                pi.requestedPermissions = new String[N];
                for (int i = 0; i < N; i++) {
                    final String perm = p.requestedPermissions.get(i);
                    pi.requestedPermissions[i] = perm;
                }
            }
        }
        if ((flags & PackageManager.GET_SIGNATURES) != 0) {
            int N = (p.mSignatures != null) ? p.mSignatures.length : 0;
            if (N > 0) {
                pi.signatures = new Signature[N];
                System.arraycopy(p.mSignatures, 0, pi.signatures, 0, N);
            } else {
                try {
                    PackageInfo outInfo = VCore.get().getUnHookPackageManager()
                            .getPackageInfo(p.packageName, PackageManager.GET_SIGNATURES);
                    pi.signatures = outInfo.signatures;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return pi;
    }

    public static ApplicationInfo generateApplicationInfo(VPackage p, int flags,
                                                          PackageUserState state, int userId) {
        if (p == null) {
            return null;
        }
        if (!checkUseInstalledOrHidden(state, flags)) {
            return null;
        }

        if (!copyNeeded(flags, p, state, null, userId)) {
            initApplicationAsUser(p.applicationInfo, userId);
            return p.applicationInfo;
        }

        // Make shallow copy so we can store the metadata/libraries safely
        ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
        if ((flags & PackageManager.GET_META_DATA) != 0) {
            ai.metaData = p.mAppMetaData;
        }
        initApplicationAsUser(ai, userId);
        return ai;
    }

    public static ActivityInfo generateActivityInfo(VPackage.ActivityComponent a, int flags,
                                                    PackageUserState state, int userId) {
        if (a == null) {
            return null;
        }
        if (!checkUseInstalledOrHidden(state, flags)) {
            return null;
        }
        // Make shallow copies so we can store the metadata safely
        ActivityInfo ai = new ActivityInfo(a.info);
        if ((flags & PackageManager.GET_META_DATA) != 0
                && (a.metaData != null)) {
            ai.metaData = a.metaData;
        }
        ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
        return ai;
    }

    public static ServiceInfo generateServiceInfo(VPackage.ServiceComponent s, int flags,
                                                  PackageUserState state, int userId) {
        if (s == null) {
            return null;
        }
        if (!checkUseInstalledOrHidden(state, flags)) {
            return null;
        }
        ServiceInfo si = new ServiceInfo(s.info);
        // Make shallow copies so we can store the metadata safely
        if ((flags & PackageManager.GET_META_DATA) != 0 && s.metaData != null) {
            si.metaData = s.metaData;
        }
        si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
        return si;
    }

    public static ProviderInfo generateProviderInfo(VPackage.ProviderComponent p, int flags,
                                                    PackageUserState state, int userId) {
        if (p == null) {
            return null;
        }
        if (!checkUseInstalledOrHidden(state, flags)) {
            return null;
        }
        // Make shallow copies so we can store the metadata safely
        ProviderInfo pi = new ProviderInfo(p.info);
        if ((flags & PackageManager.GET_META_DATA) != 0
                && (p.metaData != null)) {
            pi.metaData = p.metaData;
        }

        if ((flags & PackageManager.GET_URI_PERMISSION_PATTERNS) == 0) {
            pi.uriPermissionPatterns = null;
        }
        pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
        return pi;
    }

    public static InstrumentationInfo generateInstrumentationInfo(
            VPackage.InstrumentationComponent i, int flags) {
        if (i == null) {
            return null;
        }
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    public static PermissionInfo generatePermissionInfo(
            VPackage.PermissionComponent p, int flags) {
        if (p == null) {
            return null;
        }
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static PermissionGroupInfo generatePermissionGroupInfo(
            VPackage.PermissionGroupComponent pg, int flags) {
        if (pg == null) {
            return null;
        }
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return pg.info;
        }
        PermissionGroupInfo pgi = new PermissionGroupInfo(pg.info);
        pgi.metaData = pg.metaData;
        return pgi;
    }

    private static boolean checkUseInstalledOrHidden(PackageUserState state, int flags) {
        //noinspection deprecation
        return (state.installed && !state.hidden)
                || (flags & PackageManager.GET_UNINSTALLED_PACKAGES) != 0;
    }

    private static boolean copyNeeded(int flags, VPackage p,
                                      PackageUserState state, Bundle metaData, int userId) {
        if (!state.installed || state.hidden) {
            return true;
        }
        return (flags & PackageManager.GET_META_DATA) != 0
                && (metaData != null || p.mAppMetaData != null);
    }
/*

    private static void updatePackageApache(VPackage paramVPackage) {
        if (paramVPackage.usesLibraries == null)
            paramVPackage.usesLibraries = new ArrayList<>();
        if (paramVPackage.usesOptionalLibraries == null)
            paramVPackage.usesOptionalLibraries = new ArrayList<>();
        if (paramVPackage.applicationInfo != null && paramVPackage.applicationInfo.targetSdkVersion < 28 && !isLibraryPresent(paramVPackage.usesLibraries, paramVPackage.usesOptionalLibraries, "org.apache.http.legacy"))
            paramVPackage.usesLibraries.add(0, "org.apache.http.legacy");
        if (paramVPackage.applicationInfo != null && !isLibraryPresent(paramVPackage.usesLibraries, paramVPackage.usesOptionalLibraries, "android.test.base"))
            paramVPackage.usesLibraries.add(0, "android.test.base");
    }

    private static boolean isLibraryPresent(List<String> paramList1, List<String> paramList2, String paramString) {
        if (paramList1 != null) {
            Iterator<String> iterator = paramList1.iterator();
            while (iterator.hasNext()) {
                if (((String)iterator.next()).equals(paramString))
                    return true;
            }
        }
        if (paramList2 != null) {
            Iterator<String> iterator = paramList2.iterator();
            while (iterator.hasNext()) {
                if (((String)iterator.next()).equals(paramString))
                    return true;
            }
        }
        return false;
    }
*/

}
