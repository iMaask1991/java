package net.sf.l2j.gameserver.skills;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.effects.EffectHide;

/**
 * Efectos de High Five (L2J Mobius) que el motor de skills de aCis no tiene, portados para SanSara.
 * <ul>
 * <li><b>h5Instant</b> / <b>h5SelfInstant</b>: efectos instantaneos extra sobre cada objetivo / sobre el que castea.<br>
 * Formato "TIPO:valor;TIPO:valor1,valor2". Tipos: HEAL, HEAL_PERCENT, MANA_HEAL, MANA_HEAL_PERCENT, CP_HEAL, CP_HEAL_PERCENT,
 * CP_DAM_PERCENT, DISPEL_BUFF (rate,max), DISPEL_DEBUFF (rate,max), DISPEL_SKILL (skillId), HEAL_MAXHP_GAIN, DETECTION (radio).</li>
 * <li><b>callSkills</b> / <b>selfCallSkills</b>: CallSkill de H5 ("id-lvl;id-lvl") sobre cada objetivo / sobre uno mismo.</li>
 * <li><b>delayedCallSkill</b>: endEffects CallSkill de H5 ("id-lvl,ms"), por ejemplo la explosion de Spoil Bomb.</li>
 * <li><b>dispelStackTypes</b>: DispelBySlot de H5 (saca otros efectos con esos stackType).</li>
 * <li><b>blockStackTypes</b>: BlockAbnormalSlot de H5 (mientras el efecto esta activo no entran esos stackType).</li>
 * <li><b>h5TriggerBySkill</b>: TriggerSkillBySkill de H5 en pasivas ("castSkillId,chance,skillId,skillLvl").</li>
 * </ul>
 */
public final class H5Skills
{
	private static final Logger LOGGER = Logger.getLogger(H5Skills.class.getName());

	private static final Map<String, Set<String>> SET_CACHE = new ConcurrentHashMap<>();
	/** Todos los stackType que alguna skill puede bloquear (para no revisar efectos en cada buff que entra). */
	private static final Set<String> BLOCKABLE = ConcurrentHashMap.newKeySet();
	private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

	private H5Skills()
	{
	}

	// ------------------------------------------------------------------
	// maxRecoverable (Seal of Limit / Servitor Share)
	// ------------------------------------------------------------------

	public static double getMaxRecoverableHp(Creature c)
	{
		return c.calcStat(Stats.MAX_RECOVERABLE_HP, c.getMaxHp(), null, null);
	}

	public static double getMaxRecoverableMp(Creature c)
	{
		return c.calcStat(Stats.MAX_RECOVERABLE_MP, c.getMaxMp(), null, null);
	}

	public static double getMaxRecoverableCp(Creature c)
	{
		return c.calcStat(Stats.MAX_RECOVERABLE_CP, c.getMaxCp(), null, null);
	}

	// ------------------------------------------------------------------
	// BlockAbnormalSlot
	// ------------------------------------------------------------------

	private static Set<String> parseSet(String value)
	{
		if (value == null || value.isEmpty())
			return Collections.emptySet();

		return SET_CACHE.computeIfAbsent(value, v ->
		{
			final Set<String> set = new HashSet<>();
			for (String s : v.split("[;,\\s]+"))
				if (!s.isEmpty())
					set.add(s.trim().toLowerCase());
			return set;
		});
	}

	/**
	 * Lo llama L2Skill al cargar una skill con blockStackTypes.
	 * @param value
	 */
	public static void registerBlock(String value)
	{
		BLOCKABLE.addAll(parseSet(value));
	}
	
	/**
	 * @param owner quien recibe el efecto.
	 * @param newEffect el efecto que entra.
	 * @return true si algun efecto activo de otra skill bloquea el stackType del efecto nuevo (H5 BlockAbnormalSlot).
	 */
	public static boolean isStackBlocked(Creature owner, L2Effect newEffect)
	{
		if (owner == null || newEffect == null)
			return false;

		final String stackType = newEffect.getStackType();
		if (stackType == null || "none".equals(stackType))
			return false;

		final String type = stackType.toLowerCase();
		if (!BLOCKABLE.contains(type))
			return false;

		final L2Effect[] effects = owner.getAllEffects();
		if (effects == null)
			return false;

		for (L2Effect e : effects)
		{
			if (e == null || e == newEffect || e.getSkill().getBlockStackTypes() == null)
				continue;

			if (e.getSkill().getId() == newEffect.getSkill().getId())
				continue;

			if (parseSet(e.getSkill().getBlockStackTypes()).contains(type))
				return true;
		}
		return false;
	}

