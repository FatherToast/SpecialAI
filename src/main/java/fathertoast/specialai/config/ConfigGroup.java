package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.AbstractConfigFile;

import java.util.ArrayList;
import java.util.List;

// TODO - Copy-paste from DeadlyWorld; maybe make a useful group thingy in Crust?

/**
 * A collection used to group multiple related configs into one simple group.
 */
public abstract class ConfigGroup {
    /** A list of all the configs contained within this group. */
    private final List<AbstractConfigFile> GROUPED_CONFIGS = new ArrayList<>();
    
    /**
     * Convenience method. Used to add a config to this group and assign a field in a single line.
     *
     * @return The config that was just added.
     */
    protected <T extends AbstractConfigFile> T group( T config ) {
        GROUPED_CONFIGS.add( config );
        return config;
    }
    
    /** Loads the config group from disk. */
    public void initialize() {
        for( AbstractConfigFile config : GROUPED_CONFIGS ) { config.SPEC.initialize(); }
    }
}
