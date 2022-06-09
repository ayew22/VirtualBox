package com.fun.vbox.client;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.os.Binder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

import com.fun.vbox.GmsSupport;
import com.fun.vbox.client.core.CrashHandler;
import com.fun.vbox.client.core.InvocationStubManager;
import com.fun.vbox.client.core.SettingConfig;
import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.env.SpecialComponentList;
import com.fun.vbox.client.env.VirtualRuntime;
import com.fun.vbox.client.fixer.ContextFixer;
import com.fun.vbox.client.hook.delegate.AppInstrumentation;
import com.fun.vbox.client.hook.providers.ProviderHook;
import com.fun.vbox.client.hook.proxies.am.HCallbackStub;
import com.fun.vbox.client.hook.secondary.ProxyServiceFactory;
import com.fun.vbox.client.ipc.VActivityManager;
import com.fun.vbox.client.ipc.VDeviceManager;
import com.fun.vbox.client.ipc.VPackageManager;
import com.fun.vbox.client.ipc.VirtualStorageManager;
import com.fun.vbox.client.receiver.StaticReceiverSystem;
import com.fun.vbox.client.stub.StubManifest;
import com.fun.vbox.helper.collection.ArrayMap;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.helper.compat.NativeLibraryHelperCompat;
import com.fun.vbox.helper.compat.StorageManagerCompat;
import com.fun.vbox.helper.compat.StrictModeCompat;
import com.fun.vbox.helper.utils.ComponentUtils;
import com.fun.vbox.helper.utils.Reflect;
import com.fun.vbox.helper.utils.VLog;
import com.fun.vbox.os.VEnvironment;
import com.fun.vbox.os.VUserHandle;
import com.fun.vbox.remote.ClientConfig;
import com.fun.vbox.remote.InstalledAppInfo;
import com.fun.vbox.remote.VDeviceConfig;
import com.fun.vbox.server.pm.PackageSetting;
import com.fun.vbox.server.secondary.FakeIdentityBinder;

import java.io.File;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dalvik.system.DelegateLastClassLoader;
import mirror.vbox.app.ActivityManagerNative;
import mirror.vbox.app.ActivityThread;
import mirror.vbox.app.ActivityThreadNMR1;
import mirror.vbox.app.ActivityThreadQ;
import mirror.vbox.app.ContextImpl;
import mirror.vbox.app.ContextImplKitkat;
import mirror.vbox.app.IActivityManager;
import mirror.vbox.app.LoadedApk;
import mirror.vbox.app.LoadedApkICS;
import mirror.vbox.app.LoadedApkKitkat;
import mirror.vbox.content.ContentProviderHolderOreo;
import mirror.vbox.content.res.CompatibilityInfo;
import mirror.vbox.providers.Settings;
import mirror.vbox.renderscript.RenderScriptCacheDir;
import mirror.vbox.security.net.config.ApplicationConfig;
import mirror.vbox.security.net.config.NetworkSecurityConfigProvider;
import mirror.vbox.view.CompatibilityInfoHolder;
import mirror.vbox.view.DisplayAdjustments;
import mirror.vbox.view.HardwareRenderer;
import mirror.vbox.view.RenderScript;
import mirror.vbox.view.ThreadedRenderer;
import mirror.com.android.internal.content.ReferrerIntent;
import mirror.dalvik.system.VMRuntime;
import mirror.java.lang.ThreadGroupN;

import static com.fun.vbox.client.core.VCore.getConfig;
import static com.fun.vbox.helper.compat.ActivityManagerCompat.SERVICE_DONE_EXECUTING_ANON;
import static com.fun.vbox.helper.compat.ActivityManagerCompat.SERVICE_DONE_EXECUTING_START;
import static com.fun.vbox.helper.compat.ActivityManagerCompat.SERVICE_DONE_EXECUTING_STOP;
import static com.fun.vbox.os.VUserHandle.getUserId;
import static com.fun.vbox.remote.InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK;

/**
 * @author Lody
 */

public final class VClient extends IVClient.Stub {

    private static final int NEW_INTENT = 11;
    private static final int RECEIVER = 12;
    private static final int FINISH_ACTIVITY = 13;
    private static final int CREATE_SERVICE = 14;
    private static final int SERVICE_ARGS = 15;
    private static final int STOP_SERVICE = 16;
    private static final int BIND_SERVICE = 17;
    private static final int UNBIND_SERVICE = 18;

    private static final String TAG = VClient.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static final VClient gClient = new VClient();

    private final H mH = new H();
    private final ArrayMap<IBinder, Service> mServices = new ArrayMap<>();
    private Instrumentation mInstrumentation = AppInstrumentation.getDefault();
    private ClientConfig clientConfig;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
    private CrashHandler crashHandler;
    private InstalledAppInfo mAppInfo;
    private int mTargetSdkVersion;
    private ConditionVariable mBindingApplicationLock;
    private boolean mEnvironmentPrepared = false;

    public InstalledAppInfo getAppInfo() {
        return mAppInfo;
    }

    public static VClient get() {
        return gClient;
    }

    public boolean isEnvironmentPrepared() {
        return mEnvironmentPrepared;
    }

    public boolean isAppUseOutsideAPK() {
        InstalledAppInfo appInfo = getAppInfo();
        return appInfo != null && appInfo.appMode == MODE_APP_USE_OUTSIDE_APK;
    }

    public VDeviceConfig getDeviceConfig() {
        return VDeviceManager.get().getDeviceConfig(getUserId(getVUid()));
    }

