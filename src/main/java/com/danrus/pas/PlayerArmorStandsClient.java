package com.danrus.pas;

import com.danrus.pas.config.PasConfig;
import com.danrus.pas.managers.PasManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerArmorStandsClient {
    public static final Logger LOGGER = LoggerFactory.getLogger("PlayerArmorStands");
    public static final String MOD_ID = "pas";
    public static final String[] RPS = new String[]{"3d_items_with_head", "3d_items_player_like"};
    public static final String DEFAULT_RP = RPS[0];

    public static void initialize() {
        PasConfig.init();
        ModExecutor.init();
        PasManager.getInstance();
    }
}
