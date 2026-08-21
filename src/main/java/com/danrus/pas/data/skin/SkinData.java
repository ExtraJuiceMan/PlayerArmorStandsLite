package com.danrus.pas.data.skin;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.config.PasConfig;
import com.danrus.pas.data.Texture;
import com.danrus.pas.utils.Id;
import net.minecraft.resources.Identifier;

public class SkinData extends Texture {
    public static Identifier getDefaultTexture() {
        return PasConfig.get().showArmorStandWhileDownloading
                ? Id.vanilla("textures/entity/armorstand/armorstand.png")
                : Id.vanilla("textures/entity/player/wide/steve.png");
    }


    public static boolean resolveSlim(NameInfo info, SkinData data) {
        return info.isSlim();
    }
}