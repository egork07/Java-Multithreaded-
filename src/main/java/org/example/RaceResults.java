package org.example;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RaceResults {

    static class Athlete extends Thread {
        private final int legs;
        private final long raceStart;
        private long finishTimeMs;

        Athlete(String name, int legs, long raceStart) {
            super(name);
            this.legs = legs;
            this.raceStart = raceStart;
        }

        long getFinishTimeMs() { return finishTimeMs; }

        @Override
        public void run() {
            System.out.println(getName() + " starts the race (" + legs + " legs)");
            try {
                for (int leg = 1; leg <= legs; leg++) {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(200, 601));
                    System.out.println(getName() + " passed leg " + leg + "/" + legs);
                }
            } catch (InterruptedException e) {
                System.out.println(getName() + " was stopped");
                return;
            }
            finishTimeMs = System.currentTimeMillis() - raceStart;
            System.out.println(getName() + " FINISHED in " + finishTimeMs + " ms");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        List<Athlete> athletes = List.of(
                new Athlete("Alice", 3, start),
                new Athlete("Bob", 5, start),
                new Athlete("Carol", 7, start));

        for (Athlete a : athletes) a.start();
        for (Athlete a : athletes) a.join();

        System.out.println("=== FINAL RESULTS ===");
        List<Athlete> sorted = new ArrayList<>(athletes);
        sorted.sort(Comparator.comparingLong(Athlete::getFinishTimeMs));
        int place = 1;
        for (Athlete a : sorted) {
            System.out.println(place++ + ". " + a.getName() + " - " + a.getFinishTimeMs() + " ms");
        }
    }
}
