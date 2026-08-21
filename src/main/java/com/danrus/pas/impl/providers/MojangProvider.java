package com.danrus.pas.impl.providers;

import com.danrus.pas.ModExecutor;
import com.danrus.pas.PlayerArmorStandsClient;
import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.api.TextureProvider;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.skin.DiskSkinProvider;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.managers.OverlayMessageManager;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.utils.*;
import com.google.gson.Gson;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public class MojangProvider implements TextureProvider {
    private static final MojangProvider INSTANCE = new MojangProvider();
    private static final String SESSION_SERVER_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private final Map<String, CompletableFuture<Void>> activeDownloads = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    private MojangProvider() {}
    public static MojangProvider getInstance() { return INSTANCE; }

    @Override public String getLiteral() { return "M"; }

    @Override
    public CompletableFuture<Void> load(NameInfo info) {
        String key = "MOJANG|" + info.base();
        CompletableFuture<Void> created = new CompletableFuture<>();
        if (activeDownloads.putIfAbsent(key, created) != null) return activeDownloads.get(key);

        if (!MojangUtils.isNicknameValid(info.base())) {
            OverlayMessageManager.getInstance().showInvalidNameMessage(info.base());

            if (info.skinProvider().equals("M")) {
                SkinData d = new SkinData();
                d.setStatus(DownloadStatus.INVALID);
                PasManager.getInstance().getSkinDataManager().store(info, d);
            }
            if (info.hasCape() && info.capeProvider().equals("M")) {
                CapeData c = new CapeData();
                c.setStatus(DownloadStatus.INVALID);
                PasManager.getInstance().getCapeDataManager().store(info, c);
            }

            created.complete(null);
            activeDownloads.remove(key);
            return created;
        }

        OverlayMessageManager.getInstance().showDownloadMessage(info.base());
        MojangUtils.getUUID(info)
            .thenCompose(uuid -> RestHelper.get(SESSION_SERVER_URL + uuid))
            .thenApply(response -> {
                Profile profile = gson.fromJson(response, Profile.class);
                String encoded = EncodeUtils.decodeBase64(profile.properties[0].value);
                return gson.fromJson(encoded, TexturedProfile.class);
            })
            .thenCompose(texturedProfile -> {
                CompletableFuture<Void> skinFuture = processSkin(texturedProfile, info);
                CompletableFuture<Void> capeFuture = processCape(texturedProfile, info);
                return CompletableFuture.allOf(skinFuture, capeFuture);
            })
            .whenComplete((v, throwable) -> {
                Throwable cause = throwable;
                while (cause instanceof CompletionException && cause.getCause() != null) {
                    cause = cause.getCause();
                }

                activeDownloads.remove(key, created);
                if (throwable != null) {
                    OverlayMessageManager.getInstance().showFailMessage(info.base());
                    PlayerArmorStandsClient.LOGGER.warn("Mojang Provider failed for {}: {}", info, cause.getMessage());
                    created.completeExceptionally(throwable);
                } else {
                    OverlayMessageManager.getInstance().showSuccessMessage(info.base());
                    created.complete(null);
                }
            });
        return created;
    }

    private CompletableFuture<Void> processSkin(TexturedProfile profile, NameInfo info) {
        if (!info.skinProvider().equals("M")) return CompletableFuture.completedFuture(null);
        var tex = profile.textures != null ? profile.textures.SKIN : null;
        if (tex == null || tex.url == null) {
            SkinData d = new SkinData();
            d.setStatus(DownloadStatus.COMPLETED);
            PasManager.getInstance().getSkinDataManager().store(info, d);
            return CompletableFuture.completedFuture(null);
        }

        Identifier location = Id.pas("skins/" + EncodeUtils.encodeToSha256(info.base()));
        Path filePath = DiskSkinProvider.CACHE_PATH.resolve(EncodeUtils.encodeToSha256(info.base()) + ".png");

        return TextureDownloader.downloadAndRegister(location, filePath, tex.url, true)
                .thenAccept(textureId -> {
                    SkinData d = new SkinData();
                    d.setTexture(textureId);
                    d.setStatus(DownloadStatus.COMPLETED);
                    PasManager.getInstance().getSkinDataManager().store(info, d);
                });
    }

    private CompletableFuture<Void> processCape(TexturedProfile profile, NameInfo info) {
        if (!info.hasCape() || !info.capeProvider().equals("M")) return CompletableFuture.completedFuture(null);
        var tex = profile.textures != null ? profile.textures.CAPE : null;
        if (tex == null || tex.url == null) {
            CapeData d = new CapeData();
            d.setStatus(DownloadStatus.COMPLETED);
            PasManager.getInstance().getCapeDataManager().store(info, d);
            return CompletableFuture.completedFuture(null);
        }
        Identifier location = Id.pas("capes/" + EncodeUtils.encodeToSha256(info.base()) + "_cape");
        Path filePath = DiskSkinProvider.CACHE_PATH.resolve(EncodeUtils.encodeToSha256(info.base()) + "_cape.png");
        return TextureDownloader.downloadAndRegister(location, filePath, tex.url, false)
            .thenAccept(textureId -> {
                CapeData d = new CapeData();
                d.setTexture(textureId);
                d.setStatus(DownloadStatus.COMPLETED);
                PasManager.getInstance().getCapeDataManager().store(info, d);
            });
    }

    private static void writeSlimMetadata(Path filePath, boolean slim) {
        Path meta = filePath.resolveSibling(filePath.getFileName().toString() + ".model");

        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(meta.getParent());
                Files.writeString(meta, slim ? "slim" : "wide");
            } catch (Exception ignored) {
            }
        }, ModExecutor.DOWNLOAD_EXECUTOR);
    }

    static class Profile {
        public String id, name;
        public ProfileProperty[] properties;
        static class ProfileProperty { public String name, value; }
    }

    static class TexturedProfile {
        public Textures textures;

        static class Textures {
            public Texture SKIN, CAPE;

            static class Texture {
                public String url;
                public Metadata metadata;

                static class Metadata {
                    public String model;
                }
            }
        }
    }
}
