package fathertoast.specialai.util;

import com.mojang.authlib.GameProfile;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpecialAIFakePlayer extends FakePlayer {
    /** The fake profile used for all fake players used by this mod. */
    private static final GameProfile FAKE_PLAYER_PROFILE = new GameProfile( null, "[SpecialAIFakePlayer]" );
    
    /** The entity posing as this fake player. */
    private final MobEntity wrappedEntity;
    
    /**
     * @param entity The entity to wrap inside a fake player.
     */
    public SpecialAIFakePlayer( MobEntity entity ) {
        super( (ServerWorld) entity.level, FAKE_PLAYER_PROFILE );
        wrappedEntity = entity;
        foodData = new FakeFoodStats( this );
        
        absMoveTo( entity.getX(), entity.getY(), entity.getZ(), entity.yRot, entity.xRot );
        setDeltaMovement( entity.getDeltaMovement() );
    }
    
    /**
     * Call this method when you are done using this fake player.
     * After this is called, you should probably throw away all references to this player.
     */
    public void updateWrappedEntityState() {
        wrappedEntity.absMoveTo( getX(), getY(), getZ(), yRot, xRot );
        wrappedEntity.setDeltaMovement( getDeltaMovement() );
    }
    
    /**
     * The fake player's equally fake food stats. Converts hunger gain to health gain for the wrapped entity.
     * <p>
     * Note that the wrapped entity will never be null at the time this is created.
     */
    private static class FakeFoodStats extends FoodStats {
        /** The fake player this food stats belong to. */
        final SpecialAIFakePlayer fakePlayer;
        
        FakeFoodStats( SpecialAIFakePlayer player ) { fakePlayer = player; }
        
        @Override
        public void eat( int food, float saturationModifier ) { fakePlayer.wrappedEntity.heal( Math.max( food, 1.0F ) ); }
        
        @Override
        public int getFoodLevel() { return 10; }
        
        @Override
        public boolean needsFood() { return true; }
        
        @Override
        public float getSaturationLevel() { return 10.0F; }
    }
    
    // Health and damage
    
    @Override
    public boolean isInvulnerableTo( DamageSource source ) {
        if( wrappedEntity != null ) { return wrappedEntity.isInvulnerableTo( source ); }
        return super.isInvulnerableTo( source );
    }
    
    @Override
    public boolean canHarmPlayer( PlayerEntity player ) { return true; }
    
    @Override
    public void die( DamageSource source ) { if( wrappedEntity != null ) { wrappedEntity.die( source ); } }
    
    @Override
    public void kill() { if( wrappedEntity != null ) { wrappedEntity.kill(); } }
    
    @Override
    public void heal( float amount ) { if( wrappedEntity != null ) { wrappedEntity.heal( amount ); } }
    
    @Override
    public float getHealth() {
        if( wrappedEntity != null ) { return wrappedEntity.getHealth(); }
        return super.getHealth();
    }
    
    @Override
    public void setHealth( float amount ) { if( wrappedEntity != null ) { wrappedEntity.setHealth( amount ); } }
    
    @Override
    public boolean isDeadOrDying() {
        if( wrappedEntity != null ) { return wrappedEntity.isDeadOrDying(); }
        return super.isDeadOrDying();
    }
    
    @Override
    public boolean hurt( DamageSource source, float damage ) {
        if( wrappedEntity != null ) { return wrappedEntity.hurt( source, damage ); }
        return false;
    }
    
    // Movement
    
    @Override
    @Nullable
    public Entity changeDimension( ServerWorld world, ITeleporter teleporter ) {
        if( wrappedEntity != null ) { return wrappedEntity.changeDimension( world, teleporter ); }
        return null;
    }
    
    @Override
    public boolean startRiding( Entity mount, boolean force ) {
        if( wrappedEntity != null ) { return wrappedEntity.startRiding( mount, force ); }
        return false;
    }
    
    @Override
    public void stopRiding() { if( wrappedEntity != null ) { wrappedEntity.stopRiding(); } }
    
    // Attributes
    
    @Override
    public AttributeModifierManager getAttributes() {
        if( wrappedEntity != null ) { return wrappedEntity.getAttributes(); }
        return super.getAttributes();
    }
    
    @Override
    public CreatureAttribute getMobType() {
        if( wrappedEntity != null ) { return wrappedEntity.getMobType(); }
        return super.getMobType();
    }
    
    // Equipment and hands
    
    @Override
    public void swing( Hand hand, boolean sendToSelf ) {
        if( wrappedEntity != null ) { wrappedEntity.swing( hand, false ); }
    }
    
    @Override
    public HandSide getMainArm() {
        if( wrappedEntity != null ) { return wrappedEntity.getMainArm(); }
        return super.getMainArm();
    }
    
    @Override
    public boolean isUsingItem() {
        if( wrappedEntity != null ) { return wrappedEntity.isUsingItem(); }
        return false;
    }
    
    @Override
    public Hand getUsedItemHand() {
        if( wrappedEntity != null ) { return wrappedEntity.getUsedItemHand(); }
        return super.getUsedItemHand();
    }
    
    @Override
    public boolean isHolding( Predicate<Item> itemPredicate ) {
        if( wrappedEntity != null ) { return wrappedEntity.isHolding( itemPredicate ); }
        return false;
    }
    
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        if( wrappedEntity != null ) { return wrappedEntity.getArmorSlots(); }
        return super.getArmorSlots();
    }
    
    @Override
    public ItemStack getItemBySlot( EquipmentSlotType slot ) {
        if( wrappedEntity != null ) { return wrappedEntity.getItemBySlot( slot ); }
        return ItemStack.EMPTY;
    }
    
    @Override
    public void setItemSlot( EquipmentSlotType slot, ItemStack item ) {
        if( wrappedEntity != null ) { wrappedEntity.setItemSlot( slot, item ); }
    }
    
    // Effects and potions
    
    @Override
    public boolean removeAllEffects() {
        if( wrappedEntity != null ) { return wrappedEntity.removeAllEffects(); }
        return false;
    }
    
    @Override
    public Collection<EffectInstance> getActiveEffects() {
        if( wrappedEntity != null ) { return wrappedEntity.getActiveEffects(); }
        return super.getActiveEffects();
    }
    
    @Override
    public Map<Effect, EffectInstance> getActiveEffectsMap() {
        if( wrappedEntity != null ) { return wrappedEntity.getActiveEffectsMap(); }
        return super.getActiveEffectsMap();
    }
    
    @Override
    public boolean hasEffect( Effect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.hasEffect( effect ); }
        return false;
    }
    
    @Override
    @Nullable
    public EffectInstance getEffect( Effect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.getEffect( effect ); }
        return null;
    }
    
    @Override
    public boolean addEffect( EffectInstance effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.addEffect( effect ); }
        return false;
    }
    
    @Override
    public boolean canBeAffected( EffectInstance effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.canBeAffected( effect ); }
        return false;
    }
    
    @Override
    @Nullable
    public EffectInstance removeEffectNoUpdate( @Nullable Effect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.removeEffectNoUpdate( effect ); }
        return null;
    }
    
    @Override
    public boolean removeEffect( Effect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.removeEffect( effect ); }
        return false;
    }
}