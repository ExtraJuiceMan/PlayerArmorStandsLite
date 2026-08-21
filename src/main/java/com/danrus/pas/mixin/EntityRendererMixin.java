package com.danrus.pas.mixin;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.config.PasConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(net.minecraft.client.renderer.entity.EntityRenderer.class)
public class EntityRendererMixin {

    private void pas$submitNameTag(
            SubmitNodeCollector instance, PoseStack poseStack, Vec3 vec3, int i1,
            Component displayName, boolean b, int i2,
            CameraRenderState cameraRenderState,
            Operation<Void> original) {

        PasConfig cfg = PasConfig.get();
        String raw = displayName.getString();

        if (cfg.enableMod && cfg.hideParamsOnLabel && raw.contains("|")) {
            NameInfo info = NameInfo.parse(raw);
            Component newName = info.hasDisplayName()
                    ? Component.literal(info.displayName())
                    : Component.literal(info.base());
            original.call(instance, poseStack, vec3, i1, newName, b, i2, cameraRenderState);
        } else {
            original.call(instance, poseStack, vec3, i1, displayName, b, i2, cameraRenderState);
        }
    }
}
