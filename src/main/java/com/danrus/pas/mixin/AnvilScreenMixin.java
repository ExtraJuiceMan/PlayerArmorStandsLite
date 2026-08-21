package com.danrus.pas.mixin;

import com.danrus.pas.mixin.accessors.ScreenAccessor;
import com.danrus.pas.render.gui.PasConfiguratorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @Unique private Button pas$configuratorButton;

    // Debouncer fields
    @Unique private boolean pas$skipDebounce = false;
    @Unique private ScheduledFuture<?> pas$debounceFuture;
    @Unique private static final ScheduledExecutorService pas$debounceExecutor = Executors.newSingleThreadScheduledExecutor();

    @Inject(method = "subInit", at = @At("TAIL"))
    private void pas$addConfiguratorButton(CallbackInfo ci) {
        AnvilScreen self = (AnvilScreen) (Object) this;
        if (pas$configuratorButton == null) {
            pas$configuratorButton = Button.builder(
                    Component.translatable("pas.buttons.configurator"),
                    b -> {
                        String currentName = ((com.danrus.pas.mixin.accessors.AnvilScreenAccessor) self)
                                .pas$getNameInput().getValue();
                        Minecraft.getInstance().setScreenAndShow(new PasConfiguratorScreen(self, currentName));
                    }
            ).bounds(0, 0, 150, 20).build();
        }

        ((ScreenAccessor) this).invokeAddRenderableWidget(pas$configuratorButton);
        int i = (self.width - 150) / 2;
        int j = self.height / 2 + 87;
        pas$configuratorButton.setPosition(i, j);

        ItemStack slot0Item = self.getMenu().getSlot(0).getItem();
        boolean hasArmorStand = slot0Item.getItem() == Items.ARMOR_STAND;
        pas$configuratorButton.visible = hasArmorStand;
        pas$configuratorButton.active = hasArmorStand;
    }

    @Inject(method = "slotChanged", at = @At("HEAD"))
    private void pas$onSlotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack, CallbackInfo ci) {
        if (pas$configuratorButton == null) return;
        if (slotIndex == 0) {
            boolean hasArmorStand = itemStack.getItem() == Items.ARMOR_STAND;
            pas$configuratorButton.visible = hasArmorStand;
            pas$configuratorButton.active = hasArmorStand;
        }
    }

    @Inject(method = "onNameChanged", at = @At("HEAD"), cancellable = true)
    private void pas$debounceNameChange(String name, CallbackInfo ci) {
        if (pas$skipDebounce) {
            return;
        }

        ci.cancel();

        if (pas$debounceFuture != null) {
            pas$debounceFuture.cancel(false);
        }

        pas$debounceFuture = pas$debounceExecutor.schedule(() -> {
            Minecraft.getInstance().execute(() -> {
                AnvilScreen self = (AnvilScreen) (Object) this;
                if (Minecraft.getInstance().gui.screen() != self) return;

                pas$skipDebounce = true;
                String currentText = ((com.danrus.pas.mixin.accessors.AnvilScreenAccessor) self).pas$getNameInput().getValue();
                ((com.danrus.pas.mixin.accessors.AnvilScreenAccessor) self).pas$invokeOnNameChanged(currentText);
                pas$skipDebounce = false;
            });
        }, 300, TimeUnit.MILLISECONDS);
    }
}