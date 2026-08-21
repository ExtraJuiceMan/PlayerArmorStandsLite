package com.danrus.pas.mixin;

import com.danrus.pas.render.item.PasSpecialModelRenderer;
import com.danrus.pas.utils.Id;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpecialModelRenderers.class)
public class SpecialModelRenderersMixin {
    @Inject(method = "bootstrap", at = @At("RETURN"))
    private static void bootstrapInject(CallbackInfo ci) {
        SpecialModelRenderers.ID_MAPPER.put(Id.pas("armor_stand"), PasSpecialModelRenderer.Unbaked.MAP_CODEC);
    }
}
