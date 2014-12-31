package io.github.stalker2010.butterfly;

/**
 * @author STALKER_2010
 */
public abstract class BackendImplementation {
    public abstract int backendPriority(final ButterflyTask task);

    public abstract void shedule(final ButterflyTask task);
}
