package com.sxilverr.quickcraft.neoforge;

import com.sxilverr.quickcraft.config.QuickCraftClientConfig;
import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.config.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.neoforge.client.NeoForgeClientEvents;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(QuickCraftCommon.MODID)
public final class QuickCraftNeoForge {

    public QuickCraftNeoForge(IEventBus modBus, ModContainer container) {
        NeoForgeConfigBuilder common = new NeoForgeConfigBuilder();
        QuickCraftConfig.define(common);
        container.registerConfig(ModConfig.Type.COMMON, common.build());
        NeoForgeConfigBuilder client = new NeoForgeConfigBuilder();
        QuickCraftClientConfig.define(client);
        container.registerConfig(ModConfig.Type.CLIENT, client.build());
        modBus.addListener(QuickCraftNetwork::register);
        NeoForge.EVENT_BUS.addListener(QuickCraftNeoForge::onServerStopped);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForgeClientEvents.init(modBus);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ServerRecipeCache.clear();
    }
}
