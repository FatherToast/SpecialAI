package toast.specialAI.ai;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import toast.specialAI.Properties;
import toast.specialAI.ModSpecialAI;
import toast.specialAI.ai.grief.EntityAIEatBreedingItem;
import toast.specialAI.ai.grief.EntityAIGriefBlocks;
import toast.specialAI.ai.react.EntityAIAvoidExplosions;
import toast.specialAI.ai.react.EntityAIDodgeArrows;
import toast.specialAI.ai.special.SpecialAIHandler;
import toast.specialAI.village.EntityAIVillagerDefendVillage;

public class AIHandler
{
    // NBT tags used to store info about this mod's AI.
    private static final String AVOID_EXPLOSIONS_TAG = "AvoidExplosions";
    private static final String DODGE_ARROWS_TAG = "DodgeArrows";
    private static final String DODGE_ARROW_INIT_TAG = "SAIArrowDodgeCheck";

    private static final String DEFEND_VILLAGE_TAG = "DefendVillage";
    private static final String DEPACIFY_TAG = "Depacify";

    public static final String IDLE_RANGE_XZ_TAG = "IdleScanRangeXZ";
    public static final String IDLE_RANGE_Y_TAG = "IdleScanRangeY";

    public static final String GRIEF_TAG = "Griefing";
    public static final String GRIEF_BREAK_SPEED = "GriefBreakSpeed";
    public static final String GRIEF_TOOL_TAG = "GriefNeedsTool";
    public static final String GRIEF_LIGHT_TAG = "GriefLights";
    public static final String GRIEF_BLOCK_TAG = "GriefBlocks";
    public static final String GRIEF_LOOTABLE_TAG = "GriefLootable";
    public static final String GRIEF_EXCEPTION_TAG = "GriefBlacklist";

    public static final String FIDDLE_TAG = "Fiddling";
    public static final String FIDDLE_BLOCK_TAG = "FiddleBlocks";
    public static final String FIDDLE_EXCEPTION_TAG = "FiddleBlacklist";

    private static final String RIDER_TAG = "Rider";

    private static final String UAI_TAG = "uAI";
    private static final String FORCE_INIT_TAG = "ForceInit";

    @Deprecated
    private static final String SPECIAL_TAG = "Special";

    /* Mutex Bits:
	 * AIs may only run concurrently if they share no "mutex bits".
	 * Therefore, AIs with 0 for their "mutex" number may always run with any other AI.
	 * Use plain addition or bitwise OR to combine multiple bits. */
	public static final byte BIT_NONE = Byte.parseByte("000", 2);
	public static final byte BIT_MOVEMENT = Byte.parseByte("001", 2);
	public static final byte BIT_FACING = Byte.parseByte("010", 2);
	public static final byte BIT_SWIMMING = Byte.parseByte("100", 2);

    /* The "mutex bit" used by all targeting tasks so that none of them run at the same time. */
	public static final byte TARGET_BIT = 1;

    private static int scansLeft = Properties.get().IDLE_AI.SCAN_COUNT_GLOBAL;

    // Decrements the number of scans left and returns true if a scan can be made.
    public static boolean canScan() {
		return Properties.get().IDLE_AI.SCAN_COUNT_GLOBAL <= 0 || AIHandler.scansLeft-- > 0;
    }

    // Clears the entity's AI tasks.
	private static void clearAI(EntityLiving entity) {
        for (EntityAITaskEntry entry : entity.tasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
            entity.tasks.removeTask(entry.action);
        }
    }

    // Clears the entity's AI target tasks.
    private static void clearTargetAI(EntityLiving entity) {
        for (EntityAITaskEntry entry : entity.targetTasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
            entity.targetTasks.removeTask(entry.action);
        }
    }

    // Adds avoid explosions AI to the entity.
    private static void addAvoidExplosionsAI(EntityCreature entity) {
        entity.tasks.addTask(-1, new EntityAIAvoidExplosions(entity));
    }

    // Adds avoid explosions AI to the entity.
    private static void addDodgeArrowsAI(EntityCreature entity, float dodgeChance) {
        entity.tasks.addTask(-1, new EntityAIDodgeArrows(entity, dodgeChance));
    }

    // Adds defend village AI to the mob.
    private static void addDefendVillageAI(EntityCreature entity, boolean addedAttackAI) {
        if (!addedAttackAI) {
            entity.tasks.addTask(0, new EntityAIAttackMeleePassive(entity, 0.7, false));
        }
        entity.targetTasks.addTask(0, new EntityAIVillagerDefendVillage(entity));
    }

