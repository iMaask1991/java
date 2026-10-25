package net.sf.l2j.gameserver.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.sf.l2j.commons.util.StatsSet;

import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.conditions.Condition;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author mkizub
 */
public final class DocumentSkill extends DocumentBase
{
	public class Skill
	{
		public int id;
		public String name;
		public StatsSet[] sets;
		/** Rutas de enchant: enchsets[r] = ruta r+1 (niveles (r+1)*100+1 ...). */
		public StatsSet[][] enchsets;
		public int currentLevel;
		public List<L2Skill> skills = new ArrayList<>();
		public List<L2Skill> currentSkills = new ArrayList<>();
	}
	
	private Skill _currentSkill;
	private final List<L2Skill> _skillsInFile = new ArrayList<>();
	
	public DocumentSkill(File file)
	{
		super(file);
	}
	
	private void setCurrentSkill(Skill skill)
	{
		_currentSkill = skill;
	}
	
	@Override
	protected StatsSet getStatsSet()
	{
		return _currentSkill.sets[_currentSkill.currentLevel];
	}
	
	public List<L2Skill> getSkills()
	{
		return _skillsInFile;
	}
	
	@Override
	protected String getTableValue(String name)
	{
		try
		{
			return _tables.get(name)[_currentSkill.currentLevel];
		}
		catch (RuntimeException e)
		{
			_log.log(Level.SEVERE, "Error in table: " + name + " of Skill Id " + _currentSkill.id, e);
			return "";
		}
	}
	
	@Override
	protected String getTableValue(String name, int idx)
	{
		try
		{
			return _tables.get(name)[idx - 1];
		}
		catch (RuntimeException e)
		{
			_log.log(Level.SEVERE, "wrong level count in skill Id " + _currentSkill.id, e);
			return "";
		}
	}
	
