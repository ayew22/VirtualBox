package mirror.vbox.content.integrity;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IAppIntegrityManager {
  public static Class<?> TYPE = RefClass.load(IAppIntegrityManager.class, "android.content.integrity.IAppIntegrityManager");
  
  public static class Stub {
    public static Class<?> TYPE = RefClass.load(Stub.class, "android.content.integrity.IAppIntegrityManager$Stub");
    
    @MethodParams({IBinder.class})
    public static RefStaticMethod<IInterface> asInterface;
  }
}