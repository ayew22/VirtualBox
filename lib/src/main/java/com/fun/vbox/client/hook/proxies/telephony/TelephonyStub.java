package com.fun.vbox.client.hook.proxies.telephony;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.fun.vbox.client.VClient;
import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.MethodProxy;
import com.fun.vbox.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.fun.vbox.client.hook.base.ReplaceLastPkgMethodProxy;
import com.fun.vbox.client.hook.base.ReplaceSpecPkgMethodProxy;
import com.fun.vbox.client.hook.base.ResultStaticMethodProxy;
import com.fun.vbox.helper.compat.BuildCompat;

import java.lang.reflect.Method;

import mirror.com.android.internal.telephony.ITelephony;

/**
 * @see android.telephony.TelephonyManager
 */
public class TelephonyStub extends BinderInvocationProxy {

    public TelephonyStub() {
        super(ITelephony.Stub.asInterface, Context.TELEPHONY_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        //phone number
        addMethodProxy(new ReplaceLastPkgMethodProxy("getLine1NumberForDisplay"));
        //fake location
        addMethodProxy(new MethodProxies.GetCellLocation());
        addMethodProxy(new MethodProxies.GetAllCellInfoUsingSubId());
        addMethodProxy(new MethodProxies.GetAllCellInfo());
        addMethodProxy(new MethodProxies.GetNeighboringCellInfo());

        addMethodProxy(new MethodProxies.GetDeviceId());
        addMethodProxy(new MethodProxies.GetImeiForSlot());
        addMethodProxy(new MethodProxies.GetMeidForSlot());
        addMethodProxy(new ReplaceCallingPkgMethodProxy("call"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("isSimPinEnabled"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriIconIndex"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriIconIndexForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getCdmaEriIconMode"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriIconModeForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getCdmaEriText"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriTextForSubscriber"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getNetworkTypeForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getDataNetworkType"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getDataNetworkTypeForSubscriber"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getVoiceNetworkTypeForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getLteOnCdmaMode"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getLteOnCdmaModeForSubscriber"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCalculatedPreferredNetworkType"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getPcscfAddress"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getLine1AlphaTagForDisplay"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getMergedSubscriberIds"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getRadioAccessFamily"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("isVideoCallingEnabled"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getDeviceSoftwareVersionForSlot"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getServiceStateForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getVisualVoicemailPackageName"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("enableVisualVoicemailSmsFilter"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("disableVisualVoicemailSmsFilter"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getVisualVoicemailSmsFilterSettings"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("sendVisualVoicemailSmsForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getVoiceActivationState"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getDataActivationState"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getVoiceMailAlphaTagForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("sendDialerSpecialCode"));
        if (BuildCompat.isOreo()) {
            addMethodProxy(new ReplaceCallingPkgMethodProxy("setVoicemailVibrationEnabled"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("setVoicemailRingtoneUri"));
        }
        addMethodProxy(new ReplaceCallingPkgMethodProxy("isOffhook"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("isOffhookForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("isRinging"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("isRingingForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("isIdle"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("isIdleForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("isRadioOn"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("isRadioOnForSubscriber"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getClientRequestStats"));
        //systemApi
        if (!VCore.get().isSystemApp()) {
            addMethodProxy(new ResultStaticMethodProxy("getVisualVoicemailSettings", null));
            addMethodProxy(new ResultStaticMethodProxy("setDataEnabled", 0));
            addMethodProxy(new ResultStaticMethodProxy("getDataEnabled", false));
        }

        addMethodProxy(new ReplaceSpecPkgMethodProxy("getCallStateForSubscription", 1));
        addMethodProxy(new MethodProxy() {

            @Override
            public boolean beforeCall(Object who, Method method, Object... args) {
                args[1] = VCore.get().getHostPkg();

                return super.beforeCall(who, method, args);
            }

            @Override
            public String getMethodName() {
                return "getDataNetworkTypeForSubscriber";
            }
        });

        addMethodProxy(new MethodProxy() {
            @Override
            public boolean beforeCall(Object who, Method method, Object... args) {
                args[1] = VCore.get().getHostPkg();

                return super.beforeCall(who, method, args);
            }

            @Override
            public String getMethodName() {
                return "getVoiceNetworkTypeForSubscriber";
            }
        });


        addMethodProxy(new ReplaceCallingPkgMethodProxy("getDeviceIdWithFeature") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                try {
                    return super.call(who, method, args);
                } catch (SecurityException e) {
                    ApplicationInfo ai = VClient.get().getCurrentApplicationInfo();
                    if (ai.targetSdkVersion >= 29) {
                        throw e;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Context context = VCore.get().getContext();
                        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                                != PackageManager.PERMISSION_GRANTED) {
                            // 不排除不检查权限直接使用 try-catch 判断的情况
                            throw e;
                        }
                    }
                    return null;
                }
            }
        });

    }

}
