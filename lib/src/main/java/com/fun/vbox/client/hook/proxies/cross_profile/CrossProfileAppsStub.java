package com.fun.vbox.client.hook.proxies.cross_profile;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.fun.vbox.client.hook.base.ResultStaticMethodProxy;

import mirror.vbox.content.pm.ICrossProfileApps;

public class CrossProfileAppsStub extends BinderInvocationProxy {
    public CrossProfileAppsStub() {
        super(ICrossProfileApps.Stub.asInterface, "crossprofileapps");
    }

    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy((MethodProxy) new ReplaceCallingPkgMethodProxy("getTargetUserProfiles"));
        addMethodProxy((MethodProxy) new ResultStaticMethodProxy("startActivityAsUser", null));
    }
}