package com.danrus.pas.managers;

import com.danrus.pas.config.PasConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class OverlayMessageManager {
    private static final OverlayMessageManager INSTANCE = new OverlayMessageManager();

    public void showDownloadMessage(String name) { showMessage("pas.downloading", name, ChatFormatting.BLUE); }
    public void showInvalidNameMessage(String name) { showMessage("pas.invalid_username", name, ChatFormatting.RED); }
    public void showSuccessMessage(String name) { showMessage("pas.download_success", name, ChatFormatting.GREEN); }
    public void showFailMessage(String name) { showMessage("pas.download_failed", name, ChatFormatting.RED); }

    private void showMessage(String key, String name, ChatFormatting color) {
        if (name.isEmpty()) return;
        PasConfig.DownloadStatusDisplay display = PasConfig.get().downloadStatusDisplay;
        if (display == PasConfig.DownloadStatusDisplay.NONE) return;
        Minecraft.getInstance().execute(() -> {
            Component msg = Component.translatable(key, name).withStyle(color);
            Minecraft.getInstance().gui.chatListener().handleSystemMessage(msg, false);
        });
    }

    public static OverlayMessageManager getInstance() { return INSTANCE; }
}
