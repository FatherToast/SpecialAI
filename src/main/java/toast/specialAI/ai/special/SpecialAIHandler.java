package toast.specialAI.ai.special;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;

public class SpecialAIHandler {
    // Array of special AI patterns.
    public static final ISpecialAI[] SPECIAL_AI_LIST = {
        new EntityAICharge(), new EntityAISprint(), new EntityAIJump(), new EntityAILeap(),
        new EntityAIBarrage(), new EntityAISpawner(), new EntityAIShaman(), new EntityAIThrow(),
        new EntityAIThrowPlayer(), new EntityAIThief()
    };
    // Total weight of the SPECIAL_AI_LIST.
    public static int AI_WEIGHT;

    // Applies a random special AI to the mob.
    public static void saveSpecialAI(EntityLiving entity, NBTTagCompound aiTag) {
        if (SpecialAIHandler.AI_WEIGHT > 0) {
            int choice = entity.getRNG().nextInt(SpecialAIHandler.AI_WEIGHT);
            for (int index = SpecialAIHandler.SPECIAL_AI_LIST.length; index-- > 0;) {
                choice -= SpecialAIHandler.SPECIAL_AI_LIST[index].getWeight();
                if (choice < 0) {
                    SpecialAIHandler.SPECIAL_AI_LIST[index].save(aiTag);
                    return;
                }
            }
        }
    }

    // Adds any special AI contained within the tag.
    public static void addSpecialAI(EntityLiving entity, NBTTagCompound tag, boolean init) {
        float healthDiff = init ? entity.getMaxHealth() - entity.getHealth() : Float.NaN;

        for (ISpecialAI ai : SpecialAIHandler.SPECIAL_AI_LIST) {
            if (ai.isSaved(tag)) {
                ai.addTo(entity, tag);
                if (init) ai.initialize(entity);
            }
        }

        if (init) entity.setHealth(entity.getMaxHealth() - healthDiff);
    }
}
