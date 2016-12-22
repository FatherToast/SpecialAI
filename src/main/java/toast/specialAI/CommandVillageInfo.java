package toast.specialAI;

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

public class CommandVillageInfo extends CommandBase
{
    @Override
    public String getCommandName() {
        return "villageinfo"; // The string used to run this command
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return this.langKey("usage");
    }

    @Override
	public int getRequiredPermissionLevel() {
        return 0;
    }

    public String langKey() {
    	return "commands.sai.villageinfo";
    }
    public String langKey(String tag) {
    	return this.langKey() + "." + tag;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
    	EntityPlayerMP player = CommandBase.getCommandSenderAsPlayer(sender);
    	Village village = player.worldObj.getVillageCollection() == null ? null : player.worldObj.getVillageCollection().getNearestVillage(new BlockPos(player), 0);
    	if (village == null)
    		CommandBase.notifyCommandListener(sender, this, this.langKey("failed"), new Object[] { });
    	else {
    		BlockPos center = village.getCenter();

    		int radius = village.getVillageRadius();
    		int houses = village.getNumVillageDoors();

    		int pop = village.getNumVillagers();
    		int maxPop = (int) (houses * 0.35);

            int golems = player.worldObj.<EntityIronGolem>getEntitiesWithinAABB(EntityIronGolem.class, new AxisAlignedBB(
            		center.getX() - radius, center.getY() - 4, center.getZ() - radius,
            		center.getX() + radius, center.getY() + 4, center.getZ() + radius
        		)).size();
    		int maxGolems = houses > 20 ? pop / 10 : 0;

    		int rep = village.getPlayerReputation(player.getName());
    		TextFormatting color;
    		String standing;
    		if (rep <= -15) {
    			color = TextFormatting.RED;
    			standing = "hated";
    		}
    		else if (rep <= Properties.get().VILLAGES.BLOCK_ATTACK_LIMIT) {
    			color = TextFormatting.YELLOW;
    			standing = "disliked";
    		}
    		else if (rep <= Properties.get().VILLAGES.BLOCK_REP_LIMIT) {
    			color = TextFormatting.WHITE;
    			standing = "neutral";
    		}
    		else {
    			color = TextFormatting.GREEN;
    			standing = "trusted";
    		}

    		CommandBase.notifyCommandListener(sender, this, this.langKey("pos"), new Object[] { center.getX(), center.getY(), center.getZ() });
    		CommandBase.notifyCommandListener(sender, this, this.langKey("size"), new Object[] { houses, radius });
    		CommandBase.notifyCommandListener(sender, this, this.langKey("pop"), new Object[] { pop, maxPop, golems, maxGolems });
    		CommandBase.notifyCommandListener(sender, this, this.langKey("rep"), new Object[] { rep,
    			color.toString()
    			+ new TextComponentTranslation(this.langKey("rep." + standing), new Object[] { }).getUnformattedText()
    			+ TextFormatting.RESET.toString()
			});
    	}
    }
}
