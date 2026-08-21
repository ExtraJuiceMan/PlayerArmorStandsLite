package com.danrus.pas.impl.providers.common;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.api.TextureProvider;
import com.danrus.pas.data.Texture;
import com.danrus.pas.managers.PasManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTextureProviderManager<T extends Texture> {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private PasManager pasManager;

    private final Map<String, List<PrioritizedProvider>> providers = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<T>> pendingDownloads = new ConcurrentHashMap<>();

    public void clearPending() { pendingDownloads.clear(); }

    public void initialize(PasManager manager) {
        if (initialized.compareAndSet(false, true)) {
            this.pasManager = manager;
            prepareProviders();
        }
    }

    protected abstract String getTextureType();
    protected abstract String getDownloadKey(NameInfo info);

    public void addProvider(TextureProvider provider) { addProvider(provider, 0); }

    public void addProvider(TextureProvider provider, int priority) {
        providers.computeIfAbsent(provider.getLiteral(), k -> new ArrayList<>())
                .add(new PrioritizedProvider(provider, priority));
        providers.get(provider.getLiteral())
                .sort(Comparator.comparingInt(PrioritizedProvider::priority).reversed());
    }

    public void download(NameInfo info) {
        if (info.base().isEmpty()) return;
        String key = getDownloadKey(info);
        CompletableFuture<T> created = new CompletableFuture<>();
        CompletableFuture<T> shared = pendingDownloads.putIfAbsent(key, created);
        if (shared == null) {
            shared = created;
            created.whenComplete((d, t) -> pendingDownloads.remove(key, created));
            try {
                startDownload(info, created);
            } catch (Exception e) {
                created.completeExceptionally(e);
            }
        }
        shared.whenComplete((data, throwable) -> {
            if (throwable != null) {
                Throwable cause = unwrap(throwable);
                LOGGER.warn("Failed to download {} for {}: {}",
                        getTextureType(), info, cause.getMessage());
                invalidate(info);
            } else if (data != null) {
                store(info, data);
            }
        });
    }

    private void startDownload(NameInfo info, CompletableFuture<T> result) {
        String literal = getProvider(info);
        CompletableFuture<Void> future = null;

        if (!getExcludeLiterals().contains(literal)) {
            future = tryLoad(providers.get(literal), info);
        }

        if (future == null && !literal.equals(getDefaultLiteral())) {
            future = tryLoad(providers.get(getDefaultLiteral()), info);
        }

        if (future == null) {
            result.completeExceptionally(
                    new IllegalStateException("No provider for " + info.base()));
            return;
        }

        future.whenComplete((v, throwable) -> {
            if (throwable != null) result.completeExceptionally(throwable);
            else                   result.complete(null);
        });
    }

    private CompletableFuture<Void> tryLoad(List<PrioritizedProvider> list, NameInfo info) {
        if (list == null || list.isEmpty()) return null;
        for (PrioritizedProvider p : list) {
            try { return p.provider().load(info); }
            catch (Exception e) {
                LOGGER.error("Provider {} failed: {}",
                        p.provider().getClass().getSimpleName(), e.getMessage());
            }
        }
        return null;
    }

    private static Throwable unwrap(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    protected abstract void prepareProviders();
    protected abstract String getProvider(NameInfo info);
    protected abstract String getDefaultLiteral();
    protected abstract String getExcludeLiterals();
    protected abstract void store(NameInfo info, T data);
    protected abstract T createDataHolder();
    protected abstract void invalidate(NameInfo info);

    private record PrioritizedProvider(TextureProvider provider, int priority) {}
}