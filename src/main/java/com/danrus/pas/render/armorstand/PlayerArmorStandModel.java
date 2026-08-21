package com.danrus.pas.render.armorstand;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.config.PasConfig;
import com.danrus.pas.data.Texture;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.render.common.PasModelPoseSettings;
import com.danrus.pas.utils.ModUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.core.Rotations;

import java.util.Collection;
import java.util.List;

public class PlayerArmorStandModel extends ArmorStandArmorModel {
    public final ModelPart leftSleeve, rightSleeve, leftPants, rightPants;
    public final ModelPart leftSlimArm, rightSlimArm, leftSlimSleeve, rightSlimSleeve;
    public final ModelPart rightBodyStick, leftBodyStick, shoulderStick, basePlate;
    public final ModelPart originalHead, originalBody, originalRightArm, originalLeftArm, originalRightLeg, originalLeftLeg;
    public final ModelPart jacket;
    private final ModelPart cloak;
    private final List<ModelPart> originalParts;
    private final List<ModelPart> playerParts;

    public PlayerArmorStandModel(ModelPart root) {
        super(root);
        this.cloak = root.getChild("cloak");
        this.leftSleeve = root.getChild("left_sleeve");
        this.rightSleeve = root.getChild("right_sleeve");
        this.leftPants = root.getChild("left_pants");
        this.rightPants = root.getChild("right_pants");
        this.jacket = root.getChild("jacket");
        this.leftSlimArm = root.getChild("left_slim_arm");
        this.rightSlimArm = root.getChild("right_slim_arm");
        this.leftSlimSleeve = root.getChild("left_slim_sleeve");
        this.rightSlimSleeve = root.getChild("right_slim_sleeve");
        this.rightBodyStick = root.getChild("right_body_stick");
        this.leftBodyStick = root.getChild("left_body_stick");
        this.shoulderStick = root.getChild("shoulder_stick");
        this.basePlate = root.getChild("base_plate");
        this.originalHead = root.getChild("original_head");
        this.originalBody = root.getChild("original_body");
        this.originalRightArm = root.getChild("original_right_arm");
        this.originalLeftArm = root.getChild("original_left_arm");
        this.originalRightLeg = root.getChild("original_right_leg");
        this.originalLeftLeg = root.getChild("original_left_leg");
        this.hat.visible = true;
        this.rightBodyStick.visible = false;
        this.leftBodyStick.visible = false;
        this.shoulderStick.visible = false;
        this.basePlate.visible = false;
        this.originalParts = List.of(
                originalBody, originalLeftArm, originalRightArm,
                originalLeftLeg, originalRightLeg,
                rightBodyStick, leftBodyStick, shoulderStick,
                basePlate, originalHead);
        this.playerParts = List.of(
                body, jacket, leftArm, rightArm,
                leftSleeve, rightSleeve,
                leftPants, rightPants, leftLeg, rightLeg,
                leftSlimArm, rightSlimArm,
                leftSlimSleeve, rightSlimSleeve, head);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation deformation) {
        return createBodyLayer(deformation, deformation);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation deformation, CubeDeformation armDeformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, deformation, 1.0F, 0.5F), PartPose.offset(0.0F, -0.02F, 0.21F));
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, armDeformation), PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, armDeformation.extend(0.25F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, armDeformation.extend(0.25F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, armDeformation), PartPose.offset(-5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation), PartPose.offset(1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_pants", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation.extend(0.25F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_pants", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation.extend(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, deformation.extend(0.25F)), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_body_stick", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F, deformation), PartPose.offset(-4.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_body_stick", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F, deformation), PartPose.offset(4.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("shoulder_stick", CubeListBuilder.create().texOffs(0, 0).addBox(-6.5F, -0.5F, -0.5F, 13.0F, 1.0F, 1.0F, deformation), PartPose.offset(0.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("base_plate", CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 1.0F, 12.0F, deformation), PartPose.offset(0.0F, 23.01F, 0.0F));
        partdefinition.addOrReplaceChild("left_slim_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, armDeformation), PartPose.offset(5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("right_slim_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, armDeformation), PartPose.offset(-5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("left_slim_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, armDeformation.extend(0.25F)), PartPose.offset(5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("right_slim_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, armDeformation.extend(0.25F)), PartPose.offset(-5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("original_head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
        partdefinition.addOrReplaceChild("original_body", CubeListBuilder.create().texOffs(0, 26).addBox(-6.0F, 0.0F, -1.5F, 12.0F, 3.0F, 3.0F), PartPose.ZERO);
        partdefinition.addOrReplaceChild("original_right_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("original_left_arm", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(0.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("original_right_leg", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("original_left_leg", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition headDefinition = partdefinition.getChild("head");
        CubeListBuilder earsCubeListBuilder = CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -6.0F, -1.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(1.0F));
        headDefinition.addOrReplaceChild("left_ear", earsCubeListBuilder, PartPose.offset(-6.0F, -6.0F, 0.0F));
        headDefinition.addOrReplaceChild("right_ear", earsCubeListBuilder, PartPose.offset(6.0F, -6.0F, 0.0F));
        partdefinition.addOrReplaceChild("lol", CubeListBuilder.create().texOffs(0, 0).addBox(-32, -32, 0, 64, 64, 0), PartPose.offset(0.0f, 3.0f, 0.0f));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(ArmorStandRenderState armorStand) {
        if (armorStand instanceof PasEntityRenderState pas) {
            setupAnim(armorStand, pas.info, true);
        } else {
            setupAnim(armorStand, NameInfo.parse(armorStand.nameTag), true);
        }
    }

    public void setupAnim(ArmorStandRenderState armorStand, NameInfo info, boolean setupVisibility) {
        super.setupAnim(armorStand);

        boolean showBase = armorStand.showBasePlate;
        boolean showArms = armorStand.showArms;
        Rotations bodyPose = armorStand.bodyPose;

        copyPlayerPartsFromStand();
        setupCape();
        this.basePlate.yRot = ((float) Math.PI / 180F) * -armorStand.yRot;

        if (!PasConfig.get().enableMod) {
            setOriginalAngles(showBase, showArms, bodyPose);
            return;
        }

        if (setupVisibility) {
            SkinData skinData = PasManager.getInstance().getSkinData(info);
            boolean resolvedSlim = SkinData.resolveSlim(info, skinData);
            this.setModelVisibility(
                    !showArmorStandWhileDownload(skinData),
                    resolvedSlim, showBase, info.hasCape()
            );
        }

        if (info.isEmpty() && PasConfig.get().defaultSkin.isEmpty()) {
            setOriginalAngles(showBase, showArms, bodyPose);
        }
    }

    public void setupVisibilityForItem(PasModelPoseSettings state, NameInfo info) {
        SkinData skinData = PasManager.getInstance().getSkinData(info);
        boolean slim = SkinData.resolveSlim(info, skinData);

        this.hat.visible = state.head.mode.showPlayerPart(info);
        this.head.visible = state.head.mode.showPlayerPart(info);
        this.body.visible = state.body.mode.showPlayerPart(info);
        this.jacket.visible = state.body.mode.showPlayerPart(info);

        this.leftArm.visible = state.leftArm.mode.showPlayerPart(info) && !slim;
        this.rightArm.visible = state.rightArm.mode.showPlayerPart(info) && !slim;
        this.leftSleeve.visible = state.leftArm.mode.showPlayerPart(info) && !slim;
        this.rightSleeve.visible = state.rightArm.mode.showPlayerPart(info) && !slim;

        this.leftPants.visible = state.leftLeg.mode.showPlayerPart(info);
        this.rightPants.visible = state.rightLeg.mode.showPlayerPart(info);
        this.leftLeg.visible = state.leftLeg.mode.showPlayerPart(info);
        this.rightLeg.visible = state.rightLeg.mode.showPlayerPart(info);

        this.leftSlimArm.visible = state.leftArm.mode.showPlayerPart(info) && slim;
        this.rightSlimArm.visible = state.rightArm.mode.showPlayerPart(info) && slim;
        this.leftSlimSleeve.visible = state.leftArm.mode.showPlayerPart(info) && slim;
        this.rightSlimSleeve.visible = state.rightArm.mode.showPlayerPart(info) && slim;

        this.originalHead.visible = state.head.mode.showOriginalPart(info);
        this.originalBody.visible = state.body.mode.showOriginalPart(info);
        this.originalRightArm.visible = state.rightArm.mode.showOriginalPart(info);
        this.originalLeftArm.visible = state.leftArm.mode.showOriginalPart(info);
        this.originalRightLeg.visible = state.rightLeg.mode.showOriginalPart(info);
        this.originalLeftLeg.visible = state.leftLeg.mode.showOriginalPart(info);
        this.rightBodyStick.visible = state.body.mode.showOriginalPart(info);
        this.leftBodyStick.visible = state.body.mode.showOriginalPart(info);
        this.shoulderStick.visible = state.body.mode.showOriginalPart(info);
        this.cloak.visible = state.cloak.mode.showCapePart(info);
        this.basePlate.visible = state.baseplate;
    }

    public void setupModel(PasModelPoseSettings settings, NameInfo info) {
        setupVisibilityForItem(settings, info);
        super.setupAnim(settings.toRenderState(info));
        copyPlayerPartsFromStand();
        setupCape();
    }

    public void setupCape() {
        this.cloak.xRot = (float) Math.toRadians(-10);
        this.cloak.yRot = (float) Math.toRadians(180);
        this.cloak.y = 0.02f;
        this.cloak.z = 1.1f;
    }

    private void copyPlayerPartsFromStand() {
        cpp(leftLeg, this.leftPants);
        cpp(rightLeg, this.rightPants);
        cpp(leftArm, this.leftSleeve);
        cpp(rightArm, this.rightSleeve);
        cpp(leftArm, this.leftSlimArm);
        cpp(rightArm, this.rightSlimArm);
        cpp(leftArm, this.leftSlimSleeve);
        cpp(rightArm, this.rightSlimSleeve);
        cpp(body, this.originalBody);
        cpp(head, this.originalHead);
        cpp(rightArm, this.originalRightArm);
        cpp(leftArm, this.originalLeftArm);
        cpp(rightLeg, this.originalRightLeg);
        cpp(leftLeg, this.originalLeftLeg);
        cpp(body, this.jacket);
    }

    public void setModelVisibility(boolean player, boolean slim, boolean showBase, boolean showCape) {
        this.hat.visible = player;
        this.head.visible = player;
        this.body.visible = player;
        this.jacket.visible = player;
        this.leftArm.visible = player && !slim;
        this.rightArm.visible = player && !slim;
        this.leftSleeve.visible = player && !slim;
        this.rightSleeve.visible = player && !slim;
        this.leftPants.visible = player;
        this.rightPants.visible = player;
        this.leftLeg.visible = player;
        this.rightLeg.visible = player;
        this.leftSlimArm.visible = player && slim;
        this.rightSlimArm.visible = player && slim;
        this.leftSlimSleeve.visible = player && slim;
        this.rightSlimSleeve.visible = player && slim;
        this.originalHead.visible = !player;
        this.originalBody.visible = !player;
        this.originalRightArm.visible = !player;
        this.originalLeftArm.visible = !player;
        this.originalRightLeg.visible = !player;
        this.originalLeftLeg.visible = !player;
        this.rightBodyStick.visible = !player;
        this.leftBodyStick.visible = !player;
        this.shoulderStick.visible = !player;
        this.basePlate.visible = showBase;
        this.cloak.visible = showCape;
    }

    private void setOriginalAngles(boolean showBase, boolean showArms, Rotations bodyPose) {
        this.setModelVisibility(false, false, showBase, false);
        this.originalLeftArm.visible = showArms;
        this.originalRightArm.visible = showArms;
        this.rightBodyStick.xRot = ((float) Math.PI / 180F) * bodyPose.x();
        this.rightBodyStick.yRot = ((float) Math.PI / 180F) * bodyPose.y();
        this.rightBodyStick.zRot = ((float) Math.PI / 180F) * bodyPose.z();
        this.leftBodyStick.xRot = ((float) Math.PI / 180F) * bodyPose.x();
        this.leftBodyStick.yRot = ((float) Math.PI / 180F) * bodyPose.y();
        this.leftBodyStick.zRot = ((float) Math.PI / 180F) * bodyPose.z();
        this.shoulderStick.xRot = ((float) Math.PI / 180F) * bodyPose.x();
        this.shoulderStick.yRot = ((float) Math.PI / 180F) * bodyPose.y();
        this.shoulderStick.zRot = ((float) Math.PI / 180F) * bodyPose.z();
    }

    public ModelPart getCape() { return this.cloak; }

    public Collection<ModelPart> getOriginalParts() { return originalParts; }
    public Collection<ModelPart> getPlayerParts()   { return playerParts; }

    public static boolean showArmorStandWhileDownload(Texture data) {
        if (data == null) return true;
        boolean isLoading = data.getStatus() != DownloadStatus.COMPLETED;
        return PasConfig.get().showArmorStandWhileDownloading && isLoading;
    }

    private static void cpp(ModelPart from, ModelPart to) {
        ModUtils.copyPartPose(from, to);
    }
}
