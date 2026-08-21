package com.danrus.pas.render.armorstand;

import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;

import java.util.List;
import java.util.Map;

public class DummyEntityModel extends ArmorStandArmorModel {
    private static final ModelPart EMPTY_PART = new ModelPart(List.of(), Map.of());
    private static final ModelPart EMPTY_HEAD = new ModelPart(List.of(), Map.of("hat", EMPTY_PART));
    private static final ModelPart EMPTY_ROOT = new ModelPart(List.of(), Map.of(
        "head", EMPTY_HEAD,
        "body", EMPTY_PART,
        "right_arm", EMPTY_PART,
        "left_arm", EMPTY_PART,
        "right_leg", EMPTY_PART,
        "left_leg", EMPTY_PART
    ));
    public static final DummyEntityModel INSTANTS = new DummyEntityModel();

    protected DummyEntityModel() { super(EMPTY_ROOT); }

    @Override
    public void setupAnim(ArmorStandRenderState renderState) {}
}
