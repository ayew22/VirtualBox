package com.fun.vbox.client.hook.providers;

import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.fixer.ContextFixer;
import com.fun.vbox.client.hook.base.MethodBox;
import com.fun.vbox.helper.compat.BuildCompat;
import com.fun.vbox.helper.utils.Reflect;
import com.fun.vbox.helper.utils.VLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import mirror.vbox.content.IContentProvider;

/**
 * @author Lody
 */

public class ProviderHook implements InvocationHandler {

    public static final String QUERY_ARG_SQL_SELECTION = "android:query-arg-sql-selection";

    public static final String QUERY_ARG_SQL_SELECTION_ARGS =
            "android:query-arg-sql-selection-args";
    public static final String QUERY_ARG_SQL_SORT_ORDER = "android:query-arg-sql-sort-order";


    private static final Map<String, HookFetcher> PROVIDER_MAP = new HashMap<>();

    static {
        PROVIDER_MAP.put("settings", new HookFetcher() {
            @Override
            public ProviderHook fetch(boolean external, IInterface provider) {
                return new SettingsProviderHook(provider);
            }
        });
        PROVIDER_MAP.put("downloads", new HookFetcher() {
            @Override
            public ProviderHook fetch(boolean external, IInterface provider) {
                return new DownloadProviderHook(provider);
            }
        });
        PROVIDER_MAP.put("com.android.badge", new HookFetcher() {
            @Override
            public ProviderHook fetch(boolean external, IInterface provider) {
                return new BadgeProviderHook(provider);
            }
        });
        PROVIDER_MAP.put("com.huawei.android.launcher.settings", new HookFetcher() {
            @Override
            public ProviderHook fetch(boolean external, IInterface provider) {
                return new BadgeProviderHook(provider);
            }
        });
    }

    protected final Object mBase;

    public ProviderHook(Object base) {
        this.mBase = base;
    }

    private static HookFetcher fetchHook(String authority) {
        HookFetcher fetcher = PROVIDER_MAP.get(authority);
        if (fetcher == null) {
            fetcher = new HookFetcher() {
                @Override
                public ProviderHook fetch(boolean external, IInterface provider) {
                    if (external) {
                        return new ExternalProviderHook(provider);
                    }
                    return new InternalProviderHook(provider);
                }
            };
        }
        return fetcher;
    }

    private static IInterface createProxy(IInterface provider, ProviderHook hook) {
        if (provider == null || hook == null) {
            return null;
        }
        return (IInterface) Proxy.newProxyInstance(provider.getClass().getClassLoader(), new Class[]{
                IContentProvider.TYPE,
        }, hook);
    }

    public static IInterface createProxy(boolean external, String authority, IInterface provider) {
        if (provider instanceof Proxy && Proxy.getInvocationHandler(provider) instanceof ProviderHook) {
            return provider;
        }
        ProviderHook.HookFetcher fetcher = ProviderHook.fetchHook(authority);
        if (fetcher != null) {
            ProviderHook hook = fetcher.fetch(external, provider);
            IInterface proxyProvider = ProviderHook.createProxy(provider, hook);
            if (proxyProvider != null) {
                provider = proxyProvider;
            }
        }
        return provider;
    }

    public Bundle call(MethodBox methodBox, String method, String arg, Bundle extras) throws InvocationTargetException {
        return methodBox.call();
    }

    public Uri insert(MethodBox methodBox, Uri url, ContentValues initialValues) throws InvocationTargetException {

        return (Uri) methodBox.call();
    }

    public Cursor query(MethodBox methodBox, Uri url, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder, Bundle originQueryArgs) throws InvocationTargetException {
        return (Cursor) methodBox.call();
    }

    public String getType(MethodBox methodBox, Uri url) throws InvocationTargetException {
        return (String) methodBox.call();
    }

    public int bulkInsert(MethodBox methodBox, Uri url, ContentValues[] initialValues) throws InvocationTargetException {
        return (int) methodBox.call();
    }

    public int delete(MethodBox methodBox, Uri url, String selection, String[] selectionArgs) throws InvocationTargetException {
        return (int) methodBox.call();
    }

    public int update(MethodBox methodBox, Uri url, ContentValues values, String selection,
                      String[] selectionArgs) throws InvocationTargetException {
        return (int) methodBox.call();
    }

    public ParcelFileDescriptor openFile(MethodBox methodBox, Uri url, String mode) throws InvocationTargetException {
        return (ParcelFileDescriptor) methodBox.call();
    }

