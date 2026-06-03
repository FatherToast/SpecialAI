package fathertoast.specialai.ai.elite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.function.Supplier;

/**
 * This is a queued check intended to be used in conjunction with {@link fathertoast.crust.api.lib.DeferredAction}.
 * <p>
 * This check is intended to be queued for pathfinding mobs that have just been flung by
 * directly setting their motion vector. Sometimes when that happens, the mob may end
 * up ahead of their current path's next node. This can make them backtrack to said
 * node for a second after landing (which looks really weird), and this check aims to mitigate that.
 */
public final class PostJumpCheck implements Supplier<Boolean> {
    
    /** The mob that performed a leap. */
    private final Mob leaper;
    /** The target entity that was leaped towards. */
    private final LivingEntity target;
    /** When this timer runs out, the check yields. */
    private int giveUpCounter;
    
    /** The last */
    private BlockPos.MutableBlockPos lastNodePos;
    
    
    public PostJumpCheck( Mob leaper, LivingEntity target, int giveUpCounter ) {
        this.leaper = leaper;
        this.target = target;
        this.giveUpCounter = giveUpCounter;
    }
    
    @Override
    public Boolean get() {
        --giveUpCounter;
        
        // Give up early if either leaper or target
        // is dead or unloaded.
        if( !leaper.isAlive() || !target.isAlive() )
            return true;
        
        if( leaper.getNavigation().isDone() || giveUpCounter <= 0 )
            return true;
        
        final Path path = leaper.getNavigation().getPath();
        
        if( path != null ) {
            final Node nextNode = path.getNextNode();
            
            // Update next node while in the air so the
            // mob keeps moving smoothly towards the target.
            if( !isNodeAtTarget( nextNode, target ) ) {
                path.replaceNode( path.getNextNodeIndex(), new Node( target.getBlockX(), target.getBlockY(), target.getBlockZ() ) );
            }
        }
        return false;
    }
    
    /**
     * @return True if the specified node's position
     * is the same as the target's block position.
     */
    private static boolean isNodeAtTarget( Node node, LivingEntity target ) {
        return node.x == target.getX() && node.y == target.getY() && node.z == target.getZ();
    }
}