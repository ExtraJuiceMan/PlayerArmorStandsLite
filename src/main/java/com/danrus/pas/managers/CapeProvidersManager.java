package com.danrus.pas.managers;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.impl.providers.MojangProvider;
import com.danrus.pas.impl.providers.cape.MinecraftCapesTextureProvider;
import com.danrus.pas.impl.providers.cape.NamemcCapeProvider;
import com.danrus.pas.impl.providers.common.AbstractTextureProviderManager;

public class CapeProvidersManager extends AbstractTextureProviderManager<CapeData> {
    @Override
    protected void prepareProviders() {
        addProvider(MojangProvider.getInstance());
        addProvider(new MinecraftCapesTextureProvider(), 2);
        addProvider(new NamemcCapeProvider(), 1);
    }

    @Override
    protected String getTextureType() {
        return "Cape";
    }

    @Override
    protected String getDownloadKey(NameInfo info) {
        return "CAPE|" + info.base() + "|" + info.capeProvider() + "|" + info.capeId();
    }

    @Override
    protected String getProvider(NameInfo info) {
        return info.capeProvider();
    }

    @Override
    protected String getDefaultLiteral() { return "M"; }

    @Override
    protected String getExcludeLiterals() { return "F"; }

    @Override
    protected void store(NameInfo info, CapeData data) {
        PasManager.getInstance().getCapeDataManager().store(info, data);
    }

    @Override
    protected CapeData createDataHolder() { return new CapeData(); }

    @Override
    protected void invalidate(NameInfo info) {
        CapeData d = new CapeData();
        d.setStatus(DownloadStatus.FAILED);
        store(info, d);
    }
}
