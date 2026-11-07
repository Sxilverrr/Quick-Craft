package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.util.Reflect;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ProjectESupport {
    public static final String MODID = "projecte";
    private static final String PROJECTEX_MODID = "projectex";
    private static final String BAUBLES_MODID = "baubles";

    private static boolean resolved;
    private static boolean available;

    private static Capability<?> knowledgeCapability;
    private static Capability<?> bagCapability;
    private static Object emcProxy;
    private static Method proxyGetValue;
    private static Method proxyGetSellValue;
    private static Method getEmc;
    private static Method setEmc;
    private static Method hasKnowledge;
    private static Method addKnowledge;
    private static Method hasFullKnowledge;
    private static Method sync;
    private static Method bagGetBag;
    private static Method bagSync;
    private static Class<?> bagItemClass;
    private static Class<?> emcItemClass;
    private static Method emcStored;
    private static Method emcExtract;
    private static Constructor<?> learnEvent;
    private static Method baublesHandler;

    private ProjectESupport() {
    }

    public static synchronized boolean available() {
        if (!resolved) {
            resolved = true;
            available = resolve();
        }
        return available;
    }

    private static boolean resolve() {
        Class<?> api = Reflect.cls("moze_intel.projecte.api.ProjectEAPI");
        if (api == null) return false;

        Object cap = Reflect.get(Reflect.field(api, "KNOWLEDGE_CAPABILITY"), null);
        if (!(cap instanceof Capability)) return false;
        knowledgeCapability = (Capability<?>) cap;

        emcProxy = Reflect.invoke(Reflect.method(api, "getEMCProxy"), null);
        if (emcProxy == null) return false;
        proxyGetValue = Reflect.method(emcProxy.getClass(), "getValue", ItemStack.class);
        if (proxyGetValue == null) proxyGetValue = Reflect.methodByName(emcProxy.getClass(), "getValue", 1);
        proxyGetSellValue = Reflect.method(emcProxy.getClass(), "getSellValue", ItemStack.class);

        Class<?> provider = Reflect.cls("moze_intel.projecte.api.capabilities.IKnowledgeProvider");
        getEmc = Reflect.methodByName(provider, "getEmc", 0);
        setEmc = Reflect.method(provider, "setEmc", long.class);
        if (setEmc == null) setEmc = Reflect.methodByName(provider, "setEmc", 1);
        hasKnowledge = Reflect.methodByName(provider, "hasKnowledge", 1);
        addKnowledge = Reflect.methodByName(provider, "addKnowledge", 1);
        hasFullKnowledge = Reflect.methodByName(provider, "hasFullKnowledge", 0);
        sync = Reflect.methodByName(provider, "sync", 1);

        Object bagCap = Reflect.get(Reflect.field(api, "ALCH_BAG_CAPABILITY"), null);
        if (bagCap instanceof Capability) bagCapability = (Capability<?>) bagCap;
        Class<?> bagProvider = Reflect.cls("moze_intel.projecte.api.capabilities.IAlchBagProvider");
        bagGetBag = Reflect.methodByName(bagProvider, "getBag", 1);
        bagSync = Reflect.methodByName(bagProvider, "sync", 2);
        bagItemClass = Reflect.cls("moze_intel.projecte.gameObjs.items.AlchemicalBag");

        emcItemClass = Reflect.cls("moze_intel.projecte.api.item.IItemEmc");
        emcStored = Reflect.method(emcItemClass, "getStoredEmc", ItemStack.class);
        emcExtract = Reflect.method(emcItemClass, "extractEmc", ItemStack.class, long.class);

        learnEvent = learnEventConstructor();

        return proxyGetValue != null && getEmc != null && setEmc != null
                && hasKnowledge != null && addKnowledge != null;
    }

    private static Constructor<?> learnEventConstructor() {
        Class<?> event = Reflect.cls("moze_intel.projecte.api.event.PlayerAttemptLearnEvent");
        if (event == null) return null;
        try {
            return event.getConstructor(EntityPlayer.class, ItemStack.class);
        } catch (Throwable t) {
            return null;
        }
    }

    static Object knowledgeProvider(EntityPlayer player) {
        if (!available() || knowledgeCapability == null || player == null) return null;
        try {
            if (!player.hasCapability(knowledgeCapability, null)) return null;
            return player.getCapability(knowledgeCapability, null);
        } catch (Throwable t) {
            return null;
        }
    }

    static long value(ItemStack stack) {
        return Reflect.longValue(Reflect.invoke(proxyGetValue, emcProxy, stack), 0L);
    }

    static long sellValue(ItemStack stack) {
        if (proxyGetSellValue == null) return value(stack);
        return Reflect.longValue(Reflect.invoke(proxyGetSellValue, emcProxy, stack), 0L);
    }

    static long emc(Object provider) {
        return Reflect.longValue(Reflect.invoke(getEmc, provider), 0L);
    }

    static void setEmc(Object provider, long value) {
        Reflect.invoke(setEmc, provider, value);
    }

    static boolean hasKnowledge(Object provider, ItemStack stack) {
        return Reflect.boolValue(Reflect.invoke(hasKnowledge, provider, stack), false);
    }

    static boolean addKnowledge(Object provider, ItemStack stack) {
        return Reflect.boolValue(Reflect.invoke(addKnowledge, provider, stack), false);
    }

    static boolean fullKnowledge(Object provider) {
        return Reflect.boolValue(Reflect.invoke(hasFullKnowledge, provider), false);
    }

    static void sync(Object provider, EntityPlayerMP player) {
        Reflect.invoke(sync, provider, player);
    }

    static boolean learnCancelled(EntityPlayerMP player, ItemStack stack) {
        if (learnEvent == null || player == null) return false;
        try {
            Object event = learnEvent.newInstance(player, stack);
            return event instanceof Event && MinecraftForge.EVENT_BUS.post((Event) event);
        } catch (Throwable t) {
            return false;
        }
    }

    static Object bagProvider(EntityPlayer player) {
        if (!available() || bagCapability == null || player == null) return null;
        try {
            if (!player.hasCapability(bagCapability, null)) return null;
            return player.getCapability(bagCapability, null);
        } catch (Throwable t) {
            return null;
        }
    }

    static IItemHandler bag(Object provider, EnumDyeColor color) {
        Object handler = Reflect.invoke(bagGetBag, provider, color);
        return handler instanceof IItemHandler ? (IItemHandler) handler : null;
    }

    static void syncBag(Object provider, EnumDyeColor color, EntityPlayerMP player) {
        Reflect.invoke(bagSync, provider, color, player);
    }

    static boolean isBag(ItemStack stack) {
        return bagItemClass != null && stack != null && !stack.isEmpty() && bagItemClass.isInstance(stack.getItem());
    }

    static boolean isEmcHolder(ItemStack stack) {
        return emcItemClass != null && stack != null && !stack.isEmpty() && emcItemClass.isInstance(stack.getItem());
    }

    static long storedEmc(ItemStack stack) {
        return Reflect.longValue(Reflect.invoke(emcStored, stack.getItem(), stack), 0L);
    }

    static long extractEmc(ItemStack stack, long amount) {
        return Reflect.longValue(Reflect.invoke(emcExtract, stack.getItem(), stack, amount), 0L);
    }

    public static boolean hasAccess(EntityPlayer player, World world, BlockPos center, int range) {
        return QuickCraftConfig.emcWithoutTransmutation() || !findTablet(player).isEmpty() || findTable(world, center, range) != null;
    }

    public static ItemStack findTablet(EntityPlayer player) {
        if (player == null) return ItemStack.EMPTY;
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (isTransmutationTablet(stack)) return stack;
        }
        return findBaubleTablet(player);
    }

    private static ItemStack findBaubleTablet(EntityPlayer player) {
        IItemHandler handler = baublesHandler(player);
        if (handler == null) return ItemStack.EMPTY;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (isTransmutationTablet(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static IItemHandler baublesHandler(EntityPlayer player) {
        if (!Reg.loaded(BAUBLES_MODID)) return null;
        if (baublesHandler == null) {
            Class<?> api = Reflect.cls("baubles.api.BaublesApi");
            if (api == null) return null;
            baublesHandler = Reflect.method(api, "getBaublesHandler", EntityPlayer.class);
            if (baublesHandler == null) return null;
        }
        Object handler = Reflect.invoke(baublesHandler, null, player);
        return handler instanceof IItemHandler ? (IItemHandler) handler : null;
    }

    public static Block tableBlock() {
        return Reg.block("projecte:transmutation_table");
    }

    public static BlockPos findTable(World world, BlockPos center, int range) {
        if (world == null || center == null || range <= 0) return null;
        Block table = tableBlock();
        if (table == null) return null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!world.isBlockLoaded(cursor)) continue;
                    if (world.getBlockState(cursor).getBlock() == table) return cursor.toImmutable();
                }
            }
        }
        return null;
    }

    private static boolean isTransmutationTablet(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = Reg.idOf(stack.getItem());
        if (id == null) return false;
        if (PROJECTEX_MODID.equals(id.getNamespace())) return id.getPath().equals("arcane_tablet");
        if (!MODID.equals(id.getNamespace())) return false;
        if (id.getPath().contains("transmutation_tablet")) return true;
        String key = stack.getItem().getTranslationKey(stack);
        return key != null && key.contains("transmutation_tablet");
    }
}
