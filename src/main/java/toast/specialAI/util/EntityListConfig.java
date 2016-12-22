package toast.specialAI.util;

import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;

public class EntityListConfig
{
    // The entity descriptions in this set.
    private final EntryEntity[] ENTRIES;

    public EntityListConfig(String line) {
    	this(line.split(","));
    }
    public EntityListConfig(String[] list) {
        ArrayList<EntryEntity> entryList = new ArrayList<EntryEntity>();
        for (String item : list) {
            boolean extendable = true;
            if (item.startsWith("~")) {
                item = item.substring(1);
                extendable = false;
            }
            String[] itemList = item.split(" ");
            Class<? extends Entity> entityClass = EntityList.NAME_TO_CLASS.get(itemList[0].trim());
            if (entityClass != null) {
                entryList.add(new EntryEntity(entityClass, extendable, itemList));
            }
        }
        this.ENTRIES = entryList.toArray(new EntryEntity[0]);
    }

    // Returns true if the entity is contained in this list.
    public boolean contains(Entity entity) {
        EntryEntity entry = new EntryEntity(entity.getClass());
        for (EntryEntity currentEntry : this.ENTRIES) {
            if (currentEntry.contains(entry))
                return true;
        }
        return false;
    }

    // Returns the float array of chances for the entry. Returns null if the entity is not contained in the set.
    public float[] getChances(Entity entity) {
        EntryEntity entry = new EntryEntity(entity.getClass());
        EntryEntity bestMatch = null;
        float[] matchChances = null;
        for (EntryEntity currentEntry : this.ENTRIES) {
            if (currentEntry.contains(entry) && (bestMatch == null || bestMatch.contains(currentEntry))) {
            	bestMatch = currentEntry;
            	matchChances = currentEntry.VALUES;
            }
        }
        return matchChances;
    }

    public static class EntryEntity {

        // The class this entry is defined for.
        public final Class<? extends Entity> CLASS;
        // True if any class extending CLASS is considered within this entry.
        public final boolean EXTEND;
        // The values given to this entry (0 to 1). Null for comparison objects.
        public final float[] VALUES;

        // Constructor used to compare entity classes with the entries in an EntityListConfig.
        public EntryEntity(Class<? extends Entity> entityClass) {
        	this(entityClass, false, (float[]) null);
        }

        // Constructors used for default properties.
        public EntryEntity(Class<? extends Entity> entityClass, float... vals) {
        	this(entityClass, true, vals);
        }
        public EntryEntity(Class<? extends Entity> entityClass, boolean extend, float... vals) {
            this.CLASS = entityClass;
            this.EXTEND = extend;
            this.VALUES = vals;
        }

        // Constructor used by the property reader.
        public EntryEntity(Class<? extends Entity> entityClass, boolean extend, String[] vals) {
            this.CLASS = entityClass;
            this.EXTEND = extend;
        	this.VALUES = new float[vals.length - 1];

        	for (int i = 0; i < this.VALUES.length; i++) {
        		float val;
        		try {
        			val = Float.parseFloat(vals[i + 1].trim());
        		}
        		catch (NumberFormatException ex) {
        			val = -1.0F;
        		}

        		if (val >= 0.0F && val <= 1.0F) {
        			this.VALUES[i] = val;
        		}
        	}
        }

        // Returns true if the given entity description is contained within this one (is more specific).
        public boolean contains(EntryEntity entry) {
            if (this.CLASS == entry.CLASS)
                return !entry.EXTEND;
            if (this.EXTEND)
                return this.CLASS.isAssignableFrom(entry.CLASS);
            return false;
        }

        @Override
		public String toString() {
        	String str = EntityList.getEntityStringFromClass(this.CLASS);
        	if (!this.EXTEND) str = "~" + str;
        	if (this.VALUES != null && this.VALUES.length > 0)
        		for (float val : this.VALUES) str = str + " " + Float.toString(val);
    		return str;
        }
    }
}