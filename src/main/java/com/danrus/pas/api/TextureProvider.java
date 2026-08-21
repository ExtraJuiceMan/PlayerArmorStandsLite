package com.danrus.pas.api;

import java.util.concurrent.CompletableFuture;

public interface TextureProvider {
    CompletableFuture<Void> load(NameInfo info);
    String getLiteral();
}
