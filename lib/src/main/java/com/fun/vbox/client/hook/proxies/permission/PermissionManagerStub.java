package com.fun.vbox.client.hook.proxies.permission;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.IInterface;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.BinderInvocationStub;
import com.fun.vbox.client.hook.base.MethodInvocationProxy;
import com.fun.vbox.client.hook.base.MethodInvocationStub;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.fun.vbox.client.hook.base.StaticMethodProxy;
import com.fun.vbox.client.ipc.VPackageManager;
import com.fun.vbox.helper.utils.Reflect;

import java.lang.reflect.Method;

import mirror.vbox.app.ActivityThread;
import mirror.vbox.permission.IPermissionManager;

@TargetApi(30)
public final class PermissionManagerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {
    public PermissionManagerStub() {
        super(new MethodInvocationStub<>(ActivityThread.sPermissionManager.get()));
    }

    @Override
    public void inject() throws Throwable {
//        super.inject();
//        addMethodProxy(new ReplaceCallingPkgMethodProxy("checkDeviceIdentifierAccess"));
        IInterface iInterface = getInvocationStub().getProxyInterface();
        ActivityThread.sPermissionManager.set(iInterface);
        try {
            if (Reflect.on(VCore.getPM()).field("mPermissionManager").get() != iInterface)
                Reflect.on(VCore.getPM()).set("mPermissionManager", iInterface);
        } catch (Exception e) {
            e.printStackTrace();
        }
        BinderInvocationStub binderInvocationStub = new BinderInvocationStub(getInvocationStub().getBaseInterface());
        binderInvocationStub.copyMethodProxies(getInvocationStub());
        binderInvocationStub.replaceService("permissionmgr");
    }

    @Override
    public boolean isEnvBad() {
        return false;
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new StaticMethodProxy("addOnPermissionsChangeListener") {
            public Object call(Object who, Method method, Object... args) {
                return Integer.valueOf(0);
            }
        });
        addMethodProxy(new StaticMethodProxy("removeOnPermissionsChangeListener") {
            public Object call(Object who, Method method, Object... args) {
                return Integer.valueOf(0);
            }
        });
        addMethodProxy(new StaticMethodProxy("addPermission") {
            public Object call(Object who, Method method, Object... args) {
                return Boolean.valueOf(true);
            }
        });
        addMethodProxy(new StaticMethodProxy("checkPermission") {
            public Object call(Object who, Method method, Object... args) throws Throwable {
                return Integer.valueOf(VPackageManager.get().checkPermission((String)args[0], (String)args[1], ((Integer)args[2]).intValue()));
            }
        });
    }
}
