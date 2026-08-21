package com.danrus.pas.loaders.fabric;

import com.danrus.pas.PlayerArmorStandsClient;
import com.danrus.pas.commands.PasCommandsRegistrar;
import com.danrus.pas.utils.Id;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerArmorStandsClient.initialize();
        FabricLoader.getInstance().getModContainer(PlayerArmorStandsClient.MOD_ID).ifPresent(container -> {
            for (String packName : PlayerArmorStandsClient.RPS) {
                ResourceLoader.registerBuiltinPack(
                    Id.pas(packName), container,
                    Component.translatable("pas.rp." + packName),
                    packName.equals(PlayerArmorStandsClient.DEFAULT_RP)
                        ? PackActivationType.DEFAULT_ENABLED
                        : PackActivationType.NORMAL
                );
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            new PasCommandsRegistrar<FabricClientCommandSource>().register(dispatcher)
        );
    }
}
