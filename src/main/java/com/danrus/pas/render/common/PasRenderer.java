package com.danrus.pas.render.common;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.render.armorstand.PlayerArmorStandModel;
import com.danrus.pas.utils.Id;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class PasRenderer {
    public static Identifier WOOD = Id.vanilla("textures/entity/armorstand/armorstand.png");
    public static Identifier STEVE = Id.vanilla("textures/entity/player/wide/steve.png");

    private final PlayerArmorStandModel model;
    @Nullable
    private final PlayerArmorStandModel smallModel;
    private final boolean inGui;

    public PasRenderer(PlayerArmorStandModel model) {
        this(model, null, true);
    }

    public PasRenderer(PlayerArmorStandModel model,
                       @Nullable PlayerArmorStandModel smallModel,
                       boolean inGui) {
        this.model = model;
        this.smallModel = smallModel;
        this.inGui = inGui;
    }

    public void submit(SkinData skinData,
                       @Nullable CapeData capeData,
                       NameInfo info,
                       SubmitNodeCollector collector,
                       PasModelSettings settings,
                       PoseStack poseStack,
                       int packedLight,
                       int packedOverlay) {
        submit(
                getModel(settings.isSmall()),
                skinData,
                capeData,
                info,
                collector,
                settings,
                poseStack,
                packedLight,
                packedOverlay
        );
    }

    public void submit(PlayerArmorStandModel modelToRender,
                       SkinData skinData,
                       @Nullable CapeData capeData,
                       NameInfo info,
                       SubmitNodeCollector collector,
                       PasModelSettings settings,
                       PoseStack poseStack,
                       int packedLight,
                       int packedOverlay) {
        if (modelToRender == null) {
            return;
        }

        if (inGui && info.shouldUpsideDown()) {
            poseStack.translate(0.0F, 0.975F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }

        setupAnim(info, modelToRender, settings);

        for (ModelPart part : modelToRender.getOriginalParts()) {
            drawPart(
                    poseStack,
                    part,
                    RenderTypes.entityCutout(WOOD),
                    collector,
                    packedLight,
                    packedOverlay
            );
        }

        boolean showDefaultSkin = info.isEmpty()
                || PlayerArmorStandModel.showArmorStandWhileDownload(skinData);

        Identifier location = showDefaultSkin
                ? STEVE
                : PasManager.getInstance().getSkinWithOverlay(info);

        for (ModelPart part : modelToRender.getPlayerParts()) {
            drawPart(
                    poseStack,
                    part,
                    RenderTypes.entityTranslucent(location),
                    collector,
                    packedLight,
                    packedOverlay
            );
        }

        if (settings.foil()) {
            for (ModelPart part : modelToRender.getOriginalParts()) {
                drawPart(
                        poseStack,
                        part,
                        RenderTypes.glint(),
                        collector,
                        packedLight,
                        packedOverlay
                );
            }

            for (ModelPart part : modelToRender.getPlayerParts()) {
                drawPart(
                        poseStack,
                        part,
                        RenderTypes.glint(),
                        collector,
                        packedLight,
                        packedOverlay
                );
            }
        }

        if (info.hasCape() && capeData != null
                && !capeData.getTexture(CapeData.DEFAULT_TEXTURE).equals(CapeData.DEFAULT_TEXTURE)) {
            Identifier capeTexture = PasManager.getInstance().getCapeWithOverlay(info);

            drawPart(poseStack, modelToRender.getCape(), RenderTypes.entityTranslucent(capeTexture), collector, packedLight, packedOverlay);

            if (settings.foil()) {
                drawPart(poseStack, modelToRender.getCape(), RenderTypes.glint(), collector, packedLight, packedOverlay);
            }
        }
    }

    private static void setupAnim(NameInfo info, PlayerArmorStandModel model, PasModelSettings settings) {
        model.setupModel(settings.poseSettings(), info);
    }

    private static void drawPart(PoseStack poseStack,
                                 ModelPart part,
                                 RenderType type,
                                 SubmitNodeCollector nodeCollector,
                                 int packedLight,
                                 int packedOverlay) {
        if (!part.visible) {
            return;
        }

        PartPose savedPose = part.storePose();

        float xs = part.xScale;
        float ys = part.yScale;
        float zs = part.zScale;

        boolean vis = part.visible;
        boolean skip = part.skipDraw;

        nodeCollector.submitCustomGeometry(poseStack, type, (pose, vertexConsumer) -> {
            PoseStack renderPS = new PoseStack();
            renderPS.last().set(pose);

            part.render(renderPS, vertexConsumer, packedLight, packedOverlay);
        });

        part.loadPose(savedPose);

        part.xScale = xs;
        part.yScale = ys;
        part.zScale = zs;

        part.visible = vis;
        part.skipDraw = skip;
    }

    public PlayerArmorStandModel getModel(boolean isSmall) {
        return isSmall && smallModel != null ? smallModel : model;
    }
}