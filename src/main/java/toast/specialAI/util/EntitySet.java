package toast.specialAI.util;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;

public class EntitySet
{
    // The entity descriptions in this set.
    private final EntryEntity[] ENTRIES;

    public EntitySet(String line) {
        ArrayList<EntryEntity> entryList = new ArrayList<EntryEntity>();
        String[] list = line.split(",");
        for (String item : list) {
            boolean extendable = true;
            if (item.startsWith("~")) {
                item = item.substring(1);
                extendable = false;
            }
            Class entityClass = (Class)EntityList.stringToClassMapping.get(item);
            if (entityClass != null) {
                EntryEntity entry = new EntryEntity(entityClass, extendable);
                REDUNDANCY_CHECK: {
                    EntryEntity currentEntry;
                    for (Iterator<EntryEntity> iterator = entryList.iterator(); iterator.hasNext();) {
                        currentEntry = iterator.next();
                        if (currentEntry.contains(entry)) {
                            break REDUNDANCY_CHECK;
                        }
                        if (entry.contains(currentEntry)) {
                            iterator.remove();
                        }
                    }
                    entryList.add(entry);
                }
            }
        }
        this.ENTRIES = entryList.toArray(new EntryEntity[0]);
    }

    // Returns true if the entity is contained in this set.
    public boolean contains(Entity entity) {
        EntryEntity entry = new EntryEntity(entity.getClass(), false);
        for (EntryEntity currentEntry : this.ENTRIES) {
            if (currentEntry.contains(entry))
                return true;
        }
        return false;
    }

    private static class EntryEntity
    {
        // The class this entry is defined for.
        public final Class CLASS;
        // True if any class extending CLASS is considered within this entry.
        public final boolean EXTEND;

        public EntryEntity(Class entityClass, boolean extend) {
            this.CLASS = entityClass;
            this.EXTEND = extend;
        }

        // Returns true if the given entity description is contained within this one.
        public boolean contains(EntryEntity entry) {
            if (this.CLASS == entry.CLASS)
                return !entry.EXTEND;
            if (this.EXTEND)
                return this.CLASS.isAssignableFrom(entry.CLASS);
            return false;
        }
    }
}