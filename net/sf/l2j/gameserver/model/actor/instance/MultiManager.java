package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Bypass router for DonationManager NPC services:
 * Skin / Sex / Augment / Color Name / Color Title.
 */
public class MultiManager
{
	public static final MultiManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public boolean Skin(String command, Player player)
	{
		if (!command.startsWith("skin"))
			return true;
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		try
		{
			final String type = st.nextToken();
			switch (type)
			{
				case "Human":
					DonationManager.Human(player);
					break;
				case "HumanMage":
					DonationManager.HumanMage(player);
					break;
				case "Elf":
					DonationManager.Elf(player);
					break;
				case "DarkElf":
					DonationManager.DarkElf(player);
					break;
				case "Orc":
					DonationManager.Orc(player);
					break;
				case "OrcMage":
					DonationManager.OrcMage(player);
					break;
				case "Dwarf":
					DonationManager.Dwarf(player);
					break;
				case "ChangeSex":
					DonationManager.ChangeSex(player);
					break;
			}
		}
		catch (Exception e)
		{
		}
		return true;
	}
	
	public boolean ActiveSkill(String command, Player player)
	{
		if (!command.startsWith("active"))
			return true;
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		try
		{
			final String type = st.nextToken();
			switch (type)
			{
				case "Might":
					DonationManager.augments(player, 1062079106, 3132, 10);
					break;
				case "Empower":
					DonationManager.augments(player, 1061423766, 3133, 10);
					break;
				case "DuelMight":
					DonationManager.augments(player, 1062406807, 3134, 10);
					break;
				case "Shield":
					DonationManager.augments(player, 968884225, 3135, 10);
					break;
				case "MagicBarrier":
					DonationManager.augments(player, 956760065, 3136, 10);
					break;
				case "WildMagic":
					DonationManager.augments(player, 1067850844, 3142, 10);
					break;
				case "Focus":
					DonationManager.augments(player, 1067523168, 3141, 10);
					break;
				case "BattleRoad":
					DonationManager.augments(player, 968228865, 3125, 10);
					break;
				case "BlessedBody":
					DonationManager.augments(player, 991625216, 3124, 10);
					break;
				case "Agility":
					DonationManager.augments(player, 1060444351, 3139, 10);
					break;
				case "Heal":
					DonationManager.augments(player, 1061361888, 3123, 10);
					break;
				case "HydroBlast":
					DonationManager.augments(player, 1063590051, 3167, 10);
					break;
				case "AuraFlare":
					DonationManager.augments(player, 1063455338, 3172, 10);
					break;
				case "Hurricane":
					DonationManager.augments(player, 1064108032, 3168, 10);
					break;
				case "ReflectDamage":
					DonationManager.augments(player, 1067588698, 3204, 3);
					break;
				case "Celestial":
					DonationManager.augments(player, 974454785, 3158, 1);
					break;
				case "Stone":
					DonationManager.augments(player, 1060640984, 3169, 10);
					break;
				case "HealEmpower":
					DonationManager.augments(player, 1061230760, 3138, 10);
					break;
				case "ShadowFlare":
					DonationManager.augments(player, 1063520931, 3171, 10);
					break;
				case "Prominence":
					DonationManager.augments(player, 1063327898, 3165, 10);
					break;
				case "SolarFlare":
					DonationManager.augments(player, 1063327898, 3165, 10);
					break;
				case "Guidance":
					DonationManager.augments(player, 1070537767, 3140, 10);
					break;
			}
		}
		catch (Exception e)
		{
			player.sendMessage("Invalid augment option.");
		}
		return true;
	}
	
	public boolean PassiveSkill(String command, Player player)
	{
		if (!command.startsWith("passive"))
			return true;
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		try
		{
			final String type = st.nextToken();
			switch (type)
			{
				case "DuelMight":
					DonationManager.augments(player, 1067260101, 3243, 10);
					break;
				case "Might":
					DonationManager.augments(player, 1067125363, 3240, 10);
					break;
				case "Shield":
					DonationManager.augments(player, 1067194549, 3244, 10);
					break;
				case "MagicBarrier":
					DonationManager.augments(player, 962068481, 3245, 10);
					break;
				case "Empower":
					DonationManager.augments(player, 1066994296, 3241, 10);
					break;
				case "Agility":
					DonationManager.augments(player, 965279745, 3247, 10);
					break;
				case "Guidance":
					DonationManager.augments(player, 1070537767, 3248, 10);
					break;
				case "Focus":
					DonationManager.augments(player, 1070406728, 3249, 10);
					break;
				case "WildMagic":
					DonationManager.augments(player, 1070599653, 3250, 10);
					break;
				case "ReflectDamage":
					DonationManager.augments(player, 1070472227, 3259, 3);
					break;
				case "HealEmpower":
					DonationManager.augments(player, 1066866909, 3246, 10);
					break;
				case "Prayer":
					DonationManager.augments(player, 1066932422, 3238, 10);
					break;
				case "Clarity":
					DonationManager.augments(player, 1066866909, 3246, 10);
					break;
			}
		}
		catch (Exception e)
		{
			player.sendMessage("Invalid augment option.");
		}
		return true;
	}
	
	/** NPC 60004 (Donate Shop): su pagina principal es merchant/60004.htm. */
	private static final int DONATE_SHOP_NPC_ID = 60004;
	private static final int DONATE_SHOP_PAGE = 0;
	
