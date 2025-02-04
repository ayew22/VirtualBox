package com.fun.vbox.client.core;

import android.os.Build;

import com.fun.vbox.client.hook.base.MethodInvocationProxy;
import com.fun.vbox.client.hook.base.MethodInvocationStub;
import com.fun.vbox.client.hook.delegate.AppInstrumentation;
import com.fun.vbox.client.hook.proxies.accessibility.AccessibilityManagerStub;
import com.fun.vbox.client.hook.proxies.account.AccountManagerStub;
import com.fun.vbox.client.hook.proxies.alarm.AlarmManagerStub;
import com.fun.vbox.client.hook.proxies.am.ActivityManagerStub;
import com.fun.vbox.client.hook.proxies.am.HCallbackStub;
import com.fun.vbox.client.hook.proxies.app.ActivityClientControllerStub;
import com.fun.vbox.client.hook.proxies.appops.AppOpsManagerStub;
import com.fun.vbox.client.hook.proxies.appops.FlymePermissionServiceStub;
import com.fun.vbox.client.hook.proxies.appops.SmtOpsManagerStub;
import com.fun.vbox.client.hook.proxies.appwidget.AppWidgetManagerStub;
import com.fun.vbox.client.hook.proxies.audio.AudioManagerStub;
import com.fun.vbox.client.hook.proxies.backup.BackupManagerStub;
import com.fun.vbox.client.hook.proxies.battery_stats.BatteryStatsHub;
import com.fun.vbox.client.hook.proxies.bluetooth.BluetoothStub;
import com.fun.vbox.client.hook.proxies.clipboard.ClipBoardStub;
import com.fun.vbox.client.hook.proxies.clipboard.SemClipBoardStub;
import com.fun.vbox.client.hook.proxies.connectivity.ConnectivityStub;
import com.fun.vbox.client.hook.proxies.content.ContentServiceStub;
import com.fun.vbox.client.hook.proxies.content.integrity.AppIntegrityManagerStub;
import com.fun.vbox.client.hook.proxies.context_hub.ContextHubServiceStub;
import com.fun.vbox.client.hook.proxies.cross_profile.CrossProfileAppsStub;
import com.fun.vbox.client.hook.proxies.device.DeviceIdleControllerStub;
import com.fun.vbox.client.hook.proxies.devicepolicy.DevicePolicyManagerStub;
import com.fun.vbox.client.hook.proxies.display.DisplayStub;
import com.fun.vbox.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.fun.vbox.client.hook.proxies.fingerprint.FingerprintManagerStub;
import com.fun.vbox.client.hook.proxies.graphics.GraphicsStatsStub;
import com.fun.vbox.client.hook.proxies.imms.MmsStub;
import com.fun.vbox.client.hook.proxies.input.InputMethodManagerStub;
import com.fun.vbox.client.hook.proxies.isms.ISmsStub;
import com.fun.vbox.client.hook.proxies.isub.ISubStub;
import com.fun.vbox.client.hook.proxies.job.JobServiceStub;
import com.fun.vbox.client.hook.proxies.libcore.LibCoreStub;
import com.fun.vbox.client.hook.proxies.location.LocationManagerStub;
import com.fun.vbox.client.hook.proxies.media.router.MediaRouterServiceStub;
import com.fun.vbox.client.hook.proxies.media.session.SessionManagerStub;
import com.fun.vbox.client.hook.proxies.mount.MountServiceStub;
import com.fun.vbox.client.hook.proxies.network.NetworkManagementStub;
import com.fun.vbox.client.hook.proxies.network.TetheringConnectorStub;
import com.fun.vbox.client.hook.proxies.notification.NotificationManagerStub;
import com.fun.vbox.client.hook.proxies.os.DeviceIdentifiersPolicyServiceHub;
import com.fun.vbox.client.hook.proxies.os.StatsManagerServiceStub;
import com.fun.vbox.client.hook.proxies.permission.PermissionManagerStub;
import com.fun.vbox.client.hook.proxies.permission.UriGrantsManagerStub;
import com.fun.vbox.client.hook.proxies.persistent_data_block.PersistentDataBlockServiceStub;
import com.fun.vbox.client.hook.proxies.phonesubinfo.PhoneSubInfoStub;
import com.fun.vbox.client.hook.proxies.pm.PackageManagerStub;
import com.fun.vbox.client.hook.proxies.power.PowerManagerStub;
import com.fun.vbox.client.hook.proxies.restriction.RestrictionStub;
import com.fun.vbox.client.hook.proxies.role.RoleManagerStub;
import com.fun.vbox.client.hook.proxies.search.SearchManagerStub;
import com.fun.vbox.client.hook.proxies.shortcut.ShortcutServiceStub;
import com.fun.vbox.client.hook.proxies.slice.SliceManagerStub;
import com.fun.vbox.client.hook.proxies.storage_stats.StorageStatsStub;
import com.fun.vbox.client.hook.proxies.system.LockSettingsStub;
import com.fun.vbox.client.hook.proxies.system.SystemUpdateStub;
import com.fun.vbox.client.hook.proxies.system.WifiScannerStub;
import com.fun.vbox.client.hook.proxies.atm.ActivityTaskManagerStub;
import com.fun.vbox.client.hook.proxies.telecom.TelecomManagerStub;
import com.fun.vbox.client.hook.proxies.telephony.HwTelephonyStub;
import com.fun.vbox.client.hook.proxies.telephony.TelephonyRegistryStub;
import com.fun.vbox.client.hook.proxies.telephony.TelephonyStub;
import com.fun.vbox.client.hook.proxies.usage.UsageStatsManagerStub;
import com.fun.vbox.client.hook.proxies.user.UserManagerStub;
import com.fun.vbox.client.hook.proxies.vibrator.VibratorStub;
import com.fun.vbox.client.hook.proxies.vibrator.VibratorStubForS;
import com.fun.vbox.client.hook.proxies.view.AutoFillManagerStub;
import com.fun.vbox.client.hook.proxies.wallpaper.WallpaperManagerStub;
import com.fun.vbox.client.hook.proxies.wifi.WifiManagerStub;
import com.fun.vbox.client.hook.proxies.window.WindowManagerStub;
import com.fun.vbox.client.interfaces.IInjector;
import com.fun.vbox.helper.compat.BuildCompat;

