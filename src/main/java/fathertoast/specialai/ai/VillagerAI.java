package fathertoast.specialai.ai;

import com.mojang.datafixers.util.Pair;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.ai.sensors.NearestHooliganSensor;
import fathertoast.specialai.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraftforge.common.util.BrainBuilder;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingMakeBrainEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

public final class VillagerAI {
    
    
    //
    //----------------------------------- REGISTRY ------------------------------------------
    //
    
    
    /** Activity deferred register for Special AI. */
    public static final DeferredRegister<Activity> ACTIVITY_REGISTER = DeferredRegister.create( ForgeRegistries.ACTIVITIES, SpecialAI.MOD_ID );
    /** SensorType deferred register for Special AI. */
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPE_REGISTER = DeferredRegister.create( ForgeRegistries.SENSOR_TYPES, SpecialAI.MOD_ID );
    
    
    public static final RegistryObject<Activity> ATTACK_HOOLIGAN_ACTIVITY
            = registerActivity( "attack_hooligan" );
    
    public static final RegistryObject<SensorType<NearestHooliganSensor>> NEAREST_HOOLIGAN_SENSOR
            = registerSensorType( "nearest_hooligan", NearestHooliganSensor::new );
    
    
    /** Helper method for registering activities. */
    private static RegistryObject<Activity> registerActivity( String name ) {
        return ACTIVITY_REGISTER.register( name, () -> new Activity( ResourceLocation.fromNamespaceAndPath( SpecialAI.MOD_ID, name ).toString() ) );
    }
    
