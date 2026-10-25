package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * Maps Agathion bracelet items to passive skills granted while summoned.
 */
public class AgathionSkillData implements IXmlReader
{
	/** First bracelet with a passive (must match npc 50050 / summon skill 9000). */
	private static final int BASE_ITEM_ID = 10139;
	private static final int BASE_NPC_ID = 50050;
	
	private final Map<Integer, IntIntHolder> _byItemId = new HashMap<>();
	private final Map<Integer, IntIntHolder> _byNpcId = new HashMap<>();
	private final Set<Integer> _skillIds = new HashSet<>();
	
	protected AgathionSkillData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_byItemId.clear();
		_byNpcId.clear();
		_skillIds.clear();
		parseFile("./data/xml/AgathionSkills.xml");
		LOGGER.info("Loaded {} agathion passive skill mappings.", _byItemId.size());
	}
	
	public void reload()
	{
		load();
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "agathion", node ->
		{
			final NamedNodeMap attrs = node.getAttributes();
			final int itemId = parseInteger(attrs, "itemId");
			final int skillId = parseInteger(attrs, "skillId");
			final int skillLvl = parseInteger(attrs, "skillLvl", 1);
			
			final IntIntHolder holder = new IntIntHolder(skillId, skillLvl);
			_byItemId.put(itemId, holder);
			_skillIds.add(skillId);
			
			final int npcId = BASE_NPC_ID + (itemId - BASE_ITEM_ID);
			_byNpcId.put(npcId, holder);
		}));
	}
	
	public IntIntHolder getByNpcId(int npcId)
	{
		return _byNpcId.get(npcId);
	}
	
	public IntIntHolder getByItemId(int itemId)
	{
		return _byItemId.get(itemId);
	}
	
	public Collection<Integer> getSkillIds()
	{
		return _skillIds;
	}
	
	public void giveSkill(Player player, int npcId)
	{
		final IntIntHolder holder = _byNpcId.get(npcId);
		if (holder == null)
			return;
		
		final L2Skill skill = SkillTable.getInstance().getInfo(holder.getId(), holder.getValue());
		if (skill == null)
			return;
		
		player.addSkill(skill, false);
		player.sendSkillList();
	}
	
	public void removeSkill(Player player, int npcId)
	{
		final IntIntHolder holder = _byNpcId.get(npcId);
		if (holder == null)
			return;
		
		player.removeSkill(holder.getId(), false);
		player.sendSkillList();
	}
	
	/** Clears every configured agathion passive (login / safety). */
	public void removeAll(Player player)
	{
		boolean changed = false;
		for (int skillId : _skillIds)
		{
			if (player.getSkillLevel(skillId) > 0)
			{
				player.removeSkill(skillId, false);
				changed = true;
			}
		}
		if (changed)
			player.sendSkillList();
	}
	
	public static AgathionSkillData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AgathionSkillData INSTANCE = new AgathionSkillData();
	}
}
