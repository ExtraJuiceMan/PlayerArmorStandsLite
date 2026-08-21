package com.danrus.pas.render.armorstand;

import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.config.PasConfig;
import com.danrus.pas.data.skin.SkinData;
import com.danrus.pas.duck.DrawSwapper;
import com.danrus.pas.managers.PasManager;
import com.danrus.pas.render.common.PasModelPoseSettings;
import com.danrus.pas.render.common.PasModelSettings;
import com.danrus.pas.render.common.PasRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.WeakHashMap;

public class PasEntityRenderer extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
    private final ArmorStandRenderer armorStandRenderer;
    private final PasRenderer pasRenderer;

    public PasEntityRenderer(EntityRendererProvider.Context context) {
        super(context, DummyEntityModel.INSTANTS, .0f);

        this.armorStandRenderer = new ArmorStandRenderer(context);

        // This renderer is now mostly a fallback/helper renderer.
        // Actual entity rendering uses per-entity models from PasModelCache.
        this.pasRenderer = new PasRenderer(
                new PlayerArmorStandModel(PlayerArmorStandModel.createBodyLayer(CubeDeformation.NONE).bakeRoot()),
                new PlayerArmorStandModel(PlayerArmorStandModel.createBodyLayer(CubeDeformation.NONE)
                        .apply(HumanoidModel.BABY_TRANSFORMER).bakeRoot()),
                false
        );

        this.addLayer(new HumanoidArmorLayer(this,
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
                context.getEquipmentRenderer()));

        this.addLayer(new ItemInHandLayer(this));
        this.addLayer(new WingsLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new CustomHeadLayer(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public @NotNull PasEntityRenderState createRenderState() {
        return new PasEntityRenderState();
    }

    @Override
    public void extractRenderState(ArmorStand entity, ArmorStandRenderState vanillaState, float partialTick) {
        super.extractRenderState(entity, vanillaState, partialTick);
        armorStandRenderer.extractRenderState(entity, vanillaState, partialTick);

        if (vanillaState instanceof PasEntityRenderState state) {
            state.info = NameInfo.parse(entity.getCustomName());

            if (PasConfig.get().enableMod && (!state.info.isEmpty() || state.info.meme() != null)) {
                state.ownModel = PasModelCache.get(entity, vanillaState.isSmall);
            } else {
                state.ownModel = null;
            }
        }
    }

    @Override
    public void submit(ArmorStandRenderState vanillaState, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (!(vanillaState instanceof PasEntityRenderState state)) {
            submitVanilla(vanillaState, poseStack, collector, cameraRenderState);
            return;
        }

        if (!PasConfig.get().enableMod) {
            submitVanilla(vanillaState, poseStack, collector, cameraRenderState);
            return;
        }

        SkinData data = PasManager.getInstance().getSkinData(state.info);

        if (data == null || (data.getStatus() != DownloadStatus.COMPLETED && state.info.meme() == null)) {
            submitVanilla(vanillaState, poseStack, collector, cameraRenderState);
            return;
        }

        PlayerArmorStandModel model = state.ownModel;

        // Should not normally happen if extractRenderState ran, but keep it safe.
        if (model == null) {
            submitVanilla(vanillaState, poseStack, collector, cameraRenderState);
            return;
        }

        if (state.info.meme() != null) {
            state.bodyRot = 0;
            state.yRot = 0;

            swapVanillaDraw(() -> {
                Quaternionf rotation = calculateOrientation(new Quaternionf(entityRenderDispatcher.camera.rotation()));

                poseStack.pushPose();
                poseStack.mulPose(rotation);
                poseStack.translate(0, -1, 0);

                executeSubmit(model, data, state, collector, poseStack, state.lightCoords);

                poseStack.popPose();
            });
        } else {
            swapVanillaDraw(() -> executeSubmit(model, data, state, collector, poseStack, state.lightCoords));
        }

        // Important: use this armor stand's own model, not a shared one.
        this.model = model;

        super.submit(state, poseStack, collector, cameraRenderState);
    }

    @Override
    public boolean isEntityUpsideDown(ArmorStand entity) {
        NameInfo info = NameInfo.parse(entity.getCustomName());

        if (info.shouldUpsideDown()
                && !PlayerArmorStandModel.showArmorStandWhileDownload(PasManager.getInstance().getSkinData(info))) {
            return true;
        }

        return super.isEntityUpsideDown(entity);
    }

    private void submitVanilla(ArmorStandRenderState state, PoseStack ps,
                               SubmitNodeCollector col, CameraRenderState cam) {
        this.model = DummyEntityModel.INSTANTS;
        armorStandRenderer.submit(state, ps, col, cam);
    }

    @Override
    public @NotNull Identifier getTextureLocation(ArmorStandRenderState rs) {
        return armorStandRenderer.getTextureLocation(rs);
    }

    private void swapVanillaDraw(Runnable draw) {
        ((DrawSwapper) this).pas$swapDrawer(draw);
    }

    private void executeSubmit(PlayerArmorStandModel model, SkinData data, PasEntityRenderState state,
                               SubmitNodeCollector collector, PoseStack ps, int light) {
        if (!state.isInvisible && !state.isInvisibleToPlayer) {
            pasRenderer.submit(
                    model,
                    data,
                    PasManager.getInstance().getCapeData(state.info),
                    state.info,
                    collector,
                    new PasModelSettings(new PasModelPoseSettings(state), false, state.isBaby),
                    ps,
                    light,
                    OverlayTexture.NO_OVERLAY
            );
        }
    }

    private Quaternionf calculateOrientation(Quaternionf q) {
        Camera cam = entityRenderDispatcher.camera;

        return q.rotationYXZ(
                -0.017453292F * -(cam.yRot() - 180.0F),
                ((float) Math.PI / 180F) * (-cam.xRot()),
                0.0F
        );
    }

    /**
     * Cache of per-entity armor stand models.
     *
     * This prevents multiple armor stands from sharing the same mutable ModelParts.
     */
    private static final class PasModelCache {
        private static final WeakHashMap<ArmorStand, Models> CACHE = new WeakHashMap<>();

        static PlayerArmorStandModel get(ArmorStand entity, boolean small) {
            Models models = CACHE.get(entity);

            if (models == null) {
                models = new Models();
                CACHE.put(entity, models);
            }

            if (small) {
                if (models.small == null) {
                    models.small = new PlayerArmorStandModel(
                            PlayerArmorStandModel.createBodyLayer(CubeDeformation.NONE)
                                    .apply(HumanoidModel.BABY_TRANSFORMER)
                                    .bakeRoot()
                    );
                }

                return models.small;
            } else {
                if (models.normal == null) {
                    models.normal = new PlayerArmorStandModel(
                            PlayerArmorStandModel.createBodyLayer(CubeDeformation.NONE).bakeRoot()
                    );
                }

                return models.normal;
            }
        }

        private static final class Models {
            private PlayerArmorStandModel normal;
            private PlayerArmorStandModel small;
        }
    }
}