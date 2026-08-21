package com.danrus.pas.render.common;

import com.danrus.pas.api.NameInfo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Rotations;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class PasModelPartSettings {
    public static Codec<PasModelPartSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(part -> part.rotation),
        Codec.STRING.optionalFieldOf("mode", "original").forGetter(part -> part.mode.name())
    ).apply(instance, (rotation, mode) -> new PasModelPartSettings(rotation, Mode.valueOf(mode.toUpperCase()))));

    public Vector3fc rotation;
    public Mode mode;

    public PasModelPartSettings(Vector3fc rotation, Mode mode) {
        this.rotation = rotation;
        this.mode = mode;
    }

    public PasModelPartSettings(Rotations rotations, Mode mode) {
        this(new Vector3f(rotations.x(), rotations.y(), rotations.z()), mode);
    }

    public Rotations toRotations() {
        return new Rotations(rotation.x(), rotation.y(), rotation.z());
    }

    public enum Mode {
        ORIGINAL, INVISIBLE, DYNAMIC, PLAYER;

        public boolean showPlayerPart(NameInfo info) {
            return this == PLAYER || this == DYNAMIC && !info.isEmpty();
        }
        public boolean showCapePart(NameInfo info) {
            return this == PLAYER || this == DYNAMIC && info.hasCape();
        }
        public boolean showOriginalPart(NameInfo info) {
            return this == ORIGINAL || this == DYNAMIC && info.isEmpty();
        }
    }
}
