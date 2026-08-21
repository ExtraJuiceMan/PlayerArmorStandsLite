package com.danrus.pas.render.gui;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.skin.FileSkinProvider;
import com.danrus.pas.utils.Id;
import com.danrus.pas.utils.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PasConfiguratorScreen extends Screen {
    private static final WidgetSprites TAB_BUTTON_SPRITES = new net.minecraft.client.gui.components.WidgetSprites(
            Id.pas("tab"),
            Id.pas("tab_selected"),
            Id.pas("tab_highlighted")
    );

    private static final Identifier QUESTION_MARK_ICON = Id.pas("question");

    private static final Identifier BACKGROUND_TEXTURE =
            Id.pas("textures/gui/sprites/pas_gui.png");

    private static final float DEFAULT_PREVIEW_YAW = -45.0F;
    private static final float CAPE_PREVIEW_YAW = 135.0F;
    private static final float PREVIEW_YAW_SENSITIVITY = 0.5F;

    private float previewYaw = DEFAULT_PREVIEW_YAW;
    private float targetPreviewYaw = DEFAULT_PREVIEW_YAW;
    private boolean animatePreviewYaw = false;
    private boolean draggingPreview = false;

    private final AnvilScreen parent;
    private final ArmorStand entity;
    private final NameInfo.Builder info;

    private int activeTab = 0;

    private EditBox nameBox;
    private Button skinProviderBtn;
    private Button armTypeBtn;
    private Button openFolderBtn;

    private Button capeEnabledBtn;
    private Button capeProviderBtn;
    private EditBox capeIdBox;
    private Button armTypeBtn2;

    private EditBox overlayBox;
    private AbstractSliderButton blendSlider;
    private EditBox displayNameBox;

    private Button acceptBtn;
    private Button cancelBtn;

    private TabButton skinTabBtn;
    private TabButton capeTabBtn;
    private TabButton miscTabBtn;

    private TextWidget skinNameLabel;
    private TextWidget skinProviderLabel;
    private TextWidget skinArmTypeLabel;
    private TextWidget skinOpenFolderLabel;

    private TextWidget capeEnabledLabelWidget;
    private TextWidget capeArmTypeLabel;
    private TextWidget capeProviderLabel;
    private TextWidget capeNameLabel;

    private TextWidget overlayLabel;
    private TextWidget blendLabel;
    private TextWidget displayNameLabel;

    private CompletableFuture<Void> refreshDebounceFuture;

    private static final Executor delayedExecutor =
            CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS, Minecraft.getInstance());

    public PasConfiguratorScreen(AnvilScreen parent, String currentName) {
        super(Component.translatable("pas.menu.name"));

        this.parent = parent;
        this.entity = new ArmorStand(Minecraft.getInstance().level, 0, 0, 0);
        ((com.danrus.pas.mixin.accessors.EntityAccessor) this.entity).pas$setId(-1);

        this.info = NameInfo.parse(currentName).toBuilder();

        refreshPreview();
    }
    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        skinTabBtn = addRenderableWidget(new TabButton(
                cx - 124, cy - 109, 80, 15,
                Component.translatable("pas.menu.tab.skin"),
                b -> setTab(0),
                activeTab == 0
        ));

        capeTabBtn = addRenderableWidget(new TabButton(
                cx - 43, cy - 109, 80, 15,
                Component.translatable("pas.menu.tab.cape"),
                b -> setTab(1),
                activeTab == 1
        ));

        miscTabBtn = addRenderableWidget(new TabButton(
                cx + 38, cy - 109, 80, 15,
                Component.translatable("pas.menu.tab.overlay"),
                b -> setTab(2),
                activeTab == 2
        ));

        skinTabBtn.setActiveTab(true);

        acceptBtn = addRenderableWidget(Button.builder(
                Component.translatable("pas.menu.accept").withStyle(ChatFormatting.GREEN),
                b -> accept()
        ).bounds(cx + 10, cy + 120, 100, 20).build());

        cancelBtn = addRenderableWidget(Button.builder(
                Component.translatable("pas.menu.cancel").withStyle(ChatFormatting.RED),
                b -> onClose()
        ).bounds(cx - 110, cy + 120, 100, 20).build());

        nameBox = addRenderableWidget(new EditBox(
                font, cx + 4, cy - 70, 96, 20, Component.literal("Name")
        ));
        nameBox.setValue(info.base());

        skinProviderBtn = addRenderableWidget(Button.builder(
                providerLabel(info.getDesiredProvider()),
                this::cycleSkinProvider
        ).bounds(cx + 4, cy - 30, 96, 20).build());

        armTypeBtn = addRenderableWidget(Button.builder(
                armLabel(),
                b -> {
                    info.setSlim(!info.isSlim());
                    b.setMessage(armLabel());
                    refreshPreview();
                }
        ).bounds(cx + 4, cy + 10, 96, 20).build());

        openFolderBtn = addRenderableWidget(Button.builder(
                Component.translatable("pas.menu.tab.skin.open_folder.button"),
                b -> openSkinsFolder()
        ).bounds(cx + 4, cy + 50, 96, 20).build());

        capeEnabledBtn = addRenderableWidget(Button.builder(
                capeEnabledLabel(),
                b -> {
                    info.setCapeEnabled(!info.hasCape());
                    b.setMessage(capeEnabledLabel());
                    refreshPreview();
                }
        ).bounds(cx + 4, cy - 70, 96, 20).build());

        armTypeBtn2 = addRenderableWidget(Button.builder(
                armLabel(),
                b -> {
                    info.setSlim(!info.isSlim());
                    b.setMessage(armLabel());
                    refreshPreview();
                }
        ).bounds(cx + 4, cy - 30, 96, 20).build());

        capeProviderBtn = addRenderableWidget(Button.builder(
                capeProviderLabel(),
                this::cycleCapeProvider
        ).bounds(cx + 4, cy + 10, 96, 20).build());

        capeIdBox = addRenderableWidget(new EditBox(
                font, cx + 4, cy + 50, 96, 20, Component.literal("Cape ID")
        ));
        capeIdBox.setValue(info.capeId());

        overlayBox = addRenderableWidget(new EditBox(
                font, cx + 4, cy - 70, 96, 20, Component.literal("Overlay")
        ));
        overlayBox.setValue(info.overlayTexture());

        blendSlider = addRenderableWidget(new BlendSlider(
                cx + 4, cy - 30, 96, 20, info.overlayBlend(),
                v -> {
                    info.setBlend(v);
                    refreshPreview();
                }
        ));

        displayNameBox = addRenderableWidget(new EditBox(
                font, cx + 4, cy + 10, 96, 20, Component.literal("Display Name")
        ));
        displayNameBox.setValue(info.displayName());

        // Text widgets replacing drawLabel()
        skinNameLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 82, 96, 10,
                Component.translatable("pas.menu.tab.skin.name")
        ).setTooltip(Component.translatable("pas.menu.tab.skin.name.tooltip")));

        skinProviderLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 42, 96, 10,
                Component.translatable("pas.menu.tab.skin.provider")
        ));

        skinArmTypeLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 2, 96, 10,
                Component.translatable("pas.menu.tab.skin.arm_type")
        ));

        skinOpenFolderLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy + 38, 96, 10,
                Component.translatable("pas.menu.tab.skin.open_folder")
        ));

        capeEnabledLabelWidget = addRenderableWidget(new TextWidget(
                cx + 4, cy - 82, 96, 10,
                Component.translatable("pas.menu.tab.cape.label")
        ));

        capeArmTypeLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 42, 96, 10,
                Component.translatable("pas.menu.tab.skin.arm_type")
        ));

        capeProviderLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 2, 96, 10,
                Component.translatable("pas.menu.tab.cape.provider")
        ));

        capeNameLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy + 38, 96, 10,
                Component.translatable("pas.menu.tab.cape.name")
        ).setTooltip(Component.translatable("pas.menu.tab.cape.name.tooltip")));

        overlayLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 82, 96, 10,
                Component.translatable("pas.menu.tab.overlay.name")
        ).setTooltip(Component.translatable("pas.menu.tab.overlay.name.tooltip")));

        blendLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 42, 96, 10,
                Component.translatable("pas.menu.tab.overlay.blend")
        ));

        displayNameLabel = addRenderableWidget(new TextWidget(
                cx + 4, cy - 2, 96, 10,
                Component.translatable("pas.menu.tab.overlay.display_name")
        ).setTooltip(Component.translatable("pas.menu.tab.overlay.display_name.tooltip")));

        nameBox.setResponder(v -> {
            info.setBase(v);
            debounceRefresh();
        });

        capeIdBox.setResponder(v -> {
            info.setCapeId(v);
            debounceRefresh();
        });

        overlayBox.setResponder(v -> {
            info.setOverlay(v);
            debounceRefresh();
        });

        displayNameBox.setResponder(v -> {
            info.setDisplayName(v);
            debounceRefresh();
        });

        setTab(activeTab);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        g.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE,
                width / 2 - 128,
                height / 2 - 128 + 18,
                0.0F,
                0.0F,
                256,
                256,
                256,
                256
        );
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        g.centeredText(
                font,
                Component.translatable("pas.menu.name"),
                width / 2,
                15,
                0xFFFFFFFF
        );

        renderPreview(g);
    }

    private void updatePreviewRotation() {
        if (!animatePreviewYaw) {
            return;
        }

        float difference = wrapDegrees(targetPreviewYaw - previewYaw);

        if (Math.abs(difference) < 0.01F) {
            previewYaw = targetPreviewYaw;
            animatePreviewYaw = false;
            return;
        }

        float step = Math.min(Math.abs(difference), Math.max(0.25F, Math.abs(difference) * 0.18F));

        previewYaw = wrapDegrees(previewYaw + Math.copySign(step, difference));

        if (Math.abs(wrapDegrees(targetPreviewYaw - previewYaw)) < 0.01F) {
            previewYaw = targetPreviewYaw;
            animatePreviewYaw = false;
        }
    }

    private static float wrapDegrees(float angle) {
        float result = angle % 360.0F;

        if (result >= 180.0F) {
            result -= 360.0F;
        }

        if (result < -180.0F) {
            result += 360.0F;
        }

        return result;
    }

    private void renderPreview(GuiGraphicsExtractor g) {
        updatePreviewRotation();

        int left = width / 2 - 130;
        int top = height / 2 - 70;
        int right = width / 2 - 18;
        int bottom = height / 2 + 120;

        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(30.0F + previewYaw))
                .rotateX((float) Math.PI);

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super ArmorStand, ?> renderer = dispatcher.getRenderer(entity);

        var renderState = renderer.createRenderState(entity, 1.0F);
        renderState.lightCoords = 15728880;

        g.entity(
                renderState,
                70.0F,
                new Vector3f(0.1f, 0.75f, 0),
                rotation,
                null,
                left,
                top,
                right,
                bottom
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInsidePreview(event.x(), event.y())) {
            draggingPreview = true;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingPreview) {
            draggingPreview = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (draggingPreview && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            animatePreviewYaw = false;

            // FIX: Use 'dx' (drag delta) instead of 'event.x()' (absolute mouse position)
            previewYaw = wrapDegrees(previewYaw + (float) dx * PREVIEW_YAW_SENSITIVITY);
            targetPreviewYaw = previewYaw;

            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        int left = width / 2 - 130;
        int top = height / 2 - 70;
        int right = width / 2 - 18;
        int bottom = height / 2 + 120;

        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }
    private void setTab(int tab) {
        activeTab = tab;

        boolean isSkin = tab == 0;
        boolean isCape = tab == 1;
        boolean isMisc = tab == 2;

        skinTabBtn.setActiveTab(isSkin);
        capeTabBtn.setActiveTab(isCape);
        miscTabBtn.setActiveTab(isMisc);

        nameBox.visible = isSkin;
        skinProviderBtn.visible = isSkin;
        armTypeBtn.visible = isSkin;
        openFolderBtn.visible = isSkin && info.getDesiredProvider().equals("F");

        capeEnabledBtn.visible = isCape;
        armTypeBtn2.visible = isCape;
        capeProviderBtn.visible = isCape;
        capeIdBox.visible = isCape;

        overlayBox.visible = isMisc;
        blendSlider.visible = isMisc;
        displayNameBox.visible = isMisc;

        // Text widgets
        skinNameLabel.visible = isSkin;
        skinProviderLabel.visible = isSkin;
        skinArmTypeLabel.visible = isSkin;
        skinOpenFolderLabel.visible = isSkin && info.getDesiredProvider().equals("F");

        capeEnabledLabelWidget.visible = isCape;
        capeArmTypeLabel.visible = isCape;
        capeProviderLabel.visible = isCape;
        capeNameLabel.visible = isCape;

        overlayLabel.visible = isMisc;
        blendLabel.visible = isMisc;
        displayNameLabel.visible = isMisc;

        targetPreviewYaw = isCape
                ? CAPE_PREVIEW_YAW
                : DEFAULT_PREVIEW_YAW;

        animatePreviewYaw = true;
    }

    private void cycleSkinProvider(Button b) {
        String current = info.getDesiredProvider();

        String next = switch (current) {
            case "M" -> "N";
            case "N" -> "F";
            default -> "M";
        };

        info.setSkinProvider(next);
        b.setMessage(providerLabel(next));

        boolean isFileProvider = next.equals("F");

        openFolderBtn.visible = isFileProvider;
        skinOpenFolderLabel.visible = isFileProvider;

        refreshPreview();
    }

    private void cycleCapeProvider(Button b) {
        String current = info.capeProvider();

        String next = switch (current) {
            case "M" -> "A";
            case "A" -> "I";
            default -> "M";
        };

        info.setCapeProvider(next);
        b.setMessage(capeProviderLabel());

        refreshPreview();
    }

    private void openSkinsFolder() {
        FileSkinProvider.SKINS_PATH.toFile().mkdirs();
        Util.getPlatform().openFile(FileSkinProvider.SKINS_PATH.toFile());
    }

    private void refreshPreview() {
        String compiled = info.compile();

        if (!compiled.isEmpty()) {
            entity.setCustomName(Component.literal(compiled));
        } else {
            entity.setCustomName(null);
        }
    }

    private void accept() {
        String compiled = info.compile();

        Minecraft.getInstance().setScreenAndShow(parent);

        EditBox nameInput =
                ((com.danrus.pas.mixin.accessors.AnvilScreenAccessor) parent).pas$getNameInput();

        nameInput.setValue(compiled);

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(
                    new net.minecraft.network.protocol.game.ServerboundRenameItemPacket(compiled)
            );
        }
    }

    private void debounceRefresh() {
        if (refreshDebounceFuture != null) {
            refreshDebounceFuture.cancel(false);
        }

        refreshDebounceFuture = CompletableFuture.runAsync(this::refreshPreview, delayedExecutor);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component providerLabel(String provider) {
        return Component.translatable("pas.menu.tab.skin.provider." + provider.toLowerCase());
    }

    private Component armLabel() {
        return Component.translatable(
                "pas.menu.tab.skin.arm_type." + (info.isSlim() ? "slim" : "wide")
        );
    }

    private Component capeEnabledLabel() {
        return Component.translatable(
                "pas.menu.tab.cape." + (info.hasCape() ? "yes" : "no")
        );
    }

    private Component capeProviderLabel() {
        return Component.translatable(
                "pas.menu.tab.cape.provider." + info.capeProvider().toLowerCase()
        );
    }

    private static class BlendSlider extends AbstractSliderButton {
        private final Consumer<Integer> onChange;

        BlendSlider(int x, int y, int w, int h, int value, Consumer<Integer> onChange) {
            super(x, y, w, h, Component.literal(value + "%"), value / 100.0);
            this.onChange = onChange;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(getValue() + "%"));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getValue());
        }

        int getValue() {
            return (int) (value * 100);
        }
    }

    private static class TabButton extends Button {
        private boolean isActiveTab;

        public TabButton(int x, int y, int w, int h, Component msg, OnPress press, boolean isActiveTab) {
            super(x, y, w, h, msg, press, DEFAULT_NARRATION);
            this.isActiveTab = isActiveTab;
        }

        public void setActiveTab(boolean activeTab) {
            this.isActiveTab = activeTab;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Minecraft minecraft = Minecraft.getInstance();
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    TAB_BUTTON_SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ModUtils.getARGBwhite(this.alpha));
            this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), this.getMessage(), 2);
        }
    }

    public static class TextWidget extends AbstractWidget {
        private boolean hasTooltip = false;
        public TextWidget(int x, int y, int width, int height, Component message) {
            super(x, y, width, height, message);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.centeredText(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2 - (hasTooltip ? 4 : 0), this.getY() + (this.getHeight() - Minecraft.getInstance().font.lineHeight) / 2, 16777215 | (int) (this.alpha * 255) << 24);
            if (hasTooltip) {
                guiGraphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, QUESTION_MARK_ICON, this.getX() + Minecraft.getInstance().font.width(getMessage()) / 3 + this.getWidth() / 2 + 13, this.getY() + (this.getHeight() - Minecraft.getInstance().font.lineHeight) / 2 - 1, 9, 9);
            }
        }


        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
        }

        public TextWidget setTooltip(Component message) {
            Tooltip tooltip = Tooltip.create(message);
            this.setTooltip(tooltip);
            this.hasTooltip = true;
            return this;
        }
    }
}