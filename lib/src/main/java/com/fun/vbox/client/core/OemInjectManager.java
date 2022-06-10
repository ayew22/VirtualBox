package com.fun.vbox.client.core;

import com.fun.vbox.client.hook.proxies.vivo.PhysicalFlingManagerStub;
import com.fun.vbox.client.hook.proxies.vivo.PopupCameraManagerStub;
import com.fun.vbox.client.hook.proxies.vivo.SuperResolutionManagerStub;
import com.fun.vbox.client.hook.proxies.vivo.SystemDefenceManagerStub;
import com.fun.vbox.client.hook.proxies.vivo.VivoPermissionServiceStub;
import com.fun.vbox.client.interfaces.IInjector;

import mirror.oem.vivo.IPhysicalFlingManagerStub;
import mirror.oem.vivo.IPopupCameraManager;
import mirror.oem.vivo.ISuperResolutionManager;
import mirror.oem.vivo.ISystemDefenceManager;
import mirror.oem.vivo.IVivoPermissonService;

public class OemInjectManager {

    private static void injectVivo(InvocationStubManager paramInvocationStubManager) {
        if (IPhysicalFlingManagerStub.TYPE != null)
            paramInvocationStubManager.addInjector((IInjector) new PhysicalFlingManagerStub());
        if (IPopupCameraManager.TYPE != null)
            paramInvocationStubManager.addInjector((IInjector) new PopupCameraManagerStub());
        if (ISuperResolutionManager.TYPE != null)
            paramInvocationStubManager.addInjector((IInjector) new SuperResolutionManagerStub());
        if (ISystemDefenceManager.TYPE != null)
            paramInvocationStubManager.addInjector((IInjector) new SystemDefenceManagerStub());
        if (IVivoPermissonService.TYPE != null)
            paramInvocationStubManager.addInjector((IInjector) new VivoPermissionServiceStub());
    }

    public static void oemInject(InvocationStubManager paramInvocationStubManager) {
        injectVivo(paramInvocationStubManager);
    }

}
