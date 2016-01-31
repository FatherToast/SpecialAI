package toast.specialAI.ai;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockOre;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import toast.specialAI.Properties;
import toast.specialAI.util.BlockHelper;
import toast.specialAI.util.TargetBlock;

public class GriefSearch {
    // Useful properties for this class.
    private static final int SCAN_RANGE = Math.max(0, Properties.getInt(Properties.GRIEFING, "grief_scan_range"));
    // The limit the mob can move from its starting position before this search is cancelled.
    private static final double RESCAN_LIMIT = GriefSearch.SCAN_RANGE * GriefSearch.SCAN_RANGE / 4.0;

    // The AI this task is searching for.
    private final EntityAIGriefBlocks AI;
    // The entity that is searching.
    private final EntityLiving ENTITY;
    // The position the entity was at when this search was started.
    private final int POS_X, POS_Y, POS_Z;
    // The direction this search is scanning in.
    private final byte DIRECTION;
    // The range of heights to scan.
    private final int START_HEIGHT, END_HEIGHT;

    // If true, this search has been completed and will be terminated successfully.
    public boolean complete;

    // The coordinates of the block this entity is attacking.
    private int blockX, blockY, blockZ;
    // The block to attack.
    private Block targetBlock;

    // Used to keep track of place in the iterator.
    private int radius; // 0 ~ SCAN_RANGE
    private int offset; // 0 ~ SCAN_RANGE
    private byte position; // 0 ~ 3
    private int height; // START_HEIGHT ~ END_HEIGHT

    public GriefSearch(EntityAIGriefBlocks ai, EntityLiving entity) {
        this.AI = ai;
        this.ENTITY = entity;
        this.POS_X = (int) Math.floor(entity.posX);
        this.POS_Y = (int) Math.floor(entity.posY);
        this.POS_Z = (int) Math.floor(entity.posZ);
        this.DIRECTION = (byte) (this.ENTITY.getRNG().nextBoolean() ? -1 : 1);
        this.START_HEIGHT = Math.max(0, Math.min(255, this.POS_Y - GriefSearch.SCAN_RANGE * this.DIRECTION));
        this.END_HEIGHT = Math.max(0, Math.min(255, this.POS_Y + (GriefSearch.SCAN_RANGE + 1) * this.DIRECTION));
        this.height = this.START_HEIGHT;
    }

    // Returns true if the results of this search will still be valid.
    public boolean isValid() {
        return this.ENTITY.isEntityAlive() && this.ENTITY.getDistanceSq(this.POS_X + 0.5, this.POS_Y + 0.5, this.POS_Z + 0.5) <= GriefSearch.RESCAN_LIMIT;
    }

    // Called to clear this search from memory.
    public void clear() {
        this.AI.cancelSearch();
    }

    // Called to search up to a limit for and target a block.
    public void runSearch() {
        do {
            this.checkNext();
        }
        while (!this.complete && --SearchHandler.scansLeft > 0);

        if (this.complete && this.targetBlock != null) {
            this.AI.targetBlock(this.targetBlock, this.blockX, this.blockY, this.blockZ);
        }
    }

    // Checks the next closest solid block. Marks this search as complete if needed.
    private void checkNext() {
        if (this.radius == 0) {
            if (this.checkCoords(this.POS_X, this.POS_Z))
                return;
            this.radius++;
            this.offset = -this.radius;
        }
        while (this.radius <= GriefSearch.SCAN_RANGE) {
            while (this.offset <= this.radius) {
                while (this.position < 4) {
                    if (this.position < 2) {
                        if (this.checkCoords(this.POS_X + (this.position == 1 ? this.radius : -this.radius), this.POS_Z + this.offset))
                            return;
                    }
                    else if (this.offset != -this.radius && this.offset != this.radius) {
                        if (this.checkCoords(this.POS_X + this.offset, this.POS_Z + (this.position == 3 ? this.radius : -this.radius)))
                            return;
                    }
                    else {
                        break;
                    }
                    this.position++;
                }
                this.offset++;
                this.position = 0;
            }
            this.radius++;
            this.offset = -this.radius;
        }
        this.complete = true;
    }

    // Checks the given x, z. Returns true after scanning one solid, breakable block.
    private boolean checkCoords(int x, int z) {
        Block block;
        while (this.height != this.END_HEIGHT) {
            block = this.ENTITY.worldObj.getBlock(x, this.height, z);
            if (this.tryTargetBlock(block, x, this.height, z)) {
                this.height += this.DIRECTION;
                return true;
            }
            this.height += this.DIRECTION;
        }
        this.height = this.START_HEIGHT;
        return false;
    }

    // Tries to target a block. Returns true if the block is solid and breakable, and targets it if it is also targetable.
    private boolean tryTargetBlock(Block block, int x, int y, int z) {
        if (block == null || block instanceof BlockLiquid || !BlockHelper.shouldDamage(block, this.ENTITY, this.AI.needsTool, this.ENTITY.worldObj, x, y, z))
            return false;
        TargetBlock targetBlock = new TargetBlock(block, this.ENTITY.worldObj.getBlockMetadata(x, y, z));
        if (this.AI.getBlacklist().contains(targetBlock))
            return false;
        if (this.AI.breakLights && block.getLightValue() > 1) {
            if (! (block instanceof BlockFire) && block != Blocks.lit_redstone_ore && ! (block instanceof BlockOre)) {
                this.targetBlock(block, x, y, z);
                return true;
            }
        }
        if (this.AI.getTargetBlocks().contains(targetBlock)) {
            this.targetBlock(block, x, y, z);
        }
        return true;
    }

    // Targets the block at the given position.
    private void targetBlock(Block block, int x, int y, int z) {
        this.complete = true;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        this.targetBlock = block;
    }
}