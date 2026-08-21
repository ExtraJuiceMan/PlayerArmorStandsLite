package com.danrus.pas.data.skin;

import com.danrus.pas.ModExecutor;
import com.danrus.pas.api.DownloadStatus;
import com.danrus.pas.api.NameInfo;
import com.danrus.pas.utils.Id;
import com.danrus.pas.utils.ModUtils;
import com.danrus.pas.utils.TextureUtils;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class FileSkinProvider {
    public static final Path SKINS_PATH = ModUtils.getGameDir().resolve("pas/skins");
    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_]+");

    public SkinData tryLoad(NameInfo info) {
        if (!info.skinProvider().equals("F")) {
            return null;
        }

        if (!isValidName(info.base())) {
            SkinData invalid = new SkinData();
            invalid.setStatus(DownloadStatus.INVALID);
            return invalid;
        }

        SkinData data = new SkinData();
        data.setStatus(DownloadStatus.IN_PROGRESS);

        Path filePath = SKINS_PATH.resolve(info.base() + ".png");

        CompletableFuture
                .supplyAsync(() -> Files.exists(filePath), ModExecutor.DOWNLOAD_EXECUTOR)
                .thenAccept(exists -> {
                    if (!exists) {
                        data.setStatus(DownloadStatus.FAILED);
                        return;
                    }

                    Identifier texture = Id.pas("skins/file_" + info.base());

                    TextureUtils.registerTexture(filePath, texture, true).whenComplete((id, error) -> {
                        if (error != null) {
                            data.setStatus(DownloadStatus.FAILED);
                        } else {
                            data.setTexture(id);
                            data.setStatus(DownloadStatus.COMPLETED);
                        }
                    });
                })
                .exceptionally(error -> {
                    data.setStatus(DownloadStatus.FAILED);
                    return null;
                });

        return data;
    }

    private static boolean isValidName(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= 16
                && VALID_NAME.matcher(name).matches();
    }
}