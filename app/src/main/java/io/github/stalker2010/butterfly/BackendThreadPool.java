package io.github.stalker2010.butterfly;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author STALKER_2010
 */
public class BackendThreadPool extends BackendImplementation {
    final int cores = Runtime.getRuntime().availableProcessors();
    final int optimal = 5;
    private final ScheduledThreadPoolExecutor executor;

    public BackendThreadPool(boolean poolSizeDependsOnCoreCount) {
        if (poolSizeDependsOnCoreCount) {
            executor = new ScheduledThreadPoolExecutor(cores);
        } else {
            executor = new ScheduledThreadPoolExecutor(optimal);
        }
    }

    @Override
    public int backendPriority(ButterflyTask task) {
        System.out.println(executor);
        if (executor.getActiveCount() >= executor.getPoolSize()) {
            return 5;
        } else {
            return 100;
        }
    }

    @Override
    public void shedule(ButterflyTask task) {
        final long delay = task.options.delay;
        if (delay == -1L) {
            executor.execute(task);
        } else {
            executor.schedule(task, delay, TimeUnit.MILLISECONDS);
        }
    }
}
