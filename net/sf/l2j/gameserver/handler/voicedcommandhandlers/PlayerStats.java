package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.PveGearTier;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * .stats / .stat — retail NPC-style HTML window.
 * No target = own stats. Target player = that player's stats.
 */
public class PlayerStats implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"stats",
		"stat"
	};
	
	private static final String HTML_PATH = "data/html/mods/PlayerStats/main.html";
	
	private static final DecimalFormat INT_FMT;
	private static final DecimalFormat DEC_FMT;
	
	static
	{
		final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
		INT_FMT = new DecimalFormat("#,###", symbols);
		DEC_FMT = new DecimalFormat("0.0", symbols);
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		show(activeChar);
		return true;
	}
	
	public static void show(Player viewer)
	{
		if (viewer == null)
			return;
		
		Player viewed = viewer;
		final WorldObject target = viewer.getTarget();
		if (target instanceof Player && target != viewer)
			viewed = (Player) target;
		
		show(viewer, viewed);
	}
	
	public static void show(Player viewer, Player viewed)
	{
		if (viewer == null || viewed == null)
			return;
		
		String html = HtmCache.getInstance().getHtm(HTML_PATH);
		if (html == null)
		{
			viewer.sendMessage("Stats HTML missing: " + HTML_PATH);
			return;
		}
		
		html = fillPlaceholders(html, viewed);
		
		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(html);
		viewer.sendPacket(msg);
	}
	
	/** Shared by voiced .stats and Community Board stats page. */
	public static String fillPlaceholders(String html, Player player)
	{
		if (html == null || player == null)
			return html;
		
		return applyPlaceholders(html, player);
	}
	
	private static String applyPlaceholders(String html, Player player)
	{
		html = html.replace("%name%", player.getName());
		html = html.replace("%level%", String.valueOf(player.getLevel()));
		
		html = html.replace("%hp%", INT_FMT.format(player.getMaxHp()));
		html = html.replace("%mp%", INT_FMT.format(player.getMaxMp()));
		html = html.replace("%cp%", INT_FMT.format(player.getMaxCp()));
		
		html = html.replace("%str%", String.valueOf(player.getSTR()));
		html = html.replace("%dex%", String.valueOf(player.getDEX()));
		html = html.replace("%con%", String.valueOf(player.getCON()));
		html = html.replace("%int%", String.valueOf(player.getINT()));
		html = html.replace("%wit%", String.valueOf(player.getWIT()));
		html = html.replace("%men%", String.valueOf(player.getMEN()));
		
		html = html.replace("%patk%", INT_FMT.format(player.getPAtk(null)));
		html = html.replace("%matk%", INT_FMT.format(player.getMAtk(null, null)));
		html = html.replace("%pdef%", INT_FMT.format(player.getPDef(null)));
		html = html.replace("%mdef%", INT_FMT.format(player.getMDef(null, null)));
		
		html = html.replace("%accuracy%", String.valueOf(player.getAccuracy()));
		html = html.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
		html = html.replace("%speed%", INT_FMT.format((int) player.getStat().getMoveSpeed()));
		html = html.replace("%patkspd%", INT_FMT.format(player.getPAtkSpd()));
		html = html.replace("%matkspd%", INT_FMT.format(player.getMAtkSpd()));
		
		html = html.replace("%pcrit%", DEC_FMT.format(Formulas.getEffectivePCritRate(player, null) / 10.0) + "%");
		html = html.replace("%mcrit%", DEC_FMT.format(Formulas.getEffectiveMCritRate(player, null, null) / 10.0) + "%");
		
		// Crit power: same bases as Formulas (PCritPower / MCritPower from Balance).
		final double pCritPower = player.calcStat(Stats.CRITICAL_DAMAGE, Config.PCRIT_POWER, null, null);
		final double mCritPower = player.calcStat(Stats.MCRITICAL_DAMAGE, Config.MCRIT_POWER, null, null);
		html = html.replace("%pcritpower%", Math.round(pCritPower * 100.0) + "%");
		html = html.replace("%mcritpower%", Math.round(mCritPower * 100.0) + "%");
		
		html = html.replace("%pveDamage%", String.valueOf(PveGearTier.getEffectiveTier(player)));
		html = html.replace("%pvePhysDmg%", bonusPct(player, Stats.PVE_PHYSICAL_DMG));
		html = html.replace("%pveMagicalDmg%", bonusPct(player, Stats.PVE_MAGICAL_DMG));
		html = html.replace("%pvpPhysDmg%", bonusPct(player, Stats.PVP_PHYSICAL_DMG));
		html = html.replace("%pvpMagicalDmg%", bonusPct(player, Stats.PVP_MAGICAL_DMG));
		html = html.replace("%pvpPhysSkillsDmg%", bonusPct(player, Stats.PVP_PHYS_SKILL_DMG));
		html = html.replace("%pvpPhysDef%", bonusPct(player, Stats.PVP_PHYSICAL_DEF));
		html = html.replace("%pvpMagicalDef%", bonusPct(player, Stats.PVP_MAGICAL_DEF));
		html = html.replace("%pvpPhysSkillsDef%", bonusPct(player, Stats.PVP_PHYS_SKILL_DEF));
		html = html.replace("%critVuln%", bonusPct(player, Stats.CRIT_VULN));
		html = html.replace("%mCritVuln%", bonusPct(player, Stats.MCRITICAL_VULN));
		
		return html;
	}
	
	/** Bonus over base 1.0 → 0%, 1.05 → 5%, 0.95 → -5%. */
	private static String bonusPct(Player player, Stats stat)
	{
		final double mul = player.calcStat(stat, 1, null, null);
		final long bonus = Math.round((mul - 1.0) * 100.0);
		return bonus + "%";
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
