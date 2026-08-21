package com.danrus.pas.render.item;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.render.armorstand.PlayerArmorStandModel;
import com.danrus.pas.render.common.PasModelPoseSettings;
import com.danrus.pas.render.common.PasModelSettings;
import com.danrus.pas.render.common.PasRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PasSpecialModelRenderer implements SpecialModelRenderer<ItemRenderData> {
    protected final PasRenderer renderer;
    protected final PasModelPoseSettings state;

    protected PasSpecialModelRenderer(PlayerArmorStandModel model, PasModelPoseSettings state) {
        this.renderer = new PasRenderer(model);
        this.state = state;
    }

    @Override
    public void submit(@Nullable ItemRenderData argument, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, int packedLight,
                       int packedOverlay, boolean hasFoilType, int outlineColor) {
        SkinData skin;
        CapeData cape;
        NameInfo info;
        if (argument != null) {
            skin = argument.skinData();
            cape = argument.capeData();
            info = argument.info();
        } else {
            skin = new SkinData();
            cape = null;
            info = NameInfo.EMPTY;
        }
        preparePose(poseStack);
        prepareModel(info);
        renderer.submit(skin, cape, info, submitNodeCollector,
            new PasModelSettings(state, hasFoilType, false), poseStack, packedLight, packedOverlay);
    }

    @Override
    public void getExtents(@NonNull Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        preparePose(poseStack);
        prepareModel(null);
        List<ModelPart> partsToMeasure = new ArrayList<>();
        partsToMeasure.addAll(renderer.getModel(false).getOriginalParts());
        partsToMeasure.addAll(renderer.getModel(false).getPlayerParts());
        partsToMeasure.add(renderer.getModel(false).getMemePart());
        for (ModelPart part : partsToMeasure) {
            if (part.visible) {
                part.getExtentsForGui(poseStack, output);
            }
        }
    }

    private static void preparePose(PoseStack poseStack) {
        poseStack.translate(0.5, 0.75, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.scale(0.5f, 0.5f, 0.5f);
    }

    private void prepareModel(@Nullable NameInfo infoCandidate) {
        NameInfo info = infoCandidate != null ? infoCandidate : NameInfo.EMPTY;
        var model = renderer.getModel(false);
        var renderState = state.toRenderState(info);
        renderState.showBasePlate = state.baseplate;
        model.setupAnim(renderState, info, true);
        model.setupVisibilityForItem(state, info);
    }
    @Override
    public @Nullable ItemRenderData extractArgument(ItemStack stack) {
        NameInfo info = NameInfo.parse(stack.getCustomName());
        var skin = PasManager.getInstance().getSkinData(info);
        var cape = PasManager.getInstance().getCapeData(info);
        return new ItemRenderData(skin != null ? skin : new SkinData(), cape, info);
    }

    public record Unbaked(PasModelPoseSettings state) implements SpecialModelRenderer.Unbaked<ItemRenderData> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                PasModelPoseSettings.CODEC.optionalFieldOf("state", new PasModelPoseSettings()).forGetter(Unbaked::state)
            ).apply(instance, Unbaked::new)
        );

        public @Nullable SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            PlayerArmorStandModel pasModel = new PlayerArmorStandModel(
                PlayerArmorStandModel.createBodyLayer(CubeDeformation.NONE, new CubeDeformation(-0.001f)).bakeRoot());
            return new PasSpecialModelRenderer(pasModel, state);
        }

        @Override
        @SuppressWarnings("unchecked")
        public @Nullable SpecialModelRenderer<ItemRenderData> bake(BakingContext context) {
            return (SpecialModelRenderer<ItemRenderData>) bake(context.entityModelSet());
        }

        @Override
        public MapCodec<Unbaked> type() { return MAP_CODEC; }
    }
}
