package fathertoast.specialai.config.util;

import fathertoast.specialai.ModCore;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of entity-value entries used to link one or more numbers to specific entity types.
 */
public class EntityList {
    /** The entity-value entries in this list. */
    private final EntityEntry[] ENTRIES;
    
    /** The number of values each entry must have. If this is negative, then entries may have any non-zero number of values. */
    private int entryValues = -1;
    /** The minimum value accepted for entry values in this list. */
    private double minValue = Double.NEGATIVE_INFINITY;
    /** The maximum value accepted for entry values in this list. */
    private double maxValue = Double.POSITIVE_INFINITY;
    
    /**
     * Create a new entity list from an array of entries. Used for creating default configs.
     * <p>
     * By default, entity lists will allow any non-zero number of values, and the value(s) can be any numerical double.
     * These parameters can be changed with helper methods that alter the number of values or values' bounds and return 'this'.
     */
    public EntityList( EntityEntry... entries ) {
        ENTRIES = entries;
    }
    
    /** Create a new entity list from a list of strings, such as from a config file. */
    public EntityList( List<? extends String> list ) {
        // Iterate through the provided string list
        ArrayList<EntityEntry> entryList = new ArrayList<>();
        for( String item : list ) {
            // Check if the entry should be "specific", i.e. check for entity class equality rather than instanceof
            boolean extendable = true;
            if( item.startsWith( "~" ) ) {
                item = item.substring( 1 );
                extendable = false;
            }
            
            // Parse the entity-value pair
            String[] itemList = item.split( " " );
            EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue( new ResourceLocation( itemList[0].trim() ) );
            if( entityType != null ) {
                entryList.add( new EntityEntry( entityType, extendable, itemList ) );
            }
            else {
                ModCore.LOG.error( "Invalid entity id! ({})", item );
            }
        }
        ENTRIES = entryList.toArray( new EntityEntry[0] );
    }
    
    /** @return The string list representation of this entity list, as it would appear in a config file. */
    public List<String> toStringList() {
        // Create a list of the entries in string format
        List<String> list = new ArrayList<>( ENTRIES.length );
        for( EntityEntry entry : ENTRIES ) {
            list.add( entry.toString() );
        }
        return list;
    }
    
    /** @return True if the entity is contained in this list. */
    public boolean contains( Entity entity ) {
        final EntityEntry targetEntry = new EntityEntry( entity );
        for( EntityEntry currentEntry : ENTRIES ) {
            if( currentEntry.contains( targetEntry ) )
                return true;
        }
        return false;
    }
    
    /**
     * @param entity The entity to retrieve values for.
     * @return The array of values of the best-match entry. Returns null if the entity is not contained in this entity list.
     */
    public double[] getValues( Entity entity ) {
        final EntityEntry targetEntry = new EntityEntry( entity );
        EntityEntry bestMatch = null;
        for( EntityEntry currentEntry : ENTRIES ) {
            // Immediately return if we match the most stringent entry possible
            if( !currentEntry.EXTEND && currentEntry.entityClass == targetEntry.entityClass ) {
                return currentEntry.VALUES;
            }
            // Otherwise, update the best match if we match for the first time, or we match a more specific entry
            else if( currentEntry.contains( targetEntry ) && (bestMatch == null || bestMatch.contains( currentEntry )) ) {
                bestMatch = currentEntry;
            }
        }
        return bestMatch == null ? null : bestMatch.VALUES;
    }
    
    /**
     * @param entity The entity to retrieve a value for.
     * @return The first value in the best-match entry's value array. Returns 0 if the entity is not contained in this
     * entity list or has no values specified.
     */
    public double getValue( Entity entity ) {
        double[] values = getValues( entity );
        return values == null || values.length < 1 ? 0.0 : values[0];
    }
    
    /** Marks this entity list as a simple percentage listing; exactly one percent (0 to 1) per entry. */
    public EntityList setSinglePercent() { return setSingleValue().setRange0to1(); }
    
    /** Marks this entity list as identification only; no values will be linked to any entries. */
    public EntityList setNoValues() { return setMultiValue( 0 ); }
    
    /** Marks this entity list as single-value; each entry will have exactly one value. */
    public EntityList setSingleValue() { return setMultiValue( 1 ); }
    
    /** Marks this entity list as multi-value; each entry will have the specified number of values. */
    public EntityList setMultiValue( int numberOfValues ) {
        entryValues = numberOfValues;
        return this;
    }
    
    /** Bounds entry values in this list between 0 and 1, inclusive. */
    public EntityList setRange0to1() { return setRange( 0.0, 1.0 ); }
    
    /** Bounds entry values in this list to any positive value (including +0). */
    public EntityList setRangePos() { return setRange( 0.0, Double.POSITIVE_INFINITY ); }
    
    /** Bounds entry values in this list to the specified limits, inclusive. */
    public EntityList setRange( double min, double max ) {
        minValue = min;
        maxValue = max;
        return this;
    }
    
    /** Validator for array elements. Used to bound entry values and enforce number of values ver entry. */
    public boolean validateEntry( Object object ) {
        if( object instanceof String ) {
            // Check that the string is a space-separated list
            String[] args = ((String) object).trim().split( " " );
            
            // Verify number of arguments matches expected
            if( entryValues < 0 ) {
                // Variable-value; just needs at least one value
                if( args.length < 2 ) return false;
            }
            else if( entryValues != args.length - 1 ) {
                // Specified-value; must have the exact number of values
                return false;
            }
            
            // Make sure all arguments are numbers, except for the first index
            for( int i = 1; i < args.length; i++ ) {
                double value;
                try {
                    value = Double.parseDouble( args[i] );
                }
                catch( NumberFormatException ex ) {
                    // This is thrown if the string is not a parsable number
                    return false;
                }
                // Verify value is within range
                if( value < minValue || value > maxValue ) return false;
            }
            return true;
        }
        return false;
    }
}