package fathertoast.specialai;

import fathertoast.specialai.config.Config;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Contains and automatically registers all needed mod events.
 * Each event passes itself off to interested sub-mods.
 */
@SuppressWarnings( "unused" )
@Mod.EventBusSubscriber( modid = ModCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ModEventHandler {
    /**
     * Called after registry events, but before the client- and server-specific setup events.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void setup( final FMLCommonSetupEvent event ) {
        Config.initialize();
    }
}