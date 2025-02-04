package com.fun.vbox.client.hook.proxies.am;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.text.TextUtils;

import com.fun.vbox.client.NativeEngine;
import com.fun.vbox.client.VClient;
import com.fun.vbox.client.badger.BadgerManager;
import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.env.Constants;
import com.fun.vbox.client.env.SpecialComponentList;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.delegate.TaskDescriptionDelegate;
import com.fun.vbox.client.hook.providers.ProviderHook;
import com.fun.vbox.client.hook.secondary.ServiceConnectionDelegate;
import com.fun.vbox.client.hook.utils.MethodParameterUtils;
import com.fun.vbox.client.ipc.VActivityManager;
import com.fun.vbox.client.ipc.VNotificationManager;
import com.fun.vbox.client.ipc.VPackageManager;
import com.zb.vv.client.stub.ChooserActivity;
import com.fun.vbox.client.stub.StubManifest;
import com.fun.vbox.helper.compat.ActivityManagerCompat;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.helper.compat.BundleCompat;
import com.fun.vbox.helper.compat.ParceledListSliceCompat;
import com.fun.vbox.helper.compat.ProxyFCPUriCompat;
import com.fun.vbox.helper.utils.ArrayUtils;
import com.fun.vbox.helper.utils.BitmapUtils;
import com.fun.vbox.helper.utils.ComponentUtils;
import com.fun.vbox.helper.utils.DrawableUtils;
import com.fun.vbox.helper.utils.FileUtils;
import com.fun.vbox.helper.utils.Reflect;
import com.fun.vbox.helper.utils.VLog;
import com.fun.vbox.os.VUserHandle;
import com.fun.vbox.os.VUserInfo;
import com.fun.vbox.os.VUserManager;
import com.fun.vbox.remote.AppTaskInfo;
import com.fun.vbox.remote.BroadcastIntentData;
import com.fun.vbox.remote.ClientConfig;
import com.fun.vbox.remote.IntentSenderData;
import com.fun.vbox.remote.IntentSenderExtData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import mirror.vbox.app.IActivityManager;
import mirror.vbox.app.LoadedApk;
import mirror.vbox.content.ContentProviderHolderOreo;
import mirror.vbox.content.IIntentReceiverJB;
import mirror.vbox.content.pm.ParceledListSlice;
import mirror.vbox.content.pm.UserInfo;

/**
 * @author Lody
 */
@SuppressWarnings("unused")
public class MethodProxies {

    static class FinishReceiver extends MethodProxy {
        @Override
        public String getMethodName() {
            return "finishReceiver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
//            if (args[0] instanceof IBinder) {
//                IBinder token = (IBinder) args[0];
//            }
            return super.call(who, method, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GetRecentTasks extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getRecentTasks";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Object _infos = method.invoke(who, args);
            //noinspection unchecked
            List<ActivityManager.RecentTaskInfo> infos =
                    ParceledListSliceCompat.isReturnParceledListSlice(method)
                            ? ParceledListSlice.getList.call(_infos)
                            : (List) _infos;
            for (ActivityManager.RecentTaskInfo info : infos) {
                AppTaskInfo taskInfo = VActivityManager.get().getTaskInfo(info.id);
                if (taskInfo == null) {
                    continue;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        info.topActivity = taskInfo.topActivity;
                        info.baseActivity = taskInfo.baseActivity;
                    } catch (Throwable e) {
                        // ignore
                    }
                }
                try {
                    info.origActivity = taskInfo.baseActivity;
                    info.baseIntent = taskInfo.baseIntent;
                } catch (Throwable e) {
                    // ignore
                }
            }
            return _infos;
        }
    }

