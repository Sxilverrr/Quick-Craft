package com.sxilverr.quickcraft.forge.integration.projecte;

import com.sxilverr.quickcraft.integration.projecte.ProjectEIntegration;
import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.craft.EmcSource;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.config.QuickCraftConfig;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EmcSession implements EmcSource {
    private final IKnowledgeProvider provider;
    private final boolean fullKnowledge;
    private final Player player;
    private final ServerPlayer serverPlayer;

    private EmcSession(IKnowledgeProvider provider, Player player, ServerPlayer serverPlayer) {
        this.provider = provider;
        this.fullKnowledge = safeFullKnowledge(provider);
        this.player = player;
        this.serverPlayer = serverPlayer;
    }

    public static EmcSession open(ServerPlayer player, int range) {
        IKnowledgeProvider kp = providerFor(player);
        if (kp == null) return null;
        if (!ProjectEIntegration.hasAccess(player, player.serverLevel(), player.blockPosition(), range)) return null;
        return new EmcSession(kp, player, player);
    }

    public static EmcSession openClient(Player player, int range) {
        if (player == null) return null;
        IKnowledgeProvider kp = providerFor(player);
        if (kp == null) return null;
        if (!ProjectEIntegration.hasAccess(player, player.level(), player.blockPosition(), range)) return null;
        return new EmcSession(kp, player, null);
    }

    private static IKnowledgeProvider providerFor(Player player) {
        try {
            return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean safeFullKnowledge(IKnowledgeProvider provider) {
        try {
            return provider.hasFullKnowledge();
        } catch (Throwable t) {
            return false;
        }
    }

    public BigInteger emc() {
        BigInteger total = knowledgeEmc();
        if (QuickCraftConfig.useKleinStarEmc()) total = total.add(EmcHolders.stored(player));
        return total;
    }

    private BigInteger knowledgeEmc() {
        try {
            BigInteger value = provider.getEmc();
            return value == null ? BigInteger.ZERO : value;
        } catch (Throwable t) {
            return BigInteger.ZERO;
        }
    }

    public long value(ItemStack stack) {
        try {
            return IEMCProxy.INSTANCE.getValue(stack);
        } catch (Throwable t) {
            return 0L;
        }
    }

    public long sellValue(ItemStack stack) {
        try {
            return IEMCProxy.INSTANCE.getSellValue(stack);
        } catch (Throwable t) {
            return 0L;
        }
    }

    public boolean learned(ItemStack stack) {
        try {
            return fullKnowledge || provider.hasKnowledge(stack);
        } catch (Throwable t) {
            return false;
        }
    }

    public Map<ItemKey, Long> values(Set<ItemKey> keys) {
        Map<ItemKey, Long> values = new HashMap<>();
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
        boolean emcChanged = bank != null && bank.changed() && settle(bank.remaining());
        List<ItemInfo> learned = new ArrayList<>();
        if (!fullKnowledge && producedKeys != null) {
            for (ItemKey key : producedKeys) {
                ItemInfo info = learnable(key.toStack(1));
                if (info != null && learn(info)) learned.add(info);
            }
        }
        try {
            if (emcChanged) provider.syncEmc(serverPlayer);
            for (ItemInfo info : learned) provider.syncKnowledgeChange(serverPlayer, info, true);
        } catch (Throwable ignored) {
        }
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
        try {
            provider.setEmc(knowledge);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private ItemInfo learnable(ItemStack stack) {
        try {
            if (value(stack) <= 0L) return null;
            ItemInfo source = ItemInfo.fromStack(stack);
            ItemInfo reduced = IEMCProxy.INSTANCE.getPersistentInfo(source);
            if (provider.hasKnowledge(reduced)) return null;
            if (MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(serverPlayer, source, reduced))) return null;
            return reduced;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean learn(ItemInfo info) {
        try {
            return provider.addKnowledge(info);
        } catch (Throwable t) {
            return false;
        }
    }
}
