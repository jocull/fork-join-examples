package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        final int poolSizeStarting = ForkJoinPool.commonPool().getPoolSize();

        final ExecutorService pool1 = Executors.newWorkStealingPool();
        final ExecutorService pool2 = Executors.newWorkStealingPool();

        final CountDownLatch latch = new CountDownLatch(9000);
        Future<?> submit = pool1.submit(() -> {
            List<Future<?>> submits = new ArrayList<>(10000);
            for (int i = 0; i < 10000; i++) {
                final int lock = i;
                Future<?> submit1 = pool2.submit(() -> {
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

                        for (long x = 0; x < 500_000_000; x++) {
                            // so busy
                        }
                        System.out.println("Done #" + lock + " @ " + Thread.currentThread());
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
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        try {
            submit.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        final int poolSizeEnding = ForkJoinPool.commonPool().getPoolSize();
        System.out.println("Pool starting size: " + poolSizeStarting);
        System.out.println("Pool ending size:   " + poolSizeEnding);
    }
}