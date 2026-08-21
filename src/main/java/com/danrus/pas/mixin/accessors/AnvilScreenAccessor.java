package com.danrus.pas.mixin.accessors;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AnvilScreen.class)
public interface AnvilScreenAccessor {
    @Accessor("name")
    EditBox pas$getNameInput();

    @Invoker("onNameChanged")
    void pas$invokeOnNameChanged(String name);
}
