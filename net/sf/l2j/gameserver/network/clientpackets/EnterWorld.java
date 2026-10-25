package net.sf.l2j.gameserver.network.clientpackets;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;

import Dolls.DollsData;
import hwid.Hwid;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.Manager.MailBBSManager;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.BotsPreventionManager;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.manager.CoupleManager;
import net.sf.l2j.gameserver.data.manager.DimensionalRiftManager;
import net.sf.l2j.gameserver.data.manager.IPManager;
import net.sf.l2j.gameserver.data.manager.PetitionManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.AgathionSkillData;
import net.sf.l2j.gameserver.data.xml.AnnouncementData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAio;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHero;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminVip;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.SubPledge;
import net.sf.l2j.gameserver.network.GameClient.GameClientState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Die;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExMailArrived;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.PledgeStatusChanged;
import net.sf.l2j.gameserver.network.serverpackets.QuestList;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.partyfarm.PartyFarm;
import net.sf.l2j.gameserver.pvpfarm.PvpFarm;
import net.sf.l2j.gameserver.rebirth.RebirthManager;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.taskmanager.AfkTaskManager;
import net.sf.l2j.gameserver.taskmanager.AutoGoldBar;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.tournament.ArenaTask;

public class EnterWorld extends L2GameClientPacket
{
	private static final String LOAD_PLAYER_QUESTS = "SELECT name,var,value FROM character_quests WHERE charId=?";
	private static final String LOGS_DIRECTORY = "./config/LogsDiarios/";
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
		{
			getClient().closeNow();
			return;
		}
		
		getClient().setState(GameClientState.IN_GAME);
		
		final int objectId = player.getObjectId();
		
		if (player.isGM())
		{
			if (Config.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", player.getAccessLevel()))
				player.setIsInvul(true);
			
			if (Config.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_hide", player.getAccessLevel()))
				player.getAppearance().setInvisible();
			
			if (Config.GM_STARTUP_SILENCE && AdminData.getInstance().hasAccess("admin_silence", player.getAccessLevel()))
				player.setInRefusalMode(true);
			
			if (Config.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmlist", player.getAccessLevel()))
				AdminData.getInstance().addGm(player, false);
			else
				AdminData.getInstance().addGm(player, true);
		}

		if (Config.MULTIBOX_PROTECTION_ENABLED)
		{
			IPManager.getInstance().validBox(player, Integer.valueOf(Config.MULTIBOX_PROTECTION_CLIENTS_PER_PC), World.getInstance().getPlayers(), Boolean.valueOf(true));
		}
		
		// Set dead status if applies
		if (player.getCurrentHp() < 0.5 && player.isMortal())
			player.setIsDead(true);
		
		// Clan checks.
		final Clan clan = player.getClan();
		if (clan != null)
		{
			player.sendPacket(new PledgeSkillList(clan));
			
			// Refresh player instance.
			clan.getClanMember(objectId).setPlayerInstance(player);
			
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addCharName(player);
			final PledgeShowMemberListUpdate update = new PledgeShowMemberListUpdate(player);
			
			// Send packets to others members.
			for (Player member : clan.getOnlineMembers())
			{
				if (member == player)
					continue;
				
				member.sendPacket(msg);
				member.sendPacket(update);
			}
			
			// Send a login notification to sponsor or apprentice, if logged.
			if (player.getSponsor() != 0)
			{
				final Player sponsor = World.getInstance().getPlayer(player.getSponsor());
				if (sponsor != null)
					sponsor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addCharName(player));
			}
			else if (player.getApprentice() != 0)
			{
				final Player apprentice = World.getInstance().getPlayer(player.getApprentice());
				if (apprentice != null)
					apprentice.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addCharName(player));
			}
			
			// Add message at connexion if clanHall not paid.
			final ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (clanHall != null && !clanHall.getPaid())
				player.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
			
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final Siege siege = castle.getSiege();
				if (!siege.isInProgress())
					continue;
				
				final SiegeSide type = siege.getSide(clan);
				if (type == SiegeSide.ATTACKER)
					player.setSiegeState((byte) 1);
				else if (type == SiegeSide.DEFENDER || type == SiegeSide.OWNER)
					player.setSiegeState((byte) 2);
			}
			
			player.sendPacket(new PledgeShowMemberListAll(clan, 0));
			
			for (SubPledge sp : clan.getAllSubPledges())
				player.sendPacket(new PledgeShowMemberListAll(clan, sp.getId()));
			
			player.sendPacket(new UserInfo(player));
			player.sendPacket(new PledgeStatusChanged(clan));
		}
		
