package com.zb.vv.client.stub;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.ipc.VActivityManager;
import com.fun.vbox.client.ipc.VPackageManager;
import com.fun.vbox.client.stub.StubManifest;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

import mirror.vbox.content.ContentProviderClientICS;
import mirror.vbox.content.ContentProviderClientJB;

/**
 * @author Lody
 */
public class ContentProviderProxy extends ContentProvider {

    private ContentProviderClient acquireProviderClient(TargetProviderInfo paramTargetProviderInfo) {
        try {
            IInterface iInterface = VActivityManager.get().acquireProviderClient(paramTargetProviderInfo.userId, paramTargetProviderInfo.info);
            if (iInterface != null)
                return (Build.VERSION.SDK_INT > 15) ? (ContentProviderClient)ContentProviderClientJB.ctor.newInstance(new Object[] { getContext().getContentResolver(), iInterface, Boolean.valueOf(true) }) : (ContentProviderClient)ContentProviderClientICS.ctor.newInstance(new Object[] { getContext().getContentResolver(), iInterface });
        } catch (RemoteException remoteException) {
            remoteException.printStackTrace();
        }
        return null;
    }

    public static Uri buildProxyUri(int paramInt, boolean paramBoolean, String paramString, Uri paramUri) {
        String str = StubManifest.getProxyAuthority(paramBoolean);
        return Uri.withAppendedPath(Uri.parse(String.format(Locale.ENGLISH, "content://%1$s/%2$d/%3$s", new Object[] { str, Integer.valueOf(paramInt), paramString })), paramUri.toString());
    }

    private TargetProviderInfo getProviderProviderInfo(Uri paramUri) {
        if (!VCore.get().isEngineLaunched())
            return null;
        List<String> list = paramUri.getPathSegments();
        if (list != null && list.size() >= 3) {
            int b;
            try {
                b = Integer.parseInt(list.get(0));
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
                b = -1;
            }
            if (b == -1)
                return null;
            String str = list.get(1);
            ProviderInfo providerInfo = VPackageManager.get().resolveContentProvider(str, 0, b);
            if (providerInfo != null && providerInfo.enabled) {
                String str1 = paramUri.toString();
                str = str1.substring(str.length() + str1.indexOf(str, 1) + 1);
                str1 = str;
                if (str.startsWith("content:/")) {
                    str1 = str;
                    if (!str.startsWith("content://"))
                        str1 = str.replace("content:/", "content://");
                }
                return new TargetProviderInfo(b, providerInfo, Uri.parse(str1));
            }
        }
        return null;
    }

    public ContentProviderClient acquireTargetProviderClient(Uri paramUri) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        return (targetProviderInfo != null) ? acquireProviderClient(targetProviderInfo) : null;
    }

    public Uri canonicalize(Uri paramUri) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.canonicalize(targetProviderInfo.uri);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return null;
    }

    public int delete(Uri paramUri, String paramString, String[] paramArrayOfString) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.delete(targetProviderInfo.uri, paramString, paramArrayOfString);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return 0;
    }

    public String[] getStreamTypes(Uri paramUri, String paramString) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.getStreamTypes(targetProviderInfo.uri, paramString);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return null;
    }

    public String getType(Uri paramUri) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.getType(targetProviderInfo.uri);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return null;
    }

    public Uri insert(Uri paramUri, ContentValues paramContentValues) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.insert(targetProviderInfo.uri, paramContentValues);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return null;
    }

    public boolean onCreate() {
        return true;
    }

    public ParcelFileDescriptor openFile(Uri paramUri, String paramString) throws FileNotFoundException {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.openFile(targetProviderInfo.uri, paramString);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return null;
    }

    public Cursor query(Uri paramUri, String[] paramArrayOfString1, String paramString1, String[] paramArrayOfString2, String paramString2) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.query(targetProviderInfo.uri, paramArrayOfString1, paramString1, paramArrayOfString2, paramString2);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return null;
    }

    public boolean refresh(Uri paramUri, Bundle paramBundle, CancellationSignal paramCancellationSignal) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.refresh(targetProviderInfo.uri, paramBundle, paramCancellationSignal);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return false;
    }

    public Uri uncanonicalize(Uri paramUri) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.uncanonicalize(targetProviderInfo.uri);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return paramUri;
    }

    public int update(Uri paramUri, ContentValues paramContentValues, String paramString, String[] paramArrayOfString) {
        TargetProviderInfo targetProviderInfo = getProviderProviderInfo(paramUri);
        if (targetProviderInfo != null) {
            ContentProviderClient contentProviderClient = acquireProviderClient(targetProviderInfo);
            if (contentProviderClient != null)
                try {
                    return contentProviderClient.update(targetProviderInfo.uri, paramContentValues, paramString, paramArrayOfString);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
        }
        return 0;
    }

    private class TargetProviderInfo {
        ProviderInfo info;

        Uri uri;

        int userId;

        TargetProviderInfo(int param1Int, ProviderInfo param1ProviderInfo, Uri param1Uri) {
            this.userId = param1Int;
            this.info = param1ProviderInfo;
            this.uri = param1Uri;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TargetProviderInfo{userId=");
            stringBuilder.append(this.userId);
            stringBuilder.append(", info=");
            stringBuilder.append(this.info);
            stringBuilder.append(", uri=");
            stringBuilder.append(this.uri);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }
}
