package com.danrus.pas.data;

import com.danrus.pas.api.DownloadStatus;
import net.minecraft.resources.Identifier;
import java.util.concurrent.atomic.AtomicReference;

public class Texture {
    private volatile Identifier location;
    private final AtomicReference<DownloadStatus> status =
            new AtomicReference<>(DownloadStatus.NOT_STARTED);

    public Identifier getTexture(Identifier fallback) {
        return location != null ? location : fallback;
    }

    public void setTexture(Identifier location) {
        this.location = location;
    }

    public DownloadStatus getStatus() {
        return status.get();
    }

    public void setStatus(DownloadStatus status) {
        this.status.set(status);
    }

    public boolean compareAndSetStatus(DownloadStatus expected, DownloadStatus update) {
        return this.status.compareAndSet(expected, update);
    }
}