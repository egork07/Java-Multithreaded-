package org.example;

public class ParallelCounters {

    static class Counter extends Thread {
        private final int from, to, delayMs;

        Counter(String name, int from, int to, int delayMs) {
            super(name);
            this.from = from;
            this.to = to;
            this.delayMs = delayMs;
        }

        @Override
        public void run() {
            int step = from <= to ? 1 : -1;
            for (int i = from; i != to + step; i += step) {
                System.out.println(getName() + ": " + i);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int n = 10, m = 300;
        Thread a = new Counter("Thread-A", 1, n, m);
        Thread b = new Counter("Thread-B", n, 1, m);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Done");
    }
}

