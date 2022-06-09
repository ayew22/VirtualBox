package mirror.vbox.content;


import android.os.IBinder;
import android.os.Parcelable;

import mirror.RefClass;
import mirror.RefInt;
import mirror.RefObject;

public class AttributionSourceState {
    public static Class<?> TYPE = RefClass.load((Class<?>) AttributionSourceState.class, "android.content.AttributionSourceState");
    public static RefObject<Parcelable[]> next;
    public static RefObject<String> packageName;
    public static RefObject<IBinder> token;
    public static RefObject<Integer> uid;

    public static void next(Object obj, Parcelable[] parcelableArr) {
        RefObject<Parcelable[]> field = next;
        if (field != null) {
            field.set(obj, parcelableArr);
        }
    }

    public static void packageName(Object obj, String str) {
        RefObject<String> field = packageName;
        if (field != null) {
            field.set(obj, str);
        }
    }

    public static void token(Object obj, IBinder iBinder) {
        RefObject<IBinder> field = token;
        if (field != null) {
            field.set(obj, iBinder);
        }
    }

    public static void uid(Object obj, int i2) {
        RefObject<Integer> field = uid;
        if (field != null) {
            field.set(obj, Integer.valueOf(i2));
        }
    }

    public static Parcelable[] next(Object obj) {
        RefObject<Parcelable[]> field = next;
        if (field != null) {
            return field.get(obj);
        }
        return null;
    }

    public static String packageName(Object obj) {
        RefObject<String> field = packageName;
        if (field != null) {
            return field.get(obj);
        }
        return null;
    }

    public static IBinder token(Object obj) {
        RefObject<IBinder> field = token;
        if (field != null) {
            return field.get(obj);
        }
        return null;
    }

    /*public static int uid(Object obj) {
        RefObject<Integer> field = uid;
        if (field != null) {
            return field.get(obj).intValue();
        }
        return Process.myUid();
    }*/

}
