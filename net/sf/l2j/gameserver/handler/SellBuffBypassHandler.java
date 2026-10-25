package net.sf.l2j.gameserver.handler;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.manager.SellBuffsManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.holder.SellBuffHolder;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Handles all bypass commands for the Sell Buffs system.
 */
public class SellBuffBypassHandler
{
	private static final String[] COMMANDS =
	{
		"sellbuffadd",
		"sellbuffaddskill",
		"sellbuffedit",
		"sellbuffchangeprice",
		"sellbuffremove",
		"sellbuffbuymenu",
		"sellbuffbuyskill",
		"sellbuffbuyskillPet",
		"sellbuffstart",
		"sellbuffstop",
	};
	
	public static boolean handleBypass(String command, Player player)
	{
		if (!Config.SELLBUFF_ENABLED || command == null || command.isEmpty())
			return false;
		
		String cmd = command;
		String params = "";
		final int spaceIndex = command.indexOf(' ');
		if (spaceIndex != -1)
		{
			cmd = command.substring(0, spaceIndex);
			params = command.substring(spaceIndex + 1);
		}
		
		switch (cmd)
		{
			case "sellbuffstart":
				return handleStart(player, params);
			case "sellbuffstop":
				return handleStop(player);
			case "sellbuffadd":
				return handleAdd(player, params);
			case "sellbuffedit":
				return handleEdit(player);
			case "sellbuffchangeprice":
				return handleChangePrice(player, params);
			case "sellbuffremove":
				return handleRemove(player, params);
			case "sellbuffaddskill":
				return handleAddSkill(player, params);
			case "sellbuffbuymenu":
				return handleBuyMenu(player, params);
			case "sellbuffbuyskill":
				return handleBuySkill(player, params, false);
			case "sellbuffbuyskillPet":
				return handleBuySkill(player, params, true);
			default:
				return false;
		}
	}
	
	public static String[] getCommandList()
	{
		return COMMANDS;
	}
	
	public static boolean isSellBuffCommand(String command)
	{
		if (command == null || command.isEmpty())
			return false;
		
		final int spaceIndex = command.indexOf(' ');
		final String cmd = spaceIndex != -1 ? command.substring(0, spaceIndex) : command;
		for (String sellBuffCommand : COMMANDS)
		{
			if (sellBuffCommand.equals(cmd))
				return true;
		}
		return false;
	}
	
	private static boolean handleStart(Player player, String params)
	{
		if (player.isSellBuff() || params == null || params.isEmpty())
			return false;
		
		if (player.getSellingBuffs().isEmpty())
		{
			player.sendMessage("Your list of buffs is empty, please add some buffs first!");
			return false;
		}
		
		final StringBuilder title = new StringBuilder("BUFF SELL: ");
		final StringTokenizer st = new StringTokenizer(params, " ");
		while (st.hasMoreTokens())
			title.append(st.nextToken()).append(" ");
		
		if (title.length() > 40)
		{
			player.sendMessage("Your title cannot exceed 29 characters in length. Please try again.");
			return false;
		}
		
		SellBuffsManager.getInstance().startSellBuffs(player, title.toString().trim());
		return true;
	}
	
	private static boolean handleStop(Player player)
	{
		if (player.isSellBuff())
			SellBuffsManager.getInstance().stopSellBuffs(player);
		return true;
	}
	
	private static boolean handleAdd(Player player, String params)
	{
		if (player.isSellBuff())
			return false;
		
		int index = 0;
		if (params != null && !params.isEmpty() && StringUtil.isDigit(params))
			index = Integer.parseInt(params);
		
		SellBuffsManager.getInstance().sendBuffChoiceMenu(player, index);
		return true;
	}
	
	private static boolean handleEdit(Player player)
	{
		if (player.isSellBuff())
			return false;
		
		SellBuffsManager.getInstance().sendBuffEditMenu(player);
		return true;
	}
	
