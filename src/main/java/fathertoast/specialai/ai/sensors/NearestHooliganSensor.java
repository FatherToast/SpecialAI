package fathertoast.specialai.ai.sensors;

import fathertoast.specialai.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.Set;

// TODO - make villager attack AI work, eventually
public class NearestHooliganSensor extends Sensor<Villager> {

    @Override
    protected void doTick( ServerLevel serverLevel, Villager villager ) {
        villager.getBrain().setMemory( getMemory(), getNearestHooligan( villager ) );
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return Set.of( getMemory() );
    }

    private Optional<LivingEntity> getNearestHooligan( Villager villager ) {
        return getVisibleEntities( villager )
                .flatMap( (entities) -> entities.findClosest( (entity) ->
                        isMatchingEntity( villager, entity ) ) );
    }

    protected boolean isMatchingEntity( Villager villager, LivingEntity hooligan ) {
        if ( !( hooligan instanceof Player player ) ) return false;

        boolean isWithinRange = isWithinRange( villager, player );
        boolean hasAttackRep = isPlayerWithAttackRep( villager, player );
        boolean isAttackable = Sensor.isEntityTargetable( villager, player );

        return isWithinRange && hasAttackRep && isAttackable;
    }

    private boolean isWithinRange( LivingEntity villager, LivingEntity hooligan ) {
        return villager.distanceToSqr( hooligan ) < 128.0D;
    }

    private boolean isPlayerWithAttackRep( Villager villager, Player hooligan ) {
        return false;
        //return villager.getPlayerReputation( hooligan ) <= Config.VILLAGES.AI_TWEAKS.attackHooliganRep.get();
    }

    private Optional<NearestVisibleLivingEntities> getVisibleEntities( Villager villager ) {
        return villager.getBrain().getMemory( MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES );
    }

    private MemoryModuleType<LivingEntity> getMemory() {
        return MemoryModuleType.ATTACK_TARGET;
    }
}
