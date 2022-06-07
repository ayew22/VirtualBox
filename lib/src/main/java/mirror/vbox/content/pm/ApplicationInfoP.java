package mirror.vbox.content.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.SharedLibraryInfo;

import java.util.List;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;
import mirror.RefObject;

public class ApplicationInfoP {
    public static Class<?> TYPE = RefClass.load(ApplicationInfoP.class, ApplicationInfo.class);
    @MethodParams(int.class)
    public static RefMethod<Void> setHiddenApiEnforcementPolicy;

    public static RefObject<List<SharedLibraryInfo>> sharedLibraryInfos;

}
