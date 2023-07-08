package org.example;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    final static Map<String, AtomicInteger> hardSleepCount = new HashMap<>();

    public static void main(String[] args) {
        final int poolSizeStarting = ForkJoinPool.commonPool().getPoolSize();

        final boolean useManaged = false;

        IntStream.range(0, 100)
                .mapToObj(i -> ForkJoinPool.commonPool().submit(() -> {
                    if (useManaged) {
                        try {
                            System.out.println(Thread.currentThread().getName() + " sleeping");
                            ForkJoinPool.managedBlock(new ManagedSleepBlocker());
                            System.out.println(Thread.currentThread().getName() + " awake");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        new UnmanagedSleepBlocker().runSleep();
                    }
                }))
                .collect(Collectors.toList())
                .forEach(ForkJoinTask::join);

        final int poolSizeEnding = ForkJoinPool.commonPool().getPoolSize();
        System.out.println("Done");
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
}