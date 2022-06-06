package mirror.vbox.app;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefObject;
import mirror.RefStaticMethod;
import mirror.RefStaticObject;

public class ActivityClient {
  public static RefStaticObject<Object> INTERFACE_SINGLETON;
  
  public static Class<?> TYPE = RefClass.load(ActivityClient.class, "android.app.ActivityClient");
  
  public static RefStaticMethod<IInterface> getActivityClientController;
  
  public static class ActivityClientControllerSingleton {
    public static Class<?> TYPE = RefClass.load(ActivityClientControllerSingleton.class, "android.app.ActivityClient$ActivityClientControllerSingleton");
    
    public static RefObject<IInterface> mKnownInstance;
  }
}

