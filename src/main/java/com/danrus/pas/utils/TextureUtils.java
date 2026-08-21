package com.danrus.pas.utils;

import com.danrus.pas.ModExecutor;
import com.danrus.pas.PlayerArmorStandsClient;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.utils.EncodeUtils;
import com.danrus.pas.utils.Id;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class TextureUtils {
    private static final Map<Path, CompletableFuture<Identifier>> LOCAL_TEXTURE_LOADS = new ConcurrentHashMap<>();
    private static final Map<OverlayKey, CompletableFuture<Identifier>> OVERLAY_CACHE = new ConcurrentHashMap<>();

    private TextureUtils() {}

    public record LocalSkinTextureResult(Identifier id, boolean slim) {}

    private record RawSkinImage(NativeImage image, boolean slim) {}

    private static final Map<Path, CompletableFuture<LocalSkinTextureResult>> LOCAL_SKIN_LOADS = new ConcurrentHashMap<>();

    public static CompletableFuture<LocalSkinTextureResult> registerLocalSkinTexture(Path path, Identifier identifier) {
        CompletableFuture<LocalSkinTextureResult> existing = LOCAL_SKIN_LOADS.get(path);
        if (existing != null) {
            return existing;
        }

        CompletableFuture<LocalSkinTextureResult> created = CompletableFuture
                .supplyAsync(() -> {
                    try (var input = Files.newInputStream(path)) {
                        NativeImage image = NativeImage.read(input);
                        boolean slim = TextureProcessor.isSlimSkin(image);
                        return new RawSkinImage(image, slim);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, ModExecutor.DOWNLOAD_EXECUTOR)
                .thenCompose(raw ->
                        registerTexture(raw.image(), identifier, true)
                                .thenApply(id -> new LocalSkinTextureResult(id, raw.slim()))
                );

        CompletableFuture<LocalSkinTextureResult> previous = LOCAL_SKIN_LOADS.putIfAbsent(path, created);
        if (previous != null) {
            return previous;
        }

        created.whenComplete((result, error) -> LOCAL_SKIN_LOADS.remove(path, created));
        return created;
    }

    public static CompletableFuture<Identifier> registerTexture(Path path, Identifier identifier, boolean remap) {
        CompletableFuture<Identifier> existing = LOCAL_TEXTURE_LOADS.get(path);
        if (existing != null) {
            return existing;
        }

        CompletableFuture<Identifier> created = CompletableFuture
                .supplyAsync(() -> {
                    try (var input = Files.newInputStream(path)) {
                        return NativeImage.read(input);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, ModExecutor.DOWNLOAD_EXECUTOR)
                .thenCompose(image -> registerTexture(image, identifier, remap));

        CompletableFuture<Identifier> previous = LOCAL_TEXTURE_LOADS.putIfAbsent(path, created);
        if (previous != null) {
            return previous;
        }

        created.whenComplete((id, error) -> LOCAL_TEXTURE_LOADS.remove(path, created));
        return created;
    }

    public static CompletableFuture<Identifier> registerTexture(NativeImage image, Identifier identifier, boolean remap) {
        NativeImage processed = image;
        if (remap) {
            processed = TextureProcessor.remapLegacySkin(image);
            if (processed == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid skin"));
            }
        }
        return registerTexture(processed, identifier);
    }

    public static CompletableFuture<Identifier> registerTexture(NativeImage image, Identifier identifier) {
        CompletableFuture<Identifier> future = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> {
            DynamicTexture texture = null;
            try {
                texture = new DynamicTexture(identifier::toString, image);
                Minecraft.getInstance().getTextureManager().register(identifier, texture);
                clearOverlayCacheFor(identifier);
                future.complete(identifier);
            } catch (Exception e) {
                if (texture != null) texture.close();
                else image.close();
                PlayerArmorStandsClient.LOGGER.warn("Failed to register texture: {}", identifier, e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static void unregisterTexture(Identifier identifier) {
        if (!PlayerArmorStandsClient.MOD_ID.equals(identifier.getNamespace())) return;
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().getTextureManager().release(identifier));
        clearOverlayCacheFor(identifier);
    }

    public static void clearOverlayCacheFor(Identifier source) {
        OVERLAY_CACHE.entrySet().removeIf(entry -> {
            if (!entry.getKey().source().equals(source)) return false;
            entry.getValue().thenAccept(id ->
                Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().getTextureManager().release(id)));
            return true;
        });
    }

    public static void clearOverlayCache() {
        OVERLAY_CACHE.values().forEach(f -> f.thenAccept(id ->
            Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().getTextureManager().release(id))));
        OVERLAY_CACHE.clear();
    }

    public static Identifier getOverlayedTexture(NameInfo info, Class<?> holderType) {
        if (info.overlayTexture().isEmpty()) {
            return holderType == SkinData.class
                ? PasManager.getInstance().getSkinTexture(info)
                : PasManager.getInstance().getCapeTexture(info);
        }
        Identifier source = holderType == SkinData.class
            ? PasManager.getInstance().getSkinTexture(info)
            : PasManager.getInstance().getCapeTexture(info);
        Identifier material = Id.vanilla("textures/block/" + info.overlayTexture() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(material).isEmpty()) {
            return source;
        }
        OverlayKey key = new OverlayKey(source, material, info.overlayBlend());
        CompletableFuture<Identifier> result = OVERLAY_CACHE.get(key);
        if (result == null) {
            result = createOverlayTexture(key);
            CompletableFuture<Identifier> prev = OVERLAY_CACHE.putIfAbsent(key, result);
            if (prev != null) {
                result.cancel(false);
                result = prev;
            }
        }
        if (result.isCompletedExceptionally()) {
            OVERLAY_CACHE.remove(key, result);
            return source;
        }
        if (!result.isDone()) return source;
        return result.getNow(source);
    }

    private static CompletableFuture<Identifier> createOverlayTexture(OverlayKey key) {
        return CompletableFuture.supplyAsync(() -> {
            NativeImage sourceImage = copyNativeImage(key.source());
            if (sourceImage == null) throw new IllegalStateException("Cannot read source texture");
            try (sourceImage;
                 var input = Minecraft.getInstance().getResourceManager().open(key.material());
                 NativeImage materialImage = NativeImage.read(input)) {
                return TextureProcessor.applyMaterial(sourceImage, materialImage, key.blend() / 100.0F);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, ModExecutor.MAIN_EXECUTOR).thenCompose(result -> {
            String hash = EncodeUtils.encodeToSha256(key.source() + "|" + key.material() + "|" + key.blend());
            return registerTexture(result, Id.pas("generated/overlay/" + hash));
        });
    }

    private static NativeImage copyNativeImage(Identifier identifier) {
        var texture = Minecraft.getInstance().getTextureManager().getTexture(identifier);
        if (texture instanceof DynamicTexture dynamicTexture) {
            NativeImage pixels = dynamicTexture.getPixels();
            if (pixels == null) return null;
            NativeImage copy = new NativeImage(pixels.getWidth(), pixels.getHeight(), true);
            copy.copyFrom(pixels);
            return copy;
        }
        return null;
    }

    private record OverlayKey(Identifier source, Identifier material, int blend) {}
}
