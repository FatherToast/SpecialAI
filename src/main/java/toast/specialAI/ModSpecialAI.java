package toast.specialAI;

import java.io.File;
import java.util.Random;

import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import toast.specialAI.ai.AIHandler;
import toast.specialAI.village.ReputationHandler;

@Mod(modid = ModSpecialAI.MODID, name = "Special AI", version = ModSpecialAI.VERSION, acceptableRemoteVersions = "*")
public class ModSpecialAI
{
    // This mod's id.
    public static final String MODID = "special_ai";
    // This mod's version.
    public static final String VERSION = "1.1.3";

    // If true, this mod starts up in debug mode.
    public static final boolean debug = true;
    // The mod's random number generator.
    public static final Random random = new Random();

    // This mod's NBT tag.
    public static final String BASE_TAG = "sAI";

    // The config directory.
    public static File configFile;

    // Called before initialization. Loads the properties/configurations.
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	ModSpecialAI.configFile = event.getSuggestedConfigurationFile();
    }

    // Called after initialization.
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Properties.load(new Configuration(ModSpecialAI.configFile));
        ModSpecialAI.logDebug("Loading in debug mode!");

        new AIHandler();
        new ReputationHandler();
    }

    // Returns this mod's compound tag for the entity.
    public static NBTTagCompound getTag(Entity entity) {
        NBTTagCompound data = entity.getEntityData();
        if (!data.hasKey(ModSpecialAI.BASE_TAG)) {
            data.setTag(ModSpecialAI.BASE_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(ModSpecialAI.BASE_TAG);
    }

    // Called as the server is starting.
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ServerCommandManager commandManager = (ServerCommandManager) event.getServer().getCommandManager();
        commandManager.registerCommand(new CommandVillageInfo());
    }

	public static boolean debug() {
		return Properties.get().GENERAL.DEBUG;
	}

	// Prints the message to the console with this mod's name tag if debugging is enabled.
	public static void logDebug(String message) {
		if (ModSpecialAI.debug()) ModSpecialAI.log("(debug) " + message);
	}

	// Prints the message to the console with this mod's name tag.
	public static void log(String message) {
		System.out.println("[" + ModSpecialAI.MODID + "] " + message);
	}

	// Prints the message to the console with this mod's name tag if debugging is enabled.
	public static void logWarning(String message) {
		ModSpecialAI.log("[WARNING] " + message);
	}

	// Prints the message to the console with this mod's name tag if debugging is enabled.
	public static void logError(String message) {
		if (ModSpecialAI.debug())
			throw new RuntimeException("[" + ModSpecialAI.MODID + "] [ERROR] " + message);
		ModSpecialAI.log("[ERROR] " + message);
	}

	// Throws a runtime exception with a message and this mod's name tag.
	public static void exception(String message) {
		throw new RuntimeException("[" + ModSpecialAI.MODID + "] [FATAL ERROR] " + message);
	}
}