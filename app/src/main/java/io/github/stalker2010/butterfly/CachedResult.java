package io.github.stalker2010.butterfly;

import android.os.Bundle;

import java.util.HashMap;

final class CachedResult {
    public String message = "";
    public Bundle storagePerf = new Bundle();
    public long creationTime = -1;
    public long maxTime = 0;
    public long executionTime = 0;
    public int type;
    public HashMap<String, Object> storage = new HashMap<>();

    private CachedResult() {

    }

    public static CachedResult pack(final TaskResult res, final long maxTime) {
        final CachedResult r = new CachedResult();
        r.message = res.message;
        r.storagePerf.putAll(res.storagePerf);
        r.storage.putAll(res.storage);
        r.type = res.type;
        r.maxTime = maxTime;
        r.executionTime = res.executionTime;
        r.creationTime = System.currentTimeMillis();
        return r;
    }

    public boolean toDestroy() {
        final long end = creationTime + maxTime;
        return System.currentTimeMillis() >= end;
    }

    public TaskResult unpack() {
        final TaskResult t = new TaskResult();
        t.message = message;
        t.type = type;
        t.storagePerf.putAll(storagePerf);
        t.isFromCache = true;
        t.storage.putAll(storage);
        t.executionTime = executionTime;
        return t;
    }
}
