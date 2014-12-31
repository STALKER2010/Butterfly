package io.github.stalker2010.butterfly;

import android.os.Bundle;

import java.util.HashMap;

public class TaskResult {
    public String message = "";

    public TaskResult message(final String message) {
        this.message = message;
        return this;
    }

    public final Bundle storagePerf = new Bundle();
    public final HashMap<String, Object> storage = new HashMap<>();
    public boolean isFromCache = false;
    public long executionTime = 0;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_SUCCESS = 1;
    public static final int TYPE_FAILED = 2;
    public static final int TYPE_STOPPED = 3;
    public int type = TYPE_UNKNOWN;

    TaskResult() {

    }

    public TaskResult type(final int type) {
        this.type = type;
        return this;
    }

    public final Object arg(final String name, final Object fallback) {
        if (storagePerf.containsKey(name)) {
            return storagePerf.get(name);
        } else if (storage.containsKey(name)) {
            return storage.get(name);
        } else return fallback;
    }

    public final TaskResult put(final String name, final Object o) {
        if (!Butterfly.putInBundle(storagePerf, name, o)) {
            storage.put(name, o);
        }
        return this;
    }

    public boolean isSuccess() {
        return type == TYPE_SUCCESS;
    }

    public boolean isFailed() {
        return type == TYPE_FAILED;
    }

    public boolean isStopped() {
        return type == TYPE_STOPPED;
    }

    @Override
    public String toString() {
        String b = storagePerf.toString().substring("Bundle[".length());
        b = b.substring(0, b.length() - 1).trim();
        return "TaskResult[msg=" + message + "]["
                + b + "]["
                + storage.toString() + "]";
    }
}
