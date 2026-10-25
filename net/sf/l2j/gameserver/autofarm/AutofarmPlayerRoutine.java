package net.sf.l2j.gameserver.autofarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.NextAction;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

public class AutofarmPlayerRoutine
{
	private static final int TICK_MS = 450;
	
	private final Player player;
	private ScheduledFuture<?> _task;
	private Creature committedTarget = null;
	/** Ultimo jefe (mob con minions) que se estaba matando: al morir se priorizan sus minions. */
	private Monster _lastLeader = null;
	/** Marca de tiempo real para descontar el AutoFarm time sin sobre-cobrar. */
	private long _lastTimeTickMs;
	/** Proximo slot a probar (rotacion, para no repetir siempre el primero). */
	private int _nextSlot = 0;
	
	public AutofarmPlayerRoutine(Player player)
	{
		this.player = player;
	}
	
	public void start()
	{
		if (!Config.AUTOFARM_ENABLED)
			return;
		
		synchronized (this)
		{
			if (_task != null)
				return;
			
			committedTarget = null;
			_nextSlot = 0;
			_lastTimeTickMs = System.currentTimeMillis();
			_task = ThreadPool.scheduleAtFixedRate(this::executeRoutine, TICK_MS, TICK_MS);
			player.sendPacket(new ExShowScreenMessage("Haunting Activated...", 5 * 1000, SMPOS.TOP_CENTER, false));
		}
	}
	
	public void stop()
	{
		stop(true);
	}
	
	public void stop(boolean notify)
	{
		synchronized (this)
		{
			if (_task == null)
				return;
			
			_task.cancel(false);
			_task = null;
			committedTarget = null;
			_lastLeader = null;
			
			if (notify)
				player.sendPacket(new ExShowScreenMessage("Haunting Desactivated...", 5 * 1000, SMPOS.TOP_CENTER, false));
			VoicedAutoFarm.LimpiarListaFarm(player);
		}
	}
	
	public void executeRoutine()
	{
		if (player.isDead() || player.isAlikeDead())
		{
			VoicedAutoFarm.stopAutoFarm(player, "moriste");
			return;
		}
		
		if (player.isInsideZone(ZoneId.PEACE))
		{
			VoicedAutoFarm.stopAutoFarm(player, "zona peace");
			return;
		}
		
		final Player blocker = VoicedAutoFarm.findAutoFarmBlocker(player);
		if (blocker != null && player.getObjectId() > blocker.getObjectId())
		{
			VoicedAutoFarm.stopAutoFarm(player, blocker.getName() + " ya farmea en esta PC");
			return;
		}
		
		// Descontar el tiempo REAL transcurrido (AutoFarm por tiempo).
		final long now = System.currentTimeMillis();
		long elapsed = now - _lastTimeTickMs;
		_lastTimeTickMs = now;
		if (elapsed < 0 || elapsed > 10000L)
			elapsed = TICK_MS;
		
		if (!player.tickAutoFarmTime((int) elapsed))
		{
			VoicedAutoFarm.stopAutoFarm(player, "se te acabo el tiempo");
			return;
		}
		
		targetEligibleCreature();
		
		if (!useConfiguredSkill())
		{
			if (!player.isMageClass())
				physicalAttack();
		}
	}
	
	/**
	 * Cast first usable configured slot (priority = slot order 0..6).
	 * @return true if a skill was issued
	 */
	private boolean useConfiguredSkill()
	{
		final AutofarmSkillSlot[] slots = player.getAutoFarmSkillSlots();
		if (slots.length == 0)
			return false;
		
		// Si ya esta casteando, no mandamos otro cast (antes el pedido se perdia y parecia
		// que tiraba cada 1 segundo en vez de seguir el reuse real de la skill).
		if (player.isCastingNow())
			return true;
		
		// Rotacion: arranca por el proximo slot y da la vuelta. Antes arrancaba siempre por el 0.
		for (int k = 0; k < slots.length; k++)
		{
			final int index = (_nextSlot + k) % slots.length;
			final AutofarmSkillSlot slot = slots[index];
			if (slot == null || slot.isEmpty() || slot.isOnReuse())
				continue;
			if (!slot.matchesUserHp(player))
				continue;
			
			final L2Skill skill = player.getSkill(slot.getSkillId());
			if (skill == null)
				continue;
			if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
				continue;
			if (skill.isToggle() || skill.isPassive())
				continue;
			// Reuse REAL del server: es lo que faltaba, por eso repetia siempre el mismo slot.
			if (player.isSkillDisabled(skill))
				continue;
			if (!player.checkDoCastConditions(skill))
				continue;
			
			final boolean forceOnSelf = !skill.isOffensive();
			if (forceOnSelf)
			{
				// Buff sobre si mismo: si ya lo tiene puesto, no lo recastea en cada vuelta.
				if (player.getFirstEffect(skill.getId()) != null)
					continue;
			}
			else
			{
				final Monster target = getMonsterTarget();
				if (target == null || !slot.matchesTargetHp(target))
					continue;
				if (skill.isDebuff() && target.getFirstEffect(skill.getId()) != null)
					continue;
			}
			
			useMagicSkill(skill, forceOnSelf);
			slot.markUsed();
			_nextSlot = (index + 1) % slots.length;
			return true;
		}
		
		return false;
	}
	
