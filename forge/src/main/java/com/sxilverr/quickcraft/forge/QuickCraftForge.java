package com.sxilverr.quickcraft.forge;

import com.sxilverr.quickcraft.config.QuickCraftClientConfig;
import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.config.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ServerRecipeCache.clear();
    }
}
