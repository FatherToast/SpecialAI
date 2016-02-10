package toast.specialAI.ai.special;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;

public interface ISpecialAI
{
    // Returns the string name of this AI for use in Properties.
    public String getName();

    // Gets/sets the weight as defined in Properties.
    public int getWeight();

    public void setWeight(int weight);

    // Adds a copy of this AI to the given entity.
    public void addTo(EntityLiving entity, NBTTagCompound aiTag);

    // Saves this AI to the tag with its default value.
    public void save(NBTTagCompound aiTag);

    // Returns true if a copy of this AI is saved to the tag.
    public boolean isSaved(NBTTagCompound aiTag);

    // Initializes any one-time effects on the entity.
    public void initialize(EntityLiving entity);
}