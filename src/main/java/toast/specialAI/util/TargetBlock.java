package toast.specialAI.util;

import net.minecraft.block.Block;

public class TargetBlock {
    // The block to match.
    public final Block BLOCK;
    // The metadata of the block to match (-1 matches all).
    public final int BLOCK_DATA;

    public TargetBlock(Block block, int data) {
        this.BLOCK = block;
        this.BLOCK_DATA = data;
    }

    // Used to sort this object in a hash table.
    @Override
    public int hashCode() {
        return Block.getIdFromBlock(this.BLOCK);
    }

    // Returns true if this object is equal to the given object.
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TargetBlock && this.BLOCK == ((TargetBlock) obj).BLOCK)
            return this.BLOCK_DATA < 0 || ((TargetBlock) obj).BLOCK_DATA < 0 || this.BLOCK_DATA == ((TargetBlock) obj).BLOCK_DATA;
        return false;
    }
}