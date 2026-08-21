package com.danrus.pas.impl.providers.common;

import com.danrus.pas.PlayerArmorStandsClient;
import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.api.TextureProvider;
import com.danrus.pas.data.Texture;
import com.danrus.pas.data.skin.DiskSkinProvider;
import com.danrus.pas.managers.OverlayMessageManager;
import com.danrus.pas.utils.TextureDownloader;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract class AbstractNamemcProvider<T extends Texture> implements TextureProvider {
    @Override public String getLiteral() { return "N"; }

    protected void onDownloadComplete(NameInfo info, T data, Path filePath) {}

    @Override
    public CompletableFuture<Void> load(NameInfo info) {
        OverlayMessageManager.getInstance().showDownloadMessage(info.base());
        return getDownloadTask(info)
            .thenAccept(identifier -> {
                T data = createDataHolder();
                data.setTexture(identifier);
                data.setStatus(DownloadStatus.COMPLETED);
                String fileName = getFileName(info);
                Path filePath = DiskSkinProvider.CACHE_PATH.resolve(fileName + ".png");
                onDownloadComplete(info, data, filePath);
                store(info, data);
                OverlayMessageManager.getInstance().showSuccessMessage(info.base());
            })
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        Throwable cause = throwable;
                        while (cause instanceof CompletionException && cause.getCause() != null) {
                            cause = cause.getCause();
                        }
                        OverlayMessageManager.getInstance().showFailMessage(info.base());
                        PlayerArmorStandsClient.LOGGER.warn("NameMC Provider failed for {}: {}", info, cause.getMessage());
                    }
                });
    }

    private CompletableFuture<Identifier> getDownloadTask(NameInfo info) {
        Identifier location = getLocation(info);
        String fileName = getFileName(info);
        Path filePath = DiskSkinProvider.CACHE_PATH.resolve(fileName + ".png");
        return TextureDownloader.downloadAndRegister(location, filePath,
            "https://s.namemc.com/i/" + getNamemcId(info) + ".png", shouldRemap());
    }

    protected abstract Identifier getLocation(NameInfo info);
    protected abstract String getFileName(NameInfo info);
    protected abstract String getNamemcId(NameInfo info);
    protected abstract boolean shouldRemap();
    protected abstract T createDataHolder();
    protected abstract void store(NameInfo info, T data);
}
