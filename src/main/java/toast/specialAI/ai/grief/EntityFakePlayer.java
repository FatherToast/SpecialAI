package toast.specialAI.ai.grief;

import java.util.Collection;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.FoodStats;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

public class EntityFakePlayer extends FakePlayer
{
	// The fake profile used for all fake players used by this mod.
    private static final GameProfile FAKE_PLAYER_PROFILE = new GameProfile(null, "[SpecialAIFakePlayer]");

    // The entity posing as this fake player.
	public final EntityLiving wrappedEntity;

	public EntityFakePlayer(EntityLiving entity) {
		super((WorldServer) entity.worldObj, EntityFakePlayer.FAKE_PLAYER_PROFILE);
		this.wrappedEntity = entity;
		this.foodStats = new FakeFoodStats(this);

		this.copyLocationAndAnglesFrom(entity);
		this.motionX = entity.motionX;
		this.motionY = entity.motionY;
		this.motionZ = entity.motionZ;
	}

	// Call this method when you are done using this fake player. After this is called, you should probably throw away the reference to this player.
	public void updateWrappedEntityState() {
		this.wrappedEntity.copyLocationAndAnglesFrom(this);
		this.wrappedEntity.motionX = this.motionX;
		this.wrappedEntity.motionY = this.motionY;
		this.wrappedEntity.motionZ = this.motionZ;
	}

    @Override
	public void heal(float amount) {
    	this.wrappedEntity.heal(amount);
    }
	@Override
	public void setHealth(float amount) {
		if (this.wrappedEntity != null) {
			this.wrappedEntity.setHealth(amount);
		}
		else {
			super.setHealth(amount);
		}
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float damage) {
    	return this.wrappedEntity.attackEntityFrom(source, damage);
    }

	@Override
	public void knockBack(Entity attacker, float force, double yaw, double pitch) {
		this.wrappedEntity.knockBack(attacker, force, yaw, pitch);
	}

	@Override
	public boolean isEntityAlive() {
		return this.wrappedEntity.isEntityAlive();
	}

    @Override
	public Collection getActivePotionEffects() {
		return this.wrappedEntity.getActivePotionEffects();
	}
	@Override
	public boolean isPotionActive(Potion potion) {
		return this.wrappedEntity.isPotionActive(potion);
	}
	@Override
	public PotionEffect getActivePotionEffect(Potion potion) {
		return this.wrappedEntity.getActivePotionEffect(potion);
	}
	@Override
	public void addPotionEffect(PotionEffect potionEffect) {
		this.wrappedEntity.addPotionEffect(potionEffect);
	}
	@Override
	public boolean isPotionApplicable(PotionEffect potionEffect) {
		return this.wrappedEntity.isPotionApplicable(potionEffect);
	}
	@Override
	public void removePotionEffect(Potion potion) {
		this.wrappedEntity.removePotionEffect(potion);
	}
	@Override
	public void curePotionEffects(ItemStack curativeItem) {
		this.wrappedEntity.curePotionEffects(curativeItem);
	}

	@Override
	public boolean isEntityUndead() {
		return this.wrappedEntity.isEntityUndead();
	}

    @Override
	public ItemStack getHeldItem(EnumHand hand) {
        return this.wrappedEntity.getHeldItem(hand);
    }
    @Override
	public Iterable<ItemStack> getHeldEquipment() {
        return this.wrappedEntity.getHeldEquipment();
    }
    @Override
	public Iterable<ItemStack> getArmorInventoryList() {
        return this.wrappedEntity.getArmorInventoryList();
    }
    @Override
	public void setItemStackToSlot(EntityEquipmentSlot slot, @Nullable ItemStack itemStack) {
    	this.wrappedEntity.setItemStackToSlot(slot, itemStack);
    }
    @Override @Nullable
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        return this.wrappedEntity.getItemStackFromSlot(slot);
    }

	@Override
	public IAttributeInstance getEntityAttribute(IAttribute attribute) {
		return this.wrappedEntity != null ? this.wrappedEntity.getEntityAttribute(attribute) : super.getEntityAttribute(attribute);
	}
	@Override
	public AbstractAttributeMap getAttributeMap() {
		return this.wrappedEntity != null ? this.wrappedEntity.getAttributeMap() : super.getAttributeMap();
	}

	@Override
	public boolean isEntityInvulnerable(DamageSource source) {
		return this.wrappedEntity.isEntityInvulnerable(source);
	}
	@Override
	public Entity changeDimension(int dim) {
		return this.wrappedEntity.changeDimension(dim);
	}
	@Override
	public EnumCreatureAttribute getCreatureAttribute() {
		return this.wrappedEntity.getCreatureAttribute();
	}

	private static class FakeFoodStats extends FoodStats {
		public final EntityFakePlayer fakePlayer;

		public FakeFoodStats(EntityFakePlayer fakePlayer) {
			this.fakePlayer = fakePlayer;
		}

		@Override
		public void addStats(int food, float saturationModifier) {
			this.fakePlayer.wrappedEntity.setHealth(this.fakePlayer.wrappedEntity.getHealth() + food);
		}

		@Override
		public int getFoodLevel() {
			return 10;
		}
		@Override
		public boolean needFood() {
			return true;
		}
		@Override
		public float getSaturationLevel() {
			return 10.0F;
		}
	}
}
