package com.fun.vbox.client.hook.proxies.app;

import android.content.Intent;
import android.os.IBinder;
import android.os.IInterface;


import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.annotations.Inject;
import com.fun.vbox.client.hook.base.MethodInvocationProxy;
import com.fun.vbox.client.hook.base.MethodInvocationStub;
import com.fun.vbox.client.hook.base.StaticMethodProxy;
import com.fun.vbox.client.hook.proxies.am.MethodProxies;
import com.fun.vbox.client.ipc.VActivityManager;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.helper.utils.ComponentUtils;
import com.fun.vbox.os.VUserHandle;

import java.lang.reflect.Method;

import mirror.RefStaticMethod;
import mirror.vbox.app.ActivityClient;
import mirror.vbox.util.Singleton;

@Inject(MethodProxies.class)
public class ActivityClientControllerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {
    private static IInterface sActivityClientControllerProxy;

    public ActivityClientControllerStub() {
        super(new MethodInvocationStub(ActivityClient.getActivityClientController.call(new Object[0])));
    }

    public static IInterface getProxyInterface() {
        return sActivityClientControllerProxy;
    }

    public void inject() {
        if (ActivityClient.INTERFACE_SINGLETON != null) {
            if (ActivityClient.ActivityClientControllerSingleton.mKnownInstance != null)
                ActivityClient.ActivityClientControllerSingleton.mKnownInstance.set(ActivityClient.INTERFACE_SINGLETON.get(), getInvocationStub().getProxyInterface());
            Singleton.mInstance.set(ActivityClient.INTERFACE_SINGLETON.get(), getInvocationStub().getProxyInterface());
            sActivityClientControllerProxy = getInvocationStub().getProxyInterface();
        }
    }

    public boolean isEnvBad() {
        RefStaticMethod refStaticMethod = ActivityClient.getActivityClientController;
        boolean bool = false;
        if (refStaticMethod.call(new Object[0]) != getInvocationStub().getProxyInterface())
            bool = true;
        return bool;
    }

    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new StaticMethodProxy("activityDestroyed") {
            public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
                IBinder iBinder = (IBinder) args[0];
                VActivityManager.get().onActivityDestroy(iBinder);
                return super.afterCall(who, method, args, result);
            }
        });
        addMethodProxy(new StaticMethodProxy("activityResumed") {
            public Object call(Object who, Method method, Object... args) throws Throwable {
                IBinder iBinder = (IBinder) args[0];
                VActivityManager.get().onActivityResumed(iBinder);
                return super.call(who, method, args);
            }
        });
        addMethodProxy(new StaticMethodProxy("finishActivity") {
            public Object call(Object who, Method method, Object... args) throws Throwable {
                IBinder iBinder = (IBinder) args[0];
                Intent intent = (Intent) args[2];
                if (intent != null)
                    args[2] = ComponentUtils.processOutsideIntent(VUserHandle.myUserId(), VCore.get().isExtPackage(), intent);
                VActivityManager.get().onFinishActivity(iBinder);
                return super.call(who, method, args);
            }

            public boolean isEnable() {
                return isAppProcess();
            }
        });
        addMethodProxy(new StaticMethodProxy("finishActivityAffinity") {
            public Object call(Object who, Method method, Object... args) {
                who = args[0];
                return Boolean.valueOf(VActivityManager.get().finishActivityAffinity(getAppUserId(), (IBinder) who));
            }

            public boolean isEnable() {
                return isAppProcess();
            }
        });
        if (BuildCompat.isSamsung())
            addMethodProxy(new StaticMethodProxy("startAppLockService") {
                public Object call(Object who, Method method, Object... args) {
                    return Integer.valueOf(0);
                }
            });
    }
}