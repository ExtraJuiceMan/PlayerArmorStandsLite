package com.danrus.pas;

import com.danrus.pas.config.PasConfig;
import java.util.List;
import java.util.concurrent.*;

public class ModExecutor {
    public static volatile ExecutorService MAIN_EXECUTOR;
    public static volatile ExecutorService DOWNLOAD_EXECUTOR;
    private static final Object LOCK = new Object();

    public static void init() {
        synchronized (LOCK) {
            int threads = Math.max(1, PasConfig.get().downloadThreads);
            MAIN_EXECUTOR     = Executors.newFixedThreadPool(threads);
            DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(threads);
        }
    }

    public static void reload() {
        synchronized (LOCK) {
            int threads = Math.max(1, PasConfig.get().downloadThreads);

            List<Runnable> mainTasks = MAIN_EXECUTOR.shutdownNow();
            MAIN_EXECUTOR = Executors.newFixedThreadPool(threads);
            mainTasks.forEach(MAIN_EXECUTOR::submit);

            List<Runnable> dlTasks = DOWNLOAD_EXECUTOR.shutdownNow();
            DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(threads);
            dlTasks.forEach(DOWNLOAD_EXECUTOR::submit);
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            if (MAIN_EXECUTOR != null) MAIN_EXECUTOR.shutdown();
            if (DOWNLOAD_EXECUTOR != null) DOWNLOAD_EXECUTOR.shutdown();
        }
    }

    public static CompletableFuture<Void> execute(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, MAIN_EXECUTOR);
    }
}