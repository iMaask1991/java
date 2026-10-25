package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.Map;

import net.sf.l2j.gameserver.alchemy.AlchemyAttribute;
import net.sf.l2j.gameserver.alchemy.AlchemySlot;
import net.sf.l2j.gameserver.alchemy.AlchemyType;
import net.sf.l2j.gameserver.alchemy.AttributeAlchemyData;
import net.sf.l2j.gameserver.alchemy.AttributeAlchemyManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

/**
 * Attribute Alchemy (Sockets) - comandos de GM para testear. Trabajan sobre lo que tenes equipado:
 * <ul>
 * <li>arma -> el arma</li>
 * <li>armadura -> la pechera</li>
 * <li>joya -> el collar</li>
 * </ul>
 * Comandos:
 * <ul>
 * <li>//setalchemy &lt;slot 1-3&gt; &lt;atributo&gt; &lt;tier&gt; &lt;valor&gt; : pone un socket a mano. El item depende del atributo (ej: P_ATK va al arma, P_DEF a la pechera, RUN_SPD al collar).</li>
 * <li>//clearalchemy [weapon|armor|jewel] : borra los sockets (por defecto del arma).</li>
 * <li>//showalchemy : muestra los sockets de arma, pechera y collar y la lista de atributos.</li>
 * </ul>
 * //reload alchemy esta en AdminReloadBoxes.
 */
public class AdminAttributeAlchemy implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_setalchemy",
		"admin_clearalchemy",
		"admin_showalchemy"
	};
	
	private static ItemInstance getTarget(Player player, AlchemyType type)
	{
		switch (type)
		{
			case ARMOR:
				return player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			case JEWEL:
				return player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
			default:
				return player.getActiveWeaponInstance();
		}
	}
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		final AttributeAlchemyManager manager = AttributeAlchemyManager.getInstance();
		final String[] args = command.trim().split("\\s+");
		
		if (command.startsWith("admin_setalchemy"))
		{
			if (args.length < 5)
			{
				activeChar.sendMessage("Uso: //setalchemy <slot 1-" + data.getMaxSlots() + "> <atributo> <tier> <valor>  (ej: //setalchemy 1 P_ATK 5 10)");
				return true;
			}
			
			try
			{
				final int slot = Integer.parseInt(args[1]);
				final AlchemyAttribute attr = data.getAttribute(args[2]);
				final int tier = Integer.parseInt(args[3]);
				final double value = Double.parseDouble(args[4]);
				
				if (slot < 1 || slot > data.getMaxSlots())
				{
					activeChar.sendMessage("Slot invalido (1-" + data.getMaxSlots() + ").");
					return true;
				}
				if (attr == null)
				{
					activeChar.sendMessage("Atributo desconocido: " + args[2] + ". Usa //showalchemy para ver la lista.");
					return true;
				}
				
				final ItemInstance item = getTarget(activeChar, attr.getType());
				if (item == null || manager.getType(item) != attr.getType())
				{
					activeChar.sendMessage("Para " + attr.getId() + " tenes que tener equipad" + (attr.getType() == AlchemyType.WEAPON ? "a un arma" : (attr.getType() == AlchemyType.ARMOR ? "a una pechera" : "o un collar")) + " de grado " + data.getMinGrade() + ".");
					return true;
				}
				
				final AlchemySlot result = new AlchemySlot(slot, attr.getId(), tier, value);
				if (!data.saveSlot(item.getObjectId(), result))
				{
					activeChar.sendMessage("No se pudo guardar (fijate si corriste el SQL de la tabla).");
					return true;
				}
				
				manager.refresh(activeChar, item);
				activeChar.sendMessage(item.getItemName() + " - Socket " + slot + ": " + result.getDisplay() + " (Tier " + tier + ")");
			}
			catch (NumberFormatException e)
			{
				activeChar.sendMessage("Numeros invalidos. Uso: //setalchemy <slot> <atributo> <tier> <valor>");
			}
		}
		else if (command.startsWith("admin_clearalchemy"))
		{
			final AlchemyType type = (args.length > 1) ? AlchemyType.fromString(args[1]) : AlchemyType.WEAPON;
			final ItemInstance item = getTarget(activeChar, type);
			if (item == null)
			{
				activeChar.sendMessage("No tenes nada equipado en ese lugar.");
				return true;
			}
			
			data.deleteItem(item.getObjectId());
			manager.refresh(activeChar, item);
			activeChar.sendMessage("Sockets borrados de " + item.getItemName() + ".");
		}
		else if (command.startsWith("admin_showalchemy"))
		{
			for (AlchemyType type : AlchemyType.values())
			{
				final ItemInstance item = getTarget(activeChar, type);
				if (item == null || manager.getType(item) != type)
					continue;
				
				final Map<Integer, AlchemySlot> slots = data.getSlots(item.getObjectId());
				final StringBuilder sb = new StringBuilder(item.getItemName()).append(':');
				for (int i = 1; i <= data.getMaxSlots(); i++)
				{
					final AlchemySlot slot = slots.get(i);
					sb.append(" [").append(i).append("] ").append((slot == null) ? "--empty--" : slot.getDisplay() + " (T" + slot.getTier() + ")");
				}
				activeChar.sendMessage(sb.toString());
			}
			
			for (AlchemyType type : AlchemyType.values())
			{
				final StringBuilder sb = new StringBuilder(type.name()).append(" (orbe ").append(data.getOrbItemId(type)).append("):");
				for (AlchemyAttribute attr : data.getAttributes(type))
					sb.append(' ').append(attr.getId());
				activeChar.sendMessage(sb.toString());
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
