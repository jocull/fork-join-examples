package org.example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        final int poolSizeStarting = ForkJoinPool.commonPool().getPoolSize();

        final ExecutorService pool1 = Executors.newFixedThreadPool(1000);

        final CountDownLatch latch = new CountDownLatch(9000);
        final Semaphore semaphore = new Semaphore(Runtime.getRuntime().availableProcessors(), true);

        List<Future<?>> submits = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            final int lock = i;
            Future<?> submit1 = pool1.submit(() -> {
                try {
                    ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                        private boolean acquired;

                        @Override
                        public boolean block() throws InterruptedException {
                            acquired = latch.await(10, TimeUnit.MILLISECONDS);
                            return acquired;
                        }

                        @Override
                        public boolean isReleasable() {
                            return acquired;
                        }
                    });

                    ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                        private boolean acquired;

                        @Override
                        public boolean block() throws InterruptedException {
                            acquired = semaphore.tryAcquire(1, TimeUnit.SECONDS);
                            return acquired;
                        }

                        @Override
                        public boolean isReleasable() {
                            return acquired;
                        }
                    });
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
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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