package com.danrus.pas.utils;

import com.danrus.pas.ModExecutor;
import com.danrus.pas.PlayerArmorStandsClient;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class TextureDownloader {
    private static final int MAX_ATTEMPTS = 3;

    private static final long INITIAL_BACKOFF_MS = 500L;
    private static final long MAX_BACKOFF_MS = 4_000L;
    private static final long MAX_RETRY_AFTER_MS = 10_000L;

    private static final long MAX_DOWNLOAD_BYTES = 16L * 1024L * 1024L;
    private static final int READ_BUFFER_SIZE = 8192;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final boolean BLOCK_PRIVATE_HOSTS = true;

    private static final ScheduledExecutorService RETRY_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "PAS Retry");
                thread.setDaemon(true);
                return thread;
            });

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .proxy(new MinecraftProxySelector())
            .build();

    public static CompletableFuture<Identifier> downloadAndRegister(
            Identifier id,
            Path path,
            String uri,
            boolean remap
    ) {
        CancellationState cancellation = new CancellationState();
        CompletableFuture<Identifier> resultFuture = new CompletableFuture<>();

        CompletableFuture<NativeImage> downloadFuture = withRetries(
                "texture download " + uri,
                attemptIndex -> CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return downloadOnce(path, uri, cancellation);
                            } catch (IOException e) {
                                throw new CompletionException(e);
                            }
                        },
                        ModExecutor.DOWNLOAD_EXECUTOR
                ),
                TextureDownloader::isRetryableIOException,
                cancellation
        );

        CompletableFuture<Identifier> workFuture = downloadFuture.thenCompose(image ->
                TextureUtils.registerTexture(image, id, remap)
                        .whenComplete((textureId, error) -> {
                            if (error != null) {
                                Throwable cause = unwrap(error);
                                if (!(cause instanceof CancellationException)) {
                                    try {
                                        CacheUtils.deleteAsync(path);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                        }));

        workFuture.whenComplete((value, error) -> {
            if (error != null) {
                resultFuture.completeExceptionally(unwrap(error));
            } else {
                resultFuture.complete(value);
            }
        });

        resultFuture.whenComplete((value, error) -> {
            if (resultFuture.isCancelled()) {
                cancellation.cancel();
                workFuture.cancel(true);
            }
        });

        return resultFuture;
    }

    private static NativeImage downloadOnce(Path path, String uri, CancellationState cancellation) throws IOException {
        if (cancellation.isCancelled()) {
            throw new CancellationException("Download cancelled for " + uri);
        }

        Thread currentThread = Thread.currentThread();
        cancellation.setRunningThread(currentThread);

        try {
            URI parsedUri = parseUri(uri);

            HttpRequest request = HttpRequest.newBuilder(parsedUri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 Minecraft Client")
                    .GET()
                    .build();

            HttpResponse<InputStream> response;
            try {
                response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                if (cancellation.isCancelled()) {
                    throw new CancellationException("Download cancelled for " + uri);
                }
                throw new NonRetryableException("Download interrupted for " + uri, e);
            }

            try (InputStream input = response.body()) {
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                    if (contentLength > MAX_DOWNLOAD_BYTES) {
                        throw new NonRetryableException("Download too large from " + uri);
                    }

                    byte[] data = readLimited(input, contentLength);
                    if (data.length == 0) {
                        throw new NonRetryableException("Empty response from " + uri);
                    }

                    NativeImage image;
                    try {
                        image = NativeImage.read(new ByteArrayInputStream(data));
                    } catch (IOException e) {
                        throw new NonRetryableException("Invalid image data from " + uri, e);
                    }

                    CacheUtils.saveCacheFile(path, data);
                    return image;
                }

                if (!isRetryableStatus(status)) {
                    throw new NonRetryableException("HTTP " + status + " for " + uri);
                }

                throw new RetryableException("HTTP " + status + " for " + uri, parseRetryAfterMs(response));
            }
        } finally {
            cancellation.clearRunningThread(currentThread);
        }
    }

    private static URI parseUri(String uri) throws IOException {
        URI parsed;
        try {
            parsed = URI.create(uri);
        } catch (IllegalArgumentException e) {
            throw new NonRetryableException("Invalid download URL: " + uri, e);
        }

        String scheme = parsed.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new NonRetryableException("Unsupported download protocol: " + uri);
        }

        if (parsed.getHost() == null) {
            throw new NonRetryableException("Invalid download URL: " + uri);
        }

        return parsed;
    }

    private static byte[] readLimited(InputStream input, long contentLength) throws IOException {
        int initialCapacity = (contentLength > 0 && contentLength <= MAX_DOWNLOAD_BYTES)
                ? (int) contentLength
                : READ_BUFFER_SIZE;

        ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity);
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        long total = 0L;
        int read;

        while ((read = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Thread interrupted while reading stream");
            }

            total += read;
            if (total > MAX_DOWNLOAD_BYTES) {
                throw new NonRetryableException("Download exceeded maximum allowed bytes");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static Proxy safeProxy() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft != null ? minecraft.getProxy() : Proxy.NO_PROXY;
        } catch (Throwable t) {
            return Proxy.NO_PROXY;
        }
    }

    // ------------------------------------------------------------------
    // Plain REST GET
    // ------------------------------------------------------------------

    public static CompletableFuture<String> get(String url) {
        CancellationState cancellation = new CancellationState();
        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        CompletableFuture<String> workFuture = withRetries(
                "GET " + url,
                attemptIndex -> sendGetOnce(url, cancellation),
                TextureDownloader::isRetryableIOException,
                cancellation
        );

        workFuture.whenComplete((value, error) -> {
            if (error != null) {
                resultFuture.completeExceptionally(unwrap(error));
            } else {
                resultFuture.complete(value);
            }
        });

        resultFuture.whenComplete((value, error) -> {
            if (resultFuture.isCancelled()) {
                cancellation.cancel();
                workFuture.cancel(true);
            }
        });

        return resultFuture;
    }

    private static CompletableFuture<String> sendGetOnce(String url, CancellationState cancellation) {
        if (cancellation.isCancelled()) {
            return failedFuture(new CancellationException("GET cancelled for " + url));
        }

        HttpRequest request;
        try {
            URI uri = parseUri(url);
            request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 Minecraft Client")
                    .GET()
                    .build();
        } catch (IOException e) {
            return failedFuture(e);
        }

        CompletableFuture<HttpResponse<String>> rawFuture =
                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        cancellation.setActiveRequestFuture(rawFuture);

        return rawFuture
                .thenApply(response -> {
                    try {
                        return handleGetResponse(url, response);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                })
                .whenComplete((value, error) -> cancellation.clearActiveRequestFuture(rawFuture));
    }

    private static String handleGetResponse(String url, HttpResponse<String> response) throws IOException {
        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return response.body();
        }

        if (!isRetryableStatus(status)) {
            throw new NonRetryableException("HTTP " + status + " for " + url);
        }

        throw new RetryableException("HTTP " + status + " for " + url, parseRetryAfterMs(response));
    }

    private static <T> CompletableFuture<T> withRetries(
            String debugLabel,
            IntFunction<CompletableFuture<T>> attemptSupplier,
            Predicate<Throwable> isRetryable,
            CancellationState cancellation
    ) {
        return attempt(debugLabel, attemptSupplier, isRetryable, cancellation, 0);
    }

    private static <T> CompletableFuture<T> attempt(
            String debugLabel,
            IntFunction<CompletableFuture<T>> attemptSupplier,
            Predicate<Throwable> isRetryable,
            CancellationState cancellation,
            int attemptIndex
    ) {
        if (cancellation.isCancelled()) {
            return failedFuture(new CancellationException(debugLabel + " cancelled"));
        }

        CompletableFuture<T> attemptFuture;
        try {
            attemptFuture = attemptSupplier.apply(attemptIndex);
        } catch (Throwable t) {
            return failedFuture(unwrap(t));
        }

        return attemptFuture.exceptionallyCompose(error -> {
            Throwable cause = unwrap(error);
            if (cause == null) {
                cause = new IOException("Unknown error for " + debugLabel);
            }

            if (cause instanceof CancellationException || cancellation.isCancelled()) {
                return failedFuture(cause instanceof CancellationException
                        ? cause
                        : new CancellationException(debugLabel + " cancelled"));
            }

            if (attemptIndex + 1 >= MAX_ATTEMPTS || !isRetryable.test(cause)) {
                return failedFuture(cause);
            }

            long delay = backoffMs(attemptIndex, cause);

            PlayerArmorStandsClient.LOGGER.debug(
                    "Retrying {} in {} ms (attempt {})", debugLabel, delay, attemptIndex + 2);

            CompletableFuture<T> retryFuture = new CompletableFuture<>();

            try {
                ScheduledFuture<?> scheduled = RETRY_EXECUTOR.schedule(() -> {
                    cancellation.clearPendingRetry();

                    if (cancellation.isCancelled()) {
                        retryFuture.completeExceptionally(new CancellationException(debugLabel + " cancelled"));
                        return;
                    }

                    try {
                        attempt(debugLabel, attemptSupplier, isRetryable, cancellation, attemptIndex + 1)
                                .whenComplete((value, retryError) -> {
                                    if (retryError != null) {
                                        retryFuture.completeExceptionally(unwrap(retryError));
                                    } else {
                                        retryFuture.complete(value);
                                    }
                                });
                    } catch (Throwable t) {
                        retryFuture.completeExceptionally(unwrap(t));
                    }
                }, delay, TimeUnit.MILLISECONDS);

                cancellation.setPendingRetry(scheduled);
            } catch (Throwable t) {
                retryFuture.completeExceptionally(unwrap(t));
            }

            return retryFuture;
        });
    }

    private static boolean isRetryableStatus(int status) {
        return status == 408
                || status == 425
                || status == 429
                || status >= 500;
    }

    private static boolean isRetryableIOException(Throwable error) {
        if (error == null || error instanceof NonRetryableException) {
            return false;
        }
        if (error instanceof IllegalArgumentException || error instanceof SecurityException) {
            return false;
        }
        if (isInterrupted(error)) {
            return false;
        }
        return error instanceof IOException;
    }

    private static boolean isInterrupted(Throwable error) {
        return error instanceof InterruptedIOException && !(error instanceof SocketTimeoutException);
    }

    private static long backoffMs(int attempt, Throwable error) {
        long multiplier = 1L << Math.min(attempt, 20);
        long base = Math.min(INITIAL_BACKOFF_MS * multiplier, MAX_BACKOFF_MS);

        if (error instanceof RetryableException retryable && retryable.retryAfterMs() > 0) {
            base = Math.max(base, retryable.retryAfterMs());
        }

        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, base / 10));
        return base + jitter;
    }

    private static long parseRetryAfterMs(HttpResponse<?> response) {
        Optional<String> headerValue = response.headers().firstValue("Retry-After");

        if (headerValue.isEmpty() || headerValue.get().isBlank()) {
            return 0L;
        }

        String header = headerValue.get().trim();
        try {
            long seconds = Long.parseLong(header);
            if (seconds <= 0) return 0L;
            return Math.min(seconds, MAX_RETRY_AFTER_MS / 1000L) * 1000L;
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime dateTime = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME);
                long delay = dateTime.toInstant().toEpochMilli() - System.currentTimeMillis();
                return delay <= 0 ? 0L : Math.min(delay, MAX_RETRY_AFTER_MS);
            } catch (DateTimeParseException ignored2) {
                return 0L;
            }
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if ((current instanceof CompletionException
                    || current instanceof ExecutionException
                    || current instanceof UncheckedIOException)
                    && current.getCause() != null
                    && current.getCause() != current) {
                current = current.getCause();
            } else {
                return current;
            }
        }
        return error;
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable error) {
        return CompletableFuture.failedFuture(error == null ? new IOException("Unknown error") : error);
    }

    private static final class MinecraftProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            return List.of(safeProxy());
        }

        @Override
        public void connectFailed(URI uri, SocketAddress address, IOException failure) {
        }
    }

    private static final class CancellationState {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicReference<Thread> runningThread = new AtomicReference<>();
        private final AtomicReference<CompletableFuture<?>> activeRequestFuture = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> pendingRetry = new AtomicReference<>();

        boolean isCancelled() {
            return cancelled.get();
        }

        void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                Thread thread = runningThread.getAndSet(null);
                if (thread != null) thread.interrupt();

                CompletableFuture<?> activeFuture = activeRequestFuture.getAndSet(null);
                if (activeFuture != null) activeFuture.cancel(true);

                ScheduledFuture<?> retry = pendingRetry.getAndSet(null);
                if (retry != null) retry.cancel(false);
            }
        }

        void setRunningThread(Thread thread) {
            runningThread.set(thread);
            if (cancelled.get() && thread != null) thread.interrupt();
        }

        void clearRunningThread(Thread thread) {
            runningThread.compareAndSet(thread, null);
        }

        void setActiveRequestFuture(CompletableFuture<?> future) {
            activeRequestFuture.set(future);
            if (cancelled.get() && future != null) future.cancel(true);
        }

        void clearActiveRequestFuture(CompletableFuture<?> future) {
            activeRequestFuture.compareAndSet(future, null);
        }

        void setPendingRetry(ScheduledFuture<?> future) {
            pendingRetry.set(future);
            if (cancelled.get() && future != null) future.cancel(false);
        }

        void clearPendingRetry() {
            pendingRetry.set(null);
        }
    }

    private static final class NonRetryableException extends IOException {
        private NonRetryableException(String message) { super(message); }
        private NonRetryableException(String message, Throwable cause) { super(message, cause); }
    }

    private static final class RetryableException extends IOException {
        private final long retryAfterMs;

        private RetryableException(String message, long retryAfterMs) {
            super(message);
            this.retryAfterMs = retryAfterMs;
        }

        private long retryAfterMs() { return retryAfterMs; }
    }
}