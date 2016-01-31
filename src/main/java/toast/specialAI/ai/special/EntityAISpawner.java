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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.world.World;

public class EntityAISpawner extends EntityAIBase implements ISpecialAI {
    // The default spawner tag.
    private static final NBTTagCompound SPAWNER_TAG = new NBTTagCompound();

    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    public EntityLiving theEntity;

    // The mob spawner logic for this AI.
    private MobSpawnerBaseLogic mobSpawnerLogic;

    public EntityAISpawner() {}

    private EntityAISpawner(EntityLiving entity, NBTTagCompound spawnerTag) {
        this.theEntity = entity;
        this.mobSpawnerLogic = new MobSpawnerAILogic(entity);
        if (spawnerTag != null) {
            this.mobSpawnerLogic.readFromNBT(spawnerTag);
        }
        else {
            EntityAISpawner.SPAWNER_TAG.setString("EntityId", EntityList.getEntityString(entity));
            this.mobSpawnerLogic.readFromNBT(EntityAISpawner.SPAWNER_TAG);
        }
        this.setMutexBits(0);
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
        entity.setCurrentItemOrArmor(4, new ItemStack(Blocks.mob_spawner, 1, EntityList.getEntityID(entity)));
        entity.setEquipmentDropChance(4, 0.0F);

        entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).applyModifier(new AttributeModifier(UUID.randomUUID(), "Spawner speed penalty", -0.2, 1));
        entity.getEntityAttribute(SharedMonsterAttributes.maxHealth).applyModifier(new AttributeModifier(UUID.randomUUID(), "Spawner health boost", 80.0, 0));
        entity.setHealth(entity.getHealth() + 80.0F);
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
        EntityAISpawner.SPAWNER_TAG.setString("EntityId", ""); // This is always set before use
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
        // Marker to adjust spawner height for the particle effects.
        private boolean justSpawned;
        // Reference to the entity host.
        private EntityLiving theEntity;

        public MobSpawnerAILogic(EntityLiving entity) {
            this.theEntity = entity;
        }

        // Called to initialize the entity.
        @Override
        public Entity func_98265_a(Entity entity) {
            entity = super.func_98265_a(entity);
            this.justSpawned = true;
            return entity;
        }

        // Tells the client to reset the spinning speed.
        @Override
        public void func_98267_a(int p_98267_1_) {
            // Do nothing
        }

        // The world this spawner is in.
        @Override
        public World getSpawnerWorld() {
            return this.theEntity.worldObj;
        }

        // Returns the x, y, z coords for this spawner.
        @Override
        public int getSpawnerX() {
            return (int) Math.floor(this.theEntity.posX);
        }
        @Override
        public int getSpawnerY() {
            if (this.justSpawned) {
                this.justSpawned = false;
                return (int) Math.floor(this.theEntity.posY + this.theEntity.getEyeHeight());
            }
            return (int) Math.floor(this.theEntity.posY);
        }
        @Override
        public int getSpawnerZ() {
            return (int) Math.floor(this.theEntity.posZ);
        }
    }

}