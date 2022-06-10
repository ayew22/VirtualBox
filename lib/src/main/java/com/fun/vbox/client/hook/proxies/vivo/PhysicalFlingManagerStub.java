package com.fun.vbox.client.hook.proxies.vivo;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;

import mirror.oem.vivo.IPhysicalFlingManagerStub;

public class PhysicalFlingManagerStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "physical_fling_service";
  
  public PhysicalFlingManagerStub() {
    super(IPhysicalFlingManagerStub.Stub.TYPE, "physical_fling_service");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("isSupportPhysicalFling"));
  }
}