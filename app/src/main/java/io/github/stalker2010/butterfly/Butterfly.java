package io.github.stalker2010.butterfly;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Butterfly {
    final SparseArray<ButterflyTask> tasks = new SparseArray<>(5);
    final ConcurrentHashMap<String, CachedResult> resultsCache = new ConcurrentHashMap<>();
    volatile int tasksCursor = 0;
    static final Object tasksLock = new Object();
    volatile WeakReference<Activity> current = null;
    private static final Butterfly instance = new Butterfly();
    static final String LOG_TAG = "Butterfly";
    final BackendThreadPool backendThreadPool;

    private Butterfly() {
        backendThreadPool = new BackendThreadPool(false);
        logMem("instantiating");
    }

    public static final Butterfly get() {
        return instance;
    }

    public final Butterfly onDestroyActivity() {
        ArrayList<Integer> toRemove = null;
        boolean changed = false;
        synchronized (tasksLock) {
            Log.i(LOG_TAG, "onDestroy: " + tasks.size() + " tasks");
            for (int i = 0; i < tasks.size(); i++) {
                final ButterflyTask v = tasks.valueAt(i);
                final ButterflyTask.Options o = v.options;
                if (o.stopOnDestroy) {
                    changed = true;
                    v.markStop();
                    o.post = null;
                    o.pre = null;
                    o.error = null;
                    if (v.getThread() != null) {
                        v.getThread().interrupt();
                    }
                    if (toRemove == null) {
                        toRemove = new ArrayList<>(tasks.size());
                    }
                    toRemove.add(o.id);
                    resultsCache.remove(o.tag);
                }
            }
        }
        if (changed) {
            synchronized (tasksLock) {
                for (final Integer key : toRemove) {
                    tasks.remove(key);
                }
                Log.i(LOG_TAG, "after onDestroy: " + tasks.size());
            }
        }
        return this;
    }

    public Butterfly cleanupCache() {
        final ArrayList<String> toRemove = new ArrayList<>(resultsCache.size());
        for (final Map.Entry<String, CachedResult> e : resultsCache.entrySet()) {
            if (e.getValue().toDestroy()) {
                toRemove.add(e.getKey());
            }
        }
        for (final String s : toRemove) {
            resultsCache.remove(s);
        }
        toRemove.clear();
        return this;
    }

    public TaskBuilder create(final String tag, final Class<? extends ButterflyTask> clazz) {
        cleanupCache();
        if (tag == null) {
            throw new IllegalArgumentException("Tag is null");
        }
        TaskBuilder b;
        if (resultsCache.containsKey(tag)) {
            b = new TaskBuilder.CachedResultGetter(clazz).tag(tag);
            return b;
        } else {
            synchronized (tasksLock) {
                for (int i = 0; i < tasksCursor; i++) {
                    final ButterflyTask t = tasks.get(i, null);
                    if (t != null) {
                        final ButterflyTask.Options o = t.options;
                        if (o.tag != null) {
                            if (tag.equals(o.tag)) {
                                t.markStop();
                                o.post = null;
                                o.error = null;
                                o.pre = null;
                                o.stopOnDestroy = true;
                                if (t.getThread() != null) {
                                    t.getThread().interrupt();
                                }
                                Log.d(LOG_TAG, "Make sure, that you do not want to update existing task with the same name");
                            }
                        }
                    }
                }
            }
        }
        return new TaskBuilder.TaskInstanceCreator(clazz).tag(tag);
    }

    public TaskBuilder createOrUpdate(final String tag, final Class<? extends ButterflyTask> clazz) {
        cleanupCache();
        if (tag == null) {
            throw new IllegalArgumentException("Tag is null");
        }
        boolean canReuse = false;
        TaskBuilder b = null;
        if (resultsCache.containsKey(tag)) {
            b = new TaskBuilder.CachedResultGetter(clazz).tag(tag);
            canReuse = true;
        } else {
            synchronized (tasksLock) {
                for (int i = 0; i < tasksCursor; i++) {
                    final ButterflyTask t = tasks.get(i, null);
                    if (t != null) {
                        final ButterflyTask.Options o = t.options;
                        if (o.tag != null) {
                            if (tag.equals(o.tag)) {
                                b = new TaskBuilder.TaskInstanceUpdater(t);
                                b.pre = o.pre;
                                b.post = o.post;
                                b.error = o.error;
                                b.args(t.args)
                                        .handleErrorsOnUIThread(o.handleErrorOnUIThread)
                                        .stopOnDestroy(o.stopOnDestroy)
                                        .tag(tag);
                                canReuse = true;
                            }
                        }
                    }
                }
            }
        }
        if (!canReuse) {
            b = new TaskBuilder.TaskInstanceCreator(clazz).tag(tag);
        }
        return b;
    }

    public volatile boolean logMemory = false;
    public volatile boolean logPerformance = true;
    public volatile boolean logThreads = true;

    final void logMem(final String tag) {
        if (logMemory) {
            final Runtime r = Runtime.getRuntime();
            final long alloc = r.totalMemory() / 1024;
            final long max = r.maxMemory() / 1024;
            final String b = "MEMUSAGE[" + tag + "]: " + alloc + "/" + max + " KB";
            Log.i(LOG_TAG, b);
        }
    }

    final void logThreads() {
        if (logThreads) {
            final StringBuilder b = new StringBuilder("Thread log:");
            Thread[] threads = new Thread[100];
            Thread.enumerate(threads);
            for (final Thread t : threads) {
                if (t == null) continue;
                b.append("\n");
                b.append(t.toString());
            }
            Log.d(Butterfly.LOG_TAG, b.toString());
        }
    }

    final void logTime(final String tag, final ButterflyTask.Options o) {
        if (logPerformance) {
            Log.i(Butterfly.LOG_TAG, "Stopwatch[" + tag + "]: " + (System.currentTimeMillis() - o.startTime) + " ms");
        }
    }

    public ButterflyTask getById(final int id) {
        synchronized (tasksLock) {
            return this.tasks.get(id);
        }
    }

    public final Butterfly context(final Activity context) {
        this.current = new WeakReference<>(context);
        logMem("context set");
        return this;
    }

    final int run(final ButterflyTask task) {
        logMem("before run");
        final ButterflyTask.Options o = task.options;
        logTime("before run", o);
        int id;
        if (task.state != ButterflyTask.States.CREATED) {
            throw new IllegalStateException(task.getClass().getSimpleName() + " must call parent constructor to setup!");
        }
        id = tasksCursor;
        tasksCursor++;
        o.id = id;
        synchronized (tasksLock) {
            tasks.put(id, task);
        }
        task.isInternalCall = true;
        backendThreadPool.shedule(task);
        logMem("after run");
        logTime("after run", o);
        return id;
    }

    static final class RunCallback implements Runnable {
        private Callback cb;

        public RunCallback(final Callback task) {
            cb = task;
        }

        public final RunCallback setArgs(final Object... args) {
            cb.args = args;
            return this;
        }

        @Override
        public final void run() {
            cb.call();
            cb = null;
        }
    }


    static final boolean putInBundle(final Bundle storagePerf, final String name, final Object o) {
        if (o instanceof String) {
            storagePerf.putString(name, (String) o);
        } else if (o instanceof CharSequence) {
            storagePerf.putCharSequence(name, (CharSequence) o);
        } else if (o instanceof Integer) {
            storagePerf.putInt(name, (Integer) o);
        } else if (o instanceof Long) {
            storagePerf.putLong(name, (Long) o);
        } else if (o instanceof Float) {
            storagePerf.putFloat(name, (Float) o);
        } else if (o instanceof Double) {
            storagePerf.putDouble(name, (Double) o);
        } else if (o instanceof Short) {
            storagePerf.putShort(name, (Short) o);
        } else if (o instanceof Byte) {
            storagePerf.putByte(name, (Byte) o);
        } else if (o instanceof Boolean) {
            storagePerf.putBoolean(name, (Boolean) o);
        } else if (o instanceof Character) {
            storagePerf.putChar(name, (Character) o);
        } else {
            return false;
        }
        return true;
    }

    static final boolean isFinishing(final Activity activity) {
        if (activity.isFinishing()) return true;
        if (Build.VERSION.SDK_INT >= 17) {
            if (activity.isDestroyed()) return true;
        }
        return false;
    }
}
