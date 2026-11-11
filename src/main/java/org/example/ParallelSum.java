package org.example;

import java.util.Random;

public class ParallelSum {

    static class SumWorker extends Thread {
        private final int[] data;
        private final int from, to;
        private long partialSum;

        SumWorker(int[] data, int from, int to) {
            this.data = data;
            this.from = from;
            this.to = to;
        }

        @Override
        public void run() {
            long s = 0;
            for (int i = from; i < to; i++)
                s += data[i];
            partialSum = s;
        }

        long getPartialSum() { return partialSum; }
    }

    static long parallelSum(int[] data, int p) throws InterruptedException {
        SumWorker[] workers = new SumWorker[p];
        int chunk = data.length / p;
        for (int i = 0; i < p; i++) {
            int from = i * chunk;
            int to = (i == p - 1) ? data.length : from + chunk;
            workers[i] = new SumWorker(data, from, to);
            workers[i].start();
        }
        long total = 0;
        for (SumWorker w : workers) {
            w.join();
            total += w.getPartialSum();
        }
        return total;
    }

    static long singleSum(int[] data) {
        long s = 0;
        for (int x : data) s += x;
        return s;
    }

    public static void main(String[] args) throws InterruptedException {
        int[] data = new int[20_000_000];
        Random rnd = new Random(42);
        for (int i = 0; i < data.length; i++) data[i] = rnd.nextInt(100);

        for (int attempt = 1; attempt <= 3; attempt++) {
            long t0 = System.nanoTime();
            long single = singleSum(data);
            long t1 = System.nanoTime();
            long parallel = parallelSum(data, 4);
            long t2 = System.nanoTime();

            System.out.println("Run " + attempt + ": single=" + single + " (" + (t1 - t0) / 1_000_000 + " ms), "
                    + "parallel=" + parallel + " (" + (t2 - t1) / 1_000_000 + " ms), equal=" + (single == parallel));
        }
    }
}
