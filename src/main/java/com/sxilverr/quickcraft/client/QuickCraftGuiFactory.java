package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.QuickCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QuickCraftGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft mc) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent) {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        for (ConfigCategory category : QuickCraftConfig.guiCategories()) elements.add(new ConfigElement(category));
        return new GuiConfig(parent, elements, QuickCraft.MODID, false, false, QuickCraft.NAME,
                GuiConfig.getAbridgedConfigPath(QuickCraftConfig.fileName()));
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}
