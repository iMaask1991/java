package net.sf.l2j.gameserver.element;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * Atributos elementales como High Five: todo son PUNTOS fijos (no porcentajes).
 * <ul>
 * <li>Ataque: un solo elemento. Primero el del arma (piedra); si el arma no tiene, el elemento con mas puntos de los buffs (Holy Weapon, Seed of Fire...). Los NPC usan el de su template.</li>
 * <li>Defensa: un valor por elemento = piedras de la armadura + buffs/pasivas (fireRes, darkRes...). Jugador base 0, NPC base 20 (o lo que diga su template H5).</li>
 * <li>Skill con elemento: solo suma si tu ataque es del MISMO elemento que la skill (ataque + elementPower de la skill, normalmente 20).</li>
 * <li>Si tu ataque es menor o igual a la defensa del otro: dano normal (x1). Si es mayor: dano extra segun la diferencia (maximo x2).</li>
 * <li>Entre jugadores nunca baja de x1: la resistencia solo te saca el dano extra del elemento.</li>
 * </ul>
 * Ids de elemento del server: 1 wind, 2 fire, 3 water, 4 earth, 5 holy, 6 dark.
 */
public final class ElementAttributes
{
	public static final byte WIND = 1;
	public static final byte FIRE = 2;
	public static final byte WATER = 3;
	public static final byte EARTH = 4;
	public static final byte HOLY = 5;
	public static final byte DARK = 6;

	private ElementAttributes()
	{
	}

	/**
	 * @param type : elemento de items (piedras).
	 * @return el id de elemento del server.
	 */
	public static byte toServer(ElementType type)
	{
		if (type == null)
			return -1;

		switch (type)
		{
			case FIRE:
				return FIRE;
			case WATER:
				return WATER;
			case WIND:
				return WIND;
			case EARTH:
				return EARTH;
			case HOLY:
				return HOLY;
			default:
				return DARK;
		}
	}

	/**
	 * @param element : id de elemento del server.
	 * @return el elemento de items, o null.
	 */
	public static ElementType toItem(byte element)
	{
		switch (element)
		{
			case FIRE:
				return ElementType.FIRE;
			case WATER:
				return ElementType.WATER;
			case WIND:
				return ElementType.WIND;
			case EARTH:
				return ElementType.EARTH;
			case HOLY:
				return ElementType.HOLY;
			case DARK:
				return ElementType.DARK;
			default:
				return null;
		}
	}

	/**
	 * @param text : FIRE, WATER, WIND, EARTH, HOLY, DARK.
	 * @return el id de elemento del server, o -1.
	 */
	public static byte parseElement(String text)
	{
		return toServer(ElementType.parse(text));
	}

	// ------------------------------------------------------------------------
	// Valores base (sin buffs): template del NPC o piedras del jugador
	// ------------------------------------------------------------------------

	private static NpcAttributeData.NpcAttribute getNpcAttribute(Creature creature)
	{
		return (creature.getTemplate() instanceof NpcTemplate) ? NpcAttributeData.getInstance().get(((NpcTemplate) creature.getTemplate()).getNpcId()) : null;
	}

	/**
	 * @param creature : el personaje.
	 * @return el elemento de ataque "fijo": piedra del arma (jugador) o template (NPC). -1 si no tiene.
	 */
	public static byte getBaseAttackElement(Creature creature)
	{
		if (creature instanceof Player)
		{
			final ItemElement weapon = ItemElementManager.getInstance().getAttackElement((Player) creature);
			return (weapon == null || weapon.getValue() <= 0) ? -1 : toServer(weapon.getElement());
		}

		final NpcAttributeData.NpcAttribute a = getNpcAttribute(creature);
		return (a == null) ? -1 : a.attackElement;
	}

	/**
	 * @param creature : el personaje.
	 * @param element : id de elemento del server.
	 * @return los puntos de ataque base de ese elemento (piedra del arma o template).
	 */
	public static int getBaseAttack(Creature creature, byte element)
	{
		if (creature instanceof Player)
		{
			final ItemElement weapon = ItemElementManager.getInstance().getAttackElement((Player) creature);
			return (weapon != null && toServer(weapon.getElement()) == element) ? weapon.getValue() : 0;
		}

		final NpcAttributeData.NpcAttribute a = getNpcAttribute(creature);
		return (a != null && a.attackElement == element) ? a.attackValue : 0;
	}