import java.util.HashMap;
import java.util.Map;

import mirror.com.android.internal.app.ISmtOpsService;
import mirror.com.android.internal.telephony.IHwTelephony;
import mirror.oem.IFlymePermissionService;
import mirror.vbox.app.UriGrantsManager;
import mirror.vbox.os.IDeviceIdleController;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;

/**
 * @author Lody
 */
public final class InvocationStubManager {

    private static InvocationStubManager sInstance = new InvocationStubManager();
    private static boolean sInit;

    private Map<Class<?>, IInjector> mInjectors = new HashMap<>(13);

    private InvocationStubManager() {
    }

    public static InvocationStubManager getInstance() {
        return sInstance;
    }

    void injectAll() throws Throwable {
        for (IInjector injector : mInjectors.values()) {
            injector.inject();
        }
        if (VCore.get().isVAppProcess()) {
            // XXX: Lazy inject the Instrumentation,
            addInjector(AppInstrumentation.getDefault());
        }
    }

    /**
     * @return if the InvocationStubManager has been initialized.
     */
    public boolean isInit() {
        return sInit;
    }


    public void init() throws Throwable {
        if (isInit()) {
            throw new IllegalStateException("InvocationStubManager Has been initialized.");
        }
        injectInternal();
        sInit = true;

    }

