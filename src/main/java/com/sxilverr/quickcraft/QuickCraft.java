package com.sxilverr.quickcraft;

import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = QuickCraft.MODID,
        name = QuickCraft.NAME,
        version = QuickCraft.VERSION,
        acceptedMinecraftVersions = "[1.12,1.13)",
        guiFactory = "com.sxilverr.quickcraft.client.QuickCraftGuiFactory",
        dependencies = "after:jei@[4.15.0,);after:appliedenergistics2;after:refinedstorage;after:projecte;after:jeresources;after:avaritia")
public class QuickCraft {
    public static final String MODID = "quickcraft";
    public static final String NAME = "Quick Craft";
    public static final String VERSION = "@MOD_VERSION@";

    @SidedProxy(clientSide = "com.sxilverr.quickcraft.ClientProxy",
            serverSide = "com.sxilverr.quickcraft.CommonProxy")
    public static CommonProxy proxy;

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        QuickCraftConfig.load(event.getSuggestedConfigurationFile());
        QuickCraftNetwork.register();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        ServerRecipeCache.clear();
    }
}
