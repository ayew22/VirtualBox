package mirror.vbox.content;


import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;
import mirror.RefObject;

public class AttributionSource {
    private static final String TAG = "AttributionSource";
    public static Class<?> TYPE;
    public static Class<?> TYPE_COMP;
    @MethodParams({Object.class})
    public static RefMethod<Boolean> equals;
    public static RefMethod<String> getAttributionTag;
    public static RefMethod<String> getPackageName;
    public static RefMethod<IBinder> getToken;
    public static RefObject<Object> mAttributionSourceState;
    @MethodParams({Binder.class})
    public static RefMethod<Parcelable> withToken;

    static {
        Class<AttributionSource> cls = AttributionSource.class;
        TYPE = RefClass.load((Class<?>) cls, "android.content.AttributionSource");
        TYPE_COMP = RefClass.load((Class<?>) cls, "android.content.AttributionSource$ay");
        Class<?> cls2 = TYPE_COMP;
    }

    public static boolean equals(Object obj, Object obj2) {
        RefMethod<Boolean> method = equals;
        if (method == null) {
            return false;
        }
        return method.call(method, obj2).booleanValue();
    }

    public static String getAttributionTag(Object obj) {
        RefMethod<String> method = getAttributionTag;
        if (method != null) {
            return method.call(obj, new Object[0]);
        }
        return null;
    }

    public static String getPackageName(Object obj) {
        RefMethod<String> method = getPackageName;
        if (method != null) {
            return method.call(obj, new Object[0]);
        }
        return null;
    }

    public static IBinder getToken(Object obj) {
        RefMethod<IBinder> method = getToken;
        if (method != null) {
            return method.call(obj, new Object[0]);
        }
        return null;
    }

    public static void mAttributionSourceState(Object obj, Object obj2) {
        RefObject<Object> objRef = mAttributionSourceState;
        if (objRef != null) {
            objRef.set(obj, obj2);
        }
    }

    public static Parcelable newInstance(Object obj) {
        if (obj != null) {
            Parcelable withToken2 = withToken(obj, (Binder) null);
            Parcelable withToken22 = withToken2;
            if (withToken2 != null) {
                Object mAttributionSourceState2 = mAttributionSourceState(withToken22);
                Object mAttributionSourceState22 = mAttributionSourceState2;
                if (mAttributionSourceState2 != null) {
                    AttributionSourceState.token(mAttributionSourceState22, getToken(obj));
                    Object mAttributionSourceState3 = mAttributionSourceState(obj);
                    if (mAttributionSourceState3 != null) {
                        AttributionSourceState.next(mAttributionSourceState22, AttributionSourceState.next(mAttributionSourceState3));
                    }
                    return withToken22;
                }
            }
        }
        Log.w(TAG, "newInstance reduce failed,return null");
        return null;
    }

    public static Parcelable withToken(Object obj, Binder binder) {
        RefMethod<Parcelable> method = withToken;
        if (method == null) {
            return null;
        }
        return method.call(obj, binder);
    }

    public static Object mAttributionSourceState(Object obj) {
        RefObject<Object> objRef = mAttributionSourceState;
        if (objRef != null) {
            return objRef.get(obj);
        }
        return null;
    }
}