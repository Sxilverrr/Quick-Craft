package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.network.Buf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HarvestSources {
    private static final int ROLLS = 16;
    private static final int MAX_ITEMS = 8192;
    private static final int MAX_BLOCKS = 16;

    private HarvestSources() {
    }

    public static IBlockState mature(Block block) {
        IBlockState state = block.getDefaultState();
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (!(property instanceof PropertyInteger) || !"age".equals(property.getName())) continue;
            PropertyInteger age = (PropertyInteger) property;
            return state.withProperty(age, Collections.max(age.getAllowedValues()));
        }
        return null;
    }

    public static Map<ItemKey, List<IBlockState>> scan(World world, BlockPos pos) {
        Map<ItemKey, List<IBlockState>> out = new HashMap<ItemKey, List<IBlockState>>();
        for (Block block : ForgeRegistries.BLOCKS) {
            IBlockState state = mature(block);
            if (state == null) continue;
            Set<ItemKey> drops = new LinkedHashSet<ItemKey>();
            try {
                for (int i = 0; i < ROLLS; i++) {
                    NonNullList<ItemStack> rolled = NonNullList.create();
                    block.getDrops(rolled, world, pos, state, 0);
                    for (ItemStack stack : rolled) {
                        if (stack != null && !stack.isEmpty()) drops.add(ItemKey.of(stack));
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            for (ItemKey key : drops) {
                List<IBlockState> plants = out.get(key);
                if (plants == null) out.put(key, plants = new ArrayList<IBlockState>());
                plants.add(state);
            }
        }
        return out;
    }

    public static void write(ByteBuf buf, Map<ItemKey, List<IBlockState>> sources) {
        int count = Math.min(MAX_ITEMS, sources.size());
        buf.writeInt(count);
        int written = 0;
        for (Map.Entry<ItemKey, List<IBlockState>> entry : sources.entrySet()) {
            if (written++ >= count) break;
            Buf.writeStack(buf, entry.getKey().toStack(1));
            int blocks = Math.min(MAX_BLOCKS, entry.getValue().size());
            buf.writeInt(blocks);
            for (int b = 0; b < blocks; b++) buf.writeInt(Block.getStateId(entry.getValue().get(b)));
        }
    }

    public static Map<ItemKey, List<IBlockState>> read(ByteBuf buf) {
        int count = Math.min(MAX_ITEMS, buf.readInt());
        Map<ItemKey, List<IBlockState>> out = new HashMap<ItemKey, List<IBlockState>>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = Buf.readStack(buf);
            int blocks = Math.min(MAX_BLOCKS, buf.readInt());
            List<IBlockState> plants = new ArrayList<IBlockState>();
            for (int b = 0; b < blocks; b++) {
                IBlockState state = Block.getStateById(buf.readInt());
                if (state.getBlock() != Blocks.AIR) plants.add(state);
            }
            if (!stack.isEmpty() && !plants.isEmpty()) out.put(ItemKey.of(stack), plants);
        }
        return out;
    }
}
