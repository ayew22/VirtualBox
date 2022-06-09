package com.fun.vbox.client.hook.proxies.appops;

import android.app.SyncNotedAppOp;

import com.fun.vbox.GmsSupport;
import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.utils.MethodParameterUtils;
import com.fun.vbox.helper.compat.BuildCompat;

import java.lang.reflect.Method;

import mirror.vbox.content.AttributionSource;

public class MethodProxies {
    private static void replaceUidAndPackage(Object[] args, int pkgIndex) {
        args[pkgIndex] = VCore.get().getHostPkg();
        int uidIndex = pkgIndex - 1;
        if (args[pkgIndex - 1] instanceof Integer) {
            args[uidIndex] = Integer.valueOf(VCore.get().myUid());
        }
    }

    public static Object checkAudioOperation(Object who, Method method, Object[] args) throws Throwable {
        replaceUidAndPackage(args, 3);
        return method.invoke(who, args);
    }

    public static Object checkOperation(Object who, Method method, Object[] args) throws Throwable {
        replaceUidAndPackage(args, 2);
        return method.invoke(who, args);
    }

    public static Object checkPackage(Object who, Method method, Object[] args) throws Throwable {
        if (GmsSupport.isGoogleAppOrService((String) args[1])) {
            return 0;
        }
        replaceUidAndPackage(args, 1);
        return method.invoke(who, args);
    }

    public static Object getOpsForPackage(Object who, Method method, Object[] args) throws Throwable {
        replaceUidAndPackage(args, 1);
        return method.invoke(who, args);
    }

    public static Object getPackagesForOps(Object who, Method method, Object[] args) throws Throwable {
        return method.invoke(who, args);
    }

    public static Object noteOperation(Object who, Method method, Object[] args) throws Throwable {
        replaceUidAndPackage(args, 2);
        return method.invoke(who, args);
    }

    public static Object noteProxyOperation(Object who, Method method, Object[] args) throws Throwable {
        int index;
        if (!BuildCompat.isS() || (index = MethodParameterUtils.getIndex(args, AttributionSource.TYPE)) < 0) {
            return 0;
        }
        return new SyncNotedAppOp(0, AttributionSource.getAttributionTag(args[index]));
    }

    public static Object resetAllModes(Object who, Method method, Object[] args) throws Throwable {
        args[0] = 0;
        args[1] = VCore.get().getHostPkg();
        return method.invoke(who, args);
    }

    public static Object startOperation(Object who, Method method, Object[] args) throws Throwable {
        replaceUidAndPackage(args, 3);
        return method.invoke(who, args);
    }

    public static Object finishOperation(Object who, Method method, Object[] args) throws Throwable {
        replaceUidAndPackage(args, 3);
        return method.invoke(who, args);
    }

    public static Object checkOperationRaw(Object who, Method method, Object[] args) throws Throwable {
        replaceUidAndPackage(args, 2);
        return method.invoke(who, args);
    }

    public static Object startWatchingAsyncNoted(Object who, Method method, Object[] args) throws Throwable {
        args[0] = VCore.get().getHostPkg();
        return method.invoke(who, args);
    }

    public static Object stopWatchingAsyncNoted(Object who, Method method, Object[] args) throws Throwable {
        args[0] = VCore.get().getHostPkg();
        return method.invoke(who, args);
    }

    public static Object extractAsyncOps(Object who, Method method, Object[] args) throws Throwable {
        args[0] = VCore.get().getHostPkg();
        return method.invoke(who, args);
    }

}
