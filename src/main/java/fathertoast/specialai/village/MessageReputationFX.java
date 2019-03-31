package fathertoast.specialai.village;

import fathertoast.specialai.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public
class MessageReputationFX implements IMessage
{
	public
	enum Type
	{
		NONE, HAPPY, ANGRY;
		
		public
		void send( World world, BlockPos pos )
		{
			if( !world.isRemote ) {
				SpecialAIMod.network( ).sendToAllAround(
					new MessageReputationFX( this, pos ),
					new NetworkRegistry.TargetPoint( world.provider.getDimension( ), pos.getX( ), pos.getY( ), pos.getZ( ), 24 )
				);
			}
		}
	}
	
	private Type     effectType = Type.NONE;
	private BlockPos effectPos  = BlockPos.ORIGIN;
	
	// Used by the client reciever
	@SuppressWarnings( "unused" )
	public
	MessageReputationFX( ) { }
	
	private
	MessageReputationFX( Type type, BlockPos pos )
	{
		effectType = type;
		effectPos = pos;
	}
	
	@Override
	public
	void fromBytes( ByteBuf buf )
	{
		int ordinal = buf.readByte( ) & 0xff;
		effectType = ordinal >= 0 && ordinal < Type.values( ).length ? Type.values( )[ ordinal ] : Type.NONE;
		effectPos = new BlockPos( buf.readInt( ), buf.readInt( ), buf.readInt( ) );
	}
	
	@Override
	public
	void toBytes( ByteBuf buf )
	{
		buf.writeByte( effectType.ordinal( ) );
		buf.writeInt( effectPos.getX( ) );
		buf.writeInt( effectPos.getY( ) );
		buf.writeInt( effectPos.getZ( ) );
	}
	
	public static
	class Handler implements IMessageHandler< MessageReputationFX, IMessage >
	{
		@Override
		public
		IMessage onMessage( MessageReputationFX message, MessageContext ctx )
		{
			switch( message.effectType ) {
				case HAPPY:
					spawnParticles(
						Minecraft.getMinecraft( ).world, message.effectPos,
						EnumParticleTypes.VILLAGER_HAPPY, 10
					);
					break;
				case ANGRY:
					spawnParticles(
						Minecraft.getMinecraft( ).world, message.effectPos,
						EnumParticleTypes.VILLAGER_ANGRY, 5
					);
					break;
			}
			return null;
		}
		
		private
		void spawnParticles( World world, BlockPos pos, EnumParticleTypes particle, int count )
		{
			for( int i = 0; i < count; i++ ) {
				world.spawnParticle(
					particle,
					pos.getX( ) + 0.5F + (world.rand.nextFloat( ) - 0.5F) * 1.5F,
					pos.getY( ) + 0.5F + (world.rand.nextFloat( ) - 0.5F) * 1.5F,
					pos.getZ( ) + 0.5F + (world.rand.nextFloat( ) - 0.5F) * 1.5F,
					world.rand.nextGaussian( ) * 0.02,
					world.rand.nextGaussian( ) * 0.02,
					world.rand.nextGaussian( ) * 0.02
				);
			}
		}
	}
}
