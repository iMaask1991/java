package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.handler.itemhandlers.HeroCoin;
import net.sf.l2j.gameserver.handler.itemhandlers.VipCoin;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

public class DonationManager extends Folk
{
	public enum Races
	{
		FIGHTER(net.sf.l2j.gameserver.enums.actors.ClassRace.HUMAN, ClassId.HUMAN_FIGHTER),
		MAGE(net.sf.l2j.gameserver.enums.actors.ClassRace.HUMAN, ClassId.HUMAN_MYSTIC),
		ELVENFIGHTER(net.sf.l2j.gameserver.enums.actors.ClassRace.ELF, ClassId.ELVEN_FIGHTER),
		ELVENMAGE(net.sf.l2j.gameserver.enums.actors.ClassRace.ELF, ClassId.ELVEN_MYSTIC),
		DARKFIGHTER(net.sf.l2j.gameserver.enums.actors.ClassRace.DARK_ELF, ClassId.DARK_FIGHTER),
		DARKMAGE(net.sf.l2j.gameserver.enums.actors.ClassRace.DARK_ELF, ClassId.DARK_MYSTIC),
		ORCFIGHTER(net.sf.l2j.gameserver.enums.actors.ClassRace.ORC, ClassId.ORC_FIGHTER),
		ORCMAGE(net.sf.l2j.gameserver.enums.actors.ClassRace.ORC, ClassId.ORC_MYSTIC),
		DWARVEN(net.sf.l2j.gameserver.enums.actors.ClassRace.DWARF, ClassId.DWARVEN_FIGHTER);
		
		private net.sf.l2j.gameserver.enums.actors.ClassRace races;
		private ClassId classId;
	
		private Races(net.sf.l2j.gameserver.enums.actors.ClassRace races, ClassId classid)
		{
			this.races = races;
			this.classId = classid;
		}
		
		public int getRaceId()
		{
			return races.ordinal();
		}
		
