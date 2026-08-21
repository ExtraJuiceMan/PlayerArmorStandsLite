package com.danrus.pas.render.common;

public record PasModelSettings(
    PasModelPoseSettings poseSettings,
    boolean foil,
    boolean isSmall
) {
    public static final PasModelSettings DEFAULT = new PasModelSettings(new PasModelPoseSettings(), false, false);
}
