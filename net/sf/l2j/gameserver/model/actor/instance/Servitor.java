package net.sf.l2j.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.SetSummonRemainTime;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.basefuncs.FuncServitorShare;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

public class Servitor extends Summon
{
	private float _expPenalty = 0;
	private int _itemConsumeId = 0;
	private int _itemConsumeCount = 0;
	private int _itemConsumeSteps = 0;
	private int _totalLifeTime = 1200000;
	private int _timeLostIdle = 1000;
	private int _timeLostActive = 1000;
	private int _timeRemaining;
	private int _nextItemConsumeTime;
	
	public int lastShowntimeRemaining;
	
	private Future<?> _summonLifeTask;
	
	/** Skill que invoco este servitor (los buffs guardados van por skill, como en H5). */
	private int _referenceSkill;
	
	/** Buffs guardados de servitors: duenio -> skill de invocacion -> {skillId, level, count, time}. */
	private static final Map<Integer, Map<Integer, List<int[]>>> SAVED_EFFECTS = new ConcurrentHashMap<>();
	
	public Servitor(int objectId, NpcTemplate template, Player owner, L2Skill skill)
	{
		super(objectId, template, owner);
		
		if (skill != null)
		{
			final L2SkillSummon summonSkill = (L2SkillSummon) skill;
			_referenceSkill = summonSkill.getId();
			_itemConsumeId = summonSkill.getItemConsumeIdOT();
			_itemConsumeCount = summonSkill.getItemConsumeOT();
			_itemConsumeSteps = summonSkill.getItemConsumeSteps();
			_totalLifeTime = summonSkill.getTotalLifeTime();
			_timeLostIdle = summonSkill.getTimeLostIdle();
			_timeLostActive = summonSkill.getTimeLostActive();
		}
		_timeRemaining = _totalLifeTime;
		lastShowntimeRemaining = _totalLifeTime;
		
		if (_itemConsumeId == 0 || _itemConsumeSteps == 0)
			_nextItemConsumeTime = -1; // do not consume
		else
			_nextItemConsumeTime = _totalLifeTime - _totalLifeTime / (_itemConsumeSteps + 1);
		
		_summonLifeTask = ThreadPool.scheduleAtFixedRate(new SummonLifetime(getOwner(), this), 1000, 1000);
		
		// Servitor Share (H5): el servitor suma parte de los stats del duenio mientras el duenio tenga el efecto.
		addStatFunc(new FuncServitorShare(Stats.POWER_ATTACK, this, owner, 0.5));
		addStatFunc(new FuncServitorShare(Stats.POWER_DEFENCE, this, owner, 0.5));
		addStatFunc(new FuncServitorShare(Stats.MAGIC_ATTACK, this, owner, 0.25));
		addStatFunc(new FuncServitorShare(Stats.MAGIC_DEFENCE, this, owner, 0.25));
		addStatFunc(new FuncServitorShare(Stats.MAX_HP, this, owner, 0.1));
		addStatFunc(new FuncServitorShare(Stats.MAX_MP, this, owner, 0.1));
		addStatFunc(new FuncServitorShare(Stats.CRITICAL_RATE, this, owner, 0.2));
		addStatFunc(new FuncServitorShare(Stats.POWER_ATTACK_SPEED, this, owner, 0.1));
		addStatFunc(new FuncServitorShare(Stats.MAGIC_ATTACK_SPEED, this, owner, 0.03));
	}
	
	/**
	 * H5: guarda los buffs del servitor al desinvocarlo (o al morir con Noblesse), para devolverselos al invocarlo de nuevo.
	 */
	public void storeServitorEffects()
	{
		if (getOwner() == null || _referenceSkill == 0)
			return;
		
		final List<int[]> list = new ArrayList<>();
		final Set<Integer> seen = new HashSet<>();
		for (L2Effect effect : getAllEffects())
		{
			if (effect == null || !effect.getInUse() || effect.isHerbEffect())
				continue;
			
			switch (effect.getEffectType())
			{
				case HEAL_OVER_TIME:
				case COMBAT_POINT_HEAL_OVER_TIME:
				case MANA_HEAL_OVER_TIME:
				case NOBLESSE_BLESSING:
				case PHOENIX_BLESSING:
					continue;
			}
			
			final L2Skill skill = effect.getSkill();
			if (skill.isDebuff() || skill.isToggle() || skill.isPassive() || !seen.add(skill.getId()))
				continue;
			
			list.add(new int[]
			{
				skill.getId(),
				skill.getLevel(),
				effect.getCount(),
				effect.getTime()
			});
		}
		
		SAVED_EFFECTS.computeIfAbsent(getOwner().getObjectId(), k -> new ConcurrentHashMap<>()).put(_referenceSkill, list);
	}
	
	/**
	 * Borra los buffs guardados de este servitor (murio sin Noblesse).
	 */
	public void clearServitorEffects()
	{
		if (getOwner() == null)
			return;
		
		final Map<Integer, List<int[]>> map = SAVED_EFFECTS.get(getOwner().getObjectId());
		if (map != null)
			map.remove(_referenceSkill);
	}
	
