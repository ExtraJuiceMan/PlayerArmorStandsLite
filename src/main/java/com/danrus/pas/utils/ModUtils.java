package com.danrus.pas.utils;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public class ModUtils {
    public static Path getGameDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
    }

    public static boolean isModLoaded(String modId) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
    }

    public static int getARGBwhite(float alpha) {
        return (int) Math.floor(alpha * 255.0F) << 24 | 16777215;
    }

    public static Identifier getPlayerCapeTextureSafe(AbstractClientPlayer player) {
        try {
            return player.getSkin().cape().texturePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static void copyPartPose(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
    }
}
