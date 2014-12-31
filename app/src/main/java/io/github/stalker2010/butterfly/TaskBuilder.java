package io.github.stalker2010.butterfly;

import android.util.Log;

public abstract class TaskBuilder {
    Callback pre = null;
    Callback post = null;
    Callback error = null;
    Callback progress = null;
    TaskArguments args = new TaskArguments();
    boolean stopOnDestroy = false;
    boolean handleErrorOnUIThread = true;
    String tag = null;
    long cacheTime = 0;
    long delay = 0;
    Class<? extends ButterflyTask> clazz = ButterflyTask.class;

    public TaskBuilder pre(final Callback c) {
        pre = c;
        return this;
    }

    public TaskBuilder post(final Callback c) {
        post = c;
        return this;
    }

    public TaskBuilder error(final Callback c) {
        error = c;
        return this;
    }

    public TaskBuilder progress(final Callback c) {
        progress = c;
        return this;
    }

    public TaskBuilder args(final TaskArguments c) {
        args = c;
        return this;
    }

    public TaskBuilder arg(final String name, final Object o) {
        args.put(name, o);
        return this;
    }

    public TaskBuilder stopOnDestroy(final boolean c) {
        stopOnDestroy = c;
        return this;
    }

    public TaskBuilder handleErrorsOnUIThread(final boolean c) {
        handleErrorOnUIThread = c;
        return this;
    }

    public TaskBuilder tag(final String c) {
        tag = c;
        return this;
    }

    public TaskBuilder cacheTime(final long c) {
        cacheTime = c;
        return this;
    }

    public TaskBuilder delay(final long c) {
        delay = c;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (o instanceof TaskBuilder) {
            final TaskBuilder b = (TaskBuilder) o;
            if ((tag == null) || (b.tag == null)) {
                return false;
            }
            return tag.equals(b.tag);
        }
        return false;
    }

    public abstract int run();

    static class TaskInstanceCreator extends TaskBuilder {
        TaskInstanceCreator(final Class<? extends ButterflyTask> clazz) {
            this.clazz = clazz;
        }

        @Override
        public int run() {
            try {
                ButterflyTask task = clazz.newInstance();
                final ButterflyTask.Options o = task.options;
                o.pre = pre;
                o.post = post;
                o.error = error;
                o.progress = progress;
                task.args = new TaskArguments(args);
                o.tag = tag;
                o.cacheTime = cacheTime;
                o.stopOnDestroy = stopOnDestroy;
                o.handleErrorOnUIThread = handleErrorOnUIThread;
                o.delay = delay;
                o.startTime = System.currentTimeMillis();
                Butterfly.get().logMem("new task");
                return Butterfly.get().run(task);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class TaskInstanceUpdater extends TaskBuilder {
        private final ButterflyTask t;

        TaskInstanceUpdater(final ButterflyTask t) {
            this.clazz = t.getClass();
            this.t = t;
        }

        @Override
        public TaskInstanceUpdater pre(final Callback c) {
            final ButterflyTask.Options o = t.options;
            if (o.pre != null) {
                o.pre.instance = c.instance;
            } else {
                o.pre = c;
            }
            return this;
        }

        @Override
        public TaskInstanceUpdater post(final Callback c) {
            final ButterflyTask.Options o = t.options;
            if (o.post != null) {
                o.post.instance = c.instance;
            } else {
                o.post = c;
            }
            return this;
        }

        @Override
        public TaskInstanceUpdater error(final Callback c) {
            final ButterflyTask.Options o = t.options;
            if (o.error != null) {
                o.error.instance = c.instance;
            } else {
                o.error = c;
            }
            return this;
        }

        @Override
        public TaskInstanceUpdater progress(final Callback c) {
            final ButterflyTask.Options o = t.options;
            if (o.progress != null) {
                o.progress.instance = c.instance;
            } else {
                o.progress = c;
            }
            return this;
        }

        @Override
        public int run() {
            return t.options.id;
        }
    }

    static class CachedResultGetter extends TaskBuilder {
        CachedResultGetter(final Class<? extends ButterflyTask> clazz) {
            this.clazz = clazz;
        }

        @Override
        public int run() {
            post.args = new Object[]{
                    Butterfly.get().resultsCache.get(tag).unpack()
            };
            post.call();
            Log.d(Butterfly.LOG_TAG, "Post event called from cache with: " + post.args[0]);
            return -1;
        }
    }
}
