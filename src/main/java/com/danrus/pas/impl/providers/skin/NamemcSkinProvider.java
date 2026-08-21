package com.danrus.pas.impl.providers.skin;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.impl.providers.common.AbstractNamemcProvider;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.utils.EncodeUtils;
import com.danrus.pas.utils.Id;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public class NamemcSkinProvider extends AbstractNamemcProvider<SkinData> {
    @Override public String getLiteral() { return "N"; }
    @Override protected Identifier getLocation(NameInfo info) { return Id.pas("skins/" + getFileName(info)); }
    @Override protected String getFileName(NameInfo info) { return EncodeUtils.encodeToSha256(info.base()) + "_namemc"; }
    @Override protected String getNamemcId(NameInfo info) { return info.base(); }
    @Override protected boolean shouldRemap() { return true; }
    @Override protected SkinData createDataHolder() { return new SkinData(); }
    @Override protected void store(NameInfo info, SkinData data) {
        PasManager.getInstance().getSkinDataManager().store(info, data);
    }
}