	/**
	 * H5: devuelve los buffs que tenia el servitor la ultima vez que se lo invoco con este mismo skill.
	 */
	public void restoreServitorEffects()
	{
		if (getOwner() == null || _referenceSkill == 0)
			return;
		
		final Map<Integer, List<int[]>> map = SAVED_EFFECTS.get(getOwner().getObjectId());
		if (map == null)
			return;
		
		final List<int[]> list = map.remove(_referenceSkill);
		if (list == null || list.isEmpty())
			return;
		
		for (int[] data : list)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(data[0], data[1]);
			if (skill == null || !skill.hasEffects())
				continue;
			
			final Env env = new Env();
			env.setCharacter(this);
			env.setTarget(this);
			env.setSkill(skill);
			
			for (EffectTemplate et : skill.getEffectTemplates())
			{
				final L2Effect ef = et.getEffect(env);
				if (ef != null)
				{
					ef.setCount(data[2]);
					ef.setFirstTime(data[3]);
					ef.scheduleEffect();
				}
			}
		}
		updateEffectIcons();
	}
	
	@Override
	public final int getLevel()
	{
		return (getTemplate() != null ? getTemplate().getLevel() : 0);
	}
	
	@Override
	public int getSummonType()
	{
		return 1;
	}
	
	public void setExpPenalty(float expPenalty)
	{
		_expPenalty = expPenalty;
	}
	
	public float getExpPenalty()
	{
		return _expPenalty;
	}
	
	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}
	
	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	public int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}
	
	public int getNextItemConsumeTime()
	{
		return _nextItemConsumeTime;
	}
	
	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}
	
	public int getTimeLostIdle()
	{
		return _timeLostIdle;
	}
	
	public int getTimeLostActive()
	{
		return _timeLostActive;
	}
	
	public int getTimeRemaining()
	{
		return _timeRemaining;
	}
	
	public void setNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime = value;
	}
	
	public void decNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime -= value;
	}
	
	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}
	
	public void addExpAndSp(int addToExp, int addToSp)
	{
		getOwner().addExpAndSp(addToExp, addToSp);
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		// H5: con Noblesse el servitor no pierde los buffs al morir (los recupera al invocarlo de nuevo).
		final boolean keepEffects = isNoblesseBlessed();
		
		if (!super.doDie(killer))
			return false;
		
		if (keepEffects)
			storeServitorEffects();
		else
			clearServitorEffects();
		
		// Send aggro of mobs to summoner.
		for (Attackable mob : getKnownType(Attackable.class))
		{
			if (mob.isDead())
				continue;
			
			final AggroInfo info = mob.getAggroList().get(this);
			if (info != null)
				mob.addDamageHate(getOwner(), info.getDamage(), info.getHate());
		}
		
		// Popup for summon if phoenix buff was on
		if (isPhoenixBlessed())
			getOwner().reviveRequest(getOwner(), null, true);
		
		DecayTaskManager.getInstance().add(this, getTemplate().getCorpseTime());
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}
		return true;
		
	}
	
	@Override
	public void unSummon(Player owner)
	{
		// H5: al desinvocarlo se guardan los buffs.
		if (isVisible() && !isDead())
			storeServitorEffects();
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}
		super.unSummon(owner);
	}
	
	@Override
	public boolean destroyItem(String process, int objectId, int count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}
	
	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
	
	@Override
	public void doPickupItem(WorldObject object)
	{
	}
	
	private static class SummonLifetime implements Runnable
	{
		private final Player _player;
		private final Servitor _summon;
		
		protected SummonLifetime(Player activeChar, Servitor summon)
		{
			_player = activeChar;
			_summon = summon;
		}
		
		@Override
		public void run()
		{
			double oldTimeRemaining = _summon.getTimeRemaining();
			int maxTime = _summon.getTotalLifeTime();
			double newTimeRemaining;
			
			// if pet is attacking
			if (_summon.isAttackingNow())
				_summon.decTimeRemaining(_summon.getTimeLostActive());
			else
				_summon.decTimeRemaining(_summon.getTimeLostIdle());
			
			newTimeRemaining = _summon.getTimeRemaining();
			
			// check if the summon's lifetime has ran out
			if (newTimeRemaining < 0)
				_summon.unSummon(_player);
			else if ((newTimeRemaining <= _summon.getNextItemConsumeTime()) && (oldTimeRemaining > _summon.getNextItemConsumeTime()))
			{
				_summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));
				
				// check if owner has enought itemConsume, if requested
				if (_summon.getItemConsumeCount() > 0 && _summon.getItemConsumeId() != 0 && !_summon.isDead() && !_summon.destroyItemByItemId("Consume", _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _player, true))
					_summon.unSummon(_player);
			}
			
			// prevent useless packet-sending when the difference isn't visible.
			if ((_summon.lastShowntimeRemaining - newTimeRemaining) > maxTime / 352)
			{
				_player.sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
				_summon.lastShowntimeRemaining = (int) newTimeRemaining;
				_summon.updateEffectIcons();
			}
		}
	}
}