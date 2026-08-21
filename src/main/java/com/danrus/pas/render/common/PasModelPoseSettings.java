package com.danrus.pas.render.common;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.render.armorstand.PasEntityRenderState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3f;

public class PasModelPoseSettings {
    public static PasModelPartSettings DEFAULT_HEAD = new PasModelPartSettings(new Vector3f(0, 0, 0), PasModelPartSettings.Mode.DYNAMIC);
    public static PasModelPartSettings DEFAULT_BODY = new PasModelPartSettings(new Vector3f(0, 0, 0), PasModelPartSettings.Mode.DYNAMIC);
    public static PasModelPartSettings DEFAULT_LEFT_LEG = new PasModelPartSettings(new Vector3f(-1, 0, -1), PasModelPartSettings.Mode.DYNAMIC);
    public static PasModelPartSettings DEFAULT_RIGHT_LEG = new PasModelPartSettings(new Vector3f(1, 0, 1), PasModelPartSettings.Mode.DYNAMIC);
    public static PasModelPartSettings DEFAULT_LEFT_ARM = new PasModelPartSettings(new Vector3f(-10, 0, -10), PasModelPartSettings.Mode.DYNAMIC);
    public static PasModelPartSettings DEFAULT_RIGHT_ARM = new PasModelPartSettings(new Vector3f(-15, 0, 10), PasModelPartSettings.Mode.DYNAMIC);
    public static PasModelPartSettings DEFAULT_CLOAK_ITEM = new PasModelPartSettings(new Vector3f(0, 0, -15), PasModelPartSettings.Mode.INVISIBLE);
    public static PasModelPartSettings DEFAULT_CLOAK_TO_RS = new PasModelPartSettings(new Vector3f(0, 0, -15), PasModelPartSettings.Mode.DYNAMIC);

    public static Codec<PasModelPoseSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PasModelPartSettings.CODEC.optionalFieldOf("head", DEFAULT_HEAD).forGetter(state -> state.head),
        PasModelPartSettings.CODEC.optionalFieldOf("body", DEFAULT_BODY).forGetter(state -> state.body),
        PasModelPartSettings.CODEC.optionalFieldOf("left_leg", DEFAULT_LEFT_LEG).forGetter(state -> state.leftLeg),
        PasModelPartSettings.CODEC.optionalFieldOf("right_leg", DEFAULT_RIGHT_LEG).forGetter(state -> state.rightLeg),
        PasModelPartSettings.CODEC.optionalFieldOf("left_arm", DEFAULT_LEFT_ARM).forGetter(state -> state.leftArm),
        PasModelPartSettings.CODEC.optionalFieldOf("right_arm", DEFAULT_RIGHT_ARM).forGetter(state -> state.rightArm),
        PasModelPartSettings.CODEC.optionalFieldOf("cape", DEFAULT_CLOAK_ITEM).forGetter(state -> state.cloak),
        Codec.BOOL.optionalFieldOf("baseplate", true).forGetter(state -> state.baseplate)
    ).apply(instance, PasModelPoseSettings::new));

    public PasModelPartSettings head, body, leftLeg, rightLeg, leftArm, rightArm, cloak;
    public boolean baseplate;

    public PasModelPoseSettings() {
        this.head = DEFAULT_HEAD;
        this.body = DEFAULT_BODY;
        this.leftLeg = DEFAULT_LEFT_LEG;
        this.rightLeg = DEFAULT_RIGHT_LEG;
        this.leftArm = DEFAULT_LEFT_ARM;
        this.rightArm = DEFAULT_RIGHT_ARM;
        this.cloak = DEFAULT_CLOAK_ITEM;
        this.baseplate = true;
    }

    public PasModelPoseSettings(
            PasModelPartSettings head, PasModelPartSettings body,
            PasModelPartSettings leftLeg, PasModelPartSettings rightLeg,
            PasModelPartSettings leftArm, PasModelPartSettings rightArm,
            PasModelPartSettings cloak, boolean baseplate) {
        this.head = head; this.body = body;
        this.leftLeg = leftLeg; this.rightLeg = rightLeg;
        this.leftArm = leftArm; this.rightArm = rightArm;
        this.cloak = cloak; this.baseplate = baseplate;
    }

    public PasModelPoseSettings(PasEntityRenderState original) {
        this.head = new PasModelPartSettings(original.headPose, PasModelPartSettings.Mode.DYNAMIC);
        this.body = new PasModelPartSettings(original.bodyPose, PasModelPartSettings.Mode.DYNAMIC);
        this.leftLeg = new PasModelPartSettings(original.leftLegPose, PasModelPartSettings.Mode.DYNAMIC);
        this.rightLeg = new PasModelPartSettings(original.rightLegPose, PasModelPartSettings.Mode.DYNAMIC);
        this.leftArm = new PasModelPartSettings(original.leftArmPose, PasModelPartSettings.Mode.DYNAMIC);
        this.rightArm = new PasModelPartSettings(original.rightArmPose, PasModelPartSettings.Mode.DYNAMIC);
        this.cloak = DEFAULT_CLOAK_TO_RS;
        this.baseplate = original.showBasePlate;
    }

    public PasEntityRenderState toRenderState(NameInfo info) {
        PasEntityRenderState state = new PasEntityRenderState();
        state.leftArmPose = this.leftArm.toRotations();
        state.rightArmPose = this.rightArm.toRotations();
        state.leftLegPose = this.leftLeg.toRotations();
        state.rightLegPose = this.rightLeg.toRotations();
        state.bodyPose = this.body.toRotations();
        state.headPose = this.head.toRotations();
        state.showBasePlate = this.baseplate;
        state.info = info;
        return state;
    }
}
