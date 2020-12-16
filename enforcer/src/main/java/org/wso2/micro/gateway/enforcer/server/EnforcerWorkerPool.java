package org.wso2.micro.gateway.enforcer.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class that holds the  thread pool that serves the external auth requests coming from the router component
 */
public class EnforcerWorkerPool {
    private final BlockingQueue<Runnable> blockingQueue;
    private final ThreadPoolExecutor executor;
    private static final Logger log = LogManager.getLogger(EnforcerWorkerPool.class);

    public EnforcerWorkerPool(int core, int max, int keepAlive, int queueLength, String threadGroupName,
            String threadGroupId) {
        if (log.isDebugEnabled()) {
            log.debug("Using native util.concurrent package..");
        }

        this.blockingQueue = queueLength == -1 ? new LinkedBlockingQueue() : new LinkedBlockingQueue(queueLength);
        this.executor = new EnforcerThreadPoolExecutor(core, max, (long) keepAlive, TimeUnit.SECONDS,
                this.blockingQueue, new NativeThreadFactory(new ThreadGroup(threadGroupName), threadGroupId));
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }
}
