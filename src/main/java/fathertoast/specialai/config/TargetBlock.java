package fathertoast.specialai.config;

import com.google.common.base.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.*;

@SuppressWarnings( { "WeakerAccess", "unused" } )
public
class TargetBlock
{
	public static
	IBlockState parseStateForMatch( String targetState )
	{
		TargetBlock targetBlock = new TargetBlock( targetState );
		if( targetBlock.BLOCK != Blocks.AIR ) {
			if( targetBlock.MATCHERS.isEmpty( ) ) {
				return targetBlock.BLOCK.getDefaultState( );
			}
			return targetBlock.MATCHERS.get( 0 ).asMatching( targetBlock.BLOCK.getDefaultState( ) );
		}
		return Blocks.AIR.getDefaultState( );
	}
	
	// Returns a new target block set from the string property.
	public static
	TargetBlock.TargetMap newTargetDefinition( String line )
	{
		String[]       fragmentedStates = line.split( "," );
		List< String > repairedStates   = new ArrayList<>( );
		
		for( int i = 0; i < fragmentedStates.length; i++ ) {
			String fragment = fragmentedStates[ i ].trim( );
			// The block state needs to be repaired if it has multiple properties
			if( !fragment.endsWith( "]" ) && fragment.contains( "[" ) ) {
				boolean completed = false;
				
				// Combine following strings until we find the end bracket
				StringBuilder rebuilder = new StringBuilder( fragment );
				for( i++; i < fragmentedStates.length; i++ ) {
					String subfragment = fragmentedStates[ i ].trim( );
					rebuilder.append( "," ).append( subfragment );
					if( subfragment.endsWith( "]" ) ) {
						completed = true;
						break;
					}
				}
				fragment = rebuilder.toString( );
				
				if( !completed ) {
					Config.log.warn( "Reached end of line while parsing block state '{}' from single-line target ({})", fragment, line );
					continue;
				}
			}
			if( !fragment.isEmpty( ) ) {
				repairedStates.add( fragment );
			}
		}
		
		return TargetBlock.newTargetDefinition( repairedStates.toArray( new String[ 0 ] ) );
	}
	
	public static
	TargetBlock.TargetMap newTargetDefinition( String[] targetableBlockStates )
	{
		TargetBlock.TargetMap targetDefinition = new TargetBlock.TargetMap( );
		
		for( String targetState : targetableBlockStates ) {
			// Handle special case; add all blocks in namespace
			if( targetState.endsWith( "*" ) ) {
				TargetBlock.addAllModBlocks( targetDefinition, targetState.substring( 0, targetState.length( ) - 1 ) );
				continue;
			}
			
			TargetBlock targetBlock = new TargetBlock( targetState );
			if( targetBlock.BLOCK != Blocks.AIR ) {
				targetDefinition.mergeFrom( targetBlock );
			}
		}
		return targetDefinition;
	}
	
	static
	Block getStringAsBlock( String id )
	{
		Block block = Block.REGISTRY.getObject( new ResourceLocation( id ) );
		if( block == Blocks.AIR ) {
			try {
				block = Block.getBlockById( Integer.parseInt( id ) );
				if( block != Blocks.AIR ) {
					Config.log.warn( "Usage of numerical block id! ({})", id );
				}
			}
			catch( NumberFormatException numberformatexception ) {
				// Do nothing
			}
		}
		if( block == Blocks.AIR ) {
			Config.log.error( "Missing or invalid block! ({})", id );
		}
		return block;
	}
	
	private static
	void addAllModBlocks( TargetBlock.TargetMap targetDefinition, String namespace )
	{
		try {
			for( ResourceLocation blockId : Block.REGISTRY.getKeys( ) ) {
				if( blockId.toString( ).startsWith( namespace ) ) {
					TargetBlock targetBlock = new TargetBlock( Block.REGISTRY.getObject( blockId ) );
					if( targetBlock.BLOCK != null && targetBlock.BLOCK != Blocks.AIR ) {
						targetDefinition.mergeFrom( targetBlock );
					}
				}
			}
		}
		catch( Exception ex ) {
			Config.log.error( "Caught exception while adding namespace! ({}*)", namespace );
		}
	}
	
	// The block to match.
	public final Block BLOCK;
	
	// The metadata of the block to match. If this is empty, any metadata will match.
	private final List< TargetBlock.State > MATCHERS = new ArrayList<>( );
	
	// Creates a target block that matches any state.
	public
	TargetBlock( Block block ) { BLOCK = block; }
	
