package net.sf.l2j.gameserver.data.manager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.l2j.Config;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.StoreType;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.SellBuffHolder;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgSell;

import org.w3c.dom.Document;

/**
 * Sell Buffs system manager.
 */
public class SellBuffsManager implements IXmlReader
{
	private static final Set<Integer> ALLOWED_BUFFS = new HashSet<>();
	private static final String HTML_FOLDER = "data/html/mods/SellBuffs/";
	
	protected SellBuffsManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (!Config.SELLBUFF_ENABLED)
			return;
		
		ALLOWED_BUFFS.clear();
		parseFile("./data/xml/SellBuffData.xml");
		LOGGER.info("Loaded {} allowed sell buffs.", ALLOWED_BUFFS.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "skill", skillNode -> ALLOWED_BUFFS.add(parseInteger(skillNode.getAttributes(), "id"))));
	}
	
	public void sendSellMenu(Player player)
	{
		if (!Config.SELLBUFF_ENABLED)
		{
			player.sendMessage("Sell Buffs system is disabled.");
			return;
		}
		
		final String html = HtmCache.getInstance().getHtmForce(HTML_FOLDER + (player.isSellBuff() ? "BuffMenu_already.html" : "BuffMenu.html"));
		BaseBBSManager.separateAndSend(html, player);
	}
	
	public void sendBuffChoiceMenu(Player player, int index)
	{
		String html = HtmCache.getInstance().getHtmForce(HTML_FOLDER + "BuffChoice.html");
		html = html.replace("%list%", buildSkillMenu(player, index));
		BaseBBSManager.separateAndSend(html, player);
	}
	
	public void sendBuffEditMenu(Player player)
	{
		String html = HtmCache.getInstance().getHtmForce(HTML_FOLDER + "BuffChoice.html");
		html = html.replace("%list%", buildEditMenu(player));
		BaseBBSManager.separateAndSend(html, player);
	}
	
	public void sendBuffMenu(Player player, Player seller, int index)
	{
		if (!seller.isSellBuff() || seller.getSellingBuffs().isEmpty())
			return;
		
		String html = HtmCache.getInstance().getHtmForce(HTML_FOLDER + "BuffBuyMenu.html");
		html = html.replace("%list%", buildBuffMenu(seller, index));
		BaseBBSManager.separateAndSend(html, player);
	}
	
	public void startSellBuffs(Player player, String title)
	{
		if (!canStartSellBuffs(player))
			return;
		
		player.sitDown();
		player.setSellBuff(true);
		player.setStoreType(StoreType.PACKAGE_SELL);
		player.getSellList().setTitle(title);
		player.getSellList().setPackaged(true);
		player.broadcastUserInfo();
		player.broadcastPacket(new PrivateStoreMsgSell(player));
		player.sendPacket(new PrivateStoreMsgSell(player));
		sendSellMenu(player);
	}
	
	public void stopSellBuffs(Player player)
	{
		player.setSellBuff(false);
		player.setStoreType(StoreType.NONE);
		player.standUp();
		player.broadcastUserInfo();
		sendSellMenu(player);
	}
	
	public void stopSellBuffsSilent(Player player)
	{
		player.setSellBuff(false);
		player.setStoreType(StoreType.NONE);
	}
	
	public boolean isInSellList(Player player, L2Skill skill)
	{
		for (SellBuffHolder holder : player.getSellingBuffs())
		{
			if (holder.getSkillId() == skill.getId())
				return true;
		}
		return false;
	}
	
	public boolean isAllowedBuff(int skillId)
	{
		return ALLOWED_BUFFS.contains(skillId);
	}
	
	public boolean canStartSellBuffs(Player player)
	{
		if (player.isAlikeDead())
		{
			player.sendMessage("You can't sell buffs in fake death!");
			return false;
		}
		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			player.sendMessage("You can't sell buffs with Olympiad status!");
			return false;
		}
		if (isPlayerInEvent(player))
		{
			player.sendMessage("You can't sell buffs while registered in an event!");
			return false;
		}
		if (player.isCursedWeaponEquipped() || player.getKarma() > 0)
		{
			player.sendMessage("You can't sell buffs in Chaotic state!");
			return false;
		}
		if (player.isInDuel())
		{
			player.sendMessage("You can't sell buffs in Duel state!");
			return false;
		}
		if (player.isFishing())
		{
			player.sendMessage("You can't sell buffs while fishing.");
			return false;
		}
		if (player.isMounted() || player.isFlying())
		{
			player.sendMessage("You can't sell buffs in Mount state!");
			return false;
		}
		if (player.isInsideZone(ZoneId.NO_STORE) || !player.isInsideZone(ZoneId.PEACE) || player.isInJail())
		{
			player.sendMessage("You can't sell buffs here!");
			return false;
		}
		return true;
	}
	
	public boolean isValidPrice(int price)
	{
		return price >= Config.SELLBUFF_MIN_PRICE && price <= Config.SELLBUFF_MAX_PRICE;
	}
	
	private static boolean isPlayerInEvent(Player player)
	{
		return DMEvent.isPlayerParticipant(player) || CTFEvent.isPlayerParticipant(player.getObjectId()) || TvTEvent.isPlayerParticipant(player.getObjectId()) || LMEvent.isPlayerParticipant(player) || KTBEvent.isPlayerParticipant(player);
	}
	
	private String buildBuffMenu(Player seller, int index)
	{
		final int ceiling = 9;
		int nextIndex = -1;
		int previousIndex = -1;
		int emptyFields = 0;
		final StringBuilder sb = new StringBuilder();
		final List<SellBuffHolder> sellList = new ArrayList<>();
		
		int count = 0;
		for (SellBuffHolder holder : seller.getSellingBuffs())
		{
			count++;
			if (count > index && count <= (ceiling + index))
				sellList.add(holder);
		}
		
		if (count > 9 && count > (index + 9))
			nextIndex = index + 9;
		
		if (index >= 9)
			previousIndex = index - 9;
		
		emptyFields = ceiling - sellList.size();
		
		sb.append("<br>");
		sb.append("MP: <font color=\"1E90FF\">").append((long) seller.getCurrentMp()).append("</font> / ").append(seller.getMaxMp());
		sb.append("<br>");
		
		sb.append("<table>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr>");
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Name\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Level\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"MP Cost\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Price\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Player\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Pet\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("</tr>");
		
		final Item paymentItem = ItemTable.getInstance().getTemplate(Config.SELLBUFF_PAYMENT_ID);
		final String paymentName = paymentItem != null ? paymentItem.getName() : "";
		
		for (SellBuffHolder holder : sellList)
		{
			final L2Skill skill = seller.getSkill(holder.getSkillId());
			if (skill == null)
			{
				emptyFields++;
				continue;
			}
			
			sb.append("<tr>");
			sb.append("<td fixwidth=\"20\"></td>");
			sb.append("<td align=center><img src=\"").append(getSkillIcon(skill.getId())).append("\" width=\"32\" height=\"32\"></td>");
			sb.append("<td align=center>").append(formatSkillName(skill)).append("</td>");
			sb.append("<td align=center>").append(formatSkillLevel(skill)).append("</td>");
			sb.append("<td align=center> <font color=\"1E90FF\">").append(skill.getMpConsume() * Config.SELLBUFF_MP_MULTIPLER).append("</font></td>");
			sb.append("<td align=center> ").append(StringUtil.formatNumber(holder.getPrice())).append(" <font color=\"LEVEL\"> ").append(paymentName).append("</font> </td>");
			sb.append("<td align=right fixwidth=\"40\"><button value=\"Buy Buff\" action=\"bypass sellbuffbuyskill ").append(seller.getObjectId()).append(" ").append(skill.getId()).append(" ").append(index).append("\" width=\"75\" height=\"21\" back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td align=right fixwidth=\"40\"><button value=\"Buy Buff\" action=\"bypass sellbuffbuyskillPet ").append(seller.getObjectId()).append(" ").append(skill.getId()).append(" ").append(index).append("\" width=\"75\" height=\"21\" back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr>");
			sb.append("<tr><td><br></td></tr>");
		}
		
		for (int i = 0; i < emptyFields; i++)
		{
			sb.append("<tr>");
			sb.append("<td fixwidth=\"20\" height=\"32\"></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=left></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=center fixwidth=\"50\"></td>");
			sb.append("</tr>");
			sb.append("<tr><td><br></td></tr>");
		}
		
		sb.append("</table>");
		sb.append("<table width=\"250\" border=\"0\"><tr>");
		if (previousIndex > -1)
			sb.append("<td align=left><button value=\"Previous Page\" action=\"bypass sellbuffbuymenu ").append(seller.getObjectId()).append(" ").append(previousIndex).append("\" width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		if (nextIndex > -1)
			sb.append("<td align=right><button value=\"Next Page\" action=\"bypass sellbuffbuymenu ").append(seller.getObjectId()).append(" ").append(nextIndex).append("\" width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private String buildEditMenu(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<table><tr>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Name\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Level\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Old Price\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"New Price\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Action\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Remove\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("</tr>");
		
		if (player.getSellingBuffs().isEmpty())
		{
			sb.append("</table><br><br><br>You don't have added any buffs yet!");
		}
		else
		{
			for (SellBuffHolder holder : player.getSellingBuffs())
			{
				final L2Skill skill = player.getSkill(holder.getSkillId());
				if (skill == null)
					continue;
				
				sb.append("<tr>");
				sb.append("<td align=center><img src=\"").append(getSkillIcon(skill.getId())).append("\" width=\"32\" height=\"32\"></td>");
				sb.append("<td align=center>").append(formatSkillName(skill)).append("</td>");
				sb.append("<td align=center>").append(formatSkillLevel(skill)).append("</td>");
				sb.append("<td align=center> ").append(StringUtil.formatNumber(holder.getPrice())).append(" </td>");
				sb.append("<td align=center><edit var=\"price_").append(skill.getId()).append("\" width=100 type=\"number\"></td>");
				sb.append("<td align=center><button value=\"Edit\" action=\"bypass sellbuffchangeprice ").append(skill.getId()).append(" $price_").append(skill.getId()).append("\" width=\"70\" height=\"20\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				sb.append("<td align=center><button value=\" X \" action=\"bypass sellbuffremove ").append(skill.getId()).append("\" width=\"40\" height=\"20\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
		}
		return sb.toString();
	}
	
	private String buildSkillMenu(Player player, int index)
	{
		final int ceiling = index + 9;
		int nextIndex = -1;
		int previousIndex = -1;
		final StringBuilder sb = new StringBuilder();
		final List<L2Skill> skillList = new ArrayList<>();
		
		int count = 0;
		for (L2Skill skill : player.getSkills().values())
		{
			if (ALLOWED_BUFFS.contains(skill.getId()) && !isInSellList(player, skill))
			{
				count++;
				if (count > index && count <= ceiling)
					skillList.add(skill);
			}
		}
		
		if (count > 9 && count > (index + 9))
			nextIndex = index + 9;
		
		if (index >= 9)
			previousIndex = index - 9;
		
		sb.append("<table><tr><td><br></td></tr><tr>");
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=80 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Name\" width=100 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Level\" width=70 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Price\" width=100 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td> <button action=\"\" value=\"Action\" width=110 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>");
		sb.append("<td fixwidth=\"40\"></td></tr>");
		
		if (skillList.isEmpty())
		{
			sb.append("</table><br><br><br>At this moment you cannot add any buffs!");
		}
		else
		{
			for (L2Skill skill : skillList)
			{
				sb.append("<tr>");
				sb.append("<td fixwidth=\"20\"></td>");
				sb.append("<td align=center><img src=\"").append(getSkillIcon(skill.getId())).append("\" width=\"32\" height=\"32\"></td>");
				sb.append("<td align=center>").append(formatSkillName(skill)).append("</td>");
				sb.append("<td align=center>").append(formatSkillLevel(skill)).append("</td>");
				sb.append("<td align=left><edit var=\"price_").append(skill.getId()).append("\" width=100 type=\"number\"></td>");
				sb.append("<td align=center><button value=\" Add Buff \" action=\"bypass sellbuffaddskill ").append(skill.getId()).append(" $price_").append(skill.getId()).append("\" width=\"110\" height=\"20\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				sb.append("</tr><tr><td><br></td></tr>");
			}
			sb.append("</table>");
		}
		
		sb.append("<table width=\"250\" border=\"0\"><tr>");
		if (previousIndex > -1)
			sb.append("<td align=left><button value=\"Previous Page\" action=\"bypass sellbuffadd ").append(previousIndex).append("\" width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		if (nextIndex > -1)
			sb.append("<td align=right><button value=\"Next Page\" action=\"bypass sellbuffadd ").append(nextIndex).append("\" width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private static String formatSkillName(L2Skill skill)
	{
		if (skill.getLevel() > 100)
			return skill.getName() + "<font color=\"LEVEL\"> + " + (skill.getLevel() % 100) + "</font>";
		return skill.getName();
	}
	
	private static String formatSkillLevel(L2Skill skill)
	{
		return Integer.toString(skill.getLevel() > 100 ? SkillTable.getInstance().getMaxLevel(skill.getId()) : skill.getLevel());
	}
	
	private static String getSkillIcon(int skillId)
	{
		return String.format("icon.skill%04d", skillId);
	}
	
	public static SellBuffsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SellBuffsManager INSTANCE = new SellBuffsManager();
	}
}
