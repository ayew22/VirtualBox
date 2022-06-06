package com.fun.vbox.client.hook.proxies.network;


import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.StaticMethodProxy;
import com.fun.vbox.helper.utils.Reflect;

import java.lang.reflect.Method;

import mirror.vbox.net.ITetheringConnector;


public class TetheringConnectorStub extends BinderInvocationProxy {
  private static final String SERVER_NAME = "tethering";
  
  public TetheringConnectorStub() {
    super(ITetheringConnector.Stub.asInterface, "tethering");
  }
  
  protected void onBindMethods() {
    super.onBindMethods();
    addMethodProxy(new StaticMethodProxy("isTetheringSupported") {
          public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            try {
              Reflect.on(param1VarArgs[2]).call("onResult", new Object[] { Integer.valueOf(3) });
              return null;
            } catch (Exception exception) {
              return super.call(param1Object, param1Method, param1VarArgs);
            } 
          }
        });
  }
}