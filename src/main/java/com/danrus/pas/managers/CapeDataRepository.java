package com.danrus.pas.managers;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.cape.ClientLevelCapeProvider;
import com.danrus.pas.data.cape.DiskCapeProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CapeDataRepository {
    private final ConcurrentHashMap<PasManager.CapeKey, CapeData> cache;
    private final CapeProvidersManager providers;

    private final ClientLevelCapeProvider levelProvider = new ClientLevelCapeProvider();
    private final DiskCapeProvider diskProvider = new DiskCapeProvider();

    public CapeDataRepository(
            ConcurrentHashMap<PasManager.CapeKey, CapeData> cache,
            CapeProvidersManager providers
    ) {
        this.cache = cache;
        this.providers = providers;
    }

    public CapeData resolve(NameInfo info) {
        CapeData data = levelProvider.tryLoad(info);
        if (data != null) return data;

        return diskProvider.tryLoad(info);
    }

    public CapeData peek(NameInfo info) {
        return cache.get(new PasManager.CapeKey(info));
    }

    public void store(NameInfo info, CapeData data) {
        cache.put(new PasManager.CapeKey(info), data);
    }

    public Map<PasManager.CapeKey, CapeData> getAll() {
        return cache;
    }
}