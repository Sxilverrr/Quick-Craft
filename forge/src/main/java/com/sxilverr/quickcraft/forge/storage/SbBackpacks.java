package com.sxilverr.quickcraft.forge.storage;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.lang.reflect.Method;

public final class SbBackpacks {
    private static final Method RUN_ON_BACKPACKS = lookup();

    public interface Visitor {
        void visit(ItemStack backpack, IBackpackWrapper wrapper, String inventoryName, int slot);
    }

    private SbBackpacks() {
    }

    public static void forEach(Player player, Visitor visitor) {
        if (RUN_ON_BACKPACKS == null) return;
        try {
            PlayerInventoryProvider.BackpackInventorySlotConsumer consumer = (backpack, inventoryName, identifier, slot) -> {
                IBackpackWrapper wrapper = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
                        .resolve().orElse(null);
                if (wrapper != null) visitor.visit(backpack, wrapper, inventoryName, slot);
                return false;
            };
            RUN_ON_BACKPACKS.invoke(PlayerInventoryProvider.get(), player, consumer);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }
    }

    private static Method lookup() {
        try {
            return PlayerInventoryProvider.class.getMethod("runOnBackpacks", Player.class,
                    PlayerInventoryProvider.BackpackInventorySlotConsumer.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
