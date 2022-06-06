package com.fun.vbox.client.hook.providers;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.utils.MethodParameterUtils;
import com.fun.vbox.helper.compat.BuildCompat;

import java.lang.reflect.Method;

import mirror.vbox.content.AttributionSource;
import mirror.vbox.content.AttributionSourceState;

/**
 * @author Lody
 */

public class ExternalProviderHook extends ProviderHook {

    public ExternalProviderHook(Object base) {
        super(base);
    }

    @Override
    protected void processArgs(Method method, Object... args) {
        if (args != null && args.length > 0)
            if (args[0] instanceof String) {
                String pkg = (String)args[0];
                if (VCore.get().isAppInstalled(pkg))
                    args[0] = VCore.get().getHostPkg();
            } else {
                try {
                    if (BuildCompat.isS()) {
                        int i = MethodParameterUtils.getIndex(args, AttributionSource.class);
                        if (i < 0)
                            return;
                        Object attrSourceState = AttributionSource.mAttributionSourceState.get((args[i]));
                        if (attrSourceState != null) {
                            //android.content.AttributionSourceState
                            AttributionSourceState.packageName.set(attrSourceState, VCore.get().getHostPkg());
                            AttributionSourceState.uid.set(attrSourceState, VCore.get().myUid());
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
    }
}
