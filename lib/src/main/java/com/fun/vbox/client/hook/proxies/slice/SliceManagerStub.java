package com.fun.vbox.client.hook.proxies.slice;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ResultStaticMethodProxy;

import java.util.Collections;

import mirror.com.android.internal.app.ISliceManager;

public class SliceManagerStub extends BinderInvocationProxy {
  public SliceManagerStub() {
    super(ISliceManager.Stub.TYPE, "slice");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("pinSlice", null));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("unpinSlice", null));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("hasSliceAccess", Boolean.valueOf(false)));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("grantSlicePermission", null));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("revokeSlicePermission", null));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("checkSlicePermission", Integer.valueOf(0)));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("grantPermissionFromUser", null));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("getPinnedSpecs", Collections.EMPTY_LIST.toArray()));
    addMethodProxy((MethodProxy)new ResultStaticMethodProxy("getPinnedSlices", Collections.EMPTY_LIST.toArray()));
  }
}