	@Override
	protected void parseDocument(Document doc)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(d.getNodeName()))
					{
						setCurrentSkill(new Skill());
						parseSkill(d);
						_skillsInFile.addAll(_currentSkill.skills);
						resetTable();
					}
				}
			}
			else if ("skill".equalsIgnoreCase(n.getNodeName()))
			{
				setCurrentSkill(new Skill());
				parseSkill(n);
				_skillsInFile.addAll(_currentSkill.skills);
			}
		}
	}
	
	/** Cantidad maxima de rutas de enchant (ruta N = niveles N01-N30, formato H5). */
	public static final int MAX_ENCHANT_ROUTES = 9;
	
	protected void parseSkill(Node n)
	{
		NamedNodeMap attrs = n.getAttributes();
		int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
		String skillName = attrs.getNamedItem("name").getNodeValue();
		String levels = attrs.getNamedItem("levels").getNodeValue();
		int lastLvl = Integer.parseInt(levels);
		
		final int[] enchantLevels = new int[MAX_ENCHANT_ROUTES];
		for (int r = 0; r < MAX_ENCHANT_ROUTES; r++)
		{
			final Node att = attrs.getNamedItem("enchantLevels" + (r + 1));
			if (att != null)
				enchantLevels[r] = Math.min(99, Integer.parseInt(att.getNodeValue()));
		}
		
		_currentSkill.id = skillId;
		_currentSkill.name = skillName;
		_currentSkill.sets = new StatsSet[lastLvl];
		_currentSkill.enchsets = new StatsSet[MAX_ENCHANT_ROUTES][];
		for (int r = 0; r < MAX_ENCHANT_ROUTES; r++)
			_currentSkill.enchsets[r] = new StatsSet[enchantLevels[r]];
		
		for (int i = 0; i < lastLvl; i++)
		{
			_currentSkill.sets[i] = new StatsSet();
			_currentSkill.sets[i].set("skill_id", _currentSkill.id);
			_currentSkill.sets[i].set("level", i + 1);
			_currentSkill.sets[i].set("name", _currentSkill.name);
		}
		
		Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
				parseTable(n);
		}
		for (int i = 1; i <= lastLvl; i++)
		{
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("set".equalsIgnoreCase(n.getNodeName()))
					parseBeanSet(n, _currentSkill.sets[i - 1], i);
			}
		}
		
		for (int r = 0; r < MAX_ENCHANT_ROUTES; r++)
		{
			final String tag = "enchant" + (r + 1);
			for (int i = 0; i < enchantLevels[r]; i++)
			{
				final StatsSet es = new StatsSet();
				es.set("skill_id", _currentSkill.id);
				es.set("level", (r + 1) * 100 + i + 1);
				es.set("name", _currentSkill.name);
				
				for (n = first; n != null; n = n.getNextSibling())
				{
					if ("set".equalsIgnoreCase(n.getNodeName()))
						parseBeanSet(n, es, _currentSkill.sets.length);
				}
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (tag.equalsIgnoreCase(n.getNodeName()))
						parseBeanSet(n, es, i + 1);
				}
				_currentSkill.enchsets[r][i] = es;
			}
		}
		
		makeSkills();
		for (int i = 0; i < lastLvl; i++)
		{
			if (_currentSkill.currentSkills.get(i) == null)
				continue;
			_currentSkill.currentLevel = i;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("cond".equalsIgnoreCase(n.getNodeName()))
				{
					Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					Node msg = n.getAttributes().getNamedItem("msg");
					Node msgId = n.getAttributes().getNamedItem("msgId");
					if (condition != null && msg != null)
						condition.setMessage(msg.getNodeValue());
					else if (condition != null && msgId != null)
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						Node addName = n.getAttributes().getNamedItem("addName");
						if (addName != null && Integer.decode(getValue(msgId.getNodeValue(), null)) > 0)
							condition.addName();
					}
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("for".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
			}
		}
		
		int offset = lastLvl;
		for (int r = 0; r < MAX_ENCHANT_ROUTES; r++)
		{
			final String condTag = "enchant" + (r + 1) + "cond";
			final String forTag = "enchant" + (r + 1) + "for";
			for (int i = offset; i < offset + enchantLevels[r]; i++)
			{
				if (_currentSkill.currentSkills.get(i) == null)
					continue;
				_currentSkill.currentLevel = i - offset;
				boolean foundCond = false, foundFor = false;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (condTag.equalsIgnoreCase(n.getNodeName()))
					{
						foundCond = true;
						Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						Node msg = n.getAttributes().getNamedItem("msg");
						if (condition != null && msg != null)
							condition.setMessage(msg.getNodeValue());
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (forTag.equalsIgnoreCase(n.getNodeName()))
					{
						foundFor = true;
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
				}
				// If none found, the enchanted skill will take effects from maxLvL of norm skill
				if (!foundCond || !foundFor)
				{
					_currentSkill.currentLevel = lastLvl - 1;
					for (n = first; n != null; n = n.getNextSibling())
					{
						if (!foundCond && "cond".equalsIgnoreCase(n.getNodeName()))
						{
							Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
							Node msg = n.getAttributes().getNamedItem("msg");
							if (condition != null && msg != null)
								condition.setMessage(msg.getNodeValue());
							_currentSkill.currentSkills.get(i).attach(condition, false);
						}
						else if (!foundFor && "for".equalsIgnoreCase(n.getNodeName()))
						{
							parseTemplate(n, _currentSkill.currentSkills.get(i));
						}
					}
				}
			}
			offset += enchantLevels[r];
		}
		for (L2Skill sk : _currentSkill.currentSkills)
			if (sk != null)
				_currentSkill.skills.add(sk);
	}
	
	private void makeSkills()
	{
		int total = _currentSkill.sets.length;
		for (StatsSet[] route : _currentSkill.enchsets)
			total += route.length;
		_currentSkill.currentSkills = new ArrayList<>(total);
		
		for (StatsSet set : _currentSkill.sets)
			addSkill(set);
		for (StatsSet[] route : _currentSkill.enchsets)
			for (StatsSet set : route)
				addSkill(set);
	}
	
	private void addSkill(StatsSet set)
	{
		try
		{
			_currentSkill.currentSkills.add(set.getEnum("skillType", L2SkillType.class).makeSkill(set));
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Skill id=" + set.getInteger("skill_id", 0) + " level=" + set.getInteger("level", 0), e);
			// Keep indexes aligned with the sets (conditions / effects are attached by index).
			_currentSkill.currentSkills.add(null);
		}
	}
}
