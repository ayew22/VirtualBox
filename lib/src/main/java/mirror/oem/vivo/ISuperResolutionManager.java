package mirror.oem.vivo;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class ISuperResolutionManager {
  public static Class<?> TYPE = RefClass.load(ISuperResolutionManager.class, "vivo.app.superresolution.ISuperResolutionManager");
  
  public static class Stub {
    public static Class<?> TYPE = RefClass.load(Stub.class, "vivo.app.superresolution.ISuperResolutionManager$Stub");
    
    @MethodParams({IBinder.class})
    public static RefStaticMethod<IInterface> asInterface;
  }
}