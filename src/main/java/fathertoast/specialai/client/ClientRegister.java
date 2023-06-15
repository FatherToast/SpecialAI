package fathertoast.specialai.client;

import fathertoast.crust.api.config.client.ClientConfigUtil;
import fathertoast.specialai.SpecialAI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber( value = Dist.CLIENT, modid = SpecialAI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public class ClientRegister {
    /** Called after common setup to perform client-side-only setup. */
    @SubscribeEvent
    static void onClientSetup( FMLClientSetupEvent event ) {
        // Tell Forge to open the config editor when our mod's "Config" button is clicked in the Mods screen
        ClientConfigUtil.registerConfigButtonAsEditScreen();
    }
}