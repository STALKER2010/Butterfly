package io.github.stalker2010.butterfly;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;

public abstract class ButterflyTask implements Runnable {
    public static final class States {
        public static final int CREATED = 0;
        public static final int PRE_EXECUTING = 1;
        public static final int EXECUTING = 2;
        public static final int POST_EXECUTING = 3;
        public static final int FINISHED = 4;
        public static final int UNSTABLE = 5;
        public static final int UNKNOWN = -1;

        private States() {
        }

        public static final String toString(int state) {
            switch (state) {
                case CREATED:
                    return "CREATED";
                case PRE_EXECUTING:
                    return "PRE EXECUTING";
                case EXECUTING:
                    return "EXECUTING";
                case POST_EXECUTING:
                    return "POST EXECUTING";
                case FINISHED:
                    return "FINISHED";
                case UNSTABLE:
                    return "UNSTABLE";
                case UNKNOWN:
                default:
                    return "UNKNOWN";
            }
        }
    }

    final class Options {
        Callback pre = null;
        Callback post = null;
        Callback error = null;
        Callback progress = null;
        boolean stopFlag = false;
        boolean fastStopFlag = false;
        boolean stopOnDestroy = false;
        boolean handleErrorOnUIThread = true;
        String tag = null;
        long startTime = -1L;
        long cacheTime = 0L;
        long delay = -1L;
        int id = -1;

        private Options() {
        }
    }

    Thread thread = null;
    private final Object threadLock = new Object();

    Thread getThread() {
        synchronized (threadLock) {
            return thread;
        }
    }

    public int getId() {
        return options.id;
    }

    public int state = States.UNKNOWN;
    public TaskArguments args = new TaskArguments();
    final Options options = new Options();

    public ButterflyTask() {
        state = States.CREATED;
    }

    protected final TaskResult success() {
        return new TaskResult().type(TaskResult.TYPE_SUCCESS);
    }

    protected final TaskResult fail() {
        return new TaskResult().type(TaskResult.TYPE_FAILED);
    }

    protected final TaskResult stop() {
        return new TaskResult().type(TaskResult.TYPE_STOPPED);
    }

    protected final void progress(final Bundle b) {
        if (options.progress != null) {
            if (Butterfly.get().current != null) {
                if (Butterfly.get().current.get() != null) {
                    final Activity context = Butterfly.get().current.get();
                    if (!(Butterfly.isFinishing(context))) {
                        context.runOnUiThread(new Butterfly.RunCallback(options.progress).setArgs(b));
                    } else {
                        Log.d(Butterfly.LOG_TAG, "Cant invoke progress callback: activity is finishing");
                    }
                } else {
                    Log.d(Butterfly.LOG_TAG, "Cant invoke progress callback: activity removed by GC");
                }
            } else {
                Log.d(Butterfly.LOG_TAG, "Cant invoke progress callback: context not set");
            }
        } else {
            Log.w(Butterfly.LOG_TAG, "Requested progress update, but there is no progress callback");
        }
    }

    public final ButterflyTask markStop() {
        options.stopFlag = true;
        return this;
    }

    public final boolean toStop() {
        return options.stopFlag;
    }

    public abstract TaskResult doInBackground();

    volatile boolean isInternalCall = false;

    @Override
    public void run() {
        if (!isInternalCall) {
            throw new IllegalAccessError("You mustn't call method run of ButterflyTask");
        }
        isInternalCall = false;
        {
            Butterfly.get().logMem("starting");
            Butterfly.get().logTime("started", options);
            final Options options = this.options;
            try {
                state = States.PRE_EXECUTING;
                if (!toStop()) {
                    if (options.pre != null) {
                        final WeakReference<Activity> ar = Butterfly.get().current;
                        if (ar != null) {
                            final Activity context = ar.get();
                            if (context != null) {
                                if (!(Butterfly.isFinishing(context))) {
                                    context.runOnUiThread(new Butterfly.RunCallback(options.pre));
                                } else {
                                    Log.d(Butterfly.LOG_TAG, "Cant invoke pre callback: activity is finishing");
                                }
                            } else {
                                Log.d(Butterfly.LOG_TAG, "Cant invoke pre callback: activity removed by GC");
                            }
                        } else {
                            Log.d(Butterfly.LOG_TAG, "Cant invoke pre callback: context not set");
                        }
                    }
                    state = States.EXECUTING;
                    if (!toStop()) {
                        Butterfly.get().logMem("doing");
                        Butterfly.get().logTime("main", options);
                        final TaskResult res = doInBackground();
                        if (res.isStopped()) {
                            Log.d(Butterfly.LOG_TAG, "Got stopped result");
                        } else {
                            if (options.cacheTime > 0) {
                                Butterfly.get().resultsCache.put(options.tag, CachedResult.pack(res, options.cacheTime));
                            }
                            state = States.POST_EXECUTING;
                            Butterfly.get().logMem("post");
                            Butterfly.get().logTime("post", options);
                            if (options.post != null) {
                                final WeakReference<Activity> ar = Butterfly.get().current;
                                if (ar != null) {
                                    final Activity context = ar.get();
                                    if (context != null) {
                                        if (!(Butterfly.isFinishing(context))) {
                                            res.executionTime = System.currentTimeMillis() - options.startTime;
                                            context.runOnUiThread(new Butterfly.RunCallback(options.post).setArgs(res));
                                        } else {
                                            Log.d(Butterfly.LOG_TAG, "Cant invoke post callback: activity is finishing");
                                        }
                                    } else {
                                        Log.d(Butterfly.LOG_TAG, "Cant invoke post callback: activity removed by GC");
                                    }
                                } else {
                                    Log.d(Butterfly.LOG_TAG, "Cant invoke post callback: context not set");
                                }
                            }
                        }
                    }
                } else {
                    Log.w(Butterfly.LOG_TAG, "Stopped task before calling even pre callback");
                }
                state = States.FINISHED;
            } catch (final Throwable e) {
                Log.e(Butterfly.LOG_TAG, "Exception occured during state " + States.toString(state), e);
                state = States.UNSTABLE;
                if (options.error != null) {
                    boolean tryUI = false;
                    if (options.handleErrorOnUIThread) {
                        final WeakReference<Activity> context = Butterfly.get().current;
                        if (context != null) {
                            if (context.get() != null) {
                                tryUI = true;
                            }
                        }
                        if (tryUI) {
                            context.get().runOnUiThread(new Butterfly.RunCallback(options.error).setArgs(e));
                        }
                    }
                    if (!tryUI) {
                        options.error.args = new Object[]{e};
                        options.error.call();
                    }
                }
            }
        }
        synchronized (Butterfly.tasksLock) {
            Butterfly.get().tasks.remove(options.id);
        }
        Butterfly.get().logMem("finished");
        Butterfly.get().logTime("finish", options);
    }
}
