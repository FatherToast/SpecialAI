package fathertoast.specialai.config;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import fathertoast.specialai.ModCore;
import fathertoast.specialai.config.field.*;
import fathertoast.specialai.config.util.EntityList;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Contains all config options for this mod, and the parameters for loading them.
 */
@SuppressWarnings( "SameParameterValue" )
@Mod.EventBusSubscriber( modid = ModCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public class Config {
    /** The root folder for config files in this mod. */
    private static final String CONFIG_DIR = ModCore.MOD_ID + "-";//TODO Temporarily changed to put the config in the root
    /** The file extension used by config files. */
    private static final String FILE_EXT = ".toml";
    /** Used to split toml keys. */
    private static final Splitter DOT_SPLITTER = Splitter.on( "." );
    
    /** The lang key prefix used for configs. */
    private static final String LANG_KEY = "config." + ModCore.LANG_KEY;
    
    /** A list of all configs that have been built. */
    private static final List<AbstractConfig> MOD_CONFIGS = new ArrayList<>();
    
    public static final MainConfig MAIN = buildConfig( MainConfig::new );
    
    /**
     * Initializes all configuration files for this mod and creates their directories.
     */
    public static void initialize() {
        // We must make directories before registering any config files because Forge has not yet implemented any config file structure
        //File configDir = new File( FMLPaths.CONFIGDIR.get().toFile(), CONFIG_DIR );
        //if( !configDir.mkdirs() ) {
        //    ModCore.LOG.error( "Failed to create mod config directory! The game will probably crash now; you can make " +
        //            "the directory manually to resolve this issue ({}).", configDir.toString() );
        //}
        
        // Register configs with Forge
        registerConfig( MAIN );
    }
    
    /** Registers a config in the root directory. */
    private static void registerConfig( AbstractConfig config ) {
        registerConfig( config, "" );
    }
    
    /** Registers a config in a sub-directory. */
    private static void registerConfig( AbstractConfig config, String subDir ) {
        config.PATH = CONFIG_DIR + subDir + toFileName( config.NAME ) + FILE_EXT;
        ModLoadingContext.get().registerConfig( ModConfig.Type.COMMON, config.SPEC, config.PATH );
    }
    
    /** @return Builds the config and its spec, links the spec to its config, and then returns the constructed config. */
    private static <T extends AbstractConfig> T buildConfig( Function<ForgeConfigSpec.Builder, T> consumer ) {
        // Build the config and config spec
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        final T config = consumer.apply( builder );
        config.SPEC = builder.build();
        
        // Register the config with this mod
        MOD_CONFIGS.add( config );
        return config;
    }
    
    /** @return The config from this mod that uses a given config spec, or null if there is none (spec is for a different mod). */
    private static AbstractConfig getConfigFromSpec( ForgeConfigSpec spec ) {
        // Check if the spec matches the spec used by any of this mod's configs
        for( AbstractConfig config : MOD_CONFIGS ) {
            if( spec == config.SPEC ) { return config; }
        }
        return null;
    }
    
    /**
     * A base class used to streamline the config design process.
     */
    static abstract class AbstractConfig {
        /** The config spec for this file. */
        ForgeConfigSpec SPEC;
        /** The file path relative to the root config folder. Saved to provide more helpful user feedback. */
        String PATH;
        
        /** The name of this config in a nice, readable format. */
        final String NAME;
        /** The lang key prefix used for this file. */
        final String LANG_KEY;
        
        /** A list of all fields included in this file. Used to update their values when loading or reloading this config. */
        final List<IConfigField> REGISTERED_FIELDS = new ArrayList<>();
        
        AbstractConfig( String name ) {
            NAME = name;
            LANG_KEY = Config.LANG_KEY + Config.toLangKey( name );
        }
        
        /** @return Registers the field with this config and then returns the same field, for convenience. */
        <T extends IConfigField> T registerField( T field ) {
            REGISTERED_FIELDS.add( field );
            return field;
        }
        
        /** Called when the config is loaded or reloaded to update the underlying return value of each contained field. */
        void resolve() {
            for( IConfigField field : REGISTERED_FIELDS ) {
                field.resolve();
            }
        }
    }
    
    /**
     * A base class used to make nested categories within config files.
     */
    @SuppressWarnings( "unused" )
    static abstract class AbstractCategory {
        private static final String HEADER_BAR = "###################################################################################################";
        
        /** The config file this category resides in. */
        protected final AbstractConfig PARENT;
        /** The name of this category in a nice, readable format. */
        protected final String NAME;
        
        /** The lang key prefix used for this category. */
        private final String LANG_KEY;
        
        /** The opening comment to add at the beginning of the first field comment. Set to null once applied. */
        private String[] openingComment;
        /** Number of additional line breaks to add at the beginning of the next field comment. */
        private int lineBreaks;
        
        AbstractCategory( ForgeConfigSpec.Builder builder, AbstractConfig parent, String name, String... comment ) {
            PARENT = parent;
            NAME = name;
            LANG_KEY = parent.LANG_KEY + "." + Config.toLangKey( name );
            
            openingComment = comment;
        }
        
        /** Add a new line before the next field comment. */
        protected void lineBreak() { lineBreak( 1 );}
        
        /** Add a specified number of new lines before the next field comment. */
        protected void lineBreak( int nl ) {lineBreaks = nl; }
        
        /**
         * Adds additional information or formatting to comments, as needed.
         *
         * @param comment The raw field comment array. Each element is a new line.
         * @return The final comment to send.
         */
        private String[] processComment( String[] comment ) {
            if( openingComment != null ) {
                // Add the opening category comment before the start of this one
                final ArrayList<String> newComment = new ArrayList<>();
                newComment.add( "\n\n" + HEADER_BAR );
                newComment.addAll( Arrays.asList( openingComment ) );
                newComment.add( "[" + NAME + "]" );
                newComment.add( HEADER_BAR + "\n" );
                newComment.addAll( Arrays.asList( comment ) );
                comment = newComment.toArray( comment );
                openingComment = null;
            }
            else if( lineBreaks > 0 ) {
                // Add the requested number of line breaks before the start of this comment
                StringBuilder prefix = new StringBuilder();
                for( ; lineBreaks > 0; lineBreaks-- ) prefix.append( '\n' );
                comment[0] = prefix + comment[0];
            }
            return comment;
        }
        
        // Booleans
        
        /** Defines a boolean field. */
        protected final BooleanField entry( ForgeConfigSpec.Builder builder, String name, boolean defaultValue, String... comment ) {
            return PARENT.registerField( new BooleanField(
                    builder.comment( processComment( comment ) ).translation( LANG_KEY + name )
                            .define( name, defaultValue ) )
            );
        }
        
        // Integers
        
        /** Defines an integer field that accepts any non-negative value (>= 0). */
        protected final IntField entryNoNeg( ForgeConfigSpec.Builder builder, String name, int defaultValue, String... comment ) {
            return entry( builder, name, defaultValue, 0, Integer.MAX_VALUE, comment );
        }
        
        /** Defines an integer field that accepts any positive value (> 0). */
        protected final IntField entryPos( ForgeConfigSpec.Builder builder, String name, int defaultValue, String... comment ) {
            return entry( builder, name, defaultValue, 1, Integer.MAX_VALUE, comment );
        }
        
        /** Defines an integer field that accepts -1 and any non-negative value (>= -1). */
        protected final IntField entryTokenNeg( ForgeConfigSpec.Builder builder, String name, int defaultValue, String... comment ) {
            return entry( builder, name, defaultValue, -1, Integer.MAX_VALUE, comment );
        }
        
        /** Defines an integer field that accepts any integer value. */
        protected final IntField entryAny( ForgeConfigSpec.Builder builder, String name, int defaultValue, String... comment ) {
            return entry( builder, name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE, comment );
        }
        
        /** Defines an integer field. */
        protected final IntField entry( ForgeConfigSpec.Builder builder, String name, int defaultValue, int min, int max, String... comment ) {
            return PARENT.registerField( new IntField(
                    builder.comment( processComment( comment ) ).translation( LANG_KEY + name )
                            .defineInRange( name, defaultValue, min, max ) )
            );
        }
        
        // Doubles
        
        /** Defines a double field that accepts a value between 0 and 1, inclusive. */
        protected final DoubleField entry0to1( ForgeConfigSpec.Builder builder, String name, double defaultValue, String... comment ) {
            return entry( builder, name, defaultValue, 0.0, 1.0, comment );
        }
        
        /** Defines a double field that accepts any positive value (including +0). */
        protected final DoubleField entryPos( ForgeConfigSpec.Builder builder, String name, double defaultValue, double max, String... comment ) {
            return entry( builder, name, defaultValue, 0.0, Double.POSITIVE_INFINITY, comment );
        }
        
        /** Defines a double field that accepts any numerical value. */
        protected final DoubleField entryAny( ForgeConfigSpec.Builder builder, String name, double defaultValue, String... comment ) {
            return entry( builder, name, defaultValue, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, comment );
        }
        
        /** Defines a double field. */
        protected final DoubleField entry( ForgeConfigSpec.Builder builder, String name, double defaultValue, double min, double max, String... comment ) {
            return PARENT.registerField( new DoubleField(
                    builder.comment( processComment( comment ) ).translation( LANG_KEY + name )
                            .defineInRange( name, defaultValue, min, max ) )
            );
        }
        
        // Enums
        
        /** Defines an enum field. */
        protected final <T extends Enum<T>> EnumField<T> entry( ForgeConfigSpec.Builder builder, String name, T defaultValue, String... comment ) {
            return PARENT.registerField( new EnumField<>(
                    builder.comment( processComment( comment ) ).translation( LANG_KEY + name )
                            .defineEnum( name, defaultValue ) )
            );
        }
        
        // Specialized
        
        /** Defines an entity list field. */
        protected final EntityListField entry( ForgeConfigSpec.Builder builder, String name, EntityList defaultValue, String... comment ) {
            return PARENT.registerField( new EntityListField(
                    builder.comment( processComment( comment ) ).translation( LANG_KEY + name )
                            .defineListAllowEmpty( split( name ), defaultValue::toStringList, defaultValue::validateEntry ) )
            );
        }
    }
    
    /**
     * Called for any mod configuration event that is fired. Right now, that is only "Loading" and "Reloading".
     * <p>
     * Loading is called after all registry events and before mod setup events.
     * <p>
     * Reloading is theoretically called whenever a config file is modified.
     *
     * @param event The event data.
     */
    @SuppressWarnings( "unused" )
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onModConfigEvent( final ModConfig.ModConfigEvent event ) {
        // Determine which config this event is for
        final AbstractConfig config = getConfigFromSpec( event.getConfig().getSpec() );
        if( config == null ) return; // We are only interested in our own mod's configs
        
        // Determine event type
        final String type = event instanceof ModConfig.Loading ? "Loading" :
                event instanceof ModConfig.Reloading ? "Reloading" : "Unknown";
        
        // Notify the user and resolve all fields in the config
        ModCore.LOG.info( "Detected config event \"{}\" for file {}. Updating runtime values.", type, config.PATH );
        config.resolve();
    }
    
    /** @return The string, converted to a filename-friendly version (by replacing spaces with underscores and converting to lowercase). */
    private static String toFileName( String str ) {
        return str.replaceAll( " ", "_" ).toLowerCase();
    }
    
    /** @return The string, converted to an i18n-friendly version (by deleting spaces and converting to lowercase). */
    private static String toLangKey( String str ) {
        return str.replaceAll( " ", "" ).toLowerCase();
    }
    
    /** @return The toml path split by dots. */
    private static List<String> split( String path ) { return Lists.newArrayList( DOT_SPLITTER.split( path ) ); }
}