	/**
	 * H5 AURA_FRIENDLY: saca al que castea de la lista de objetivos (skill con excludeSelf).
	 * @param caster
	 * @param skill
	 * @param targets
	 * @return la lista filtrada.
	 */
	public static WorldObject[] filterTargets(Creature caster, L2Skill skill, WorldObject[] targets)
	{
		if (skill == null || targets == null || !skill.isExcludeSelf())
			return targets;
		
		final java.util.List<WorldObject> list = new java.util.ArrayList<>(targets.length);
		for (WorldObject o : targets)
			if (o != caster)
				list.add(o);
		return list.toArray(new WorldObject[0]);
	}
	
	// ------------------------------------------------------------------
	// Hook de Creature.callSkill
	// ------------------------------------------------------------------

	/**
	 * Se llama antes de lanzar la skill. Guarda el Max HP de cada objetivo para HEAL_MAXHP_GAIN.
	 * @param caster
	 * @param skill
	 * @param targets
	 * @return mapa objetivo -> Max HP antes, o null si la skill no lo necesita.
	 */
	public static Map<Creature, Integer> before(Creature caster, L2Skill skill, WorldObject[] targets)
	{
		if (skill == null || targets == null)
			return null;

		final String inst = skill.getH5Instant();
		if (inst == null || !inst.contains("HEAL_MAXHP_GAIN"))
			return null;

		final Map<Creature, Integer> map = new HashMap<>();
		for (WorldObject obj : targets)
			if (obj instanceof Creature)
				map.put((Creature) obj, ((Creature) obj).getMaxHp());
		return map;
	}

