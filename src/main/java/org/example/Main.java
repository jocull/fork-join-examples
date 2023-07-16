package org.example;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== A TESTS ==========");
        for (int i = 0; i < 10; i++) {
            a();
        }

        System.out.println("========== B TESTS ==========");
        for (int i = 0; i < 10; i++) {
            b();
        }
    }

    private static void a() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(1000);
        final CountDownLatch latch = new CountDownLatch(9000);
        final Instant start = Instant.now();

        final AtomicInteger workCounter = new AtomicInteger();
        IntStream.range(0, 10_000)
                .mapToObj(i -> {
                    final int lock = i;
                    workCounter.incrementAndGet();
                    final Future<?> submit = executor.submit(() -> {
                        try {
                            // SemaphoreThread.releaseForOperation(() -> latch.await());
                            latch.await();

                            // System.out.println(Instant.now() + " Busy #" + lock + " @ " + Thread.currentThread());
                            for (long x = 0; x < 10_000_000L; x++) {
                                // so busy
                            }
                            final int remaining = workCounter.decrementAndGet();
                            // System.out.println(Instant.now() + " Done #" + lock + " @ " + Thread.currentThread() + " : " + remaining + " left");
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    latch.countDown();
                    return submit;
                })
                .collect(Collectors.toList())
                .forEach(j -> {
                    try {
                        j.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                });

        final Instant end = Instant.now();
        final Duration duration = Duration.between(start, end);
        System.out.println("Took " + duration);

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    private static void b() throws InterruptedException {
        final ExecutorService executor = new SemaphoreThreadPoolExecutor(1000, Runtime.getRuntime().availableProcessors());
        final CountDownLatch latch = new CountDownLatch(9000);
        final Instant start = Instant.now();

        final AtomicInteger workCounter = new AtomicInteger();
        IntStream.range(0, 10_000)
                .mapToObj(i -> {
                    final int lock = i;
                    workCounter.incrementAndGet();
                    final Future<?> submit = executor.submit(() -> {
                        try {
                            SemaphoreThread.releaseForOperation(() -> latch.await());

                            // System.out.println(Instant.now() + " Busy #" + lock + " @ " + Thread.currentThread());
                            for (long x = 0; x < 10_000_000L; x++) {
                                // so busy
                            }
                            final int remaining = workCounter.decrementAndGet();
                            // System.out.println(Instant.now() + " Done #" + lock + " @ " + Thread.currentThread() + " : " + remaining + " left");
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    latch.countDown();
                    return submit;
                })
                .collect(Collectors.toList())
                .forEach(j -> {
                    try {
                        j.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                });

        final Instant end = Instant.now();
        final Duration duration = Duration.between(start, end);
        System.out.println("Took " + duration);

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
}