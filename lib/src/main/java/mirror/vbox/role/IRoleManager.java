package mirror.vbox.role;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IRoleManager {
  public static Class<?> TYPE = RefClass.load(IRoleManager.class, "android.app.role.IRoleManager");
  
  public static class Stub {
    public static Class<?> TYPE = RefClass.load(Stub.class, "android.app.role.IRoleManager$Stub");
    
    @MethodParams({IBinder.class})
    public static RefStaticMethod<IInterface> asInterface;
  }
}