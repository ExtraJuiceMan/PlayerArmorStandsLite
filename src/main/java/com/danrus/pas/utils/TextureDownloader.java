package com.danrus.pas.utils;

import com.danrus.pas.ModExecutor;
import com.danrus.pas.PlayerArmorStandsClient;
import com.danrus.pas.data.skin.DiskSkinProvider;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class TextureDownloader {
    public static CompletableFuture<Identifier> downloadAndRegister(Identifier id, Path path, String uri, boolean remap) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return download(path, uri);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, ModExecutor.DOWNLOAD_EXECUTOR).thenCompose(image -> TextureUtils.registerTexture(image, id, remap));
    }

    private static NativeImage download(Path path, String uri) throws IOException {
        int maxRetries = 3;
        IOException lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) URI.create(uri).toURL()
                    .openConnection(Minecraft.getInstance().getProxy());
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 Minecraft Client");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    byte[] data = connection.getInputStream().readAllBytes();
                    try {
                        Files.createDirectories(path.getParent());
                        Files.write(path, data);
                    } catch (IOException e) {
                        PlayerArmorStandsClient.LOGGER.error("Failed to save file {}", path, e);
                    }
                    return NativeImage.read(new ByteArrayInputStream(data));
                }
                lastException = new IOException("HTTP " + responseCode + " for " + uri);
            } catch (IOException e) {
                lastException = e;
            } finally {
                if (connection != null) connection.disconnect();
            }
            try { Thread.sleep(1000); }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Download interrupted", lastException);
            }
        }
        throw new IOException("Failed to download after " + maxRetries + " attempts: " + uri, lastException);
    }
}
