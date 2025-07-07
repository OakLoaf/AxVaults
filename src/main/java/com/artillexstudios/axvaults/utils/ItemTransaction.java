package com.artillexstudios.axvaults.utils;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ItemTransaction(@Nullable ItemStack incoming, @Nullable ItemStack outgoing) {

    public boolean isEmpty() {
        return incoming == null && outgoing == null;
    }
}