    public Application getCurrentApplication() {
        return mInitialApplication;
    }

    public String getCurrentPackage() {
        return mBoundApplication != null ?
                mBoundApplication.appInfo.packageName :
                VPackageManager.get().getNameForUid(getVUid());
    }

    public ApplicationInfo getCurrentApplicationInfo() {
        return mBoundApplication != null ? mBoundApplication.appInfo : null;
    }

    public int getCurrentTargetSdkVersion() {
        return mTargetSdkVersion == 0 ?
                VCore.get().getTargetSdkVersion()
                : mTargetSdkVersion;
    }

    public CrashHandler getCrashHandler() {
        return crashHandler;
    }

    public void setCrashHandler(CrashHandler crashHandler) {
        this.crashHandler = crashHandler;
    }

    public int getVUid() {
        if (clientConfig == null) {
            return 0;
        }
        return clientConfig.vuid;
    }

    /**
     * $Px
     * 0-99
     */
    public int getVpid() {
        if (clientConfig == null) {
            return 0;
        }
        return clientConfig.vpid;
    }

    public int getBaseVUid() {
        if (clientConfig == null) {
            return 0;
        }
        return VUserHandle.getAppId(clientConfig.vuid);
    }

    public int getCallingVUid() {
        return VActivityManager.get().getCallingUid();
    }

    public ClassLoader getClassLoader(ApplicationInfo appInfo) {
        Context context = createPackageContext(appInfo.packageName);
        return context.getClassLoader();
    }

    private void sendMessage(int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        mH.sendMessage(msg);
    }

    @Override
    public IBinder getAppThread() {
        return ActivityThread.getApplicationThread.call(VCore.mainThread());
    }

    @Override
    public IBinder getToken() {
        if (clientConfig == null) {
            return null;
        }
        return clientConfig.token;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    @Override
    public boolean isAppRunning() {
        return mBoundApplication != null;
    }

    public void initProcess(ClientConfig clientConfig) {
        if (this.clientConfig != null) {
            Log.e(TAG,
                    "reject init process: " + clientConfig.processName + ", this process is : " +
                            this.clientConfig.processName);
        }
        this.clientConfig = clientConfig;
    }

    private void handleNewIntent(NewIntentData data) {
        Intent intent;
        ComponentUtils.unpackFillIn(data.intent, get().getClassLoader());
        if (Build.VERSION.SDK_INT >= 22) {
            intent = ReferrerIntent.ctor.newInstance(data.intent, data.creator);
        } else {
            intent = data.intent;
        }
        if (ActivityThread.performNewIntents != null) {
            ActivityThread.performNewIntents.call(VCore.mainThread(), data.token, Collections.singletonList(intent));
        } else if (ActivityThreadNMR1.performNewIntents != null) {
            ActivityThreadNMR1.performNewIntents.call(VCore.mainThread(), data.token, Collections.singletonList(intent), true);
        } else if (BuildCompat.isS()) {
            Object obj = ActivityThread.mActivities.get(VCore.mainThread()).get(data.token);
            if (obj != null) {
                ActivityThread.handleNewIntent(obj, Collections.singletonList(intent));
            }
        } else {
            ActivityThreadQ.handleNewIntent.call(VCore.mainThread(), data.token, Collections.singletonList(intent));
        }


        /*

        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent = ReferrerIntent.ctor.newInstance(data.intent, data.creator);
        } else {
            intent = data.intent;
        }
        if (ActivityThread.performNewIntents != null) {
            ActivityThread.performNewIntents.call(
                    VCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent)
            );
        } else if (ActivityThreadNMR1.performNewIntents != null) {
            ActivityThreadNMR1.performNewIntents.call(
                    VCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent),
                    true);
        }else if (BuildCompat.isS()) {
//            data = (NewIntentData)(ActivityThread.mActivities.get(VCore.mainThread())).get(data.token);//todo  android.app.ActivityThread$ActivityClientRecord cannot be cast to com.fun.vbox.client.VClient$NewIntentData
//            if (data != null)
//                ActivityThread.handleNewIntent(data, Collections.singletonList(intent));
//            Map activities = ActivityThread.mActivities.get(ActivityThread.currentActivityThread.call(null));
//            ActivityThread.handleNewIntent.call(VCore.mainThread(), activities.get(data.token), Collections.singletonList(intent));


            Object obj = ActivityThread.mActivities.get(VCore.mainThread()).get(data.token);
            if (obj != null) {
                ActivityThread.handleNewIntent(obj, Collections.singletonList(intent));
            }

        } else {
            ActivityThreadQ.handleNewIntent.call(
                    VCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent));
        }

        */
    }

    public void bindApplication(final String packageName, final String processName) {
        if (clientConfig == null) {
            throw new RuntimeException("Unrecorded process: " + processName);
        }
        if (isAppRunning()) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            if (mBindingApplicationLock != null) {
                mBindingApplicationLock.block();
                mBindingApplicationLock = null;
            } else {
                mBindingApplicationLock = new ConditionVariable();
            }
            VirtualRuntime.getUIHandler().post(new Runnable() {
                @Override
                public void run() {
                    bindApplicationNoCheck(packageName, processName);
                    ConditionVariable lock = mBindingApplicationLock;
                    mBindingApplicationLock = null;
                    if (lock != null) {
                        lock.open();
                    }
                }
            });
            if (mBindingApplicationLock != null) {
                mBindingApplicationLock.block();
            }
        } else {
            bindApplicationNoCheck(packageName, processName);
        }
    }

