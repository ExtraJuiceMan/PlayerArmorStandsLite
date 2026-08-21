package com.danrus.pas.config;

import com.danrus.pas.utils.ModUtils;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PasConfig {
    public static final ConfigClassHandler<PasConfig> HANDLER = ConfigClassHandler.createBuilder(PasConfig.class)
        .serializer(config -> GsonConfigSerializerBuilder.create(config)
            .setPath(YACLPlatform.getConfigDir().resolve("pas.json5"))
            .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
            .setJson5(true)
            .build())
        .build();

    private static final PasConfig FALLBACK = new PasConfig();

    @SerialEntry public boolean enableMod = true;
    @SerialEntry public int downloadThreads = 3;
    @SerialEntry public DownloadStatusDisplay downloadStatusDisplay = DownloadStatusDisplay.NONE;
    @SerialEntry public boolean hideParamsOnLabel = true;
    @SerialEntry public String defaultSkin = "";
    @SerialEntry public boolean showArmorStandWhileDownloading = true;
    @SerialEntry public boolean showEasterEggs = true;
    @SerialEntry public boolean tryApplyFromServerPlayer = true;
    @SerialEntry public SkinReloadTime skinReloadTime = SkinReloadTime.DAY_1;

    public enum DownloadStatusDisplay { NONE, CHAT }
    public enum SkinReloadTime { HOUR_12, DAY_1, DAY_3, DAY_7, NEVER }

    public static PasConfig get() {
        return isYaclLoaded() ? HANDLER.instance() : FALLBACK;
    }

    public static void init() {
        if (isYaclLoaded()) HANDLER.load();
    }

    public static void save() {
        if (isYaclLoaded()) HANDLER.save();
    }

    public static Screen getConfigScreen(Screen parent) {
        if (!isYaclLoaded()) return null;
        PasConfig c = get();
        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Player Armor Stands"))
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("pas.config.group.general"))
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("pas.config.group.general"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("pas.config.enable_mod"))
                        .binding(true, () -> c.enableMod, v -> c.enableMod = v)
                        .controller(TickBoxControllerBuilder::create).build())
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("pas.config.group.armorstands"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("pas.config.hide_params_on_label"))
                        .binding(true, () -> c.hideParamsOnLabel, v -> c.hideParamsOnLabel = v)
                        .controller(TickBoxControllerBuilder::create).build())
                    .option(Option.<String>createBuilder()
                        .name(Component.translatable("pas.config.default_skin"))
                        .binding("", () -> c.defaultSkin, v -> c.defaultSkin = v)
                        .controller(StringControllerBuilder::create).build())
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("pas.config.group.skin_loading"))
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("pas.config.download_threads"))
                        .binding(3, () -> c.downloadThreads, v -> c.downloadThreads = v)
                        .controller(IntegerFieldControllerBuilder::create)
                        .addListener((opt, ev) -> com.danrus.pas.ModExecutor.reload())
                        .build())
                    .option(Option.<DownloadStatusDisplay>createBuilder()
                        .name(Component.translatable("pas.config.download_status_display"))
                        .binding(DownloadStatusDisplay.NONE, () -> c.downloadStatusDisplay, v -> c.downloadStatusDisplay = v)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(DownloadStatusDisplay.class)
                            .formatValue(v -> Component.translatable("pas.config.download_status_display." + v.name().toLowerCase())))
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("pas.config.try_apply_from_server_player"))
                        .binding(true, () -> c.tryApplyFromServerPlayer, v -> c.tryApplyFromServerPlayer = v)
                        .controller(TickBoxControllerBuilder::create).build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("pas.config.show_armor_stand_while_downloading"))
                        .binding(true, () -> c.showArmorStandWhileDownloading, v -> c.showArmorStandWhileDownloading = v)
                        .controller(TickBoxControllerBuilder::create).build())
                    .option(Option.<SkinReloadTime>createBuilder()
                        .name(Component.translatable("pas.config.reload_time"))
                        .binding(SkinReloadTime.DAY_1, () -> c.skinReloadTime, v -> c.skinReloadTime = v)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(SkinReloadTime.class)
                            .formatValue(v -> Component.translatable("pas.config.reload_time." + v.name().toLowerCase())))
                        .build())
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("pas.config.group.secret_settings"))
                    .collapsed(true)
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("pas.config.show_easter_eggs"))
                        .binding(true, () -> c.showEasterEggs, v -> c.showEasterEggs = v)
                        .controller(TickBoxControllerBuilder::create).build())
                    .build())
                .build())
            .save(PasConfig::save)
            .build()
            .generateScreen(parent);
    }

    public static long millisFromSkinReloadTime(SkinReloadTime time) {
        return switch (time) {
            case NEVER   -> Long.MAX_VALUE;
            case HOUR_12 -> 12L * 60 * 60 * 1000;
            case DAY_1   -> 24L * 60 * 60 * 1000;
            case DAY_3   -> 3L  * 24 * 60 * 60 * 1000;
            case DAY_7   -> 7L  * 24 * 60 * 60 * 1000;
        };
    }

    private static boolean isYaclLoaded() {
        return ModUtils.isModLoaded("yet_another_config_lib_v3");
    }
}