    static class ForceStopPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "forceStopPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            int userId = VUserHandle.myUserId();
            VActivityManager.get().killAppByPkg(pkg, userId);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class CrashApplication extends MethodProxy {

        @Override
        public String getMethodName() {
            return "crashApplication";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class AddPackageDependency extends MethodProxy {

        @Override
        public String getMethodName() {
            return "addPackageDependency";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GetPackageForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            String pkg = VActivityManager.get().getPackageForToken(token);
            if (pkg != null) {
                return pkg;
            }
            return super.call(who, method, args);
        }
    }

    static class UnbindService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IServiceConnection conn = (IServiceConnection) args[0];
            ServiceConnectionDelegate delegate = ServiceConnectionDelegate.removeDelegate(conn);
            if (delegate == null) {
                return method.invoke(who, args);
            }
            return VActivityManager.get().unbindService(delegate);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class GetContentProviderExternal extends GetContentProvider {

        @Override
        public String getMethodName() {
            return "getContentProviderExternal";
        }

        @Override
        public int getProviderNameIndex() {
            return BuildCompat.isQ() ? 1 : 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StartVoiceActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startVoiceActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class UnstableProviderDied extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unstableProviderDied";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[0] == null) {
                return 0;
            }
            return method.invoke(who, args);
        }
    }


    static class PeekService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "peekService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent service = (Intent) args[0];
            String resolvedType = (String) args[1];
            return VActivityManager.get().peekService(service, resolvedType);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetIntentSender extends MethodProxy {

        protected int mIntentIndex = 5;
        protected int mResolvedTypesIndex = 6;
        protected int mFlagsIndex = 7;

        @Override
        public String getMethodName() {
            return "getIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String creator = (String) args[1];
            args[1] = getHostPkg();
            // force userId to 0
            if (args[args.length - 1] instanceof Integer) {
                args[args.length - 1] = 0;
            }
            String[] resolvedTypes = (String[]) args[mResolvedTypesIndex];
            int indexOfFirst = ArrayUtils.indexOfFirst(args, IBinder.class);
            int type = (int) args[0];
            int flags = (int) args[mFlagsIndex];
            Intent[] intents = (Intent[]) args[mIntentIndex];

            int fillInFlags = Intent.FILL_IN_ACTION
                    | Intent.FILL_IN_DATA
                    | Intent.FILL_IN_CATEGORIES
                    | Intent.FILL_IN_COMPONENT
                    | Intent.FILL_IN_PACKAGE
                    | Intent.FILL_IN_SOURCE_BOUNDS
                    | Intent.FILL_IN_SELECTOR
                    | Intent.FILL_IN_CLIP_DATA;

            if (intents.length > 0) {
                Intent intent = intents[intents.length - 1];
                /*
                 * Fix:
                 * android.os.BadParcelableException: ClassNotFoundException when unmarshalling: meri.pluginsdk.PluginIntent
                 */
                intent = new Intent(intent);
                if (resolvedTypes != null && resolvedTypes.length >= intents.length) {
                    intent.setDataAndType(intent.getData(), resolvedTypes[intents.length - 1]);
                }
                Intent targetIntent = ComponentUtils.redirectIntentSender(type, creator, intent);
                if (targetIntent == null) {
                    return null;
                }
                args[mIntentIndex] = new Intent[]{targetIntent};
                args[mResolvedTypesIndex] = new String[]{null};
                args[mFlagsIndex] = flags & ~fillInFlags;
                IInterface sender = (IInterface) method.invoke(who, args);
                if (sender != null) {
                    IBinder token = sender.asBinder();
                    IntentSenderData data = new IntentSenderData(creator, token, intent, flags, type, VUserHandle.myUserId());
                    VActivityManager.get().addOrUpdateIntentSender(data);
                }
                return sender;
            }
            return method.invoke(who, args);

        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    public static class StartActivity extends MethodProxy {

        private static final String SCHEME_FILE = "file";
        private static final String SCHEME_PACKAGE = "package";
        private static final String SCHEME_CONTENT = "content";

        @Override
        public String getMethodName() {
            return "startActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int intentIndex = ArrayUtils.indexOfObject(args, Intent.class, 1);
            if (intentIndex < 0) {
                return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
            }
            int resultToIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            String resolvedType = (String) args[intentIndex + 1];
            Intent intent = (Intent) args[intentIndex];
            intent.setDataAndType(intent.getData(), resolvedType);
            IBinder resultTo = resultToIndex >= 0 ? (IBinder) args[resultToIndex] : null;
            String resultWho = null;
            int requestCode = 0;
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            if (resultTo != null) {
                resultWho = (String) args[resultToIndex + 1];
                requestCode = (int) args[resultToIndex + 2];
            }
            int userId = VUserHandle.myUserId();
            if ("android.intent.action.MAIN".equals(intent.getAction())
                    && intent.hasCategory("android.intent.category.HOME")) {
                Intent homeIntent = getConfig().onHandleLauncherIntent(intent);
                if (homeIntent != null) {
                    args[intentIndex] = homeIntent;
                }
                return method.invoke(who, args);
            }

            if (isHostIntent(intent)) {
                return method.invoke(who, args);
            }

            if (Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())
                    || (Intent.ACTION_VIEW.equals(intent.getAction())
                    && "application/vnd.android.package-archive".equals(intent.getType()))) {
                if (handleInstallRequest(intent)) {
                    if (resultTo != null && requestCode > 0) {
                        VActivityManager.get().sendCancelActivityResult(resultTo, resultWho, requestCode);
                    }
                    return 0;
                }
            } else if ((Intent.ACTION_UNINSTALL_PACKAGE.equals(intent.getAction())
                    || Intent.ACTION_DELETE.equals(intent.getAction()))
                    && "package".equals(intent.getScheme())) {

                if (handleUninstallRequest(intent)) {
                    return 0;
                }
            }
            String pkg = intent.getPackage();
            if (pkg != null && !isAppPkg(pkg)) {
                return method.invoke(who, args);
            }
            // chooser
            if (ChooserActivity.check(intent)) {
                intent = ComponentUtils.processOutsideIntent(userId, VCore.get().is64BitEngine(), intent);
                args[intentIndex] = intent;
                Bundle extras = new Bundle();
                extras.putInt(Constants.EXTRA_USER_HANDLE, userId);
                extras.putBundle(ChooserActivity.EXTRA_DATA, options);
                extras.putString(ChooserActivity.EXTRA_WHO, resultWho);
                extras.putInt(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
                BundleCompat.putBinder(extras, ChooserActivity.EXTRA_RESULTTO, resultTo);
                intent.setComponent(new ComponentName(StubManifest.PACKAGE_NAME, ChooserActivity.class.getName()));
                intent.setAction(null);
                intent.putExtras(extras);
                ProxyFCPUriCompat.get().fakeFileUri(intent);
                return method.invoke(who, args);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                args[intentIndex - 1] = getHostPkg();
            }
            if (intent.getScheme() != null && intent.getScheme().equals(SCHEME_PACKAGE) && intent.getData() != null) {
                if (intent.getAction() != null && intent.getAction().startsWith("android.settings.")) {
                    intent.setData(Uri.parse("package:" + getHostPkg()));
                }
            }
            ActivityInfo activityInfo = VCore.get().resolveActivityInfo(intent, userId);
            if (activityInfo == null) {
                VLog.e("VActivityManager", "Unable to resolve activityInfo : %s", intent);
                if (intent.getPackage() != null && isAppPkg(intent.getPackage())) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }
                //args[intentIndex] = ComponentUtils.processOutsideIntent(userId,
                        //VCore.get().is64BitEngine(), intent);
                args[intentIndex] = ProxyFCPUriCompat.get().fakeFileUri(intent);
                ResolveInfo resolveInfo = VCore.get().getUnHookPackageManager().resolveActivity(intent, 0);
                if (resolveInfo == null || resolveInfo.activityInfo == null) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }
                if (!Intent.ACTION_VIEW.equals(intent.getAction())
                        && !isVisiblePackage(resolveInfo.activityInfo.applicationInfo)) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }
                if((BuildCompat.isR()) && args[1] != null && (args[1] instanceof String)) {
                    args[1] = getHostPkg();
                }
                return method.invoke(who, args);
            }
            int res = VActivityManager.get().startActivity(intent, activityInfo, resultTo, options, resultWho, requestCode, VUserHandle.myUserId());
            if (res != 0 && resultTo != null && requestCode > 0) {
                VActivityManager.get().sendCancelActivityResult(resultTo, resultWho, requestCode);
            }
            return res;
        }


        private boolean handleInstallRequest(Intent intent) {
            VCore.AppRequestListener listener = VCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_FILE.equals(packageUri.getScheme())) {
                    File sourceFile = new File(packageUri.getPath());
                    String path = NativeEngine.getRedirectedPath(sourceFile.getAbsolutePath());
                    listener.onRequestInstall(path);
                    return true;
                } else if (SCHEME_CONTENT.equals(packageUri.getScheme())) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    File sharedFileCopy = new File(getHostContext().getCacheDir(), packageUri.getLastPathSegment());
                    try {
                        inputStream = getHostContext().getContentResolver().openInputStream(packageUri);
                        outputStream = new FileOutputStream(sharedFileCopy);
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, count);
                        }
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.closeQuietly(inputStream);
                        FileUtils.closeQuietly(outputStream);
                    }
                    listener.onRequestInstall(sharedFileCopy.getPath());
                    sharedFileCopy.delete();
                    return true;
                }
            }
            return false;
        }

        private boolean handleUninstallRequest(Intent intent) {
            VCore.AppRequestListener listener = VCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_PACKAGE.equals(packageUri.getScheme())) {
                    String pkg = packageUri.getSchemeSpecificPart();
                    listener.onRequestUninstall(pkg);
                    return true;
                }

            }
            return false;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    public static class StartActivities extends MethodProxy {

        @Override
        public String getMethodName() {
            return "startActivities";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            new Exception().printStackTrace();
            Intent[] intents = ArrayUtils.getFirst(args, Intent[].class);
            String[] resolvedTypes = ArrayUtils.getFirst(args, String[].class);
            IBinder token = null;
            int tokenIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            if (tokenIndex != -1) {
                token = (IBinder) args[tokenIndex];
            }
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            return VActivityManager.get().startActivities(intents, resolvedTypes, token, options, VUserHandle.myUserId());
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class ShouldUpRecreateTask extends MethodProxy {

        @Override
        public String getMethodName() {
            return "shouldUpRecreateTask";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return false;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    public static class GetCallingPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getCallingPackage(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPackageForIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getPackageForIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface sender = (IInterface) args[0];
            if (sender != null) {
                IntentSenderData data = VActivityManager.get().getIntentSender(sender.asBinder());
                if (data != null) {
                    return data.creator;
                }
            }
            Object a = super.call(who, method, args);
            if (!BuildCompat.isR()) {
                return a;
            }
            return getAppPkg();
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    @SuppressWarnings("unchecked")
    static class PublishContentProviders extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishContentProviders";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetServices extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getServices";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int maxNum = (int) args[0];
            int flags = (int) args[1];
            return VActivityManager.get().getServices(maxNum, flags).getList();
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GrantUriPermissionFromOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "grantUriPermissionFromOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class SetServiceForeground extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setServiceForeground";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (!getConfig().isAllowServiceStartForeground()) {
                return 0;
            }
            ComponentName component = (ComponentName) args[0];
            IBinder token = (IBinder) args[1];
            int id = (int) args[2];
            Notification notification = (Notification) args[3];
            boolean removeNotification = false;
            if (args[4] instanceof Boolean) {
                removeNotification = (boolean) args[4];
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && args[4] instanceof Integer) {
                int flags = (int) args[4];
                removeNotification = (flags & Service.STOP_FOREGROUND_REMOVE) != 0;
            } else {
                VLog.e(getClass().getSimpleName(), "Unknown flag : " + args[4]);
            }
            if (!VNotificationManager.get().dealNotification(id, notification, getAppPkg())) {
                notification = new Notification();
                notification.icon = getHostContext().getApplicationInfo().icon;
            }
            /**
             * `BaseStatusBar#updateNotification` aosp will use use
             * `new StatusBarIcon(...notification.getSmallIcon()...)`
             *  while in samsung SystemUI.apk ,the corresponding code comes as
             * `new StatusBarIcon(...pkgName,notification.icon...)`
             * the icon comes from `getSmallIcon.getResource`
             * which will throw an exception on :x process thus crash the application
             */
            if (notification != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (Build.BRAND.equalsIgnoreCase("samsung") || Build.MANUFACTURER.equalsIgnoreCase("samsung"))) {
                notification.icon = getHostContext().getApplicationInfo().icon;
                Icon icon = Icon.createWithResource(getHostPkg(), notification.icon);
                Reflect.on(notification).call("setSmallIcon", icon);
            }

//            VActivityManager.get().setServiceForeground(component, token, id, notification, removeNotification);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class UpdateDeviceOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "updateDeviceOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class GetIntentForIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentForIntentSender";
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) {
            Intent intent = (Intent) result;
            if (intent != null) {
                Intent selector = intent.getSelector();
                if (selector != null) {
                    Intent targetIntent = selector.getParcelableExtra("_VBOX_|_intent_");
                    if (targetIntent != null) {
                        return targetIntent;
                    }
                }
            }
            return intent;
        }
    }


    static class UnbindFinished extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindFinished";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            Intent service = (Intent) args[1];
            boolean doRebind = (boolean) args[2];
            VActivityManager.get().unbindFinished(token, service, doRebind);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    public static class StartActivityIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "startActivityIntentSender";
        }

        /*
        public int startActivityIntentSender(IApplicationThread caller, IntentSender intent,
                    Intent fillInIntent, String resolvedType, IBinder resultTo, String resultWho,
                    int requestCode, int flagsMask, int flagsValues, Bundle options)

        public int startActivityIntentSender(IApplicationThread caller, IIntentSender target,
                    IBinder whitelistToken, Intent fillInIntent, String resolvedType, IBinder resultTo,
                    String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle bOptions)
         */
        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int intentIndex;
            int resultToIndex;
            int resultWhoIndex;
            int optionsIndex;
            int requestCodeIndex;
            int flagsMaskIndex;
            int flagsValuesIndex;
            if (BuildCompat.isOreo()) {
                intentIndex = 3;
                resultToIndex = 5;
                resultWhoIndex = 6;
                requestCodeIndex = 7;
                flagsMaskIndex = 8;
                flagsValuesIndex = 9;
                optionsIndex = 10;
            } else {
                intentIndex = 2;
                resultToIndex = 4;
                resultWhoIndex = 5;
                requestCodeIndex = 6;
                flagsMaskIndex = 7;
                flagsValuesIndex = 8;
                optionsIndex = 9;
            }
            Object target = args[1];
            Intent originFillIn = (Intent) args[intentIndex];
            IBinder resultTo = (IBinder) args[resultToIndex];
            String resultWho = (String) args[resultWhoIndex];
            int requestCode = (int) args[requestCodeIndex];
            Bundle options = (Bundle) args[optionsIndex];
            int flagsMask = (int) args[flagsMaskIndex];
            int flagsValues = (int) args[flagsValuesIndex];
            Intent fillIn = new Intent();
            IInterface sender;
            if (target instanceof IInterface) {
                sender = (IInterface) target;
            } else {
                sender = mirror.vbox.content.IntentSender.mTarget.get(target);
            }
            fillIn.putExtra("_VBOX_|_ext_", new IntentSenderExtData(sender.asBinder(), originFillIn, resultTo, resultWho, requestCode, options, flagsMask, flagsValues));
            args[intentIndex] = fillIn;
            return super.call(who, method, args);
        }
    }

    static class SendIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "sendIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface sender = (IInterface) args[0];
            // after [parameter] code
            int intentIndex = ArrayUtils.indexOfObject(args, Integer.class, 1) + 1;
            Intent fillIn = (Intent) args[intentIndex];
            Bundle options = (Bundle) args[args.length - 1];
            int permissionIndex = args.length - 2;
            if (args[permissionIndex] instanceof String) {
                args[permissionIndex] = null;
            }
            IntentSenderExtData ext = new IntentSenderExtData(sender.asBinder(), fillIn, null, null, 0, options, 0, 0);
            Intent newFillIn = new Intent();
            newFillIn.putExtra("_VBOX_|_ext_", ext);
            args[intentIndex] = newFillIn;
            return super.call(who, method, args);
        }
    }



    static class BindService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "bindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            IBinder token = (IBinder) args[1];
            Intent service = (Intent) args[2];
            String resolvedType = (String) args[3];
            IServiceConnection conn = (IServiceConnection) args[4];
            int flags = (int) args[5];
            int userId = VUserHandle.myUserId();
            if (isServerProcess()) {
                userId = service.getIntExtra("_VBOX_|_user_id_", VUserHandle.USER_NULL);
            }
            if (userId == VUserHandle.USER_NULL) {
                MethodProxy.replaceLastUserId(args);
                return method.invoke(who, args);
            }
            ServiceInfo serviceInfo = VCore.get().resolveServiceInfo(service, userId);
            if (serviceInfo != null) {
                if (SpecialComponentList.FB_PACKAGE_NAME.equals(serviceInfo.packageName) && !SpecialComponentList.isFbEnable(VClient.get().getCurrentPackage())) {
                    return Integer.valueOf(0);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                }
                conn = ServiceConnectionDelegate.getDelegate(conn);
                return VActivityManager.get().bindService(caller.asBinder(), token, service, resolvedType,
                        conn, flags, userId);
            }
            ResolveInfo resolveInfo = VCore.get().getUnHookPackageManager().resolveService(service, 0);
            if (resolveInfo == null || !isVisiblePackage(resolveInfo.serviceInfo.applicationInfo)) {
                return 0;
            }
            MethodProxy.replaceLastUserId(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class BindIsolatedService extends BindService {
        BindIsolatedService() {
        }

        public String getMethodName() {
            return "bindIsolatedService";
        }

        public Object call(Object who, Method method, Object... args) throws Throwable {
            args[7] = getHostPkg();
            MethodProxy.replaceLastUserId(args);
            return super.call(who, method, args);
        }

        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class StartService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "startService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent service = (Intent) args[1];
            String resolvedType = (String) args[2];
            if (service == null) {
                return null;
            }
            if (service.getComponent() != null
                    && getHostPkg().equals(service.getComponent().getPackageName())) {
                MethodProxy.replaceLastUserId(args);
                return method.invoke(who, args);
            }
            int userId = VUserHandle.myUserId();
            if (isServerProcess()) {
                userId = service.getIntExtra("_VBOX_|_user_id_", VUserHandle.USER_NULL);
            }
            service.setDataAndType(service.getData(), resolvedType);
            ServiceInfo serviceInfo = VCore.get().resolveServiceInfo(service, VUserHandle.myUserId());
            if (serviceInfo != null) {
                return VActivityManager.get().startService(service, resolvedType, userId);
            }
            ResolveInfo resolveInfo = VCore.get().getUnHookPackageManager().resolveService(service, 0);
            if (resolveInfo == null || !isVisiblePackage(resolveInfo.serviceInfo.applicationInfo)) {
                return null;
            }
            MethodProxy.replaceLastUserId(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    public static class StartActivityAndWait extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityAndWait";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class PublishService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            Intent intent = (Intent) args[1];
            IBinder service = (IBinder) args[2];
            VActivityManager.get().publishService(token, intent, service);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    @SuppressWarnings("unchecked")
    static class GetRunningAppProcesses extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getRunningAppProcesses";
        }

        @Override
        public synchronized Object call(Object who, Method method, Object... args) throws Throwable {
            if (!VClient.get().isEnvironmentPrepared()) {
                return method.invoke(who, args);
            }
            List<ActivityManager.RunningAppProcessInfo> _infoList = (List<ActivityManager.RunningAppProcessInfo>) method
                    .invoke(who, args);
            if (_infoList == null) {
                return null;
            }
            List<ActivityManager.RunningAppProcessInfo> infoList = new ArrayList<>(_infoList);
            Iterator<ActivityManager.RunningAppProcessInfo> it = infoList.iterator();
            while (it.hasNext()) {
                ActivityManager.RunningAppProcessInfo info = it.next();
                if (info.uid == getRealUid()) {
                    if (VActivityManager.get().isAppPid(info.pid)) {
                        int vuid = VActivityManager.get().getUidByPid(info.pid);
                        int userId = VUserHandle.getUserId(vuid);
                        if (userId != getAppUserId()) {
                            it.remove();
                            continue;
                        }
                        List<String> pkgList = VActivityManager.get().getProcessPkgList(info.pid);
                        String processName = VActivityManager.get().getAppProcessName(info.pid);
                        if (processName != null) {
                            info.importanceReasonCode = 0;
                            info.importanceReasonPid = 0;
                            info.importanceReasonComponent = null;
                            info.processName = processName;
                        }
                        info.pkgList = pkgList.toArray(new String[0]);
                        //info.uid = vuid;
                    } else {
                        if (info.processName.startsWith(getConfig().getHostPackageName())
                                || info.processName.startsWith(getConfig().get64bitEnginePackageName())) {
                            it.remove();
                        }
                    }
                }
            }
            return infoList;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class SetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    public static class GetCallingActivity extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getCallingActivity(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetCurrentUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCurrentUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            try {
                return UserInfo.ctor.newInstance(0, "user", VUserInfo.FLAG_PRIMARY);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    static class KillApplicationProcess extends MethodProxy {

        @Override
        public String getMethodName() {
            return "killApplicationProcess";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args.length > 1 && args[0] instanceof String && args[1] instanceof Integer) {
                String processName = (String) args[0];
                int uid = (int) args[1];
                VActivityManager.get().killApplicationProcess(processName, uid);
                return 0;
            }
            return 0;//method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class KillBackgroundProcesses extends MethodProxy {
        @Override
        public String getMethodName() {
            return "killBackgroundProcesses";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[0] instanceof String) {
                String pkg = (String) args[0];
                VActivityManager.get().killAppByPkg(pkg, getAppUserId());
                return 0;
            }
            MethodProxy.replaceLastUserId(args);
            return super.call(who, method, args);
        }
    }


    public static class StartActivityAsUser extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodProxy.replaceLastUserId(args);
            return super.call(who, method, args);
        }
    }


    static class CheckPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String permission = (String) args[0];
            int pid = (int) args[1];
            int uid = (int) args[2];
            return VActivityManager.get().checkPermission(permission, pid, uid);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }

    static class CheckPermissionWithToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkPermissionWithToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String permission = (String) args[0];
            int pid = (int) args[1];
            int uid = (int) args[2];
            return VActivityManager.get().checkPermission(permission, pid, uid);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class StartActivityAsCaller extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsCaller";
        }
    }


    static class HandleIncomingUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "handleIncomingUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int lastIndex = args.length - 1;
            if (args[lastIndex] instanceof String) {
                args[lastIndex] = getHostPkg();
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    @SuppressWarnings("unchecked")
    public static class GetTasks extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getTasks";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = (List<ActivityManager.RunningTaskInfo>) method
                    .invoke(who, args);
            for (ActivityManager.RunningTaskInfo info : runningTaskInfos) {
                AppTaskInfo taskInfo = VActivityManager.get().getTaskInfo(info.id);
                if (taskInfo != null) {
                    info.topActivity = taskInfo.topActivity;
                    info.baseActivity = taskInfo.baseActivity;
                }
            }
            return runningTaskInfos;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPersistedUriPermissions extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPersistedUriPermissions";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    public static class RegisterReceiver extends MethodProxy {

        protected int mIIntentReceiverIndex = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 2
                : 1;

        protected int mRequiredPermissionIndex = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 4
                : 3;
        protected int mIntentFilterIndex = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 3
                : 2;
//        private static int IDX_IIntentReceiver;
//        private static int IDX_RequiredPermission;
//        private static int IDX_IntentFilter;
//
//        static {
//            if (BuildCompat.isR()) {
//                IDX_IIntentReceiver = 3;
//                IDX_RequiredPermission = 5;
//                IDX_IntentFilter = 4;
//            } else {
//                IDX_IIntentReceiver = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
//                        ? 2
//                        : 1;
//
//                IDX_RequiredPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
//                        ? 4
//                        : 3;
//
//                IDX_IntentFilter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
//                        ? 3
//                        : 2;
//            }
//        }

        private WeakHashMap<IBinder, IIntentReceiver> mProxyIIntentReceivers = new WeakHashMap<>();

        @Override
        public String getMethodName() {
            return "registerReceiver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodProxy.replaceFirstUserId(args);
            args[mRequiredPermissionIndex] = null;
            IntentFilter filter = (IntentFilter) args[mIntentFilterIndex];
            if (filter == null) {
                return method.invoke(who, args);
            }
            filter = new IntentFilter(filter);
            if (filter.hasCategory("__VBOX__|_static_receiver_")) {
                List<String> categories = mirror.vbox.content.IntentFilter.mCategories.get(filter);
                categories.remove("__VBOX__|_static_receiver_");
                return method.invoke(who, args);
            }
            SpecialComponentList.protectIntentFilter(filter);
            args[mIntentFilterIndex] = filter;
            if (args.length > mIIntentReceiverIndex && IIntentReceiver.class.isInstance(args[mIIntentReceiverIndex])) {
                final IInterface old = (IInterface) args[mIIntentReceiverIndex];
                if (!IIntentReceiverProxy.class.isInstance(old)) {
                    final IBinder token = old.asBinder();
                    if (token != null) {
                        token.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                token.unlinkToDeath(this, 0);
                                mProxyIIntentReceivers.remove(token);
                            }
                        }, 0);
                        IIntentReceiver proxyIIntentReceiver = mProxyIIntentReceivers.get(token);
                        if (proxyIIntentReceiver == null) {
                            proxyIIntentReceiver = new IIntentReceiverProxy(old, filter);
                            mProxyIIntentReceivers.put(token, proxyIIntentReceiver);
                        }
                        WeakReference mDispatcher = LoadedApk.ReceiverDispatcher.InnerReceiver.mDispatcher.get(old);
                        if (mDispatcher != null) {
                            LoadedApk.ReceiverDispatcher.mIIntentReceiver.set(mDispatcher.get(), proxyIIntentReceiver);
                            args[mIIntentReceiverIndex] = proxyIIntentReceiver;
                        }
                    }
                }
            }
            return method.invoke(who, args);
        }


        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

        private static class IIntentReceiverProxy extends IIntentReceiver.Stub {

            IInterface mOld;
            IntentFilter mFilter;

            IIntentReceiverProxy(IInterface old, IntentFilter filter) {
                this.mOld = old;
                this.mFilter = filter;
            }

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky, int sendingUser) {
                Bundle extraData = intent.getExtras();
                BroadcastIntentData intentData = null;
                if (extraData != null) {
                    extraData.setClassLoader(BroadcastIntentData.class.getClassLoader());
                    intentData = extraData.getParcelable("_VBOX_|_data_");
                }
                if (intentData != null) {
                    if (intentData.userId != VUserHandle.myUserId()) {
                        return;
                    }
                    intent = intentData.intent;
                } else {
                    SpecialComponentList.unprotectIntent(intent);
                }
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    IIntentReceiverJB.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky, sendingUser);
                } else {
                    mirror.vbox.content.IIntentReceiver.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky);
                }
            }

            @SuppressWarnings("unused")
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky) {
                this.performReceive(intent, resultCode, data, extras, ordered, sticky, 0);
            }

        }
    }


    static class StopService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            Intent intent = (Intent) args[1];
            String resolvedType = (String) args[2];
            intent.setDataAndType(intent.getData(), resolvedType);
            ComponentName componentName = intent.getComponent();
            PackageManager pm = VCore.getPM();
            if (componentName == null) {
                ResolveInfo resolveInfo = pm.resolveService(intent, 0);
                if (resolveInfo != null && resolveInfo.serviceInfo != null) {
                    componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                }
            }
            if (componentName != null && !getHostPkg().equals(componentName.getPackageName())) {
                return VActivityManager.get().stopService(caller, intent, resolvedType);
            }
            MethodProxy.replaceLastUserId(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }


    static class GetContentProvider extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getContentProvider";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Object obj = who;
            Method method2 = method;
            Object[] objArr = args;
            int nameIdx = getProviderNameIndex();
            String name = (String) objArr[nameIdx];
            if (name.startsWith(StubManifest.STUB_CP_AUTHORITY)) {
            } else if (name.startsWith(StubManifest.STUB_CP_AUTHORITY_64BIT)) {
            } else if (name.equals(getConfig().get64bitHelperAuthority())) {
            } else if (name.equals(getConfig().getBinderProviderAuthority())) {
                int i = nameIdx;
            } else {
                VLog.w("VActivityManger", "getContentProvider:%s", name);
                if (BuildCompat.isQ()) {
                    int pkgIdx = nameIdx - 1;
                    if (objArr[pkgIdx] instanceof String) {
                        objArr[pkgIdx] = getHostPkg();
                    }
                }
                int userId = VUserHandle.myUserId();
                ProviderInfo info = VPackageManager.get().resolveContentProvider(name, 0, userId);
                if (info != null && !info.enabled) {
                    return null;
                }
                if (info == null || !isAppPkg(info.packageName)) {
                    replaceLastUserId(args);
                    Object holder = method2.invoke(obj, objArr);
                    if (holder == null) {
                        return null;
                    }
                    if (BuildCompat.isOreo()) {
                        IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                        ProviderInfo info2 = ContentProviderHolderOreo.info.get(holder);
                        if (provider != null) {
                            provider = ProviderHook.createProxy(true, info2.authority, provider);
                        }
                        ContentProviderHolderOreo.provider.set(holder, provider);
                    } else {
                        IInterface provider2 = IActivityManager.ContentProviderHolder.provider.get(holder);
                        ProviderInfo info3 = IActivityManager.ContentProviderHolder.info.get(holder);
                        if (provider2 != null) {
                            provider2 = ProviderHook.createProxy(true, info3.authority, provider2);
                        }
                        IActivityManager.ContentProviderHolder.provider.set(holder, provider2);
                    }
                    return holder;
                }
                ClientConfig config = VActivityManager.get().initProcess(info.packageName, info.processName, userId);
                if (config == null) {
                    VLog.e("ActivityManager", "failed to initProcess for provider: " + name);
                    return null;
                }
                objArr[nameIdx] = StubManifest.getStubAuthority(config.vpid, config.is64Bit);
                replaceLastUserId(args);
                Object holder2 = method2.invoke(obj, objArr);
                if (holder2 == null) {
                    return null;
                }
                boolean maybeLoadingProvider = false;
                if (BuildCompat.isOreo()) {
                    IInterface provider3 = ContentProviderHolderOreo.provider.get(holder2);
                    if (provider3 != null) {
                        int i2 = nameIdx;
                        provider3 = VActivityManager.get().acquireProviderClient(userId, info);
                        if (BuildCompat.isS() && provider3 != null) {
                            provider3 = ProviderHook.createProxy(false, name, provider3);
                        }
                    } else {
                        maybeLoadingProvider = true;
                    }
                    if (provider3 != null) {
                        ContentProviderHolderOreo.provider.set(holder2, provider3);
                        ContentProviderHolderOreo.info.set(holder2, info);
                    } else if (maybeLoadingProvider) {
                        VLog.w("VActivityManager", "Loading provider: " + info.authority + "(" + info.processName + ")", new Object[0]);
                        ContentProviderHolderOreo.info.set(holder2, info);
                        return holder2;
                    } else {
                        VLog.e("VActivityManager", "acquireProviderClient fail: " + info.authority + "(" + info.processName + ")");
                        return null;
                    }
                } else {
                    IInterface provider4 = IActivityManager.ContentProviderHolder.provider.get(holder2);
                    if (provider4 != null) {
                        provider4 = VActivityManager.get().acquireProviderClient(userId, info);
                    } else {
                        maybeLoadingProvider = true;
                    }
                    if (provider4 != null) {
                        IActivityManager.ContentProviderHolder.provider.set(holder2, provider4);
                        IActivityManager.ContentProviderHolder.info.set(holder2, info);
                    } else if (!maybeLoadingProvider) {
                        VLog.e("VActivityManager", "acquireProviderClient fail: " + info.authority + "(" + info.processName + ")");
                        return null;
                    } else if (!BuildCompat.isMIUI() || !miuiProviderWaitingTargetProcess(holder2)) {
                        return null;
                    } else {
                        VLog.w("VActivityManager", "miui provider waiting process: " + info.authority + "(" + info.processName + ")", new Object[0]);
                        return null;
                    }
                }
                return holder2;
            }
            replaceLastUserId(args);
            return method2.invoke(obj, objArr);


           /* int nameIdx = getProviderNameIndex();
            String name = (String) args[nameIdx];

            if ((name.startsWith(StubManifest.STUB_CP_AUTHORITY)
                    || name.startsWith(StubManifest.STUB_CP_AUTHORITY_64BIT)
                    || name.equals(getConfig().get64bitHelperAuthority()))
                    || name.equals(getConfig().getBinderProviderAuthority())
                    || name.equals(ProxyFCPUriCompat.get().getAuthority())) {
                return method.invoke(who, args);
            }

            if (BuildCompat.isQ()) {
                int i = nameIdx - 1;
                if (args[i] instanceof String) {
                    args[i] = VCore.get().getHostPkg();
                }
            }

            if (ProxyFCPUriCompat.get().isOutSide(name)) {
                args[nameIdx] = ProxyFCPUriCompat.get().unWrapperOutSide(name);
                return method.invoke(who, args);
            }

            int userId = VUserHandle.myUserId();
            ProviderInfo info = VPackageManager.get().resolveContentProvider(name, 0, userId);
            if (info != null && info.enabled && isAppPkg(info.packageName)) {
                ClientConfig config = VActivityManager.get().initProcess(info.packageName, info.processName, userId);
                if (config == null) {
                    return null;
                }
                args[nameIdx] = StubManifest.getStubAuthority(config.vpid, config.is64Bit);
                MethodProxy.replaceLastUserId(args);
                Object holder = method.invoke(who, args);
                if (holder == null) {
                    return null;
                }
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    if (provider != null) {
                        provider = VActivityManager.get().acquireProviderClient(userId, info);
                        if (BuildCompat.isS()) {
                            if (provider != null){
                                //todo 待验证
                                provider = ProviderHook.createProxy(false, name, provider);
                            }
                        }
                    }
                    if (provider == null) {
                        VLog.e("VActivityManager", "acquireProviderClient fail: " + info.authority + "(" + info.processName + ")");
                        return null;
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                    ContentProviderHolderOreo.info.set(holder, info);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    if (provider != null) {
                        provider = VActivityManager.get().acquireProviderClient(userId, info);
                    }
                    if (provider == null) {
                        VLog.e("VActivityManager", "acquireProviderClient fail: " + info.authority);
                        return null;
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                    IActivityManager.ContentProviderHolder.info.set(holder, info);
                }
                return holder;
            }
            VLog.w("ActivityManger", "getContentProvider:%s", name);
            MethodProxy.replaceLastUserId(args);
            Object holder = method.invoke(who, args);
            if (holder != null) {
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    info = ContentProviderHolderOreo.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    info = IActivityManager.ContentProviderHolder.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
                return holder;
            }
            return null;*/
        }

        public int getProviderNameIndex() {
            if (BuildCompat.isQ()) {
                return 2;
            }
            return 1;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }

        private boolean miuiProviderWaitingTargetProcess(Object providerHolder) {
            if (providerHolder == null || IActivityManager.ContentProviderHolderMIUI.waitProcessStart == null) {
                return false;
            }
            return IActivityManager.ContentProviderHolderMIUI.waitProcessStart.get(providerHolder);
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static class SetTaskDescription extends MethodProxy {
        @Override
        public String getMethodName() {
            return "setTaskDescription";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ActivityManager.TaskDescription td = (ActivityManager.TaskDescription) args[1];
            String label = td.getLabel();
            Bitmap icon = td.getIcon();

            // If the activity label/icon isn't specified, the application's label/icon is shown instead
            // Android usually does that for us, but in this case we want info about the contained app, not VIrtualApp itself
            if (label == null || icon == null) {
                Application app = VClient.get().getCurrentApplication();
                if (app != null) {
                    try {
                        if (label == null) {
                            label = app.getApplicationInfo().loadLabel(app.getPackageManager()).toString();
                        }
                        if (icon == null) {
                            Drawable drawable = app.getApplicationInfo().loadIcon(app.getPackageManager());
                            if (drawable != null) {
                                icon = DrawableUtils.drawableToBitMap(drawable);
                            }
                        }
                        td = new ActivityManager.TaskDescription(label, icon, td.getPrimaryColor());
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }

            TaskDescriptionDelegate descriptionDelegate = VCore.get().getTaskDescriptionDelegate();
            if (descriptionDelegate != null) {
                td = descriptionDelegate.getTaskDescription(td);
            }

            args[1] = td;
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StopServiceToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopServiceToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            IBinder token = (IBinder) args[1];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int startId = (int) args[2];
            if (componentName != null) {
                return VActivityManager.get().stopServiceToken(componentName, token, startId);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    public static class StartActivityWithConfig extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityWithConfig";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }

    static class StartNextMatchingActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startNextMatchingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return false;
        }
    }


    public static class BroadcastIntent extends MethodProxy {

        @Override
        public String getMethodName() {
            return "broadcastIntent";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent intent = (Intent) args[1];
            String type = (String) args[2];
            intent.setDataAndType(intent.getData(), type);
            Intent newIntent = handleIntent(intent);
            if (newIntent != null) {
                args[1] = newIntent;
            } else {
                return 0;
            }
            if (args[7] instanceof String || args[7] instanceof String[]) {
                // clear the permission
                args[7] = null;
            }
            MethodProxy.replaceLastUserId(args);
            return method.invoke(who, args);
        }


        protected Intent handleIntent(final Intent intent) {
            final String action = intent.getAction();
            if ("android.intent.action.CREATE_SHORTCUT".equals(action)
                    || "com.android.launcher.action.INSTALL_SHORTCUT".equals(action)
                    || "com.aliyun.homeshell.action.INSTALL_SHORTCUT".equals(action)) {

                return getConfig().isAllowCreateShortcut() ? handleInstallShortcutIntent(intent) : null;

            } else if ("com.android.launcher.action.UNINSTALL_SHORTCUT".equals(action)
                    || "com.aliyun.homeshell.action.UNINSTALL_SHORTCUT".equals(action)) {

                handleUninstallShortcutIntent(intent);

            } else if (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action)) {
                return handleMediaScannerIntent(intent);
            } else if (BadgerManager.handleBadger(intent)) {
                return null;
            } else {
                return ComponentUtils.redirectBroadcastIntent(intent, VUserHandle.myUserId());
            }
            return intent;
        }

        private Intent handleMediaScannerIntent(Intent intent) {
            if (intent == null) {
                return null;
            }
            Uri data = intent.getData();
            if (data == null) {
                return intent;
            }
            String scheme = data.getScheme();
            if (!"file".equalsIgnoreCase(scheme)) {
                return intent;
            }
            String path = data.getPath();
            if (path == null) {
                return intent;
            }
            String newPath = NativeEngine.getRedirectedPath(path);
            File newFile = new File(newPath);
            if (!newFile.exists()) {
                return intent;
            }
            intent.setData(Uri.fromFile(newFile));
            return intent;
        }

        private Intent handleInstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName component = shortcut.resolveActivity(VCore.getPM());
                if (component != null) {
                    String pkg = component.getPackageName();
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    newShortcutIntent.setAction(VCore.getConfig().getShortcutProxyActionName());
                    newShortcutIntent.setPackage(getHostPkg());
                    newShortcutIntent.putExtra("_VBOX_|_intent_", shortcut);
                    newShortcutIntent.putExtra("_VBOX_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.putExtra("_VBOX_|_user_id_", VUserHandle.myUserId());
                    intent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);

                    Intent.ShortcutIconResource icon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (icon != null && !TextUtils.equals(icon.packageName, getHostPkg())) {
                        try {
                            Resources resources = VCore.get().getResources(pkg);
                            int resId = resources.getIdentifier(icon.resourceName, "drawable", pkg);
                            if (resId > 0) {
                                //noinspection deprecation
                                Drawable iconDrawable = resources.getDrawable(resId);
                                Bitmap newIcon = BitmapUtils.drawableToBitmap(iconDrawable);
                                if (newIcon != null) {
                                    intent.removeExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, newIcon);
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return intent;
        }

        private void handleUninstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName componentName = shortcut.resolveActivity(getPM());
                if (componentName != null) {
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.putExtra("_VBOX_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.setClassName(getHostPkg(), VCore.getConfig().getShortcutProxyActivityName());
                    newShortcutIntent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);
                }
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetActivityClassForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getActivityClassForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getActivityForToken(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GrantUriPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "grantUriPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            for (int i = 0; i < args.length; i++) {
                Object uri = args[i];
                if (uri instanceof Uri) {
                    Uri wrapperUri = ProxyFCPUriCompat.get().wrapperUri((Uri) uri);
                    if (wrapperUri != null) {
                        args[i] = wrapperUri;
                    }
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class CheckGrantUriPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkGrantUriPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class ServiceDoneExecuting extends MethodProxy {

        @Override
        public String getMethodName() {
            return "serviceDoneExecuting";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int type = (int) args[1];
            int startId = (int) args[2];
            int res = (int) args[3];
            VActivityManager.get().serviceDoneExecuting(token, type, startId, res);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class isUserRunning extends MethodProxy {
        @Override
        public String getMethodName() {
            return "isUserRunning";
        }

        @Override
        public Object call(Object who, Method method, Object... args) {
            int userId = (int) args[0];
            for (VUserInfo userInfo : VUserManager.get().getUsers()) {
                if (userInfo.id == userId) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPackageProcessState extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageProcessState";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            return 4/*ActivityManager.PROCESS_STATE_FOREGROUND_SERVICE*/;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    public static class OverridePendingTransition extends MethodProxy {

        @Override
        public String getMethodName() {
            return "overridePendingTransition";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (!VClient.get().isAppUseOutsideAPK()) {
                return 0;
            }
            return super.call(who, method, args);
        }
    }


    public static class GetIntentSenderWithFeature extends GetIntentSender {

        public GetIntentSenderWithFeature() {
            // http://aospxref.com/android-11.0.0_r21/xref/frameworks/base/core/java/android/app/IActivityManager.aidl?fi=IActivityManager#245
            mIntentIndex = 6;
            mResolvedTypesIndex = 7;
            mFlagsIndex = 8;
        }

        @Override
        public String getMethodName() {
            return "getIntentSenderWithFeature";
        }
    }

    // For Android 11
    public static class RegisterReceiverWithFeature extends RegisterReceiver {
        public RegisterReceiverWithFeature() {
            if (BuildCompat.isS()) {
                // http://aospxref.com/android-12.0.0_r3/xref/frameworks/base/core/java/android/app/IActivityManager.aidl?fi=IActivityManager#127
                mIIntentReceiverIndex = 4;
                mIntentFilterIndex = 5;
                mRequiredPermissionIndex = 6;
            } else {
                // http://aospxref.com/android-11.0.0_r21/xref/frameworks/base/core/java/android/app/IActivityManager.aidl?fi=IActivityManager#124
                mIIntentReceiverIndex = 3;
                mIntentFilterIndex = 4;
                mRequiredPermissionIndex = 5;
            }
        }

        @Override
        public String getMethodName() {
            return "registerReceiverWithFeature";
        }
    }

    public static class GetAppTasks extends MethodProxy {
        public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(param1VarArgs);
            return super.call(param1Object, param1Method, param1VarArgs);
        }

        public String getMethodName() {
            return "getAppTasks";
        }
    }
}
