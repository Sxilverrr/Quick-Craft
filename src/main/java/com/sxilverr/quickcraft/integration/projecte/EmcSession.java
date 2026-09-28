package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.craft.EmcSource;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EmcSession implements EmcSource {
    private static final BigInteger LONG_CAP = BigInteger.valueOf(Long.MAX_VALUE);

    private final Object provider;
    private final boolean fullKnowledge;
    private final EntityPlayer player;
    private final EntityPlayerMP serverPlayer;

    private EmcSession(Object provider, EntityPlayer player, EntityPlayerMP serverPlayer) {
        this.provider = provider;
        this.fullKnowledge = ProjectESupport.fullKnowledge(provider);
        this.player = player;
        this.serverPlayer = serverPlayer;
    }

    public static EmcSession open(EntityPlayerMP player, int range) {
        Object provider = ProjectESupport.knowledgeProvider(player);
        if (provider == null) return null;
        if (!ProjectESupport.hasAccess(player, player.world, player.getPosition(), range)) return null;
        return new EmcSession(provider, player, player);
    }

    public static EmcSession openClient(EntityPlayer player, int range) {
        if (player == null) return null;
        Object provider = ProjectESupport.knowledgeProvider(player);
        if (provider == null) return null;
        if (!ProjectESupport.hasAccess(player, player.world, player.getPosition(), range)) return null;
        return new EmcSession(provider, player, null);
    }

    public BigInteger emc() {
        BigInteger total = BigInteger.valueOf(Math.max(0L, ProjectESupport.emc(provider)));
        if (QuickCraftConfig.useKleinStarEmc()) total = total.add(EmcHolders.stored(player));
        return total;
    }

    public long value(ItemStack stack) {
        return ProjectESupport.value(stack);
    }

    public long sellValue(ItemStack stack) {
        return ProjectESupport.sellValue(stack);
    }

    public boolean learned(ItemStack stack) {
        return fullKnowledge || ProjectESupport.hasKnowledge(provider, stack);
    }

    public Map<ItemKey, Long> values(Set<ItemKey> keys) {
        Map<ItemKey, Long> values = new HashMap<ItemKey, Long>();
        for (ItemKey key : keys) {
            ItemStack stack = key.toStack(1);
            if (!learned(stack)) continue;
            long v = value(stack);
            if (v > 0L) values.put(key, v);
        }
        return values;
    }

    public EmcBank bank(Set<ItemKey> keys, BigInteger budget) {
        return new EmcBank(values(keys), budget);
    }

    public void apply(EmcBank bank, Set<ItemKey> producedKeys) {
        if (serverPlayer == null) return;
        boolean changed = bank != null && bank.changed() && settle(bank.remaining());
        if (!fullKnowledge && producedKeys != null) {
            for (ItemKey key : producedKeys) {
                ItemStack stack = key.toStack(1);
                if (value(stack) <= 0L || ProjectESupport.hasKnowledge(provider, stack)) continue;
                if (ProjectESupport.learnCancelled(serverPlayer, stack)) continue;
                if (ProjectESupport.addKnowledge(provider, stack)) changed = true;
            }
        }
        if (changed) ProjectESupport.sync(provider, serverPlayer);
    }

    private boolean settle(BigInteger remaining) {
        if (remaining.signum() < 0) remaining = BigInteger.ZERO;
        BigInteger knowledge = remaining;
        if (QuickCraftConfig.useKleinStarEmc()) {
            knowledge = remaining.subtract(EmcHolders.stored(player));
            if (knowledge.signum() < 0) {
                EmcHolders.drain(player, knowledge.negate());
                knowledge = BigInteger.ZERO;
            }
        }
        if (knowledge.compareTo(LONG_CAP) > 0) knowledge = LONG_CAP;
        ProjectESupport.setEmc(provider, knowledge.longValue());
        return true;
    }
}