	// Creates a target block that matches one specific state.
	public
	TargetBlock( IBlockState block )
	{
		this( block.getBlock( ) );
		
		// Add all the properties present in the blockstate
		StateBuilder state = new StateBuilder( );
		for( IProperty< ? extends Comparable< ? > > property : block.getPropertyKeys( ) ) {
			state.add( property, block.getValue( property ) );
		}
		if( !state.isEmpty( ) ) {
			MATCHERS.add( state.toTargetState( ) );
		}
	}
	
	// Creates a target block from a string that specifies a single target.
	public
	TargetBlock( String line )
	{
		String[] pair = line.split( "\\[", 2 );
		
		BLOCK = getStringAsBlock( pair[ 0 ] );
		if( BLOCK == Blocks.AIR || pair.length < 2 ) {
			return;
		}
		if( !pair[ 1 ].endsWith( "]" ) ) {
			Config.log.warn( "Ignoring properties for broken target blockstate definition '{}' (no end bracket found)", line );
			return;
		}
		
		TargetBlock.State state = TargetBlock.State.parseState( pair[ 1 ].substring( 0, pair[ 1 ].length( ) - 1 ), BLOCK );
		if( !state.PROPERTIES_TO_MATCH.isEmpty( ) ) {
			MATCHERS.add( state );
		}
	}
	
	@Override
	public
	String toString( )
	{
		String registryName = Block.REGISTRY.getNameForObject( BLOCK ).toString( );
		if( MATCHERS.isEmpty( ) ) {
			return registryName;
		}
		
		StringBuilder str = new StringBuilder( );
		for( State state : MATCHERS ) {
			str.append( registryName ).append( "[" ).append( state.toString( ) ).append( "]," );
		}
		return str.substring( 0, str.length( ) - 1 );
	}
	
	// Used to sort this object in a hash table.
	@Override
	public
	int hashCode( )
	{
		return Block.getIdFromBlock( BLOCK );
	}
	
	public
	boolean matches( IBlockState block )
	{
		if( MATCHERS.isEmpty( ) ) {
			return true;
		}
		
		for( State state : MATCHERS ) {
			if( state.matches( block ) ) {
				return true;
			}
		}
		return false;
	}
	
	void mergeFrom( TargetBlock other )
	{
		if( MATCHERS.isEmpty( ) ) {
			Config.log.warn( "Ignoring attempt to add redundant blockstate to config with blockstate wildcard '{}'", other );
		}
		else if( other.MATCHERS.isEmpty( ) ) {
			Config.log.warn( "Adding blockstate wildcard to config with redundant blockstate(s) '{}'", this );
			MATCHERS.clear( );
		}
		else {
			MATCHERS.addAll( other.MATCHERS );
		}
	}
	
	private static final
	class State
	{
		final List< Map.Entry< IProperty< ? >, Comparable< ? > > > PROPERTIES_TO_MATCH;
		
		State( List< Map.Entry< IProperty< ? >, Comparable< ? > > > properties ) { PROPERTIES_TO_MATCH = properties; }
		
		boolean matches( IBlockState block )
		{
			for( Map.Entry< IProperty< ? >, Comparable< ? > > entry : PROPERTIES_TO_MATCH ) {
				if( notEqual( block.getValue( entry.getKey( ) ), entry.getValue( ) ) ) {
					return false;
				}
			}
			return true;
		}
		
		IBlockState asMatching( IBlockState block )
		{
			if( PROPERTIES_TO_MATCH.isEmpty( ) ) {
				// All states match the empty set
				return block;
			}
			for( Map.Entry< IProperty< ? >, Comparable< ? > > entry : PROPERTIES_TO_MATCH ) {
				block = withProperty( block, entry.getKey( ), entry.getValue( ) );
			}
			return block;
		}
		
		@Override
		public
		String toString( )
		{
			if( PROPERTIES_TO_MATCH.isEmpty( ) ) {
				return "";
			}
			StringBuilder str = new StringBuilder( );
			for( Map.Entry< IProperty< ? >, Comparable< ? > > entry : PROPERTIES_TO_MATCH ) {
				str.append( entry.getKey( ).getName( ) ).append( "=" ).append( getPropertyName( entry.getKey( ), entry.getValue( ) ) ).append( "," );
			}
			return str.substring( 0, str.length( ) - 1 );
		}
		
		< T extends Comparable< T > > IBlockState withProperty( IBlockState block, IProperty< T > property, Comparable< ? > value )
		{
			//noinspection unchecked
			return block.withProperty( property, (T) value );
		}
		
