package io.github.stalker2010.butterfly;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class Callback {
    Method method = null;
    private boolean isStatic = false;
    private int paramsCount = 0;
    WeakReference<Object> instance = null;
    Object[] args = new Object[0];

    public Callback(Object instance, String methodName, final Object... args) {
        final Class cl = instance.getClass();
        for (final Method m : cl.getMethods()) {
            if (m.getName().equals(methodName)) {
                final int mods = m.getModifiers();
                if (!Modifier.isPrivate(mods)) {
                    method = m;
                    isStatic = Modifier.isStatic(mods);
                    paramsCount = m.getParameterTypes().length;
                    break;
                } else {
                    Log.w(Butterfly.LOG_TAG, "Found callback " + methodName + ", but with private modifier");
                }
            }
        }
        if (method == null) {
            throw new NullPointerException("Callback " + methodName + " not found!");
        }
        this.instance = new WeakReference<>(instance);
        this.args = args;
    }

    public boolean isCallable() {
        if (method == null) return false;
        if (!isStatic) {
            if (instance == null) return false;
            if (instance.get() == null) return false;
        }
        if (paramsCount != args.length) return false;
        return true;
    }

    public void call() {
        if (!isCallable()) {
            Log.e(Butterfly.LOG_TAG, "Callback is not callable");
        } else {
            try {
                Object i = null;
                if (instance != null) {
                    i = instance.get();
                }
                method.invoke(i, args);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
