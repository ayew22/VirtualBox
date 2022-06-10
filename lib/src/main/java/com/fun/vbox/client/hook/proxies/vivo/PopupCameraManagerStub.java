package com.fun.vbox.client.hook.proxies.vivo;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceLastPkgMethodProxy;

import mirror.oem.vivo.IPopupCameraManager;

public class PopupCameraManagerStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "popup_camera_service";
  
  public PopupCameraManagerStub() {
    super(IPopupCameraManager.Stub.TYPE, "popup_camera_service");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceLastPkgMethodProxy("notifyCameraStatus"));
  }
}