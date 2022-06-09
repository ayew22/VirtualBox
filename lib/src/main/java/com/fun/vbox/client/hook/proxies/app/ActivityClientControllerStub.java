package com.fun.vbox.client.hook.proxies.app;

import android.content.Intent;
import android.os.IBinder;
import android.os.IInterface;


import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.annotations.Inject;
import com.fun.vbox.client.hook.base.MethodInvocationProxy;
import com.fun.vbox.client.hook.base.MethodInvocationStub;
import com.fun.vbox.client.hook.base.MethodProxy;
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
            if (ActivityClient.ActivityClientControllerSingleton.mKnownInstance != null) {
                ActivityClient.ActivityClientControllerSingleton.mKnownInstance.set(ActivityClient.INTERFACE_SINGLETON.get(), (IInterface) getInvocationStub().getProxyInterface());
            }
            Singleton.mInstance.set(ActivityClient.INTERFACE_SINGLETON.get(), getInvocationStub().getProxyInterface());
            sActivityClientControllerProxy = (IInterface) getInvocationStub().getProxyInterface();
        }

    }

    public boolean isEnvBad() {
        return ActivityClient.getActivityClientController.call(new Object[0]) != getInvocationStub().getProxyInterface();
    }

    public void onBindMethods() {
        super.onBindMethods();
        addMethodProxy((MethodProxy) new StaticMethodProxy("activityDestroyed") {
            public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
                VActivityManager.get().onActivityDestroy((IBinder) args[0]);
                return super.afterCall(who, method, args, result);
            }
        });
        addMethodProxy((MethodProxy) new StaticMethodProxy("activityResumed") {
            public Object call(Object who, Method method, Object... args) throws Throwable {
                VActivityManager.get().onActivityResumed((IBinder) args[0]);
                return super.call(who, method, args);
            }
        });
        addMethodProxy((MethodProxy) new StaticMethodProxy("finishActivity") {
            public Object call(Object who, Method method, Object... args) throws Throwable {
                IBinder token = (IBinder) args[0];
                Intent intent = (Intent) args[2];
                if (intent != null) {
                    args[2] = ComponentUtils.processOutsideIntent(VUserHandle.myUserId(), VCore.get().isExtPackage(), intent);
                }
                VActivityManager.get().onFinishActivity(token);
                return super.call(who, method, args);
            }

            public boolean isEnable() {
                return isAppProcess();
            }
        });
        addMethodProxy((MethodProxy) new StaticMethodProxy("finishActivityAffinity") {
            public Object call(Object who, Method method, Object... args) {
                return Boolean.valueOf(VActivityManager.get().finishActivityAffinity(getAppUserId(), (IBinder) args[0]));
            }

            public boolean isEnable() {
                return isAppProcess();
            }
        });
        if (BuildCompat.isSamsung()) {
            addMethodProxy((MethodProxy) new StaticMethodProxy("startAppLockService") {
                public Object call(Object who, Method method, Object... args) {
                    return 0;
                }
            });
        }
    }

}