    private void bindApplicationNoCheck(String packageName, String processName) {
        if (isAppRunning()) {
            return;
        }
        if (processName == null) {
            processName = packageName;
        }
        try {
            setupUncaughtHandler();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final int userId = getUserId(getVUid());
        try {
            fixInstalledProviders();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        VDeviceConfig deviceConfig = getDeviceConfig();
        VDeviceManager.get().applyBuildProp(deviceConfig);
        final boolean is64Bit = VCore.get().is64BitEngine();
        // Fix: com.loafwallet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String entry = aliases.nextElement();
                    VLog.w(TAG, "remove entry: " + entry);
                    keyStore.deleteEntry(entry);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        ActivityThread.mInitialApplication.set(
                VCore.mainThread(),
                null
        );
        AppBindData data = new AppBindData();
        InstalledAppInfo info = VCore.get().getInstalledAppInfo(packageName, 0);
        if (info == null) {
            new Exception("app not exist").printStackTrace();
            Process.killProcess(0);
            System.exit(0);
        }
        mAppInfo = info;
        data.appInfo = VPackageManager.get().getApplicationInfo(packageName, 0, userId);
        data.processName = processName;
        data.providers = VPackageManager.get()
                .queryContentProviders(processName, getVUid(), PackageManager.GET_META_DATA);
        mTargetSdkVersion = data.appInfo.targetSdkVersion;
        VLog.i(TAG, "Binding application %s (%s [%d])", data.appInfo.packageName, data.processName,
                Process.myPid());
        mBoundApplication = data;
        VirtualRuntime.setupRuntime(data.processName, data.appInfo);
        if (VCore.get().is64BitEngine()) {
            File apkFile = new File(info.getApkPath());
            File libDir = new File(data.appInfo.nativeLibraryDir);
            if (!apkFile.exists()) {
                VCore.get().requestCopyPackage64(packageName);
            }
            String[] listFiles = libDir.list();
            if (listFiles == null || listFiles.length == 0) {
                NativeLibraryHelperCompat.copyNativeBinaries(apkFile, libDir);
            }
        }
        int targetSdkVersion = data.appInfo.targetSdkVersion;
        if (targetSdkVersion < Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy newPolicy =
                    new StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy())
                            .permitNetwork().build();
            StrictMode.setThreadPolicy(newPolicy);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (VCore.get().getTargetSdkVersion() >= Build.VERSION_CODES.N
                    && targetSdkVersion < Build.VERSION_CODES.N) {
                StrictModeCompat.disableDeathOnFileUriExposure();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                targetSdkVersion < Build.VERSION_CODES.LOLLIPOP) {
            mirror.vbox.os.Message.updateCheckRecycle.call(targetSdkVersion);
        }
        AlarmManager alarmManager =
                (AlarmManager) VCore.get().getContext().getSystemService(Context.ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (mirror.vbox.app.AlarmManager.mTargetSdkVersion != null) {
                try {
                    mirror.vbox.app.AlarmManager.mTargetSdkVersion
                            .set(alarmManager, targetSdkVersion);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //tmp dir
        if (is64Bit) {
            System.setProperty("java.io.tmpdir",
                    new File(VEnvironment.getDataUserPackageDirectory64(userId, info.packageName),
                            "cache").getAbsolutePath());
        } else {
            System.setProperty("java.io.tmpdir",
                    new File(VEnvironment.getDataUserPackageDirectory(userId, info.packageName),
                            "cache").getAbsolutePath());
        }
        if (getConfig().isEnableIORedirect()) {
            if (VCore.get().isIORelocateWork()) {
                startIORelocater(info, is64Bit);
            } else {
                VLog.w(TAG, "IO Relocate verify fail.");
            }
        }
        //NativeEngine.launchEngine();
        mEnvironmentPrepared = true;
        Object mainThread = VCore.mainThread();
        NativeEngine.startDexOverride();
        initDataStorage(is64Bit, userId, packageName);
        Context context = createPackageContext(data.appInfo.packageName);
        File codeCacheDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            codeCacheDir = context.getCodeCacheDir();
        } else {
            codeCacheDir = context.getCacheDir();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (HardwareRenderer.setupDiskCache != null) {
                HardwareRenderer.setupDiskCache.call(codeCacheDir);
            }
        } else {
            if (ThreadedRenderer.setupDiskCache != null) {
                ThreadedRenderer.setupDiskCache.call(codeCacheDir);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (RenderScriptCacheDir.setupDiskCache != null) {
                RenderScriptCacheDir.setupDiskCache.call(codeCacheDir);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (RenderScript.setupDiskCache != null) {
                RenderScript.setupDiskCache.call(codeCacheDir);
            }
        }
        mBoundApplication.info = ContextImpl.mPackageInfo.get(context);
        Object thread = VCore.mainThread();
        Object boundApp = mirror.vbox.app.ActivityThread.mBoundApplication.get(thread);
        mirror.vbox.app.ActivityThread.AppBindData.appInfo.set(boundApp, data.appInfo);
        mirror.vbox.app.ActivityThread.AppBindData.processName.set(boundApp, data.processName);
        mirror.vbox.app.ActivityThread.AppBindData.instrumentationName.set(
                boundApp,
                new ComponentName(data.appInfo.packageName, Instrumentation.class.getName())
        );
        mirror.vbox.app.ActivityThread.AppBindData.info.set(boundApp, data.info);
        ActivityThread.AppBindData.providers.set(boundApp, data.providers);
        if (LoadedApk.mSecurityViolation != null) {
            LoadedApk.mSecurityViolation.set(mBoundApplication.info, false);
        }
        VMRuntime.setTargetSdkVersion
                .call(VMRuntime.getRuntime.call(), data.appInfo.targetSdkVersion);

        Configuration configuration = context.getResources().getConfiguration();
        if (!is64Bit && info.flag == PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<String> supportAbiList = new LinkedList<>();
            for (String abi : Build.SUPPORTED_ABIS) {
                if (NativeLibraryHelperCompat.is32bitAbi(abi)) {
                    supportAbiList.add(abi);
                }
            }
            String[] supportAbis = supportAbiList.toArray(new String[0]);
            Reflect.on(Build.class).set("SUPPORTED_ABIS", supportAbis);
        }
        Object compatInfo = null;
        if (CompatibilityInfo.ctor != null) {
            compatInfo = CompatibilityInfo.ctor
                    .newInstance(data.appInfo, configuration.screenLayout,
                            configuration.smallestScreenWidthDp, false);
        }
        if (CompatibilityInfo.ctorLG != null) {
            compatInfo = CompatibilityInfo.ctorLG
                    .newInstance(data.appInfo, configuration.screenLayout,
                            configuration.smallestScreenWidthDp, false, 0);
        }

        if (compatInfo != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    DisplayAdjustments.setCompatibilityInfo
                            .call(ContextImplKitkat.mDisplayAdjustments.get(context), compatInfo);
                }
                DisplayAdjustments.setCompatibilityInfo
                        .call(LoadedApkKitkat.mDisplayAdjustments.get(mBoundApplication.info),
                                compatInfo);
            } else {
                CompatibilityInfoHolder.set
                        .call(LoadedApkICS.mCompatibilityInfo.get(mBoundApplication.info),
                                compatInfo);
            }
        }
        fixSystem();
        VCore.get().getAppCallback().beforeStartApplication(packageName, processName, context);
        if (NetworkSecurityConfigProvider.install != null) {
            Security.removeProvider("AndroidNSSP");
            NetworkSecurityConfigProvider.install.call(context);
        }
        if (Build.VERSION.SDK_INT >= 30){
            ApplicationConfig.setDefaultInstance.call(new Object[] { null });
        }
        //fix junit not found class (for wechat)
        /*
        if (BuildCompat.isR() && data.appInfo.targetSdkVersion < 30) {
            ClassLoader classLoader = LoadedApk.getClassLoader.call(data.info, new Object[0]);
            if (Build.VERSION.SDK_INT >= 30){
                Reflect.on(classLoader).set("parent", new ClassLoader() {
                    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                        if(name.startsWith("junit")
//                                || name.startsWith("org.apache.http")|| name.startsWith("org.apache.commons")
//                                || name.startsWith("com.tencent.mobileqq")
//                                || name.startsWith("java.nio")
                        ){
                            return VClient.class.getClassLoader().loadClass(name);
                        }
                        return  super.loadClass(name, resolve);
                    }
                });
            }

        }
        */

         // Android 12: Fix junit
        // ClassLoaderContext type mismatch. expected=PCL, found=DLC (PCL[] | DLC[])  这样会有警告
/*        ClassLoader cl = LoadedApk.getClassLoader.call(data.info);
        if (BuildCompat.isS()) {
            ClassLoader parent = cl.getParent();
            Reflect.on(cl).set("parent", new DelegateLastClassLoader("/system/framework/android.test.base.jar", parent));
        }*/

        try {
            mInitialApplication = LoadedApk.makeApplication.callWithException(data.info, false, null);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to makeApplication", e);
        }
        mirror.vbox.app.ActivityThread.mInitialApplication.set(mainThread, mInitialApplication);
        ContextFixer.fixContext(mInitialApplication);
        if (Build.VERSION.SDK_INT >= 24 && "com.tencent.mm:recovery".equals(processName)) {
            fixWeChatRecovery(mInitialApplication);
        }
        if (GmsSupport.VENDING_PKG.equals(packageName)) {
            try {
                context.getSharedPreferences("vending_preferences", 0)
                        .edit()
                        .putBoolean("notify_updates", false)
                        .putBoolean("notify_updates_completion", false)
                        .apply();
                context.getSharedPreferences("finsky", 0)
                        .edit()
                        .putBoolean("auto_update_enabled", false)
                        .apply();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        /*
         * Support Atlas plugin framework
         * see:
         * https://github.com/alibaba/atlas/blob/master/atlas-core/src/main/java/android/taobao/atlas/bridge/BridgeApplicationDelegate.java
         */
        List<ProviderInfo> providers = ActivityThread.AppBindData.providers.get(boundApp);
        if (providers != null && !providers.isEmpty()) {
            installContentProviders(mInitialApplication, providers);
        }
        VCore.get().getAppCallback()
                .beforeApplicationCreate(packageName, processName, mInitialApplication);
        try {
            mInstrumentation.callApplicationOnCreate(mInitialApplication);
            InvocationStubManager.getInstance().checkEnv(HCallbackStub.class);
            Application createdApp = ActivityThread.mInitialApplication.get(mainThread);
            if (createdApp != null) {
                mInitialApplication = createdApp;
            }
        } catch (Exception e) {
            if (!mInstrumentation.onException(mInitialApplication, e)) {
                throw new RuntimeException(
                        "Unable to create application " + data.appInfo.name + ": " + e.toString(),
                        e);
            }
        }
        VCore.get().getAppCallback()
                .afterApplicationCreate(packageName, processName, mInitialApplication);
        StaticReceiverSystem.get().attach(processName, context, data.appInfo, userId);
        VActivityManager.get().appDoneExecuting(info.packageName);
    }



    private void initDataStorage(boolean is64bit, int userId, String pkg) {
        // ensure dir created
        if (is64bit) {
            VEnvironment.getDataUserPackageDirectory64(userId, pkg);
            VEnvironment.getDeDataUserPackageDirectory64(userId, pkg);
        } else {
            VEnvironment.getDataUserPackageDirectory(userId, pkg);
            VEnvironment.getDeDataUserPackageDirectory(userId, pkg);
        }
    }

    private void fixWeChatRecovery(Application app) {
        try {
            Field field = app.getClassLoader().loadClass("com.tencent.recovery.Recovery")
                    .getField("context");
            field.setAccessible(true);
            if (field.get(null) != null) {
                return;
            }
            field.set(null, app.getBaseContext());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void setupUncaughtHandler() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        ThreadGroup newRoot = new RootThreadGroup(root);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            final List<ThreadGroup> groups = mirror.java.lang.ThreadGroup.groups.get(root);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (groups) {
                List<ThreadGroup> newGroups = new ArrayList<>(groups);
                newGroups.remove(newRoot);
                mirror.java.lang.ThreadGroup.groups.set(newRoot, newGroups);
                groups.clear();
                groups.add(newRoot);
                mirror.java.lang.ThreadGroup.groups.set(root, groups);
                for (ThreadGroup group : newGroups) {
                    if (group == newRoot) {
                        continue;
                    }
                    mirror.java.lang.ThreadGroup.parent.set(group, newRoot);
                }
            }
        } else {
            final ThreadGroup[] groups = ThreadGroupN.groups.get(root);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (groups) {
                ThreadGroup[] newGroups = groups.clone();
                ThreadGroupN.groups.set(newRoot, newGroups);
                ThreadGroupN.groups.set(root, new ThreadGroup[]{newRoot});
                for (Object group : newGroups) {
                    if (group == null) {
                        continue;
                    }
                    if (group == newRoot) {
                        continue;
                    }
                    ThreadGroupN.parent.set(group, newRoot);
                }
                ThreadGroupN.ngroups.set(root, 1);
            }
        }
    }

    @SuppressLint("SdCardPath")
    private void startIORelocater(InstalledAppInfo info, boolean is64bit) {
        String packageName = info.packageName;
        if ("com.tencent.mm".equals(packageName)) {
            if (mBoundApplication.appInfo.targetSdkVersion < 26) {
                NativeEngine.whitelist("/storage/emulated/0/tencent/MicroMsg/WeiXin/");
                NativeEngine.whitelist("/sdcard/tencent/MicroMsg/WeiXin/");
                File file = new File("/sdcard/tencent/MicroMsg/WeiXin/");
                if (!file.exists()) {
                    file.mkdirs();
                }
                NativeEngine.whitelist("/storage/emulated/0/tencent/MicroMsg/Download/");
                NativeEngine.whitelist("/sdcard/tencent/MicroMsg/Download/");
                File file2 = new File("/sdcard/tencent/MicroMsg/Download/");
                if (!file2.exists()) {
                    file2.mkdirs();
                }
            }
        }

        int userId = VUserHandle.myUserId();
        String dataDir, de_dataDir, libPath;
        if (is64bit) {
            dataDir = VEnvironment.getDataUserPackageDirectory64(userId, packageName).getPath();
            de_dataDir =
                    VEnvironment.getDeDataUserPackageDirectory64(userId, packageName).getPath();
            libPath = VEnvironment.getAppLibDirectory64(packageName).getAbsolutePath();
        } else {
            dataDir = VEnvironment.getDataUserPackageDirectory(userId, packageName).getPath();
            de_dataDir = VEnvironment.getDeDataUserPackageDirectory(userId, packageName).getPath();
            libPath = VEnvironment.getAppLibDirectory(packageName).getAbsolutePath();
        }
        VDeviceConfig deviceConfig = getDeviceConfig();
        if (deviceConfig.enable) {
            File wifiMacAddressFile = getDeviceConfig().getWifiFile(userId, is64bit);
            if (wifiMacAddressFile != null && wifiMacAddressFile.exists()) {
                String wifiMacAddressPath = wifiMacAddressFile.getPath();
                NativeEngine.redirectFile("/sys/class/net/wlan0/address", wifiMacAddressPath);
                NativeEngine.redirectFile("/sys/class/net/eth0/address", wifiMacAddressPath);
                NativeEngine.redirectFile("/sys/class/net/wifi/address", wifiMacAddressPath);
            }
        }
        LinuxCompat.forgeProcDriver(is64bit);
        forbidHost();
        String cache = new File(dataDir, "cache").getAbsolutePath();
        NativeEngine.redirectDirectory("/tmp/", cache);
        NativeEngine.redirectDirectory("/data/data/" + packageName, dataDir);
        NativeEngine.redirectDirectory("/data/user/0/" + packageName, dataDir);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NativeEngine.redirectDirectory("/data/user_de/0/" + packageName, de_dataDir);
        }
        SettingConfig.AppLibConfig appLibConfig = getConfig().getAppLibConfig(packageName);

        if (appLibConfig == SettingConfig.AppLibConfig.UseRealLib) {
            if (info.appMode != MODE_APP_USE_OUTSIDE_APK
                    || !VCore.get().isOutsideInstalled(info.packageName)) {
                appLibConfig = SettingConfig.AppLibConfig.UseOwnLib;
            }
        }
        NativeEngine.whitelist(libPath);
        NativeEngine.whitelist("/data/user/0/" + packageName + "/lib/");
        if (appLibConfig == SettingConfig.AppLibConfig.UseOwnLib) {
            NativeEngine.redirectDirectory("/data/data/" + packageName + "/lib/", libPath);
            NativeEngine.redirectDirectory("/data/user/0/" + packageName + "/lib/", libPath);
        }
        File userLibDir = VEnvironment.getUserAppLibDirectory(userId, packageName);
        NativeEngine.redirectDirectory(userLibDir.getPath(), libPath);
        VirtualStorageManager vsManager = VirtualStorageManager.get();
        String vsPath = vsManager.getVirtualStorage(info.packageName, userId);
        boolean enable = vsManager.isVirtualStorageEnable(info.packageName, userId);
        if (enable && vsPath != null) {
            File vsDirectory = new File(vsPath);
            if (vsDirectory.exists() || vsDirectory.mkdirs()) {
                HashSet<String> mountPoints = getMountPoints();
                for (String mountPoint : mountPoints) {
                    NativeEngine.redirectDirectory(mountPoint, vsPath);
                }
            }
        } else if (BuildCompat.isR()) {
            String externalSdPath = VEnvironment.n(info.packageName).getAbsolutePath();
            File file = new File(externalSdPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            String str = "/storage/emulated/0/Android/data/" + info.packageName + "/";
            NativeEngine.redirectDirectory(str, externalSdPath);

            // if (!"com.tencent.mm".equals(packageName)) {
            //     NativeEngine.redirectDirectory("/sdcard/", VEnvironment.m().getAbsolutePath());
            //     NativeEngine
            //             .redirectDirectory("/storage/emulated/0/",
            //                     VEnvironment.m().getAbsolutePath());
            // }
        }
        NativeEngine.enableIORedirect();
    }

    private void forbidHost() {
        ActivityManager am = (ActivityManager) VCore.get().getContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == Process.myPid()) {
                continue;
            }
            if (info.uid != VCore.get().myUid()) {
                continue;
            }
            if (VActivityManager.get().isAppPid(info.pid)) {
                continue;
            }
            if (info.processName.startsWith(StubManifest.PACKAGE_NAME)
                    || StubManifest.PACKAGE_NAME_64BIT != null
                    && info.processName.startsWith(StubManifest.PACKAGE_NAME_64BIT)) {
                NativeEngine.forbid("/proc/" + info.pid + "/maps", true);
                NativeEngine.forbid("/proc/" + info.pid + "/cmdline", true);
            }
        }
    }

    @SuppressLint("SdCardPath")
    private HashSet<String> getMountPoints() {
        HashSet<String> mountPoints = new HashSet<>(3);
        mountPoints.add("/mnt/sdcard/");
        mountPoints.add("/sdcard/");
        mountPoints.add("/storage/emulated/0/");
        String[] points = StorageManagerCompat.getAllPoints(VCore.get().getContext());
        if (points != null) {
            Collections.addAll(mountPoints, points);
        }
        return mountPoints;

    }

    private Context createPackageContext(String packageName) {
        try {
            Context hostContext = VCore.get().getContext();
            return hostContext.createPackageContext(packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
        throw new RuntimeException();
    }

    private void installContentProviders(Context app, List<ProviderInfo> providers) {
        long origId = Binder.clearCallingIdentity();
        Object mainThread = VCore.mainThread();
        try {
            for (ProviderInfo cpi : providers) {
                try {
                    ActivityThread.installProvider(mainThread, app, cpi, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    @Override
    public IBinder acquireProviderClient(ProviderInfo info) {
        if (!isAppRunning()) {
            VClient.get().bindApplication(info.packageName, info.processName);
        }
        if (VClient.get().getCurrentApplication() == null) {
            return null;
        }
        IInterface provider = null;
        String[] authorities = info.authority.split(";");
        String authority = authorities.length == 0 ? info.authority : authorities[0];
        ContentResolver resolver = VCore.get().getContext().getContentResolver();
        ContentProviderClient client = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                client = resolver.acquireUnstableContentProviderClient(authority);
            } else {
                client = resolver.acquireContentProviderClient(authority);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (client != null) {
            provider = mirror.vbox.content.ContentProviderClient.mContentProvider.get(client);
            client.release();
        }
        IBinder binder = provider != null ? provider.asBinder() : null;
        if (binder != null) {
            if (binder instanceof Binder) {
                return new FakeIdentityBinder((Binder) binder);
            } else {
                VLog.e(TAG, "binder not instanceof Binder.");
                return binder;
            }
        }
        return null;
    }

    private void fixInstalledProviders() {
        clearSettingProvider();
        //noinspection unchecked
        Map<Object, Object> clientMap = ActivityThread.mProviderMap.get(VCore.mainThread());
        for (Map.Entry<Object, Object> e : clientMap.entrySet()) {
            Object clientRecord = e.getValue();
            if (BuildCompat.isOreo()) {
                IInterface provider =
                        ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = ContentProviderHolderOreo.info.get(holder);
                if (!info.authority.startsWith(StubManifest.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, info.authority, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    ContentProviderHolderOreo.provider.set(holder, provider);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                IInterface provider =
                        ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = IActivityManager.ContentProviderHolder.info.get(holder);
                if (!info.authority.startsWith(StubManifest.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, info.authority, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
            } else {
                String authority = ActivityThread.ProviderClientRecord.mName.get(clientRecord);
                IInterface provider =
                        ActivityThread.ProviderClientRecord.mProvider.get(clientRecord);
                if (provider != null && !authority.startsWith(StubManifest.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, authority, provider);
                    ActivityThread.ProviderClientRecord.mProvider.set(clientRecord, provider);
                }
            }
        }
    }

    private void clearSettingProvider() {
        Object cache;
        cache = Settings.System.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        cache = Settings.Secure.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                Settings.Global.TYPE != null) {
            cache = Settings.Global.sNameValueCache.get();
            if (cache != null) {
                clearContentProvider(cache);
            }
        }
    }

    private static void clearContentProvider(Object cache) {
        if (BuildCompat.isOreo()) {
            Object holder = Settings.NameValueCacheOreo.mProviderHolder.get(cache);
            if (holder != null) {
                Settings.ContentProviderHolder.mContentProvider.set(holder, null);
            }
        } else {
            Settings.NameValueCache.mContentProvider.set(cache, null);
        }
    }

    @Override
    public void finishActivity(IBinder token) {
        sendMessage(FINISH_ACTIVITY, token);
    }

    @Override
    public void scheduleNewIntent(String creator, IBinder token, Intent intent) {
        NewIntentData data = new NewIntentData();
        data.creator = creator;
        data.token = token;
        data.intent = intent;
        sendMessage(NEW_INTENT, data);
    }

    public void scheduleReceiver(String processName, ComponentName component, Intent intent,
                                 BroadcastReceiver.PendingResult pendingResult) {
        ReceiverData receiverData = new ReceiverData();
        receiverData.pendingResult = pendingResult;
        receiverData.intent = intent;
        receiverData.component = component;
        receiverData.processName = processName;
        receiverData.stacktrace = new Exception();
        sendMessage(RECEIVER, receiverData);
    }

    @Override
    public void scheduleCreateService(IBinder token, ServiceInfo info) {
        CreateServiceData data = new CreateServiceData();
        data.token = token;
        data.info = info;
        sendMessage(CREATE_SERVICE, data);
    }

    @Override
    public void scheduleBindService(IBinder token, Intent intent, boolean rebind) {
        BindServiceData data = new BindServiceData();
        data.token = token;
        data.intent = intent;
        data.rebind = rebind;
        sendMessage(BIND_SERVICE, data);
    }

    @Override
    public void scheduleUnbindService(IBinder token, Intent intent) {
        BindServiceData data = new BindServiceData();
        data.token = token;
        data.intent = intent;
        sendMessage(UNBIND_SERVICE, data);
    }

    @Override
    public void scheduleServiceArgs(IBinder token, int startId, Intent args) {
        ServiceArgsData data = new ServiceArgsData();
        data.token = token;
        data.startId = startId;
        data.args = args;
        sendMessage(SERVICE_ARGS, data);
    }

    @Override
    public void scheduleStopService(IBinder token) {
        sendMessage(STOP_SERVICE, token);
    }

    private void handleReceiver(ReceiverData data) {
        BroadcastReceiver.PendingResult result = data.pendingResult;
        try {
            Context context = mInitialApplication.getBaseContext();
            Context receiverContext = ContextImpl.getReceiverRestrictedContext.call(context);
            String className = data.component.getClassName();
            ClassLoader classLoader = LoadedApk.getClassLoader.call(mBoundApplication.info);
            BroadcastReceiver receiver =
                    (BroadcastReceiver) classLoader.loadClass(className).newInstance();
            mirror.vbox.content.BroadcastReceiver.setPendingResult.call(receiver, result);
            data.intent.setExtrasClassLoader(context.getClassLoader());
            if (data.intent.getComponent() == null) {
                data.intent.setComponent(data.component);
            }
            receiver.onReceive(receiverContext, data.intent);
            if (mirror.vbox.content.BroadcastReceiver.getPendingResult.call(receiver) != null) {
                result.finish();
            }
        } catch (Exception e) {
            data.stacktrace.printStackTrace();
            Log.e("myne", "Unable to start receiver " + data.component, e);
        }
        StaticReceiverSystem.get().broadcastFinish(data.pendingResult);
    }

    public ClassLoader getClassLoader() {
        return LoadedApk.getClassLoader.call(mBoundApplication.info);
    }

    private void handleCreateService(CreateServiceData data) {
        ServiceInfo info = data.info;
        if (!isAppRunning()) {
            bindApplication(info.packageName, info.processName);
        }
        ClassLoader classLoader = LoadedApk.getClassLoader.call(mBoundApplication.info);
        Service service = null;
        try {
            service = (Service) classLoader.loadClass(info.name).newInstance();
        } catch (Exception e) {
            Log.e("myne", "Unable to instantiate service " + info.name, e);
        }
        try {
            Context context = VCore.get().getContext().createPackageContext(
                    data.info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            ContextImpl.setOuterContext.call(context, service);
            mirror.vbox.app.Service.attach.call(
                    service,
                    context,
                    VCore.mainThread(),
                    info.name,
                    clientConfig.token,
                    mInitialApplication,
                    ActivityManagerNative.getDefault.call()
            );
            ContextFixer.fixContext(service);
            service.onCreate();
            mServices.put(data.token, service);
            VActivityManager.get()
                    .serviceDoneExecuting(data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
        } catch (Exception e) {
            Log.e("myne", "Unable to create service " + data.info.name, e);
        }
    }

    private void handleBindService(BindServiceData data) {
        Service s = mServices.get(data.token);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                if (!data.rebind) {
                    IBinder binder = s.onBind(data.intent);
                    VActivityManager.get().publishService(data.token, data.intent, binder);
                } else {
                    s.onRebind(data.intent);
                    VActivityManager.get().serviceDoneExecuting(
                            data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
                }
            } catch (Exception e) {
                Log.e("myne", "Unable to bind to service " + s, e);
            }
        }
    }

    private void handleUnbindService(BindServiceData data) {
        Service s = mServices.get(data.token);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                boolean doRebind = s.onUnbind(data.intent);
                if (doRebind) {
                    VActivityManager.get().unbindFinished(
                            data.token, data.intent, true);
                } else {
                    VActivityManager.get().serviceDoneExecuting(
                            data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
                }
            } catch (Exception e) {
                Log.e("myne", "Unable to unbind to service " + s, e);
            }
        }
    }

    private void handleServiceArgs(ServiceArgsData data) {
        Service s = mServices.get(data.token);
        if (s != null) {
            try {
                if (data.args != null) {
                    data.args.setExtrasClassLoader(s.getClassLoader());
                }
                int res;
                if (!data.taskRemoved) {
                    res = s.onStartCommand(data.args, data.flags, data.startId);
                } else {
                    s.onTaskRemoved(data.args);
                    res = 0;
                }
                VActivityManager.get().serviceDoneExecuting(
                        data.token, SERVICE_DONE_EXECUTING_START, data.startId, res);
            } catch (Exception e) {
                Log.e("myne", "Unable to start service " + s, e);
            }
        }
    }

    private void handleStopService(IBinder token) {
        Service s = mServices.remove(token);
        if (s != null) {
            try {
                s.onDestroy();
                VActivityManager.get().serviceDoneExecuting(
                        token, SERVICE_DONE_EXECUTING_STOP, 0, 0);
            } catch (Exception e) {
                if (!mInstrumentation.onException(s, e)) {
                    Log.e("myne", "Unable to stop service " + s, e);
                }
            }
        }
    }

    public Service createService(ServiceInfo info, IBinder token) {
        if (!isAppRunning()) {
            bindApplication(info.packageName, info.processName);
        }
        ClassLoader classLoader = LoadedApk.getClassLoader.call(mBoundApplication.info);
        Service service;
        try {
            service = (Service) classLoader.loadClass(info.name).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to instantiate service " + info.name
                            + ": " + e.toString(), e);
        }
        try {
            Context context = VCore.get().getContext().createPackageContext(
                    info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            ContextImpl.setOuterContext.call(context, service);
            mirror.vbox.app.Service.attach.call(
                    service,
                    context,
                    VCore.mainThread(),
                    info.name,
                    token,
                    mInitialApplication,
                    ActivityManagerNative.getDefault.call()
            );
            ContextFixer.fixContext(service);
            service.onCreate();
            return service;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to create service " + info.name
                            + ": " + e.toString(), e);
        }
    }

    @Override
    public IBinder createProxyService(ComponentName component, IBinder binder) {
        return ProxyServiceFactory.getProxyService(getCurrentApplication(), component, binder);
    }

    @Override
    public String getDebugInfo() {
        return VirtualRuntime.getProcessName();
    }

    private static class RootThreadGroup extends ThreadGroup {

        RootThreadGroup(ThreadGroup parent) {
            super(parent, "VA");
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            CrashHandler handler = VClient.gClient.crashHandler;
            if (handler != null) {
                handler.handleUncaughtException(t, e);
            } else {
                VLog.e("uncaught", e);
                System.exit(0);
            }
        }
    }

    private final class NewIntentData {
        String creator;
        IBinder token;
        Intent intent;
    }

    private final class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }

    private final class ReceiverData {
        BroadcastReceiver.PendingResult pendingResult;
        Intent intent;
        ComponentName component;
        String processName;
        Throwable stacktrace;
    }

    static final class CreateServiceData {
        IBinder token;
        ServiceInfo info;
    }

    static final class BindServiceData {
        IBinder token;
        Intent intent;
        boolean rebind;
    }

    static final class ServiceArgsData {
        IBinder token;
        boolean taskRemoved;
        int startId;
        int flags;
        Intent args;
    }

    @SuppressLint("HandlerLeak")
    private class H extends Handler {

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEW_INTENT: {
                    handleNewIntent((NewIntentData) msg.obj);
                    break;
                }
                case RECEIVER: {
                    handleReceiver((ReceiverData) msg.obj);
                    break;
                }
                case FINISH_ACTIVITY: {
                    VActivityManager.get().finishActivity((IBinder) msg.obj);
                    break;
                }
                case CREATE_SERVICE: {
                    handleCreateService((CreateServiceData) msg.obj);
                    break;
                }
                case SERVICE_ARGS: {
                    handleServiceArgs((ServiceArgsData) msg.obj);
                    break;
                }
                case STOP_SERVICE: {
                    handleStopService((IBinder) msg.obj);
                    break;
                }
                case BIND_SERVICE: {
                    handleBindService((BindServiceData) msg.obj);
                    break;
                }
                case UNBIND_SERVICE: {
                    handleUnbindService((BindServiceData) msg.obj);
                    break;
                }
            }
        }
    }

    private void fixSystem() {
        if (BuildCompat.isS()) {
            try {
                Reflect.on((Class<?>) Canvas.class).call("setCompatibilityVersion", 26);
            } catch (Exception e) {
            }
        }

    }
}
