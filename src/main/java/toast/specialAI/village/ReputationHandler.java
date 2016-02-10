package toast.specialAI.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import toast.specialAI.Properties;
import toast.specialAI.util.BlockHelper;
import toast.specialAI.util.TargetBlock;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ReputationHandler
{
    // Useful properties for this class.
    private static final boolean HOUSE_REP = Properties.getBoolean(Properties.VILLAGES, "house_rep");
    private static final boolean REFRESH_HOUSES = Properties.getBoolean(Properties.VILLAGES, "refresh_houses");

    private static final double BLOCK_REP_CHANCE = Properties.getDouble(Properties.VILLAGES, "block_rep_loss_chance");
    private static final int BLOCK_REP_LIMIT = Properties.getInt(Properties.VILLAGES, "block_rep_loss_limit");
    private static final double BLOCK_DEFEND_CHANCE = Properties.getDouble(Properties.VILLAGES, "block_aggression_chance");
    private static final int BLOCK_DEFEND_LIMIT = Properties.getInt(Properties.VILLAGES, "block_aggression_limit");
    private static final HashSet<TargetBlock> BLOCK_BLACKLIST = BlockHelper.newBlockSet(Properties.getString(Properties.VILLAGES, "block_blacklist"));
    private static final HashSet<TargetBlock> BLOCK_WHITELIST = BlockHelper.newBlockSet(Properties.getString(Properties.VILLAGES, "block_whitelist"));

    private static final double SPECIAL_REP_CHANCE = Properties.getDouble(Properties.VILLAGES, "block_special_rep_loss_chance");
    private static final double SPECIAL_DEFEND_CHANCE = Properties.getDouble(Properties.VILLAGES, "block_special_aggression_chance");
    private static final HashSet<TargetBlock> SPECIAL_LIST = BlockHelper.newBlockSet(Properties.getString(Properties.VILLAGES, "block_special_list"));

    private static final double HELP_REP_CHANCE = Properties.getDouble(Properties.VILLAGES, "help_rep_chance");

    // Returns the block aggression limit.
    public static int getDefendLimit() {
        return ReputationHandler.BLOCK_DEFEND_LIMIT;
    }

    // Counter to periodically refresh village doors and alter reputation for new/destroyed doors.
    private int updateTicks = 0;

    // Last saved door count for each village.
    private final HashMap<Village, Integer> doorCountCache = new HashMap<Village, Integer>();

    public ReputationHandler() {
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
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            this.updateTicks--;
            if (this.updateTicks % 40 == 0) {
                boolean refreshDoors = this.updateTicks <= 0;
                if (refreshDoors) {
                    this.updateTicks = 400;
                }

                int villageTicks;
                for (WorldServer world : FMLCommonHandler.instance().getMinecraftServerInstance().worldServers) {
                    villageTicks = -1;
                    for (Village village : (List<Village>) world.villageCollectionObj.getVillageList()) {
                        // Add/remove reputation for adding/removing houses.
                        if (ReputationHandler.HOUSE_REP) {
                            if (!this.doorCountCache.containsKey(village)) {
                                this.doorCountCache.put(village, Integer.valueOf(village.getNumVillageDoors()));
                            }
                            else {
                                int difference = village.getNumVillageDoors() - this.doorCountCache.get(village).intValue();
                                if (difference != 0) {
                                    this.doorCountCache.put(village, Integer.valueOf(village.getNumVillageDoors()));

                                    double maxDistSq = village.getVillageRadius() + 32.0;
                                    maxDistSq = maxDistSq * maxDistSq;
                                    ChunkCoordinates coords = village.getCenter();
                                    ArrayList<EntityPlayer> nearbyPlayers = new ArrayList<EntityPlayer>();
                                    for (Object player : world.playerEntities) {
                                        if (player instanceof EntityPlayer && ((EntityPlayer) player).getDistanceSq(coords.posX, coords.posY, coords.posZ) <= maxDistSq) {
                                            nearbyPlayers.add((EntityPlayer) player);
                                        }
                                    }
                                    for (EntityPlayer player : nearbyPlayers) {
                                        village.setReputationForPlayer(player.getCommandSenderName(), difference);
                                    }
                                }
                            }
                        }

                        // Keep doors saved in the village until destroyed.
                        if (refreshDoors && ReputationHandler.REFRESH_HOUSES) {
                        	boolean shouldRefresh = true;

                            // Check if all villagers are dead and not merely unloaded
                            if (village.getNumVillagers() <= 0) {
                                ChunkCoordinates coords = village.getCenter();
                                EntityPlayer nearestPlayer = world.getClosestPlayer(coords.posX, coords.posY, coords.posZ, village.getVillageRadius() + 32.0);
                                if (nearestPlayer != null) {
                                    shouldRefresh = false;
                                }
                            }

                            if (shouldRefresh) {
	                            if (villageTicks < 0) { // All villages in a village collection have the same tick counter
	                                NBTTagCompound tag = new NBTTagCompound();
	                                village.writeVillageDataToNBT(tag);
	                                villageTicks = tag.getInteger("Tick");
	                            }
	                            for (VillageDoorInfo doorInfo : (List<VillageDoorInfo>) village.getVillageDoorInfoList()) {
	                                doorInfo.lastActivityTimestamp = villageTicks;
	                            }
                            }
                        }
                    }
                }
            }
        }
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
        // Give good reputation for defending village
        if (ReputationHandler.HELP_REP_CHANCE > 0.0 && event.entityLiving instanceof IMob && event.source.getEntity() instanceof EntityPlayer && event.entityLiving.getRNG().nextDouble() < ReputationHandler.HELP_REP_CHANCE) {
            Village village = event.entityLiving.worldObj.villageCollectionObj.findNearestVillage(MathHelper.floor_double(event.entityLiving.posX), MathHelper.floor_double(event.entityLiving.posY), MathHelper.floor_double(event.entityLiving.posZ), 32);
            if (village != null) {
                village.setReputationForPlayer(((EntityPlayer) event.source.getEntity()).getCommandSenderName(), 1);
            }
        }
    }

    /**
     * Called by Block.dropBlockAsItemWithChance().
     * World world = the world the event is in.
     * Block block = the block being broken.
     * int blockMetadata = the metadata of the block being broken.
     * int x, y, z = the coordinates of the block.
     * int fortuneLevel = the harvester's fortune level.
     * ArrayList<ItemStack> drops = the items being dropped.
     * boolean isSilkTouching = true if silk touch is being used.
     * float dropChance = the chance for each item in the list to be dropped, not always used.
     * EntityPlayer harvester = the player harvesting the block, may be null.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockEvent.HarvestDropsEvent event) {
        if (event.harvester == null)
            return;
        if (ReputationHandler.BLOCK_WHITELIST.size() > 0 && !ReputationHandler.BLOCK_WHITELIST.contains(new TargetBlock(event.block, event.blockMetadata)))
            return;
        if (ReputationHandler.BLOCK_BLACKLIST.contains(new TargetBlock(event.block, event.blockMetadata)))
            return;
        Village village = event.world.villageCollectionObj.findNearestVillage(event.x, event.y, event.z, 8);
        if (village == null)
            return;
        boolean special = ReputationHandler.SPECIAL_LIST.contains(new TargetBlock(event.block, event.blockMetadata));
        int playerRep = village.getReputationForPlayer(event.harvester.getCommandSenderName());
        boolean trigger;

        // Reduce reputation for players that destroy the village
        if (playerRep <= ReputationHandler.BLOCK_REP_LIMIT) {
            if (special) {
                trigger = ReputationHandler.SPECIAL_REP_CHANCE > 0.0 && event.world.rand.nextDouble() < ReputationHandler.SPECIAL_REP_CHANCE;
            }
            else {
                trigger = ReputationHandler.BLOCK_REP_CHANCE > 0.0 && event.world.rand.nextDouble() < ReputationHandler.BLOCK_REP_CHANCE;
            }
            if (trigger) {
                village.setReputationForPlayer(event.harvester.getCommandSenderName(), -1);
                event.world.playAuxSFX(2004, event.x, event.y, event.z, 0);
            }
        }

        // Attack players that destroy the village
        if (playerRep <= ReputationHandler.BLOCK_DEFEND_LIMIT) {
            if (special) {
                trigger = ReputationHandler.SPECIAL_DEFEND_CHANCE > 0.0 && event.world.rand.nextDouble() < ReputationHandler.SPECIAL_DEFEND_CHANCE;
            }
            else {
                trigger = ReputationHandler.BLOCK_DEFEND_CHANCE > 0.0 && event.world.rand.nextDouble() < ReputationHandler.BLOCK_DEFEND_CHANCE;
            }
            if (trigger) {
                village.addOrRenewAgressor(event.harvester);
            }
        }
    }
}