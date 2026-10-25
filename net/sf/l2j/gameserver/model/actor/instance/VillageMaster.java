package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import Dolls.DollsData;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.data.xml.SkillTreeData;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.skills.AcquireSkillType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.SubClass;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.holder.skillnode.ClanSkillNode;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.model.pledge.SubPledge;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AcquireSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.scripting.QuestState;

/**
 * The generic villagemaster. Some childs instances depends of it for race/classe restriction.
 */
public class VillageMaster extends Folk
{
	public VillageMaster(int objectId, NpcTemplate template)
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
		
		return "data/html/villagemaster/" + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		String[] commandStr = command.split(" ");
		String actualCommand = commandStr[0];
		
		String cmdParams = "";
		String cmdParams2 = "";
		
		if (commandStr.length >= 2)
			cmdParams = commandStr[1];
		if (commandStr.length >= 3)
			cmdParams2 = commandStr[2];
		
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.isEmpty())
				return;
			
			ClanTable.getInstance().createClan(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("create_academy"))
		{
			if (cmdParams.isEmpty())
				return;
			
			createSubPledge(player, cmdParams, null, Clan.SUBUNIT_ACADEMY, 5);
		}
		else if (command.startsWith("1stClass"))
			showHtmlMenu(player, getObjectId(), 1);
		else if (command.startsWith("2ndClass"))
			showHtmlMenu(player, getObjectId(), 2);
		else if (command.startsWith("3rdClass"))
			showHtmlMenu(player, getObjectId(), 3);
		else if (command.startsWith("change_class"))
		{
			int val = Integer.parseInt(command.substring(13));
			
			if (checkAndChangeClass(player, val))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/classmaster/ok.htm");
				html.replace("%name%", PlayerData.getInstance().getClassNameById(val));
				player.sendPacket(html);
			}
		}
		else if (actualCommand.equalsIgnoreCase("rename_pledge"))
		{
			if (cmdParams.isEmpty() || cmdParams2.isEmpty())
				return;
			
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			final Clan clan = player.getClan();
			final SubPledge subPledge = player.getClan().getSubPledge(Integer.valueOf(cmdParams));
			
			if (subPledge == null)
			{
				player.sendMessage("Pledge doesn't exist.");
				return;
			}
			
			if (!StringUtil.isAlphaNumeric(cmdParams2))
			{
				player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
				return;
			}
			
			if (cmdParams2.length() < 2 || cmdParams2.length() > 16)
			{
				player.sendPacket(SystemMessageId.CLAN_NAME_LENGTH_INCORRECT);
				return;
			}
			
			subPledge.setName(cmdParams2);
			clan.updateSubPledgeInDB(subPledge);
			clan.broadcastToOnlineMembers(new PledgeShowMemberListAll(clan, subPledge.getId()));
			player.sendMessage("Pledge name have been changed to: " + cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_royal"))
		{
			if (cmdParams.isEmpty())
				return;
			
			createSubPledge(player, cmdParams, cmdParams2, Clan.SUBUNIT_ROYAL1, 6);
		}
		else if (actualCommand.equalsIgnoreCase("create_knight"))
		{
			if (cmdParams.isEmpty())
				return;
			
			createSubPledge(player, cmdParams, cmdParams2, Clan.SUBUNIT_KNIGHT1, 7);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if (cmdParams.isEmpty())
				return;
			
			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
				return;
			
			if (player.getClan() == null)
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			else
				player.getClan().createAlly(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			final Clan clan = player.getClan();
			if (clan.getAllyId() != 0)
			{
				player.sendPacket(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY);
				return;
			}
			
			if (clan.isAtWar())
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR);
				return;
			}
			
			if (clan.hasCastle() || clan.hasClanHall())
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE);
				return;
			}
			
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				if (castle.getSiege().checkSides(clan))
				{
					player.sendPacket((castle.getSiege().isInProgress()) ? SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE : SystemMessageId.CANNOT_DISSOLVE_CAUSE_CLAN_WILL_PARTICIPATE_IN_CASTLE_SIEGE);
					return;
				}
			}
			
			if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
			{
				player.sendPacket(SystemMessageId.DISSOLUTION_IN_PROGRESS);
				return;
			}
			
			if (Config.ALT_CLAN_DISSOLVE_DAYS > 0)
			{
				clan.setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L);
				clan.updateClanInDB();
				
				ClanTable.getInstance().scheduleRemoveClan(clan);
			}
			else
				ClanTable.getInstance().destroyClan(clan);
			
			// The clan leader should take the XP penalty of a full death.
			player.deathPenalty(false, false, false);
		}
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
				return;
			
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			if (player.getName().equalsIgnoreCase(cmdParams))
				return;
			
			final Clan clan = player.getClan();
			final ClanMember member = clan.getClanMember(cmdParams);
			
			if (member == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DOES_NOT_EXIST).addString(cmdParams));
				return;
			}
			
			if (!member.isOnline())
			{
				player.sendPacket(SystemMessageId.INVITED_USER_NOT_ONLINE);
				return;
			}
			
			if (member.getPledgeType() != 0)
			{
				player.sendMessage("Selected member cannot be found in main clan.");
				return;
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (clan.getNewLeaderId() == 0)
			{
				clan.setNewLeaderId(member.getObjectId(), true);
				html.setFile("data/html/scripts/village_master/Clan/9000-07-success.htm");
			}
			else
				html.setFile("data/html/scripts/village_master/Clan/9000-07-in-progress.htm");
			
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("cancel_clan_leader_change"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			final Clan clan = player.getClan();
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (clan.getNewLeaderId() != 0)
			{
				clan.setNewLeaderId(0, true);
				html.setFile("data/html/scripts/village_master/Clan/9000-08-success.htm");
			}
			else
				html.setFile("data/html/scripts/village_master/Clan/9000-08-no.htm");
			
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			final Clan clan = player.getClan();
			clan.setDissolvingExpiryTime(0);
			clan.updateClanInDB();
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (player.getClan().levelUpClan(player))
				player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 0, 0));
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else if (command.startsWith("Subclass"))
		{
			// Subclasses may not be changed while a skill is in use.
			if (player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
				return;
			}
			
			// Affecting subclasses (add/del/change) if registered in Olympiads makes you ineligible to compete.
			if (OlympiadManager.getInstance().isRegisteredInComp(player))
				OlympiadManager.getInstance().unRegisterNoble(player);
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			int cmdChoice = 0;
			int paramOne = 0;
			int paramTwo = 0;
			
			try
			{
				cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
				
				int endIndex = command.indexOf(' ', 11);
				if (endIndex == -1)
					endIndex = command.length();
				
				paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
				if (command.length() > endIndex)
					paramTwo = Integer.parseInt(command.substring(endIndex).trim());
			}
			catch (Exception NumberFormatException)
			{
			}
			
			StringBuilder sb;
			Set<ClassId> subsAvailable;
			
			switch (cmdChoice)
			{
				case 0: // Subclass change menu
					html.setFile("data/html/villagemaster/SubClass.htm");
					break;
				
				case 1: // Add Subclass - Initial
					// Subclasses may not be added while a summon is active.
					if (player.getSummon() != null)
					{
						player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
						return;
					}
					
					// Subclasses may not be added while you are over your weight limit.
					if (player.getInventoryLimit() * 0.8 <= player.getInventory().getSize() || player.getWeightPenalty() > 2)
					{
						player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
						return;
					}
					
					// Avoid giving player an option to add a new sub class, if they have three already.
					if (player.getSubClasses().size() >= Config.SUB_COUNT)
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
						break;
					}
					
					subsAvailable = getAvailableSubClasses(player);
					if (subsAvailable == null || subsAvailable.isEmpty())
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					
					sb = new StringBuilder(300);
					for (ClassId subClass : subsAvailable)
						StringUtil.append(sb, "<a action=\"bypass -h npc_%objectId%_Subclass 4 ", subClass.getId(), "\" msg=\"1268;", subClass, "\">", subClass, "</a><br>");
					
					html.setFile("data/html/villagemaster/SubClass_Add.htm");
					html.replace("%list%", sb.toString());
					break;
				
				case 2: // Change Class - Initial
					// Subclasses may not be changed while a summon is active.
					if (player.getSummon() != null)
					{
						player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
						return;
					}
					
					// Subclasses may not be changed while a you are over your weight limit.
					if (player.getInventoryLimit() * 0.8 <= player.getInventory().getSize() || player.getWeightPenalty() > 2)
					{
						player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
						return;
					}
					
					if (player.getSubClasses().isEmpty())
						html.setFile("data/html/villagemaster/SubClass_ChangeNo.htm");
					else
					{
						sb = new StringBuilder(300);
						
						if (checkVillageMaster(player.getBaseClass()))
							StringUtil.append(sb, "<a action=\"bypass -h npc_%objectId%_Subclass 5 0\">", PlayerData.getInstance().getClassNameById(player.getBaseClass()), "</a><br>");
						
						for (SubClass subclass : player.getSubClasses().values())
						{
							if (checkVillageMaster(subclass.getClassDefinition()))
								StringUtil.append(sb, "<a action=\"bypass -h npc_%objectId%_Subclass 5 ", subclass.getClassIndex(), "\">", subclass.getClassDefinition(), "</a><br>");
						}
						
						if (sb.length() > 0)
						{
							html.setFile("data/html/villagemaster/SubClass_Change.htm");
							html.replace("%list%", sb.toString());
						}
						else
							html.setFile("data/html/villagemaster/SubClass_ChangeNotFound.htm");
					}
					break;
				
				case 3: // Change/Cancel Subclass - Initial
					if (player.getSubClasses().isEmpty())
					{
						html.setFile("data/html/villagemaster/SubClass_ModifyEmpty.htm");
						break;
					}
					
					html.setFile("data/html/villagemaster/SubClass_Modify.htm");
					if (player.getSubClasses().containsKey(1))
						html.replace("%sub1%", PlayerData.getInstance().getClassNameById(player.getSubClasses().get(1).getClassId()));
					else
						html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 1\">%sub1%</a><br>", "");
					
					if (player.getSubClasses().containsKey(2))
						html.replace("%sub2%", PlayerData.getInstance().getClassNameById(player.getSubClasses().get(2).getClassId()));
					else
						html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 2\">%sub2%</a><br>", "");
					
					if (player.getSubClasses().containsKey(3))
						html.replace("%sub3%", PlayerData.getInstance().getClassNameById(player.getSubClasses().get(3).getClassId()));
					else
						html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 3\">%sub3%</a><br>", "");
					break;
				
				case 4: // Add Subclass - Action (Subclass 4 x[x])
					if (!FloodProtectors.performAction(player.getClient(), Action.SUBCLASS))
						return;
					
					boolean allowAddition = true;
					
					if (player.getSubClasses().size() >= Config.SUB_COUNT)
						allowAddition = false;
					
					if (player.getLevel() < 75)
						allowAddition = false;
					
					if (allowAddition)
					{
						for (SubClass subclass : player.getSubClasses().values())
						{
							if (subclass.getLevel() < 75)
							{
								allowAddition = false;
								break;
							}
						}
					}
					
					// Verify if the character has completed the Mimir's Elixir (Path to Subclass) and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items.
					if (allowAddition && !Config.SUBCLASS_WITHOUT_QUESTS)
						allowAddition = checkQuests(player);
					
					// If they both exist, remove both unique items and continue with adding the subclass.
					if (allowAddition && isValidNewSubClass(player, paramOne))
					{
						if (!player.addSubClass(paramOne, player.getSubClasses().size() + 1))
							return;
						
						player.setActiveClass(player.getSubClasses().size());
						DollsData.refreshAllDollSkills(player);
						
						html.setFile("data/html/villagemaster/SubClass_AddOk.htm");
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
						
						if (Config.VILLAGE_SUB_ANNOUNCE)
							announceVillageSub(player);
					}
					else
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					break;
				
				case 5: // Change Class - Action
					if (!FloodProtectors.performAction(player.getClient(), Action.SUBCLASS))
						return;
					
					if (player.getClassIndex() == paramOne)
					{
						html.setFile("data/html/villagemaster/SubClass_Current.htm");
						break;
					}
					
					if (paramOne == 0)
					{
						if (!checkVillageMaster(player.getBaseClass()))
							return;
					}
					else
					{
						try
						{
							if (!checkVillageMaster(player.getSubClasses().get(paramOne).getClassDefinition()))
								return;
						}
						catch (NullPointerException e)
						{
							return;
						}
					}
					
					player.setActiveClass(paramOne);
					
					player.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED); // Transfer completed.
					DollsData.refreshAllDollSkills(player);
					return;
				
				case 6: // Change/Cancel Subclass - Choice
					// validity check
					if (paramOne < 1 || paramOne > 3)
						return;
					
					subsAvailable = getAvailableSubClasses(player);
					
					// another validity check
					if (subsAvailable == null || subsAvailable.isEmpty())
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					
					sb = new StringBuilder(300);
					for (ClassId subClass : subsAvailable)
						StringUtil.append(sb, "<a action=\"bypass -h npc_%objectId%_Subclass 7 ", paramOne, " ", subClass.getId(), "\" msg=\"1445;", "\">", subClass, "</a><br>");
					
					switch (paramOne)
					{
						case 1:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice1.htm");
							break;
						
						case 2:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice2.htm");
							break;
						
						case 3:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice3.htm");
							break;
						
						default:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice.htm");
					}
					html.replace("%list%", sb.toString());
					break;
				
				case 7: // Change Subclass - Action
					if (!FloodProtectors.performAction(player.getClient(), Action.SUBCLASS))
						return;
					
					if (!isValidNewSubClass(player, paramTwo))
						return;
					
					if (Config.VILLAGE_CANCEL_SUB_ITEM_ID > 0 && Config.VILLAGE_CANCEL_SUB_COUNT > 0)
					{
						if (player.getInventory().getInventoryItemCount(Config.VILLAGE_CANCEL_SUB_ITEM_ID, 0) < Config.VILLAGE_CANCEL_SUB_COUNT)
						{
							player.sendMessage("No tienes los items necesarios para cancelar/cambiar la SubClass.");
							return;
						}
					}
					
					if (player.modifySubClass(paramOne, paramTwo))
					{
						if (Config.VILLAGE_CANCEL_SUB_ITEM_ID > 0 && Config.VILLAGE_CANCEL_SUB_COUNT > 0)
						{
							player.getInventory().destroyItemByItemId("VillageCancelSub", Config.VILLAGE_CANCEL_SUB_ITEM_ID, Config.VILLAGE_CANCEL_SUB_COUNT, player, this);
							player.getInventory().updateDatabase();
							player.sendPacket(new ItemList(player, true));
						}
						
						player.abortCast();
						player.stopAllEffectsExceptThoseThatLastThroughDeath(); // all effects from old subclass stopped!
						player.stopCubics();
						player.setActiveClass(paramOne);
						DollsData.refreshAllDollSkills(player);
						html.setFile("data/html/villagemaster/SubClass_ModifyOk.htm");
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
					}
					else
					{
						player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
						
						player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						return;
					}
					break;
			}
			
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else
		{
			// this class dont know any other commands, let forward the command to the parent class
			super.onBypassFeedback(player, command);
		}
	}
	
	protected boolean checkQuests(Player player)
	{
		// Noble players can add subbclasses without quests
		if (player.isNoble())
			return true;
		
		QuestState qs = player.getQuestState("Q234_FatesWhisper");
		if (qs == null || !qs.isCompleted())
			return false;
		
		qs = player.getQuestState("Q235_MimirsElixir");
		if (qs == null || !qs.isCompleted())
			return false;
		
		return true;
	}
	
	/** Anuncio 1ª/2ª/3ª sub (Village Master). */
	private static void announceVillageSub(Player player)
	{
		final int subNumber = player.getSubClasses().size();
		String format;
		switch (subNumber)
		{
			case 1:
				format = Config.CUSTOM_SUB_ANNOUNCE_TEXT_1;
				break;
			case 2:
				format = Config.CUSTOM_SUB_ANNOUNCE_TEXT_2;
				break;
			case 3:
				format = Config.CUSTOM_SUB_ANNOUNCE_TEXT_3;
				break;
			default:
				format = Config.CUSTOM_SUB_ANNOUNCE_TEXT;
				break;
		}
		if (format == null || format.isEmpty())
			format = Config.CUSTOM_SUB_ANNOUNCE_TEXT;
		
		World.announceToOnlinePlayers(format.replace("%playername%", player.getName()));
	}
	
	private final Set<ClassId> getAvailableSubClasses(Player player)
	{
		Set<ClassId> availSubs = ClassId.getAvailableSubclasses(player);
		
		if (availSubs != null && !availSubs.isEmpty())
		{
			for (Iterator<ClassId> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				ClassId classId = availSub.next();
				
				// check for the village master
				if (!checkVillageMaster(classId))
				{
					availSub.remove();
					continue;
				}
				
				// scan for already used subclasses
				for (SubClass subclass : player.getSubClasses().values())
				{
					if (subclass.getClassDefinition().equalsOrChildOf(classId))
					{
						availSub.remove();
						break;
					}
				}
			}
		}
		
		return availSubs;
	}
	
	/**
	 * Check subclass classId for validity (villagemaster race/type is not contains in previous subclasses, but in allowed subclasses). Base class not added into allowed subclasses.
	 * @param player : The player to check.
	 * @param classId : The class id to check.
	 * @return true if the {@link Player} can pick this subclass id.
	 */
	private final boolean isValidNewSubClass(Player player, int classId)
	{
		if (!checkVillageMaster(classId))
			return false;
		
		final ClassId cid = ClassId.VALUES[classId];
		for (SubClass subclass : player.getSubClasses().values())
		{
			if (subclass.getClassDefinition().equalsOrChildOf(cid))
				return false;
		}
		
		final Set<ClassId> availSubs = ClassId.getAvailableSubclasses(player);
		if (availSubs == null || availSubs.isEmpty())
			return false;
		
		boolean found = false;
		for (ClassId pclass : availSubs)
		{
			if (pclass.getId() == classId)
			{
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	protected boolean checkVillageMasterRace(ClassId pclass)
	{
		return true;
	}
	
	protected boolean checkVillageMasterTeachType(ClassId pclass)
	{
		return true;
	}
	
	public final boolean checkVillageMaster(int classId)
	{
		return checkVillageMaster(ClassId.VALUES[classId]);
	}
	
	public final boolean checkVillageMaster(ClassId pclass)
	{
		if (Config.ALT_GAME_SUBCLASS_EVERYWHERE)
			return true;
		
		return checkVillageMasterRace(pclass) && checkVillageMasterTeachType(pclass);
	}
	
	private static final void createSubPledge(Player player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == Clan.SUBUNIT_ACADEMY)
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY);
			else
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			
			return;
		}
		
		if (!StringUtil.isAlphaNumeric(clanName))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
			return;
		}
		
		if (clanName.length() < 2 || clanName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_LENGTH_INCORRECT);
			return;
		}
		
		for (Clan tempClan : ClanTable.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == Clan.SUBUNIT_ACADEMY)
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_EXISTS).addString(clanName));
				else
					player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME);
				
				return;
			}
		}
		
		if (pledgeType != Clan.SUBUNIT_ACADEMY)
		{
			if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
			{
				if (pledgeType >= Clan.SUBUNIT_KNIGHT1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
				else if (pledgeType >= Clan.SUBUNIT_ROYAL1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
				
				return;
			}
		}
		
		final int leaderId = pledgeType != Clan.SUBUNIT_ACADEMY ? clan.getClanMember(leaderName).getObjectId() : 0;
		if (clan.createSubPledge(player, pledgeType, leaderId, clanName) == null)
			return;
		
		SystemMessage sm;
		if (pledgeType == Clan.SUBUNIT_ACADEMY)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= Clan.SUBUNIT_KNIGHT1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= Clan.SUBUNIT_ROYAL1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
			sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_CREATED);
		player.sendPacket(sm);
		
		if (pledgeType != Clan.SUBUNIT_ACADEMY)
		{
			final ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			final Player leaderPlayer = leaderSubPledge.getPlayerInstance();
			if (leaderPlayer != null)
			{
				leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
				leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
			}
		}
	}
	
	private static final void assignSubPledgeLeader(Player player, String clanName, String leaderName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		if (leaderName.length() > 16)
		{
			player.sendPacket(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS);
			return;
		}
		
		if (player.getName().equals(leaderName))
		{
			player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}
		
		final Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(clanName);
		
		if (null == subPledge || subPledge.getId() == Clan.SUBUNIT_ACADEMY)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
			return;
		}
		
		final ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		
		if (leaderSubPledge == null || leaderSubPledge.getPledgeType() != 0)
		{
			if (subPledge.getId() >= Clan.SUBUNIT_KNIGHT1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			else if (subPledge.getId() >= Clan.SUBUNIT_ROYAL1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			
			return;
		}
		
		// Avoid naming sub pledges with the same captain
		if (clan.isSubPledgeLeader(leaderSubPledge.getObjectId()))
		{
			if (subPledge.getId() >= Clan.SUBUNIT_KNIGHT1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			else if (subPledge.getId() >= Clan.SUBUNIT_ROYAL1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			
			return;
		}
		
		subPledge.setLeaderId(leaderSubPledge.getObjectId());
		clan.updateSubPledgeInDB(subPledge);
		
		final Player leaderPlayer = leaderSubPledge.getPlayerInstance();
		if (leaderPlayer != null)
		{
			leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
			leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
		}
		
		clan.broadcastToOnlineMembers(new PledgeShowMemberListAll(clan, subPledge.getId()), SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2).addString(leaderName).addString(clanName));
	}
	
	public static final void showPledgeSkillList(Player player)
	{
		if (!player.isClanLeader())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/scripts/village_master/Clan/9000-09-no.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final List<ClanSkillNode> skills = SkillTreeData.getInstance().getClanSkillsFor(player);
		if (skills.isEmpty())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/scripts/village_master/Clan/9000-09-no.htm");
			player.sendPacket(html);
		}
		else
			player.sendPacket(new AcquireSkillList(AcquireSkillType.CLAN, skills));
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	private static final void showHtmlMenu(Player player, int objectId, int level)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		
		if (!Config.CLASS_MASTER_SETTINGS.isAllowed(level))
		{
			final StringBuilder sb = new StringBuilder(100);
			sb.append("<html><body>");
			
			switch (player.getClassId().level())
			{
				case 0:
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(1))
						sb.append("Come back here when you reached level 20 to change your class.<br>");
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
						sb.append("Come back after your first occupation change.<br>");
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
						sb.append("Come back after your second occupation change.<br>");
					else
						sb.append("I can't change your occupation.<br>");
					break;
				
				case 1:
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
						sb.append("Come back here when you reached level 40 to change your class.<br>");
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
						sb.append("Come back after your second occupation change.<br>");
					else
						sb.append("I can't change your occupation.<br>");
					break;
				
				case 2:
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
						sb.append("Come back here when you reached level 76 to change your class.<br>");
					else
						sb.append("I can't change your occupation.<br>");
					break;
				
				case 3:
					sb.append("There is no class change available for you anymore.<br>");
					break;
			}
			sb.append("</body></html>");
			html.setHtml(sb.toString());
		}
		else
		{
			final ClassId currentClassId = player.getClassId();
			if (currentClassId.level() >= level)
				html.setFile("data/html/classmaster/nomore.htm");
			else
			{
				final int minLevel = getMinLevel(currentClassId.level());
				if (player.getLevel() >= minLevel || Config.ALLOW_ENTIRE_TREE)
				{
					final StringBuilder menu = new StringBuilder(100);
					for (ClassId cid : ClassId.VALUES)
					{
						if (cid.level() != level)
							continue;
						
						if (validateClassId(currentClassId, cid))
							StringUtil.append(menu, "<a action=\"bypass -h npc_%objectId%_change_class ", cid.getId(), "\">", PlayerData.getInstance().getClassNameById(cid.getId()), "</a><br>");
					}
					
					if (menu.length() > 0)
					{
						html.setFile("data/html/classmaster/template.htm");
						html.replace("%name%", PlayerData.getInstance().getClassNameById(currentClassId.getId()));
						html.replace("%menu%", menu.toString());
					}
					else
					{
						html.setFile("data/html/classmaster/comebacklater.htm");
						html.replace("%level%", getMinLevel(level - 1));
					}
				}
				else
				{
					if (minLevel < Integer.MAX_VALUE)
					{
						html.setFile("data/html/classmaster/comebacklater.htm");
						html.replace("%level%", minLevel);
					}
					else
						html.setFile("data/html/classmaster/nomore.htm");
				}
			}
		}
		
		html.replace("%objectId%", objectId);
		html.replace("%req_items%", getRequiredItems(level));
		player.sendPacket(html);
	}
	
	private static final boolean checkAndChangeClass(Player player, int val)
	{
		final ClassId currentClassId = player.getClassId();
		if (getMinLevel(currentClassId.level()) > player.getLevel() && !Config.ALLOW_ENTIRE_TREE)
			return false;
		
		if (!validateClassId(currentClassId, val))
			return false;
		
		int newJobLevel = currentClassId.level() + 1;
		
		// Weight/Inventory check
		if (!Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).isEmpty())
		{
			if (player.getWeightPenalty() > 2)
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return false;
			}
		}
		
		final List<IntIntHolder> neededItems = Config.CLASS_MASTER_SETTINGS.getRequiredItems(newJobLevel);
		
		// check if player have all required items for class transfer
		for (IntIntHolder item : neededItems)
		{
			if (player.getInventory().getInventoryItemCount(item.getId(), -1) < item.getValue())
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return false;
			}
		}
		
		// get all required items for class transfer
		for (IntIntHolder item : neededItems)
		{
			if (!player.destroyItemByItemId("ClassMaster", item.getId(), item.getValue(), player, true))
				return false;
		}
		
		// reward player with items
		for (IntIntHolder item : Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel))
			player.addItem("ClassMaster", item.getId(), item.getValue(), player, true);
		
		player.setClassId(val);
		
		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());
		
		player.sendPacket(new HennaInfo(player));
		player.broadcastUserInfo();
		return true;
	}
	
	/**
	 * @param level - current skillId level (0 - start, 1 - first, etc)
	 * @return minimum player level required for next class transfer
	 */
	private static final int getMinLevel(int level)
	{
		switch (level)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param val new class index
	 * @return
	 */
	private static final boolean validateClassId(ClassId oldCID, int val)
	{
		try
		{
			return validateClassId(oldCID, ClassId.VALUES[val]);
		}
		catch (Exception e)
		{
			// possible ArrayOutOfBoundsException
		}
		return false;
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param newCID new ClassId
	 * @return true if class change is possible
	 */
	private static final boolean validateClassId(ClassId oldCID, ClassId newCID)
	{
		if (newCID == null)
			return false;
		
		if (oldCID == newCID.getParent())
			return true;
		
		if (Config.ALLOW_ENTIRE_TREE && newCID.childOf(oldCID))
			return true;
		
		return false;
	}
	
	private static String getRequiredItems(int level)
	{
		final List<IntIntHolder> neededItems = Config.CLASS_MASTER_SETTINGS.getRequiredItems(level);
		if (neededItems == null || neededItems.isEmpty())
			return "<tr><td>none</td></r>";
		
		final StringBuilder sb = new StringBuilder();
		for (IntIntHolder item : neededItems)
			StringUtil.append(sb, "<tr><td><font color=\"LEVEL\">", item.getValue(), "</font></td><td>", ItemTable.getInstance().getTemplate(item.getId()).getName(), "</td></tr>");
		
		return sb.toString();
	}
}