		public int getClassId()
		{
			return classId.getId();
		}
	}
	public DonationManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		if (CommunityBoard.openCustomNpcBoard(player, getNpcId()))
			return;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/mods/donationmanager/Services.htm");
		html.replace("%skin%", String.valueOf(Config.CANTIDADSKIN));
		html.replace("%argu%", String.valueOf(Config.CANTIDADARGU));
		html.replace("%hero%", String.valueOf(Config.CANTIDAD_HERO_30D));
		html.replace("%nobles%", String.valueOf(Config.CANTIDADNOBLES));
		html.replace("%combo%", String.valueOf(Config.CANTIDAD_COMBO_30D));
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player == null)
			return;
		
		if (command.equalsIgnoreCase("hero"))
		{
			buyHero30(player);
			return;
		}
		if (command.equalsIgnoreCase("nobles"))
		{
			buyNobles(player);
			return;
		}
		if (command.equalsIgnoreCase("combo"))
		{
			buyCombo30(player);
			return;
		}
		
		// Services are handled via RequestBypassToServer -> MultiManager.
		if (command.startsWith("services"))
			showChatWindow(player);
		else
			super.onBypassFeedback(player, command);
	}
	
	public static void buyHero30(Player player)
	{
		if (player == null)
			return;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		if (player.isHero() && (player.getHeroEndTime() == 0 || player.getHeroEndTime() > System.currentTimeMillis()))
		{
			html.setFile("data/html/mods/donationmanager/alreadyhero.htm");
			player.sendPacket(html);
			return;
		}
		
		final int price = Config.CANTIDAD_HERO_30D;
		final int days = Config.DIAS_HERO_30D;
		if (price <= 0 || days <= 0)
		{
			player.sendMessage("Hero no esta configurado.");
			return;
		}
		
		final int payId = Config.ITEMSKIN > 0 ? Config.ITEMSKIN : 10001;
		if (!player.destroyItemByItemId("DonateHero", payId, price, player, true))
		{
			html.setFile("data/html/mods/donationmanager/noitems.htm");
			player.sendPacket(html);
			return;
		}
		
		if (!HeroCoin.apply(player, days))
		{
			player.addItem("DonateHeroRefund", payId, price, player, true);
			return;
		}
		
		html.setFile("data/html/mods/donationmanager/okhero.htm");
		player.sendPacket(html);
	}
	
	public static void buyNobles(Player player)
	{
		if (player == null)
			return;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		if (player.isNoble())
		{
			html.setFile("data/html/mods/donationmanager/alreadynobles.htm");
			player.sendPacket(html);
			return;
		}
		
		final int payId = Config.ITEMNOBLES > 0 ? Config.ITEMNOBLES : 10001;
		final int price = Config.CANTIDADNOBLES;
		if (price <= 0)
		{
			player.sendMessage("Noblesse no esta configurado.");
			return;
		}
		
		if (!player.destroyItemByItemId("DonateNobles", payId, price, player, true))
		{
			html.setFile("data/html/mods/donationmanager/noitems.htm");
			player.sendPacket(html);
			return;
		}
		
		player.setNoble(true, true);
		player.sendPacket(new UserInfo(player));
		html.setFile("data/html/mods/donationmanager/oknobles.htm");
		player.sendPacket(html);
	}
	
	/**
	 * COMBO: VIP + Hero + Nobless en una sola compra (mismo criterio que el Community Board).<br>
	 * VIP y Hero NO se acumulan: si ya hay un status activo, hay que esperar a que venza para poder<br>
	 * comprar el pack de nuevo (el Noblesse, al ser permanente, no tiene vencimiento que esperar).
	 */
	public static void buyCombo30(Player player)
	{
		if (player == null)
			return;

		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		if (!Config.COMBO_ENABLED)
		{
			player.sendMessage("El Combo VIP + Hero + Nobless esta desactivado.");
			return;
		}

		final int price = Config.CANTIDAD_COMBO_30D;
		final int days = Config.DIAS_COMBO_30D;
		if (price <= 0 || days <= 0)
		{
			player.sendMessage("El Combo no esta configurado.");
			return;
		}

		final long now = System.currentTimeMillis();

		// No dejar comprar de nuevo mientras el VIP del combo anterior siga activo.
		if (Config.COMBO_GIVE_VIP && player.isVip() && player.getVipEndTime() > now)
		{
			player.sendMessage("Ya tenes VIP activo. Espera a que venza para comprar el Combo de nuevo.");
			return;
		}

		// No dejar comprar de nuevo mientras el Hero del combo anterior siga activo.
		if (Config.COMBO_GIVE_HERO && player.isHero() && (player.getHeroEndTime() == 0 || player.getHeroEndTime() > now))
		{
			player.sendMessage("Ya tenes Hero activo. Espera a que venza para comprar el Combo de nuevo.");
			return;
		}

		final int payId = Config.ITEMSKIN > 0 ? Config.ITEMSKIN : 10001;
		if (!player.destroyItemByItemId("DonateCombo", payId, price, player, true))
		{
			html.setFile("data/html/mods/donationmanager/noitems.htm");
			player.sendPacket(html);
			return;
		}

		boolean applied = false;

		// apply() (no stackea) en vez de addDays() (acumulaba sobre el vencimiento): ya validamos arriba
		// que no hay status activo, asi que esto solo actua como defensa extra ante una carrera de condicion.
		if (Config.COMBO_GIVE_VIP && VipCoin.apply(player, days))
			applied = true;

		if (Config.COMBO_GIVE_HERO && HeroCoin.apply(player, days))
			applied = true;

		if (Config.COMBO_GIVE_NOBLESS)
		{
			if (!player.isNoble())
			{
				player.setNoble(true, true);
				player.sendPacket(new UserInfo(player));
			}
			// Ya era noble: no hay nada que dar, pero el combo se cobra igual (no se devuelve).
			applied = true;
		}

		if (!applied)
		{
			player.addItem("DonateComboRefund", payId, price, player, true);
			return;
		}

		player.sendMessage("COMBO activado: VIP " + days + "d + Hero " + days + "d + Nobless.");
		html.setFile("data/html/mods/donationmanager/okcombo.htm");
		player.sendPacket(html);
	}
	
	public static void Human(Player player)
	{
	Races races = null;
	races = Races.FIGHTER;
	   
	if (!(player.getInventory().getInventoryItemCount(Config.ITEMSKIN, 0) >= Config.CANTIDADSKIN))
	{
	    final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	    html.setFile("data/html/mods/donationmanager/noitems.htm");
	  	player.sendPacket(html);
		return;
	 } 
	 setSkins(player);
	 setRaceCustomSkin(player, races);
	 }
	
	 public static void HumanMage(Player player)
	 {
	 Races races = null;
	 races = Races.MAGE;
	 if (!(player.getInventory().getInventoryItemCount(Config.ITEMSKIN, 0) >= Config.CANTIDADSKIN))
	 {
	  	final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	  	html.setFile("data/html/mods/donationmanager/noitems.htm");
	  	player.sendPacket(html);
	 	return;
	 } 
	 setSkins(player);
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void Elf(Player player)
	 {
	 Races races = null;
	 races = Races.ELVENFIGHTER;
	 if (!(player.getInventory().getInventoryItemCount(Config.ITEMSKIN, 0) >= Config.CANTIDADSKIN))
	 {
	    final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	    html.setFile("data/html/mods/donationmanager/noitems.htm");
	    player.sendPacket(html);
	 	return;
	 } 
	 setSkins(player);
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void DarkElf(Player player)
	 {
	 Races races = null;
	 races = Races.DARKFIGHTER;
	 if (!(player.getInventory().getInventoryItemCount(Config.ITEMSKIN, 0) >= Config.CANTIDADSKIN))
	 {
	  	final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	  	html.setFile("data/html/mods/donationmanager/noitems.htm");
	  	player.sendPacket(html);
	 	return;
	 } 
	 setSkins(player);
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void Orc(Player player)
	 {
	 Races races = null;
	 races = Races.ORCFIGHTER;
	 if (!(player.getInventory().getInventoryItemCount(Config.ITEMSKIN, 0) >= Config.CANTIDADSKIN))
	 {
	    final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	    html.setFile("data/html/mods/donationmanager/noitems.htm");
	    player.sendPacket(html);
	    return;
	 } 
	 setSkins(player);
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void OrcMage(Player player)
	 {
	 Races races = null;
	 races = Races.ORCMAGE;
	 if (!(player.getInventory().getInventoryItemCount(Config.ITEMSKIN, 0) >= Config.CANTIDADSKIN))
	 {
	    final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	    html.setFile("data/html/mods/donationmanager/noitems.htm");
	  	player.sendPacket(html);
	 	return;
	 } 
	 setSkins(player);
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void Dwarf(Player player)
	 {
	 Races races = null;
	 races = Races.DWARVEN;
	 if (!(player.getInventory().getInventoryItemCount(Config.ITEMSKIN, 0) >= Config.CANTIDADSKIN))
	 {
	  	final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	  	html.setFile("data/html/mods/donationmanager/noitems.htm");
	  	player.sendPacket(html);
	 	return;
	 } 
	 setSkins(player);
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void ChangeSex(Player player)
	 {
	 if (!(player.getInventory().getInventoryItemCount(Config.ITEMSEXO, 0) >= Config.CANTIDADSEXO))
	 {
	        final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
	 	  	html.setFile("data/html/mods/donationmanager/noitems.htm");
		  	player.sendPacket(html);
			return;
	  }
	  if (player.getAppearance().getSex() == Sex.MALE)
	  {
	   player.getAppearance().setSex(Sex.FEMALE);
	  }
	  else
	  {
	  player.getAppearance().setSex(Sex.MALE);
	  }
	  player.broadcastUserInfo();
	  setSexos(player);  
	  }

	 public static void ClanRep(Player player)
	 {
		if (!(player.getInventory().getInventoryItemCount(Config.ITEMCLANREP, 0) >= Config.CANTIDADCLANREP))
		{
		    final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
			html.setFile("data/html/mods/donationmanager/noitems.htm");
		  	player.sendPacket(html);
			return;
		}
	 	else if (player.getClan() == null)
	 	{
	 		player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
	 		return;
	 	}
	 	else if (!player.isClanLeader())
	 	{
	 		player.sendPacket(SystemMessageId.NOT_AUTHORIZED_TO_BESTOW_RIGHTS);
	 		return;
	 	}
	 	else
	 	{
	 		player.destroyItemByItemId("Consume", Config.ITEMCLANREP, Config.CANTIDADCLANREP, player, true);
	 		player.getClan().addReputationScore(20000);
	 		player.getClan().broadcastClanStatus();
	 		player.sendMessage("Your clan reputation score has been increased.");
	 	}
	 }
	 
	 public static void Clanlvl(Player player)
	 {
		if (!(player.getInventory().getInventoryItemCount(Config.ITEMCLANLVL, 0) >= Config.CANTIDADCLANLVL))
		{
			 final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
			 html.setFile("data/html/mods/donationmanager/noitems.htm");
			 player.sendPacket(html);
			 return;
		}
	 	else if (player.getClan() == null)
	 	{
	 		player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
	 		return;
	 	}
	 	else if (!player.isClanLeader())
	 	{
	 		player.sendPacket(SystemMessageId.NOT_AUTHORIZED_TO_BESTOW_RIGHTS);
	 		return;
	 	}
	 	else if (player.getClan().getLevel() == 8)
	 	{
	 		player.sendMessage("Your clan is already level 8.");
	 	}
	 	else
	 	{
	 		player.getClan().changeLevel(player.getClan().getLevel() + 1);
	 		player.getClan().broadcastClanStatus();
	 		player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 1000, 0));
	 		player.destroyItemByItemId("Consume", Config.ITEMCLANLVL, Config.CANTIDADCLANLVL, player, true);
	 	}
	 }
	 	
	 public static void augments(Player player, int attributes, int idaugment, int levelaugment)
	 {
	 	ItemInstance rhand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	 	
	 	if (rhand == null)
	 	{
	 		player.sendMessage(player.getName() + " have to equip a weapon.");
	 		return;
	 		}
	 	else if (rhand.getItem().getCrystalType().getId() == 0 || rhand.getItem().getCrystalType().getId() == 1 || rhand.getItem().getCrystalType().getId() == 2)
	 	{
	 		player.sendMessage("You can't augment under " + rhand.getItem().getCrystalType() + " Grade Weapon!");
	 		return;
	 	}
	 	else if (rhand.isHeroItem())
	 	{
	 		player.sendMessage("You Cannot be add Augment On " + rhand.getItemName() + " !");
	 		return;
	 	}
	 	else if (!(player.getInventory().getInventoryItemCount(Config.ITEMARGU, 0) >= Config.CANTIDADARGU))
		{
			 final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
			 html.setFile("data/html/mods/donationmanager/noitems.htm");
			 player.sendPacket(html);
			 return;
		}
	 	if (!rhand.isAugmented())
	 	{
	 		player.sendMessage("Successfully To Add " + SkillTable.getInstance().getInfo(idaugment, levelaugment).getName() + ".");
	 		augmentweapondatabase(player, attributes, idaugment, levelaugment);
	 	}
	 	else
	 	{
	 		player.sendMessage("You Have Augment on weapon!");
	 		return;
	 	}
	 }
	 	
	 public static void augmentweapondatabase(Player player, int attributes, int id, int level)
	 {
	 	ItemInstance item = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	 	L2Augmentation augmentation = new L2Augmentation(attributes, id, level);
	 	augmentation.applyBonus(player);
	 	player.destroyItemByItemId("Consume", Config.ITEMARGU, Config.CANTIDADARGU, player, true);
	 	item.setAugmentation(augmentation);
	 	disarm(player);
	 	
	 	try (Connection con = L2DatabaseFactory.getInstance().getConnection())
	 	{
	 		@SuppressWarnings("resource")
			PreparedStatement statement = con.prepareStatement("REPLACE INTO augmentations VALUES(?,?,?,?)");
	 		statement.setInt(1, item.getObjectId());
	 		statement.setInt(2, attributes);
	 		statement.setInt(3, id);
	 		statement.setInt(4, level);
	 		InventoryUpdate iu = new InventoryUpdate();
	 		player.sendPacket(iu);
	 		statement.execute();
	 		statement.close();
	 	}
	 	catch (SQLException e)
	 	{
	 		System.out.println(e);
	 	}
	 }
	 
	 public static void Enchant(Player player, int enchant, int type)
	 {
	 	ItemInstance item = player.getInventory().getPaperdollItem(type);
	 	
	 	if (player.getInventory().getInventoryItemCount(57, 0) >= 10)
	 	{
	 		if (item == null)
	 		{
	 			player.sendMessage("That item doesn't exist in your inventory.");
	 			return;
	 		}
	 		else if (item.getEnchantLevel() == 20)
	 		{
	 			player.sendMessage("Your " + item.getItemName() + " is already on maximun enchant!");
	 			return;
	 		}
	 		else if (item.getItem().getCrystalType().getId() == 0 || item.getItem().getCrystalType().getId() == 1 || item.getItem().getCrystalType().getId() == 2)
	 		{
	 			player.sendMessage("You can't Enchant under " + item.getItem().getCrystalType() + " Grade Weapon!");
	 			return;
	 		}
	 		else if (item.isHeroItem())
	 		{
	 			player.sendMessage("You Cannot be Enchant On " + item.getItemName() + " !");
	 			return;
	 		}
	 		
	 		if (item.isEquipped())
	 		{
	 			item.setEnchantLevel(enchant);
	 			item.updateDatabase();
	 			player.sendPacket(new ItemList(player, false));
	 			player.broadcastUserInfo();
	 			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
	 		}
	 		if (!player.destroyItemByItemId("items", 57, 10, player, false))
	 			return;
	 	}
	 	else
	 	{
	 		player.sendMessage("You do not have the neccessary Items.");
	 	}
	 }
	 	
	 public static void disarm(Player player)
	 {
	 	// Unequip the weapon
	 	ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	 	if (wpn != null)
	 	{
	 		ItemInstance[] unequipped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
	 		InventoryUpdate iu = new InventoryUpdate();
	 		for (ItemInstance itm : unequipped)
	 			iu.addModifiedItem(itm);
	 		player.sendPacket(iu);
	 		
	 		player.abortAttack();
	 		player.broadcastUserInfo();
	 	}
	 }
	 
	 /**
	  * @param player
	  */
	 public static void removeAugmentation(Player player)
	 {
	 	ItemInstance item = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	 	
	 	if (item == null)
	 	{
	 		player.sendMessage("Equipped first a weapon!");
	 		return;
	 	}
	 	
	 	if (!item.isAugmented())
 	{
	 		player.sendMessage("The weapon is not augmented.");
	 		return;
	 	}
	 	
	 	item.getAugmentation().removeBonus(player);
	 	item.removeAugmentation();
	 	{
	 		player.sendMessage("Your augmented has been removed!");
	 		// Unequip the weapon
	 		ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	 		ItemInstance[] unequipped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
	 		InventoryUpdate iu = new InventoryUpdate();
	 		for (ItemInstance itm : unequipped)
	 		iu.addModifiedItem(itm);
	 		player.sendPacket(iu);
	 		
	 		player.abortAttack();
	 		player.broadcastUserInfo();
	 	}
	 }
	 	
	 public static void ClanSkill(Player player)
	 {
	 	
	 	if (!player.isClanLeader())
	 	{
	 		player.sendMessage("Only a clan leader can add clan skills.!");
	 		return;
	 	}
	 	else if (!(player.getInventory().getInventoryItemCount(Config.ITEMCLANSKILL, 0) >= Config.CANTIDADCLANSKILL))
		{
			 final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
			 html.setFile("data/html/mods/donationmanager/noitems.htm");
			 player.sendPacket(html);
			 return;
		}
	 	else
	 	{
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(370, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(371, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(372, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(373, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(374, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(375, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(376, 3));
			player.getClan().addNewSkill(SkillTable.getInstance().getInfo(377, 3));
			player.getClan().addNewSkill(SkillTable.getInstance().getInfo(378, 3));
			player.getClan().addNewSkill(SkillTable.getInstance().getInfo(379, 3));
			player.getClan().addNewSkill(SkillTable.getInstance().getInfo(380, 3));
			player.getClan().addNewSkill(SkillTable.getInstance().getInfo(381, 3));
			player.getClan().addNewSkill(SkillTable.getInstance().getInfo(382, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(383, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(384, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(385, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(386, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(387, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(388, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(389, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(390, 3));
	 		player.getClan().addNewSkill(SkillTable.getInstance().getInfo(391, 1));
	 		player.destroyItemByItemId("Consume", Config.ITEMCLANSKILL, Config.CANTIDADCLANSKILL, player, true);
	 		}
	 	}
	 
	  public static void setSkins(Player player)
	  {
		  player.destroyItemByItemId("Consume", Config.ITEMSKIN, Config.CANTIDADSKIN, player, true);
		  final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
		  html.setFile("data/html/mods/donationmanager/okskin.htm");
		  player.sendPacket(html);
		  player.broadcastUserInfo();
	  }	
	  
	  public static void setSexos(Player player)
	  {
		  player.destroyItemByItemId("Consume", Config.ITEMSEXO, Config.CANTIDADSEXO, player, true);
		  final NpcHtmlMessage html = new NpcHtmlMessage(1);		  			
		  html.setFile("data/html/mods/donationmanager/oksexo.htm");
		  player.sendPacket(html);
		  player.broadcastUserInfo();
	  }	

	  @Override
	  public String getHtmlPath(int npcId, int val)
	  {
		return "data/html/mods/donationmanager/Services.htm";
	  }

	  private static void setRaceCustomSkin(Player player, Races valor)
	  {
		 if ((player == null) || (valor == null)) 
		 {
		  	 return;
		 }
		 player.setCustomRaceSkin(valor.getRaceId());
		 player.setCustomClassSkin(valor.getClassId());
	}
}