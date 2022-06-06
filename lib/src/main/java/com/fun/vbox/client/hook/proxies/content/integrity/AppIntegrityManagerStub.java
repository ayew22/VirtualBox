package com.fun.vbox.client.hook.proxies.content.integrity;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.ResultStaticMethodProxy;
import com.fun.vbox.helper.compat.ParceledListSliceCompat;

import java.util.Collections;

import mirror.vbox.content.integrity.IAppIntegrityManager;


public class AppIntegrityManagerStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "app_integrity";
  
  public AppIntegrityManagerStub() {
    super(IAppIntegrityManager.Stub.asInterface, "app_integrity");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy(new ResultStaticMethodProxy("updateRuleSet", null));
    addMethodProxy(new ResultStaticMethodProxy("getCurrentRuleSetVersion", ""));
    addMethodProxy(new ResultStaticMethodProxy("getCurrentRuleSetProvider", ""));
    addMethodProxy(new ResultStaticMethodProxy("getCurrentRules", ParceledListSliceCompat.create(Collections.emptyList())));
    addMethodProxy(new ResultStaticMethodProxy("getWhitelistedRuleProviders", Collections.emptyList()));
  }
}