		// Updating Seal of Strife Buff/Debuff
		if (SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) != CabalType.NORMAL)
		{
			CabalType cabal = SevenSignsManager.getInstance().getPlayerCabal(objectId);
			if (cabal != CabalType.NORMAL)
			{
				if (cabal == SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE))
					player.addSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill(), false);
				else
					player.addSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill(), false);
			}
		}
		else
		{
			player.removeSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill().getId(), false);
			player.removeSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill().getId(), false);
		}
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
			player.setSpawnProtection(true);
		
		player.spawnMe();

		// Engage and notify partner.
		if (Config.ALLOW_WEDDING)
		{
			for (Entry<Integer, IntIntHolder> coupleEntry : CoupleManager.getInstance().getCouples().entrySet())
			{
				final IntIntHolder couple = coupleEntry.getValue();
				if (couple.getId() == objectId || couple.getValue() == objectId)
				{
					player.setCoupleId(coupleEntry.getKey());
					break;
				}
			}
		}
		
		if (Config.BPS_BOTS_PREVENTION)
		{
			BotsPreventionManager.getInstance().onLogin(player);
		}
		
		AgathionSkillData.getInstance().removeAll(player);
	       
		// Announcements, welcome & Seven signs period messages
		player.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		player.loadAutoFarmSettings();
		
		player.sendPacket(SevenSignsManager.getInstance().getCurrentPeriod().getMessageId());
		AnnouncementData.getInstance().showAnnouncements(player, false);

		if (ArenaTask.is_started() && Config.ARENA_MESSAGE_ENABLED)
			player.sendPacket(new ExShowScreenMessage(Config.ARENA_MESSAGE_TEXT, Config.ARENA_MESSAGE_TIME, 2, true));
		
		if (Config.PARTY_MESSAGE_ENABLED && PartyFarm.is_started())
			player.sendPacket(new ExShowScreenMessage(Config.PARTY_FARM_MESSAGE_TEXT, Config.PARTY_FARM_MESSAGE_TIME));
		
		if (Config.PVPFARM_MESSAGE_ENABLED && PvpFarm.is_started())
			player.sendPacket(new ExShowScreenMessage(Config.PVPFARM_MESSAGE_TEXT, Config.PVPFARM_MESSAGE_TIME));
		
		// if player is DE, check for shadow sense skill at night
		if (player.getRace() == ClassRace.DARK_ELF && player.hasSkill(L2Skill.SKILL_SHADOW_SENSE))
			player.sendPacket(SystemMessage.getSystemMessage((GameTimeTaskManager.getInstance().isNight()) ? SystemMessageId.NIGHT_S1_EFFECT_APPLIES : SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(L2Skill.SKILL_SHADOW_SENSE));
		
		DollsData.refreshAllDollSkills(player);
		player.getMacroList().sendUpdate();
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new HennaInfo(player));
		player.sendPacket(new FriendList(player));
		// activeChar.queryGameGuard();
		player.sendPacket(new ItemList(player, false));
		player.sendPacket(new ShortCutInit(player));
		player.sendPacket(new ExStorageMaxCount(player));
		
		// no broadcast needed since the player will already spawn dead to others
		if (player.isAlikeDead())
			player.sendPacket(new Die(player));
		
		player.updateEffectIcons();
		player.sendPacket(new EtcStatusUpdate(player));
		player.sendSkillList();
		
		// Load quests.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_PLAYER_QUESTS))
		{
			ps.setInt(1, objectId);
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final String questName = rs.getString("name");
					
					// Test quest existence.
					final Quest quest = ScriptData.getInstance().getQuest(questName);
					if (quest == null)
					{
						LOGGER.warn("Unknown quest {} for player {}.", questName, player.getName());
						continue;
					}
					
					// Each quest get a single state ; create one QuestState per found <state> variable.
					final String var = rs.getString("var");
					if (var.equals("<state>"))
					{
						new QuestState(player, quest, rs.getByte("value"));
						
						// Notify quest for enterworld event, if quest allows it.
						if (quest.getOnEnterWorld())
							quest.notifyEnterWorld(player);
					}
					// Feed an existing quest state.
					else
					{
						final QuestState qs = player.getQuestState(questName);
						if (qs == null)
						{
							LOGGER.warn("Unknown quest state {} for player {}.", questName, player.getName());
							continue;
						}
						
						qs.setInternal(var, rs.getString("value"));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load quests for player {}.", e, player.getName());
		}
		
		player.sendPacket(new QuestList(player));

		if (player.isAio())
			onEnterAio(player);
		
		// Migra nick viejo (*Aio-*) / aplica prefijo properties si sigue AIO.
		if (player.isAio())
			AdminAio.applyAioName(player);
			                  
		if (Config.ALLOW_AIO_NCOLOR && player.isAio())
			player.getAppearance().setNameColor(Config.AIO_NCOLOR);
			                           
		if (Config.ALLOW_AIO_TCOLOR && player.isAio())
			player.getAppearance().setTitleColor(Config.AIO_TCOLOR);
			
		if (player.isVip())
			onEnterVip(player);
		
		if (player.isHero())
			onEnterHero(player);
		
		// Unread mails make a popup appears.
		if (Config.ENABLE_COMMUNITY_BOARD && MailBBSManager.getInstance().checkUnreadMail(player) > 0)
		{
			player.sendPacket(SystemMessageId.NEW_MAIL);
			player.sendPacket(new PlaySound("systemmsg_e.1233"));
			player.sendPacket(ExMailArrived.STATIC_PACKET);
		}
		
		// Clan notice, if active.
		if (Config.ENABLE_COMMUNITY_BOARD && clan != null && clan.isNoticeEnabled())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/clan_notice.htm");
			html.replace("%clan_name%", clan.getName());
			html.replace("%notice_text%", clan.getNotice().replaceAll("\r\n", "<br>").replaceAll("action", "").replaceAll("bypass", ""));
			sendPacket(html);
		}
		else if (Config.SERVER_NEWS)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/servnews.htm");
			sendPacket(html);
		}
		
		PetitionManager.getInstance().checkPetitionMessages(player);
		AfkTaskManager.getInstance().add(player);
		
		player.onPlayerEnter();
		if (player.getMemos().getBool("autoGb", false))
		{
			player.setAutoGb(true);
			AutoGoldBar.getInstance().add(player);
		}
		// Means that it's not ok multiBox situation, so logout
		Hwid.enterlog(player, getClient());
		// Event Gear: si se cayo el server con el jugador en un evento, se le devuelve su equipo.
		net.sf.l2j.gameserver.events.eventgear.EventGearManager.onEnterWorld(player);
		TvTEvent.onLogin(player);
		sendPacket(new SkillCoolTime(player));
		
