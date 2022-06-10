package com.fun.vbox.client.hook.proxies.vivo;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.fun.vbox.client.hook.base.ReplaceLastPkgMethodProxy;

import mirror.oem.vivo.ISystemDefenceManager;

public class SystemDefenceManagerStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "system_defence_service";
  
  public SystemDefenceManagerStub() {
    super(ISystemDefenceManager.Stub.TYPE, "system_defence_service");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("checkTransitionTimoutErrorDefence"));
    addMethodProxy((MethodProxy)new ReplaceLastPkgMethodProxy("checkSkipKilledByRemoveTask"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("checkSmallIconNULLPackage"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("checkDelayUpdate"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("onSetActivityResumed"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("checkReinstallPacakge"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("reportFgCrashData"));
  }
}
