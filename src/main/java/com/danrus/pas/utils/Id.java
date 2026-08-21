package com.danrus.pas.utils;

import com.danrus.pas.PlayerArmorStandsClient;
import net.minecraft.resources.Identifier;

public class Id {
    public static Identifier pas(String path) {
        return Identifier.fromNamespaceAndPath(PlayerArmorStandsClient.MOD_ID, path);
    }
    public static Identifier vanilla(String path) {
        return Identifier.withDefaultNamespace(path);
    }
}
