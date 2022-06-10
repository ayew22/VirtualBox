package com.fun.vbox.client.hook.proxies.device;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;

import mirror.vbox.os.IDeviceIdleController;

public class DeviceIdleControllerStub extends BinderInvocationProxy {
  public DeviceIdleControllerStub() {
    super(IDeviceIdleController.Stub.asInterface, "deviceidle");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("addPowerSaveWhitelistApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("removePowerSaveWhitelistApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("removeSystemPowerWhitelistApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("restoreSystemPowerWhitelistApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("isPowerSaveWhitelistExceptIdleApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("isPowerSaveWhitelistApp"));
  }
}