	private static boolean handleChangePrice(Player player, String params)
	{
		if (player.isSellBuff() || params == null || params.isEmpty())
			return false;
		
		final StringTokenizer st = new StringTokenizer(params, " ");
		if (!st.hasMoreTokens())
			return false;
		
		final int skillId = Integer.parseInt(st.nextToken());
		if (!st.hasMoreTokens())
			return false;
		
		int price;
		try
		{
			price = Integer.parseInt(st.nextToken());
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("Too big price! Maximum price is " + Config.SELLBUFF_MAX_PRICE);
			SellBuffsManager.getInstance().sendBuffEditMenu(player);
			return false;
		}
		
		if (!SellBuffsManager.getInstance().isValidPrice(price))
		{
			player.sendMessage("Price must be between " + Config.SELLBUFF_MIN_PRICE + " and " + Config.SELLBUFF_MAX_PRICE + ".");
			SellBuffsManager.getInstance().sendBuffEditMenu(player);
			return false;
		}
		
		final L2Skill skillToChange = player.getSkill(skillId);
		if (skillToChange == null)
			return false;
		
		for (SellBuffHolder holder : player.getSellingBuffs())
		{
			if (holder.getSkillId() == skillToChange.getId())
			{
				holder.setPrice(price);
				player.sendMessage("Price of " + skillToChange.getName() + " has been changed to " + price + "!");
				SellBuffsManager.getInstance().sendBuffEditMenu(player);
				return true;
			}
		}
		return false;
	}
	
	private static boolean handleRemove(Player player, String params)
	{
		if (player.isSellBuff() || params == null || params.isEmpty())
			return false;
		
		final int skillId = Integer.parseInt(params.trim());
		final L2Skill skillToRemove = player.getSkill(skillId);
		if (skillToRemove == null)
			return false;
		
		for (SellBuffHolder holder : player.getSellingBuffs())
		{
			if (holder.getSkillId() == skillToRemove.getId() && player.getSellingBuffs().remove(holder))
			{
				player.sendMessage("Skill " + skillToRemove.getName() + " has been removed!");
				SellBuffsManager.getInstance().sendBuffEditMenu(player);
				return true;
			}
		}
		return false;
	}
	
	private static boolean handleAddSkill(Player player, String params)
	{
		if (player.isSellBuff() || params == null || params.isEmpty())
			return false;
		
		final StringTokenizer st = new StringTokenizer(params, " ");
		if (!st.hasMoreTokens())
			return false;
		
		final int skillId = Integer.parseInt(st.nextToken());
		if (!st.hasMoreTokens())
			return false;
		
		int price;
		try
		{
			price = Integer.parseInt(st.nextToken());
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("Too big price! Maximum price is " + Config.SELLBUFF_MAX_PRICE);
			SellBuffsManager.getInstance().sendBuffChoiceMenu(player, 0);
			return false;
		}
		
		final L2Skill skillToAdd = player.getSkill(skillId);
		if (skillToAdd == null || !SellBuffsManager.getInstance().isAllowedBuff(skillId))
			return false;
		
		if (!SellBuffsManager.getInstance().isValidPrice(price))
		{
			player.sendMessage("Price must be between " + Config.SELLBUFF_MIN_PRICE + " and " + Config.SELLBUFF_MAX_PRICE + ".");
			return false;
		}
		
		if (player.getSellingBuffs().size() >= Config.SELLBUFF_MAX_BUFFS)
		{
			player.sendMessage("You already reached max count of buffs! Max buffs is: " + Config.SELLBUFF_MAX_BUFFS);
			return false;
		}
		
		if (!SellBuffsManager.getInstance().isInSellList(player, skillToAdd))
		{
			player.getSellingBuffs().add(new SellBuffHolder(skillToAdd.getId(), price));
			player.sendMessage(skillToAdd.getName() + " has been added!");
			SellBuffsManager.getInstance().sendBuffChoiceMenu(player, 0);
		}
		return true;
	}
	
	private static boolean handleBuyMenu(Player player, String params)
	{
		if (params == null || params.isEmpty())
			return false;
		
		final StringTokenizer st = new StringTokenizer(params, " ");
		if (!st.hasMoreTokens())
			return false;
		
		final int objId = Integer.parseInt(st.nextToken());
		int index = 0;
		if (st.hasMoreTokens())
			index = Integer.parseInt(st.nextToken());
		
		final Player seller = World.getInstance().getPlayer(objId);
		if (seller == null || !seller.isSellBuff() || !MathUtil.checkIfInRange(Npc.INTERACTION_DISTANCE, player, seller, true))
			return false;
		
		SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
		return true;
	}
	
