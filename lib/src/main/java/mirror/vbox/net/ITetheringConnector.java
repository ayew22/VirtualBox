package mirror.vbox.net;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class ITetheringConnector {
  public static Class<?> TYPE = RefClass.load(ITetheringConnector.class, "android.net.ITetheringConnector");
  
  public static class Stub {
    public static Class<?> TYPE = RefClass.load(Stub.class, "android.net.ITetheringConnector$Stub");
    
    @MethodParams({IBinder.class})
    public static RefStaticMethod<IInterface> asInterface;
  }
}