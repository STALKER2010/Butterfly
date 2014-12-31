package io.github.stalker2010.butterfly;

import android.os.Bundle;

import java.util.HashMap;

public class TaskArguments {
    public final Bundle storagePerf = new Bundle();
    public final HashMap<String, Object> storage = new HashMap<>();

    public final Object arg(final String name, final Object fallback) {
        if (storagePerf.containsKey(name)) {
            return storagePerf.get(name);
        } else if (storage.containsKey(name)) {
            return storage.get(name);
        } else return fallback;
    }

    public TaskArguments() {

    }

    public TaskArguments(final TaskArguments toCopy) {
        storagePerf.putAll(toCopy.storagePerf);
        storage.putAll(toCopy.storage);
    }

    public final TaskArguments put(final String name, final Object o) {
        if (!Butterfly.putInBundle(storagePerf, name, o)) {
            storage.put(name, o);
        }
        return this;
    }

    @Override
    public String toString() {
        String b = storagePerf.toString().substring("Bundle[".length());
        b = b.substring(0, b.length() - 1).trim();
        return "TaskArguments[" + b + "]["
                + storage.toString() + "]";
    }
}
