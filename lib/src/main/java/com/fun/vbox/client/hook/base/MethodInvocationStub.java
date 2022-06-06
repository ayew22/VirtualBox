package com.fun.vbox.client.hook.base;

import android.text.TextUtils;
import android.util.Log;

import com.fun.vbox.client.core.VCore;
import com.fun.vbox.client.hook.annotations.LogInvocation;
import com.fun.vbox.client.hook.utils.MethodParameterUtils;
import com.fun.vbox.helper.utils.VLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *         <p>
 *         HookHandler uses Java's {@link Proxy} to create a wrapper for existing services.
 *         <p>
 *         When any method is called on the wrapper, it checks if there is any {@link MethodProxy} registered
 *         and enabled for that method. If so, it calls the startUniformer instead of the wrapped implementation.
 *         <p>
 *         The whole thing is managed by a {@link MethodInvocationProxy} subclass
 */
@SuppressWarnings("unchecked")
public class MethodInvocationStub<T> {

    private static final String TAG = MethodInvocationStub.class.getSimpleName();

    private Map<String, MethodProxy> mInternalMethodProxies = new HashMap<>();
    private T mBaseInterface;
    private T mProxyInterface;
    private MethodProxy mDefaultProxy;
    private LogInvocation.Condition mInvocationLoggingCondition = LogInvocation.Condition.NEVER;


    public Map<String, MethodProxy> getAllHooks() {
        return mInternalMethodProxies;
    }


    public MethodInvocationStub(T baseInterface, Class<?>... proxyInterfaces) {
        this.mBaseInterface = baseInterface;
        if (baseInterface != null) {
            if (proxyInterfaces == null) {
                proxyInterfaces = MethodParameterUtils.getAllInterface(baseInterface.getClass());
            }
            mProxyInterface = (T) Proxy.newProxyInstance(baseInterface.getClass().getClassLoader(), proxyInterfaces, new HookInvocationHandler());
        }
    }

    public LogInvocation.Condition getInvocationLoggingCondition() {
        return mInvocationLoggingCondition;
    }

    public void setInvocationLoggingCondition(LogInvocation.Condition invocationLoggingCondition) {
        mInvocationLoggingCondition = invocationLoggingCondition;
    }

    public MethodInvocationStub(T baseInterface) {
        this(baseInterface, (Class[]) null);
    }

    /**
     * Copy all proxies from the input HookDelegate.
     *
     * @param from the HookDelegate we copy from.
     */
    public void copyMethodProxies(MethodInvocationStub from) {
        this.mInternalMethodProxies.putAll(from.getAllHooks());
    }

    /**
     * Add a method proxy.
     *
     * @param methodProxy proxy
     */
    public MethodProxy addMethodProxy(MethodProxy methodProxy) {
        if (methodProxy != null && !TextUtils.isEmpty(methodProxy.getMethodName())) {
            if (mInternalMethodProxies.containsKey(methodProxy.getMethodName())) {
                VLog.w(TAG, "The Hook(%s, %s) you added has been in existence.", methodProxy.getMethodName(),
                        methodProxy.getClass().getName());
                return methodProxy;
            }
            mInternalMethodProxies.put(methodProxy.getMethodName(), methodProxy);
        }
        return methodProxy;
    }

    public MethodProxy addMethodProxy(String methodName, MethodProxy methodProxy) {
        if (methodProxy != null && !TextUtils.isEmpty(methodName)) {
            if (mInternalMethodProxies.containsKey(methodName)) {
                VLog.w(TAG, "The Hook(%s, %s) you added has been in existence.", methodName,
                        methodProxy.getClass().getName());
                return methodProxy;
            }
            mInternalMethodProxies.put(methodName, methodProxy);
        }
        return methodProxy;
    }

    /**
     * Remove a method proxy.
     *
     * @param hookName proxy
     * @return The proxy you removed
     */
    public MethodProxy removeMethodProxy(String hookName) {
        return mInternalMethodProxies.remove(hookName);
    }