    public AssetFileDescriptor openAssetFile(MethodBox methodBox, Uri url, String mode) throws InvocationTargetException {
        return (AssetFileDescriptor) methodBox.call();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
        try {
            processArgs(method, args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        MethodBox methodBox = new MethodBox(method, mBase, args);
        int start = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? 1 : 0;
        if (BuildCompat.isS()) {
            // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r16:frameworks/base/core/java/android/content/IContentProvider.java
            start = 1;
        } else if (BuildCompat.isR()) {
            // Android 11: https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/IContentProvider.java?q=IContentProvider&ss=android%2Fplatform%2Fsuperproject
            start = 2;
        }
        if (BuildCompat.isS() ) {
            tryFixAttributionSource(args);
        }
        try {
            String name = method.getName();
            if ("call".equals(name)) {
                Bundle extras;
                if (BuildCompat.isS()) {
                    // What's the fuck with Google???
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/core/java/android/content/IContentProvider.java;l=123
                    start = 2;
                } else if (BuildCompat.isR()) {
                    start = 3;
                } else if (BuildCompat.isQ()) {
                    start = 2;
                }
                try {
                    extras = (Bundle) args[start + 2];
                } catch (Throwable ignore) {
                    if (BuildCompat.isR()) {
                        start = 3;
                    } else if (BuildCompat.isQ()) {
                        start = 2;
                    }
                    extras = (Bundle) args[start + 2];
                }
                String methodName = (String) args[start];
                String arg = (String) args[start + 1];
                return call(methodBox, methodName, arg, extras);
            } else if ("insert".equals(name)) {
                Uri url = (Uri) args[start];
                ContentValues initialValues = (ContentValues) args[start + 1];
                return insert(methodBox, url, initialValues);
            } else if ("getType".equals(name)) {
                return getType(methodBox, (Uri) args[0]);
            } else if ("delete".equals(name)) {
                Uri url = (Uri) args[start];
                String selection = (String) args[start + 1];
                String[] selectionArgs = (String[]) args[start + 2];
                return delete(methodBox, url, selection, selectionArgs);
            } else if ("bulkInsert".equals(name)) {
                Uri url = (Uri) args[start];
                ContentValues[] initialValues = (ContentValues[]) args[start + 1];
                return bulkInsert(methodBox, url, initialValues);
            } else if ("update".equals(name)) {
                Uri url = (Uri) args[start];
                ContentValues values = (ContentValues) args[start + 1];
                String selection = (String) args[start + 2];
                String[] selectionArgs = (String[]) args[start + 3];
                return update(methodBox, url, values, selection, selectionArgs);
            } else if ("openFile".equals(name)) {
                Uri url = (Uri) args[start];
                String mode = (String) args[start + 1];
                return openFile(methodBox, url, mode);
            } else if ("openAssetFile".equals(name)) {
                Uri url = (Uri) args[start];
                String mode = (String) args[start + 1];
                return openAssetFile(methodBox, url, mode);
            } else if ("query".equals(name)) {
                Uri url;
                try {
                    url = (Uri) args[start];
                } catch (Throwable ignore) {
                    start = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? 1 : 0;
                    url = (Uri) args[start];
                }
               /* if (BuildCompat.isS()) {
                    fixAttributionSource(args[0]);
                }*/
                String[] projection = (String[]) args[start + 1];
                String selection = null;
                String[] selectionArgs = null;
                String sortOrder = null;
                Bundle queryArgs = null;
                if (BuildCompat.isOreo()) {
                    queryArgs = (Bundle) args[start + 2];
                    if (queryArgs != null) {
                        selection = queryArgs.getString(QUERY_ARG_SQL_SELECTION);
                        selectionArgs = queryArgs.getStringArray(QUERY_ARG_SQL_SELECTION_ARGS);
                        sortOrder = queryArgs.getString(QUERY_ARG_SQL_SORT_ORDER);
                    }
                } else {
                    selection = (String) args[start + 2];
                    selectionArgs = (String[]) args[start + 3];
                    sortOrder = (String) args[start + 4];
                }
                return query(methodBox, url, projection, selection, selectionArgs, sortOrder, queryArgs);
            }
            return methodBox.call();
        } catch (Throwable e) {
            VLog.w("ProviderHook", "call: %s (%s) with error", method.getName(), Arrays.toString(args));
            if (e instanceof InvocationTargetException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    protected void processArgs(Method method, Object... args) {

    }

    public interface HookFetcher {
        ProviderHook fetch(boolean external, IInterface provider);
    }


   /* private void fixAttributionSource(Object attribution) {
        try {
            Object mAttributionSourceState = Reflect.on(attribution).get("mAttributionSourceState");
            Reflect.on(mAttributionSourceState).set("uid", VCore.get().myUid());
            Reflect.on(mAttributionSourceState).set("packageName", VCore.get().getHostPkg());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }*/

    private void tryFixAttributionSource(Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        Object attribution = args[0];
        if (attribution == null) {
            return;
        }
        if (!attribution.getClass().getName().equals("android.content.AttributionSource")) {
            return;
        }

        ContextFixer.fixAttributionSource(attribution, VCore.get().getHostPkg(), VCore.get().myUid());
    }
}