	private static boolean handleBuySkill(Player player, String params, boolean forPet)
	{
		if (params == null || params.isEmpty())
			return false;
		
		final StringTokenizer st = new StringTokenizer(params, " ");
		if (st.countTokens() < 2)
			return false;
		
		final int objId = Integer.parseInt(st.nextToken());
		final int skillId = Integer.parseInt(st.nextToken());
		int index = 0;
		if (st.hasMoreTokens())
			index = Integer.parseInt(st.nextToken());
		
		final Player seller = World.getInstance().getPlayer(objId);
		if (seller == null)
			return false;
		
		final L2Skill skillToBuy = seller.getSkill(skillId);
		if (!seller.isSellBuff() || !MathUtil.checkIfInRange(Npc.INTERACTION_DISTANCE, player, seller, true) || skillToBuy == null)
			return false;
		
		if (seller.getCurrentMp() < (skillToBuy.getMpConsume() * Config.SELLBUFF_MP_MULTIPLER))
		{
			player.sendMessage(seller.getName() + " has not enough mana for " + skillToBuy.getName() + "!");
			SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
			return false;
		}
		
		final int itemConsumeId = skillToBuy.getItemConsumeId();
		final int itemConsumeCount = skillToBuy.getItemConsume();
		if (itemConsumeId > 0 && itemConsumeCount > 0 && seller.getInventory().getInventoryItemCount(itemConsumeId, -1) < itemConsumeCount)
		{
			final Item requiredItem = ItemTable.getInstance().getTemplate(itemConsumeId);
			final String itemName = requiredItem != null ? requiredItem.getName() : "required item";
			seller.sendMessage(player.getName() + " tried to buy " + skillToBuy.getName() + " but you do not have enough " + itemName + "!");
			player.sendMessage(seller.getName() + " doesn't have enough " + itemName + " to cast " + skillToBuy.getName() + "!");
			SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
			return false;
		}
		
		SellBuffHolder holder = null;
		for (SellBuffHolder sellBuff : seller.getSellingBuffs())
		{
			if (sellBuff.getSkillId() == skillToBuy.getId())
			{
				holder = sellBuff;
				break;
			}
		}
		
		if (holder == null)
			return false;
		
		if (player.getInventory().getInventoryItemCount(Config.SELLBUFF_PAYMENT_ID, -1) < holder.getPrice())
		{
			final Item paymentItem = ItemTable.getInstance().getTemplate(Config.SELLBUFF_PAYMENT_ID);
			player.sendMessage("Not enough " + (paymentItem != null ? paymentItem.getName() : "items") + "!");
			SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
			return false;
		}
		
		final Summon summon = player.getSummon();
		if (forPet)
		{
			if (summon == null || summon.isDead())
			{
				player.sendMessage("Your pet must be summoned and alive to receive buffs.");
				SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
				return false;
			}
		}
		
		if (!transferPayment(player, seller, holder.getPrice()))
		{
			player.sendMessage("Payment failed.");
			SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
			return false;
		}
		
		seller.reduceCurrentMp(skillToBuy.getMpConsume() * Config.SELLBUFF_MP_MULTIPLER);
		
		if (itemConsumeId > 0 && itemConsumeCount > 0)
			seller.destroyItemByItemId("SellBuff", itemConsumeId, itemConsumeCount, player, true);
		
		if (forPet)
		{
			skillToBuy.getEffects(seller, summon);
			seller.broadcastPacket(new MagicSkillUse(seller, summon, skillToBuy.getId(), skillToBuy.getLevel(), 500, 0));
		}
		else
		{
			skillToBuy.getEffects(seller, player);
			seller.broadcastPacket(new MagicSkillUse(seller, player, skillToBuy.getId(), skillToBuy.getLevel(), 500, 0));
		}
		
		SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
		return true;
	}
	
	private static boolean transferPayment(Player buyer, Player seller, int price)
	{
		if (buyer.getInventory().getInventoryItemCount(Config.SELLBUFF_PAYMENT_ID, -1) < price)
			return false;
		
		if (Config.SELLBUFF_PAYMENT_ID == 57)
		{
			if (!buyer.reduceAdena("SellBuff", price, seller, true))
				return false;
			
			seller.addAdena("SellBuff", price, seller, true);
			return true;
		}
		
		if (!buyer.destroyItemByItemId("SellBuff", Config.SELLBUFF_PAYMENT_ID, price, seller, true))
			return false;
		
		seller.addItem("SellBuff", Config.SELLBUFF_PAYMENT_ID, price, seller, true);
		return true;
	}
	
	private SellBuffBypassHandler()
	{
	}
}