    // Adds hurt by target AI to the mob.
    private static void addHurtByTargetAI(EntityCreature entity, byte depacify, boolean addedAttackAI) {
        if (!addedAttackAI) {
            entity.tasks.addTask(0, new EntityAIAttackMeleePassive(entity, entity instanceof EntityChicken ? 1.8 : 1.4, false));
        }
        entity.targetTasks.addTask(0, new EntityAIHurtByTarget(entity, Properties.get().REACT_AI.CALL_HELP));
        if (depacify > 1) {
            entity.targetTasks.addTask(1, new EntityAINearestAttackableTarget(entity, EntityPlayer.class, true));
        }
    }

    // Sets the entity's "call for help" to true.
    private static void setHelpAI(EntityCreature entity) {
        for (EntityAITaskEntry entry : entity.targetTasks.taskEntries.toArray(new EntityAITaskEntry[0]))
            if (entry.action.getClass() == EntityAIHurtByTarget.class) {
                int priority = entry.priority;
                entity.targetTasks.removeTask(entry.action);
                entity.targetTasks.addTask(priority, new EntityAIHurtByTarget(entity, true));
                return;
            }
    }

    // Gives the entity rider AI.
    private static void addRiderAI(EntityLiving entity, boolean small) {
        entity.tasks.addTask(AIHandler.getPassivePriority(entity), new EntityAIRider(entity, small));
    }

    // Adds the passive griefing AI to the same priority as the mob's wandering AI.
    private static void addEatingAI(EntityAnimal entity) {
        entity.tasks.addTask(AIHandler.getPassivePriority(entity), new EntityAIEatBreedingItem(entity));
    }

    // Adds the passive griefing AI to the same priority as the mob's wandering AI.
    private static void addGriefAI(EntityLiving entity, boolean griefing, boolean fiddling, NBTTagCompound tag) {
        entity.tasks.addTask(AIHandler.getPassivePriority(entity), new EntityAIGriefBlocks(entity, griefing, fiddling, tag));
    }

    // Returns the priority to assign to an idle AI.
    private static int getPassivePriority(EntityLiving entity) {
        int highest = Integer.MIN_VALUE;
        for (EntityAITaskEntry entry : entity.tasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
            if (entry.action instanceof EntityAIWander || entry.action instanceof EntityAIWatchClosest || entry.action instanceof EntityAILookIdle)
                return entry.priority;
            if (highest < entry.priority) {
                highest = entry.priority;
            }
        }
        return highest + 1;
    }

    // Gives the entity digging AI.
	private static void addDigAI(EntityLiving entity) {
        //entity.tasks.addTask(0, new EntityAIDig(entity));
    }

