package fathertoast.specialai.config.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * One entity-value entry in an entity list.
 */
public class EntityEntry {
    /** The entity type this entry is defined for. Null for comparison objects. */
    public final EntityType<? extends Entity> TYPE;
    /** True if this should check for instanceof the entity class (as opposed to equals). */
    public final boolean EXTEND;
    /** The values given to this entry. Null for comparison objects. */
    public final double[] VALUES;
    
    /** The class this entry is defined for. This is not assigned until a world has been loaded. */
    Class<? extends Entity> entityClass;
    
    /** Creates an entry used to compare entity classes internally with the entries in an entity list. */
    EntityEntry( Entity entity ) {
        TYPE = null;
        EXTEND = false;
        VALUES = null;
        entityClass = entity.getClass();
    }
    
    /** Creates an extendable entry with the specified values. Used for creating default configs. */
    public EntityEntry( EntityType<? extends Entity> entityType, double... values ) {
        this( entityType, true, values );
    }
    
    /** Creates an entry with the specified values. Used for creating default configs. */
    public EntityEntry( EntityType<? extends Entity> entityType, boolean extend, double... values ) {
        TYPE = entityType;
        EXTEND = extend;
        VALUES = values;
    }
    
    /** Constructor used by the internal parser. Note that the 'args' parameter includes the entity string in its first index. */
    EntityEntry( EntityType<?> entityType, boolean extend, String[] args ) {
        TYPE = entityType;
        EXTEND = extend;
        VALUES = new double[args.length - 1];
        
        for( int i = 0; i < VALUES.length; i++ ) {
            double val;
            try {
                val = Double.parseDouble( args[i + 1] );
            }
            catch( NumberFormatException ex ) {
                val = 0.0;
            }
            VALUES[i] = val;
        }
    }
    
    /**
     * @return Returns true if the given entity description is contained within this one (is more specific).
     * <p>
     * This operates under the assumption that there will never be more than one non-extendable entry for the same class in a list.
     */
    public boolean contains( EntityEntry entry ) {
        if( entityClass == entry.entityClass )
            return !entry.EXTEND;
        if( EXTEND )
            return entityClass.isAssignableFrom( entry.entityClass );
        return false;
    }
    
    /**
     * @return The string representation of this entity list entry, as it would appear in a config file.
     * <p>
     * Format is "~registry_key value0 value1 ...", the ~ prefix is optional.
     */
    @Override
    public String toString() {
        // Start with the entity type registry key
        ResourceLocation resource = TYPE == null ? null : ForgeRegistries.ENTITIES.getKey( TYPE );
        StringBuilder str = new StringBuilder( resource == null ? "null" : resource.toString() );
        // Insert "specific" prefix if not extendable
        if( !EXTEND ) {
            str.insert( 0, '~' );
        }
        // Append values array
        if( VALUES != null && VALUES.length > 0 ) {
            for( double value : VALUES ) {
                str.append( ' ' ).append( value );
            }
        }
        return str.toString();
    }
}