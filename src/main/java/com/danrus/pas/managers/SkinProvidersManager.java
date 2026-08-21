package com.danrus.pas.managers;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.impl.providers.MojangProvider;
import com.danrus.pas.impl.providers.skin.NamemcSkinProvider;
import com.danrus.pas.impl.providers.common.AbstractTextureProviderManager;

public class SkinProvidersManager extends AbstractTextureProviderManager<SkinData> {
    @Override
    protected void prepareProviders() {
        addProvider(MojangProvider.getInstance());
        addProvider(new NamemcSkinProvider(), 1);
    }

    @Override
    protected String getTextureType() {
        return "Skin";
    }

    @Override
    protected String getDownloadKey(NameInfo info) {
        return "SKIN|" + info.base() + "|" + info.skinProvider();
    }

    @Override
    protected String getProvider(NameInfo info) {
        return info.skinProvider();
    }

    @Override
    protected String getDefaultLiteral() { return "M"; }

    @Override
    protected String getExcludeLiterals() { return "F"; }

    @Override
    protected void store(NameInfo info, SkinData data) {
        PasManager.getInstance().getSkinDataManager().store(info, data);
    }

    @Override
    protected SkinData createDataHolder() { return new SkinData(); }

    @Override
    protected void invalidate(NameInfo info) {
        SkinData d = new SkinData();
        d.setStatus(DownloadStatus.FAILED);
        store(info, d);
    }
}
