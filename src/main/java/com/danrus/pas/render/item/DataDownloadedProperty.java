package com.danrus.pas.render.item;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.render.armorstand.PlayerArmorStandModel;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class DataDownloadedProperty implements ConditionalItemModelProperty {
    public static final MapCodec<DataDownloadedProperty> MAP_CODEC = MapCodec.unit(new DataDownloadedProperty());

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() { return MAP_CODEC; }

    @Override
    public boolean get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner,
                       int seed, ItemDisplayContext displayContext) {
        NameInfo info = NameInfo.parse(itemStack.get(DataComponents.CUSTOM_NAME));
        SkinData data = PasManager.getInstance().getSkinData(info);
        return !PlayerArmorStandModel.showArmorStandWhileDownload(data);
    }
}
