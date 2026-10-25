package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;

import net.sf.l2j.Config;

import net.sf.l2j.gameserver.data.xml.SkillTreeData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.skillnode.EnchantSkillNode;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExEnchantSkillList;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.network.serverpackets.HennaRemoveList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * All-in-one GM Shop (npc 60000): regular {@link Merchant} (buy / sell / multisell / chat pages)
 * plus the services of {@link MultiServiceNpc} (dyes draw/remove and skill enchant).<br>
 * Donation services are opened with the global "bypass services" link, so they need nothing here.<br>
 * HTML path stays data/html/merchant/60000*.htm (inherited from Merchant).
 */
public final class GmShopNpc extends Merchant
{
	/** 60000-X.htm page used as donation menu (the "back" of every donation window returns here). */
	public static final int DONATION_PAGE = 2;
	
	public GmShopNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		// Same PK check as Merchant.
		if (!Config.KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0 && showPkDenyChatWindow(player, "merchant"))
			return;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		
		// Donation prices / days from DonationManager.properties, so donate pages copied to 60000-X.htm show real values.
		html.replace("%skin%", String.valueOf(Config.CANTIDADSKIN));
		html.replace("%argu%", String.valueOf(Config.CANTIDADARGU));
		html.replace("%hero%", String.valueOf(Config.CANTIDAD_HERO_30D));
		html.replace("%herodays%", String.valueOf(Config.DIAS_HERO_30D));
		html.replace("%nobles%", String.valueOf(Config.CANTIDADNOBLES));
		html.replace("%combo%", String.valueOf(Config.CANTIDAD_COMBO_30D));
		html.replace("%combodays%", String.valueOf(Config.DIAS_COMBO_30D));
		html.replace("%name%", String.valueOf(Config.CANTIDADNAMECOLOR));
		html.replace("%title%", String.valueOf(Config.CANTIDADTITLECOLOR));
		html.replace("%sexo%", String.valueOf(Config.CANTIDADSEXO));
		html.replace("%clanrep%", String.valueOf(Config.CANTIDADCLANREP));
		html.replace("%clanlvl%", String.valueOf(Config.CANTIDADCLANLVL));
		html.replace("%clanskill%", String.valueOf(Config.CANTIDADCLANSKILL));
		player.sendPacket(html);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void showEnchantSkillList(Player player)
	{
		// Same behaviour as MultiServiceNpc: every class can enchant here (no teachTo list needed).
		if (player.getClassId().level() < 3)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setHtml("<html><body>You must have 3rd class change quest completed.</body></html>");
			player.sendPacket(html);
			return;
		}

		final List<EnchantSkillNode> skills = SkillTreeData.getInstance().getEnchantSkillsFor(player);
		if (skills.isEmpty())
		{
			player.sendPacket(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT);

			if (player.getLevel() < 74)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1).addNumber(74));
			else
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		}
		else
			player.sendPacket(new ExEnchantSkillList(skills));

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		// Dyes (same commands as MultiServiceNpc).
		if (command.equals("Draw"))
		{
			player.sendPacket(new HennaEquipList(player));
			return;
		}

		if (command.equals("RemoveList"))
		{
			if (player.getHennaList().isEmpty())
			{
				player.sendPacket(SystemMessageId.SYMBOL_NOT_FOUND);
				return;
			}

			player.sendPacket(new HennaRemoveList(player));
			return;
		}

		// Buy / Sell / Multisell / Chat -> Merchant ; EnchantSkillList / EnchantSkillSafe -> Folk.
		super.onBypassFeedback(player, command);
	}
}
