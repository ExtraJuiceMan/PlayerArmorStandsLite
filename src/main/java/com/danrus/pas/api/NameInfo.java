package com.danrus.pas.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class NameInfo {
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[<>:\"/\\\\?* ]");
    private static final ConcurrentHashMap<String, NameInfo> PARSE_CACHE = new ConcurrentHashMap<>(256);
    public static final NameInfo EMPTY = new NameInfo();

    private final String base;
    private final String skinProvider;
    private final boolean slim;
    private final boolean capeEnabled;
    private final String capeProvider;
    private final String capeId;
    private final String overlayTexture;
    private final int overlayBlend;
    private final String displayName;
    private final int version;
    private volatile String compiled;

    private NameInfo() {
        this("", "M", false, false, "M", "", "", 100, "", null, 2);
    }

    private NameInfo(String base, String skinProvider, boolean slim,
                     boolean capeEnabled, String capeProvider, String capeId,
                     String overlayTexture, int overlayBlend, String displayName,
                     @Nullable Identifier meme, int version) {
        this.base = base;
        this.skinProvider = skinProvider;
        this.slim = slim;
        this.capeEnabled = capeEnabled;
        this.capeProvider = capeProvider;
        this.capeId = capeId;
        this.overlayTexture = overlayTexture;
        this.overlayBlend = overlayBlend;
        this.displayName = displayName;
        this.version = version;
    }

    public static NameInfo parse(@Nullable Component input) {
        return input == null ? EMPTY : parse(input.getString());
    }

    public static NameInfo parse(@Nullable String input) {
        if (input == null || input.isEmpty()) return EMPTY;
        NameInfo cached = PARSE_CACHE.get(input);
        if (cached != null) return cached;
        NameInfo parsed = doParse(input);
        PARSE_CACHE.putIfAbsent(input, parsed);
        return parsed;
    }

    private static NameInfo doParse(String input) {
        return input.contains("||") ? parseV2(input) : parseV1(input);
    }

    private static NameInfo parseV1(String input) {
        int sep = input.indexOf('|');
        String base = (sep < 0 ? input : input.substring(0, sep)).trim();

        if (hasIllegalChars(base)) return EMPTY;

        String params = sep < 0 ? "" : input.substring(sep + 1).trim();
        FeatureParseResult f = parseFeatures(params);

        return new NameInfo(
                base,
                f.skinProvider,
                f.slim,
                f.capeEnabled,
                f.capeProvider,
                f.capeId,
                f.overlayTexture,
                f.overlayBlend,
                f.displayName,
                null,
                1
        );
    }

    private static NameInfo parseV2(String input) {
        int sep = input.indexOf("||");
        String base = input.substring(0, sep).trim();

        if (hasIllegalChars(base)) return EMPTY;

        String params = sep + 2 < input.length()
                ? input.substring(sep + 2).trim()
                : "";

        FeatureParseResult f = parseFeatures(params);

        return new NameInfo(
                base,
                f.skinProvider,
                f.slim,
                f.capeEnabled,
                f.capeProvider,
                f.capeId,
                f.overlayTexture,
                f.overlayBlend,
                f.displayName,
                null,
                2
        );
    }

    public String compile() {
        String c = compiled;
        if (c != null) return c;

        StringBuilder sb = new StringBuilder(base);
        StringBuilder feats = new StringBuilder();

        if (capeEnabled) {
            StringBuilder cape = new StringBuilder("C");

            if (!capeProvider.equals("M")) {
                cape.append(':').append(capeProvider);

                if (!capeId.isEmpty()) {
                    cape.append('%').append(capeId).append('%');
                }
            }

            appendFeature(feats, cape.toString());
        }

        if (!skinProvider.equals("M")) {
            appendFeature(feats, skinProvider);
        }

        if (slim) {
            appendFeature(feats, "S");
        }

        if (!overlayTexture.isEmpty()) {
            appendFeature(feats, "T:" + overlayTexture + "%" + overlayBlend);
        }

        if (!displayName.isEmpty()) {
            appendFeature(feats, "D:" + displayName);
        }

        if (feats.length() > 0) {
            sb.append("||").append(feats);
        }

        compiled = sb.toString();
        return compiled;
    }

    private static void appendFeature(StringBuilder feats, String feature) {
        if (feats.length() > 0) {
            feats.append(';');
        }
        feats.append(feature);
    }

    public String base() { return base; }
    public String skinProvider() { return skinProvider; }
    public boolean isSlim() { return slim; }
    public boolean hasCape() { return capeEnabled; }
    public String capeProvider() { return capeProvider; }
    public String capeId() { return capeId; }
    public String overlayTexture() { return overlayTexture; }
    public int overlayBlend() { return overlayBlend; }
    public String displayName() { return displayName; }
    public boolean isEmpty() { return base.isEmpty(); }
    public int version() { return version; }
    public boolean shouldUpsideDown() {
        return base.equalsIgnoreCase("Dinnerbone") || base.equalsIgnoreCase("Grumm");
    }
    public boolean hasDisplayName() { return !displayName.isEmpty(); }

    @Override public String toString() { return compile(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NameInfo n)) return false;
        return version == n.version && base.equals(n.base)
            && skinProvider.equals(n.skinProvider) && slim == n.slim
            && capeEnabled == n.capeEnabled && capeProvider.equals(n.capeProvider)
            && capeId.equals(n.capeId) && overlayTexture.equals(n.overlayTexture)
            && overlayBlend == n.overlayBlend && displayName.equals(n.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, skinProvider, slim, capeEnabled, capeProvider, capeId, overlayTexture, overlayBlend, displayName, version);
    }

    public Builder toBuilder() { return new Builder(this); }

    private static boolean hasIllegalChars(String s) {
        return ILLEGAL_CHARS.matcher(s).find();
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static final class FeatureParseResult {
        String skinProvider = "M";
        boolean slim = false;
        boolean capeEnabled = false;
        String capeProvider = "M";
        String capeId = "";
        String overlayTexture = "";
        int overlayBlend = 100;
        String displayName = "";
    }

    private static FeatureParseResult parseFeatures(String params) {
        FeatureParseResult r = new FeatureParseResult();

        if (params == null || params.isEmpty()) {
            return r;
        }

        int dIndex = params.indexOf("D:");
        String feats = params;

        if (dIndex >= 0) {
            r.displayName = params.substring(dIndex + 2);
            feats = params.substring(0, dIndex);
        }

        int i = 0;
        int len = feats.length();

        while (i < len) {
            char ch = feats.charAt(i);

            if (ch == ';' || ch == ' ') {
                i++;
                continue;
            }

            if (feats.startsWith("T:", i)) {
                i += 2;

                int pct = feats.indexOf('%', i);

                if (pct >= 0) {
                    r.overlayTexture = feats.substring(i, pct).trim();

                    int next = findNextFeatureStart(feats, pct + 1);
                    String blendText = next < 0
                            ? feats.substring(pct + 1)
                            : feats.substring(pct + 1, next);

                    try {
                        r.overlayBlend = clampInt(Integer.parseInt(blendText.trim()), 0, 100);
                    } catch (NumberFormatException ignored) {
                    }

                    i = next < 0 ? len : next;
                } else {
                    int next = findNextFeatureStart(feats, i);
                    r.overlayTexture = next < 0
                            ? feats.substring(i).trim()
                            : feats.substring(i, next).trim();

                    i = next < 0 ? len : next;
                }

                continue;
            }

            if (ch == 'S') {
                r.slim = true;
                i++;
                continue;
            }

            if (ch == 'N' || ch == 'F') {
                r.skinProvider = String.valueOf(ch);
                i++;
                continue;
            }

            if (ch == 'C') {
                r.capeEnabled = true;
                i++;

                if (i < len && feats.charAt(i) == ':') {
                    i++;

                    int pct = feats.indexOf('%', i);

                    if (pct >= 0) {
                        r.capeProvider = feats.substring(i, pct).trim();

                        int end = feats.indexOf('%', pct + 1);

                        if (end > pct) {
                            r.capeId = feats.substring(pct + 1, end).trim();
                            i = end + 1;
                        } else {
                            i = pct + 1;
                        }
                    } else {
                        int next = findNextFeatureStart(feats, i);
                        r.capeProvider = next < 0
                                ? feats.substring(i).trim()
                                : feats.substring(i, next).trim();

                        i = next < 0 ? len : next;
                    }
                } else {
                    int next = findNextFeatureStart(feats, i);

                    if (next > i) {
                        i = next;
                    } else if (i < len && !isFeatureStart(feats, i) && feats.charAt(i) != ';') {
                        i++;
                    }
                }

                continue;
            }

            i++;
        }

        return r;
    }

    private static int findNextFeatureStart(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == ';') {
                return i;
            }

            if (isFeatureStart(s, i)) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isFeatureStart(String s, int i) {
        if (i >= s.length()) return false;

        char c = s.charAt(i);

        if (c == 'C' || c == 'N' || c == 'F' || c == 'S') {
            return true;
        }

        return s.startsWith("T:", i);
    }

    public static class Builder {
        private String base = "";
        private String skinProvider = "M";
        private boolean slim = false;
        private boolean cape = false;
        private String capeProvider = "M";
        private String capeId = "";
        private String overlay = "";
        private int blend = 100;
        private String displayName = "";
        private int version = 2;

        public Builder(NameInfo src) {
            this.base = src.base; this.skinProvider = src.skinProvider;
            this.slim = src.slim; this.cape = src.capeEnabled;
            this.capeProvider = src.capeProvider; this.capeId = src.capeId;
            this.overlay = src.overlayTexture; this.blend = src.overlayBlend;
            this.displayName = src.displayName; this.version = src.version;
        }

        public Builder setBase(String base) { this.base = base == null ? "" : base; return this; }
        public Builder setSkinProvider(String p) { this.skinProvider = p == null ? "M" : p; return this; }
        public Builder setSlim(boolean s) { this.slim = s; return this; }
        public Builder setCapeEnabled(boolean e) { this.cape = e; return this; }
        public Builder setCapeProvider(String p) { this.capeProvider = p == null ? "M" : p; return this; }
        public Builder setCapeId(String id) { this.capeId = id == null ? "" : id; return this; }
        public Builder setOverlay(String tex) { this.overlay = tex == null ? "" : tex; return this; }
        public Builder setBlend(int b) { this.blend = Math.max(0, Math.min(100, b)); return this; }
        public Builder setDisplayName(String n) { this.displayName = n == null ? "" : n; return this; }

        public String base() { return base; }
        public String getDesiredProvider() { return skinProvider; }
        public boolean isSlim() { return slim; }
        public boolean hasCape() { return cape; }
        public String capeProvider() { return capeProvider; }
        public String capeId() { return capeId; }
        public String overlayTexture() { return overlay; }
        public int overlayBlend() { return blend; }
        public String displayName() { return displayName; }

        public NameInfo build() {
            return new NameInfo(base, skinProvider, slim, cape, capeProvider, capeId, overlay, blend, displayName, null, version);
        }
        public String compile() { return build().compile(); }
    }
}
