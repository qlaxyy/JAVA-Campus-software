package edu.seu.vcampus.server.infrastructure;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates predictably named platform threads for diagnostics.
 */
final class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger sequence = new AtomicInteger(1);

    NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable task) {
        return new Thread(task, prefix + sequence.getAndIncrement());
    }
}
