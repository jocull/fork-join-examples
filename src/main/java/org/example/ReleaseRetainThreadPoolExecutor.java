package org.example;

import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReleaseRetainThreadPoolExecutor extends ThreadPoolExecutor {
    private static final ThreadLocal<Semaphore> SEMAPHORE_THREAD_LOCAL = new ThreadLocal<>();
    private final Semaphore semaphore;

    public ReleaseRetainThreadPoolExecutor(int poolSize, int parallelism) {
        super(poolSize, poolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        semaphore = new Semaphore(parallelism);
    }

    public interface InterruptibleRunnable {
        void run() throws InterruptedException;
    }

    public interface InterruptibleSupplier<T> {
        T get() throws InterruptedException;
    }

    public static void releaseForOperation(InterruptibleRunnable runnable) throws InterruptedException {
        releaseForOperation(() -> {
            runnable.run();
            return (Void) null;
        });
    }

    public static <T> T releaseForOperation(InterruptibleSupplier<T> supplier) throws InterruptedException {
        final Semaphore semaphore = SEMAPHORE_THREAD_LOCAL.get();
        if (semaphore == null) {
            throw new IllegalStateException();
        }
        semaphore.release();
        final T result = supplier.get();
        semaphore.acquire();
        return result;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        SEMAPHORE_THREAD_LOCAL.set(semaphore);
        super.beforeExecute(t, r);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        SEMAPHORE_THREAD_LOCAL.remove(); // TODO: ...is this necessary?
        semaphore.release();
    }
}
