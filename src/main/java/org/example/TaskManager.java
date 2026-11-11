package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskManager {

    private final Path root;
    private final Pattern pattern;
    private final ThreadGroup group = new ThreadGroup("FileSearch");
    private final ThreadFactory factory;
    private final List<Thread> threads = new ArrayList<>();
    private final AtomicInteger filesProcessed = new AtomicInteger();
    private final AtomicInteger totalMatches = new AtomicInteger();

    public TaskManager(String rootDir, String regex) {
        this.root = Paths.get(rootDir);
        this.pattern = Pattern.compile(regex);
        AtomicInteger id = new AtomicInteger(1);
        this.factory = r -> new Thread(group, r, "FileSearch-" + id.getAndIncrement());
    }

    public void run() {
        System.out.println("Starting search in: " + root);
        System.out.println("Search pattern: " + pattern);

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (Thread.currentThread().isInterrupted()) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (attrs.isRegularFile()) {
                        Thread t = factory.newThread(() -> searchFile(file));
                        threads.add(t);
                        t.start();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.err.println("Cannot access " + file + ": " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Directory traversal failed: " + e.getMessage());
        }

        try {
            joinAll();
        } catch (InterruptedException e) {
            System.out.println("Interrupted! Cancelling workers...");
            for (Thread t : threads) t.interrupt();
            try {
                joinAll();
            } catch (InterruptedException e2) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Search completed!");
        System.out.println("Files processed: " + filesProcessed.get());
        System.out.println("Total matches: " + totalMatches.get());
    }

    private void joinAll() throws InterruptedException {
        for (Thread t : threads) t.join();
    }

    private void searchFile(Path file) {
        String name = Thread.currentThread().getName();
        System.out.println("[" + name + "] Searching in file: " + file.getFileName());
        int matches = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("[" + name + "] Cancelled: " + file.getFileName());
                    return;
                }
                lineNo++;
                Matcher m = pattern.matcher(line);
                int inLine = 0;
                while (m.find()) inLine++;
                if (inLine > 0) {
                    matches += inLine;
                    System.out.println("[" + name + "] Match found (" + file.getFileName()
                            + ":" + lineNo + "): " + line.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("[" + name + "] Cannot read " + file + ": " + e);
        } finally {
            filesProcessed.incrementAndGet();
            totalMatches.addAndGet(matches);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TaskManager <directory> <regex>");
            return;
        }
        new TaskManager(args[0], args[1]).run();
    }
}