	private void castSpellWithAppropriateTarget(L2Skill skill, Boolean forceOnSelf)
	{
		if (forceOnSelf)
		{
			WorldObject oldTarget = player.getTarget();
			player.setTarget(player);
			player.useMagic(skill, false, false);
			player.setTarget(oldTarget);
			return;
		}
		player.useMagic(skill, false, false);
	}
	
	private void physicalAttack()
	{
		if (!(player.getTarget() instanceof Monster))
			return;
		
		Monster target = (Monster) player.getTarget();
		if (player.isMageClass())
			return;
		
		if (target.isAutoAttackable(player) && GeoEngine.getInstance().canSeeTarget(player, target))
		{
			player.getAI().setIntention(IntentionType.ATTACK, target);
			player.onActionRequest();
		}
		else if (GeoEngine.getInstance().canSeeTarget(player, target))
			player.getAI().setIntention(IntentionType.FOLLOW, target);
	}
	
	public void targetEligibleCreature()
	{
		// Si el target que muere es un jefe (no minion), lo recordamos para ir despues por sus minions.
		if (player.getTarget() instanceof Monster)
		{
			final Monster current = (Monster) player.getTarget();
			if (current.isDead() && !current.isMinion())
				_lastLeader = current;
		}
		
		if (!(player.getTarget() instanceof Monster) || ((Monster) player.getTarget()).isDead())
		{
			committedTarget = null;
			selectNewTarget();
			return;
		}
		
		if (committedTarget != null)
		{
			if (!committedTarget.isDead() && GeoEngine.getInstance().canSeeTarget(player, committedTarget))
			{
				if (player.getTarget() != committedTarget)
					player.setTarget(committedTarget);
				if (!player.isMageClass())
					physicalAttack();
				return;
			}
			else if (!GeoEngine.getInstance().canSeeTarget(player, committedTarget))
			{
				committedTarget = null;
				selectNewTarget();
				return;
			}
			committedTarget = null;
			player.setTarget(null);
			selectNewTarget();
			return;
		}
		
		if (player.getTarget() instanceof Monster)
		{
			Monster current = (Monster) player.getTarget();
			if (!current.isDead() && GeoEngine.getInstance().canSeeTarget(player, current) && !(current instanceof Chest))
			{
				committedTarget = current;
				if (!player.isMageClass())
					physicalAttack();
				return;
			}
		}
		
		selectNewTarget();
	}
	
	private void selectNewTarget()
	{
		// Candidatos: alcanzables, vivos, sin raids ni chests. Minions SOLO si su jefe ya murio (o no tienen jefe).
		List<Monster> targets = getKnownMonstersInRadius(player, player.getRadius(), creature -> GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ()) && !creature.isRaidRelated() && !creature.isDead() && !(creature instanceof Chest) && (!creature.isMinion() || isOrphanMinion(creature)));
		
		if (targets.isEmpty())
		{
			_lastLeader = null;
			return;
		}
		
		// Prioridad: 1) minions del jefe que acabamos de matar, 2) otros minions sin jefe, 3) mobs normales.
		final Monster leader = _lastLeader;
		Monster best = null;
		int bestPriority = Integer.MAX_VALUE;
		double bestDist = Double.MAX_VALUE;
		for (Monster m : targets)
		{
			final int priority;
			if (m.isMinion())
				priority = (leader != null && m.getMaster() == leader) ? 0 : 1;
			else
				priority = 2;
			
			final double dist = player.getDistanceSq(m);
			if (priority < bestPriority || (priority == bestPriority && dist < bestDist))
			{
				best = m;
				bestPriority = priority;
				bestDist = dist;
			}
		}
		
		// Si ya no quedan minions de ese jefe, lo olvidamos.
		if (bestPriority != 0)
			_lastLeader = null;
		
		committedTarget = best;
		player.setTarget(best);
	}
	
	/**
	 * @return true si es un minion cuyo jefe ya murio (o no tiene jefe).
	 */
	private static boolean isOrphanMinion(Monster minion)
	{
		final Monster master = minion.getMaster();
		return master == null || master.isDead();
	}
	
	public final static List<Monster> getKnownMonstersInRadius(Player player, int radius, Function<Monster, Boolean> condition)
	{
		final WorldRegion region = player.getRegion();
		if (region == null)
			return Collections.emptyList();
		
		final List<Monster> result = new ArrayList<>();
		for (WorldRegion reg : region.getSurroundingRegions())
		{
			for (WorldObject obj : reg.getObjects())
			{
				if (!(obj instanceof Monster) || player.getInstance() != obj.getInstance() || !MathUtil.checkIfInRange(radius, player, obj, true) || !condition.apply((Monster) obj))
					continue;
				result.add((Monster) obj);
			}
		}
		return result;
	}
	
	public Monster getMonsterTarget()
	{
		if (!(player.getTarget() instanceof Monster))
			return null;
		return (Monster) player.getTarget();
	}
	
	private void useMagicSkill(L2Skill skill, Boolean forceOnSelf)
	{
		if (skill.getSkillType() == L2SkillType.RECALL && !Config.KARMA_PLAYER_CAN_TELEPORT && player.getKarma() > 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (skill.isToggle() && player.isMounted())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isAttackingNow())
			player.getAI().setNextAction(new NextAction(AiEventType.READY_TO_ACT, IntentionType.CAST, () -> castSpellWithAppropriateTarget(skill, forceOnSelf)));
		else
			castSpellWithAppropriateTarget(skill, forceOnSelf);
	}
}
