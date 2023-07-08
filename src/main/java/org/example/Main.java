package org.example;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    final static Map<String, AtomicInteger> hardSleepCount = new HashMap<>();

    public static void main(String[] args) {
        final int poolSizeStarting = ForkJoinPool.commonPool().getPoolSize();

        final boolean useManaged = false;

        Optional<Double> reduce = IntStream.range(0, 1000000)
                .mapToObj(i -> {
                            final CompletableFuture<Double> future = new CompletableFuture<>();
                            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                                future.complete(ThreadLocalRandom.current().nextDouble());
                            });
                            return future;
                        })
                .collect(Collectors.toList())
                .stream()
                .map(CompletableFuture::join)
                .reduce(Double::sum);

//        Optional<Double> reduce = IntStream.range(0, 10000)
//                .mapToObj(i -> {
//                    return ForkJoinPool.commonPool().submit(() -> {
////                        final ManagedSleepingResultBlocker managedSleepingResultBlocker = new ManagedSleepingResultBlocker();
////                        try {
////                            ForkJoinPool.managedBlock(managedSleepingResultBlocker);
////                        } catch (InterruptedException e) {
////                            throw new RuntimeException(e);
////                        }
////                        return managedSleepingResultBlocker.getResult();
//
//                        final ManagedDelayingResultBlocker managedDelayingResultBlocker = new ManagedDelayingResultBlocker();
//                        try {
//                            ForkJoinPool.managedBlock(managedDelayingResultBlocker);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                        return managedDelayingResultBlocker.getResult();
//
////                        if (useManaged) {
////                            try {
////                                System.out.println(Thread.currentThread().getName() + " sleeping");
////                                ForkJoinPool.managedBlock(new ManagedSleepBlocker());
////                                System.out.println(Thread.currentThread().getName() + " awake");
////                            } catch (InterruptedException e) {
////                                throw new RuntimeException(e);
////                            }
////                        } else {
////                            new UnmanagedSleepBlocker().runSleep();
////                        }
//                    });
//                })
//                .collect(Collectors.toList())
//                .stream()
//                .map(ForkJoinTask::join)
//                .reduce(Double::sum);

        final int poolSizeEnding = ForkJoinPool.commonPool().getPoolSize();
        System.out.println("Done: " + reduce.orElse(null));
    }

    private static class UnmanagedSleepBlocker {
        boolean runSleep() {
            try {
                // Perform the blocking operation
                System.out.println(Thread.currentThread() + " literally sleeping");
                synchronized (hardSleepCount) {
                    hardSleepCount.compute(Thread.currentThread().getName(), (s, atomicInteger) -> {
                        if (atomicInteger == null) {
                            atomicInteger = new AtomicInteger(1);
                        } else {
                            atomicInteger.incrementAndGet();
                        }
                        return atomicInteger;
                    });
                }
                TimeUnit.SECONDS.sleep(5);
                System.out.println(Thread.currentThread() + " literally awaking");
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static class ManagedSleepBlocker implements ForkJoinPool.ManagedBlocker {
        final long deadline;

        public ManagedSleepBlocker() {
            deadline = Instant.now().plus(Duration.ofSeconds(5)).toEpochMilli();
        }

        @Override
        public boolean block() {
//            System.out.println(Thread.currentThread() + " checked its block");
//            return false;
            return new UnmanagedSleepBlocker().runSleep();
        }

        @Override
        public boolean isReleasable() {
            // Check the blocking condition
            // ... or any other condition that indicates the blocking operation can be released
            final boolean result = System.currentTimeMillis() >= deadline;
            System.out.println(Thread.currentThread() + " releasable = " + result);
            return result;
        }
    }

    private static class ManagedSleepingResultBlocker implements ForkJoinPool.ManagedBlocker {
        private static final ThreadLocalRandom tlr = ThreadLocalRandom.current();
        private Double result;

        public Double getResult() {
            return result;
        }

        @Override
        public boolean block() throws InterruptedException {
            TimeUnit.SECONDS.sleep(5);
            result = tlr.nextDouble();
            return false;
        }

        @Override
        public boolean isReleasable() {
            return result != null;
        }
    }

    private static class ManagedDelayingResultBlocker implements ForkJoinPool.ManagedBlocker {
        private static final ThreadLocalRandom tlr = ThreadLocalRandom.current();
        private Long deadline;
        private Double result;

        public Double getResult() {
            return result;
        }

        @Override
        public boolean block() throws InterruptedException {
            if (deadline == null) {
                deadline = Instant.now().plus(Duration.ofSeconds(5)).toEpochMilli();
                Thread.sleep(100);
                return false;
            }
            final long remainder = deadline - System.currentTimeMillis();
            if (result == null && System.currentTimeMillis() >= deadline) {
                result = tlr.nextDouble();
                return true;
            }
            Thread.sleep(Math.min(100, remainder));
            return false;
        }

        @Override
        public boolean isReleasable() {
            return result != null;
        }
    }
}