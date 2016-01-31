package toast.specialAI.ai;

import java.util.ArrayList;
import java.util.Iterator;

import toast.specialAI.Properties;
import toast.specialAI._SpecialAI;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class SearchHandler {
    // Useful properties for this class.
    private static final int MAX_SCAN = Math.max(1, Properties.getInt(Properties.GRIEFING, "grief_scan_cap"));
    private static final int SCAN_CAP = Math.max(1, Properties.getInt(Properties.GRIEFING, "grief_scan_max"));
    private static final boolean DEBUG_MESSGAE = Properties.getBoolean(Properties.GRIEFING, "grief_scan_cap_info");

    // The remaining number of blocks that can be checked. Refreshed each tick.
    private static final ArrayList<GriefSearch> scanners = new ArrayList<GriefSearch>();
    // The remaining number of blocks that can be checked. Refreshed each tick.
    public static int scansLeft;

    // Adds/removes the scanner from the list of things that need to be scanned.
    public static GriefSearch addScanner(GriefSearch scanner) {
        if (SearchHandler.scanners.size() < SearchHandler.SCAN_CAP) {
            SearchHandler.scanners.add(scanner);
            return scanner;
        }
        if (SearchHandler.DEBUG_MESSGAE) {
            _SpecialAI.console("Scan request rejected - cap reached!");
        }
        return null;
    }

    public static void removeScanner(GriefSearch scanner) {
        if (scanner != null) {
            SearchHandler.scanners.remove(scanner);
        }
    }

    // Counter to the next village update.
    private int updateCounter;

    public SearchHandler() {
        FMLCommonHandler.instance().bus().register(this);
    }

    /**
     * Called each tick.
     * TickEvent.Type type = the type of tick.
     * Side side = the side this tick is on.
     * TickEvent.Phase phase = the phase of this tick (START, END).
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Grief searching
            if (!SearchHandler.scanners.isEmpty()) {
                SearchHandler.scansLeft = SearchHandler.MAX_SCAN;
                for (Iterator<GriefSearch> iterator = SearchHandler.scanners.iterator(); iterator.hasNext() && SearchHandler.scansLeft > 0;) {
                    GriefSearch scanner = iterator.next();
                    if (scanner.isValid()) {
                        scanner.runSearch();
                        if (scanner.complete) {
                            scanner.clear();
                            iterator.remove();
                        }
                    }
                    else {
                        scanner.clear();
                        iterator.remove();
                    }
                }
            }
        }
    }
}