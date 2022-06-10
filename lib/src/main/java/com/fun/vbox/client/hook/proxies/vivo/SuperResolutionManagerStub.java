package com.fun.vbox.client.hook.proxies.vivo;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;

import mirror.oem.vivo.ISuperResolutionManager;

public class SuperResolutionManagerStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "SuperResolutionManager";
  
  public SuperResolutionManagerStub() {
    super(ISuperResolutionManager.Stub.TYPE, "SuperResolutionManager");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("registerPackageSettingStateChangeListener"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("unRegisterPackageSettingStateChangeListener"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("registerSuperResolutionStateChange"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("unRegisterSuperResolutionStateChange"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("getPackageSettingState"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("putPackageSettingState"));
  }
}


/* Location:              F:\何章易\项目文件夹\项目24\va\classes_merge.jar!\com\lody\virtual\client\hook\proxies\oem\vivo\SuperResolutionManagerStub.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */