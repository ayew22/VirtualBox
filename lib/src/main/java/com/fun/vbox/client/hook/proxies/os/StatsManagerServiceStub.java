package com.fun.vbox.client.hook.proxies.os;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.ResultStaticMethodProxy;

import mirror.vbox.os.IStatsManagerService;

public class StatsManagerServiceStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "statsmanager";
  
  public StatsManagerServiceStub() {
    super(IStatsManagerService.Stub.asInterface, "statsmanager");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy(new ResultStaticMethodProxy("setDataFetchOperation", null));
    addMethodProxy(new ResultStaticMethodProxy("removeDataFetchOperation", null));
    addMethodProxy(new ResultStaticMethodProxy("setActiveConfigsChangedOperation", new long[0]));
    addMethodProxy(new ResultStaticMethodProxy("removeActiveConfigsChangedOperation", null));
    addMethodProxy(new ResultStaticMethodProxy("setBroadcastSubscriber", null));
    addMethodProxy(new ResultStaticMethodProxy("unsetBroadcastSubscriber", null));
    addMethodProxy(new ResultStaticMethodProxy("getRegisteredExperimentIds", new long[0]));
    addMethodProxy(new ResultStaticMethodProxy("getMetadata", new byte[0]));
    addMethodProxy(new ResultStaticMethodProxy("getData", new byte[0]));
    addMethodProxy(new ResultStaticMethodProxy("addConfiguration", null));
    addMethodProxy(new ResultStaticMethodProxy("registerPullAtomCallback", null));
    addMethodProxy(new ResultStaticMethodProxy("unregisterPullAtomCallback", null));
  }
}
