package mirror.oem.vivo;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IPopupCameraManager {
  public static Class<?> TYPE = RefClass.load(IPopupCameraManager.class, "vivo.app.popupcamera.IPopupCameraManager");
  
  public static class Stub {
    public static Class<?> TYPE = RefClass.load(Stub.class, "vivo.app.popupcamera.IPopupCameraManager$Stub");
    
    @MethodParams({IBinder.class})
    public static RefStaticMethod<IInterface> asInterface;
  }
}