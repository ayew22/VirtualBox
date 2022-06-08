package com.fun.vbox.client.hook.proxies.appops;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.annotations.Inject;
import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.fun.vbox.client.hook.base.ReplaceLastPkgMethodProxy;
import com.fun.vbox.client.hook.base.StaticMethodProxy;
import com.fun.vbox.helper.compat.BuildCompat;

import java.lang.reflect.Method;

import mirror.com.android.internal.app.IAppOpsService;

/**
 *
 * <p>
 * Fuck the AppOpsService.
 * @see android.app.AppOpsManager
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
//@Inject(MethodProxies.class)
public class AppOpsManagerStub extends BinderInvocationProxy {

    public AppOpsManagerStub() {
        super(IAppOpsService.Stub.asInterface, Context.APP_OPS_SERVICE);
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        if (mirror.vbox.app.AppOpsManager.mService != null) {
            AppOpsManager appOpsManager = (AppOpsManager) VCore.get().getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                mirror.vbox.app.AppOpsManager.mService.set(appOpsManager, getInvocationStub().getProxyInterface());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();

        addMethodProxy(new BaseMethodProxy("checkOperation", 1, 2));
        addMethodProxy(new BaseMethodProxy("noteOperation", 1, 2));
        addMethodProxy(new BaseMethodProxy("startOperation", 2, 3));
        addMethodProxy(new BaseMethodProxy("finishOperation", 2, 3));
        addMethodProxy(new BaseMethodProxy("startWatchingMode", -1, 1));
        addMethodProxy(new BaseMethodProxy("checkPackage", 0, 1));
        addMethodProxy(new BaseMethodProxy("getOpsForPackage", 0, 1));
        addMethodProxy(new BaseMethodProxy("setMode", 1, 2));
        addMethodProxy(new BaseMethodProxy("checkAudioOperation", 2, 3));
        addMethodProxy(new BaseMethodProxy("setAudioRestriction", 2, -1));
        addMethodProxy(new ReplaceLastPkgMethodProxy("resetAllModes"));
        addMethodProxy(new MethodProxy() {
            @Override
            public String getMethodName() {
                return "noteProxyOperation";
            }

            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {

                if (Build.VERSION.SDK_INT >= 31) {
                    return method.invoke(who, args);
                }

                return 0;
            }
        });

        switch (Build.VERSION.SDK_INT) {
            case 32:
            case 31:
            case Build.VERSION_CODES.R:
                addMethodProxy(new BaseMethodProxy("startWatchingAsyncNoted", -1,  0));
                addMethodProxy(new BaseMethodProxy("stopWatchingAsyncNoted", -1,  0));
                addMethodProxy(new BaseMethodProxy("extractAsyncOps", -1,  0));
                addMethodProxy(new BaseMethodProxy("collectNoteOpCallsForValidation", -1,  2));
                break;
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.P:
                addMethodProxy(new BaseMethodProxy("startWatchingModeWithFlags", -1,  1));
                break;
        }    }

    private class BaseMethodProxy extends StaticMethodProxy {
        final int pkgIndex;
        final int uidIndex;

        BaseMethodProxy(String name, int uidIndex, int pkgIndex) {
            super(name);
            this.pkgIndex = pkgIndex;
            this.uidIndex = uidIndex;
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            if (pkgIndex != -1 && args.length > pkgIndex && args[pkgIndex] instanceof String) {
                args[pkgIndex] = getHostPkg();
            }
            if (uidIndex != -1 && args[uidIndex] instanceof Integer) {
                args[uidIndex] = getRealUid();
            }
            return true;
        }
    }

}
