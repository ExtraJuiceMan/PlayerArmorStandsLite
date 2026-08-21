package com.danrus.pas.mixin;

import com.danrus.pas.duck.DrawSwapper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
public class LivingEntityRendererMixin implements DrawSwapper {
    @Unique @Nullable private Runnable pas$drawer = null;

    @Override
    public void pas$swapDrawer(Runnable drawer) {
        this.pas$drawer = drawer;
    }

    @WrapOperation(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private void pas$wrapRender(
            SubmitNodeCollector instance, Model model, Object object,
            PoseStack poseStack, RenderType renderType, int i, int d, int k,
            TextureAtlasSprite textureAtlasSprite, int h,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            Operation<Void> original) {
        if (pas$drawer == null) {
            original.call(instance, model, object, poseStack, renderType, i, d, k, textureAtlasSprite, h, crumblingOverlay);
            return;
        }
        pas$drawer.run();
        pas$drawer = null;
    }
}
