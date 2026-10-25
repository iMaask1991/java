package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.SkillTreeData;
import net.sf.l2j.gameserver.enums.skills.AcquireSkillType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.rebirth.RebirthManager;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.skillnode.FishingSkillNode;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AcquireSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * An instance type extending {@link Merchant}, used for rebirth.
 */
public class Rebirth extends Merchant
{
	public Rebirth(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";

		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;

		// Must match the real folder casing (Linux hosts are case sensitive).
		return "data/html/mods/Rebirth/" + filename + ".htm";
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("SkillListRebirth"))
		{
			if (Config.ENABLE_REBIRTH_SYSTEM)
				showRebirthSkillList(player);
			else
				player.sendMessage("Sistema deshabilitado.");
		}
		else if (command.startsWith("RebirthBoard"))
		{
			// Opens the Community Board Rebirth shrine (misma tabla de costes Price1..PriceN).
			net.sf.l2j.gameserver.communitybbs.CommunityBoard.getInstance().handleCommands(player.getClient(), "_bbstop;rebirth/main.html");
		}
		else if (command.startsWith("Rebirth"))
		{
			// Rebirth original del NPC: logica central (coste de la tabla Price1..PriceN + contador en la DB).
			setRebirth(player);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		showChatWindow(player, getHtmlPath(getNpcId(), val));
	}

	public static void showRebirthSkillList(Player player)
	{
		final List<FishingSkillNode> skills = SkillTreeData.getInstance().getRebirthSkillsFor(player);

		if (skills.isEmpty())
		{
			final int minlevel = SkillTreeData.getInstance().getRequiredLevelForNextRebirthSkill(player);
			if (minlevel > 0)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1).addNumber(minlevel));
			else
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		}
		else
			player.sendPacket(new AcquireSkillList(AcquireSkillType.FISHING, skills));

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Rebirth del NPC original.<br>
	 * <br>
	 * Ahora delega en el sistema central ({@link RebirthManager}) para que:
	 * <ul>
	 * <li>El conteo viva en la tabla {@code character_rebirth}: ya no se puede alterar pagando (el viejo item
	 * contador {@code RebirthCountID} se gastaba al pagar y auto-financiaba el rebirth).</li>
	 * <li>El coste salga de la tabla multi-moneda {@code Price1..PriceN} de {@code Rebirth.properties}.</li>
	 * </ul>
	 * @param player The player that wants to rebirth.
	 */
	public static void setRebirth(Player player)
	{
		RebirthManager.doRebirth(player);
	}
}
