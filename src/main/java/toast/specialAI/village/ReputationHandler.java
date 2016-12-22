package toast.specialAI.village;

import java.io.File;
import java.io.FileInputStream;

import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import toast.specialAI.Properties;
import toast.specialAI.util.TargetBlock;

public class ReputationHandler
{
    // Counter to periodically refresh village doors and alter reputation for new/destroyed doors.
    private int updateTicks = 1;

    public ReputationHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void addReputation(EntityPlayer player, Village village, int rep) {
    	village.modifyPlayerReputation(player.getName(), rep);
    }
    public static void addReputationToAll(World world, double radius, Village village, int rep) {
    	// Add 0 to nearby players' rep to ensure they are known to the village
        double maxDistSq = village.getVillageRadius() + radius;
        maxDistSq = maxDistSq * maxDistSq;
        BlockPos coords = village.getCenter();
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistanceSq(coords) <= maxDistSq)
                village.modifyPlayerReputation(player.getName(), 0);
        }

        // Add to all known players' rep
        village.setDefaultPlayerReputation(rep);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onWorldLoad(WorldEvent.Load event) {
    	if (event.getWorld().villageCollectionObj != null && !VillageCollectionSafe.class.equals(event.getWorld().villageCollectionObj.getClass())) {
    		// Load our own village collection class and overwrite the old one
            String dataIdentifier = VillageCollection.fileNameForProvider(event.getWorld().provider);
            VillageCollection newVillageCollectionObj = null;
            try {
                File villagesdat = event.getWorld().getSaveHandler().getMapFileFromName(dataIdentifier);
                if (villagesdat != null && villagesdat.exists()) {
                	newVillageCollectionObj = new VillageCollectionSafe(dataIdentifier);

                	// Set world before loading; fixes player reputation data corrupting village objects
                	newVillageCollectionObj.setWorldsForAll(event.getWorld());

                    FileInputStream in = new FileInputStream(villagesdat);
                    NBTTagCompound tag = CompressedStreamTools.readCompressed(in);
                    in.close();
                    newVillageCollectionObj.readFromNBT(tag.getCompoundTag("data"));
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            if (newVillageCollectionObj == null)
            	newVillageCollectionObj = new VillageCollectionSafe(event.getWorld());

        	event.getWorld().villageCollectionObj = newVillageCollectionObj;
        	event.getWorld().getPerWorldStorage().setData(dataIdentifier, event.getWorld().villageCollectionObj);
    	}
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingDeath(LivingDeathEvent event) {
        // Give good reputation for defending village
        if (Properties.get().VILLAGES.HELP_REP_CHANCE > 0.0 && event.getEntityLiving() instanceof IMob && event.getSource().getEntity() instanceof EntityPlayer && event.getEntityLiving().getRNG().nextDouble() < Properties.get().VILLAGES.HELP_REP_CHANCE) {
            Village village = event.getEntityLiving().worldObj.villageCollectionObj.getNearestVillage(new BlockPos(event.getEntityLiving()), 32);
            if (village != null) {
                ReputationHandler.addReputation((EntityPlayer) event.getSource().getEntity(), village, 1);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockEvent.HarvestDropsEvent event) {
        if (event.getHarvester() == null)
            return;
        if (Properties.get().VILLAGES.BLOCK_WHITELIST.size() > 0 && !Properties.get().VILLAGES.BLOCK_WHITELIST.contains(new TargetBlock(event.getState())))
            return;
        if (Properties.get().VILLAGES.BLOCK_BLACKLIST.contains(new TargetBlock(event.getState())))
            return;
        Village village = event.getWorld().villageCollectionObj.getNearestVillage(event.getPos(), 8);
        if (village == null)
            return;
        boolean special = Properties.get().VILLAGES.SP_BLOCK_LIST.contains(new TargetBlock(event.getState()));
        int playerRep = village.getPlayerReputation(event.getHarvester().getName());
        boolean trigger;

        // Reduce reputation for players that destroy the village
        if (playerRep <= Properties.get().VILLAGES.BLOCK_REP_LIMIT) {
            if (special)
                trigger = Properties.get().VILLAGES.SP_BLOCK_REP_CHANCE > 0.0 && event.getWorld().rand.nextDouble() < Properties.get().VILLAGES.SP_BLOCK_REP_CHANCE;
            else
                trigger = Properties.get().VILLAGES.BLOCK_REP_CHANCE > 0.0 && event.getWorld().rand.nextDouble() < Properties.get().VILLAGES.BLOCK_REP_CHANCE;

            if (trigger) {
                ReputationHandler.addReputation(event.getHarvester(), village, -1);
                event.getWorld().playEvent(event.getHarvester(), 2004, event.getPos(), 0);
            }
        }

        // Attack players that destroy the village
        if (playerRep <= Properties.get().VILLAGES.BLOCK_ATTACK_LIMIT) {
            if (special) {
                trigger = Properties.get().VILLAGES.SP_BLOCK_ATTACK_CHANCE > 0.0 && event.getWorld().rand.nextDouble() < Properties.get().VILLAGES.SP_BLOCK_ATTACK_CHANCE;
            }
            else {
                trigger = Properties.get().VILLAGES.BLOCK_ATTACK_CHANCE > 0.0 && event.getWorld().rand.nextDouble() < Properties.get().VILLAGES.BLOCK_ATTACK_CHANCE;
            }
            if (trigger) {
                village.addOrRenewAgressor(event.getHarvester());
            }
        }
    }

    /*
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
                    villageTicks = -1; // Each world may have a different tick counter for its villages
                    for (Village village : world.villageCollectionObj.getVillageList()) {

                        // Add/remove reputation for adding/removing houses.
                        if (Properties.get().VILLAGES.HOUSE_REP) {
                            if (!this.doorCountCache.containsKey(village)) {
                                this.doorCountCache.put(village, Integer.valueOf(village.getNumVillageDoors()));
                            }
                            else {
                                int difference = village.getNumVillageDoors() - this.doorCountCache.get(village).intValue();
                                if (difference != 0) {
                                    this.doorCountCache.put(village, Integer.valueOf(village.getNumVillageDoors()));

                                    // Add 0 to nearby players' rep to ensure they are known to the village
                                    double maxDistSq = village.getVillageRadius() + 32.0;
                                    maxDistSq = maxDistSq * maxDistSq;
                                    BlockPos coords = village.getCenter();
                                    for (Object player : world.playerEntities) {
                                        if (player instanceof EntityPlayer && ((EntityPlayer) player).getDistanceSq(coords) <= maxDistSq) {
                                            village.modifyPlayerReputation(((EntityPlayer) player).getName(), 0);
                                        }
                                    }

                                    // Add the difference to all known players' rep
                                    village.setDefaultPlayerReputation(difference);
                                }
                            }
                        }

                        // Keep doors saved in the village until destroyed.
                        if (refreshDoors && Properties.get().VILLAGES.REFRESH_HOUSES) {
                            if (villageTicks < 0) {
                            	// All villages in a village collection have the same tick counter
                                NBTTagCompound tag = new NBTTagCompound();
                                village.writeVillageDataToNBT(tag);
                                villageTicks = tag.getInteger("Tick");
                            }

                            BlockPos doorPos;
                            int chunkX, chunkZ;
                            for (VillageDoorInfo doorInfo : village.getVillageDoorInfoList()) {
                        		// Refresh the door if any chunks in a 3x3 area are unloaded
                            	doorPos = doorInfo.getDoorBlockPos();
                            	chunkX = doorPos.getX();
                            	chunkZ = doorPos.getZ();
                            	for (int x = -1; x <= 1; x++) {
	                            	for (int z = -1; z <= 1; z++) {
		                            	if (!world.getChunkProvider().chunkExists(chunkX + x, chunkZ + z)) {
		                            		doorInfo.setLastActivityTimestamp(villageTicks);
		                            		break;
		                            	}
	                            	}
                            	}
                            }
                        }
                    }
                }
            }
        }
    }
    */
}
