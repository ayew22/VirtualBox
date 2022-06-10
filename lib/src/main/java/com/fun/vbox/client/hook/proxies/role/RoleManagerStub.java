package com.fun.vbox.client.hook.proxies.role;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;

import mirror.vbox.role.IRoleManager;

public class RoleManagerStub extends BinderInvocationProxy {
  public RoleManagerStub() {
    super(IRoleManager.Stub.TYPE, "role");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("isRoleHeld"));
  }
}
