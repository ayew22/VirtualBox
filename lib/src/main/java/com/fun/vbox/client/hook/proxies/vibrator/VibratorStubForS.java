package com.fun.vbox.client.hook.proxies.vibrator;

import android.content.Context;

import com.fun.vbox.client.hook.base.BinderInvocationProxy;
import com.fun.vbox.client.hook.base.ReplaceUidMethodProxy;

import mirror.com.android.internal.os.IVibratorManagerService;


/**
 * @author LittleAngry
 */
public class VibratorStubForS extends BinderInvocationProxy {

    public VibratorStubForS() {
        super(IVibratorManagerService.Stub.asInterface, "vibrator_manager");
    }

    @Override
    protected void onBindMethods() {
        addMethodProxy(new ReplaceUidMethodProxy("vibrate", 0));
        addMethodProxy(new ReplaceUidMethodProxy("setAlwaysOnEffect", 0));
    }




}
