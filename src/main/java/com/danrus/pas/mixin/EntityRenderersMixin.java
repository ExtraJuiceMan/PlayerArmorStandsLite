package com.danrus.pas.mixin;

import com.danrus.pas.render.armorstand.PasEntityRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderers.class)
public class EntityRenderersMixin {
    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderers;register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/client/renderer/entity/EntityRendererProvider;)V")
    )
    private static <T extends Entity> void pas$regRenderer(
            EntityType<? extends T> entityType,
            EntityRendererProvider<T> entityRendererProvider,
            Operation<Void> original) {
        if (entityType == EntityTypes.ARMOR_STAND) {
            EntityRendererProvider<ArmorStand> p = PasEntityRenderer::new;
            original.call(entityType, p);
            return;
        }
        original.call(entityType, entityRendererProvider);
    }
}