    public AIHandler() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Called each tick.
     * TickEvent.Type type = the type of tick.
     * Side side = the side this tick is on.
     * TickEvent.Phase phase = the phase of this tick (START, END).
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
        	AIHandler.scansLeft = Properties.get().IDLE_AI.SCAN_COUNT_GLOBAL;
        }
    }

    /**
     * Called each tick for each world.
     * TickEvent.Type type = the type of tick.
     * Side side = the side this tick is on.
     * TickEvent.Phase phase = the phase of this tick (START, END).
     * World world = the world that is ticking.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world != null && event.phase == TickEvent.Phase.END) {
        	Object entity;
        	for (int i = 0; i < event.world.loadedEntityList.size(); i++) {
        		entity = event.world.loadedEntityList.get(i);
        		if (entity instanceof EntityItem) {
        			this.equipToThief((EntityItem) entity);
        		}
        	}
        }
    }

    private void equipToThief(EntityItem item) {
    	if (item.getEntityData().hasUniqueId("ThiefUUID")) {
    		UUID thiefId = item.getEntityData().getUniqueId("ThiefUUID");
        	Object entity;
        	for (int i = 0; i < item.worldObj.loadedEntityList.size(); i++) {
        		entity = item.worldObj.loadedEntityList.get(i);
        		if (entity instanceof EntityLiving && thiefId.equals(((EntityLiving) entity).getUniqueID())) {
        			((EntityLiving) entity).setItemStackToSlot(EntityEquipmentSlot.MAINHAND, item.getEntityItem());
        			((EntityLiving) entity).setDropChance(EntityEquipmentSlot.MAINHAND, 2.0F);
        			((EntityLiving) entity).enablePersistence();
        			item.setDead();
        			return;
        		}
        	}
        	item.getEntityData().removeTag("ThiefUUID" + "Most");
        	item.getEntityData().removeTag("ThiefUUID" + "Least");
    	}
    }

    /**
     * Called by World.spawnEntityInWorld().
     * Entity entity = the entity joining the world.
     * World world = the world the entity is joining.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote)
            return;

        if (event.getEntity() instanceof EntityArrow && !event.getEntity().getEntityData().getBoolean(AIHandler.DODGE_ARROW_INIT_TAG)) {
        	event.getEntity().getEntityData().setBoolean(AIHandler.DODGE_ARROW_INIT_TAG, true);
        	EntityAIDodgeArrows.doDodgeCheckForArrow(event.getEntity());
        }

        if (!(event.getEntity() instanceof EntityLiving))
        	return;

        EntityLiving theEntity = (EntityLiving) event.getEntity();
        NBTTagCompound tag = ModSpecialAI.getTag(theEntity);
    	float[] chances;

        if (theEntity instanceof EntityCreature) {
            // So we don't add multiple attack AIs
            boolean addedAttackAI = false;

            // Avoid explosions
            if (!tag.hasKey(AIHandler.AVOID_EXPLOSIONS_TAG)) {
                tag.setBoolean(AIHandler.AVOID_EXPLOSIONS_TAG, Properties.get().REACT_AI.AVOID_EXPLOSIONS);
            }
            if (tag.getBoolean(AIHandler.AVOID_EXPLOSIONS_TAG)) {
                AIHandler.addAvoidExplosionsAI((EntityCreature) theEntity);
            }

            // Dodge arrows
            if (!tag.hasKey(AIHandler.DODGE_ARROWS_TAG)) {
                tag.setFloat(AIHandler.DODGE_ARROWS_TAG, (float) Properties.get().REACT_AI.DODGE_ARROWS);
            }
            if (tag.getFloat(AIHandler.DODGE_ARROWS_TAG) > 0.0F) {
                AIHandler.addDodgeArrowsAI((EntityCreature) theEntity, tag.getFloat(AIHandler.DODGE_ARROWS_TAG));
            }

            // Defend village
            if (!tag.hasKey(AIHandler.DEFEND_VILLAGE_TAG)) {
                tag.setBoolean(AIHandler.DEFEND_VILLAGE_TAG, Properties.get().VILLAGES.VILLAGERS_DEFEND && theEntity instanceof EntityVillager);
            }
            if (tag.getBoolean(AIHandler.DEFEND_VILLAGE_TAG)) {
                AIHandler.addDefendVillageAI((EntityCreature) theEntity, addedAttackAI);
                addedAttackAI = true;
            }

            // Depacify
            byte depacify;
            if (tag.hasKey(AIHandler.DEPACIFY_TAG)) {
                depacify = tag.getByte(AIHandler.DEPACIFY_TAG);
            }
            else {
            	chances = Properties.get().GENERAL.DEPACIFY_LIST.getChances(theEntity);
                if (chances != null && chances.length > 0 && theEntity.getRNG().nextFloat() < chances[0]) {
                    if (theEntity.getRNG().nextDouble() < Properties.get().GENERAL.AGGRESSIVE_CHANCE) {
                        depacify = 2;
                    }
                    else {
                        depacify = 1;
                    }
                }
                else {
                    depacify = 0;
                }
            }
            if (depacify > 0) {
                AIHandler.addHurtByTargetAI((EntityCreature) theEntity, depacify, addedAttackAI);
                addedAttackAI = true;
            }
            // Call for help (already covered if depacified)
            else if (Properties.get().REACT_AI.CALL_HELP) {
                AIHandler.setHelpAI((EntityCreature) theEntity);
            }
        }

        // Rider
        boolean small = Properties.get().JOCKEYS.RIDER_LIST_SMALL.contains(theEntity);
        if (!tag.hasKey(AIHandler.RIDER_TAG)) {
            if (small) {
            	chances = Properties.get().JOCKEYS.RIDER_LIST_SMALL.getChances(theEntity);
            }
            else {
            	chances = Properties.get().JOCKEYS.RIDER_LIST.getChances(theEntity);
            }

            if (chances != null && chances.length > 0 && theEntity.getRNG().nextFloat() < chances[0]) {
                tag.setBoolean(AIHandler.RIDER_TAG, true);
            }
            else tag.setBoolean(AIHandler.RIDER_TAG, false);
        }
        if (tag.getBoolean(AIHandler.RIDER_TAG)) {
            AIHandler.addRiderAI(theEntity, small);
        }

        // Eat breeding items
        if (Properties.get().GENERAL.EAT_BREEDING_ITEMS && theEntity instanceof EntityAnimal) {
            AIHandler.addEatingAI((EntityAnimal) theEntity);
        }

        // Passive griefing
        if (!tag.hasKey(AIHandler.GRIEF_TAG)) {
        	chances = Properties.get().GRIEFING.MOB_LIST.getChances(theEntity);
            tag.setBoolean(AIHandler.GRIEF_TAG, chances != null && chances.length > 0 && theEntity.getRNG().nextFloat() < chances[0]);
        }
        if (!tag.hasKey(AIHandler.FIDDLE_TAG)) {
        	chances = Properties.get().FIDDLING.MOB_LIST.getChances(theEntity);
            tag.setBoolean(AIHandler.FIDDLE_TAG, chances != null && chances.length > 0 && theEntity.getRNG().nextFloat() < chances[0]);
        }
        boolean griefing = Properties.get().GRIEFING.ENABLED && tag.getBoolean(AIHandler.GRIEF_TAG);
        boolean fiddling = Properties.get().FIDDLING.ENABLED && tag.getBoolean(AIHandler.FIDDLE_TAG);
        if (griefing || fiddling) {
            AIHandler.addGriefAI(theEntity, griefing, fiddling, tag);
        }

        /* WIP
        // Digging
        if (event.entity instanceof EntityZombie) {
            AIHandler.addDigAI(theEntity);
        }
         */

        // Unique AI
        NBTTagCompound aiTag;
        if (!tag.hasKey(AIHandler.UAI_TAG)) {
            aiTag = new NBTTagCompound();
            tag.setTag(AIHandler.UAI_TAG, aiTag);

            // Compatibility
            if (tag.hasKey(AIHandler.SPECIAL_TAG)) {
                byte aiCode = tag.getByte(AIHandler.SPECIAL_TAG);
                if (aiCode > 0) {
                    SpecialAIHandler.SPECIAL_AI_LIST[aiCode - 1].save(aiTag);
                }
            }

            // Apply new AI(s), if needed
            chances = Properties.get().SPECIAL_AI.MOB_LIST.getChances(theEntity);
            if (chances != null) for (float chance : chances) {
            	if (chance > 0.0F && theEntity.getRNG().nextFloat() < chance)
            		SpecialAIHandler.saveSpecialAI(theEntity, aiTag);
            }

            // Mark this entity to init, if not already forced
            if (!tag.hasKey(AIHandler.FORCE_INIT_TAG)) {
                tag.setBoolean(AIHandler.FORCE_INIT_TAG, true);
            }
        }
        else {
            aiTag = tag.getCompoundTag(AIHandler.UAI_TAG);
        }
        SpecialAIHandler.addSpecialAI(theEntity, aiTag, tag.getBoolean(AIHandler.FORCE_INIT_TAG));
        tag.removeTag(AIHandler.FORCE_INIT_TAG);
    }

    /**
     * Called by EntityLivingBase.onDeath().
     * EntityLivingBase entityLiving = the entity dying.
     * DamageSource source = the damage source that killed the entity.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingDeath(LivingDeathEvent event) {
        // Call for help on death
        if (Properties.get().REACT_AI.CALL_HELP_ON_DEATH > 0.0 && event.getEntityLiving() instanceof EntityLiving && event.getEntityLiving().getRNG().nextDouble() < Properties.get().REACT_AI.CALL_HELP_ON_DEATH) {
            EntityLiving theEntity = (EntityLiving) event.getEntityLiving();
            Entity target = event.getSource().getEntity();
            if (target instanceof EntityLivingBase) {
                IAttributeInstance attribute = theEntity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
                double range = attribute == null ? 16.0 : attribute.getAttributeValue();

                List entities = theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(theEntity, new AxisAlignedBB(theEntity.posX, theEntity.posY, theEntity.posZ, theEntity.posX + 1.0, theEntity.posY + 1.0, theEntity.posZ + 1.0).expand(range, 10.0, range));
                for (Object entity : entities) {
                    if (entity instanceof EntityLiving && (entity.getClass().isAssignableFrom(theEntity.getClass()) || theEntity.getClass().isAssignableFrom(entity.getClass()))) {
                        EntityLiving alliedEntity = (EntityLiving) entity;
                        if (alliedEntity.getAttackTarget() == null && !alliedEntity.isOnSameTeam(target)) {
                            alliedEntity.setAttackTarget((EntityLivingBase) target);
                        }
                    }
                }
            }
        }
    }
}