	public void Page(String command, Player player)
	{
		if (!command.startsWith("services"))
			return;
		
		// Talking with the all-in-one GM Shop (60000): go back to its own donation page (60000-2.htm)
		// so every "back" button of the donation windows returns to the GM Shop, not to the old Donation Manager page.
		final Folk folk = player.getCurrentFolk();
		if (folk instanceof GmShopNpc && folk.canInteract(player))
		{
			folk.showChatWindow(player, GmShopNpc.DONATION_PAGE);
			return;
		}
		
		// Talking with the Donate Shop (60004): go back to its main page (60004.htm).
		if (folk != null && folk.getNpcId() == DONATE_SHOP_NPC_ID && folk.canInteract(player))
		{
			folk.showChatWindow(player, DONATE_SHOP_PAGE);
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/mods/donationmanager/Services.htm");
		html.replace("%skin%", String.valueOf(Config.CANTIDADSKIN));
		html.replace("%argu%", String.valueOf(Config.CANTIDADARGU));
		html.replace("%hero%", String.valueOf(Config.CANTIDAD_HERO_30D));
		html.replace("%nobles%", String.valueOf(Config.CANTIDADNOBLES));
		html.replace("%combo%", String.valueOf(Config.CANTIDAD_COMBO_30D));
		player.sendPacket(html);
	}
	
	public void showNameColorManager(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/mods/donationmanager/NameColor.htm");
		html.replace("%name%", String.valueOf(Config.CANTIDADNAMECOLOR));
		player.sendPacket(html);
	}
	
	public void showTitleColorManager(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/mods/donationmanager/TitleColor.htm");
		html.replace("%title%", String.valueOf(Config.CANTIDADTITLECOLOR));
		player.sendPacket(html);
	}
	
	public void showDonaciones(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/mods/donationmanager/Donaciones.htm");
		player.sendPacket(html);
	}
	
	public void NameColor(String command, Player player)
	{
		if (!command.startsWith("namecolor"))
			return;
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		String newColor = "";
		try
		{
			if (st.hasMoreTokens())
				newColor = st.nextToken().toUpperCase();
		}
		catch (Exception e)
		{
			player.sendMessage("Elija un color valido.");
			return;
		}
		
		if (!isValidHex(newColor, player))
			return;
		
		try
		{
			final int color = Integer.decode("0x" + invertHexColorOrder(newColor));
			if (!player.destroyItemByItemId("DonateColorName", Config.ITEMNAMECOLOR, Config.CANTIDADNAMECOLOR, player, true))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/mods/donationmanager/noitems.htm");
				player.sendPacket(html);
				return;
			}
			
			player.getAppearance().setNameColor(color);
			player.store();
			player.broadcastUserInfo();
			player.sendMessage("Tu nuevo color de nombre es: " + newColor);
			showNameColorManager(player);
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("El color proporcionado no es valido.");
		}
	}
	
	public void TitleColor(String command, Player player)
	{
		if (!command.startsWith("titlecolor"))
			return;
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		String newColor = "";
		try
		{
			if (st.hasMoreTokens())
				newColor = st.nextToken().toUpperCase();
		}
		catch (Exception e)
		{
			player.sendMessage("Elija un color valido.");
			return;
		}
		
		if (!isValidHex(newColor, player))
			return;
		
		try
		{
			final int color = Integer.decode("0x" + invertHexColorOrder(newColor));
			if (!player.destroyItemByItemId("DonateColorTitle", Config.ITEMTITLECOLOR, Config.CANTIDADTITLECOLOR, player, true))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/mods/donationmanager/noitems.htm");
				player.sendPacket(html);
				return;
			}
			
			player.getAppearance().setTitleColor(color);
			player.store();
			player.broadcastUserInfo();
			player.sendMessage("Tu nuevo color de title es: " + newColor);
			showTitleColorManager(player);
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("El color proporcionado no es valido.");
		}
	}
	
	public boolean trySetNameColor(Player player, String newColor)
	{
		if (newColor != null)
			newColor = newColor.toUpperCase();
		
		if (!isValidHex(newColor, player))
			return false;
		
		try
		{
			final int color = Integer.decode("0x" + invertHexColorOrder(newColor));
			if (!player.destroyItemByItemId("DonateColorName", Config.ITEMNAMECOLOR, Config.CANTIDADNAMECOLOR, player, true))
			{
				player.sendMessage("Not enough Donation Coin.");
				return false;
			}
			
			player.getAppearance().setNameColor(color);
			player.store();
			player.broadcastUserInfo();
			player.sendMessage("Tu nuevo color de nombre es: " + newColor);
			return true;
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("El color proporcionado no es valido.");
			return false;
		}
	}
	
	public boolean trySetTitleColor(Player player, String newColor)
	{
		if (newColor != null)
			newColor = newColor.toUpperCase();
		
		if (!isValidHex(newColor, player))
			return false;
		
		try
		{
			final int color = Integer.decode("0x" + invertHexColorOrder(newColor));
			if (!player.destroyItemByItemId("DonateColorTitle", Config.ITEMTITLECOLOR, Config.CANTIDADTITLECOLOR, player, true))
			{
				player.sendMessage("Not enough Donation Coin.");
				return false;
			}
			
			player.getAppearance().setTitleColor(color);
			player.store();
			player.broadcastUserInfo();
			player.sendMessage("Tu nuevo color de title es: " + newColor);
			return true;
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("El color proporcionado no es valido.");
			return false;
		}
	}
	
	private static boolean isValidHex(String newColor, Player player)
	{
		if (newColor == null || newColor.length() != 6 || !newColor.matches("[0-9A-Fa-f]{6}"))
		{
			player.sendMessage("El color debe ser HEX de 6 caracteres (ej: FF0000).");
			return false;
		}
		return true;
	}
	
	private static String invertHexColorOrder(String color)
	{
		return color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2);
	}
	
	private static class SingletonHolder
	{
		protected static final MultiManager _instance = new MultiManager();
	}
}
