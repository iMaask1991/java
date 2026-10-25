package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.MultiSkillData;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

/**
 * NPC that sells class skills from other trees (MultiSkill).
 */
public class MultiSkillManager extends Folk
{
	public MultiSkillManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (!Config.MULTISKILL_ENABLED)
		{
			player.sendMessage("MultiSkill is disabled.");
			return;
		}
		
		MultiSkillData.getInstance().showMain(player, getObjectId());
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		showChatWindow(player);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!Config.MULTISKILL_ENABLED)
		{
			player.sendMessage("MultiSkill is disabled.");
			return;
		}
		
		if (!command.startsWith("multiskill"))
		{
			super.onBypassFeedback(player, command);
			return;
		}
		
		final String[] parts = command.split(" ");
		if (parts.length < 2)
		{
			MultiSkillData.getInstance().showMain(player, getObjectId());
			return;
		}
		
		final MultiSkillData data = MultiSkillData.getInstance();
		final String action = parts[1];
		
		try
		{
			switch (action)
			{
				case "main":
					data.showMain(player, getObjectId());
					break;
				
				case "race":
					if (parts.length < 3)
					{
						data.showMain(player, getObjectId());
						return;
					}
					data.showRace(player, getObjectId(), ClassRace.valueOf(parts[2]));
					break;
				
				case "class":
				{
					if (parts.length < 3)
					{
						data.showMain(player, getObjectId());
						return;
					}
					final int classId = Integer.parseInt(parts[2]);
					final int page = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
					final ClassId cid = ClassId.VALUES[classId];
					data.showClass(player, getObjectId(), cid, page);
					break;
				}
				
				case "learn":
				{
					if (parts.length < 4)
						return;
					
					final int classId = Integer.parseInt(parts[2]);
					final int skillId = Integer.parseInt(parts[3]);
					final int page = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
					final ClassId cid = ClassId.VALUES[classId];
					
					data.learnNextLevel(player, cid, skillId);
					data.showClass(player, getObjectId(), cid, page);
					break;
				}
				
				default:
					data.showMain(player, getObjectId());
					break;
			}
		}
		catch (Exception e)
		{
			player.sendMessage("Invalid MultiSkill request.");
			data.showMain(player, getObjectId());
		}
	}
}
