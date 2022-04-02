package com.fun.vbox.client.hook.base;

import java.lang.reflect.Method;

/**
 * @author Lody
 */

public class StaticMethodProxy extends MethodProxy {

	private String mName;

	public StaticMethodProxy(String name) {
		this.mName = name;
	}

	@Override
	public String getMethodName() {
		return mName;
	}
}
