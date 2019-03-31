package fathertoast.specialai.village;

import fathertoast.specialai.*;
import fathertoast.specialai.config.*;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.Village;

public
class CommandVillageInfo extends CommandBase
{
	// The string used to run this command
	private static final String NAME     = "villageinfo";
	private static final String LANG_KEY = "commands." + SpecialAIMod.MOD_ID + "." + NAME + ".";
	
	@Override
	public
	String getName( ) { return NAME; }
	
	@Override
	public
	String getUsage( ICommandSender sender ) { return LANG_KEY + "usage"; }
	
	@Override
	public
	int getRequiredPermissionLevel( ) { return 0; }
	
	@Override
	public
	void execute( MinecraftServer server, ICommandSender sender, String[] args ) throws CommandException
	{
		EntityPlayerMP player = CommandBase.getCommandSenderAsPlayer( sender );
		player.world.getVillageCollection( );
		Village village = player.world.getVillageCollection( ).getNearestVillage( new BlockPos( player ), 0 );
		if( village == null )
			return;
		BlockPos center = village.getCenter( );
		
		int radius = village.getVillageRadius( );
		int houses = village.getNumVillageDoors( );
		
		int pop    = village.getNumVillagers( );
		int maxPop = (int) (houses * 0.35);
		
		int golems = player.world.getEntitiesWithinAABB( EntityIronGolem.class, new AxisAlignedBB(
			center.getX( ) - radius, center.getY( ) - 4, center.getZ( ) - radius,
			center.getX( ) + radius, center.getY( ) + 4, center.getZ( ) + radius
		) ).size( );
		int maxGolems = houses > 20 ? pop / 10 : 0;
		
		int            rep = village.getPlayerReputation( player.getUniqueID( ) );
		TextFormatting standingColor;
		String         standingKey;
		if( rep <= ReputationHandler.REPUTATION_HATED ) {
			standingColor = TextFormatting.RED;
			standingKey = "hated";
		}
		else if( rep <= Config.get( ).VILLAGES.BLOCK_ATTACK_LIMIT ) {
			standingColor = TextFormatting.YELLOW;
			standingKey = "disliked";
		}
		else if( rep <= Config.get( ).VILLAGES.BLOCK_REP_LIMIT ) {
			standingColor = TextFormatting.WHITE;
			standingKey = "neutral";
		}
		else {
			standingColor = TextFormatting.GREEN;
			standingKey = "trusted";
		}
		
		if( Config.get( ).VILLAGES.COMMAND_INCLUDE_CENTER ) {
			CommandBase.notifyCommandListener( sender, this, LANG_KEY + "pos", center.getX( ), center.getY( ), center.getZ( ) );
		}
		CommandBase.notifyCommandListener( sender, this, LANG_KEY + "size", houses, radius );
		CommandBase.notifyCommandListener( sender, this, LANG_KEY + "pop", pop, maxPop, golems, maxGolems );
		
		String standing = standingColor.toString( ) + new TextComponentTranslation( LANG_KEY + "rep." + standingKey ).getUnformattedText( ) + TextFormatting.RESET.toString( );
		CommandBase.notifyCommandListener( sender, this, LANG_KEY + "rep", rep, standing );
		CommandBase.notifyCommandListener( sender, this, LANG_KEY + "rep." + standingKey + ".note", "\n", "\n" );
	}
}
