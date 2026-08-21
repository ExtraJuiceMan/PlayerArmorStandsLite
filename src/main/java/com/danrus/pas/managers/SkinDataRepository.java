package com.danrus.pas.managers;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.skin.ClientLevelSkinProvider;
import com.danrus.pas.data.skin.DiskSkinProvider;
import com.danrus.pas.data.skin.FileSkinProvider;
import com.danrus.pas.data.skin.SkinData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkinDataRepository {
    private final ConcurrentHashMap<PasManager.SkinKey, SkinData> cache;
    private final SkinProvidersManager providers;

    private final ClientLevelSkinProvider levelProvider = new ClientLevelSkinProvider();
    private final DiskSkinProvider diskProvider = new DiskSkinProvider();
    private final FileSkinProvider fileProvider = new FileSkinProvider();

    public SkinDataRepository(
            ConcurrentHashMap<PasManager.SkinKey, SkinData> cache,
            SkinProvidersManager providers
    ) {
        this.cache = cache;
        this.providers = providers;
    }

    public SkinData resolve(NameInfo info) {
        SkinData data;
        if ((data = levelProvider.tryLoad(info)) != null) return data;
        if ((data = diskProvider.tryLoad(info))  != null) return data;
        return fileProvider.tryLoad(info);
    }

    public SkinData peek(NameInfo info) {
        return cache.get(new PasManager.SkinKey(info));
    }

    public void store(NameInfo info, SkinData data) {
        cache.put(new PasManager.SkinKey(info), data);
    }

    public Map<PasManager.SkinKey, SkinData> getAll() {
        return cache;
    }
}