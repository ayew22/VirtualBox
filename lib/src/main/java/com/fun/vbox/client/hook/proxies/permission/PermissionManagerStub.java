package com.fun.vbox.client.hook.proxies.permission;

import android.annotation.TargetApi;

import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.fun.vbox.client.hook.base.ResultStaticMethodProxy;
import com.fun.vbox.helper.compat.BuildCompat;

import mirror.vbox.permission.IPermissionManager;

@TargetApi(30)
public final class PermissionManagerStub extends BinderInvocationProxy {
    public PermissionManagerStub() {
        super(IPermissionManager.Stub.asInterface, "permissionmgr");
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        addMethodProxy(new ResultStaticMethodProxy("addPermissionAsync",true));
        addMethodProxy(new ResultStaticMethodProxy("addPermission",true));
        addMethodProxy(new ResultStaticMethodProxy("performDexOpt",true));
        addMethodProxy(new ResultStaticMethodProxy("performDexOptIfNeeded",false));
        addMethodProxy(new ResultStaticMethodProxy("performDexOptSecondary",true));
        addMethodProxy(new ResultStaticMethodProxy("addOnPermissionsChangeListener",0));
        addMethodProxy(new ResultStaticMethodProxy("removeOnPermissionsChangeListener",0));
        //addMethodProxy(new ReplaceCallingPkgMethodProxy("checkDeviceIdentifierAccess"));
        addMethodProxy(new ResultStaticMethodProxy("checkDeviceIdentifierAccess",false));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("shouldShowRequestPermissionRationale"));
        if (BuildCompat.isOreo()){
            addMethodProxy(new ResultStaticMethodProxy("notifyDexLoad",0));
            addMethodProxy(new ResultStaticMethodProxy("notifyPackageUse",0));
            addMethodProxy(new ResultStaticMethodProxy("setInstantAppCookie",false));
            addMethodProxy(new ResultStaticMethodProxy("isInstantApp",false));
        }
    }

}