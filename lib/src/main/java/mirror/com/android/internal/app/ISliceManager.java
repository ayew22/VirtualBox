package mirror.com.android.internal.app;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class ISliceManager {
  public static Class<?> TYPE = RefClass.load(ISliceManager.class, "android.app.slice.ISliceManager");
  
  public static class Stub {
    public static Class<?> TYPE = RefClass.load(Stub.class, "android.app.slice.ISliceManager$Stub");
    
    @MethodParams({IBinder.class})
    public static RefStaticMethod<IInterface> asInterface;
  }
}
