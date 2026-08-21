package com.danrus.pas.render.armorstand;

import com.danrus.pas.api.NameInfo;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import org.jetbrains.annotations.Nullable;

public class PasEntityRenderState extends ArmorStandRenderState {
    public NameInfo info = NameInfo.EMPTY;
    @Nullable public PlayerArmorStandModel ownModel;
}