package net.sf.l2j.gameserver.donate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.xml.SkillTreeData;
import net.sf.l2j.gameserver.element.ItemElementData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Donate Shop - Skill Enchant al maximo (SanSara).<br>
 * <ul>
 * <li>Muestra las skills del jugador que se pueden enchantear (las que estan en enchantSkills.xml).</li>
 * <li>El jugador elige la ruta y la skill queda directo en el maximo de esa ruta (+30, o +15 si la ruta llega a 15). Sin fallo.</li>
 * <li>La skill tiene que estar en su nivel maximo normal (o ya enchanteada). Si ya tenia otra ruta, la cambia por la nueva.</li>
 * <li>Mismas condiciones que el enchant normal: 3ra clase y nivel 76+.</li>
 * </ul>
 * Precio: donationmanager.properties (ItemSkillEnchant / CantidadSkillEnchant), igual para todas las skills y rutas.<br>
 * Nombres de las rutas: data/xml/custom/skillEnchantRoutes.xml (se recarga solo si cambias el archivo).<br>
 * Diseno: data/html/mods/donationmanager/skillench_*.htm (aca solo se rellenan los datos).
 */
public final class DonateSkillEnchant
{
	private static final Logger LOGGER = Logger.getLogger(DonateSkillEnchant.class.getName());

	private static final String HTML_PATH = "data/html/mods/donationmanager/";
	private static final String ROUTES_FILE = "./data/xml/custom/skillEnchantRoutes.xml";
	private static final int PAGE_SIZE = 8;

	/** Evita doble compra si el jugador hace doble click en "Comprar". */
	private static final Set<Integer> BUSY = ConcurrentHashMap.newKeySet();

	/** skillId -> ruta -> {nombre, descripcion} */
	private static volatile Map<Integer, Map<Integer, String[]>> _routes = new HashMap<>();
	private static volatile long _routesTime = -1;

	private DonateSkillEnchant()
	{
	}

	/**
	 * @param command : el bypass completo.
	 * @return true si el bypass es de esta tienda.
	 */
	public static boolean isShopBypass(String command)
	{
		return command.startsWith("dskillench");
	}

	public static void onBypass(Player player, String command)
	{
		if (player == null)
			return;

		final StringTokenizer st = new StringTokenizer(command, " ");
		final String action = st.nextToken();
		final int a;
		final int b;
		try
		{
			a = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
			b = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
		}
		catch (NumberFormatException e)
		{
			return;
		}

		if (!canUse(player))
		{
			send(player, load("skillench_noreq.htm"));
			return;
		}

		switch (action)
		{
			case "dskillench":
				showMain(player, a);
				break;
			case "dskillench_skill":
				showSkill(player, a, b);
				break;
			case "dskillench_ask":
				ask(player, a, b);
				break;
			case "dskillench_buy":
				buy(player, a, b);
				break;
		}
	}

	// ========================================================================
	// Paginas
	// ========================================================================

	private static void showMain(Player player, int page)
	{
		final List<L2Skill> list = getEnchantable(player);
		final int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		page = Math.max(0, Math.min(page, pages - 1));

		final String rowTpl = load("skillench_row.htm");
		final StringBuilder sb = new StringBuilder();
		for (int i = page * PAGE_SIZE; i < Math.min(list.size(), (page + 1) * PAGE_SIZE); i++)
		{
			final L2Skill skill = list.get(i);
			sb.append(rowTpl.replace("%skillName%", skill.getName()).replace("%state%", stateText(skill)).replace("%skillId%", String.valueOf(skill.getId())).replace("%page%", String.valueOf(page)).replace("%icon%", icon(skill.getId())));
		}

		if (sb.length() == 0)
			sb.append(load("skillench_empty.htm"));

		final StringBuilder pg = new StringBuilder();
		if (pages > 1)
		{
			final String pageTpl = load("skillench_page.htm");
			final String pageCurTpl = load("skillench_page_cur.htm");
			for (int i = 0; i < pages; i++)
				pg.append(((i == page) ? pageCurTpl : pageTpl).replace("%p%", String.valueOf(i)).replace("%n%", String.valueOf(i + 1)));
		}

		send(player, load("skillench_main.htm").replace("%rows%", sb.toString()).replace("%pages%", pg.toString()).replace("%price%", priceText()).replace("%count%", String.valueOf(list.size())));
	}

