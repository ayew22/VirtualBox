package com.fun.vbox.client.hook.providers;

import com.fun.vbox.client.VClient;
import com.fun.vbox.client.core.VCore;
import com.fun.vbox.os.VUserHandle;

import java.lang.reflect.Method;

import mirror.vbox.content.AttributionSource;
import mirror.vbox.content.AttributionSourceState;

/**
 * @author Lody
 */

public class InternalProviderHook extends ProviderHook {

    public InternalProviderHook(Object base) {
        super(base);
    }

    public void processArgs(Method method, Object... args) {
        if (args != null && args.length > 0 && (args[0] instanceof AttributionSource)) {
            try {
                Object attributionSourceState = mirror.vbox.content.AttributionSource.mAttributionSourceState(args[0]);
                AttributionSourceState.uid(attributionSourceState, VUserHandle.getAppId(VClient.get().getVUid()));
                AttributionSourceState.packageName(attributionSourceState, VCore.get().getHostPkg());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
