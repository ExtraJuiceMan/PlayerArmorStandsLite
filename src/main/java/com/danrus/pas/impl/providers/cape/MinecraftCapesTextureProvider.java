package com.danrus.pas.impl.providers.cape;

import com.danrus.pas.PlayerArmorStandsClient;
import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.api.TextureProvider;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.skin.DiskSkinProvider;
import com.danrus.pas.managers.OverlayMessageManager;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.utils.EncodeUtils;
import com.danrus.pas.utils.Id;
import com.danrus.pas.utils.MojangUtils;
import com.danrus.pas.utils.TextureDownloader;
import com.google.gson.Gson;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class MinecraftCapesTextureProvider implements TextureProvider {
    private static final String API_URL = "https://api.minecraftcapes.net/profile/";
    private final Gson gson = new Gson();

    @Override public String getLiteral() { return "I"; }

    @Override
    public CompletableFuture<Void> load(NameInfo info) {
        return MojangUtils.getUUID(info)
            .thenCompose(uuid -> TextureDownloader.get(API_URL + uuid))
            .thenApply(response -> gson.fromJson(response, Profile.class))
            .thenCompose(profile -> {
                if (profile.cape_url == null || profile.cape_url.isEmpty()) {
                    CapeData d = new CapeData();
                    d.setStatus(DownloadStatus.COMPLETED);
                    PasManager.getInstance().getCapeDataManager().store(info, d);
                    return CompletableFuture.completedFuture(null);
                }
                Identifier location = Id.pas("capes/mccapes_" + EncodeUtils.encodeToSha256(info.base()));
                Path filePath = DiskSkinProvider.CACHE_PATH.resolve("mccapes_" + EncodeUtils.encodeToSha256(info.base()) + ".png");
                return TextureDownloader.downloadAndRegister(location, filePath, profile.cape_url, false)
                    .thenAccept(textureId -> {
                        CapeData d = new CapeData();
                        d.setTexture(textureId);
                        d.setStatus(DownloadStatus.COMPLETED);
                        PasManager.getInstance().getCapeDataManager().store(info, d);
                    });
            })
            .whenComplete((v, throwable) -> {
                if (throwable != null) {
                    OverlayMessageManager.getInstance().showFailMessage(info.base());
                    PlayerArmorStandsClient.LOGGER.error("MinecraftCapes failed for {}", info, throwable);
                }
            });
    }

    private static class Profile { String cape_url; }
}
