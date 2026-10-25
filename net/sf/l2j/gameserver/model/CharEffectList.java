package net.sf.l2j.gameserver.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.skills.L2EffectFlag;
import net.sf.l2j.gameserver.enums.skills.L2EffectType;
import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameTask;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbnormalStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import net.sf.l2j.gameserver.network.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class CharEffectList
{
	private static final L2Effect[] EMPTY_EFFECTS = new L2Effect[0];
	
	private List<L2Effect> _buffs;
	private List<L2Effect> _debuffs;
	
	// The table containing the List of all stacked effect in progress for each Stack group Identifier
	private Map<String, List<L2Effect>> _stackedEffects;
	
	private volatile boolean _hasBuffsRemovedOnAnyAction = false;
	private volatile boolean _hasBuffsRemovedOnDamage = false;
	private volatile boolean _hasDebuffsRemovedOnDamage = false;
	
	private boolean _queuesInitialized = false;
	private LinkedBlockingQueue<L2Effect> _addQueue;
	private LinkedBlockingQueue<L2Effect> _removeQueue;
	private final AtomicBoolean queueLock = new AtomicBoolean();
	private final AtomicBoolean _suppressIconUpdate = new AtomicBoolean();
	private int _effectFlags;
	
	// only party icons need to be updated
	private boolean _partyOnly = false;
	
	// Servitor Share: si el duenio lo tiene (o se le acaba de ir), hay que refrescar los stats del servitor.
	private boolean _servitorShareActive = false;
	
	// Owner of this list
	private final Creature _owner;
	
	private L2Effect[] _effectCache;
	private volatile boolean _rebuildCache = true;
	private final Object _buildEffectLock = new Object();
	
	public CharEffectList(Creature owner)
	{
		_owner = owner;
	}
	
	/**
	 * Returns all effects affecting stored in this CharEffectList
	 * @return
	 */
	public final L2Effect[] getAllEffects()
	{
		// If no effect is active, return EMPTY_EFFECTS
		if ((_buffs == null || _buffs.isEmpty()) && (_debuffs == null || _debuffs.isEmpty()))
			return EMPTY_EFFECTS;
		
		synchronized (_buildEffectLock)
		{
			// If we dont need to rebuild the cache, just return the current one.
			if (!_rebuildCache)
				return _effectCache;
			
			_rebuildCache = false;
			
			// Create a copy of the effects
			List<L2Effect> temp = new ArrayList<>();
			
			// Add all buffs and all debuffs
			if (_buffs != null && !_buffs.isEmpty())
				temp.addAll(_buffs);
			if (_debuffs != null && !_debuffs.isEmpty())
				temp.addAll(_debuffs);
			
			// Return all effects in an array
			L2Effect[] tempArray = new L2Effect[temp.size()];
			temp.toArray(tempArray);
			return (_effectCache = tempArray);
		}
	}
	
	/**
	 * Returns the first effect matching the given EffectType
	 * @param tp
	 * @return
	 */
	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		L2Effect effectNotInUse = null;
		
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
					continue;
				
				if (e.getEffectType() == tp)
				{
					if (e.getInUse())
						return e;
					
					effectNotInUse = e;
				}
			}
		}
		
		if (effectNotInUse == null && _debuffs != null && !_debuffs.isEmpty())
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
					continue;
				
				if (e.getEffectType() == tp)
				{
					if (e.getInUse())
						return e;
					
					effectNotInUse = e;
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * Returns the first effect matching the given L2Skill
	 * @param skill
	 * @return
	 */
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		L2Effect effectNotInUse = null;
		
		if (skill.isDebuff())
		{
			if (_debuffs != null && !_debuffs.isEmpty())
			{
				for (L2Effect e : _debuffs)
				{
					if (e == null)
						continue;
					
					if (e.getSkill() == skill)
					{
						if (e.getInUse())
							return e;
						
						effectNotInUse = e;
					}
				}
			}
		}
		else
		{
			if (_buffs != null && !_buffs.isEmpty())
			{
				for (L2Effect e : _buffs)
				{
					if (e == null)
						continue;
					
					if (e.getSkill() == skill)
					{
						if (e.getInUse())
							return e;
						
						effectNotInUse = e;
					}
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * @param skillId The skill id to check.
	 * @return the first effect matching the given skillId.
	 */
	public final L2Effect getFirstEffect(int skillId)
	{
		L2Effect effectNotInUse = null;
		
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
					continue;
				
				if (e.getSkill().getId() == skillId)
				{
					if (e.getInUse())
						return e;
					
					effectNotInUse = e;
				}
			}
		}
		
		if (effectNotInUse == null && _debuffs != null && !_debuffs.isEmpty())
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
					continue;
				if (e.getSkill().getId() == skillId)
				{
					if (e.getInUse())
						return e;
					
					effectNotInUse = e;
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * Checks if the given skill stacks with an existing one.
	 * @param checkSkill the skill to be checked
	 * @return Returns whether or not this skill will stack
	 */
	private boolean doesStack(L2Skill checkSkill)
	{
		if (_buffs == null || _buffs.isEmpty())
			return false;
		
		if (checkSkill._effectTemplates == null || checkSkill._effectTemplates.isEmpty())
			return false;
		
		final String stackType = checkSkill._effectTemplates.get(0).stackType;
		if (stackType == null || "none".equals(stackType))
			return false;
		
		for (L2Effect e : _buffs)
		{
			if (e.getStackType() != null && e.getStackType().equals(stackType))
				return true;
		}
		return false;
	}
	
	// ==========================================================================================
	// Slots de buffs estilo H5: buffs (MaxBuffsAmount + Divine Inspiration), songs/dances
	// (MaxDanceAmount) y buffs que salen por chance (MaxTriggeredBuffAmount), cada uno por su lado.
	// Los debuffs no ocupan slots, salvo DebuffsTakeBuffSlots = True (modo C4).
	// Se cuenta por skill (un skill con 2 efectos ocupa 1 slot, igual que su icono).
	// ==========================================================================================
	
	private enum BuffSlot
	{
		NONE,
		BUFF,
		DANCE,
		TRIGGERED
	}
	
	private static boolean isSlotBuffType(L2Skill skill)
	{
		switch (skill.getSkillType())
		{
			case BUFF:
			case COMBATPOINTHEAL:
			case REFLECT:
			case HEAL_PERCENT:
			case HEAL_STATIC:
			case MANAHEAL_PERCENT:
				return true;
			default:
				return false;
		}
	}
	
	private static BuffSlot getSlot(L2Skill skill)
	{
		if (skill.isDebuff() || skill.isToggle() || skill.isPassive() || skill.is7Signs())
			return BuffSlot.NONE;
		
		if (skill.isDance())
			return BuffSlot.DANCE;
		
		if (!isSlotBuffType(skill))
			return BuffSlot.NONE;
		
		if (skill.isTriggeredSkill())
			return BuffSlot.TRIGGERED;
		
		return BuffSlot.BUFF;
	}
	
	private int getMaxSlots(BuffSlot slot)
	{
		switch (slot)
		{
			case BUFF:
				return _owner.getMaxBuffCount();
			case DANCE:
				return Config.MAX_DANCE_AMOUNT;
			case TRIGGERED:
				return Config.MAX_TRIGGERED_BUFF_AMOUNT;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * @param slot : the slot group to count.
	 * @param excludeSkillId : skill id to ignore (the skill being added), or -1.
	 * @return the amount of different skills using that slot group.
	 */
	private int countSlot(BuffSlot slot, int excludeSkillId)
	{
		if (_buffs == null || _buffs.isEmpty())
			return 0;
		
		final java.util.Set<Integer> ids = new java.util.HashSet<>();
		for (L2Effect e : _buffs)
		{
			if (e == null || !e.getShowIcon())
				continue;
			
			final L2Skill skill = e.getSkill();
			if (skill.getId() == excludeSkillId)
				continue;
			
			if (getSlot(skill) == slot)
				ids.add(skill.getId());
		}
		return ids.size();
	}
	
	/**
	 * @param excludeSkillId : skill id to ignore, or -1.
	 * @return the amount of different visible debuffs.
	 */
	private int countDebuffs(int excludeSkillId)
	{
		if (_debuffs == null || _debuffs.isEmpty())
			return 0;
		
		final java.util.Set<Integer> ids = new java.util.HashSet<>();
		for (L2Effect e : _debuffs)
		{
			if (e == null || !e.getShowIcon())
				continue;
			
			final L2Skill skill = e.getSkill();
			if (skill.getId() == excludeSkillId || skill.isToggle() || skill.isPassive())
				continue;
			
			ids.add(skill.getId());
		}
		return ids.size();
	}
	
	/**
	 * @param slot : the slot group.
	 * @param excludeSkillId : skill id to ignore, or -1.
	 * @return used slots of that group (in C4 mode the debuffs use buff slots).
	 */
	private int getUsedSlots(BuffSlot slot, int excludeSkillId)
	{
		int used = countSlot(slot, excludeSkillId);
		if (slot == BuffSlot.BUFF && Config.DEBUFFS_TAKE_BUFF_SLOTS)
			used += countDebuffs(excludeSkillId);
		return used;
	}
	
	/**
	 * Saca los skills mas viejos (arriba a la izquierda) de ese grupo de slots.
	 * @param slot : the slot group.
	 * @param amount : how many skills to remove.
	 * @param excludeSkillId : skill id to keep, or -1.
	 */
	private void removeOldest(BuffSlot slot, int amount, int excludeSkillId)
	{
		if (_buffs == null || amount <= 0)
			return;
		
		final List<Integer> order = new ArrayList<>();
		for (L2Effect e : _buffs)
		{
			if (e == null || !e.getShowIcon())
				continue;
			
			final int id = e.getSkill().getId();
			if (id != excludeSkillId && getSlot(e.getSkill()) == slot && !order.contains(id))
				order.add(id);
		}
		
		for (int k = 0; k < amount && k < order.size(); k++)
		{
			final int id = order.get(k);
			for (L2Effect e : _buffs)
			{
				if (e != null && e.getSkill().getId() == id)
					e.exit();
			}
		}
	}
	
	/**
	 * Hace lugar para un skill nuevo en su grupo de slots, si hace falta.
	 * @param slot : the slot group.
	 * @param newSkillId : the skill being added.
	 */
	private void makeRoom(BuffSlot slot, int newSkillId)
	{
		if (slot == BuffSlot.NONE)
			return;
		
		final int over = getUsedSlots(slot, newSkillId) - getMaxSlots(slot) + 1;
		if (over > 0)
			removeOldest(slot, over, newSkillId);
	}
	
	/**
	 * @return the number of buffs (not counting songs/dances nor triggered buffs).
	 */
	public int getBuffCount()
	{
		return countSlot(BuffSlot.BUFF, -1);
	}
	
	/**
	 * @return the number of visible debuffs.
	 */
	public int getDebuffCount()
	{
		return countDebuffs(-1);
	}
	
	/**
	 * @return the number of songs/dances.
	 */
	public int getDanceCount()
	{
		return countSlot(BuffSlot.DANCE, -1);
	}
	
	/**
	 * @return the number of buffs launched by a chance trigger.
	 */
	public int getTriggeredBuffCount()
	{
		return countSlot(BuffSlot.TRIGGERED, -1);
	}
	
	/**
	 * Exits all effects in this CharEffectList
	 */
	public final void stopAllEffects()
	{
		// Get all active skills effects from this list
		L2Effect[] effects = getAllEffects();
		
		// Exit them (preventExitUpdate=true: sin icon/msg por cada buff)
		for (L2Effect e : effects)
		{
			if (e != null)
				e.exit(true);
		}
		
		// Un solo AbnormalStatusUpdate al final (evita freeze UI al cancel masivo)
		updateEffectIcons();
	}
	
	/**
	 * Exits all effects in this CharEffectList
	 */
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		// Get all active skills effects from this list
		L2Effect[] effects = getAllEffects();
		
		// Exit them
		for (L2Effect e : effects)
		{
			if (e != null && !e.getSkill().isStayAfterDeath())
				e.exit(true);
		}
		
		updateEffectIcons();
	}
	
	/**
	 * Exit all toggle-type effects
	 */
	public void stopAllToggles()
	{
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				if (e != null && e.getSkill().isToggle())
					e.exit();
			}
		}
	}
	
	/**
	 * Exit all effects having a specified type
	 * @param type
	 */
	public final void stopEffects(L2EffectType type)
	{
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				// Get active skills effects of the selected type
				if (e != null && e.getEffectType() == type)
					e.exit();
			}
		}
		
		if (_debuffs != null && !_debuffs.isEmpty())
		{
			for (L2Effect e : _debuffs)
			{
				// Get active skills effects of the selected type
				if (e != null && e.getEffectType() == type)
					e.exit();
			}
		}
	}
	
	/**
	 * Exits all effects created by a specific skillId
	 * @param skillId
	 */
	public final void stopSkillEffects(int skillId)
	{
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				if (e != null && e.getSkill().getId() == skillId)
					e.exit();
			}
		}
		
		if (_debuffs != null && !_debuffs.isEmpty())
		{
			for (L2Effect e : _debuffs)
			{
				if (e != null && e.getSkill().getId() == skillId)
					e.exit();
			}
		}
	}
	
	/**
	 * Exits all effects created by a specific skill type
	 * @param skillType skill type
	 * @param negateLvl
	 */
	public final void stopSkillEffects(L2SkillType skillType, int negateLvl)
	{
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				if (e != null && (e.getSkill().getSkillType() == skillType || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == skillType)) && (negateLvl == -1 || (e.getSkill().getEffectType() != null && e.getSkill().getEffectAbnormalLvl() >= 0 && e.getSkill().getEffectAbnormalLvl() <= negateLvl) || (e.getSkill().getAbnormalLvl() >= 0 && e.getSkill().getAbnormalLvl() <= negateLvl)))
					e.exit();
			}
		}
		
		if (_debuffs != null && !_debuffs.isEmpty())
		{
			for (L2Effect e : _debuffs)
			{
				if (e != null && (e.getSkill().getSkillType() == skillType || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == skillType)) && (negateLvl == -1 || (e.getSkill().getEffectType() != null && e.getSkill().getEffectAbnormalLvl() >= 0 && e.getSkill().getEffectAbnormalLvl() <= negateLvl) || (e.getSkill().getAbnormalLvl() >= 0 && e.getSkill().getAbnormalLvl() <= negateLvl)))
					e.exit();
			}
		}
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set. Called on any action except movement (attack, cast).
	 */
	public void stopEffectsOnAction()
	{
		if (_hasBuffsRemovedOnAnyAction)
		{
			if (_buffs != null && !_buffs.isEmpty())
			{
				for (L2Effect e : _buffs)
				{
					if (e != null && e.getSkill().isRemovedOnAnyActionExceptMove())
						e.exit(true);
				}
			}
		}
	}
	
	public void stopEffectsOnDamage(boolean awake)
	{
		if (_hasBuffsRemovedOnDamage)
		{
			if (_buffs != null && !_buffs.isEmpty())
			{
				for (L2Effect e : _buffs)
				{
					if (e != null && e.getSkill().isRemovedOnDamage() && (awake || e.getSkill().getSkillType() != L2SkillType.SLEEP))
						e.exit(true);
				}
			}
		}
		
		if (_hasDebuffsRemovedOnDamage)
		{
			if (_debuffs != null && !_debuffs.isEmpty())
			{
				for (L2Effect e : _debuffs)
				{
					if (e != null && e.getSkill().isRemovedOnDamage() && (awake || e.getSkill().getSkillType() != L2SkillType.SLEEP))
						e.exit(true);
				}
			}
		}
	}
	
	public void setSuppressIconUpdate(boolean suppress)
	{
		_suppressIconUpdate.set(suppress);
	}
	
	public boolean isSuppressIconUpdate()
	{
		return _suppressIconUpdate.get();
	}
	
	public void updateEffectIcons(boolean partyOnly)
	{
		if (_buffs == null && _debuffs == null)
			return;
		
		if (partyOnly)
			_partyOnly = true;
		
		// Llamada explicita (fin de batch fighter/mage/scheme): forzar 1 packet
		queueRunner(true);
	}
	
	public void queueEffect(L2Effect effect, boolean remove)
	{
		if (effect == null)
			return;
		
		if (!_queuesInitialized)
			init();
		
		if (remove)
			_removeQueue.offer(effect);
		else
			_addQueue.offer(effect);
		
		queueRunner(false);
	}
	
	private synchronized void init()
	{
		if (_queuesInitialized)
			return;
		
		_addQueue = new LinkedBlockingQueue<>();
		_removeQueue = new LinkedBlockingQueue<>();
		_queuesInitialized = true;
	}
	
	private void queueRunner(boolean forceIconUpdate)
	{
		if (!queueLock.compareAndSet(false, true))
			return;
		
		try
		{
			L2Effect effect;
			boolean updateIcons = forceIconUpdate;
			do
			{
				// remove has more priority than add so removing all effects from queue first
				while ((effect = _removeQueue.poll()) != null)
				{
					// exit(true) setea preventExitUpdate: no spamear iconos al cancel masivo
					if (!effect.preventExitUpdate)
						updateIcons = true;
					removeEffectFromQueue(effect);
					_partyOnly = false;
				}
				
				if ((effect = _addQueue.poll()) != null)
				{
					addEffectFromQueue(effect);
					updateIcons = true;
					_partyOnly = false;
				}
			}
			while (!_addQueue.isEmpty() || !_removeQueue.isEmpty());
			
			computeEffectFlags();
			// suppress = batch NPC buffer (fighter/mage/scheme): 1 update al final
			if (updateIcons && !_suppressIconUpdate.get())
				updateEffectIcons();
		}
		finally
		{
			queueLock.set(false);
		}
	}
	
	protected void removeEffectFromQueue(L2Effect effect)
	{
		if (effect == null)
			return;
		
		List<L2Effect> effectList;
		
		// array modified, then rebuild on next request
		_rebuildCache = true;
		
		if (effect.getSkill().isDebuff())
		{
			if (_debuffs == null)
				return;
			
			effectList = _debuffs;
		}
		else
		{
			if (_buffs == null)
				return;
			
			effectList = _buffs;
		}
		
		if ("none".equals(effect.getStackType()))
		{
			// Remove Func added by this effect from the Creature Calculator
			_owner.removeStatsByOwner(effect);
		}
		else
		{
			if (_stackedEffects == null)
				return;
			
			// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
			List<L2Effect> stackQueue = _stackedEffects.get(effect.getStackType());
			
			if (stackQueue == null || stackQueue.isEmpty())
				return;
			
			int index = stackQueue.indexOf(effect);
			
			// Remove the effect from the stack group
			if (index >= 0)
			{
				stackQueue.remove(effect);
				// Check if the first stacked effect was the effect to remove
				if (index == 0)
				{
					// Remove all its Func objects from the Creature calculator set
					_owner.removeStatsByOwner(effect);
					
					// Check if there's another effect in the Stack Group
					if (!stackQueue.isEmpty())
					{
						L2Effect newStackedEffect = listsContains(stackQueue.get(0));
						if (newStackedEffect != null)
						{
							// Set the effect to In Use
							if (newStackedEffect.setInUse(true))
								// Add its list of Funcs to the Calculator set of the Creature
								_owner.addStatFuncs(newStackedEffect.getStatFuncs());
						}
					}
				}
				
				if (stackQueue.isEmpty())
					_stackedEffects.remove(effect.getStackType());
				else
					// Update the Stack Group table _stackedEffects of the Creature
					_stackedEffects.put(effect.getStackType(), stackQueue);
			}
		}
		
		// Remove the active skill L2effect from _effects of the Creature
		if (effectList.remove(effect) && _owner instanceof Player && effect.getShowIcon())
		{
			SystemMessage sm;
			if (effect.getSkill().isToggle())
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ABORTED);
			else
				sm = SystemMessage.getSystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED);
			
			sm.addSkillName(effect);
			_owner.sendPacket(sm);
		}
	}
	
	protected void addEffectFromQueue(L2Effect newEffect)
	{
		if (newEffect == null)
			return;
		
		L2Skill newSkill = newEffect.getSkill();
		
		// array modified, then rebuild on next request
		_rebuildCache = true;
		
		if (isAffected(newEffect.getEffectFlags()) && !newEffect.onSameEffect(null))
		{
			newEffect.stopEffectTask();
			return;
		}
		
		// H5 BlockAbnormalSlot: un efecto activo (ej. Chant of Combat) bloquea los stackType que reemplaza (pa_up, pd_up...).
		if (net.sf.l2j.gameserver.skills.H5Skills.isStackBlocked(_owner, newEffect))
		{
			newEffect.stopEffectTask();
			return;
		}
		// isStackBlocked lee getAllEffects() y reconstruye el cache: hay que volver a marcarlo para que entre el efecto nuevo.
		_rebuildCache = true;
		
		if (newSkill.isDebuff())
		{
			if (_debuffs == null)
				_debuffs = new CopyOnWriteArrayList<>();
			
			for (L2Effect e : _debuffs)
			{
				if (e != null && e.getSkill().getId() == newEffect.getSkill().getId() && e.getEffectType() == newEffect.getEffectType() && e.getStackOrder() == newEffect.getStackOrder() && e.getStackType().equals(newEffect.getStackType()))
				{
					// Started scheduled timer needs to be canceled.
					newEffect.stopEffectTask();
					return;
				}
			}
			// Modo C4 (DebuffsTakeBuffSlots): el debuff ocupa un slot de buff. Si buffs + debuffs ya llegan al maximo, se va el buff mas viejo.
			// Si reemplaza a un debuff del mismo tipo (mismo stackType) no ocupa un slot nuevo.
			boolean replacesDebuff = false;
			if (!"none".equals(newEffect.getStackType()))
			{
				for (L2Effect e : _debuffs)
				{
					if (e != null && newEffect.getStackType().equals(e.getStackType()))
					{
						replacesDebuff = true;
						break;
					}
				}
			}
			if (Config.DEBUFFS_TAKE_BUFF_SLOTS && !replacesDebuff && newEffect.getShowIcon() && !newSkill.isToggle() && !newSkill.isPassive())
			{
				final int over = getUsedSlots(BuffSlot.BUFF, newSkill.getId()) - _owner.getMaxBuffCount() + 1;
				if (over > 0)
					removeOldest(BuffSlot.BUFF, over, -1);
			}
			
			_debuffs.add(newEffect);
		}
		else
		{
			if (_buffs == null)
				_buffs = new CopyOnWriteArrayList<>();
			
			for (L2Effect e : _buffs)
			{
				if (e != null && e.getSkill().getId() == newEffect.getSkill().getId() && e.getEffectType() == newEffect.getEffectType() && e.getStackOrder() == newEffect.getStackOrder() && e.getStackType().equals(newEffect.getStackType()))
				{
					e.exit(); // exit this
				}
			}
			
			final BuffSlot slot = getSlot(newSkill);
			
			// if max buffs, no herb effects are used, even if they would replace one old
			if (newEffect.isHerbEffect() && slot == BuffSlot.BUFF && getUsedSlots(BuffSlot.BUFF, newSkill.getId()) >= _owner.getMaxBuffCount())
			{
				newEffect.stopEffectTask();
				return;
			}
			
			// Lista llena: se va el mas viejo de su mismo grupo (buffs, danzas o triggered).
			if (newEffect.getShowIcon() && !doesStack(newSkill))
				makeRoom(slot, newSkill.getId());
			
			// Icons order: buffs then toggles
			if (newSkill.isToggle())
				_buffs.add(newEffect);
			else
			{
				int pos = 0;
				for (L2Effect e : _buffs)
				{
					if (e == null || e.getSkill().isToggle() || e.getSkill().is7Signs())
						continue;
					
					pos++;
				}
				_buffs.add(pos, newEffect);
			}
		}
		
		// Check if a stack group is defined for this effect
		if ("none".equals(newEffect.getStackType()))
		{
			// Set this L2Effect to In Use
			if (newEffect.setInUse(true))
				// Add Funcs of this effect to the Calculator set of the Creature
				_owner.addStatFuncs(newEffect.getStatFuncs());
			
			return;
		}
		
		List<L2Effect> stackQueue;
		L2Effect effectToAdd = null;
		L2Effect effectToRemove = null;
		if (_stackedEffects == null)
			_stackedEffects = new HashMap<>();
		
		// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
		stackQueue = _stackedEffects.get(newEffect.getStackType());
		
		if (stackQueue != null)
		{
			int pos = 0;
			if (!stackQueue.isEmpty())
			{
				// Get the first stacked effect of the Stack group selected
				effectToRemove = listsContains(stackQueue.get(0));
				
				// Create an Iterator to go through the list of stacked effects in progress on the Creature
				Iterator<L2Effect> queueIterator = stackQueue.iterator();
				
				while (queueIterator.hasNext())
				{
					if (newEffect.getStackOrder() < queueIterator.next().getStackOrder())
						pos++;
					else
						break;
				}
				// Add the new effect to the Stack list in function of its position in the Stack group
				stackQueue.add(pos, newEffect);
				
				// skill.exit() could be used, if the users don't wish to see "effect
				// removed" always when a timer goes off, even if the buff isn't active
				// any more (has been replaced). but then check e.g. npc hold and raid petrification.
				if (Config.EFFECT_CANCELING && !newEffect.isHerbEffect() && stackQueue.size() > 1)
				{
					// Se saca de la lista a la que pertenece el efecto reemplazado (no la del nuevo):
					// ej. Surrender (debuff) reemplaza Resist Aqua (buff) -> el buff se borra de _buffs.
					final L2Effect replaced = stackQueue.remove(1);
					if (replaced.getSkill().isDebuff())
						_debuffs.remove(replaced);
					else
						_buffs.remove(replaced);
				}
			}
			else
				stackQueue.add(0, newEffect);
		}
		else
		{
			stackQueue = new ArrayList<>();
			stackQueue.add(0, newEffect);
		}
		
		// Update the Stack Group table _stackedEffects of the Creature
		_stackedEffects.put(newEffect.getStackType(), stackQueue);
		
		// Get the first stacked effect of the Stack group selected
		if (!stackQueue.isEmpty())
			effectToAdd = listsContains(stackQueue.get(0));
		
		if (effectToRemove != effectToAdd)
		{
			if (effectToRemove != null)
			{
				// Remove all Func objects corresponding to this stacked effect from the Calculator set of the Creature
				_owner.removeStatsByOwner(effectToRemove);
				
				// Set the L2Effect to Not In Use
				effectToRemove.setInUse(false);
			}
			
			if (effectToAdd != null)
			{
				// Set this L2Effect to In Use
				if (effectToAdd.setInUse(true))
					// Add all Func objects corresponding to this stacked effect to the Calculator set of the Creature
					_owner.addStatFuncs(effectToAdd.getStatFuncs());
			}
		}
	}
	
	protected void updateEffectIcons()
	{
		if (_owner == null)
			return;
		
		if (!(_owner instanceof Playable))
		{
			updateEffectFlags();
			return;
		}
		
		AbnormalStatusUpdate mi = null;
		PartySpelled ps = null;
		ExOlympiadSpelledInfo os = null;
		
		if (_owner instanceof Player)
		{
			if (_partyOnly)
				_partyOnly = false;
			else
				mi = new AbnormalStatusUpdate();
			
			if (_owner.isInParty())
				ps = new PartySpelled(_owner);
			
			if (((Player) _owner).isInOlympiadMode() && ((Player) _owner).isOlympiadStart())
				os = new ExOlympiadSpelledInfo((Player) _owner);
		}
		else if (_owner instanceof Summon)
			ps = new PartySpelled(_owner);
		
		boolean foundRemovedOnAction = false;
		boolean foundRemovedOnDamage = false;
		final java.util.Set<Integer> shownIcons = new java.util.HashSet<>();
		
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
					continue;
				
				if (e.getSkill().isRemovedOnAnyActionExceptMove())
					foundRemovedOnAction = true;
				if (e.getSkill().isRemovedOnDamage())
					foundRemovedOnDamage = true;
				
				if (!e.getShowIcon())
					continue;
				
				switch (e.getEffectType())
				{
					case SIGNET_GROUND:
						continue;
				}
				
				if (e.getInUse())
				{
					// Un icono por skill (los skills con 2 efectos no muestran 2 iconos).
					if (!shownIcons.add(e.getSkill().getId()))
						continue;
					
					if (mi != null)
						e.addIcon(mi);
					
					if (ps != null)
						e.addPartySpelledIcon(ps);
					
					if (os != null)
						e.addOlympiadSpelledIcon(os);
				}
			}
		}
		
		_hasBuffsRemovedOnAnyAction = foundRemovedOnAction;
		_hasBuffsRemovedOnDamage = foundRemovedOnDamage;
		foundRemovedOnDamage = false;
		
		if (_debuffs != null && !_debuffs.isEmpty())
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
					continue;
				
				if (e.getSkill().isRemovedOnAnyActionExceptMove())
					foundRemovedOnAction = true;
				if (e.getSkill().isRemovedOnDamage())
					foundRemovedOnDamage = true;
				
				if (!e.getShowIcon())
					continue;
				
				switch (e.getEffectType())
				{
					case SIGNET_GROUND:
						continue;
				}
				
				if (e.getInUse())
				{
					// Un icono por skill (los skills con 2 efectos no muestran 2 iconos).
					if (!shownIcons.add(e.getSkill().getId()))
						continue;
					
					if (mi != null)
						e.addIcon(mi);
					
					if (ps != null)
						e.addPartySpelledIcon(ps);
					
					if (os != null)
						e.addOlympiadSpelledIcon(os);
				}
			}
		}
		
		_hasDebuffsRemovedOnDamage = foundRemovedOnDamage;
		
		if (mi != null)
			_owner.sendPacket(mi);
		
		// Servitor Share: los stats del servitor dependen de los del duenio; cuando cambian los buffs del duenio se refresca la ventana del pet.
		if (_owner instanceof Player)
		{
			final boolean shareNow = _owner.getFirstEffect(net.sf.l2j.gameserver.skills.basefuncs.FuncServitorShare.SERVITOR_SHARE_SKILL_ID) != null;
			if (shareNow || _servitorShareActive)
			{
				final Summon summon = ((Player) _owner).getSummon();
				if (summon != null && !summon.isDead())
					summon.updateAndBroadcastStatus(1);
			}
			_servitorShareActive = shareNow;
		}
		
		if (ps != null)
		{
			if (_owner instanceof Summon)
			{
				final Player summonOwner = ((Summon) _owner).getOwner();
				if (summonOwner != null)
				{
					final Party party = summonOwner.getParty();
					if (party != null)
						party.broadcastPacket(ps);
					else
						summonOwner.sendPacket(ps);
				}
			}
			else if (_owner instanceof Player && _owner.isInParty())
				_owner.getParty().broadcastPacket(ps);
		}
		
		if (os != null)
		{
			final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(((Player) _owner).getOlympiadGameId());
			if (game != null && game.isBattleStarted())
				game.getZone().broadcastPacketToObservers(os);
		}
	}
	
	protected void updateEffectFlags()
	{
		boolean foundRemovedOnAction = false;
		boolean foundRemovedOnDamage = false;
		
		if (_buffs != null && !_buffs.isEmpty())
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
					continue;
				
				if (e.getSkill().isRemovedOnAnyActionExceptMove())
					foundRemovedOnAction = true;
				if (e.getSkill().isRemovedOnDamage())
					foundRemovedOnDamage = true;
			}
		}
		_hasBuffsRemovedOnAnyAction = foundRemovedOnAction;
		_hasBuffsRemovedOnDamage = foundRemovedOnDamage;
		foundRemovedOnDamage = false;
		
		if (_debuffs != null && !_debuffs.isEmpty())
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
					continue;
				
				if (e.getSkill().isRemovedOnDamage())
					foundRemovedOnDamage = true;
			}
		}
		_hasDebuffsRemovedOnDamage = foundRemovedOnDamage;
	}
	
	/**
	 * Returns effect if contains in _buffs or _debuffs and null if not found
	 * @param effect
	 * @return
	 */
	private L2Effect listsContains(L2Effect effect)
	{
		if (_buffs != null && !_buffs.isEmpty() && _buffs.contains(effect))
			return effect;
		if (_debuffs != null && !_debuffs.isEmpty() && _debuffs.contains(effect))
			return effect;
		
		return null;
	}
	
	/**
	 * Recalculate effect bits flag.<br>
	 * Please no concurrency access
	 */
	private final void computeEffectFlags()
	{
		int flags = 0;
		
		if (_buffs != null)
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
					continue;
				
				flags |= e.getEffectFlags();
			}
		}
		
		if (_debuffs != null)
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
					continue;
				
				flags |= e.getEffectFlags();
			}
		}
		
		_effectFlags = flags;
	}
	
	/**
	 * Check if target is affected with special buff
	 * @param flag flag of special buff
	 * @return boolean true if affected
	 */
	public boolean isAffected(L2EffectFlag flag)
	{
		return isAffected(flag.getMask());
	}
	
	public boolean isAffected(int mask)
	{
		return (_effectFlags & mask) != 0;
	}
	
	/**
	 * Clear and null all queues and lists Use only during delete character from the world.
	 */
	public void clear()
	{
		_addQueue = null;
		_removeQueue = null;
		_buffs = null;
		_debuffs = null;
		_stackedEffects = null;
		_queuesInitialized = false;
	}
}