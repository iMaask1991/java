package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.List;

import net.sf.l2j.gameserver.element.ElementType;
import net.sf.l2j.gameserver.element.ItemElement;
import net.sf.l2j.gameserver.element.ItemElementData;
import net.sf.l2j.gameserver.element.ItemElementManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

/**
 * Elementos de items - comandos de GM para testear. Trabajan sobre lo que tenes equipado.
 * <ul>
 * <li>//setelement &lt;elemento&gt; &lt;valor&gt; [parte] : pone el elemento a mano (por defecto en el arma). En armadura agrega o actualiza ese elemento (hasta 3, sin opuestos). Valor 0 lo saca.</li>
 * <li>//clearelement &lt;parte|all&gt; [elemento] : saca todos los elementos de la parte, o uno solo si pones el elemento.</li>
 * <li>//showelement : muestra los elementos del arma y de cada pieza, y tu defensa total contra cada elemento.</li>
 * </ul>
 * Partes: weapon, head, chest (tambien body/full/pecho: la armadura entera va en el pecho), legs (pants), gloves, feet (boots).<br>
 * Elementos: fire, water, wind, earth, holy, dark (o 1-6). //reload elements esta en AdminReloadBoxes.
 */
public class AdminItemElements implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_setelement",
		"admin_clearelement",
		"admin_showelement"
	};
	
	private static final String[] PARTS =
	{
		"weapon",
		"head",
		"chest",
		"legs",
		"gloves",
		"feet"
	};
	
	private static final String PARTS_HELP = "weapon|head|chest|legs|gloves|feet (body/full = chest, pants = legs, boots = feet)";
	
	/**
	 * @param part : lo que escribio el GM.
	 * @return el nombre de la parte (weapon, head, chest, legs, gloves, feet), "all", o null si no existe.
	 */
	private static String parsePart(String part)
	{
		switch (part.toLowerCase())
		{
			case "weapon":
			case "arma":
				return "weapon";
			case "head":
			case "helmet":
			case "casco":
				return "head";
			case "chest":
			case "body":
			case "full":
			case "fullbody":
			case "fullarmor":
			case "pecho":
			case "pechera":
				return "chest";
			case "legs":
			case "pants":
			case "pantalon":
				return "legs";
			case "gloves":
			case "guantes":
				return "gloves";
			case "feet":
			case "boots":
			case "botas":
				return "feet";
			case "all":
			case "todo":
				return "all";
		}
		return null;
	}
	
	private static ItemInstance getItem(Player player, String part)
	{
		switch (part)
		{
			case "head":
				return player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD);
			case "chest":
				return player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			case "legs":
				return player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			case "gloves":
				return player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			case "feet":
				return player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
			default:
				return player.getActiveWeaponInstance();
		}
	}
	
	private static String describe(boolean weapon, List<ItemElement> list)
	{
		if (list.isEmpty())
			return "sin elemento";
		
		final StringBuilder sb = new StringBuilder();
		for (ItemElement e : list)
		{
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(e.getElement().getName()).append(" (").append(ItemElementManager.getDescription(weapon, e.getElement(), e.getValue())).append(')');
		}
		return sb.toString();
	}
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final ItemElementData data = ItemElementData.getInstance();
		final ItemElementManager manager = ItemElementManager.getInstance();
		final String[] args = command.trim().split("\\s+");
		
		if (command.startsWith("admin_setelement"))
		{
			if (args.length < 3)
			{
				activeChar.sendMessage("Uso: //setelement <fire|water|wind|earth|holy|dark> <valor> [" + PARTS_HELP + "]");
				return true;
			}
			
			final ElementType element = ElementType.parse(args[1]);
			if (element == null)
			{
				activeChar.sendMessage("Elemento invalido: " + args[1] + " (fire, water, wind, earth, holy, dark o 1-6).");
				return true;
			}
			
			final int value;
			try
			{
				value = Integer.parseInt(args[2]);
			}
			catch (NumberFormatException e)
			{
				activeChar.sendMessage("Valor invalido: " + args[2]);
				return true;
			}
			
			final String part = (args.length > 3) ? parsePart(args[3]) : "weapon";
			if (part == null || part.equals("all"))
			{
				activeChar.sendMessage("Parte invalida: " + args[3] + ". Partes: " + PARTS_HELP + ".");
				return true;
			}
			
			final ItemInstance item = getItem(activeChar, part);
			if (item == null)
			{
				activeChar.sendMessage("No tenes nada equipado en " + part + ".");
				return true;
			}
			
			final boolean weapon = manager.isWeapon(item);
			if (!weapon && !manager.isArmor(item))
			{
				activeChar.sendMessage(item.getItemName() + " no puede tener elemento (armas y armaduras grado " + data.getMinGrade() + ").");
				return true;
			}
			
			if (value <= 0)
			{
				final boolean removed = weapon ? data.delete(item.getObjectId()) : data.delete(item.getObjectId(), element);
				manager.sendItem(activeChar, item);
				activeChar.sendMessage(removed ? element.getName() + " sacado de " + item.getItemName() + "." : item.getItemName() + " no tenia " + element.getName() + ".");
				return true;
			}
			
			// Armadura: mismas reglas que la piedra (hasta 3 elementos, sin opuestos)
			if (!weapon && data.get(item.getObjectId(), element) == null)
			{
				final List<ItemElement> list = data.getAll(item.getObjectId());
				for (ItemElement e : list)
				{
					if (e.getElement() == element.getOpposite())
					{
						activeChar.sendMessage(item.getItemName() + " tiene " + e.getElement().getName() + ": no se puede juntar con " + element.getName() + " (son opuestos). Sacalo con //clearelement " + part + " " + e.getElement().getName().toLowerCase() + ".");
						return true;
					}
				}
				if (list.size() >= data.getArmorMaxElements())
				{
					activeChar.sendMessage(item.getItemName() + " ya tiene " + list.size() + " elementos (maximo). Saca uno con //clearelement " + part + " <elemento>.");
					return true;
				}
			}
			
			if (!data.save(item.getObjectId(), new ItemElement(element, value), weapon))
			{
				activeChar.sendMessage("No se pudo guardar (fijate si corriste el SQL de item_elements).");
				return true;
			}
			
			manager.sendItem(activeChar, item);
			activeChar.sendMessage(item.getItemName() + ": " + describe(weapon, data.getAll(item.getObjectId())) + ".");
		}
		else if (command.startsWith("admin_clearelement"))
		{
			if (args.length < 2)
			{
				activeChar.sendMessage("Uso: //clearelement <" + PARTS_HELP + "|all> [elemento]");
				return true;
			}
			
			final String part = parsePart(args[1]);
			if (part == null)
			{
				activeChar.sendMessage("Parte invalida: " + args[1] + ". Partes: " + PARTS_HELP + " o all.");
				return true;
			}
			
			ElementType element = null;
			if (args.length > 2)
			{
				element = ElementType.parse(args[2]);
				if (element == null)
				{
					activeChar.sendMessage("Elemento invalido: " + args[2] + " (fire, water, wind, earth, holy, dark o 1-6).");
					return true;
				}
			}
			
			int cleared = 0;
			for (String p : PARTS)
			{
				if (!part.equals("all") && !part.equals(p))
					continue;
				
				final ItemInstance item = getItem(activeChar, p);
				if (item == null)
				{
					if (!part.equals("all"))
						activeChar.sendMessage("No tenes nada equipado en " + p + ".");
					continue;
				}
				
				final boolean removed = (element == null) ? data.delete(item.getObjectId()) : data.delete(item.getObjectId(), element);
				if (removed)
				{
					cleared++;
					manager.sendItem(activeChar, item);
					activeChar.sendMessage(((element == null) ? "Elementos sacados de " : element.getName() + " sacado de ") + item.getItemName() + ".");
				}
				else if (!part.equals("all"))
					activeChar.sendMessage(item.getItemName() + ((element == null) ? " no tiene elementos." : " no tiene " + element.getName() + "."));
			}
			
			if (part.equals("all") && cleared == 0)
				activeChar.sendMessage("No tenias elementos para sacar en lo equipado.");
		}
		else if (command.startsWith("admin_showelement"))
		{
			for (String p : PARTS)
			{
				final ItemInstance item = getItem(activeChar, p);
				if (item == null)
					continue;
				
				activeChar.sendMessage(p + ": " + item.getItemName() + " -> " + describe(p.equals("weapon"), data.getAll(item.getObjectId())));
			}
			
			final StringBuilder sb = new StringBuilder("Defensa total (armadura + resistencias):");
			for (ElementType e : ElementType.values())
				sb.append(' ').append(e.getName()).append(' ').append(manager.getDefense(activeChar, e));
			activeChar.sendMessage(sb.toString());
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
