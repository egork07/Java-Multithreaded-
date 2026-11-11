package org.example;

public class DownloadCancellation {

    static final int TOTAL_CHUNKS = 20;

    static class FakeConnection implements AutoCloseable {
        FakeConnection() { System.out.println("[Resource] Connection opened"); }
        void downloadChunk(int n) { }
        @Override
        public void close() { System.out.println("[Resource] Connection closed"); }
    }

    // Версия 1 sleep + InterruptedException
    static class SleepDownloader implements Runnable {
        @Override
        public void run() {
            int done = 0;
            try (FakeConnection conn = new FakeConnection()) {
                for (int i = 1; i <= TOTAL_CHUNKS; i++) {
                    Thread.sleep(500);
                    conn.downloadChunk(i);
                    done++;
                    System.out.println("[Sleep] Chunk " + i + "/" + TOTAL_CHUNKS);
                }
                System.out.println("[Sleep] Download complete!");
            } catch (InterruptedException e) {
                System.out.println("[Sleep] Cancelled. Chunks completed: " + done);
                Thread.currentThread().interrupt();
            }
        }
    }

    // Версия 2 периодическая проверка isInterrupted
    static class PollingDownloader implements Runnable {
        @Override
        public void run() {
            int done = 0;
            try (FakeConnection conn = new FakeConnection()) {
                for (int i = 1; i <= TOTAL_CHUNKS; i++) {
                    if (!simulateWork(500)) {
                        System.out.println("[Poll] Cancelled. Chunks completed: " + done);
                        return;
                    }
                    conn.downloadChunk(i);
                    done++;
                    System.out.println("[Poll] Chunk " + i + "/" + TOTAL_CHUNKS);
                }
                System.out.println("[Poll] Download complete!");
            }
        }

        private boolean simulateWork(long ms) {
            long end = System.nanoTime() + ms * 1_000_000;
            while (System.nanoTime() < end) {
                if (Thread.currentThread().isInterrupted()) return false;
                Thread.onSpinWait();
            }
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        runScenario(new SleepDownloader(), "Downloader-Sleep");
        runScenario(new PollingDownloader(), "Downloader-Poll");
    }

    static void runScenario(Runnable task, String name) throws InterruptedException {
        Thread worker = new Thread(task, name);
        worker.start();
        Thread.sleep(3200);
        System.out.println("[Main] Cancel requested");
        worker.interrupt();
        worker.join();
        System.out.println();
    }
}
