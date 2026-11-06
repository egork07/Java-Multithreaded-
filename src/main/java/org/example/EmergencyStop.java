package org.example;

public class EmergencyStop {
    public static void main(String[] args) throws InterruptedException {
        Thread monitor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("[Monitor] System is OK");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("[Monitor] Interrupted. Shutting down gracefully.");
        }, "Monitor");

        monitor.start();
        Thread.sleep(5000);
        monitor.interrupt();
        monitor.join();
        System.out.println("[Main] Monitor terminated.");
    }
}
