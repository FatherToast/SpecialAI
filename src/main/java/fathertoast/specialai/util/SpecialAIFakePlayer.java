package fathertoast.specialai.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
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
    private final Mob wrappedEntity;
    
    /**
     * @param entity The entity to wrap inside a fake player.
     */
    public SpecialAIFakePlayer( Mob entity ) {
        super( (ServerLevel) entity.level, FAKE_PLAYER_PROFILE );
        wrappedEntity = entity;
        foodData = new FakeFoodStats( this );
        
        absMoveTo( entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot() );
        setDeltaMovement( entity.getDeltaMovement() );
    }
    
    /**
     * Call this method when you are done using this fake player.
     * After this is called, you should probably throw away all references to this player.
     */
    public void updateWrappedEntityState() {
        wrappedEntity.absMoveTo( getX(), getY(), getZ(), this.getYRot(), this.getXRot() );
        wrappedEntity.setDeltaMovement( getDeltaMovement() );
    }
    
    /**
     * The fake player's equally fake food stats. Converts hunger gain to health gain for the wrapped entity.
     * <p>
     * Note that the wrapped entity will never be null at the time this is created.
     */
    private static class FakeFoodStats extends FoodData {
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
    public boolean canHarmPlayer( Player player ) { return true; }
    
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
    public Entity changeDimension( ServerLevel level, ITeleporter teleporter ) {
        if( wrappedEntity != null ) { return wrappedEntity.changeDimension( level, teleporter ); }
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
    public AttributeMap getAttributes() {
        if( wrappedEntity != null ) { return wrappedEntity.getAttributes(); }
        return super.getAttributes();
    }
    
    @Override
    public MobType getMobType() {
        if( wrappedEntity != null ) { return wrappedEntity.getMobType(); }
        return super.getMobType();
    }
    
    // Equipment and hands
    
    @Override
    public void swing( InteractionHand hand, boolean sendToSelf ) {
        if( wrappedEntity != null ) { wrappedEntity.swing( hand, false ); }
    }
    
    @Override
    public HumanoidArm getMainArm() {
        if( wrappedEntity != null ) { return wrappedEntity.getMainArm(); }
        return super.getMainArm();
    }
    
    @Override
    public boolean isUsingItem() {
        if( wrappedEntity != null ) { return wrappedEntity.isUsingItem(); }
        return false;
    }
    
    @Override
    public InteractionHand getUsedItemHand() {
        if( wrappedEntity != null ) { return wrappedEntity.getUsedItemHand(); }
        return super.getUsedItemHand();
    }
    
    @Override
    public boolean isHolding( Predicate<ItemStack> itemPredicate ) {
        if( wrappedEntity != null ) { return wrappedEntity.isHolding( itemPredicate ); }
        return false;
    }
    
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        if( wrappedEntity != null ) { return wrappedEntity.getArmorSlots(); }
        return super.getArmorSlots();
    }
    
    @Override
    public ItemStack getItemBySlot( EquipmentSlot slot ) {
        if( wrappedEntity != null ) { return wrappedEntity.getItemBySlot( slot ); }
        return ItemStack.EMPTY;
    }
    
    @Override
    public void setItemSlot( EquipmentSlot slot, ItemStack item ) {
        if( wrappedEntity != null ) { wrappedEntity.setItemSlot( slot, item ); }
    }
    
    // Effects and potions
    
    @Override
    public boolean removeAllEffects() {
        if( wrappedEntity != null ) { return wrappedEntity.removeAllEffects(); }
        return false;
    }
    
    @Override
    public Collection<MobEffectInstance> getActiveEffects() {
        if( wrappedEntity != null ) { return wrappedEntity.getActiveEffects(); }
        return super.getActiveEffects();
    }
    
    @Override
    public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
        if( wrappedEntity != null ) { return wrappedEntity.getActiveEffectsMap(); }
        return super.getActiveEffectsMap();
    }
    
    @Override
    public boolean hasEffect( MobEffect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.hasEffect( effect ); }
        return false;
    }
    
    @Override
    @Nullable
    public MobEffectInstance getEffect( MobEffect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.getEffect( effect ); }
        return null;
    }

    @Override
    public boolean addEffect( MobEffectInstance effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.addEffect( effect ); }
        return false;
    }
    
    @Override
    public boolean canBeAffected( MobEffectInstance effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.canBeAffected( effect ); }
        return false;
    }
    
    @Override
    @Nullable
    public MobEffectInstance removeEffectNoUpdate( @Nullable MobEffect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.removeEffectNoUpdate( effect ); }
        return null;
    }
    
    @Override
    public boolean removeEffect( MobEffect effect ) {
        if( wrappedEntity != null ) { return wrappedEntity.removeEffect( effect ); }
        return false;
    }
}