	/**
	 * Se llama despues de lanzar la skill (handler ya aplicado).
	 * @param caster
	 * @param skill
	 * @param targets
	 * @param pre datos de {@link #before}.
	 */
	public static void after(Creature caster, L2Skill skill, WorldObject[] targets, Map<Creature, Integer> pre)
	{
		if (caster == null || skill == null)
			return;

		final int[] depth = DEPTH.get();
		if (depth[0] > 3) // evita loops de skills que se llaman entre si
			return;

		depth[0]++;
		try
		{
			if (skill.hasH5Extras() && targets != null)
			{
				final Set<String> dispel = parseSet(skill.getDispelStackTypes());
				final int[][] calls = parseSkills(skill.getCallSkills());

				for (WorldObject obj : targets)
				{
					if (!(obj instanceof Creature))
						continue;

					final Creature target = (Creature) obj;
					if (target.isDead() || target instanceof Door)
						continue;

					// En H5 los efectos extra de una skill ofensiva solo van si la skill entro (el debuff principal esta puesto).
					if (!landed(skill, target))
						continue;

					if (!dispel.isEmpty())
						dispelStack(target, dispel, skill.getId());

					if (skill.getH5Instant() != null)
						applyInstant(caster, target, skill, skill.getH5Instant(), pre);

					for (int[] c : calls)
						castInstant(caster, SkillTable.getInstance().getInfo(c[0], c[1]), target, false);
				}

				if (skill.getDelayedCallSkill() != null)
					scheduleDelayed(caster, skill, targets);
			}

			if (skill.getH5SelfInstant() != null)
				applyInstant(caster, caster, skill, skill.getH5SelfInstant(), null);

			for (int[] c : parseSkills(skill.getSelfCallSkills()))
				castInstant(caster, SkillTable.getInstance().getInfo(c[0], c[1]), caster);

			if (caster instanceof Player && !skill.isPassive())
				triggerBySkill((Player) caster, skill);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "H5Skills: error en extras de la skill " + skill.getId(), e);
		}
		finally
		{
			depth[0]--;
		}
	}

	private static boolean landed(L2Skill skill, Creature target)
	{
		if (!skill.hasEffects() || !(skill.isOffensive() || skill.isDebuff()))
			return true;

		return target.getFirstEffect(skill.getId()) != null;
	}

	// ------------------------------------------------------------------
	// CallSkill
	// ------------------------------------------------------------------

	private static int[][] parseSkills(String value)
	{
		if (value == null || value.isEmpty())
			return new int[0][];

		final String[] parts = value.split(";");
		final int[][] res = new int[parts.length][];
		int n = 0;
		for (String p : parts)
		{
			p = p.trim();
			if (p.isEmpty())
				continue;
			final String[] il = p.split("-");
			res[n++] = new int[]
			{
				Integer.parseInt(il[0].trim()),
				(il.length > 1) ? Integer.parseInt(il[1].trim()) : 1
			};
		}
		final int[][] out = new int[n][];
		System.arraycopy(res, 0, out, 0, n);
		return out;
	}

	/**
	 * Lanza una skill al instante (como el makeTriggerCast de H5), con su animacion y sin gastar MP ni reuse.
	 * @param caster
	 * @param skill
	 * @param target
	 */
	public static void castInstant(Creature caster, L2Skill skill, Creature target)
	{
		castInstant(caster, skill, target, true);
	}

	/**
	 * @param caster
	 * @param skill
	 * @param target
	 * @param animate false = sin MagicSkillUse (el cliente Interlude corta el efecto de la skill que se esta lanzando si llega otro MagicSkillUse del mismo personaje).
	 */
	public static void castInstant(Creature caster, L2Skill skill, Creature target, boolean animate)
	{
		if (caster == null || skill == null || caster.isDead() || skill.getSkillType() == L2SkillType.NOTDONE)
			return;

		final WorldObject[] list = skill.getTargetList(caster, false, (target != null) ? target : caster);
		if (list == null || list.length == 0)
			return;

		final Creature first = (list[0] instanceof Creature) ? (Creature) list[0] : caster;
		if (animate)
			caster.broadcastPacket(new MagicSkillUse(caster, first, skill.getId(), skill.getLevel(), 0, 0));
		caster.callSkill(skill, list);
	}

	private static void scheduleDelayed(Creature caster, L2Skill skill, WorldObject[] targets)
	{
		final String[] v = skill.getDelayedCallSkill().split(",");
		final int[][] sk = parseSkills(v[0]);
		if (sk.length == 0)
			return;

		final long delay = (v.length > 1) ? Long.parseLong(v[1].trim()) : 10000;
		final L2Skill called = SkillTable.getInstance().getInfo(sk[0][0], sk[0][1]);
		if (called == null)
			return;

		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature))
				continue;

			final Creature target = (Creature) obj;

			// Spoil Bomb: solo explotan los que quedaron spoileados por este jugador.
			if (skill.getSkillType() == L2SkillType.SPOIL && (!(target instanceof Monster) || ((Monster) target).getSpoilerId() != caster.getObjectId()))
				continue;

			ThreadPool.schedule(() ->
			{
				if (!target.isDead() && !caster.isDead() && target.isVisible())
					castInstant(caster, called, target);
			}, delay);
		}
	}

	// ------------------------------------------------------------------
	// TriggerSkillBySkill
	// ------------------------------------------------------------------

	private static void triggerBySkill(Player player, L2Skill used)
	{
		for (L2Skill passive : player.getSkills().values())
		{
			if (passive == null || passive.getH5TriggerBySkill() == null)
				continue;

			final String[] v = passive.getH5TriggerBySkill().split(",");
			if (v.length < 3 || Integer.parseInt(v[0].trim()) != used.getId())
				continue;

			if (Rnd.get(100) >= Integer.parseInt(v[1].trim()))
				continue;

			final L2Skill trig = SkillTable.getInstance().getInfo(Integer.parseInt(v[2].trim()), (v.length > 3) ? Integer.parseInt(v[3].trim()) : 1);
			if (trig != null && passive.checkCondition(player, player, false))
				castInstant(player, trig, player);
		}
	}

	// ------------------------------------------------------------------
	// DispelBySlot / DispelByCategory
	// ------------------------------------------------------------------

	private static void dispelStack(Creature target, Set<String> types, int ownSkillId)
	{
		for (L2Effect e : target.getAllEffects())
		{
			if (e == null || e.getSkill().getId() == ownSkillId || e.getStackType() == null)
				continue;

			if (types.contains(e.getStackType().toLowerCase()))
				e.exit();
		}
	}

	private static void dispelSkill(Creature target, int skillId)
	{
		for (L2Effect e : target.getAllEffects())
			if (e != null && e.getSkill().getId() == skillId)
				e.exit();
	}

	/** H5 DispelByCategory slot "buff" (Formulas.calcCancelStealEffects de Mobius). */
	private static void dispelBuffs(Creature caster, Creature target, double rate, int max)
	{
		// En SanSara cancelVuln es un multiplicador (1 = normal, addMul 40 = 0.6): resistencia a cancel de H5.
		final double vuln = target.calcStat(Stats.CANCEL_VULN, 1, target, null);
		final double finalRate = Math.max(0, Math.min(100, rate * vuln));

		int count = 0;
		int lastSkill = 0;
		final L2Effect[] effects = target.getAllEffects();
		for (int i = effects.length - 1; i >= 0 && count < max; i--)
		{
			final L2Effect e = effects[i];
			if (e == null || e.getSkill().isDebuff() || e.getSkill().isToggle() || e.getSkill().isPassive() || !e.getSkill().canBeDispeled())
				continue;

			if (e.getSkill().getId() == lastSkill)
				continue;

			if (Rnd.get(100) < finalRate)
			{
				lastSkill = e.getSkill().getId();
				target.stopSkillEffects(lastSkill);
				count++;
			}
		}
	}

	/** H5 DispelByCategory slot "debuff": cada debuff sale con "rate"% hasta "max". */
	private static void dispelDebuffs(Creature target, double rate, int max)
	{
		int count = 0;
		int lastSkill = 0;
		for (L2Effect e : target.getAllEffects())
		{
			if (count >= max)
				break;
			
			if (e == null || !e.getSkill().isDebuff() || !e.getSkill().canBeDispeled() || e.getSkill().getId() == lastSkill)
				continue;
			
			if (Rnd.get(100) < rate)
			{
				lastSkill = e.getSkill().getId();
				target.stopSkillEffects(lastSkill);
				count++;
			}
		}
	}
	
	// ------------------------------------------------------------------
	// Efectos instantaneos
	// ------------------------------------------------------------------

	private static void applyInstant(Creature caster, Creature target, L2Skill skill, String spec, Map<Creature, Integer> pre)
	{
		for (String part : spec.split(";"))
		{
			part = part.trim();
			if (part.isEmpty())
				continue;

			final int sep = part.indexOf(':');
			final String type = ((sep < 0) ? part : part.substring(0, sep)).trim().toUpperCase();
			final String[] vals = (sep < 0) ? new String[0] : part.substring(sep + 1).split(",");
			final double v = (vals.length > 0) ? Double.parseDouble(vals[0].trim()) : 0;

			switch (type)
			{
				case "HEAL":
					heal(caster, target, (v + Math.sqrt(caster.getMAtk(target, skill))) * target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100.);
					break;

				case "HEAL_PERCENT":
					heal(caster, target, target.getMaxHp() * v / 100.);
					break;

				case "HEAL_MAXHP_GAIN":
					if (pre != null && pre.containsKey(target))
						heal(caster, target, Math.max(0, target.getMaxHp() - pre.get(target)));
					break;

				case "MANA_HEAL":
					manaHeal(caster, target, target.calcStat(Stats.RECHARGE_MP_RATE, v, null, null));
					break;

				case "MANA_HEAL_PERCENT":
					manaHeal(caster, target, target.getMaxMp() * v / 100.);
					break;

				case "MANA_HEAL_BY_LEVEL":
				{
					// Mobius ManaHealByLevel: penalidad si el objetivo tiene mas nivel que la skill.
					double amount = target.calcStat(Stats.RECHARGE_MP_RATE, v, null, null);
					final int diff = target.getLevel() - skill.getMagicLevel();
					if (skill.getMagicLevel() > 0 && diff > 5)
						amount = (diff < 20) ? amount * (1 - (diff - 5) * 0.05) : amount * 0.1;
					manaHeal(caster, target, amount);
					break;
				}

				case "CP_HEAL":
					cpHeal(caster, target, v);
					break;

				case "CP_HEAL_PERCENT":
					cpHeal(caster, target, target.getMaxCp() * v / 100.);
					break;

				case "CP_DAM_PERCENT":
					if (target instanceof Player && !target.isInvul())
					{
						final double dmg = target.getCurrentCp() * v / 100.;
						if (dmg > 0)
						{
							target.setCurrentCp(target.getCurrentCp() - dmg);
							Formulas.calcCastBreak(target, dmg);
						}
					}
					break;

				case "DISPEL_BUFF":
					if (!target.isInvul())
						dispelBuffs(caster, target, v, (vals.length > 1) ? Integer.parseInt(vals[1].trim()) : 1);
					break;

				case "DISPEL_DEBUFF":
					dispelDebuffs(target, v, (vals.length > 1) ? Integer.parseInt(vals[1].trim()) : 1);
					break;
				
				case "DISPEL_SKILL":
					for (String id : vals)
						dispelSkill(target, Integer.parseInt(id.trim()));
					break;

				case "DETECTION":
					detection(caster, (int) ((v > 0) ? v : 300));
					break;

				default:
					LOGGER.warning("H5Skills: tipo desconocido '" + type + "' en la skill " + skill.getId());
			}
		}
	}

	private static boolean canBeHealed(Creature target)
	{
		return !target.isDead() && !(target instanceof Door);
	}

	private static void heal(Creature caster, Creature target, double amount)
	{
		if (!canBeHealed(target))
			return;

		final double max = Math.min(target.getMaxHp(), getMaxRecoverableHp(target));
		amount = Math.max(0, Math.min(amount, max - target.getCurrentHp()));
		if (amount > 0)
			target.setCurrentHp(target.getCurrentHp() + amount);

		final StatusUpdate su = new StatusUpdate(target);
		su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
		target.sendPacket(su);

		if (caster instanceof Player && caster != target)
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1).addCharName(caster).addNumber((int) amount));
		else
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED).addNumber((int) amount));
	}

	private static void manaHeal(Creature caster, Creature target, double amount)
	{
		if (!canBeHealed(target))
			return;

		final double max = Math.min(target.getMaxMp(), getMaxRecoverableMp(target));
		amount = Math.max(0, Math.min(amount, max - target.getCurrentMp()));
		if (amount > 0)
			target.setCurrentMp(target.getCurrentMp() + amount);

		final StatusUpdate su = new StatusUpdate(target);
		su.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
		target.sendPacket(su);

		if (caster instanceof Player && caster != target)
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_MP_RESTORED_BY_S1).addCharName(caster).addNumber((int) amount));
		else
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_MP_RESTORED).addNumber((int) amount));
	}

	private static void cpHeal(Creature caster, Creature target, double amount)
	{
		if (!(target instanceof Player) || !canBeHealed(target))
			return;

		final double max = Math.min(target.getMaxCp(), getMaxRecoverableCp(target));
		amount = Math.max(0, Math.min(amount, max - target.getCurrentCp()));
		if (amount > 0)
			target.setCurrentCp(target.getCurrentCp() + amount);

		final StatusUpdate su = new StatusUpdate(target);
		su.addAttribute(StatusUpdate.CUR_CP, (int) target.getCurrentCp());
		target.sendPacket(su);

		if (caster instanceof Player && caster != target)
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_CP_WILL_BE_RESTORED_BY_S1).addCharName(caster).addNumber((int) amount));
		else
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED).addNumber((int) amount));
	}

	/** H5 Detection: saca el Hide a los enemigos cercanos. */
	private static void detection(Creature caster, int radius)
	{
		final Player me = caster.getActingPlayer();
		for (Player p : caster.getKnownTypeInRadius(Player.class, radius))
		{
			if (p == me)
				continue;

			if (me != null && me.getParty() != null && me.getParty().getMembers().contains(p))
				continue;

			for (L2Effect e : p.getAllEffects())
				if (e instanceof EffectHide)
					e.exit();
		}
	}
}
