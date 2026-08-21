package com.danrus.pas.managers;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.utils.TextureUtils;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class PasManager {
    private static volatile PasManager INSTANCE;

    private final ConcurrentHashMap<SkinKey, SkinData> skinCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CapeKey, CapeData> capeCache = new ConcurrentHashMap<>();

    private final SkinProvidersManager skinProviders = new SkinProvidersManager();
    private final CapeProvidersManager capeProviders = new CapeProvidersManager();

    private final SkinDataRepository skinRepo = new SkinDataRepository(skinCache, skinProviders);
    private final CapeDataRepository capeRepo = new CapeDataRepository(capeCache, capeProviders);

    private PasManager() {
        skinProviders.initialize(this);
        capeProviders.initialize(this);
    }

    public Identifier getSkinTexture(NameInfo info) {
        SkinData d = skinCache.get(new SkinKey(info));
        return d != null ? d.getTexture(SkinData.getDefaultTexture()) : SkinData.getDefaultTexture();
    }

    public Identifier getCapeTexture(NameInfo info) {
        CapeData d = capeCache.get(new CapeKey(info));
        return d != null ? d.getTexture(CapeData.DEFAULT_TEXTURE) : CapeData.DEFAULT_TEXTURE;
    }

    public Identifier getSkinWithOverlay(NameInfo info) {
        if (info.isEmpty()) return SkinData.getDefaultTexture();
        return TextureUtils.getOverlayedTexture(info, SkinData.class);
    }

    public Identifier getCapeWithOverlay(NameInfo info) {
        if (info.isEmpty() || !info.hasCape()) return CapeData.DEFAULT_TEXTURE;
        return TextureUtils.getOverlayedTexture(info, CapeData.class);
    }

    public SkinData getSkinData(NameInfo info) {
        if (info.isEmpty()) return null;

        SkinKey key = new SkinKey(info);
        SkinData cached = skinCache.get(key);

        if (cached != null) {
            return cached;
        }

        SkinData data = skinRepo.resolve(info);

        if (data != null) {
            skinCache.putIfAbsent(key, data);
            return skinCache.get(key);
        }

        SkinData placeholder = new SkinData();
        placeholder.setStatus(DownloadStatus.IN_PROGRESS);

        SkinData existing = skinCache.putIfAbsent(key, placeholder);

        if (existing == null) {
            skinProviders.download(info);
            return placeholder;
        }

        return existing;
    }

    public CapeData getCapeData(NameInfo info) {
        if (info.isEmpty() || !info.hasCape()) return null;

        CapeKey key = new CapeKey(info);
        CapeData cached = capeCache.get(key);

        if (cached != null) {
            return cached;
        }

        CapeData data = capeRepo.resolve(info);

        if (data != null) {
            capeCache.putIfAbsent(key, data);
            return capeCache.get(key);
        }

        CapeData placeholder = new CapeData();
        placeholder.setStatus(DownloadStatus.IN_PROGRESS);

        CapeData existing = capeCache.putIfAbsent(key, placeholder);

        if (existing == null) {
            capeProviders.download(info);
            return placeholder;
        }

        return existing;
    }

    public void reloadSkin(String baseName) {
        NameInfo info = NameInfo.parse(baseName);

        skinCache.keySet().removeIf(key -> key.base.equals(info.base()));
        skinProviders.download(info);
    }

    public void reloadCape(String baseName) {
        NameInfo info = NameInfo.parse(baseName);

        capeCache.keySet().removeIf(key -> key.base.equals(info.base()));
        capeProviders.download(info);
    }

    public void reloadAll() {
        for (Map.Entry<SkinKey, SkinData> e : skinCache.entrySet()) {
            if (e.getValue().getStatus() != DownloadStatus.COMPLETED) {
                skinProviders.download(e.getKey().info());
            }
        }

        for (Map.Entry<CapeKey, CapeData> e : capeCache.entrySet()) {
            if (e.getValue().getStatus() != DownloadStatus.COMPLETED) {
                capeProviders.download(e.getKey().info());
            }
        }
    }

    public void reloadFailed() {
        for (Map.Entry<SkinKey, SkinData> e : skinCache.entrySet()) {
            if (e.getValue().getStatus() == DownloadStatus.FAILED) {
                skinProviders.download(e.getKey().info());
            }
        }

        for (Map.Entry<CapeKey, CapeData> e : capeCache.entrySet()) {
            if (e.getValue().getStatus() == DownloadStatus.FAILED) {
                capeProviders.download(e.getKey().info());
            }
        }
    }

    public void dropCache() {
        TextureUtils.clearOverlayCache();
        skinCache.clear();
        capeCache.clear();
        skinProviders.clearPending();
        capeProviders.clearPending();
    }

    public SkinDataRepository getSkinDataManager() {
        return skinRepo;
    }

    public CapeDataRepository getCapeDataManager() {
        return capeRepo;
    }

    public SkinProvidersManager getSkinProviderManager() {
        return skinProviders;
    }

    public CapeProvidersManager getCapeProviderManager() {
        return capeProviders;
    }

    public static PasManager getInstance() {
        if (INSTANCE == null) {
            synchronized (PasManager.class) {
                if (INSTANCE == null) INSTANCE = new PasManager();
            }
        }
        return INSTANCE;
    }

    public static final class SkinKey {
        private final String base;
        private final String provider;
        private final NameInfo info;

        public SkinKey(NameInfo info) {
            this.base = info.base();
            this.provider = info.skinProvider();
            this.info = info;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SkinKey skinKey)) return false;
            return base.equals(skinKey.base) && provider.equals(skinKey.provider);
        }

        @Override
        public int hashCode() {
            return Objects.hash(base, provider);
        }

        public NameInfo info() {
            return info;
        }
    }

    public static final class CapeKey {
        private final String base;
        private final String provider;
        private final String id;
        private final NameInfo info;

        public CapeKey(NameInfo info) {
            this.base = info.base();
            this.provider = info.capeProvider();
            this.id = info.capeId();
            this.info = info;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CapeKey capeKey)) return false;
            return base.equals(capeKey.base)
                    && provider.equals(capeKey.provider)
                    && id.equals(capeKey.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(base, provider, id);
        }

        public NameInfo info() {
            return info;
        }
    }
}