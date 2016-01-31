package toast.specialAI.ai;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIArrowAttack;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import toast.specialAI.Properties;
import toast.specialAI._SpecialAI;
import toast.specialAI.ai.special.SpecialAIHandler;
import toast.specialAI.util.EntitySet;
import toast.specialAI.village.EntityAIVillagerDefendVillage;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class AIHandler {
    // Useful properties for this class.
    private static final boolean AVOID_EXPLOSIONS = Properties.getBoolean(Properties.GENERAL, "avoid_explosions");
    private static final boolean CALL_FOR_HELP = Properties.getBoolean(Properties.GENERAL, "call_for_help");
    private static final double CALL_FOR_HELP_DEATH = Properties.getDouble(Properties.GENERAL, "call_for_help_on_death");
    private static final boolean EAT_BREEDING_ITEMS = Properties.getBoolean(Properties.GENERAL, "eat_breeding_items");
    private static final EntitySet DEPACIFY_SET = new EntitySet(Properties.getString(Properties.GENERAL, "depacify_list"));
    private static final double AGGRESSIVE_CHANCE = Properties.getDouble(Properties.GENERAL, "depacify_aggressive_chance");

    private static final boolean GRIEFING = Properties.getBoolean(Properties.GRIEFING, "_enabled");
    private static final EntitySet GRIEF_SET = new EntitySet(Properties.getString(Properties.GRIEFING, "mob_list"));

    private static final double RIDER_CHANCE = Properties.getDouble(Properties.JOCKEYS, "_rider_chance");
    public static final EntitySet MOUNT_SET = new EntitySet(Properties.getString(Properties.JOCKEYS, "mount_list"));
    public static final EntitySet MOUNT_SET_SMALL = new EntitySet(Properties.getString(Properties.JOCKEYS, "mount_list_small"));
    private static final EntitySet RIDER_SET = new EntitySet(Properties.getString(Properties.JOCKEYS, "rider_list"));
    private static final EntitySet RIDER_SET_SMALL = new EntitySet(Properties.getString(Properties.JOCKEYS, "rider_list_small"));

    private static final double SPECIAL_CHANCE_1 = Properties.getDouble(Properties.SPECIAL_AI, "_chance_1");
    private static final EntitySet SPECIAL_SET_1 = new EntitySet(Properties.getString(Properties.SPECIAL_AI, "_mob_list_1"));
    private static final double SPECIAL_CHANCE_2 = Properties.getDouble(Properties.SPECIAL_AI, "_chance_2");
    private static final EntitySet SPECIAL_SET_2 = new EntitySet(Properties.getString(Properties.SPECIAL_AI, "_mob_list_2"));
    private static final double SPECIAL_CHANCE_3 = Properties.getDouble(Properties.SPECIAL_AI, "_chance_3");
    private static final EntitySet SPECIAL_SET_3 = new EntitySet(Properties.getString(Properties.SPECIAL_AI, "_mob_list_3"));

    private static final boolean VILLAGERS_DEFEND = Properties.getBoolean(Properties.VILLAGES, "villagers_defend");

    // NBT tags used to store info about this mod's AI.
    private static final String AVOID_EXPLOSIONS_TAG = "AvoidExplosions";
    private static final String DEFEND_VILLAGE_TAG = "DefendVillage";
    private static final String DEPACIFY_TAG = "Depacify";

    private static final String GRIEF_TAG = "Griefing";
    private static final String GRIEF_TOOL_TAG = "GriefNeedsTool";
    private static final String GRIEF_LIGHT_TAG = "GriefLights";
    private static final String GRIEF_BLOCK_TAG = "GriefBlocks";
    private static final String GRIEF_EXCEPTION_TAG = "GriefBlacklist";

    private static final String RIDER_TAG = "Rider";

    private static final String UAI_TAG = "uAI";
    private static final String FORCE_INIT_TAG = "ForceInit";

    @Deprecated
    private static final String SPECIAL_TAG = "Special";

    // Clears the entity's AI tasks.
    private static void clearAI(EntityLiving entity) {
        for (EntityAITaskEntry entry : (EntityAITaskEntry[]) entity.tasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
            entity.tasks.removeTask(entry.action);
        }
    }

    // Clears the entity's AI target tasks.
    private static void clearTargetAI(EntityLiving entity) {
        for (EntityAITaskEntry entry : (EntityAITaskEntry[]) entity.targetTasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
            entity.targetTasks.removeTask(entry.action);
        }
    }

    // Adds avoid explosions AI to the entity.
    private static void addAvoidExplosionsAI(EntityCreature entity) {
        entity.tasks.addTask(-1, new EntityAIAvoidExplosions(entity));
    }

    // Adds defend village AI to the mob.
    private static void addDefendVillageAI(EntityCreature entity, boolean addedAttackAI) {
        if (!addedAttackAI) {
            entity.tasks.addTask(0, new EntityAIAttackOnCollidePassive(entity, 0.7, false));
        }
        entity.targetTasks.addTask(0, new EntityAIVillagerDefendVillage(entity));
    }

    // Adds hurt by target AI to the mob.
    private static void addHurtByTargetAI(EntityCreature entity, byte depacify, boolean addedAttackAI) {
        if (!addedAttackAI) {
            entity.tasks.addTask(0, new EntityAIAttackOnCollidePassive(entity, entity instanceof EntityChicken ? 1.8 : 1.4, false));
        }
        entity.targetTasks.addTask(0, new EntityAIHurtByTarget(entity, AIHandler.CALL_FOR_HELP));
        if (depacify > 1) {
            entity.targetTasks.addTask(1, new EntityAINearestAttackableTarget(entity, EntityPlayer.class, 0, true));
        }
    }

    // Sets the entity's "call for help" to true.
    private static void setHelpAI(EntityCreature entity) {
        for (EntityAITaskEntry entry : (EntityAITaskEntry[]) entity.targetTasks.taskEntries.toArray(new EntityAITaskEntry[0]))
            if (entry.action.getClass() == EntityAIHurtByTarget.class) {
                int priority = entry.priority;
                entity.targetTasks.removeTask(entry.action);
                entity.targetTasks.addTask(priority, new EntityAIHurtByTarget(entity, true));
                return;
            }
    }

    // Gives the entity mount target AI.
    private static void addMountAI(EntityCreature entity, boolean addedAttackAI) {
        if (!addedAttackAI) {
            for (EntityAITaskEntry entry : (EntityAITaskEntry[]) entity.tasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
                if (entry.action instanceof EntityAIAttackOnCollide || entry.action instanceof EntityAIArrowAttack) {
                    addedAttackAI = true;
                    break;
                }
            }
        }
        entity.targetTasks.addTask(-1, new EntityAIRiderTarget(entity, addedAttackAI));
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
    private static void addGriefAI(EntityLiving entity, byte tool, byte lights, String nbtTargetBlocks, String nbtBlacklist) {
        entity.tasks.addTask(AIHandler.getPassivePriority(entity), new EntityAIGriefBlocks(entity, tool, lights, nbtTargetBlocks, nbtBlacklist));
    }

    // Returns the priority to assign to an idle AI.
    private static int getPassivePriority(EntityLiving entity) {
        int highest = Integer.MIN_VALUE;
        for (EntityAITaskEntry entry : (EntityAITaskEntry[]) entity.tasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
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
        entity.tasks.addTask(0, new EntityAIDig(entity));
    }

    public AIHandler() {
        MinecraftForge.EVENT_BUS.register(this);
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
        if (event.world.isRemote || ! (event.entity instanceof EntityLiving))
            return;

        EntityLiving theEntity = (EntityLiving) event.entity;
        NBTTagCompound tag = _SpecialAI.getTag(theEntity);

        if (theEntity instanceof EntityCreature) {
            // So we don't add multiple attack AIs
            boolean addedAttackAI = false;

            // Avoid explosions
            if (!tag.hasKey(AIHandler.AVOID_EXPLOSIONS_TAG)) {
                tag.setBoolean(AIHandler.AVOID_EXPLOSIONS_TAG, AIHandler.AVOID_EXPLOSIONS);
            }
            if (tag.getBoolean(AIHandler.AVOID_EXPLOSIONS_TAG)) {
                AIHandler.addAvoidExplosionsAI((EntityCreature) theEntity);
            }

            // Defend village
            if (!tag.hasKey(AIHandler.DEFEND_VILLAGE_TAG)) {
                tag.setBoolean(AIHandler.DEFEND_VILLAGE_TAG, AIHandler.VILLAGERS_DEFEND && theEntity instanceof EntityVillager);
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
                if (AIHandler.DEPACIFY_SET.contains(theEntity)) {
                    if (theEntity.getRNG().nextDouble() < AIHandler.AGGRESSIVE_CHANCE) {
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
            else if (AIHandler.CALL_FOR_HELP) {
                AIHandler.setHelpAI((EntityCreature) theEntity);
            }

            // Mount
            if (AIHandler.MOUNT_SET.contains(theEntity) || AIHandler.MOUNT_SET_SMALL.contains(theEntity)) {
                AIHandler.addMountAI((EntityCreature) theEntity, addedAttackAI);
            }
        }

        // Rider
        if (!tag.hasKey(AIHandler.RIDER_TAG) && AIHandler.RIDER_CHANCE > 0.0) {
            boolean small = AIHandler.RIDER_SET_SMALL.contains(theEntity);
            if ((small || AIHandler.RIDER_SET.contains(theEntity)) && theEntity.getRNG().nextDouble() < AIHandler.RIDER_CHANCE) {
                tag.setBoolean(AIHandler.RIDER_TAG, true);
            }
            else {
                tag.setBoolean(AIHandler.RIDER_TAG, false);
            }
        }
        if (tag.getBoolean(AIHandler.RIDER_TAG)) {
            AIHandler.addRiderAI(theEntity, AIHandler.RIDER_SET_SMALL.contains(theEntity));
        }

        // Eat breeding items
        if (AIHandler.EAT_BREEDING_ITEMS && theEntity instanceof EntityAnimal) {
            AIHandler.addEatingAI((EntityAnimal) theEntity);
        }

        // Passive griefing
        if (!tag.hasKey(AIHandler.GRIEF_TAG)) {
            tag.setBoolean(AIHandler.GRIEF_TAG, AIHandler.GRIEF_SET.contains(theEntity));
        }
        if (AIHandler.GRIEFING && tag.getBoolean(AIHandler.GRIEF_TAG)) {
            byte tool = -1;
            byte lights = -1;
            String nbtTargetBlocks = null;
            String nbtBlacklist = null;
            if (tag.hasKey(AIHandler.GRIEF_TOOL_TAG)) {
                tool = tag.getByte(AIHandler.GRIEF_TOOL_TAG);
            }
            if (tag.hasKey(AIHandler.GRIEF_LIGHT_TAG)) {
                lights = tag.getByte(AIHandler.GRIEF_LIGHT_TAG);
            }
            if (tag.hasKey(AIHandler.GRIEF_BLOCK_TAG)) {
                nbtTargetBlocks = tag.getString(AIHandler.GRIEF_BLOCK_TAG);
            }
            if (tag.hasKey(AIHandler.GRIEF_EXCEPTION_TAG)) {
                nbtBlacklist = tag.getString(AIHandler.GRIEF_EXCEPTION_TAG);
            }
            AIHandler.addGriefAI(theEntity, tool, lights, nbtTargetBlocks, nbtBlacklist);
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

            // Apply a new AI, if needed
            if (AIHandler.SPECIAL_CHANCE_1 > 0.0 && AIHandler.SPECIAL_SET_1.contains(theEntity) && theEntity.getRNG().nextDouble() < AIHandler.SPECIAL_CHANCE_1) {
                SpecialAIHandler.saveSpecialAI(theEntity, aiTag);
            }
            if (AIHandler.SPECIAL_CHANCE_2 > 0.0 && AIHandler.SPECIAL_SET_2.contains(theEntity) && theEntity.getRNG().nextDouble() < AIHandler.SPECIAL_CHANCE_2) {
                SpecialAIHandler.saveSpecialAI(theEntity, aiTag);
            }
            if (AIHandler.SPECIAL_CHANCE_3 > 0.0 && AIHandler.SPECIAL_SET_3.contains(theEntity) && theEntity.getRNG().nextDouble() < AIHandler.SPECIAL_CHANCE_3) {
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
        if (AIHandler.CALL_FOR_HELP_DEATH > 0.0 && event.entityLiving instanceof EntityLiving && event.entityLiving.getRNG().nextDouble() < AIHandler.CALL_FOR_HELP_DEATH) {
            EntityLiving theEntity = (EntityLiving) event.entityLiving;
            Entity target = event.source.getEntity();
            if (target instanceof EntityLivingBase) {
                IAttributeInstance attribute = theEntity.getEntityAttribute(SharedMonsterAttributes.followRange);
                double range = attribute == null ? 16.0 : attribute.getAttributeValue();

                List entities = theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(theEntity, AxisAlignedBB.getBoundingBox(theEntity.posX, theEntity.posY, theEntity.posZ, theEntity.posX + 1.0, theEntity.posY + 1.0, theEntity.posZ + 1.0).expand(range, 10.0, range));
                for (Object entity : entities) {
                    if (entity instanceof EntityLiving && (entity.getClass().isAssignableFrom(theEntity.getClass()) || theEntity.getClass().isAssignableFrom(entity.getClass()))) {
                        EntityLiving alliedEntity = (EntityLiving) entity;
                        if (alliedEntity.getAttackTarget() == null && !alliedEntity.isOnSameTeam((EntityLivingBase) target)) {
                            alliedEntity.setAttackTarget((EntityLivingBase) target);
                        }
                    }
                }
            }
        }
    }
}