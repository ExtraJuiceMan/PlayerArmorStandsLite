package com.danrus.pas.commands;

import com.danrus.pas.managers.PasManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PasCommandsRegistrar<S> {
    public static final List<String> COMMANDS_NAMES = List.of("player-armor-stands", "pas");

    public void register(CommandDispatcher<S> dispatcher) {
        for (String name : COMMANDS_NAMES) {
            dispatcher.register(literal(name).executes(ctx -> {
                Minecraft.getInstance().gui.chatListener()
                    .handleSystemMessage(Component.translatable("commands.pas.default_feedback"), false);
                return 1;
            })
            .then(literal("reload")
                .then(literal("all").executes(ctx -> {
                    PasManager.getInstance().reloadAll();
                    return 1;
                }))
                .then(literal("failed").executes(ctx -> {
                    PasManager.getInstance().reloadFailed();
                    return 1;
                }))
                .then(literal("skin")
                    .then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                        PasManager.getInstance().reloadSkin(StringArgumentType.getString(ctx, "name"));
                        return 1;
                    })))
                .then(literal("cape")
                    .then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                        PasManager.getInstance().reloadCape(StringArgumentType.getString(ctx, "name"));
                        return 1;
                    }))))
            .then(literal("debug")
                .then(literal("drop_cache").executes(ctx -> {
                    PasManager.getInstance().dropCache();
                    return 1;
                }))));
        }
    }

    private LiteralArgumentBuilder<S> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private <T> com.mojang.brigadier.builder.RequiredArgumentBuilder<S, T> argument(String name, com.mojang.brigadier.arguments.ArgumentType<T> type) {
        return com.mojang.brigadier.builder.RequiredArgumentBuilder.argument(name, type);
    }
}
