package com.danrus.pas.data.skin;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public class DiskSkinProvider {
    public static final Path CACHE_PATH = ModUtils.getGameDir().resolve("cache/pas");

    public SkinData tryLoad(NameInfo info) {
        if (info.isEmpty() || info.skinProvider().equals("F")) {
            return null;
        }

        SkinData data = new SkinData();
        data.setStatus(DownloadStatus.IN_PROGRESS);

        String fileName = getFileName(info);
        Path filePath = CACHE_PATH.resolve(fileName + ".png");

        CacheUtils.validateCacheAsync(filePath).thenAccept(valid -> {
            if (!valid) {
                Minecraft.getInstance().execute(() ->
                        PasManager.getInstance().getSkinProviderManager().download(info)
                );
                return;
            }

            Identifier texture = Id.pas("skins/" + fileName);

            TextureUtils.registerTexture(filePath, texture, true).whenComplete((id, error) -> {
                if (error != null) {
                    CacheUtils.deleteAsync(filePath).thenRun(() ->
                            Minecraft.getInstance().execute(() ->
                                    PasManager.getInstance().getSkinProviderManager().download(info)
                            )
                    );
                } else {
                    data.setTexture(id);
                    data.setStatus(DownloadStatus.COMPLETED);
                }
            });
        }).exceptionally(error -> {
            data.setStatus(DownloadStatus.FAILED);
            return null;
        });

        return data;
    }

    private static String getFileName(NameInfo info) {
        String provider = info.skinProvider();

        if (provider.equals("M")) {
            return EncodeUtils.encodeToSha256(info.base());
        }

        if (provider.equals("N")) {
            return EncodeUtils.encodeToSha256(info.base()) + "_namemc";
        }

        return info.base();
    }
}