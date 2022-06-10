package com.fun.vbox.client.hook.proxies.vivo;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgAndLastUserIdMethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.fun.vbox.client.hook.base.ReplaceLastUidMethodProxy;
import com.fun.vbox.client.hook.base.StaticMethodProxy;

import java.lang.reflect.Method;

import mirror.oem.vivo.IVivoPermissonService;

public class VivoPermissionServiceStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "vivo_permission_service";
  
  public VivoPermissionServiceStub() {
    super(IVivoPermissonService.Stub.TYPE, "vivo_permission_service");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy((MethodProxy)new ReplaceLastUidMethodProxy("checkPermission"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("getAppPermission"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("setAppPermission"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("setWhiteListApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("setBlackListApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("noteStartActivityProcess"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("isBuildInThirdPartApp"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgAndLastUserIdMethodProxy("setOnePermission"));
    addMethodProxy((MethodProxy)new ReplaceCallingPkgAndLastUserIdMethodProxy("setOnePermissionExt"));
    addMethodProxy((MethodProxy)new StaticMethodProxy("checkDelete") {
          public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            if (param1VarArgs[1] instanceof String)
              param1VarArgs[1] = getHostPkg(); 
            replaceLastUserId(param1VarArgs);
            return super.call(param1Object, param1Method, param1VarArgs);
          }
        });
    addMethodProxy((MethodProxy)new ReplaceCallingPkgMethodProxy("isVivoImeiPkg"));
  }
}


/* Location:              F:\何章易\项目文件夹\项目24\va\classes_merge.jar!\com\lody\virtual\client\hook\proxies\oem\vivo\VivoPermissionServiceStub.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */