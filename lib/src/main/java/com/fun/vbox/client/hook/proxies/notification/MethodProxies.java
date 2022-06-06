package com.fun.vbox.client.hook.proxies.notification;

import android.app.Notification;
import android.os.Build;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.utils.MethodParameterUtils;
import com.fun.vbox.client.ipc.VNotificationManager;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.helper.utils.ArrayUtils;

import java.lang.reflect.Method;

/**
 * @author Lody
 */

@SuppressWarnings("unused")
class MethodProxies {

    static class AreNotificationsEnabledForPackage extends MethodProxy {
        public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            String str = (String)param1VarArgs[0];
            return getHostPkg().equals(str) ? param1Method.invoke(param1Object, param1VarArgs) : Boolean.valueOf(VNotificationManager.get().areNotificationsEnabledForPackage(str, getAppUserId()));
        }

        public String getMethodName() {
            return "areNotificationsEnabledForPackage";
        }
    }

    static class CancelAllNotifications extends MethodProxy {
        public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            String str = MethodParameterUtils.replaceFirstAppPkg(param1VarArgs);
            if (VCore.get().isAppInstalled(str)) {
                VNotificationManager.get().cancelAllNotification(str, getAppUserId());
                return Integer.valueOf(0);
            }
            replaceLastUserId(param1VarArgs);
            return param1Method.invoke(param1Object, param1VarArgs);
        }

        public String getMethodName() {
            return "cancelAllNotifications";
        }
    }

    static class CancelNotificationWithTag extends MethodProxy {
        public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            byte b2;
            boolean bool = BuildCompat.isR();
            byte b1 = 2;
            if (bool) {
                b2 = 3;
            } else {
                b2 = 2;
            }
            if (!BuildCompat.isR())
                b1 = 1;
            String str1 = MethodParameterUtils.replaceFirstAppPkg(param1VarArgs);
            replaceLastUserId(param1VarArgs);
            if (getHostPkg().equals(str1))
                return param1Method.invoke(param1Object, param1VarArgs);
            String str2 = (String)param1VarArgs[b1];
            int i = ((Integer)param1VarArgs[b2]).intValue();
            i = VNotificationManager.get().dealNotificationId(i, str1, str2, getAppUserId());
            param1VarArgs[b1] = VNotificationManager.get().dealNotificationTag(i, str1, str2, getAppUserId());
            param1VarArgs[b2] = Integer.valueOf(i);
            return param1Method.invoke(param1Object, param1VarArgs);
        }

        public String getMethodName() {
            return "cancelNotificationWithTag";
        }
    }

    static class EnqueueNotification extends MethodProxy {
        public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            String str = (String)param1VarArgs[0];
            replaceLastUserId(param1VarArgs);
            if (getHostPkg().equals(str))
                return param1Method.invoke(param1Object, param1VarArgs);
            int i = ArrayUtils.indexOfFirst(param1VarArgs, Notification.class);
            int j = ArrayUtils.indexOfFirst(param1VarArgs, Integer.class);
            int k = ((Integer)param1VarArgs[j]).intValue();
            k = VNotificationManager.get().dealNotificationId(k, str, null, getAppUserId());
            param1VarArgs[j] = Integer.valueOf(k);
            Notification notification = (Notification)param1VarArgs[i];
            if (!VNotificationManager.get().dealNotification(k, notification, str))
                return Integer.valueOf(0);
            VNotificationManager.get().addNotification(k, null, str, getAppUserId());
            param1VarArgs[0] = getHostPkg();
            return param1Method.invoke(param1Object, param1VarArgs);
        }

        public String getMethodName() {
            return "enqueueNotification";
        }
    }

    static class EnqueueNotificationWithTag extends MethodProxy {
        public Object call(Object who, Method method, Object... args) throws Throwable {
            boolean bool;
            String pkg = (String)args[0];
            replaceLastUserId(args);
            if (getHostPkg().equals(pkg))
                return method.invoke(who, args);
            int notificationIndex = ArrayUtils.indexOfFirst(args, Notification.class);
            int idIndex = ArrayUtils.indexOfFirst(args, Integer.class);
            int id = ((Integer)args[idIndex]).intValue();
            int tagIndex = (Build.VERSION.SDK_INT >= 18 ? 2 : 1);
            String tag = (String) args[tagIndex];

            id = VNotificationManager.get().dealNotificationId(id, pkg, tag, getAppUserId());
            tag = VNotificationManager.get().dealNotificationTag(id, pkg, tag, getAppUserId());
            args[idIndex] = Integer.valueOf(id);
            args[tagIndex] = tag;
            Notification notification = (Notification)args[notificationIndex];
            if (!VNotificationManager.get().dealNotification(id, notification, pkg))
                return Integer.valueOf(0);
            VNotificationManager.get().addNotification(id, tag, pkg, getAppUserId());
            args[0] = getHostPkg();
            if (Build.VERSION.SDK_INT >= 18 && args[1] instanceof String)
                args[1] = getHostPkg();
            return method.invoke(who, args);
        }

        public String getMethodName() {
            return "enqueueNotificationWithTag";
        }
    }

    static class EnqueueNotificationWithTagPriority extends EnqueueNotificationWithTag {
        public String getMethodName() {
            return "enqueueNotificationWithTagPriority";
        }
    }

    static class GetAppActiveNotifications extends MethodProxy {
        public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            param1VarArgs[0] = getHostPkg();
            replaceLastUserId(param1VarArgs);
            return param1Method.invoke(param1Object, param1VarArgs);
        }

        public String getMethodName() {
            return "getAppActiveNotifications";
        }
    }

    static class SetNotificationsEnabledForPackage extends MethodProxy {
        public Object call(Object param1Object, Method param1Method, Object... param1VarArgs) throws Throwable {
            String str = (String)param1VarArgs[0];
            if (getHostPkg().equals(str))
                return param1Method.invoke(param1Object, param1VarArgs);
            boolean bool = ((Boolean)param1VarArgs[ArrayUtils.indexOfFirst(param1VarArgs, Boolean.class)]).booleanValue();
            VNotificationManager.get().setNotificationsEnabledForPackage(str, bool, getAppUserId());
            return Integer.valueOf(0);
        }

        public String getMethodName() {
            return "setNotificationsEnabledForPackage";
        }
    }
}
