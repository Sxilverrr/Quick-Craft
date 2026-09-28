package com.sxilverr.quickcraft.forge;

import com.sxilverr.quickcraft.config.QuickCraftConfig;
import com.sxilverr.quickcraft.craft.EmcSource;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.integration.jer.MobItemSource;
import com.sxilverr.quickcraft.forge.integration.jer.JerIntegration;
import com.sxilverr.quickcraft.forge.integration.projecte.EmcSession;
import com.sxilverr.quickcraft.integration.projecte.ProjectEIntegration;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import com.sxilverr.quickcraft.platform.IPlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public ItemStack getCraftingRemainder(ItemStack stack) {
        return stack.getCraftingRemainingItem();
    }

    @Override
    public void sendCraftRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                 Map<String, Item> ingredientChoices, String destinationId) {
        QuickCraftNetwork.sendCraftRequest(target, quantity, overrides, ingredientChoices, destinationId);
    }

    @Override
    public void sendCraftPreviewRequest(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                        Map<String, Item> ingredientChoices) {
        QuickCraftNetwork.sendCraftPreviewRequest(target, quantity, overrides, ingredientChoices);
    }

    @Override
    public void requestDepositTargets() {
        QuickCraftNetwork.requestDepositTargets();
    }

    @Override
    public void requestAvailability(Collection<ItemKey> keys) {
        QuickCraftNetwork.requestAvailability(keys);
    }

    @Override
    public EmcSource openEmc(Player player, int range) {
        if (player == null || !QuickCraftConfig.useProjectEEmc() || !ProjectEIntegration.available()) return null;
        return EmcSession.openClient(player, range);
    }

    @Override
    public List<MobItemSource> mobSources(Item item) {
        return JerIntegration.sourcesFor(item);
    }
}