	private static void showSkill(Player player, int skillId, int page)
	{
		final L2Skill skill = getEnchantable(player, skillId);
		if (skill == null)
		{
			showMain(player, page);
			return;
		}

		final String rowTpl = load("skillench_route_row.htm");
		final String maxTpl = load("skillench_route_max.htm");
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Integer> e : SkillTreeData.getInstance().getEnchantRoutes(skillId).entrySet())
		{
			final int route = e.getKey();
			final int level = e.getValue();
			if (SkillTable.getInstance().getInfo(skillId, level) == null)
				continue;

			final String tpl = (skill.getLevel() == level) ? maxTpl : rowTpl;
			sb.append(tpl.replace("%route%", String.valueOf(route)).replace("%routeName%", routeName(skillId, route)).replace("%routeDesc%", routeDesc(skillId, route)).replace("%plus%", String.valueOf(level % 100)).replace("%skillId%", String.valueOf(skillId)));
		}

		send(player, load("skillench_skill.htm").replace("%rows%", sb.toString()).replace("%skillName%", skill.getName()).replace("%state%", stateText(skill)).replace("%price%", priceText()).replace("%page%", String.valueOf(page)).replace("%icon%", icon(skillId)));
	}

	private static void ask(Player player, int skillId, int route)
	{
		final L2Skill skill = getEnchantable(player, skillId);
		final int level = targetLevel(skill, route);
		if (level <= 0)
		{
			showSkill(player, skillId, 0);
			return;
		}

		String html = load("skillench_confirm.htm");
		html = html.replace("%skillName%", skill.getName()).replace("%state%", stateText(skill)).replace("%routeName%", routeName(skillId, route)).replace("%routeDesc%", routeDesc(skillId, route)).replace("%plus%", String.valueOf(level % 100));
		html = html.replace("%changeRoute%", (skill.getLevel() > 100 && skill.getLevel() / 100 != route) ? load("skillench_change.htm") : "");
		html = html.replace("%price%", priceText()).replace("%skillId%", String.valueOf(skillId)).replace("%route%", String.valueOf(route)).replace("%icon%", icon(skillId));
		send(player, html);
	}

	private static void buy(Player player, int skillId, int route)
	{
		if (!BUSY.add(player.getObjectId()))
			return;

		try
		{
			final L2Skill skill = getEnchantable(player, skillId);
			final int level = targetLevel(skill, route);
			if (level <= 0)
			{
				player.sendMessage("No se pudo: la skill no se puede enchantear en esa ruta o ya esta al maximo.");
				showSkill(player, skillId, 0);
				return;
			}

			final L2Skill newSkill = SkillTable.getInstance().getInfo(skillId, level);
			if (newSkill == null)
			{
				showSkill(player, skillId, 0);
				return;
			}

			if (!pay(player))
				return;

			player.addSkill(newSkill, true, true);
			player.sendSkillList();
			player.sendPacket(new UserInfo(player));

			final int anim = ItemElementData.getInstance().getAnimationSkillId();
			if (anim > 0)
				player.broadcastPacket(new MagicSkillUse(player, player, anim, 1, 500, 0));

			player.sendMessage(newSkill.getName() + ": " + routeName(skillId, route) + " +" + (level % 100) + " (maximo).");
			LOGGER.info("DonateSkillEnchant: " + player.getName() + " -> " + newSkill.getName() + " (" + skillId + ") lvl " + level + ".");

			String html = load("skillench_ok.htm");
			html = html.replace("%skillName%", newSkill.getName()).replace("%routeName%", routeName(skillId, route)).replace("%plus%", String.valueOf(level % 100)).replace("%skillId%", String.valueOf(skillId)).replace("%icon%", icon(skillId));
			send(player, html);
		}
		finally
		{
			BUSY.remove(player.getObjectId());
		}
	}

	// ========================================================================
	// Reglas
	// ========================================================================

	/** Mismas condiciones que el enchant normal de skills. */
	private static boolean canUse(Player player)
	{
		return player.getClassId().level() >= 3 && player.getLevel() >= 76;
	}

	/** Skills del jugador que se pueden enchantear (en su nivel maximo normal o ya enchanteadas), ordenadas por nombre. */
	private static List<L2Skill> getEnchantable(Player player)
	{
		final List<L2Skill> list = new ArrayList<>();
		for (L2Skill skill : player.getSkills().values())
		{
			if (isEnchantable(skill))
				list.add(skill);
		}
		list.sort((x, y) -> x.getName().compareToIgnoreCase(y.getName()));
		return list;
	}

	private static L2Skill getEnchantable(Player player, int skillId)
	{
		final L2Skill skill = player.getSkill(skillId);
		return isEnchantable(skill) ? skill : null;
	}

	private static boolean isEnchantable(L2Skill skill)
	{
		if (skill == null || SkillTreeData.getInstance().getEnchantRoutes(skill.getId()).isEmpty())
			return false;

		return skill.getLevel() > 100 || skill.getLevel() == SkillTable.getInstance().getMaxLevel(skill.getId());
	}

	/** @return el nivel al que queda la skill con esa ruta al maximo, o 0 si no se puede (no existe o ya esta asi). */
	private static int targetLevel(L2Skill skill, int route)
	{
		if (skill == null)
			return 0;

		final Integer level = SkillTreeData.getInstance().getEnchantRoutes(skill.getId()).get(route);
		if (level == null || level == skill.getLevel() || SkillTable.getInstance().getInfo(skill.getId(), level) == null)
			return 0;

		return level;
	}

	private static String stateText(L2Skill skill)
	{
		if (skill.getLevel() <= 100)
			return "Sin enchant";

		return routeName(skill.getId(), skill.getLevel() / 100) + " +" + (skill.getLevel() % 100);
	}

	// ========================================================================
	// Nombres de rutas
	// ========================================================================

	private static String routeName(int skillId, int route)
	{
		final String[] r = getRoute(skillId, route);
		return (r == null || r[0].isEmpty()) ? "Ruta " + route : r[0];
	}

	private static String routeDesc(int skillId, int route)
	{
		final String[] r = getRoute(skillId, route);
		return (r == null) ? "" : r[1];
	}

	private static String[] getRoute(int skillId, int route)
	{
		final Map<Integer, String[]> m = routes().get(skillId);
		return (m == null) ? null : m.get(route);
	}

	private static Map<Integer, Map<Integer, String[]>> routes()
	{
		final File file = new File(ROUTES_FILE);
		final long time = file.exists() ? file.lastModified() : 0;
		if (time != _routesTime)
		{
			synchronized (DonateSkillEnchant.class)
			{
				if (time != _routesTime)
				{
					_routes = loadRoutes(file);
					_routesTime = time;
				}
			}
		}
		return _routes;
	}

	private static Map<Integer, Map<Integer, String[]>> loadRoutes(File file)
	{
		final Map<Integer, Map<Integer, String[]>> map = new HashMap<>();
		if (!file.exists())
			return map;

		try
		{
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			for (Node n = doc.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (!(n instanceof Element) || !"skill".equals(n.getNodeName()))
					continue;

				final int id = Integer.parseInt(((Element) n).getAttribute("id"));
				final Map<Integer, String[]> m = new HashMap<>();
				for (Node r = n.getFirstChild(); r != null; r = r.getNextSibling())
				{
					if (!(r instanceof Element) || !"route".equals(r.getNodeName()))
						continue;

					final Element e = (Element) r;
					m.put(Integer.parseInt(e.getAttribute("id")), new String[]
					{
						e.getAttribute("name"),
						e.getAttribute("desc")
					});
				}
				map.put(id, m);
			}
			LOGGER.info("DonateSkillEnchant: nombres de " + map.size() + " skills cargados.");
		}
		catch (Exception e)
		{
			LOGGER.warning("DonateSkillEnchant: error leyendo " + ROUTES_FILE + ": " + e.getMessage());
		}
		return map;
	}

	// ========================================================================
	// Utiles
	// ========================================================================

	/** Icono de la skill como lo usa el cliente (icon.skill0001). */
	private static String icon(int skillId)
	{
		return "icon.skill" + ((skillId < 1000) ? String.format("%04d", skillId) : String.valueOf(skillId));
	}

	private static boolean pay(Player player)
	{
		final int itemId = Config.DONATE_SKILL_ENCHANT_ITEM;
		final int count = Config.DONATE_SKILL_ENCHANT_COUNT;
		if (count <= 0)
			return true;

		final boolean paid = (itemId == 57) ? player.reduceAdena("DonateSkillEnchant", count, player, true) : player.destroyItemByItemId("DonateSkillEnchant", itemId, count, player, true);
		if (!paid)
		{
			player.sendMessage("No te alcanza: necesitas " + priceText() + ".");
			send(player, load("noitems.htm"));
		}
		return paid;
	}

	private static String priceText()
	{
		final int itemId = Config.DONATE_SKILL_ENCHANT_ITEM;
		final int count = Config.DONATE_SKILL_ENCHANT_COUNT;
		if (count <= 0)
			return "Gratis";

		final Item template = ItemTable.getInstance().getTemplate(itemId);
		return String.format("%,d", count) + " " + ((template == null) ? "item " + itemId : template.getName());
	}

	private static String load(String file)
	{
		final String html = HtmCache.getInstance().getHtm(HTML_PATH + file);
		return (html == null) ? "<html><body>Donate Shop: falta " + HTML_PATH + file + "</body></html>" : html;
	}

	private static void send(Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(1);
		msg.setHtml(html);
		player.sendPacket(msg);
	}
}
