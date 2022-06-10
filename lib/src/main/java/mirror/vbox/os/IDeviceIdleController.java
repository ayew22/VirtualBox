package mirror.vbox.os;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IDeviceIdleController {
  public static Class<?> TYPE = RefClass.load(IDeviceIdleController.class, "android.os.IDeviceIdleController");
  
  public static class Stub {
    public static Class<?> TYPE = RefClass.load(Stub.class, "android.os.IDeviceIdleController$Stub");
    
    @MethodParams({IBinder.class})
    public static RefStaticMethod<IInterface> asInterface;
  }
}