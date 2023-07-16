package org.example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        final int poolSizeStarting = ForkJoinPool.commonPool().getPoolSize();

        final ReleaseRetainThreadPoolExecutor pool1 = new ReleaseRetainThreadPoolExecutor(1000, Runtime.getRuntime().availableProcessors());

        final CountDownLatch latch = new CountDownLatch(9000);
        final Semaphore semaphore = new Semaphore(Runtime.getRuntime().availableProcessors(), true);

        List<Future<?>> submits = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            final int lock = i;
            Future<?> submit1 = pool1.submit(() -> {
                try {
                    ReleaseRetainThreadPoolExecutor.releaseForOperation(() -> latch.await());
                    try {
                        System.out.println(Instant.now() + " Busy #" + lock + " @ " + Thread.currentThread());
                        for (long x = 0; x < 5_000_000_000L; x++) {
                            // so busy
                        }
                        System.out.println(Instant.now() + " Done #" + lock + " @ " + Thread.currentThread());
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            latch.countDown();
            submits.add(submit1);
        }
        submits.forEach(j -> {
            try {
                j.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}