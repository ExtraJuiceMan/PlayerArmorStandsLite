package com.danrus.pas.data.cape;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.config.PasConfig;
import com.danrus.pas.utils.ModUtils;
import net.minecraft.client.Minecraft;

public class ClientLevelCapeProvider {
    public CapeData tryLoad(NameInfo info) {
        if (!PasConfig.get().tryApplyFromServerPlayer) return null;

        if (!info.hasCape() || !info.capeProvider().equals("M")) return null;

        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        for (var player : level.players()) {
            if (player.getName().getString().equals(info.base())) {
                var capeTex = ModUtils.getPlayerCapeTextureSafe(player);
                if (capeTex == null) return null;
                CapeData data = new CapeData();
                data.setTexture(capeTex);
                data.setStatus(DownloadStatus.COMPLETED);
                return data;
            }
        }
        return null;
    }
}