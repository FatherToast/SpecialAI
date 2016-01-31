package toast.specialAI;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Configuration;
import toast.specialAI.ai.AIHandler;
import toast.specialAI.ai.SearchHandler;
import toast.specialAI.village.ReputationHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = _SpecialAI.MODID, name = "Special AI", version = _SpecialAI.VERSION, acceptableRemoteVersions = "*")
public class _SpecialAI {
    /* TO DO *\
    >> CURRENT
        * "pack" ai
    \* ** ** */

    // This mod's id.
    public static final String MODID = "SpecialAI";
    // This mod's version.
    public static final String VERSION = "1.1.1";

    // If true, this mod starts up in debug mode.
    public static final boolean debug = true;
    // The mod's random number generator.
    public static final Random random = new Random();

    // This mod's NBT tag.
    public static final String BASE_TAG = "sAI";

    // Called before initialization. Loads the properties/configurations.
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        _SpecialAI.debugConsole("Loading in debug mode!");
        Properties.init(new Configuration(event.getSuggestedConfigurationFile()));
    }

    // Called during initialization. Registers entities, mob spawns, and renderers.
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        new AIHandler();
        new ReputationHandler();
        new SearchHandler();
    }

    // Returns this mod's compound tag for the entity.
    public static NBTTagCompound getTag(Entity entity) {
        NBTTagCompound data = entity.getEntityData();
        if (!data.hasKey(_SpecialAI.BASE_TAG)) {
            data.setTag(_SpecialAI.BASE_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(_SpecialAI.BASE_TAG);
    }

    // Prints the message to the console with this mod's name tag.
    public static void console(String message) {
        System.out.println("[SpecialAI] " + message);
    }

    // Prints the message to the console with this mod's name tag if debugging is enabled.
    public static void debugConsole(String message) {
        if (_SpecialAI.debug) {
            System.out.println("[SpecialAI] (debug) " + message);
        }
    }

    // Throws a runtime exception with a message and this mod's name tag if debugging is enabled. Otherwise, just prints a console error message.
    public static void debugException(String message) {
        if (_SpecialAI.debug)
            throw new RuntimeException("[SpecialAI] " + message);
        _SpecialAI.console("[ERROR] " + message);
    }
}