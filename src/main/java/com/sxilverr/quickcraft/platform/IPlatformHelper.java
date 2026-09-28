package com.sxilverr.quickcraft.platform;

import com.sxilverr.quickcraft.craft.EmcSource;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.integration.jer.MobItemSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IPlatformHelper {

    boolean isModLoaded(String modId);

    Path getConfigDir();

    ItemStack getCraftingRemainder(ItemStack stack);

    void sendCraftRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                          Map<String, Item> ingredientChoices, String destinationId);

    void sendCraftPreviewRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                 Map<String, Item> ingredientChoices);

    void requestDepositTargets();

    void requestAvailability(Collection<ItemKey> keys);

    EmcSource openEmc(Player player, int range);

    List<MobItemSource> mobSources(Item item);
}
