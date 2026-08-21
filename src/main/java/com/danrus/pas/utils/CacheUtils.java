package com.danrus.pas.utils;

import com.danrus.pas.ModExecutor;
import com.danrus.pas.config.PasConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class CacheUtils {
    private CacheUtils() {}

    public static CompletableFuture<Boolean> validateCacheAsync(Path file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(file)) return false;
                long maxAge = PasConfig.millisFromSkinReloadTime(
                        PasConfig.get().skinReloadTime);
                long age = System.currentTimeMillis()
                        - Files.getLastModifiedTime(file).toMillis();
                if (age > maxAge) {
                    Files.deleteIfExists(file);
                    return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }, ModExecutor.DOWNLOAD_EXECUTOR);
    }

    public static CompletableFuture<Void> deleteAsync(Path path) {
        return CompletableFuture.runAsync(() -> {
            try { Files.deleteIfExists(path); }
            catch (Exception ignored) {}
        }, ModExecutor.DOWNLOAD_EXECUTOR);
    }
}