    /** Helper method for registering sensor types. */
    private static <T extends Sensor<?>> RegistryObject<SensorType<T>> registerSensorType( String name, Supplier<T> sensorSupplier ) {
        return SENSOR_TYPE_REGISTER.register( name, () -> new SensorType<>( sensorSupplier ) );
    }
    
    
    //
    //----------------------------------- REPUTATION ------------------------------------------
    //
    
    
    /**
     * Called from {@link fathertoast.specialai.GameEventHandler#onBlockBreak(BlockEvent.BreakEvent)}.<br><br>
     * <p>
     * Slander or praise the reputation of players when they break certain
     * blocks in a village.
     */
    public static void onBlockBreak( BlockEvent.BreakEvent event ) {
        if( !event.getLevel().isClientSide() ) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            ServerLevel serverLevel = player.serverLevel();
            
            // Don't do anything if the player is in creative or spec mode
            if( player.isCreative() || player.isSpectator() ) return;
            
            // Check if there are even villagers nearby before doing anything else
            List<Villager> nearbyVillagers = event.getLevel().getEntitiesOfClass( Villager.class, event.getPlayer().getBoundingBox().inflate( 15 ) );
            
            if( nearbyVillagers.isEmpty() ) return;
            
            int repChange = (int) Config.VILLAGES.REPUTATION.repChangingBlocks.getValue( event.getState().getBlock() );
            
            // Nothing would change, abort
            if( repChange == 0 ) return;
            
            GossipType gossipType = GossipType.MINOR_NEGATIVE;
            
            if( repChange > 0 ) {
                gossipType = GossipType.MINOR_POSITIVE;
            }
            // If negative rep, invert value
            else {
                repChange = -repChange;
            }
            
            if( serverLevel.isVillage( player.blockPosition() ) ) {
                for( Villager villager : nearbyVillagers ) {
                    int reputationForPlayer = villager.getPlayerReputation( player );
                    
                    // Villagers will overlook breaking bad reputation blocks if the
                    // player has very good reputation (or whatever the config says is the threshold)
                    if( gossipType == GossipType.MINOR_POSITIVE && reputationForPlayer >= Config.VILLAGES.REPUTATION.breakBlockThreshold.get() )
                        return;
                    
                    villager.getGossips().add( player.getUUID(), gossipType, repChange );
                    reputationParticle( (ServerLevel) villager.level(), villager, gossipType == GossipType.MINOR_NEGATIVE );
                }
            }
        }
    }
    
    /**
     * Called from {@link fathertoast.specialai.GameEventHandler#onLivingDeath(LivingDeathEvent)}.<br><br>
     * <p>
     * Slander or praise the reputation of players when they kill certain
     * creatures in a village.
     */
    public static void onPlayerKillLiving( LivingDeathEvent event, ServerPlayer player ) {
        // Don't do anything if the player is in creative or spec mode
        if( player.isCreative() || player.isSpectator() ) return;
        
        // Check if there are even villagers nearby before doing anything else
        List<Villager> nearbyVillagers = player.level().getEntitiesOfClass( Villager.class, player.getBoundingBox().inflate( 15 ) );
        
        if( nearbyVillagers.isEmpty() ) return;
        
        int repChange = (int) Config.VILLAGES.REPUTATION.repChangingCreatures.getValue( event.getEntity() );
        
        // Nothing would change, abort
        if( repChange == 0 ) return;
        
        GossipType gossipType = GossipType.MINOR_NEGATIVE;
        
        if( repChange > 0 ) {
            gossipType = GossipType.MINOR_POSITIVE;
        }
        // If negative rep, invert value
        else {
            repChange = -repChange;
        }
        ServerLevel serverLevel = player.serverLevel();
        
        if( serverLevel.isVillage( player.blockPosition() ) ) {
            for( Villager villager : nearbyVillagers ) {
                int reputationForPlayer = villager.getPlayerReputation( player );
                
                // Villagers will overlook killing bad reputation creatures if the
                // player has very good reputation (or whatever the config says is the threshold)
                if( gossipType == GossipType.MINOR_POSITIVE && reputationForPlayer >= Config.VILLAGES.REPUTATION.killCreatureThreshold.get() )
                    return;
                
                villager.getGossips().add( player.getUUID(), gossipType, repChange );
                reputationParticle( (ServerLevel) villager.level(), villager, gossipType == GossipType.MINOR_NEGATIVE );
            }
        }
    }
    
    /**
     * Called from {@link fathertoast.specialai.GameEventHandler#onTrampleFarmland(BlockEvent.FarmlandTrampleEvent)}.<br><br>
     * <p>
     * Slander the reputation of players when they trample farmland
     * near farmer villagers.
     */
    public static void onTrampleFarmland( ServerPlayer player, BlockPos tramplePos ) {
        // Don't do anything if the player is in creative or spec mode
        if( player.isCreative() || player.isSpectator() ) return;
        
        // Check if there are even villagers nearby before doing anything else
        List<Villager> nearbyVillagers = player.level().getEntitiesOfClass( Villager.class, player.getBoundingBox().inflate( 15 ) );
        
        if( nearbyVillagers.isEmpty() ) return;
        
        int repChange = Config.VILLAGES.REPUTATION.trampleFarmlandAnger.get();
        
        // Nothing would change, abort
        if( repChange == 0 ) return;
        
        for( Villager villager : nearbyVillagers ) {
            // Skip villagers that are not farmers
            if( villager.getVillagerData().getProfession() != VillagerProfession.FARMER )
                continue;
            
            villager.getGossips().add( player.getUUID(), GossipType.MINOR_NEGATIVE, -repChange );
            reputationParticle( (ServerLevel) villager.level(), villager, true );
        }
    }
    
    /**
     * Called from {@link fathertoast.specialai.GameEventHandler#onOpenContainer(PlayerContainerEvent.Open)}
     * if the event's container is an instance of {@link MerchantMenu}.<br><br>
     * <p>
     * Checks if the container's merchant is a villager, and if so, check if
     * the player that is trying to trade has too bad a reputation to be allowed to trade and
     * cancel the opening of the container.
     */
    public static void onPlayerTryTrade( PlayerContainerEvent.Open event, MerchantMenu menu ) {
        if( !event.getEntity().isCreative() && menu.trader instanceof Villager villager ) {
            if( villager.getPlayerReputation( event.getEntity() ) <= Config.VILLAGES.AI_TWEAKS.refuseTradeRep.get() ) {
                villager.playSound( SoundEvents.VILLAGER_NO, 1.0F, villager.getVoicePitch() );
                event.setCanceled( true );
            }
        }
    }
    
    /** Spawns a singular angry or happy villager particle. */
    private static void reputationParticle( ServerLevel level, LivingEntity entity, boolean angry ) {
        level.sendParticles( angry ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getEyeY() + 0.3D, entity.getZ(),
                1, 0.0D, 0.01D, 0.0D, 0.001D );
    }
    
    
    //
    //----------------------------------- AI TWEAKS ------------------------------------------
    //
    
    
    // TODO - Make this stuff work; understanding the brain AI system is like trying to read IKEA assembly manuals
    
    /**
     * Called from {@link fathertoast.specialai.GameEventHandler#onLivingMakeBrain(LivingMakeBrainEvent)}.<br><br>
     * <p>
     * Villager activities, sensors and memory types can be modified here.
     */
    public static void initSpecialAI( LivingMakeBrainEvent event, Villager villager ) {
        BrainBuilder<Villager> builder = event.getTypedBrainBuilder( villager );
        
        builder.getSensorTypes().add( NEAREST_HOOLIGAN_SENSOR.get() );
        
        builder.addMemoriesToEraseWhenActivityStopped( ATTACK_HOOLIGAN_ACTIVITY.get(), List.of( MemoryModuleType.ATTACK_TARGET ) );
        
        builder.addBehaviorToActivityByPriority( 40, ATTACK_HOOLIGAN_ACTIVITY.get(),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create( 1.2F ) );
        
        builder.addBehaviorToActivityByPriority( 41, ATTACK_HOOLIGAN_ACTIVITY.get(),
                BehaviorBuilder.triggerIf( ( villgr ) -> (!villgr.isBaby()), MeleeAttack.create( 40 ) ) );
        
        builder.addBehaviorToActivityByPriority( 42, ATTACK_HOOLIGAN_ACTIVITY.get(),
                StopAttackingIfTargetInvalid.create() );
        
        builder.addBehaviorToActivityByPriority( 43, ATTACK_HOOLIGAN_ACTIVITY.get(),
                EraseMemoryIf.create( ( villgr ) -> villgr.getBrain().isActive( Activity.PANIC ), MemoryModuleType.ATTACK_TARGET ) );
        
        builder.getActiveActivites().add( ATTACK_HOOLIGAN_ACTIVITY.get() );
        
        builder.addRequirementsToActivity( ATTACK_HOOLIGAN_ACTIVITY.get(),
                List.of( Pair.of( MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT ) ) );
    }
    
    // Utility class, no instantiation
    private VillagerAI() { }
}