	/**
	 * @param creature : el personaje.
	 * @param element : id de elemento del server.
	 * @return los puntos de defensa base contra ese elemento (piedras de la armadura o template). Jugador 0, NPC 20.
	 */
	public static int getBaseDefense(Creature creature, byte element)
	{
		if (element < 1 || element > 6)
			return 0;

		if (creature instanceof Player)
			return ItemElementManager.getInstance().getArmorDefense((Player) creature, toItem(element));

		if (creature.getTemplate() instanceof NpcTemplate)
		{
			final NpcAttributeData.NpcAttribute a = getNpcAttribute(creature);
			final int base = (a == null) ? NpcAttributeData.DEFAULT_DEFENSE : a.defense[element];
			if (Config.NPC_ELEMENT_DEFENSE_RATE == 1.0f && Config.NPC_ELEMENT_DEFENSE_BONUS == 0 && !Config.NPC_ELEMENT_DEFENSE_MIN_ENABLED)
				return base;
			
			int value = (int) Math.round(base * (double) Config.NPC_ELEMENT_DEFENSE_RATE) + Config.NPC_ELEMENT_DEFENSE_BONUS;
			if (Config.NPC_ELEMENT_DEFENSE_MIN_ENABLED)
				value = Math.max(Config.NPC_ELEMENT_DEFENSE_MIN, value);
			return value;
		}
		return 0;
	}

	// ------------------------------------------------------------------------
	// Formula H5 (calcAttributeBonus)
	// ------------------------------------------------------------------------

	/**
	 * Multiplicador de dano por elementos, igual que High Five.
	 * @param attacker : el que pega.
	 * @param target : el que recibe.
	 * @param skill : la skill, o null si es golpe normal.
	 * @return el multiplicador (1 = sin cambio, maximo 2).
	 */
	public static double calcAttributeBonus(Creature attacker, Creature target, L2Skill skill)
	{
		if (attacker == null || target == null)
			return 1;

		final byte attackElement = attacker.getAttackElement();
		int attack;
		if (skill != null)
		{
			// La skill solo cuenta si tu ataque es del mismo elemento
			if (skill.getElement() <= 0 || attackElement != skill.getElement())
				return 1;

			attack = attacker.getAttackElementValue(attackElement) + skill.getElementPower();
		}
		else
		{
			if (attackElement <= 0)
				return 1;

			attack = attacker.getAttackElementValue(attackElement);
			if (attack == 0)
				return 1;
		}

		int defence = (int) target.getDefenseElementValue(attackElement);
		if (attack <= defence)
			return 1;

		double attackMod;
		double defenceMod;
		if (attack >= 450)
		{
			if (defence >= 450)
			{
				attackMod = 0.06909;
				defenceMod = 0.078;
			}
			else if (defence >= 350)
			{
				attackMod = 0.0887;
				defenceMod = 0.1007;
			}
			else
			{
				attackMod = 0.129;
				defenceMod = 0.1473;
			}
		}
		else if (attack >= 300)
		{
			if (defence >= 300)
			{
				attackMod = 0.0887;
				defenceMod = 0.1007;
			}
			else if (defence >= 150)
			{
				attackMod = 0.129;
				defenceMod = 0.1473;
			}
			else
			{
				attackMod = 0.25;
				defenceMod = 0.2894;
			}
		}
		else if (attack >= 150)
		{
			if (defence >= 150)
			{
				attackMod = 0.129;
				defenceMod = 0.1473;
			}
			else if (defence >= 0)
			{
				attackMod = 0.25;
				defenceMod = 0.2894;
			}
			else
			{
				attackMod = 0.4;
				defenceMod = 0.55;
			}
		}
		else if (attack >= -99)
		{
			if (defence >= 0)
			{
				attackMod = 0.25;
				defenceMod = 0.2894;
			}
			else
			{
				attackMod = 0.4;
				defenceMod = 0.55;
			}
		}
		else
		{
			if (defence >= 450)
			{
				attackMod = 0.06909;
				defenceMod = 0.078;
			}
			else if (defence >= 350)
			{
				attackMod = 0.0887;
				defenceMod = 0.1007;
			}
			else
			{
				attackMod = 0.129;
				defenceMod = 0.1473;
			}
		}

		final int diff = attack - defence;
		final double max;
		final double min;
		if (diff >= 300)
		{
			max = 100.0;
			min = -50;
		}
		else if (diff >= 150)
		{
			max = 70.0;
			min = -50;
		}
		else if (diff >= -150)
		{
			max = 40.0;
			min = -50;
		}
		else if (diff >= -300)
		{
			max = 40.0;
			min = -60;
		}
		else
		{
			max = 40.0;
			min = -80;
		}

		attack += 100;
		attackMod *= (attack * (double) attack) / 144.0;

		defence += 100;
		defenceMod *= (defence * (double) defence) / 169.0;

		final double modDiff = Math.max(min, Math.min(max, attackMod - defenceMod));
		double result = (modDiff / 100.0) + 1;

		if (attacker instanceof Playable && target instanceof Playable && result < 1.0)
			result = 1.0;

		return result;
	}
}
