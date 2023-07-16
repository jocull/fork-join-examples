package org.example;

import java.util.concurrent.*;

public class SemaphoreThreadPoolExecutor extends ThreadPoolExecutor {
    public SemaphoreThreadPoolExecutor(int poolSize, int parallelism) {
        super(poolSize, poolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new SemaphoreThreadFactory(new Semaphore(parallelism, true)));
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        try {
            SemaphoreThread.getSemaphore(t).acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        SemaphoreThread.getSemaphore().release();
        super.afterExecute(r, t);
    }
}
