package toast.specialAI.ai.special;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import toast.specialAI.ai.AIHandler;

public class EntityAISpawner extends EntityAIBase implements ISpecialAI
{
    // The default spawner tag.
    private static final NBTTagCompound SPAWNER_TAG = new NBTTagCompound();

    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    public EntityLiving theEntity;

    // The mob spawner logic for this AI.
    private MobSpawnerBaseLogic mobSpawnerLogic;

    public EntityAISpawner() { }

    private EntityAISpawner(EntityLiving entity, NBTTagCompound spawnerTag) {
        this.theEntity = entity;
        this.mobSpawnerLogic = new MobSpawnerAILogic(entity);
        if (spawnerTag != null) {
            this.mobSpawnerLogic.readFromNBT(spawnerTag);
        }
        else {
            this.mobSpawnerLogic.readFromNBT(EntityAISpawner.SPAWNER_TAG);
            this.mobSpawnerLogic.setEntityName(EntityList.getEntityString(entity));
        }
        this.setMutexBits(AIHandler.BIT_NONE);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "spawner";
    }

    // Gets/sets the weight as defined in Properties.
    @Override
    public int getWeight() {
        return this.WEIGHT;
    }

    @Override
    public void setWeight(int weight) {
        this.WEIGHT = weight;
    }

    // Adds a copy of this AI to the given entity.
    @Override
    public void addTo(EntityLiving entity, NBTTagCompound aiTag) {
        NBTTagCompound spawnerTag;
        if (aiTag.hasKey(this.getName() + "Tags")) {
            spawnerTag = aiTag.getCompoundTag(this.getName() + "Tags");
        }
        else {
            spawnerTag = null;
        }
        entity.tasks.addTask(0, new EntityAISpawner(entity, spawnerTag));
    }

    // Saves this AI to the tag with its default value.
    @Override
    public void save(NBTTagCompound aiTag) {
        aiTag.setByte(this.getName(), (byte) 1);
    }

    // Returns true if a copy of this AI is saved to the tag.
    @Override
    public boolean isSaved(NBTTagCompound aiTag) {
        return aiTag.getByte(this.getName()) > 0;
    }

    // Initializes any one-time effects on the entity.
    @Override
    public void initialize(EntityLiving entity) {
        entity.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Blocks.MOB_SPAWNER));
        entity.setDropChance(EntityEquipmentSlot.HEAD, 0.0F);

        entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(new AttributeModifier(UUID.randomUUID(), "Spawner speed penalty", -0.2, 1));
        entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).applyModifier(new AttributeModifier(UUID.randomUUID(), "Spawner health boost", 40.0, 0));
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        return target != null && this.theEntity.getEntitySenses().canSee(target);
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return this.shouldExecute();
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        this.mobSpawnerLogic.updateSpawner();
    }

    static {
        // Initialize the default spawner tag.
    	MobSpawnerAILogic spawnLogic = new MobSpawnerAILogic(null);
    	spawnLogic.setEntityName("Pig");
    	spawnLogic.writeToNBT(EntityAISpawner.SPAWNER_TAG);
        EntityAISpawner.SPAWNER_TAG.removeTag("SpawnPotentials");
        EntityAISpawner.SPAWNER_TAG.setShort("Delay", (short) 20);
        EntityAISpawner.SPAWNER_TAG.setShort("MinSpawnDelay", (short) 150);
        EntityAISpawner.SPAWNER_TAG.setShort("MaxSpawnDelay", (short) 600);
        EntityAISpawner.SPAWNER_TAG.setShort("SpawnCount", (short) 4);
        EntityAISpawner.SPAWNER_TAG.setShort("MaxNearbyEntities", (short) 7);
        EntityAISpawner.SPAWNER_TAG.setShort("RequiredPlayerRange", (short) 16);
        EntityAISpawner.SPAWNER_TAG.setShort("SpawnRange", (short) 4);
    }

    // The version of mob spawner base logic used by this AI.
    private static class MobSpawnerAILogic extends MobSpawnerBaseLogic {

        // Reference to the entity host.
        private EntityLiving theEntity;

		protected short aiSpawnRange;

        public MobSpawnerAILogic(EntityLiving entity) {
            this.theEntity = entity;
        }

        @Override
        public World getSpawnerWorld() {
            return this.theEntity.worldObj;
        }
		@Override
		public BlockPos getSpawnerPosition() {
			return new BlockPos(this.theEntity.posX, this.theEntity.posY + this.theEntity.getEyeHeight(), this.theEntity.posZ);
		}

		@Override
		public void broadcastEvent(int id) {
			if (id == 1) { // Timer reset event
				for (Entity entity : this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.getEntityBoundingBox().expand(this.aiSpawnRange + 4.0, 2.0, this.aiSpawnRange + 4.0))) {
					if (entity.ticksExisted == 0 && entity instanceof EntityLiving) {
						((EntityLiving) entity).setAttackTarget(this.theEntity.getAttackTarget());
						((EntityLiving) entity).setRevengeTarget(this.theEntity.getAttackTarget());
					}
				}
			}
		}

	    @Override
		public void readFromNBT(NBTTagCompound tag) {
	    	super.readFromNBT(tag);

            this.aiSpawnRange = tag.getShort("SpawnRange");
	    }
    }

}