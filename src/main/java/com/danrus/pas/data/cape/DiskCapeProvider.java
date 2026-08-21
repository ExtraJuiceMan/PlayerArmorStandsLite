package com.danrus.pas.data.cape;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.utils.CacheUtils;
import com.danrus.pas.utils.EncodeUtils;
import com.danrus.pas.utils.Id;
import com.danrus.pas.utils.TextureUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

import static com.danrus.pas.data.skin.DiskSkinProvider.CACHE_PATH;

public class DiskCapeProvider {
    public CapeData tryLoad(NameInfo info) {
        if (info.isEmpty() || !info.hasCape()) {
            return null;
        }

        CapeData data = new CapeData();
        data.setStatus(DownloadStatus.IN_PROGRESS);

        String fileName = getFileName(info);
        Path filePath = CACHE_PATH.resolve(fileName + ".png");

        CacheUtils.validateCacheAsync(filePath).thenAccept(valid -> {
            if (!valid) {
                Minecraft.getInstance().execute(() ->
                        PasManager.getInstance().getCapeProviderManager().download(info)
                );
                return;
            }

            Identifier texture = Id.pas("capes/" + fileName);

            TextureUtils.registerTexture(filePath, texture, false).whenComplete((id, error) -> {
                if (error != null) {
                    CacheUtils.deleteAsync(filePath).thenRun(() ->
                            Minecraft.getInstance().execute(() ->
                                    PasManager.getInstance().getCapeProviderManager().download(info)
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
        String provider = info.capeProvider();

        if (provider.equals("M")) {
            return EncodeUtils.encodeToSha256(info.base()) + "_cape";
        }

        if (provider.equals("A")) {
            return "cape_" + info.capeId();
        }

        if (provider.equals("I")) {
            return "mccapes_" + EncodeUtils.encodeToSha256(info.base());
        }

        return info.base();
    }
}