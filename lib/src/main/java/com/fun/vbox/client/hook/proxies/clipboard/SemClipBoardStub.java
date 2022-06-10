package com.fun.vbox.client.hook.proxies.clipboard;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceLastPkgMethodProxy;

import mirror.vbox.sec.clipboard.IClipboardService;

public class SemClipBoardStub extends BinderInvocationProxy {
  public SemClipBoardStub() {
    super(IClipboardService.Stub.asInterface, "semclipboard");
  }
  
  public void inject() throws Throwable {
    super.inject();
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceLastPkgMethodProxy("getClipData"));
  }
}