package fathertoast.specialai.ai.elite;

import com.google.common.collect.Lists;
import fathertoast.crust.api.lib.LevelEventHelper;
import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.ai.AIManager;
import fathertoast.specialai.config.Config;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.AbstractSpawner;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * This AI causes an entity to act like a dungeon spawner block, spawning 'copies' of itself.
 * The spawner logic is saved to and loaded from nbt, identical to a vanilla spawner.
 */
public class SpawnerEliteGoal extends AbstractEliteGoal {
    /** The mob spawner logic for this AI. */
    private final AbstractSpawner spawnerLogic;
    /** The nbt compound used to store any additional data used by the AI. */
    private final CompoundNBT extraData;
    
    /** Time until the entity can check line of sight again. */
    private int canSeeTicks;
    /** Time until the next save update. */
    private int saveTicks;
    
    SpawnerEliteGoal( MobEntity entity, CompoundNBT aiTag ) {
        super( entity, aiTag );
        spawnerLogic = new SpawnerLogic( this );
        
        final boolean initialized = EliteAIType.SPAWNER.hasTag( aiTag );
        extraData = EliteAIType.SPAWNER.getTag( aiTag );
        
        // Load from tag if initialized, otherwise initialize by saving to tag
        if( initialized ) { spawnerLogic.load( extraData ); }
        else { save(); }
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        return checkSight( false );
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return checkSight( true );
    }
    
