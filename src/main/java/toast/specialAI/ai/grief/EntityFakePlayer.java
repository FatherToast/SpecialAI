package toast.specialAI.ai.grief;

import java.util.Collection;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

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

		this.getHeldItem();
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
	public boolean isPotionActive(int potionId) {
		return this.wrappedEntity.isPotionActive(potionId);
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
	public void removePotionEffect(int potionId) {
		this.wrappedEntity.removePotionEffect(potionId);
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
	public ItemStack getHeldItem() {
        return this.getEquipmentInSlot(0);
    }
    @Override
	public ItemStack getEquipmentInSlot(int slot) {
        return this.wrappedEntity.getEquipmentInSlot(slot);
    }
    @Override
	public void setCurrentItemOrArmor(int slot, ItemStack itemStack) {
    	this.wrappedEntity.setCurrentItemOrArmor(slot, itemStack);
    }
    @Override
	public ItemStack[] getLastActiveItems() {
        return this.wrappedEntity.getLastActiveItems();
    }

	@Override
	public IAttributeInstance getEntityAttribute(IAttribute attribute) {
		return this.wrappedEntity != null ? this.wrappedEntity.getEntityAttribute(attribute) : super.getEntityAttribute(attribute);
	}
	@Override
	public BaseAttributeMap getAttributeMap() {
		return this.wrappedEntity != null ? this.wrappedEntity.getAttributeMap() : super.getAttributeMap();
	}

	@Override
	public ChunkCoordinates getPlayerCoordinates() {
		return new ChunkCoordinates((int) Math.floor(this.posX), (int) Math.floor(this.posY + 0.5), (int) Math.floor(this.posZ));
	}
	@Override
	public boolean isEntityInvulnerable() {
		return this.wrappedEntity.isEntityInvulnerable();
	}
	@Override
	public void travelToDimension(int dim) {
		this.wrappedEntity.travelToDimension(dim);
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
