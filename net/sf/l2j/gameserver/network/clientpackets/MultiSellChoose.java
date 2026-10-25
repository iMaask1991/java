package net.sf.l2j.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.AnnounceMultiSellData;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.multisell.Entry;
import net.sf.l2j.gameserver.model.multisell.Ingredient;
import net.sf.l2j.gameserver.model.multisell.PreparedListContainer;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class MultiSellChoose extends L2GameClientPacket
{
	// Special IDs.
	private static final int CLAN_REPUTATION = 65336;
	// private static final int PC_BANG_POINTS = 65436;
	
	private int _listId;
	private int _entryId;
	private int _amount;
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readD();
	}
	
	@Override
	public void runImpl()
	{
		// Event Gear: durante TvT / DM / LM / CTF no se puede tirar, tradear, vender, guardar, enchantar, etc.
		if (net.sf.l2j.gameserver.events.eventgear.EventGearManager.blockAction(getClient().getPlayer()))
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (!FloodProtectors.performAction(getClient(), Action.MULTISELL))
		{
			player.setMultiSell(null);
			return;
		}
		
		if (_amount < 1 || _amount > 9999)
		{
			player.setMultiSell(null);
			return;
		}
		
		final PreparedListContainer list = player.getMultiSell();
		if (list == null || list.getId() != _listId)
		{
			player.setMultiSell(null);
			return;
		}
		
		if (_entryId < 1 || _entryId > list.getEntries().size())
		{
			player.setMultiSell(null);
			return;
		}
		
		final Folk folk = player.getCurrentFolk();
		final boolean communityBoardMultisell = player.isCMultisell() || list.isCommunityBoardMultisell();
		
		if (((folk != null && !list.isNpcAllowed(folk.getNpcId())) || (folk == null && list.isNpcOnly())) && !communityBoardMultisell)
		{
			player.setMultiSell(null);
			return;
		}
		
		if (folk != null && !folk.canInteract(player) && !communityBoardMultisell)
		{
			player.setMultiSell(null);
			return;
		}
		
		final PcInventory inv = player.getInventory();
		final Entry entry = list.getEntries().get(_entryId - 1); // Entry Id begins from 1. We currently use entry IDs as index pointer.
		if (entry == null)
		{
			player.setMultiSell(null);
			return;
		}
		
		if (!entry.isStackable() && _amount > 1)
		{
			player.setMultiSell(null);
			return;
		}
		
		// Evita overflow de int en productos grandes (ej: 2.000.000.000 de adena o canje de Gold Bar).
		for (Ingredient e : entry.getProducts())
		{
			if (e.getItemId() < 0)
				continue;
			
			final long total = (long) e.getItemCount() * _amount;
			final ItemInstance owned = e.isStackable() ? inv.getItemByItemId(e.getItemId()) : null;
			final long current = (owned == null) ? 0 : owned.getCount();
			if (total > Integer.MAX_VALUE || current + total > Integer.MAX_VALUE)
			{
				player.sendMessage("No podes tener mas de " + Integer.MAX_VALUE + " de ese item. Baja la cantidad.");
				return;
			}
		}
		
		int slots = 0;
		int weight = 0;
		for (Ingredient e : entry.getProducts())
		{
			if (e.getItemId() < 0)
				continue;
			
			if (!e.isStackable())
				slots += e.getItemCount() * _amount;
			else if (player.getInventory().getItemByItemId(e.getItemId()) == null)
				slots++;
			
			weight += e.getItemCount() * _amount * e.getWeight();
		}
		
		if (!inv.validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		
		if (!inv.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		
		// Generate a list of distinct ingredients and counts in order to check if the correct item-counts are possessed by the player
		List<Ingredient> ingredientsList = new ArrayList<>(entry.getIngredients().size());
		boolean newIng;
		
		for (Ingredient e : entry.getIngredients())
		{
			newIng = true;
			
			// at this point, the template has already been modified so that enchantments are properly included
			// whenever they need to be applied. Uniqueness of items is thus judged by item id AND enchantment level
			for (int i = ingredientsList.size(); --i >= 0;)
			{
				Ingredient ex = ingredientsList.get(i);
				
				// if the item was already added in the list, merely increment the count
				// this happens if 1 list entry has the same ingredient twice (example 2 swords = 1 dual)
				if (ex.getItemId() == e.getItemId() && ex.getEnchantLevel() == e.getEnchantLevel())
				{
//					if (ex.getItemCount() + e.getItemCount() > Integer.MAX_VALUE)
					if (ex.getItemCount() > Integer.MAX_VALUE - e.getItemCount())	
					{
						player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
						return;
					}
					
					// two same ingredients, merge into one and replace old
					final Ingredient ing = ex.getCopy();
					ing.setItemCount(ex.getItemCount() + e.getItemCount());
					ingredientsList.set(i, ing);
					
					newIng = false;
					break;
				}
			}
			
			// if it's a new ingredient, just store its info directly (item id, count, enchantment)
			if (newIng)
				ingredientsList.add(e);
		}
		
		// now check if the player has sufficient items in the inventory to cover the ingredients' expences
		for (Ingredient e : ingredientsList)
		{
//			if (e.getItemCount() * _amount > Integer.MAX_VALUE)
			if (e.getItemCount() > Integer.MAX_VALUE / _amount)	
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				return;
			}
			
			if (e.getItemId() == CLAN_REPUTATION)
			{
				if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
					return;
				}
				
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
					return;
				}
				
				if (player.getClan().getReputationScore() < e.getItemCount() * _amount)
				{
					player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					return;
				}
			}
			else
			{
				// CB abre el shop completo (sin filtrar inventario): el recipe queda en +0.
				// Si maintainEnchantment está activo pero el enchant del recipe es 0, aceptar cualquier +N (incluido equipado).
				final int enchantCheck = (list.getMaintainEnchantment() && e.getEnchantLevel() > 0) ? e.getEnchantLevel() : -1;
				if (inv.getInventoryItemCount(e.getItemId(), enchantCheck, true) < ((Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount()))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}
			}
		}
		
		List<L2Augmentation> augmentation = new ArrayList<>();
		int transferredEnchant = 0;
		
		for (Ingredient e : entry.getIngredients())
		{
			if (e.getItemId() == CLAN_REPUTATION)
			{
				final int amount = e.getItemCount() * _amount;
				
				player.getClan().takeReputationScore(amount);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(amount));
			}
			else
			{
				ItemInstance itemToTake = inv.getItemByItemId(e.getItemId());
				if (itemToTake == null)
				{
					player.setMultiSell(null);
					return;
				}
				
				if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient())
				{
					// if it's a stackable item, just reduce the amount from the first (only) instance that is found in the inventory
					if (itemToTake.isStackable())
					{
						if (!player.destroyItem("Multisell", itemToTake.getObjectId(), (e.getItemCount() * _amount), player.getTarget(), true))
						{
							player.setMultiSell(null);
							return;
						}
					}
					else
					{
						// a) maintain + enchant del recipe > 0: tomar exactamente ese +N (inventory-only / exc).
						// b) maintain + recipe +0 (CB shop completo): tomar el item (incluye equipado) y transferir su +N.
						// c) sin maintain: tomar el de menor enchant.
						if (list.getMaintainEnchantment() && e.getEnchantLevel() > 0)
						{
							ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantLevel(), true);
							for (int i = 0; i < (e.getItemCount() * _amount); i++)
							{
								if (inventoryContents[i].isAugmented())
									augmentation.add(inventoryContents[i].getAugmentation());
								
								transferredEnchant = Math.max(transferredEnchant, inventoryContents[i].getEnchantLevel());
								if (!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player.getTarget(), true))
								{
									player.setMultiSell(null);
									return;
								}
							}
						}
						else
						{
							for (int i = 1; i <= (e.getItemCount() * _amount); i++)
							{
								itemToTake = pickIngredientItem(inv, e.getItemId(), list.getMaintainEnchantment());
								if (itemToTake == null)
								{
									player.setMultiSell(null);
									return;
								}
								
								if (itemToTake.isAugmented())
									augmentation.add(itemToTake.getAugmentation());
								
								if (list.getMaintainEnchantment() && itemToTake.getItem() != null && itemToTake.getItem().isEquipable())
									transferredEnchant = Math.max(transferredEnchant, itemToTake.getEnchantLevel());
								
								if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true))
								{
									player.setMultiSell(null);
									return;
								}
							}
						}
					}
				}
			}
		}
		
		// Generate the appropriate items
		for (Ingredient e : entry.getProducts())
		{
			if (e.getItemId() == CLAN_REPUTATION)
				player.getClan().addReputationScore(e.getItemCount() * _amount);
			else
			{
				if (e.isStackable())
					inv.addItem("Multisell", e.getItemId(), e.getItemCount() * _amount, player, player.getTarget());
				else
				{
					for (int i = 0; i < (e.getItemCount() * _amount); i++)
					{
						ItemInstance product = inv.addItem("Multisell", e.getItemId(), 1, player, player.getTarget());
						if (product != null && list.getMaintainEnchantment())
						{
							if (i < augmentation.size())
								product.setAugmentation(new L2Augmentation(augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill()));
							
							final int productEnchant = e.getEnchantLevel() > 0 ? e.getEnchantLevel() : transferredEnchant;
							product.setEnchantLevel(productEnchant);
							product.updateDatabase();
						}
					}
				}
				
				// msg part (antes del announce: si el announce falla, igual llega el "earned")
				SystemMessage sm;
				
				if (e.getItemCount() * _amount > 1)
					sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(e.getItemId()).addNumber(e.getItemCount() * _amount);
				else
				{
					final int shownEnchant = e.getEnchantLevel() > 0 ? e.getEnchantLevel() : transferredEnchant;
					if (list.getMaintainEnchantment() && shownEnchant > 0)
						sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_S2).addNumber(shownEnchant).addItemName(e.getItemId());
					else
						sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(e.getItemId());
				}
				player.sendPacket(sm);
				
				try
				{
					AnnounceMultiSellData.getInstance().tryAnnounce(player, _listId, e.getItemId(), e.getItemCount() * _amount, e.getEnchantLevel());
				}
				catch (Exception announceEx)
				{
					LOGGER.warn("AnnounceMultiSell failed for list {} item {}: {}", _listId, e.getItemId(), announceEx.getMessage());
				}
			}
		}
		player.sendPacket(new ItemList(player, false));
		
		// All ok, send success message, remove items and add final product
		player.sendPacket(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC);
		
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		// finally, give the tax to the castle...
		if (folk != null && entry.getTaxAmount() > 0)
			folk.getCastle().addToTreasury(entry.getTaxAmount() * _amount);
	}
	
	/**
	 * Elige el item a consumir. Con maintainEnchantment toma el de mayor +N (upgrade encantado desde CB).
	 * Sin maintain, toma el de menor +N (retail).
	 */
	private static ItemInstance pickIngredientItem(PcInventory inv, int itemId, boolean keepHighestEnchant)
	{
		final ItemInstance[] inventoryContents = inv.getAllItemsByItemId(itemId, true);
		if (inventoryContents == null || inventoryContents.length == 0)
			return null;
		
		ItemInstance chosen = inventoryContents[0];
		for (ItemInstance item : inventoryContents)
		{
			if (keepHighestEnchant)
			{
				if (item.getEnchantLevel() > chosen.getEnchantLevel())
					chosen = item;
				else if (item.getEnchantLevel() == chosen.getEnchantLevel() && chosen.isEquipped() && !item.isEquipped())
					chosen = item;
			}
			else if (item.getEnchantLevel() < chosen.getEnchantLevel())
			{
				chosen = item;
				if (chosen.getEnchantLevel() == 0)
					break;
			}
		}
		return chosen;
	}
}