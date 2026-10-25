package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.Experience;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * NPC Custom Level (beta). Los valores se configuran en config/HadesProject/Subclass.properties:
 * CustomLevelTarget, CustomLevelAdena, CustomLevelCoinId, CustomLevelCoinCount.
 * En el html se puede usar %level% para mostrar el nivel configurado.
 */
public class CustomLevel extends Folk
{
	public CustomLevel(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		if (player == null)
			return;
		
		String html = HtmCache.getInstance().getHtm("data/html/mods/HadesProject/Subclass/Level.htm");
		if (val > 0)
			html = HtmCache.getInstance().getHtm("data/html/mods/HadesProject/Subclass/Level-" + val + ".htm");
		
		if (html != null)
		{
			final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
			msg.setHtml(html);
			msg.replace("%objectId%", String.valueOf(getObjectId()));
			msg.replace("%level%", String.valueOf(getTargetLevel()));
			player.sendPacket(msg);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private static int getTargetLevel()
	{
		final int max = Math.min(Config.MAX_PLAYER_LEVEL - 1, Experience.LEVEL.length - 1);
		return Math.max(1, Math.min(Config.CUSTOM_LEVEL_TARGET, max));
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("LevelUP"))
		{
			final long targetExp = Experience.LEVEL[getTargetLevel()];
			final long exp = player.getExp();
			if (exp > targetExp)
				player.removeExpAndSp(exp - targetExp, 0);
			else if (exp < targetExp)
				player.addExpAndSp(targetExp - exp, 0);
			
			player.sendMessage("Accion exitosa");
			return;
		}
		
		if (command.startsWith("FullAdena"))
		{
			if (Config.CUSTOM_LEVEL_ADENA > 0)
				player.addItem("Beta", 57, Config.CUSTOM_LEVEL_ADENA, player, true);
			if (Config.CUSTOM_LEVEL_COIN_ID > 0 && Config.CUSTOM_LEVEL_COIN_COUNT > 0)
				player.addItem("Beta", Config.CUSTOM_LEVEL_COIN_ID, Config.CUSTOM_LEVEL_COIN_COUNT, player, true);
			
			player.sendMessage("Accion exitosa");
			return;
		}
		
		super.onBypassFeedback(player, command);
	}
}