    /**
     * Remove a method proxy.
     *
     * @param methodProxy target proxy
     */
    public void removeMethodProxy(MethodProxy methodProxy) {
        if (methodProxy != null) {
            removeMethodProxy(methodProxy.getMethodName());
        }
    }

    /**
     * Remove all method proxies.
     */
    public void removeAllMethodProxies() {
        mInternalMethodProxies.clear();
    }

    /**
     * Get the startUniformer by its name.
     *
     * @param name name of the Hook
     * @param <H>  Type of the Hook
     * @return target startUniformer
     */
    @SuppressWarnings("unchecked")
    public <H extends MethodProxy> H getMethodProxy(String name) {
        H proxy = (H) mInternalMethodProxies.get(name);
        if(proxy == null){
            return (H) mDefaultProxy;
        }
        return proxy;
    }

    public  void setDefaultMethodProxy(MethodProxy proxy){
        mDefaultProxy = proxy;
    }

    /**
     * @return Proxy interface
     */
    public T getProxyInterface() {
        return mProxyInterface;
    }

    /**
     * @return Origin Interface
     */
    public T getBaseInterface() {
        return mBaseInterface;
    }

    /**
     * @return count of the hooks
     */
    public int getMethodProxiesCount() {
        return mInternalMethodProxies.size();
    }

    private class HookInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MethodProxy methodProxy = getMethodProxy(method.getName());
            boolean useProxy = VCore.get().isStartup() && methodProxy != null && methodProxy.isEnable();
            boolean mightLog = (mInvocationLoggingCondition != LogInvocation.Condition.NEVER) ||
                    (methodProxy != null && methodProxy.getInvocationLoggingCondition() != LogInvocation.Condition.NEVER);

            String argStr = null;
            Object res = null;
            Throwable exception = null;
            if (mightLog) {
                // Arguments to string is done before the method is called because the method might actually change it
                try {
                    argStr = Arrays.toString(args);
                    argStr = argStr.substring(1, argStr.length() - 1);
                } catch (Throwable e) {
                    argStr = "" + e.getMessage();
                }
            }


            try {
                if (useProxy && methodProxy.beforeCall(mBaseInterface, method, args)) {
                    res = methodProxy.call(mBaseInterface, method, args);
                    res = methodProxy.afterCall(mBaseInterface, method, args, res);
                } else {
                    res = method.invoke(mBaseInterface, args);
                }
                return res;

            } catch (Throwable t) {
                exception = t;
                if (exception instanceof InvocationTargetException && ((InvocationTargetException) exception).getTargetException() != null) {
                    exception = ((InvocationTargetException) exception).getTargetException();
                }
                try {
                    argStr = Arrays.toString(args);
                    argStr = argStr.substring(1, argStr.length() - 1);
                    VLog.e(TAG, "[Throwable] "+ method.getName() + "=>" + argStr+" %s", VLog.getStackTraceString(exception));
                } catch (Throwable e) {
                    //
                }

                if (exception instanceof SecurityException) {
                    return null;
                }
                if (method.getName().equals("startActivity")) {
                    return null;
                }
                throw exception;

            } finally {
                if (mightLog) {
                    int logPriority = mInvocationLoggingCondition.getLogLevel(useProxy, exception != null);
                    if (methodProxy != null) {
                        logPriority = Math.max(logPriority, methodProxy.getInvocationLoggingCondition().getLogLevel(useProxy, exception != null));
                    }
                    if (logPriority >= 0) {
                        String retString;
                        if (exception != null) {
                            retString = exception.toString();
                        } else if (method.getReturnType().equals(void.class)) {
                            retString = "void";
                        } else {
                            retString = String.valueOf(res);
                        }

                        Log.println(logPriority, TAG, method.getDeclaringClass().getSimpleName() + "." + method.getName() + "(" + argStr + ") => " + retString);
                    }
                }
            }
        }
    }

    private void dumpMethodProxies() {
        StringBuilder sb = new StringBuilder(50);
        sb.append("*********************");
        for (MethodProxy proxy : mInternalMethodProxies.values()) {
            sb.append(proxy.getMethodName()).append("\n");
        }
        sb.append("*********************");
        VLog.e(TAG, sb.toString());
    }

}
