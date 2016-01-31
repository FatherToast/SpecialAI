package toast.specialAI.util;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import toast.specialAI.Properties;
import toast.specialAI._SpecialAI;

public abstract class BlockHelper {
    // Useful properties for this class.
    public static final float SPEED_MULT = (float) Properties.getDouble(Properties.GENERAL, "break_speed");

    // Returns true if the mob should detroy the block.
    public static boolean shouldDamage(Block block, EntityLiving entity, boolean needsTool, World world, int x, int y, int z) {
        ItemStack held = entity.getHeldItem();
        int metadata = world.getBlockMetadata(x, y, z);
        return block.getBlockHardness(world, x, y, z) >= 0.0F && (!needsTool || block.getMaterial().isToolNotRequired() || held != null && ForgeHooks.canToolHarvestBlock(block, metadata, held));
    }

    // Returns the amount of damage to deal to a block.
    public static float getDamageAmount(Block block, EntityLiving entity, World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        float hardness = block.getBlockHardness(world, x, y, z);
        if (hardness < 0.0F)
            return 0.0F;

        if (!BlockHelper.canHarvestBlock(entity.getHeldItem(), block))
            return BlockHelper.SPEED_MULT / hardness / 100.0F;
        return BlockHelper.getCurrentStrengthVsBlock(entity, block, metadata) * BlockHelper.SPEED_MULT / hardness / 30.0F;
    }

    // Returns whether the item can harvest the specified block.
    public static boolean canHarvestBlock(ItemStack itemStack, Block block) {
        return block.getMaterial().isToolNotRequired() || itemStack != null && itemStack.func_150998_b(block);
    }

    // Returns the mob's strength vs. the given block.
    public static float getCurrentStrengthVsBlock(EntityLiving entity, Block block, int metadata) {
        ItemStack held = entity.getHeldItem();
        float strength = held == null ? 1.0F : held.getItem().getDigSpeed(held, block, metadata);

        if (strength > 1.0F) {
            int efficiency = EnchantmentHelper.getEfficiencyModifier(entity);
            if (efficiency > 0 && held != null) {
                strength += efficiency * efficiency + 1;
            }
        }

        if (entity.isPotionActive(Potion.digSpeed)) {
            strength *= 1.0F + (entity.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2F;
        }
        if (entity.isPotionActive(Potion.digSlowdown)) {
            strength *= 1.0F - (entity.getActivePotionEffect(Potion.digSlowdown).getAmplifier() + 1) * 0.2F;
        }

        if (entity.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(entity)) {
            strength /= 5.0F;
        }
        if (!entity.onGround) {
            strength /= 5.0F;
        }

        return strength < 0.0F ? 0.0F : strength;
    }

    // Returns a new target block set from the string property.
    public static HashSet<TargetBlock> newBlockSet(String line) {
        HashSet<TargetBlock> blockSet = new HashSet<TargetBlock>();
        String[] targetableBlocks = line.split(",");
        String[] pair, modCheck;
        TargetBlock targetableBlock;
        for (String target : targetableBlocks) {
            pair = target.split(" ", 2);
            if (pair.length > 1) {
                targetableBlock = new TargetBlock(BlockHelper.getStringAsBlock(pair[0]), Integer.parseInt(pair[1]));
            }
            else {
                modCheck = pair[0].split(":", 2);
                if (modCheck.length > 1 && modCheck[1].equals("*")) {
                    BlockHelper.addAllModBlocks(blockSet, modCheck[0] + ":");
                    continue;
                }

                targetableBlock = new TargetBlock(BlockHelper.getStringAsBlock(pair[0]), -1);
            }

            if (targetableBlock.BLOCK == null || targetableBlock.BLOCK == Blocks.air) {
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
    private static Block getStringAsBlock(String id) {
        Block block = (Block) Block.blockRegistry.getObject(id);
        if (block == null || block == Blocks.air) {
            try {
                block = Block.getBlockById(Integer.parseInt(id));
                if (block != null && block != Blocks.air) {
                    _SpecialAI.console("[WARNING] Usage of numerical block id! (" + id + ")");
                }
            }
            catch (NumberFormatException numberformatexception) {
                // Do nothing
            }
        }
        if (block == null || block == Blocks.air) {
            _SpecialAI.console("[WARNING] Missing or invalid block! (" + id + ")");
        }
        return block;
    }
    private static void addAllModBlocks(HashSet<TargetBlock> blockSet, String namespace) {
        try {
            TargetBlock targetableBlock;
            for (String blockId : (Set<String>) Block.blockRegistry.getKeys()) {
                if (blockId.startsWith(namespace)) {
                    targetableBlock = new TargetBlock(BlockHelper.getStringAsBlock(blockId), -1);
                    if (targetableBlock.BLOCK == null || targetableBlock.BLOCK == Blocks.air) {
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
            _SpecialAI.debugException("Caught exception while adding namespace! (" + namespace + "*)");
        }
    }
}