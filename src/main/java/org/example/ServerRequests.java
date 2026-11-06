package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerRequests {

    static class ClientThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger counter = new AtomicInteger(1);

        ClientThreadFactory(ThreadGroup group) { this.group = group; }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(group, r, "Client-" + counter.getAndIncrement());
        }
    }

    static class RequestTask implements Runnable {
        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            long total = ThreadLocalRandom.current().nextLong(1000, 10001);
            long elapsed = 0;
            System.out.println("[" + name + "] Request processing started.");
            try {
                while (elapsed < total) {
                    long step = Math.min(1000, total - elapsed);
                    Thread.sleep(step);
                    elapsed += step;
                    if (elapsed < total) {
                        System.out.println("[" + name + "] Processing request... "
                                + (elapsed * 100 / total) + "%");
                    }
                }
                System.out.println("[" + name + "] Request completed successfully.");
            } catch (InterruptedException e) {
                System.out.println("[" + name + "] Request cancelled.");
            }
        }
    }

    static void printGroupStatus(ThreadGroup group) {
        System.out.println("ThreadGroup: " + group.getName());
        System.out.println("Active threads: " + group.activeCount());
        Thread[] threads = new Thread[group.activeCount() + 5];
        int n = group.enumerate(threads);
        for (int i = 0; i < n; i++) {
            System.out.println("  " + threads[i].getName() + " -> " + threads[i].getState());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        boolean emergency = !(args.length > 0 && args[0].equalsIgnoreCase("graceful"));

        ThreadGroup group = new ThreadGroup("ClientRequests");
        ThreadFactory factory = new ClientThreadFactory(group);
        List<Thread> workers = new ArrayList<>();

        System.out.println("Server started. Shutdown mode: " + (emergency ? "EMERGENCY" : "GRACEFUL"));
        for (int i = 0; i < 5; i++) {
            Thread t = factory.newThread(new RequestTask());
            workers.add(t);
            t.start();
        }

        Thread.sleep(2000);
        printGroupStatus(group);
        Thread.sleep(3000);

        if (emergency) {
            System.out.println("Emergency shutdown initiated!");
            group.interrupt();
        } else {
            System.out.println("Graceful shutdown: waiting for running requests to finish...");
        }

        for (Thread t : workers) t.join();

        System.out.println("All client requests have been terminated.");
        System.out.println("Server shutdown completed.");
    }
}
