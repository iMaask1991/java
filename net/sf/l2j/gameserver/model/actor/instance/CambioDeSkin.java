package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class CambioDeSkin extends Folk
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


	public CambioDeSkin(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player == null)
			return;
		
		if (command.startsWith("itemskins"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/mods/ItemSkin/Skins.htm");
			player.sendPacket(html);
		}
		else if (command.startsWith("next"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/mods/ItemSkin/Skins-1.htm");
			player.sendPacket(html);
		}
	}
	
	public static void Humano(Player player)
	{
	Races races = null;
	races = Races.FIGHTER;
	setRaceCustomSkin(player, races);
	}
	
	 public static void HumanoMago(Player player)
	 {
	 Races races = null;
	 races = Races.MAGE;
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void Elfo(Player player)
	 {
	 Races races = null;
	 races = Races.ELVENFIGHTER;
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void ElfoOscuro(Player player)
	 {
	 Races races = null;
	 races = Races.DARKFIGHTER;
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void Orco(Player player)
	 {
	 Races races = null;
	 races = Races.ORCFIGHTER; 
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void OrcoMago(Player player)
	 {
	 Races races = null;
	 races = Races.ORCMAGE;
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void Enano(Player player)
	 {
	 Races races = null;
	 races = Races.DWARVEN;
	 setRaceCustomSkin(player, races);
	 }
	 
	 public static void CambiarSexo(Player player)
	 {
	  if (player.getAppearance().getSex() == Sex.MALE)
	  {
	   player.getAppearance().setSex(Sex.FEMALE);
	  }
	  else
	  {
	  player.getAppearance().setSex(Sex.MALE);
	  }
	  player.broadcastUserInfo();
	  player.broadcastCharInfo();
	  }

	  private static void setRaceCustomSkin(Player player, Races valor)
	  {
		 if ((player == null) || (valor == null)) 
		 {
		  	 return;
		 }
		 player.setCustomRaceSkin(valor.getRaceId());
		 player.setCustomClassSkin(valor.getClassId());
		 player.broadcastUserInfo();
		 player.broadcastCharInfo();
	}
	}