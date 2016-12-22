package toast.specialAI.util;

import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import toast.specialAI.ModSpecialAI;

public abstract class BlockHelper {

    // Returns true if the mob should detroy the block.
    public static boolean shouldDamage(IBlockState block, EntityLiving entity, boolean needsTool, World world, BlockPos pos) {
        ItemStack held = entity.getHeldItemMainhand();
        return block.getBlockHardness(world, pos) >= 0.0F && (!needsTool || block.getMaterial().isToolNotRequired() || held != null && ForgeHooks.canToolHarvestBlock(world, pos, held));
    }

    // Returns the amount of damage to deal to a block.
    public static float getDamageAmount(IBlockState block, EntityLiving entity, World world, BlockPos pos) {
        float hardness = block.getBlockHardness(world, pos);
        if (hardness < 0.0F)
            return 0.0F;

        if (!BlockHelper.canHarvestBlock(entity.getHeldItemMainhand(), block))
            return 1.0F / hardness / 100.0F;
        return BlockHelper.getCurrentStrengthVsBlock(entity, block) / hardness / 30.0F;
    }

    // Returns whether the item can harvest the specified block.
    public static boolean canHarvestBlock(ItemStack itemStack, IBlockState block) {
        return block.getMaterial().isToolNotRequired() || itemStack != null && itemStack.canHarvestBlock(block);
    }

    // Returns the mob's strength vs. the given block.
    public static float getCurrentStrengthVsBlock(EntityLiving entity, IBlockState block) {
        ItemStack held = entity.getHeldItemMainhand();
        float strength = held == null ? 1.0F : held.getStrVsBlock(block);

        if (strength > 1.0F) {
            int efficiency = EnchantmentHelper.getEfficiencyModifier(entity);
            if (efficiency > 0 && held != null) {
                strength += efficiency * efficiency + 1;
            }
        }

        if (entity.isPotionActive(MobEffects.HASTE)) {
            strength *= 1.0F + (entity.getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2F;
        }
        if (entity.isPotionActive(MobEffects.MINING_FATIGUE)) {
            strength *= 1.0F - (entity.getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier() + 1) * 0.2F;
        }

        if (entity.isInsideOfMaterial(Material.WATER) && !EnchantmentHelper.getAquaAffinityModifier(entity)) {
            strength /= 5.0F;
        }
        if (!entity.onGround) {
            strength /= 5.0F;
        }

        return strength < 0.0F ? 0.0F : strength;
    }

    // Returns a new target block set from the string property.
	public static HashSet<TargetBlock> newBlockSet(String line) {
		return BlockHelper.newBlockSet(line.split(","));
	}
    public static HashSet<TargetBlock> newBlockSet(String[] targetableBlocks) {
        HashSet<TargetBlock> blockSet = new HashSet<TargetBlock>();
        String[] pair, modCheck;
        TargetBlock targetableBlock;
        for (String target : targetableBlocks) {
            pair = target.split(" ", 2);
            if (pair.length > 1) {
                targetableBlock = new TargetBlock(BlockHelper.getStringAsBlock(pair[0]), Integer.parseInt(pair[1].trim()));
            }
            else {
                if (pair[0].endsWith("*")) {
                    BlockHelper.addAllModBlocks(blockSet, pair[0].substring(pair[0].length() - 1));
                    continue;
                }

                targetableBlock = new TargetBlock(BlockHelper.getStringAsBlock(pair[0]), -1);
            }

            if (targetableBlock.BLOCK == null || targetableBlock.BLOCK == Blocks.AIR) {
                continue;
            }

            if (targetableBlock.BLOCK_DATA < 0) {
                while (blockSet.contains(targetableBlock)) {
                    blockSet.remove(targetableBlock);
                }
            }
            blockSet.add(targetableBlock);
        }
        return blockSet;
    }
    private static IBlockState getStringAsBlock(String id) {
        Block block = Block.REGISTRY.getObject(new ResourceLocation(id));
        if (block == null || block == Blocks.AIR) {
            try {
                block = Block.getBlockById(Integer.parseInt(id));
                if (block != null && block != Blocks.AIR) {
                    ModSpecialAI.logWarning("Usage of numerical block id! (" + id + ")");
                }
            }
            catch (NumberFormatException numberformatexception) {
                // Do nothing
            }
        }
        if (block == null) {
        	block = Blocks.AIR;
        }
        if (block == Blocks.AIR) {
            ModSpecialAI.logWarning("Missing or invalid block! (" + id + ")");
        }
        return block.getDefaultState();
    }
    private static void addAllModBlocks(HashSet<TargetBlock> blockSet, String namespace) {
        try {
            TargetBlock targetableBlock;
            for (ResourceLocation blockId : Block.REGISTRY.getKeys()) {
                if (blockId.toString().startsWith(namespace)) {
                    targetableBlock = new TargetBlock(BlockHelper.getStringAsBlock(blockId.toString()), -1);
                    if (targetableBlock.BLOCK == null || targetableBlock.BLOCK == Blocks.AIR) {
                        continue;
                    }
                    while (blockSet.contains(targetableBlock)) {
                        blockSet.remove(targetableBlock);
                    }
                    blockSet.add(targetableBlock);
                }
            }
        }
        catch (Exception ex) {
            ModSpecialAI.logError("Caught exception while adding namespace! (" + namespace + "*)");
        }
    }
}