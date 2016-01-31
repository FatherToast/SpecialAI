package toast.specialAI;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public abstract class EffectHelper {
    // Applies the potion's effect on the entity. If the potion is already active, its duration is increased up to the given duration and its amplifier is increased by the given amplifier + 1.
    public static void stackEffect(EntityLivingBase entity, Potion potion, int duration, int amplifier) {
        if (entity.isPotionActive(potion)) {
            PotionEffect potionEffect = entity.getActivePotionEffect(potion);
            entity.addPotionEffect(new PotionEffect(potion.id, Math.max(duration, potionEffect.getDuration()), potionEffect.getAmplifier() + amplifier + 1));
        }
        else {
            entity.addPotionEffect(new PotionEffect(potion.id, duration, amplifier));
        }
    }

    // Applies the potion's effect on the entity. If the potion is already active, its duration is increased up to the given duration and its amplifier is increased by the given amplifier + 1 up to the given amplifierMax.
    public static void stackEffect(EntityLivingBase entity, Potion potion, int duration, int amplifier, int amplifierMax) {
        if (amplifierMax < 0) {
            EffectHelper.stackEffect(entity, potion, duration, amplifier);
            return;
        }
        if (entity.isPotionActive(potion)) {
            PotionEffect potionEffect = entity.getActivePotionEffect(potion);
            entity.addPotionEffect(new PotionEffect(potion.id, Math.max(duration, potionEffect.getDuration()), Math.min(amplifierMax, potionEffect.getAmplifier() + amplifier + 1)));
        }
        else if (amplifier >= 0) {
            entity.addPotionEffect(new PotionEffect(potion.id, duration, Math.min(amplifier, amplifierMax)));
        }
    }

    // Adds a custom attribute modifier to the item stack.
    // Operations: 0 = add, 1 = percent (additive), 2 = percent (multiplicative)
    public static void addModifier(ItemStack itemStack, IAttribute attribute, double value, int operation) {
        if (itemStack.stackTagCompound == null) {
            itemStack.stackTagCompound = new NBTTagCompound();
        }
        if (!itemStack.stackTagCompound.hasKey("AttributeModifiers")) {
            itemStack.stackTagCompound.setTag("AttributeModifiers", new NBTTagList());
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("AttributeName", attribute.getAttributeUnlocalizedName());
        tag.setString("Name", _SpecialAI.BASE_TAG + "|" + Integer.toString(_SpecialAI.random.nextInt(), Character.MAX_RADIX));
        tag.setDouble("Amount", value);
        tag.setInteger("Operation", operation);
        UUID id = UUID.randomUUID();
        tag.setLong("UUIDMost", id.getMostSignificantBits());
        tag.setLong("UUIDLeast", id.getLeastSignificantBits());
        itemStack.stackTagCompound.getTagList("AttributeModifiers", tag.getId()).appendTag(tag);
    }
}