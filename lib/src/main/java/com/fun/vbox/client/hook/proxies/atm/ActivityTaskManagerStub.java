package com.fun.vbox.client.hook.proxies.atm;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.IBinder;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.annotations.Inject;
import com.fun.vbox.client.hook.annotations.LogInvocation;
import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.BinderInvocationStub;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.StaticMethodProxy;
import com.fun.vbox.client.ipc.VActivityManager;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.helper.utils.ComponentUtils;
import com.fun.vbox.helper.utils.Reflect;
import com.fun.vbox.os.VUserHandle;

import java.lang.reflect.Method;

import mirror.vbox.app.IActivityTaskManager;
import mirror.vbox.util.Singleton;

@TargetApi(29)
@Inject(MethodProxies.class)
@LogInvocation
public class ActivityTaskManagerStub extends BinderInvocationProxy {
//    public ActivityTaskManagerStub() {
//        super(IActivityTaskManager.Stub.asInterface, "activity_task");
//    }

    public ActivityTaskManagerStub() {
        super(IActivityTaskManager.Stub.asInterface, "activity_task");
        try {
            Object object = Reflect.on("android.app.ActivityTaskManager").field("IActivityTaskManagerSingleton").get();
            Singleton.mInstance.set(object, ((BinderInvocationStub)getInvocationStub()).getProxyInterface());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    protected void onBindMethods() {
        /*super.onBindMethods();
        addMethodProxy(new StaticMethodProxy("activityDestroyed") {
            public Object call(Object obj, Method method, Object... objArr) throws Throwable {
                VActivityManager.get().onActivityDestroy((IBinder) objArr[0]);
                return super.call(obj, method, objArr);
            }
        });
        addMethodProxy(new StaticMethodProxy("activityResumed") {
            public Object call(Object obj, Method method, Object... objArr) throws Throwable {
                VActivityManager.get().onActivityResumed((IBinder) objArr[0]);
                return super.call(obj, method, objArr);
            }
        });
        addMethodProxy(new StaticMethodProxy("finishActivity") {
            public Object call(Object obj, Method method, Object... objArr) throws Throwable {
                VActivityManager.get().onFinishActivity((IBinder) objArr[0]);
                return super.call(obj, method, objArr);
            }

            public boolean isEnable() {
                return MethodProxy.isAppProcess();
            }
        });
        addMethodProxy(new StaticMethodProxy("finishActivityAffinity") {
            public Object call(Object obj, Method method, Object... objArr) {
                return VActivityManager.get()
                        .finishActivityAffinity(MethodProxy.getAppUserId(),
                                (IBinder) objArr[Integer.parseInt(null)]);
            }

            public boolean isEnable() {
                return MethodProxy.isAppProcess();
            }
        });
        addMethodProxy(new StaticMethodProxy("requestAutofillData"));*/

        super.onBindMethods();
        addMethodProxy(new StaticMethodProxy("activityDestroyed") {
            public Object afterCall(Object param1Object1, Method param1Method, Object[] param1ArrayOfObject, Object param1Object2) throws Throwable {
                IBinder iBinder = (IBinder)param1ArrayOfObject[0];
                VActivityManager.get().onActivityDestroy(iBinder);
                return super.afterCall(param1Object1, param1Method, param1ArrayOfObject, param1Object2);
            }
        });
        addMethodProxy(new StaticMethodProxy("activityResumed") {
            public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
                IBinder iBinder = (IBinder)param1VarArgs[0];
                VActivityManager.get().onActivityResumed(iBinder);
                return super.call(param1Object, param1Method, param1VarArgs);
            }
        });
        addMethodProxy(new StaticMethodProxy("finishActivity") {
            public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
                IBinder iBinder = (IBinder)param1VarArgs[0];
                Intent intent = (Intent)param1VarArgs[2];
                if (intent != null)
                    param1VarArgs[2] = ComponentUtils.processOutsideIntent(VUserHandle.myUserId(), VCore.get().isExtPackage(), intent);
                VActivityManager.get().onFinishActivity(iBinder);
                return super.call(param1Object, param1Method, param1VarArgs);
            }

            public boolean isEnable() {
                return isAppProcess();
            }
        });
        addMethodProxy(new StaticMethodProxy("finishActivityAffinity") {
            public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) {
                param1Object = param1VarArgs[0];
                return Boolean.valueOf(VActivityManager.get().finishActivityAffinity(getAppUserId(), (IBinder)param1Object));
            }

            public boolean isEnable() {
                return isAppProcess();
            }
        });
        if (BuildCompat.isSamsung())
            addMethodProxy(new StaticMethodProxy("startAppLockService") {
                public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) {
                    return Integer.valueOf(0);
                }
            });
    }
}
