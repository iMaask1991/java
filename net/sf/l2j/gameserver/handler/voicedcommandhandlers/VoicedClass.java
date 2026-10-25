package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.ClassRewardData.RewardItem;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.ClassRewardData.RewardSet;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import Dolls.DollsData;

/**
 * Comando .class : cambio de clase por comando + ventana de premios (portado del server ruso).
 * <ul>
 * <li>.class : muestra las clases disponibles (o los premios pendientes).</li>
 * <li>.class_change ID : cambia a esa clase (lo usan los botones del HTML).</li>
 * <li>.class_reward ID : entrega el set elegido (lo usan los botones del HTML).</li>
 * <li>.class_reload : recarga ClassReward.properties y ClassRewards.xml (solo GM).</li>
 * </ul>
 * Precios y premios fijos: los mismos que el NPC Class Master (ConfigClassMaster en npcs.properties).
 */
public class VoicedClass implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"class",
		"class_change",
		"class_reward",
		"class_reload"
	};

	private static final String HTML_PATH = "data/html/mods/classmaster/";

	/** Memo del player: nivel de clase con premio pendiente (0 = nada). Persiste si hace logout. */
	private static final String MEMO_PENDING = "class_reward_pending";

	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (player == null)
			return false;

		final ClassRewardData data = ClassRewardData.getInstance();

		if (command.equals("class_reload"))
		{
			if (!player.isGM())
				return false;

			data.load();
			player.sendMessage("ClassReward: config y premios recargados.");
			return true;
		}

		if (!data.isCommandEnabled())
		{
			player.sendMessage("El comando .class esta desactivado.");
			return true;
		}

		if (command.equals("class"))
		{
			// Si tiene un premio sin elegir, se lo mostramos de nuevo.
			final int pending = player.getMemos().getInteger(MEMO_PENDING, 0);
			if (pending > 0 && data.hasRewardFor(pending))
				showRewardWindow(player, pending);
			else
				showClassList(player);
		}
		else if (command.startsWith("class_change"))
		{
			final int classId = parseArg(command);
			if (classId >= 0)
				changeClass(player, classId);
		}
		else if (command.startsWith("class_reward"))
		{
			final int setId = parseArg(command);
			if (setId >= 0)
				giveReward(player, setId);
		}
		return true;
	}

	private static int parseArg(String command)
	{
		final int space = command.indexOf(' ');
		if (space < 0)
			return -1;

		try
		{
			return Integer.parseInt(command.substring(space + 1).trim());
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/**
	 * Chequeos comunes antes de mostrar la lista o de cambiar de clase.
	 * @return el nivel de la proxima clase (1, 2 o 3), o -1 si no puede.
	 */
	private static int checkConditions(Player player)
	{
		if (player.isAio())
		{
			player.sendMessage("Los AIO no pueden cambiar de clase.");
			return -1;
		}

		if (player.isDead() || player.isInOlympiadMode())
		{
			player.sendMessage("No podes cambiar de clase en este momento.");
			return -1;
		}

		if (!Config.ALLOW_CLASS_MASTERS || Config.CLASS_MASTER_SETTINGS == null)
		{
			player.sendMessage("El cambio de clase esta desactivado.");
			return -1;
		}

		final ClassId current = player.getClassId();
		final int nextLevel = current.level() + 1;
		if (nextLevel > 3)
		{
			player.sendMessage("Ya tenes todas las clases.");
			return -1;
		}

		if (!Config.CLASS_MASTER_SETTINGS.isAllowed(nextLevel))
		{
			player.sendMessage("Ese cambio de clase no esta disponible en este server.");
			return -1;
		}

		final int minLevel = getMinLevel(current.level());
		if (player.getLevel() < minLevel && !Config.ALLOW_ENTIRE_TREE)
		{
			player.sendMessage("Volve cuando seas nivel " + minLevel + ".");
			return -1;
		}

		return nextLevel;
	}

	private static void showClassList(Player player)
	{
		final int nextLevel = checkConditions(player);
		if (nextLevel < 0)
			return;

		final ClassId current = player.getClassId();
		final StringBuilder sb = new StringBuilder(500);
		for (ClassId child : ClassId.VALUES)
		{
			if (child.level() != nextLevel || !validateClassId(current, child))
				continue;

			sb.append("<button value=\"").append(PlayerData.getInstance().getClassNameById(child.getId()));
			sb.append("\" action=\"bypass -h voiced_class_change ").append(child.getId());
			sb.append("\" width=123 height=26 back=L2UI_NOVA.btn_e_down fore=L2UI_NOVA.btn_e><br1>");
		}

		if (sb.length() == 0)
		{
			player.sendMessage("No hay clases disponibles para vos.");
			return;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + "class.htm");
		html.replace("%classname%", PlayerData.getInstance().getClassNameById(current.getId()));
		html.replace("%menu%", sb.toString());
		html.replace("%req_items%", getItemsText(Config.CLASS_MASTER_SETTINGS.getRequiredItems(nextLevel), "Gratis."));
		html.replace("%reward_items%", getItemsText(Config.CLASS_MASTER_SETTINGS.getRewardItems(nextLevel), "-"));
		html.replace("%reward_sets%", ClassRewardData.getInstance().hasRewardFor(nextLevel) ? "Al cambiar vas a poder elegir un set de regalo." : "");
		player.sendPacket(html);
	}

	private static void changeClass(Player player, int classId)
	{
		// Si todavia no eligio el premio anterior, primero eso.
		final int pending = player.getMemos().getInteger(MEMO_PENDING, 0);
		if (pending > 0 && ClassRewardData.getInstance().hasRewardFor(pending))
		{
			player.sendMessage("Primero elegi tu premio del cambio de clase anterior.");
			showRewardWindow(player, pending);
			return;
		}

		final int nextLevel = checkConditions(player);
		if (nextLevel < 0)
			return;

		final ClassId current = player.getClassId();
		if (classId >= ClassId.VALUES.length || ClassId.VALUES[classId].level() != nextLevel || !validateClassId(current, ClassId.VALUES[classId]))
			return;

		// Mismos precios y premios fijos que el NPC Class Master.
		if (!Config.CLASS_MASTER_SETTINGS.getRewardItems(nextLevel).isEmpty() && player.getWeightPenalty() > 2)
		{
			player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return;
		}

		final List<IntIntHolder> required = Config.CLASS_MASTER_SETTINGS.getRequiredItems(nextLevel);
		for (IntIntHolder holder : required)
		{
			if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getValue())
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
		}

		for (IntIntHolder holder : required)
		{
			if (!player.destroyItemByItemId("ClassCommand", holder.getId(), holder.getValue(), player, true))
				return;
		}

		for (IntIntHolder holder : Config.CLASS_MASTER_SETTINGS.getRewardItems(nextLevel))
			player.addItem("ClassCommand", holder.getId(), holder.getValue(), player, true);

		// Cambio de clase (igual que el NPC). setClassId ya manda el mensaje y da las skills.
		player.setClassId(classId);
		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());

		DollsData.refreshAllDollSkills(player);
		player.sendPacket(new HennaInfo(player));
		player.broadcastUserInfo();

		// Premio para elegir.
		if (ClassRewardData.getInstance().hasRewardFor(nextLevel))
		{
			player.getMemos().set(MEMO_PENDING, nextLevel);
			showRewardWindow(player, nextLevel);
		}
	}

	private static void showRewardWindow(Player player, int level)
	{
		final StringBuilder sb = new StringBuilder(1000);
		for (RewardSet set : ClassRewardData.getInstance().getSets(level))
		{
			sb.append("<img src=\"L2UI.SquareGray\" width=280 height=1><table width=280><tr>");

			if (!set.getIcon().isEmpty())
			{
				sb.append("<td width=40><button action=\"bypass -h voiced_class_reward ").append(set.getId());
				sb.append("\" width=32 height=32 back=\"").append(set.getIcon()).append("\" fore=\"").append(set.getIcon()).append("\"></td>");
			}
			else
			{
				sb.append("<td width=90><button value=\"Elegir\" action=\"bypass -h voiced_class_reward ").append(set.getId());
				sb.append("\" width=80 height=22 back=L2UI_NOVA.btn_e_down fore=L2UI_NOVA.btn_e></td>");
			}

			sb.append("<td><font color=\"LEVEL\">").append(set.getName()).append("</font><br1>");
			for (RewardItem ri : set.getItems())
			{
				final Item template = ItemTable.getInstance().getTemplate(ri.getId());
				sb.append("<font color=\"B09878\">");
				if (ri.getEnchant() > 0)
					sb.append("+").append(ri.getEnchant()).append(" ");
				sb.append((template != null) ? template.getName() : ("Item " + ri.getId()));
				if (ri.getCount() > 1)
					sb.append(" x").append(ri.getCount());
				sb.append("</font><br1>");
			}
			sb.append("</td></tr></table>");
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + "reward.htm");
		html.replace("%classname%", PlayerData.getInstance().getClassNameById(player.getClassId().getId()));
		html.replace("%sets%", sb.toString());
		player.sendPacket(html);
	}

	private static void giveReward(Player player, int setId)
	{
		final int pending = player.getMemos().getInteger(MEMO_PENDING, 0);
		if (pending <= 0)
		{
			player.sendMessage("No tenes ningun premio pendiente.");
			return;
		}

		// Solo puede elegir un set del nivel que tiene pendiente (evita bypass trucho).
		final RewardSet set = ClassRewardData.getInstance().getSet(setId);
		if (set == null || set.getLevel() != pending)
			return;

		if (player.getWeightPenalty() > 2 || !player.getInventory().validateCapacity(set.getItems().size()))
		{
			player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return;
		}

		// Se borra ANTES de entregar: un doble click no puede dar dos sets.
		player.getMemos().remove(MEMO_PENDING);

		for (RewardItem ri : set.getItems())
		{
			final ItemInstance item = player.addItem("ClassReward", ri.getId(), ri.getCount(), player, true);
			if (item == null || !item.isEquipable())
				continue;

			if (ri.getEnchant() > 0)
				item.setEnchantLevel(ri.getEnchant());

			if (ri.isEquip() && !item.isEquipped())
				player.getInventory().equipItemAndRecord(item);
		}

		player.sendPacket(new ItemList(player, false));
		player.broadcastUserInfo();
		player.sendMessage("Recibiste: " + set.getName() + ".");
	}

	private static String getItemsText(List<IntIntHolder> items, String empty)
	{
		if (items == null || items.isEmpty())
			return empty;

		final StringBuilder sb = new StringBuilder();
		for (IntIntHolder holder : items)
		{
			final Item template = ItemTable.getInstance().getTemplate(holder.getId());
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(holder.getValue()).append(" ").append((template != null) ? template.getName() : ("Item " + holder.getId()));
		}
		return sb.toString();
	}

	private static int getMinLevel(int classLevel)
	{
		switch (classLevel)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
		}
		return Integer.MAX_VALUE;
	}

	private static boolean validateClassId(ClassId current, ClassId next)
	{
		if (next == null)
			return false;

		if (current == next.getParent())
			return true;

		return Config.ALLOW_ENTIRE_TREE && next.childOf(current);
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
