package fathertoast.specialai.util;

import fathertoast.specialai.ModCore;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;

/**
 * Contains helper methods and info used for NBT data.
 */
@SuppressWarnings( "unused" )
public final class NBTHelper {
    /** Special 'id' that represents all numerical tag types (byte, short, int, long, float, or double). */
    public static final byte ID_NUMERICAL = 99;
    /** Id for the Byte numerical tag type. */
    public static final byte ID_BYTE = 1;
    /** Id for the Short numerical tag type. */
    public static final byte ID_SHORT = 2;
    /** Id for the Integer numerical tag type. */
    public static final byte ID_INT = 3;
    /** Id for the Long numerical tag type. */
    public static final byte ID_LONG = 4;
    /** Id for the Float numerical tag type. */
    public static final byte ID_FLOAT = 5;
    /** Id for the Double numerical tag type. */
    public static final byte ID_DOUBLE = 6;
    
    /** Id for the Byte Array tag type. */
    public static final byte ID_BYTE_ARRAY = 7;
    /** Id for the Integer Array tag type. */
    public static final byte ID_INT_ARRAY = 11;
    /** Id for the Long Array tag type. */
    public static final byte ID_LONG_ARRAY = 12;
    
    /** Id for the String tag type. */
    public static final byte ID_STRING = 8;
    
    /** Id for the Tag List tag type. */
    public static final byte ID_LIST = 9;
    /** Id for the Tag Compound tag type. */
    public static final byte ID_COMPOUND = 10;
    
    /**
     * @param entity The entity to get data for.
     * @return The nbt compound to store all the given entity's data for this mod.
     */
    public static CompoundNBT getModTag( Entity entity ) {
        final CompoundNBT data = entity.getPersistentData();
        if( !data.contains( ModCore.MOD_ID, ID_COMPOUND ) ) {
            data.put( ModCore.MOD_ID, new CompoundNBT() );
        }
        return data.getCompound( ModCore.MOD_ID );
    }
    
    /** @return Gets a compound tag within the given parent tag; if the compound didn't exist, this will generate it. */
    public static CompoundNBT getOrCreateTag( CompoundNBT tag, String key ) {
        if( !tag.contains( key, NBTHelper.ID_COMPOUND ) ) {
            tag.put( key, new CompoundNBT() );
        }
        return tag.getCompound( key );
    }
    
    /** @return Gets a compound tag within the given parent tag; if the compound didn't exist, this will generate it. */
    public static CompoundNBT getOrCreateTag( CompoundNBT tag, String... path ) {
        CompoundNBT returnTag = tag;
        for( String key : path ) {
            returnTag = getOrCreateTag( returnTag, key );
        }
        return returnTag;
    }
    
    // This is a static-only helper class.
    private NBTHelper() {}
}