		< T extends Comparable< T > > boolean notEqual( T stateValue, Comparable< ? > targetValue ) { return !stateValue.equals( targetValue ); }
		
		< T extends Comparable< T > > String getPropertyName( IProperty< T > property, Comparable< ? > value )
		{
			//noinspection unchecked
			return property.getName( (T) value );
		}
		
		static
		State parseState( String stateString, Block block )
		{
			if( stateString.isEmpty( ) ) {
				return new State( Collections.emptyList( ) );
			}
			BlockStateContainer stateContainer = block.getBlockState( );
			
			StateBuilder builder    = new StateBuilder( );
			String[]     properties = stateString.split( "," );
			for( String combinedEntry : properties ) {
				String[] entry = combinedEntry.split( "=", 2 );
				if( entry.length != 2 ) {
					Config.log.warn( "Invalid block property entry '{}' - format must follow 'property=value'", combinedEntry );
					continue;
				}
				else if( entry[ 1 ].equals( "*" ) ) {
					continue;
				}
				
				// Parse the entry key and value
				IProperty< ? extends Comparable< ? > > property = stateContainer.getProperty( entry[ 0 ] );
				if( property == null ) {
					Config.log.warn( "Invalid block property key '{}' for block '{}'", entry[ 0 ], block.getRegistryName( ) );
					continue;
				}
				Optional< ? extends Comparable< ? > > value = property.parseValue( entry[ 1 ] );
				if( value == null || !value.isPresent( ) ) {
					Config.log.warn(
						"Invalid block property value '{}' for property key '{}' and block '{}'",
						entry[ 1 ], entry[ 0 ], block.getRegistryName( )
					);
					continue;
				}
				
				// Add the completed entry to the state
				builder.add( property, value.get( ) );
			}
			return builder.toTargetState( );
		}
	}
	
	public static final
	class StateBuilder
	{
		private final Block BLOCK;
		
		private final Map< IProperty< ? extends Comparable< ? > >, Comparable< ? > > propertiesToMatch = new HashMap<>( );
		
		public
		StateBuilder( Block block ) { BLOCK = block; }
		
		// Only used by target blocks, so we shouldn't call #toTargetBlock.
		private
		StateBuilder( ) { BLOCK = null; }
		
		public
		boolean isEmpty( ) { return propertiesToMatch.isEmpty( ); }
		
		public
		boolean contains( IProperty< ? extends Comparable< ? > > property ) { return propertiesToMatch.containsKey( property ); }
		
		public
		StateBuilder add( IProperty< ? extends Comparable< ? > > property, Comparable< ? > value )
		{
			propertiesToMatch.put( property, value );
			return this;
		}
		
		public
		TargetBlock toTargetBlock( )
		{
			TargetBlock target = new TargetBlock( BLOCK );
			if( !isEmpty( ) ) {
				target.MATCHERS.add( toTargetState( ) );
			}
			return target;
		}
		
		private
		State toTargetState( ) { return new State( new ArrayList<>( propertiesToMatch.entrySet( ) ) ); }
	}
	
	public static final
	class TargetMap
	{
		private final Map< Block, TargetBlock > UNDERLYING_MAP = new HashMap<>( );
		
		public
		boolean isEmpty( ) { return UNDERLYING_MAP.isEmpty( ); }
		
		public
		boolean matches( IBlockState block )
		{
			TargetBlock target = UNDERLYING_MAP.get( block.getBlock( ) );
			return target != null && target.matches( block );
		}
		
		public
		List< Block > getSortedBlocks( )
		{
			List< Block > list = new ArrayList<>( UNDERLYING_MAP.keySet( ) );
			list.sort( Comparator.comparing( IForgeRegistryEntry.Impl::getRegistryName ) );
			return list;
		}
		
		void mergeFrom( TargetBlock other )
		{
			TargetBlock current = UNDERLYING_MAP.get( other.BLOCK );
			if( current == null ) {
				UNDERLYING_MAP.put( other.BLOCK, other );
			}
			else {
				current.mergeFrom( other );
			}
		}
		
		@Override
		public
		String toString( )
		{
			if( UNDERLYING_MAP.isEmpty( ) ) {
				return "";
			}
			StringBuilder str = new StringBuilder( );
			for( TargetBlock target : UNDERLYING_MAP.values( ) ) {
				str.append( target.toString( ) ).append( "," );
			}
			return str.substring( 0, str.length( ) - 1 );
		}
	}
}