    /**
     * @return Performs a sight check to the target, if possible.
     * <p>
     * Returns false if there is no target or the target can't be seen; and true if the target exists and can be seen.
     * <p>
     * If the last sight check was too recent, returns the provided fallback value.
     */
    private boolean checkSight( boolean fallback ) {
        final LivingEntity target = mob.getTarget();
        if( target != null ) {
            if( canSeeTicks-- > 0 ) return fallback;
            canSeeTicks = 4 + mob.getRandom().nextInt( 7 );
            return mob.canSee( target );
        }
        return false;
    }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        spawnerLogic.tick();
        if( saveTicks-- <= 0 ) { save(); }
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        save();
    }
    
    /** Saves the current state of the spawner logic and resets the save timer. */
    private void save() {
        spawnerLogic.save( extraData );
        saveTicks = 10 + mob.getRandom().nextInt( 20 );
    }
    
    /**
     * Holds the underlying spawner logic used by the spawner elite AI.
     * This only exists on the server side, so the client part of the spawner logic is ignored.
     */
    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static final class SpawnerLogic extends AbstractSpawner {
        /** The AI goal using this logic. */
        private final SpawnerEliteGoal aiGoal;
        /** The entity using this logic. */
        private final MobEntity mob;
        
        /** List of mobs that can be spawned; will pick a new mob for each spawn wave. */
        private final List<WeightedSpawnerEntity> spawnPotentials = Lists.newArrayList();
        /** Minimum time between spawn waves. */
        private int minSpawnDelay = Config.ELITE_AI.SPAWNER.cooldown.getMin();
        /** Maximum time between spawn waves. */
        private int maxSpawnDelay = Config.ELITE_AI.SPAWNER.cooldown.getMax();
        /** Number of mobs to try spawning with each spawn wave. */
        private int spawnCount = Config.ELITE_AI.SPAWNER.spawnCount.get();
        /** Maximum number of similar mobs nearby. */
        private int maxNearbyEntities = Config.ELITE_AI.SPAWNER.maxNearby.get();
        /** Range to activate the spawner logic. */
        private int requiredPlayerRange = Config.ELITE_AI.SPAWNER.range.get();
        /** Maximum horizontal range that mobs can spawn in. */
        private int spawnRange = Config.ELITE_AI.SPAWNER.spawnRange.get();
        
        /** Time before the next spawn wave. */
        private int spawnDelay = Config.ELITE_AI.SPAWNER.initialCooldown.get();
        /** Data of the mob to spawn for the next wave. */
        private WeightedSpawnerEntity nextSpawnData = new WeightedSpawnerEntity();
        
        SpawnerLogic( SpawnerEliteGoal goal ) {
            aiGoal = goal;
            mob = goal.mob;
            setEntityId( mob.getType() );
        }
        
        /** Sends an event to the client. */
        @Override
        public void broadcastEvent( int eventId ) {
            // Ignore; there is no client side AI to receive events
        }
        
        /** @return The world this spawner is in. */
        @Override
        public World getLevel() { return mob.level; }
        
        /** @return The block position this spawner is at. */
        @Override
        public BlockPos getPos() { return mob.blockPosition(); }
        
        /** @return The entity acting as the spawner, if any. */
        @Override
        @Nullable
        public Entity getSpawnerEntity() { return mob; }
        
        /** Sets the mob type this will spawn. */
        @Override
        public void setEntityId( EntityType<?> entityType ) {
            // Carry over deprecated registry from base spawner logic
            //noinspection deprecation
            nextSpawnData.getTag().putString( TAG_ENTITY_ID, Registry.ENTITY_TYPE.getKey( entityType ).toString() );
        }
        
        /** Sets the data of the mob to spawn for the next wave. */
        @Override
        public void setNextSpawnData( WeightedSpawnerEntity spawnData ) { nextSpawnData = spawnData; }
        
        /** Called each tick while this spawner is active. */
        @Override
        public void tick() {
            final LivingEntity target = mob.getTarget();
            if( target != null && mob.distanceToSqr( target ) <= requiredPlayerRange * requiredPlayerRange ) {
                if( spawnDelay == -1 ) {
                    delay();
                }
                
                if( spawnDelay > 0 ) {
                    spawnDelay--;
                }
                else {
                    spawnWave();
                    delay();
                }
            }
        }
        
        /** Spawns a wave of mobs from the spawner. */
        private void spawnWave() {
            final AxisAlignedBB nearbyCheckBB = new AxisAlignedBB( mob.blockPosition() ).inflate( spawnRange );
            final BlockPos headPos = new BlockPos( mob.getX(), mob.getEyeY(), mob.getZ() );
            final ServerWorld world = (ServerWorld) getLevel();
            final Random random = mob.getRandom();
            
            for( int i = 0; i < spawnCount; i++ ) {
                // Load entity type
                final CompoundNBT spawnData = nextSpawnData.getTag();
                final Optional<EntityType<?>> entityType = EntityType.by( spawnData );
                if( !entityType.isPresent() ) break;
                
                // Pick a random spawn position; ignore any dumb nbt position (unlike base spawn logic)
                final Vector3d spawnPos = mob.position().add(
                        (random.nextDouble() - random.nextDouble()) * (double) spawnRange,
                        random.nextInt( 3 ) - 1,
                        (random.nextDouble() - random.nextDouble()) * (double) spawnRange
                );
                
                // Test if the spawn position is valid
                if( !world.noCollision( entityType.get().getAABB( spawnPos.x, spawnPos.y, spawnPos.z ) ) ) continue;
                
                // Create the entity to spawn
                final Entity newEntity = EntityType.loadEntityRecursive( spawnData, world, ( loadEntity ) -> {
                    loadEntity.moveTo( spawnPos.x, spawnPos.y, spawnPos.z, loadEntity.yRot, loadEntity.xRot );
                    return loadEntity;
                } );
                if( newEntity == null ) break;
                
                // Enforce max nearby entities
                int nearbyEntities = world.getEntitiesOfClass( newEntity.getClass(), nearbyCheckBB ).size();
                if( mob.getClass().equals( newEntity.getClass() ) ) nearbyEntities--;
                if( nearbyEntities >= maxNearbyEntities ) break;
                
                // Attempt to spawn the entity
                newEntity.moveTo( newEntity.getX(), newEntity.getY(), newEntity.getZ(),
                        random.nextFloat() * 360.0F, 0.0F );
                final MobEntity newMob = newEntity instanceof MobEntity ? (MobEntity) newEntity : null;
                if( newMob != null ) {
                    // Create the elite AI compound to prevent any elite AIs from generating on the spawned entity
                    NBTHelper.getForgeData( newMob, SpecialAI.MOD_ID, AIManager.TAG_ELITE_AI );
                    
                    // Fire the Forge can spawn event
                    if( !ForgeEventFactory.canEntitySpawnSpawner( newMob, world, (float) newEntity.getX(), (float) newEntity.getY(), (float) newEntity.getZ(), this ) )
                        continue;
                    
                    // If needed, perform the standard entity spawn initialization
                    if( nextSpawnData.getTag().size() == 1 && NBTHelper.containsString( nextSpawnData.getTag(), TAG_ENTITY_ID ) &&
                            !ForgeEventFactory.doSpecialSpawn( newMob, world, (float) newEntity.getX(), (float) newEntity.getY(), (float) newEntity.getZ(), this, SpawnReason.SPAWNER ) ) {
                        newMob.finalizeSpawn( world, world.getCurrentDifficultyAt( newEntity.blockPosition() ),
                                SpawnReason.SPAWNER, null, null );
                    }
                }
                if( !world.tryAddFreshEntityWithPassengers( newEntity ) ) break;
                
                // This spawn was successful
                LevelEventHelper.SMOKE_AND_FLAME.play( world, headPos );
                if( newMob != null ) {
                    newMob.spawnAnim();
                    
                    // Copy target from the spawner mob
                    newMob.setTarget( mob.getTarget() );
                    newMob.setLastHurtByMob( mob.getTarget() );
                }
            }
        }
        
        /** Resets the spawn delay and picks a new mob to spawn, if needed. */
        private void delay() {
            // Reset spawn delay
            if( maxSpawnDelay <= minSpawnDelay ) {
                spawnDelay = minSpawnDelay;
            }
            else {
                spawnDelay = minSpawnDelay + mob.getRandom().nextInt( maxSpawnDelay - minSpawnDelay );
            }
            // Reset mob selection
            if( !spawnPotentials.isEmpty() ) {
                setNextSpawnData( WeightedRandom.getRandomItem( mob.getRandom(), spawnPotentials ) );
            }
            // Save the updated data
            aiGoal.save();
        }
        
        // Tag names for the nbt save data
        private static final String TAG_ENTITY_ID = "id";
        private static final String TAG_DELAY = "Delay";
        private static final String TAG_MIN_DELAY = "MinSpawnDelay";
        private static final String TAG_MAX_DELAY = "MaxSpawnDelay";
        private static final String TAG_SPAWN_COUNT = "SpawnCount";
        private static final String TAG_MAX_NEARBY = "MaxNearbyEntities";
        private static final String TAG_ACTIVATION_RANGE = "RequiredPlayerRange";
        private static final String TAG_SPAWN_RANGE = "SpawnRange";
        private static final String TAG_SPAWN_DATA = "SpawnData";
        private static final String TAG_SPAWN_POTENTIALS = "SpawnPotentials";
        
        /** Loads the spawner from nbt. */
        @Override
        public void load( CompoundNBT tag ) {
            spawnDelay = tag.getShort( TAG_DELAY );
            spawnPotentials.clear();
            if( NBTHelper.containsList( tag, TAG_SPAWN_POTENTIALS ) ) {
                ListNBT listnbt = tag.getList( TAG_SPAWN_POTENTIALS, NBTHelper.ID_COMPOUND );
                
                for( int i = 0; i < listnbt.size(); ++i ) {
                    spawnPotentials.add( new WeightedSpawnerEntity( listnbt.getCompound( i ) ) );
                }
            }
            if( NBTHelper.containsCompound( tag, TAG_SPAWN_DATA ) ) {
                setNextSpawnData( new WeightedSpawnerEntity( 1, tag.getCompound( TAG_SPAWN_DATA ) ) );
            }
            else if( !spawnPotentials.isEmpty() ) {
                setNextSpawnData( WeightedRandom.getRandomItem( mob.getRandom(), spawnPotentials ) );
            }
            if( NBTHelper.containsNumber( tag, TAG_MIN_DELAY ) ) {
                minSpawnDelay = tag.getShort( TAG_MIN_DELAY );
                maxSpawnDelay = tag.getShort( TAG_MAX_DELAY );
                spawnCount = tag.getShort( TAG_SPAWN_COUNT );
            }
            if( NBTHelper.containsNumber( tag, TAG_MAX_NEARBY ) ) {
                maxNearbyEntities = tag.getShort( TAG_MAX_NEARBY );
                requiredPlayerRange = tag.getShort( TAG_ACTIVATION_RANGE );
            }
            if( NBTHelper.containsNumber( tag, TAG_SPAWN_RANGE ) ) {
                spawnRange = tag.getShort( TAG_SPAWN_RANGE );
            }
        }
        
        /** Saves the spawner to nbt. */
        @Override
        public CompoundNBT save( CompoundNBT tag ) {
            tag.putShort( TAG_DELAY, (short) spawnDelay );
            tag.putShort( TAG_MIN_DELAY, (short) minSpawnDelay );
            tag.putShort( TAG_MAX_DELAY, (short) maxSpawnDelay );
            tag.putShort( TAG_SPAWN_COUNT, (short) spawnCount );
            tag.putShort( TAG_MAX_NEARBY, (short) maxNearbyEntities );
            tag.putShort( TAG_ACTIVATION_RANGE, (short) requiredPlayerRange );
            tag.putShort( TAG_SPAWN_RANGE, (short) spawnRange );
            verifyEntityId();
            tag.put( TAG_SPAWN_DATA, nextSpawnData.getTag().copy() );
            ListNBT potentials = new ListNBT();
            if( spawnPotentials.isEmpty() ) {
                potentials.add( nextSpawnData.save() );
            }
            else {
                for( WeightedSpawnerEntity weightedspawnerentity : spawnPotentials ) {
                    potentials.add( weightedspawnerentity.save() );
                }
            }
            tag.put( TAG_SPAWN_POTENTIALS, potentials );
            return tag;
        }
        
        /** Makes sure the saved entity id is valid. */
        private void verifyEntityId() {
            // Try to load the entity id
            final String id = nextSpawnData.getTag().getString( TAG_ENTITY_ID );
            ResourceLocation regKey;
            try { regKey = StringUtils.isNullOrEmpty( id ) ? null : new ResourceLocation( id ); }
            catch( ResourceLocationException ex ) { regKey = null; }
            
            if( regKey == null ) {
                // Entity id is invalid; reset to default
                setEntityId( mob.getType() );
            }
        }
    }
}