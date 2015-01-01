package io.github.stalker2010.butterfly;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author STALKER_2010
 */
public class BackendThreadPool {
    final int cores = Runtime.getRuntime().availableProcessors();
    final int optimal = 5;
    private final ScheduledThreadPoolExecutor sheduledExecutor;
    private final ExecutorService executor;

    public BackendThreadPool() {
        sheduledExecutor = new ScheduledThreadPoolExecutor(Math.max(cores, optimal));
        executor = Executors.newCachedThreadPool();
    }

    public void shedule(ButterflyTask task) {
        final long delay = task.options.delay;
        if (delay == -1L) {
            executor.execute(task);
        } else {
            sheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        sheduledExecutor.shutdownNow();
        executor.shutdownNow();
    }
}
