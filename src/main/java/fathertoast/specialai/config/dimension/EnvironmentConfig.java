package fathertoast.specialai.config.dimension;

import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.collection.BlockStateListField;
import fathertoast.crust.api.config.common.value.collection.BlockStateList;
import net.minecraft.world.level.block.Blocks;

@SuppressWarnings( "UnstableApiUsage" )
public class EnvironmentConfig extends DimensionConfig {
    
    public final NaturalBlocks NATURAL_BLOCKS;
    
    EnvironmentConfig( ConfigManager manager, String dir, DimensionConfigGroup dimConfigs, String name ) {
        super( manager, dir, dimConfigs, name );
        
        NATURAL_BLOCKS = new NaturalBlocks( this );
    }
    
    public static class NaturalBlocks extends DimensionCategory {
        
        public final BlockStateListField lightSourceList;
        
        
        public NaturalBlocks( EnvironmentConfig parent ) {
            super( parent, "natural_blocks",
                    "Options for defining blocks that are considered natural in the dimension this config belongs to." );
            
            lightSourceList = SPEC.define( new BlockStateListField( "light_sources", createDefaultNaturalLightSources(),
                    "A list of light source blocks that are considered natural/native to this config's dimension.",
                    "Used by the idle griefing AI to avoid destroying light source blocks that are not likely placed by players." ) );
        }
        
        private BlockStateList createDefaultNaturalLightSources() {
            final BlockStateList.Builder<?> builder = new BlockStateList.Builder<>();
            
            if( isOverworldDimension() ) {
                builder.add( Blocks.GLOW_LICHEN );
                builder.add( Blocks.SEA_PICKLE );
                builder.add( Blocks.CAVE_VINES );
                builder.add( Blocks.SCULK_CATALYST );
                builder.add( Blocks.MEDIUM_AMETHYST_BUD ).add( Blocks.LARGE_AMETHYST_BUD ).add( Blocks.AMETHYST_CLUSTER );
                builder.add( Blocks.REDSTONE_ORE );
                builder.add( Blocks.DEEPSLATE_REDSTONE_ORE );
            }
            else if( isNetherDimension() ) {
                builder.add( Blocks.GLOWSTONE );
                builder.add( Blocks.SHROOMLIGHT );
                builder.add( Blocks.SOUL_FIRE );
            }
            // Shared between overworld and nether
            if( isOverworldDimension() || isNetherDimension() ) {
                builder.add( Blocks.FIRE );
                builder.add( Blocks.MAGMA_BLOCK );
            }
            else if( isEndDimension() ) {
                builder.add( Blocks.END_ROD );
            }
            return builder.build();
        }
    }
}