    private void injectInternal() throws Throwable {
        if (!VCore.get().isMainProcess()) {
            if (VCore.get().isServerProcess()) {
                addInjector(new ActivityManagerStub());
            } else if (VCore.get().isVAppProcess()) {
//                addInjector(new LibCoreStub());
                addInjector(new ActivityManagerStub());
                addInjector(new PackageManagerStub());
                addInjector(HCallbackStub.getDefault());
                addInjector(new ISmsStub());
                addInjector(new ISubStub());
                addInjector(new DropBoxManagerStub());
                addInjector(new NotificationManagerStub());
                addInjector(new LocationManagerStub());
                addInjector(new WindowManagerStub());
                addInjector(new ClipBoardStub());
                addInjector(new SemClipBoardStub());
                addInjector(new MountServiceStub());
                addInjector(new BackupManagerStub());
                addInjector(new TelephonyStub());
                addInjector(new AccessibilityManagerStub());
                if (BuildCompat.isOreo() && IHwTelephony.TYPE != null) {
                    addInjector(new HwTelephonyStub());
                }
                addInjector(new TelephonyRegistryStub());
                addInjector(new PhoneSubInfoStub());
                addInjector(new PowerManagerStub());
                addInjector(new AppWidgetManagerStub());
                addInjector(new AccountManagerStub());
                addInjector(new AudioManagerStub());
                addInjector(new SearchManagerStub());
                addInjector(new ContentServiceStub());
                addInjector(new ConnectivityStub());
                addInjector(new BluetoothStub());
                addInjector(new VibratorStub());
                addInjector(new WifiManagerStub());
                addInjector(new ContextHubServiceStub());
                addInjector(new UserManagerStub());
                addInjector(new WallpaperManagerStub());
                addInjector(new DisplayStub());
                addInjector(new PersistentDataBlockServiceStub());
                addInjector(new InputMethodManagerStub());
                addInjector(new MmsStub());
                addInjector(new SessionManagerStub());
                addInjector(new JobServiceStub());
                addInjector(new RestrictionStub());
                addInjector(new TelecomManagerStub());
                addInjector(new AlarmManagerStub());
                addInjector(new AppOpsManagerStub());
                addInjector(new MediaRouterServiceStub());
                if (ISmtOpsService.TYPE != null) {
                    addInjector(new SmtOpsManagerStub());
                }
                if (Build.VERSION.SDK_INT >= 22) {
                    addInjector(new GraphicsStatsStub());
                    addInjector(new UsageStatsManagerStub());
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    addInjector(new FingerprintManagerStub());
                    addInjector(new NetworkManagementStub());
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    addInjector(new WifiScannerStub());
                    addInjector(new ShortcutServiceStub());
                    addInjector(new DevicePolicyManagerStub());
                    addInjector(new BatteryStatsHub());
                }
                if (BuildCompat.isOreo()) {
                    addInjector(new AutoFillManagerStub());
                }
                if (BuildCompat.isPie()) {
                    addInjector(new SystemUpdateStub());
                    addInjector(new LockSettingsStub());
                    addInjector(new CrossProfileAppsStub());
                    addInjector(new SliceManagerStub());
                }
                if (IFlymePermissionService.TYPE != null) {
                    addInjector(new FlymePermissionServiceStub());
                }
                if (BuildCompat.isQ()) {
                    addInjector(new ActivityTaskManagerStub());
                    addInjector(new DeviceIdentifiersPolicyServiceHub());
                    addInjector(new UriGrantsManagerStub());
                    addInjector(new RoleManagerStub());
                }
                if (BuildCompat.isR()) {
                    addInjector(new PermissionManagerStub());
                    addInjector(new AppIntegrityManagerStub());
                    addInjector(new StatsManagerServiceStub());
                    addInjector(new TetheringConnectorStub());
                }
                if (BuildCompat.isS()) {
                    addInjector(new ActivityClientControllerStub());
                }
                if (IDeviceIdleController.TYPE != null) {
                    addInjector(new DeviceIdleControllerStub());
                }
                OemInjectManager.oemInject(this);
            }
        }

    }

    public void addInjector(IInjector IInjector) {
        mInjectors.put(IInjector.getClass(), IInjector);
    }

    public <T extends IInjector> T findInjector(Class<T> clazz) {
        // noinspection unchecked
        return (T) mInjectors.get(clazz);
    }

    public <T extends IInjector> void checkEnv(Class<T> clazz) {
        IInjector IInjector = findInjector(clazz);
        if (IInjector != null && IInjector.isEnvBad()) {
            try {
                IInjector.inject();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void checkAllEnv() {
        for (IInjector injector : mInjectors.values()) {
            if (injector.isEnvBad()) {
                try {
                    injector.inject();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public <T extends IInjector, H extends MethodInvocationStub> H getInvocationStub(Class<T> injectorClass) {
        T injector = findInjector(injectorClass);
        if (injector instanceof MethodInvocationProxy) {
            // noinspection unchecked
            return (H) ((MethodInvocationProxy) injector).getInvocationStub();
        }
        return null;
    }

}