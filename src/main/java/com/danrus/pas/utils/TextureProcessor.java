package com.danrus.pas.utils;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TextureProcessor {
    private TextureProcessor() {}

    public static boolean isSlimSkin(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width != 64 || (height != 32 && height != 64)) return false;

        int emptyPixels = 0;
        int totalPixels = 0;

        for (int y = 20; y < 32; y++) {
            for (int x = 53; x < 56; x++) {
                totalPixels++;
                int alpha = (image.getPixel(x, y) >> 24) & 255;
                if (alpha == 0) {
                    emptyPixels++;
                }
            }
        }

        return emptyPixels > (totalPixels * 0.8);
    }

    public static boolean detectSlimFromFile(Path path) {
        try (NativeImage img = NativeImage.read(Files.newInputStream(path))) {
            return isSlimSkin(img);
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static NativeImage remapLegacySkin(NativeImage image) {
        int height = image.getHeight();
        int width = image.getWidth();
        if (width == 64 && (height == 32 || height == 64)) {
            boolean needRemap = height == 32;
            if (needRemap) {
                NativeImage nativeImage = new NativeImage(64, 64, true);
                nativeImage.copyFrom(image);
                image.close();
                image = nativeImage;
                nativeImage.fillRect(0, 32, 64, 32, 0);
                nativeImage.copyRect(4, 16, 16, 32, 4, 4, true, false);
                nativeImage.copyRect(8, 16, 16, 32, 4, 4, true, false);
                nativeImage.copyRect(0, 20, 24, 32, 4, 12, true, false);
                nativeImage.copyRect(4, 20, 16, 32, 4, 12, true, false);
                nativeImage.copyRect(8, 20, 8, 32, 4, 12, true, false);
                nativeImage.copyRect(12, 20, 16, 32, 4, 12, true, false);
                nativeImage.copyRect(44, 16, -8, 32, 4, 4, true, false);
                nativeImage.copyRect(48, 16, -8, 32, 4, 4, true, false);
                nativeImage.copyRect(40, 20, 0, 32, 4, 12, true, false);
                nativeImage.copyRect(44, 20, -8, 32, 4, 12, true, false);
                nativeImage.copyRect(48, 20, -16, 32, 4, 12, true, false);
                nativeImage.copyRect(52, 20, -8, 32, 4, 12, true, false);
            }
            stripAlpha(image, 0, 0, 32, 16);
            if (needRemap) {
                stripColor(image, 32, 0, 64, 32);
            }
            stripAlpha(image, 0, 16, 64, 32);
            stripAlpha(image, 16, 48, 48, 64);
            return image;
        } else {
            image.close();
            return null;
        }
    }

    public static NativeImage applyMaterial(NativeImage source, NativeImage material, float blendStrength) {
        if (material.getWidth() <= 0 || material.getHeight() <= 0) {
            throw new IllegalArgumentException("Material texture must not be empty");
        }
        float blend = Math.max(0.0F, Math.min(1.0F, blendStrength));
        int width = source.getWidth();
        int height = source.getHeight();
        NativeImage result = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceArgb = source.getPixel(x, y);
                int sourceAlpha = alpha(sourceArgb);
                if (sourceAlpha == 0) { result.setPixel(x, y, 0); continue; }
                int sourceRed = red(sourceArgb);
                int sourceGreen = green(sourceArgb);
                int sourceBlue = blue(sourceArgb);
                int materialArgb = material.getPixel(x % material.getWidth(), y % material.getHeight());
                float materialAlpha = alpha(materialArgb) / 255.0F;
                float effectiveBlend = blend * materialAlpha;
                float luminance = (0.2126F * sourceRed + 0.7152F * sourceGreen + 0.0722F * sourceBlue) / 255.0F;
                int shadedRed = Math.round(red(materialArgb) * luminance);
                int shadedGreen = Math.round(green(materialArgb) * luminance);
                int shadedBlue = Math.round(blue(materialArgb) * luminance);
                int resultRed = lerp(sourceRed, shadedRed, effectiveBlend);
                int resultGreen = lerp(sourceGreen, shadedGreen, effectiveBlend);
                int resultBlue = lerp(sourceBlue, shadedBlue, effectiveBlend);
                result.setPixel(x, y, packArgb(sourceAlpha, resultRed, resultGreen, resultBlue));
            }
        }
        return result;
    }

    private static int lerp(int from, int to, float amount) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * amount)));
    }
    private static int alpha(int argb) { return argb >>> 24 & 0xFF; }
    private static int red(int argb) { return argb >>> 16 & 0xFF; }
    private static int green(int argb) { return argb >>> 8 & 0xFF; }
    private static int blue(int argb) { return argb & 0xFF; }
    private static int packArgb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int i = x1; i < x2; ++i) {
            for (int j = y1; j < y2; ++j) {
                int k = image.getPixel(i, j);
                if ((k >> 24 & 255) < 128) return;
            }
        }
        for (int i = x1; i < x2; ++i) {
            for (int j = y1; j < y2; ++j) {
                image.setPixel(i, j, image.getPixel(i, j) & 16777215);
            }
        }
    }

    private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int i = x1; i < x2; ++i) {
            for (int j = y1; j < y2; ++j) {
                image.setPixel(i, j, image.getPixel(i, j) | -16777216);
            }
        }
    }
}
