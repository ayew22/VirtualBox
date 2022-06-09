package com.fun.vbox.client.hook.proxies.content;


import android.content.pm.ProviderInfo;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.Build;

import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.ipc.VContentManager;
import com.fun.vbox.client.ipc.VPackageManager;
import com.fun.vbox.helper.Keep;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.os.VUserHandle;

import java.lang.reflect.Method;

/**
 * @author Lody
 */
@Keep
public class MethodProxies {

    private static boolean isAppUri(Uri uri) {
        ProviderInfo info = VPackageManager.get().resolveContentProvider(uri.getAuthority(), 0, VUserHandle.myUserId());
        if (info == null || !info.enabled) {
            return false;
        }
        return true;
    }

    public static Object registerContentObserver(Object who, Method method, Object[] args) throws Throwable {
        if (Build.VERSION.SDK_INT >= 24 && args.length >= 5) {
            args[4] = 22;
        }
        Uri uri = (Uri) args[0];
        boolean notifyForDescendents = (boolean) args[1];
        IContentObserver observer = (IContentObserver) args[2];
        if (isAppUri(uri)) {
            VContentManager.get().registerContentObserver(uri, notifyForDescendents, observer, VUserHandle.myUserId());
            return 0;
        }
        MethodProxy.replaceFirstUserId(args);
        return method.invoke(who, args);
    }

    public static Object unregisterContentObserver(Object who, Method method, Object[] args) throws Throwable {
        VContentManager.get().unregisterContentObserver((IContentObserver) args[0]);
        return method.invoke(who, args);
    }

    public static Object notifyChange(Object who, Method method, Object[] args) throws Throwable {
        Object obj = who;
        Method method2 = method;
        Object[] objArr = args;
        if (Build.VERSION.SDK_INT >= 24 && objArr.length >= 6) {
            objArr[5] = 22;
        }
        boolean syncToNetwork = true;
        IContentObserver observer = (IContentObserver) objArr[1];
        boolean observerWantsSelfNotifications = ((Boolean) objArr[2]).booleanValue();
        if (!(objArr[3] instanceof Integer)) {
            syncToNetwork = ((Boolean) objArr[3]).booleanValue();
        } else if ((((Integer) objArr[3]).intValue() & 1) == 0) {
            syncToNetwork = false;
        }
        if (BuildCompat.isR()) {
            MethodProxy.replaceLastUserId(args);
            for (Uri uri : (Uri[]) objArr[0]) {
                if (isAppUri(uri)) {
                    VContentManager.get().notifyChange(uri, observer, observerWantsSelfNotifications, syncToNetwork, VUserHandle.myUserId());
                } else {
                    method2.invoke(obj, objArr);
                }
            }
            return 0;
        }
        Uri uri2 = (Uri) objArr[0];
        if (isAppUri(uri2)) {
            VContentManager.get().notifyChange(uri2, observer, observerWantsSelfNotifications, syncToNetwork, VUserHandle.myUserId());
            return 0;
        }
        MethodProxy.replaceLastUserId(args);
        return method2.invoke(obj, objArr);
    }

}
