package com.sxilverr.quickcraft.forge;

import com.sxilverr.quickcraft.config.QuickCraftClientConfig;
import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.config.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.HarvestSources;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;
import java.util.Map;

@Mod(QuickCraftCommon.MODID)
public class QuickCraftForge {

    public QuickCraftForge(FMLJavaModLoadingContext context) {
        ForgeConfigBuilder common = new ForgeConfigBuilder();
        QuickCraftConfig.define(common);
        context.registerConfig(ModConfig.Type.COMMON, common.build());
        ForgeConfigBuilder client = new ForgeConfigBuilder();
        QuickCraftClientConfig.define(client);
        context.registerConfig(ModConfig.Type.CLIENT, client.build());
        QuickCraftNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(QuickCraftForge::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(QuickCraftForge::onDatapackSync);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ServerRecipeCache.clear();
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> players = event.getPlayer() != null ? List.of(event.getPlayer()) : event.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer first = players.get(0);
        Map<Item, List<Block>> sources = HarvestSources.scan(first.serverLevel(), first.blockPosition());
        for (ServerPlayer player : players) QuickCraftNetwork.sendHarvestSources(player, sources);
    }
}
