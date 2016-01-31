package toast.specialAI;


public abstract class MobHelper {
    /*
    // Clears the entity's special AI.
    public static void clearSpecialAI(EntityLiving entity) {
        for (EntityAITaskEntry entry : (EntityAITaskEntry[])entity.tasks.taskEntries.toArray(new EntityAITaskEntry[0])) if (entry.action instanceof EntityAISpecial) {
            entity.tasks.removeTask(entry.action);
            entity.getEntityData().setByte("sai", (byte)-1);
            return;
        }
    }

    // Sets the mob's special AI and saves its index to NBT. clearSpecialAI() should usually be called first.
    public static void setSpecialAI(EntityLiving entity, int priority, String name) {
        setSpecialAI(entity, priority, (int)getSpecialAIIndex(name));
    }
    public static void setSpecialAI(EntityLiving entity, int priority, int index) {
        if (index < 0 || index >= _SpecialMobs.ai.length) {
            entity.getEntityData().setByte("sai", (byte)-1);
            return;
        }
        try {
            entity.tasks.addTask(priority, (EntityAISpecial)Class.forName("toast.specialMobs.ai.EntityAISpecial" + _SpecialMobs.ai[index]).getConstructor(new Class[] { EntityLiving.class }).newInstance(new Object[] { entity }));
            entity.getEntityData().setByte("sai", (byte)index);
        }
        catch (Exception ex) {
            _SpecialMobs.debugException("@" + _SpecialMobs.ai[index] + ": Error setting special AI! " + ex.getClass().getName());
        }
    }

    // Returns the special AI index for the given special AI name.
    public static byte getSpecialAIIndex(String name) {
        for (byte index = (byte)_SpecialMobs.ai.length; index-- > 0;) {
            if (name == _SpecialMobs.ai[index])
                return index;
        }
        return (byte)-1;
    }
    */
}