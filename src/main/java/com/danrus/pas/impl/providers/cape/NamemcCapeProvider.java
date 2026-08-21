package com.danrus.pas.impl.providers.cape;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.impl.providers.common.AbstractNamemcProvider;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.utils.Id;
import net.minecraft.resources.Identifier;

public class NamemcCapeProvider extends AbstractNamemcProvider<CapeData> {
    @Override public String getLiteral() { return "A"; }
    @Override protected Identifier getLocation(NameInfo info) { return Id.pas("capes/cape_" + info.capeId()); }
    @Override protected String getFileName(NameInfo info) { return "cape_" + info.capeId(); }
    @Override protected String getNamemcId(NameInfo info) { return info.capeId(); }
    @Override protected boolean shouldRemap() { return false; }
    @Override protected CapeData createDataHolder() { return new CapeData(); }
    @Override protected void store(NameInfo info, CapeData data) {
        PasManager.getInstance().getCapeDataManager().store(info, data);
    }
}
