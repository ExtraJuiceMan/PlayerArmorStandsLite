package com.danrus.pas.utils;

import com.danrus.pas.ModExecutor;
import com.danrus.pas.PlayerArmorStandsClient;
import com.danrus.pas.config.PasConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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

    public static void saveCacheFile(Path path, byte[] data) {
        try {
            Path absolutePath = path.toAbsolutePath();
            Path parent = absolutePath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path fileName = absolutePath.getFileName();
            if (fileName == null) return;

            Path tempFile = Files.createTempFile(parent, "pas_" + fileName, ".tmp");
            tempFile.toFile().deleteOnExit();

            try {
                Files.write(tempFile, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                try {
                    Files.move(tempFile, absolutePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException moveError) {
                    Files.write(absolutePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                }
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
                throw e;
            }
        } catch (Exception e) {
            PlayerArmorStandsClient.LOGGER.warn("Failed to save file {} to cache: {}", path, e);
        }
    }
}