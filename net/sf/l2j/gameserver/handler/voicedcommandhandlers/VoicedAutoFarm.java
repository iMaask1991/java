package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.autofarm.AutofarmConstants;
import net.sf.l2j.gameserver.autofarm.AutofarmPlayerRoutine;
import net.sf.l2j.gameserver.autofarm.AutofarmSkillSlot;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class VoicedAutoFarm implements IVoicedCommandHandler
{
	private final String[] VOICED_COMMANDS =
	{
		"farm",
		"autofarm", // interface ACDC
		"enableAutoFarm",
		"radiusAutoFarm",
		"farmSkillEdit",
		"farmSkillSet",
		"farmSkillReuse",
		"farmSkillUserHp",
		"farmSkillTargetHp",
		"farmSkillClear",
		"farmSkillPrio",
		"addAutoFarmTime"
	};
	
	public static ArrayList<Player> _playersFarm = new ArrayList<>();
	
	private static final String STOPPED = "START";
	private static final String STARTED = "STOP";
	
	@Override
	public boolean useVoicedCommand(final String command, final Player activeChar, final String args)
	{
		if (!Config.AUTOFARM_ENABLED)
		{
			activeChar.sendMessage("Auto Farm is disabled.");
			return false;
		}
		
		final AutofarmPlayerRoutine bot = activeChar.getBot();
		
		if (command.startsWith("farmSkillEdit"))
		{
			handleSkillEdit(activeChar, command);
			return true;
		}
		if (command.startsWith("farmSkillSet"))
		{
			handleSkillSet(activeChar, command);
			return true;
		}
		if (command.startsWith("farmSkillReuse"))
		{
			handleSkillDouble(activeChar, command, true, false, false);
			return true;
		}
		if (command.startsWith("farmSkillUserHp"))
		{
			handleSkillDouble(activeChar, command, false, true, false);
			return true;
		}
		if (command.startsWith("farmSkillTargetHp"))
		{
			handleSkillDouble(activeChar, command, false, false, true);
			return true;
		}
		if (command.startsWith("farmSkillClear"))
		{
			handleSkillClear(activeChar, command);
			return true;
		}
		if (command.startsWith("farmSkillPrio"))
		{
			handleSkillPrio(activeChar, command);
			return true;
		}
		
		if (command.equals("farm") || command.startsWith("farm ") || command.equals("autofarm"))
		{
			showAutoFarm(activeChar);
			return true;
		}
		
		if (command.startsWith("radiusAutoFarm"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			try
			{
				String param = st.nextToken();
				if (param.startsWith("inc_radius"))
					activeChar.setRadius(activeChar.getRadius() + 200);
				else if (param.startsWith("dec_radius"))
					activeChar.setRadius(activeChar.getRadius() - 200);
				activeChar.saveAutoFarmSettings();
				showAutoFarm(activeChar);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}
		
		if (command.startsWith("enableAutoFarm"))
		{
			if (activeChar.isAutoFarm())
				stopAutoFarm(activeChar, null);
			else
			{
				if (activeChar.isDead())
				{
					activeChar.sendMessage("No podes activar Auto Farm estando muerto.");
					return false;
				}
				if (activeChar.isInsideZone(ZoneId.PEACE))
				{
					activeChar.sendMessage("No podes activar Auto Farm en zona peace.");
					return false;
				}
				if (!canStartAutoFarm(activeChar))
					return false;
				
				if (Config.AUTOFARM_TIME_ENABLED && activeChar.getAutoFarmTotalLeft() <= 0)
				{
					activeChar.sendMessage("No te queda tiempo de Auto Farm. Usa un " + getAutoFarmTimeItemName() + " para sumar tiempo.");
					showAutoFarm(activeChar);
					return false;
				}
				
				bot.start();
				activeChar.setAutoFarm(true);
				activeChar.applyAutoFarmVisual();
				if (!_playersFarm.contains(activeChar))
					_playersFarm.add(activeChar);
			}
			showAutoFarm(activeChar);
			return true;
		}
		
		if (command.startsWith("addAutoFarmTime"))
		{
			if (!Config.AUTOFARM_TIME_ENABLED)
			{
				activeChar.sendMessage("El sistema de tiempo de Auto Farm esta desactivado.");
				showAutoFarm(activeChar);
				return false;
			}
			
			final int itemId = Config.AUTOFARM_TIME_ITEM_ID;
			final int itemCount = Config.AUTOFARM_TIME_ITEM_COUNT;
			final int addMinutes = Config.AUTOFARM_TIME_ITEM_MINUTES;
			if (itemId <= 0 || itemCount <= 0 || addMinutes <= 0)
			{
				activeChar.sendMessage("Auto Farm time item no esta configurado.");
				return false;
			}
			
			final String itemName = getAutoFarmTimeItemName();
			if (activeChar.getInventory().getInventoryItemCount(itemId, -1) < itemCount)
			{
				activeChar.sendMessage("Necesitas " + itemCount + "x " + itemName + " en el inventario.");
				showAutoFarm(activeChar);
				return false;
			}
			
			if (!activeChar.destroyItemByItemId("AutoFarmTime", itemId, itemCount, activeChar, true))
			{
				showAutoFarm(activeChar);
				return false;
			}
			
			activeChar.addAutoFarmBonusSeconds(addMinutes * 60);
			activeChar.sendMessage("Usaste " + itemCount + "x " + itemName + ". +" + addMinutes + " min de Auto Farm (total: " + Player.formatAutoFarmTime(activeChar.getAutoFarmTotalLeft()) + ").");
			activeChar.sendPacket(new ExShowScreenMessage("Auto Farm +" + addMinutes + " min", 4 * 1000, SMPOS.TOP_CENTER, false));
			showAutoFarm(activeChar);
			return true;
		}
		
		return false;
	}
	
	public static void showAutoFarm(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/mods/menu/AutoFarm.htm");
		html.replace("%player%", activeChar.getName());
		html.replace("%radius%", StringUtil.formatNumber(activeChar.getRadius()));
		html.replace("%button%", activeChar.isAutoFarm() ? STARTED : STOPPED);
		html.replace("%slots%", buildSlotIcons(activeChar));
		if (Config.AUTOFARM_TIME_ENABLED)
		{
			html.replace("%time%", Player.formatAutoFarmTime(activeChar.getAutoFarmTotalLeft()));
			html.replace("%nextReset%", activeChar.getAutoFarmNextResetLabel());
			html.replace("%itemMinutes%", String.valueOf(Config.AUTOFARM_TIME_ITEM_MINUTES));
			html.replace("%itemCount%", String.valueOf(Config.AUTOFARM_TIME_ITEM_COUNT));
			html.replace("%itemName%", getAutoFarmTimeItemName());
		}
		else
		{
			html.replace("%time%", "Ilimitado");
			html.replace("%nextReset%", "-");
			html.replace("%itemMinutes%", "0");
			html.replace("%itemCount%", "0");
			html.replace("%itemName%", getAutoFarmTimeItemName());
		}
		activeChar.sendPacket(html);
	}
	
	private static String buildSlotIcons(Player player)
	{
		final StringBuilder sb = new StringBuilder(512);
		final AutofarmSkillSlot[] slots = player.getAutoFarmSkillSlots();
		for (int i = 0; i < slots.length; i++)
		{
			final AutofarmSkillSlot slot = slots[i];
			final String icon = slot.isEmpty() ? "L2UI.SquareGray" : AutofarmSkillSlot.iconOf(slot.getSkillId());
			sb.append("<td align=center width=36>");
			sb.append("<button action=\"bypass voiced_farmSkillEdit ").append(i).append(" 0\" width=32 height=32 back=").append(icon).append(" fore=").append(icon).append(">");
			sb.append("</td>");
		}
		return sb.toString();
	}
	
	private static void handleSkillEdit(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		if (!st.hasMoreTokens())
			return;
		final int slot = Integer.parseInt(st.nextToken());
		final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
		showSkillEdit(player, slot, Math.max(0, page));
	}
	
	private static void handleSkillSet(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		if (st.countTokens() < 2)
			return;
		final int slot = Integer.parseInt(st.nextToken());
		final int skillId = Integer.parseInt(st.nextToken());
		final AutofarmSkillSlot s = player.getAutoFarmSkillSlot(slot);
		if (s == null)
			return;
		final L2Skill skill = player.getSkill(skillId);
		if (skill == null || !skill.isActive() || skill.isToggle() || skill.isPassive())
		{
			player.sendMessage("Skill invalido para Auto Farm.");
			showSkillEdit(player, slot, 0);
			return;
		}
		s.setSkillId(skillId);
		player.saveAutoFarmSettings();
		showSkillEdit(player, slot, 0);
	}
	
	private static void handleSkillClear(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		if (!st.hasMoreTokens())
			return;
		final int slot = Integer.parseInt(st.nextToken());
		final AutofarmSkillSlot s = player.getAutoFarmSkillSlot(slot);
		if (s != null)
			s.clear();
		player.saveAutoFarmSettings();
		showAutoFarm(player);
	}
	
	private static void handleSkillPrio(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		if (st.countTokens() < 2)
			return;
		final int from = Integer.parseInt(st.nextToken());
		final String dest = st.nextToken();
		if ("Remove".equalsIgnoreCase(dest))
		{
			final AutofarmSkillSlot s = player.getAutoFarmSkillSlot(from);
			if (s != null)
				s.clear();
			player.saveAutoFarmSettings();
			showAutoFarm(player);
			return;
		}
		final int to = Integer.parseInt(dest) - 1;
		final AutofarmSkillSlot a = player.getAutoFarmSkillSlot(from);
		final AutofarmSkillSlot b = player.getAutoFarmSkillSlot(to);
		if (a == null || b == null || from == to)
		{
			showSkillEdit(player, from, 0);
			return;
		}
		final String tmp = a.serialize();
		a.deserialize(b.serialize());
		b.deserialize(tmp);
		player.saveAutoFarmSettings();
		showSkillEdit(player, to, 0);
	}
	
	private static void handleSkillDouble(Player player, String command, boolean reuse, boolean userHp, boolean targetHp)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		if (st.countTokens() < 2)
			return;
		final int slot = Integer.parseInt(st.nextToken());
		final double value = Double.parseDouble(st.nextToken());
		final AutofarmSkillSlot s = player.getAutoFarmSkillSlot(slot);
		if (s == null)
			return;
		if (reuse)
			s.setReuseSec(value);
		else if (userHp)
			s.setUserHp(value);
		else if (targetHp)
			s.setTargetHp(value);
		player.saveAutoFarmSettings();
		showSkillEdit(player, slot, 0);
	}
	
	public static void showSkillEdit(Player player, int slot, int page)
	{
		final AutofarmSkillSlot conf = player.getAutoFarmSkillSlot(slot);
		if (conf == null)
			return;
		
		final List<L2Skill> skills = new ArrayList<>();
		for (L2Skill skill : player.getSkills().values())
		{
			if (skill != null && skill.isActive() && !skill.isToggle() && !skill.isPassive())
				skills.add(skill);
		}
		skills.sort(Comparator.comparingInt(L2Skill::getId));
		
		final int perPage = AutofarmConstants.SKILLS_PER_PAGE;
		final int pages = Math.max(1, (skills.size() + perPage - 1) / perPage);
		if (page >= pages)
			page = pages - 1;
		
		final StringBuilder ask = new StringBuilder(1024);
		ask.append("<tr>");
		final int start = page * perPage;
		for (int i = 0; i < perPage; i++)
		{
			final int idx = start + i;
			if (idx >= skills.size())
			{
				ask.append("<td width=36></td>");
				continue;
			}
			final L2Skill skill = skills.get(idx);
			final String icon = AutofarmSkillSlot.iconOf(skill.getId());
			ask.append("<td align=center width=36>");
			ask.append("<button action=\"bypass voiced_farmSkillSet ").append(slot).append(" ").append(skill.getId());
			ask.append("\" width=32 height=32 back=").append(icon).append(" fore=").append(icon).append(">");
			ask.append("</td>");
			if ((i + 1) % 3 == 0 && i + 1 < perPage)
				ask.append("</tr><tr>");
		}
		ask.append("</tr>");
		
		final StringBuilder pagesHtml = new StringBuilder(256);
		for (int p = 0; p < pages; p++)
		{
			if (p == page)
				pagesHtml.append("<td align=center><font color=LEVEL>").append(p + 1).append("</font></td>");
			else
				pagesHtml.append("<td align=center><a action=\"bypass voiced_farmSkillEdit ").append(slot).append(" ").append(p).append("\">").append(p + 1).append("</a></td>");
		}
		
		final StringBuilder prioList = new StringBuilder(64);
		for (int i = 1; i <= AutofarmConstants.SKILL_SLOTS; i++)
		{
			if (i > 1)
				prioList.append(";");
			prioList.append(i);
		}
		prioList.append(";Remove");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/mods/menu/AutoFarmSkill.htm");
		html.replace("%slot%", String.valueOf(slot));
		html.replace("%priority%", String.valueOf(slot + 1));
		html.replace("%ask%", ask.toString());
		html.replace("%pages%", pagesHtml.toString());
		html.replace("%pr%", prioList.toString());
		
		if (!conf.isEmpty())
		{
			final L2Skill selected = player.getSkill(conf.getSkillId());
			html.replace("%sic%", AutofarmSkillSlot.iconOf(conf.getSkillId()));
			html.replace("%sna%", selected != null ? selected.getName() : ("Skill " + conf.getSkillId()));
			html.replace("%reu%", String.format("%.1f", conf.getReuseSec()));
			html.replace("%hpp%", String.format("%.0f", conf.getUserHp()));
			html.replace("%tpp%", String.format("%.0f", conf.getTargetHp()));
		}
		else
		{
			html.replace("%sic%", "L2UI.SquareGray");
			html.replace("%sna%", "Empty");
			html.replace("%reu%", "0");
			html.replace("%hpp%", "100");
			html.replace("%tpp%", "100");
		}
		player.sendPacket(html);
	}
	
	private static String getAutoFarmTimeItemName()
	{
		final Item item = ItemTable.getInstance().getTemplate(Config.AUTOFARM_TIME_ITEM_ID);
		return item != null ? item.getName() : "Coin";
	}
	
	public static boolean canStartAutoFarm(Player activeChar)
	{
		final Player blocker = findAutoFarmBlocker(activeChar);
		if (blocker != null)
		{
			activeChar.sendMessage("Ya hay otro personaje farmeando desde esta computadora (" + blocker.getName() + ").");
			if (Config.DEBUG_FARM)
				activeChar.sendMessage("HWID: " + getSafeHwid(activeChar) + " | IP: " + getSafeIp(activeChar));
			return false;
		}
		return true;
	}
	
	public static Player findAutoFarmBlocker(Player activeChar)
	{
		if (activeChar == null || activeChar.getClient() == null)
			return null;
		
		final String hwid = getSafeHwid(activeChar);
		final String ip = getSafeIp(activeChar);
		
		for (Player p : World.getInstance().getPlayers())
		{
			if (p == null || p == activeChar || !p.isAutoFarm())
				continue;
			if (isSameFarmPc(hwid, ip, p))
				return p;
		}
		
		for (Player p : _playersFarm)
		{
			if (p == null || p == activeChar)
				continue;
			if (!p.isOnline() || p.getClient() == null)
				continue;
			if (isSameFarmPc(hwid, ip, p))
				return p;
		}
		return null;
	}
	
	public static boolean isSameFarmPc(String hwid, String ip, Player other)
	{
		final String otherHwid = getSafeHwid(other);
		if (hwid != null && otherHwid != null && hwid.equalsIgnoreCase(otherHwid))
			return true;
		final String otherIp = getSafeIp(other);
		return ip != null && otherIp != null && ip.equals(otherIp);
	}
	
	public static String getSafeHwid(Player player)
	{
		if (player == null || player.getClient() == null)
			return null;
		final String hwid = player.getClient().getHWID();
		if (hwid == null || hwid.isEmpty())
			return null;
		if (hwid.equalsIgnoreCase("NoGuard") || hwid.regionMatches(true, 0, "NoHWID", 0, 6))
			return null;
		return hwid;
	}
	
	public static String getSafeIp(Player player)
	{
		try
		{
			final GameClient client = player.getClient();
			if (client == null || client.getConnection() == null || client.getConnection().getInetAddress() == null)
				return null;
			return client.getConnection().getInetAddress().getHostAddress();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static void LimpiarListaFarm(Player activeChar)
	{
		_playersFarm.remove(activeChar);
	}
	
	public static void stopAutoFarm(Player player, String reason)
	{
		if (player == null)
			return;
		
		final boolean wasOn = player.isAutoFarm();
		player.getBot().stop(reason == null);
		player.setAutoFarm(false);
		if (wasOn)
			player.clearAutoFarmVisual();
		LimpiarListaFarm(player);
		player.saveAutoFarmSettings();
		
		if (!wasOn)
			return;
		
		if (reason != null && !reason.isEmpty())
		{
			player.sendMessage("Auto Farm desactivado: " + reason);
			player.sendPacket(new ExShowScreenMessage("Auto Farm OFF: " + reason, 4 * 1000, SMPOS.TOP_CENTER, false));
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