//		CommunityBoard.getInstance().handleCommands(getClient(), Config.BBS_DEFAULT);
		
	        // Obtener la fecha actual
	        Date fechaActual = new Date();
	        SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        String fechaActualStr = formatoFecha.format(fechaActual);
	
	        // Nombre del archivo de log con la fecha actual
	        String nombreArchivo = LOGS_DIRECTORY + fechaActualStr.substring(0, 10) + ".txt";
	
	        // Crear el archivo de log (client/hwid/ip pueden ser null en algunos enters)
	        try {
	        	String hd1 = "N/A";
	        	String ip = "N/A";
	        	if (player.getClient() != null)
	        	{
	        		if (player.getClient().getHWID() != null)
	        			hd1 = player.getClient().getHWID();
	        		if (player.getClient().getConnection() != null && player.getClient().getConnection().getInetAddress() != null)
	        			ip = player.getClient().getConnection().getInetAddress().getHostAddress();
	        	}
	            PrintWriter escritor = new PrintWriter(new FileWriter(nombreArchivo, true));
	            escritor.println("| Login: " + fechaActualStr + " | Cuenta: " + player.getAccountName() + " | Personaje: " + player.getName() + " | IP: " + ip + " | HWID: " + hd1);
	            escritor.close();
	        } catch (IOException e) {
	            System.err.println("Error al crear el archivo de log, verifique si existe la carpeta " + e.getMessage());
	        }
		
		// If player logs back in a stadium, port him in nearest town.
		if (Olympiad.getInstance().playerInStadia(player))
			player.teleportTo(TeleportType.TOWN);
		
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), false))
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
		
		if (player.getClanJoinExpiryTime() > System.currentTimeMillis())
			player.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		
		// Attacker or spectator logging into a siege zone will be ported at town.
		if (!player.isGM() && (!player.isInSiege() || player.getSiegeState() < 2) && player.isInsideZone(ZoneId.SIEGE))
			player.teleportTo(TeleportType.TOWN);
		
		if (Config.ALLOW_VIP_TCOLOR && player.isVip() && !(Config.ALLOW_AIO_TCOLOR && player.isAio()))
			player.getAppearance().setTitleColor(Config.VIP_TCOLOR);
		else if (Config.ALLOW_AIO_TCOLOR && player.isAio())
			player.getAppearance().setTitleColor(Config.AIO_TCOLOR);
		
		// Rebirth: limpia leftovers de Poderes/bonos (el sistema de stats del CB queda OFF).
		if (RebirthManager.ENABLED)
			RebirthManager.refreshBonuses(player);
		
		player.restoreOfflineNameColor();
		player.broadcastUserInfo();
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		// Anti interface: desafio a la interface del server (ver AntiInterface.properties).
		net.sf.l2j.gameserver.antiinterface.AntiInterfaceManager.onEnterWorld(player);
		
		// Barra VIP de la interface (StatusWnd.VIPBar)
		net.sf.l2j.gameserver.vip.VipBar.onEnterWorld(player);
	}

	 private static void onEnterVip(Player activeChar)
	 {
	       long now = Calendar.getInstance().getTimeInMillis();
	       long endDay = activeChar.getVipEndTime();
	       if (now > endDay)
	            new AdminVip().removeVip(null, activeChar, "");
	    }
	 
	 private static void onEnterHero(Player activeChar)
	 {
		 long now = Calendar.getInstance().getTimeInMillis();
		 long endDay = activeChar.getHeroEndTime();
		 // hero_end=0 = permanente (legacy / NPC) o Hero Oly sin custom timed
		 if (endDay > 0 && now > endDay)
			 new AdminHero().removeHero(null, activeChar, "");
	 }
	 
   private static void onEnterAio(Player activeChar)
   {
       long now = Calendar.getInstance().getTimeInMillis();
       long endDay = activeChar.getAioEndTime();
       // aio_end=0 = permanente; solo vence si hay fecha y ya pasó
       if (endDay > 0 && now > endDay)
           new AdminAio().removeAio(null, activeChar, "");
   }
   
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}