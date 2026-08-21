package com.danrus.pas.data.skin;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.config.PasConfig;
import net.minecraft.client.Minecraft;

public class ClientLevelSkinProvider {
    public SkinData tryLoad(NameInfo info) {
        if (!PasConfig.get().tryApplyFromServerPlayer) return null;

        if (!info.skinProvider().equals("M")) return null;

        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        for (var player : level.players()) {
            if (player.getName().getString().equals(info.base())) {
                SkinData data = new SkinData();
                data.setTexture(player.getSkin().body().texturePath());
                data.setStatus(DownloadStatus.COMPLETED);
                return data;
            }
        }
        return null;
    }
}