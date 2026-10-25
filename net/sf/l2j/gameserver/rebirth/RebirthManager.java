package net.sf.l2j.gameserver.rebirth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * Community Board / NPC Rebirth system with a <b>simple explicit cost table</b>: every rebirth can cost a
 * different amount, in as many currencies as you want (adena, medals, donation coins...).
 * <br>
 * <br>
 * Costs come from {@code ./config/HadesProject/Rebirth.properties} and are resolved in this order:
 * <ol>
 * <li><b>Ranges (recommended, scales to 100+ rebirths)</b>: {@code Range1 = 1-10 | 57:100, 6393:100, 10001:100}.
 * Format {@code desde-hasta | itemId:cantidad, ...}. Numbers above the last range repeat it, gaps use the
 * closest lower range, so the table can cover as many rebirths as you want.</li>
 * <li><b>Exact per-rebirth override</b>: {@code Price7 = 57:500, 10001:500} (wins over the range) for one-offs.</li>
 * <li><b>LEGACY fallback</b> (only when neither the above exists): FORMULA
 * {@code price(n) = Base + Step * n + Base * ((1 + Growth / 100) ^ n - 1)} or the old single-value TABLE
 * ({@code PriceTable}), plus the optional second currency ({@code ExtraPriceId}).</li>
 * </ol>
 * Payment is <b>verify-all-then-charge-all</b>: if one currency is missing, nothing is taken from the player.
 * <br>
 * <br>
 * Character data lives in {@code character_rebirth} instead of a quest item, so paying the currency can no longer
 * change the rebirth counter.
 */
public final class RebirthManager
{
	private static final CLogger LOGGER = new CLogger(RebirthManager.class.getName());
	
	private static final String CB_PATH = "data/html/CommunityBoard/Custom/rebirth/";

	/** Interned owner marker used to clean up any stat bonus a player still has from the old (removed) bonus system. */
	private static final String BONUS_OWNER = "rebirthbonus";

	public static boolean ENABLED;
	public static boolean DYNAMIC_PRICE;
	/** 0 = FORMULA, 1 = TABLE. */
	public static int PRICE_MODE;
	public static double PRICE_BASE;
	public static double PRICE_STEP;
	public static double PRICE_GROWTH_PERCENT;
	public static long PRICE_MAX;
	public static double[] PRICE_TABLE = new double[0];
	
	public static int EXTRA_PRICE_ID;
	public static double EXTRA_PRICE_BASE;
	public static double EXTRA_PRICE_GROWTH_PERCENT;
	
	public static int MAX_REBIRTHS;
	public static int DELEVEL;
	public static int MIN_LEVEL;
	public static boolean ANNOUNCE;
	public static int CAST_SKILL_ID;

	/** Private message sent to the player that rebirths. Placeholders: %name% %count% %max% %level% %points% %next_price% %price_name%. */
	public static String MESSAGE_SELF;
	/** Global announcement text. Same placeholders. Empty = disabled. */
	public static String ANNOUNCE_TEXT;
	/** ANNOUNCEMENT (default) | CRITICAL | EVENT | HERO | NONE. */
	public static String ANNOUNCE_CHANNEL;

	private static final Map<Integer, RebirthData> DATA = new ConcurrentHashMap<>();
	
	/** Matches the explicit multi-currency cost keys ({@code Price1}, {@code Price2}, ...). */
	private static final Pattern COST_KEY = Pattern.compile("^[Pp]rice(\\d+)$");
	
	/**
	 * Explicit multi-currency cost table, keyed by rebirth number (1 = first rebirth, 2 = second...).<br>
	 * Each entry holds {@code itemId -> amount}. When a rebirth has no exact entry, the closest lower entry is
	 * used; when there are more rebirths than entries, the last entry repeats.
	 */
	private static final Map<Integer, Map<Integer, Long>> COST_TABLE = new LinkedHashMap<>();
	
	/** Returned when a rebirth has no cost configured at all. */
	private static final Map<Integer, Long> NO_COST = Collections.emptyMap();
	
	/** Matches the range cost keys ({@code Range1}, {@code Range2}, ...). */
	private static final Pattern RANGE_KEY = Pattern.compile("^[Rr]ange(\\d+)$");
	
	/** Configured cost ranges ({@code RangeN = desde-hasta | coste}), kept sorted by {@code from}. */
	private static final List<CostRange> RANGES = new ArrayList<>();
	
	/** One configured cost range: rebirth numbers {@code from..to} (both inclusive) cost {@code cost}. */
	private static final class CostRange
	{
		private final int from;
		private final int to;
		private final Map<Integer, Long> cost;
		
		private CostRange(int from, int to, Map<Integer, Long> cost)
		{
			this.from = from;
			this.to = to;
			this.cost = cost;
		}
	}
	
	private RebirthManager()
	{
	}
	/**
	 * Loads the price scaling (Rebirth.properties). The old per-rebirth stat bonus / Power Points system
	 * (RebirthPowers.ini) was removed - rebirth bonuses now come from rebirthSkills.xml instead.
	 */
	public static void load()
	{
		try
		{
			final ExProperties rebirth = new ExProperties();
			rebirth.load(Config.REBIRTH_FILE);
			
			ENABLED = rebirth.getProperty("EnableRebirthSystem", Config.ENABLE_REBIRTH_SYSTEM);
			DYNAMIC_PRICE = rebirth.getProperty("EnableDynamicPrice", true);
			PRICE_MODE = "TABLE".equalsIgnoreCase(rebirth.getProperty("PriceMode", "FORMULA")) ? 1 : 0;
			PRICE_BASE = Double.parseDouble(rebirth.getProperty("PriceBase", String.valueOf(Config.REBIRTH_PRICE_COUNT)));
			PRICE_STEP = Double.parseDouble(rebirth.getProperty("PriceStep", "0"));
			PRICE_GROWTH_PERCENT = Double.parseDouble(rebirth.getProperty("PriceGrowthPercent", "0"));
			PRICE_MAX = Long.parseLong(rebirth.getProperty("PriceMax", "0"));
			PRICE_TABLE = parseDoubles(rebirth.getProperty("PriceTable", ""));
			
			EXTRA_PRICE_ID = rebirth.getProperty("ExtraPriceId", 0);
			EXTRA_PRICE_BASE = Double.parseDouble(rebirth.getProperty("ExtraPriceBase", "0"));
			EXTRA_PRICE_GROWTH_PERCENT = Double.parseDouble(rebirth.getProperty("ExtraPriceGrowthPercent", "0"));
			
			MAX_REBIRTHS = rebirth.getProperty("MaxRebirths", 0);
			DELEVEL = rebirth.getProperty("Delevel", Config.REBIRTH_DELEVEL);
			MIN_LEVEL = rebirth.getProperty("MinLevel", Config.REBIRTH_LEVEL_MIN);
			ANNOUNCE = rebirth.getProperty("Announce", true);
			CAST_SKILL_ID = rebirth.getProperty("CastSkillId", 5103);
			
			MESSAGE_SELF = rebirth.getProperty("MessageSelf", "Renacimiento #%count% completado. Nivel %level%. Proximo coste: %next_price%.");
			ANNOUNCE_TEXT = rebirth.getProperty("AnnounceText", "%name% ha realizado su Rebirth #%count%!");
			ANNOUNCE_CHANNEL = rebirth.getProperty("AnnounceChannel", "ANNOUNCEMENT").trim().toUpperCase();
			
			// Explicit multi-currency cost table: Price1 = first rebirth, Price2 = second, ...
			COST_TABLE.clear();
			final List<Integer> costIndexes = new ArrayList<>();
			for (String key : rebirth.stringPropertyNames())
			{
				final Matcher matcher = COST_KEY.matcher(key);
				if (matcher.matches())
					costIndexes.add(Integer.parseInt(matcher.group(1)));
			}
			Collections.sort(costIndexes);
			
			for (int index : costIndexes)
			{
				final Map<Integer, Long> cost = parseCost(rebirth.getProperty("Price" + index, ""));
				if (cost.isEmpty())
				{
					LOGGER.warn("Rebirth: 'Price{}' no tiene ningun coste valido. Formato: itemId:cantidad (monedas separadas por ',').", index);
					continue;
				}
				
				COST_TABLE.put(index, cost);
			}
			
			// Cost ranges: RangeN = desde-hasta | itemId:cantidad, itemId:cantidad, ...
			RANGES.clear();
			final List<Integer> rangeIndexes = new ArrayList<>();
			for (String key : rebirth.stringPropertyNames())
			{
				final Matcher matcher = RANGE_KEY.matcher(key);
				if (matcher.matches())
					rangeIndexes.add(Integer.parseInt(matcher.group(1)));
			}
			Collections.sort(rangeIndexes);
			
			for (int index : rangeIndexes)
			{
				final CostRange range = parseRange(rebirth.getProperty("Range" + index, ""), index);
				if (range != null)
					RANGES.add(range);
			}
			
			RANGES.sort((a, b) -> Integer.compare(a.from, b.from));
			
			// Defensive: a currency that is also the reward item would self-finance the rebirth
			// (you pay it and you get it back on the same rebirth).
			if (Config.REBIRTH_REWARD_ID > 0 && Config.REBIRTH_REWARD_COUNT > 0)
			{
				for (Map<Integer, Long> cost : allCosts())
				{
					if (cost.containsKey(Config.REBIRTH_REWARD_ID))
						LOGGER.warn("Rebirth: el coste cobra el item {} que TAMBIEN se entrega como reward (RebirthRewardID) -> el rebirth se auto-financia. Cambia uno de los dos.", Config.REBIRTH_REWARD_ID);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Rebirth: failed to load {} ({}).", Config.REBIRTH_FILE, e.getMessage());
		}
		
		// El sistema viejo de bonuses automaticos por stat (RebirthPowers.ini: PointsPerRebirth /
		// RebirthBonus / Power Points) se saco del todo: las skills de rebirth ahora salen de rebirthSkills.xml.
		LOGGER.info("Rebirth system: {} exact costs, {} cost ranges, dynamic price {}.", COST_TABLE.size(), RANGES.size(), DYNAMIC_PRICE);
	}
	
	// ------------------------------------------------------------------
	// Price
	// ------------------------------------------------------------------
	
	/**
	 * @param rebirthIndex The rebirth being bought (amount of rebirths already done, 0 based).
	 * @return How many {@link Config#REBIRTH_PRICE_ID} items that rebirth costs.
	 */
	public static long getPrice(int rebirthIndex)
	{
		final double price;
		
		if (!DYNAMIC_PRICE)
			price = PRICE_BASE;
		else if (PRICE_MODE == 1 && PRICE_TABLE.length > 0)
			price = PRICE_TABLE[Math.min(Math.max(0, rebirthIndex), PRICE_TABLE.length - 1)];
		else
			price = PRICE_BASE + PRICE_STEP * rebirthIndex + PRICE_BASE * (Math.pow(1.0 + PRICE_GROWTH_PERCENT / 100.0, Math.max(0, rebirthIndex)) - 1.0);
		
		long result = Math.round(price);
		if (PRICE_MAX > 0)
			result = Math.min(result, PRICE_MAX);
		
		return Math.max(1, result);
	}
	
	/**
	 * @param rebirthIndex The rebirth being bought.
	 * @return Optional second currency cost (0 = disabled).
	 */
	public static long getExtraPrice(int rebirthIndex)
	{
		if (EXTRA_PRICE_ID <= 0 || EXTRA_PRICE_BASE <= 0)
			return 0;
		
		final double price = EXTRA_PRICE_BASE * Math.pow(1.0 + EXTRA_PRICE_GROWTH_PERCENT / 100.0, Math.max(0, rebirthIndex));
		return Math.max(1, Math.round(price));
	}
	
	public static String getPriceItemName()
	{
		final Item item = ItemTable.getInstance().getTemplate(Config.REBIRTH_PRICE_ID);
		return item == null ? String.valueOf(Config.REBIRTH_PRICE_ID) : item.getName();
	}
	
	public static String getExtraPriceItemName()
	{
		final Item item = ItemTable.getInstance().getTemplate(EXTRA_PRICE_ID);
		return item == null ? String.valueOf(EXTRA_PRICE_ID) : item.getName();
	}
	// ------------------------------------------------------------------
	// Explicit multi-currency cost (Price1, Price2, ...)
	// ------------------------------------------------------------------
	
	/**
	 * @return True when at least one {@code PriceN} line is configured (the explicit table wins over the legacy price).
	 */
	public static boolean hasCostTable()
	{
		return !COST_TABLE.isEmpty() || !RANGES.isEmpty();
	}
	
	/**
	 * @param rebirthNumber The rebirth being bought, <b>1 based</b> (1 = the first one).
	 * @return The {@code itemId -> amount} cost of that rebirth. Resolution order: an exact {@code PriceN} wins,
	 *         then the {@code RangeN} that contains the number, then the closest lower range (or the last range when
	 *         the number is above the whole table, which makes the system keep working past 100 rebirths).
	 *         Empty map = free / not configured (the caller falls back to the legacy price).
	 */
	public static Map<Integer, Long> getCost(int rebirthNumber)
	{
		// 1) Exact per-rebirth override (PriceN).
		final Map<Integer, Long> exact = COST_TABLE.get(rebirthNumber);
		if (exact != null)
			return exact;
		
		// 2) Ranges (RangeN = desde-hasta | coste).
		Map<Integer, Long> lower = null;
		for (CostRange range : RANGES)
		{
			if (rebirthNumber >= range.from && rebirthNumber <= range.to)
				return range.cost;
			
			if (range.to < rebirthNumber)
				lower = range.cost;
			else
				break;
		}
		
		if (lower != null)
			return lower;
		
		if (!RANGES.isEmpty())
			return RANGES.get(0).cost;
		
		// 3) Only exact entries configured: closest lower one (or the first).
		Map<Integer, Long> fallback = null;
		for (Map.Entry<Integer, Map<Integer, Long>> entry : COST_TABLE.entrySet())
		{
			if (entry.getKey() < rebirthNumber)
				fallback = entry.getValue();
			else
				break;
		}
		
		return fallback != null ? fallback : (COST_TABLE.isEmpty() ? NO_COST : COST_TABLE.values().iterator().next());
	}
	
	/**
	 * @return Every configured cost (exact overrides + ranges). Used by the anti self-financing check.
	 */
	private static List<Map<Integer, Long>> allCosts()
	{
		final List<Map<Integer, Long>> costs = new ArrayList<>(COST_TABLE.values());
		for (CostRange range : RANGES)
			costs.add(range.cost);
		
		return costs;
	}
	
	/**
	 * Verifies <b>every</b> currency of the cost before charging anything, then charges all of them. If a charge
	 * fails midway, the already taken currencies are given back, so the player never loses items without a rebirth.
	 * @param player The player paying.
	 * @param cost The {@code itemId -> amount} cost (see {@link #getCost(int)}).
	 * @return True when the payment is complete (or when the cost is empty).
	 */
	public static boolean chargeCost(Player player, Map<Integer, Long> cost)
	{
		if (cost == null || cost.isEmpty())
			return true;
		
		// 1) Verify everything first: nothing is touched when one currency is missing.
		for (Map.Entry<Integer, Long> entry : cost.entrySet())
		{
			final long have = player.getInventory().getInventoryItemCount(entry.getKey(), 0);
			if (have < entry.getValue())
			{
				player.sendMessage("Te faltan " + (entry.getValue() - have) + " x " + getItemName(entry.getKey()) + " para renacer.");
				return false;
			}
		}
		
		// 2) Charge everything, rolling back what was already taken if something fails.
		final List<int[]> charged = new ArrayList<>(cost.size());
		for (Map.Entry<Integer, Long> entry : cost.entrySet())
		{
			final int itemId = entry.getKey();
			final int amount = (int) Math.min(Integer.MAX_VALUE, entry.getValue());
			if (!player.destroyItemByItemId("RebirthCost", itemId, amount, player, true))
			{
				for (int[] back : charged)
					player.addItem("RebirthRefund", back[0], back[1], player, true);
				
				player.sendMessage("No se pudo completar el pago del renacimiento. No se cobro nada.");
				return false;
			}
			
			charged.add(new int[] { itemId, amount });
		}
		
		return true;
	}
	
	/**
	 * @param rebirthNumber The rebirth being bought, 1 based.
	 * @return Human readable cost (e.g. {@code 100 x Adena, 100 x Medalla}). "Gratis" when nothing is configured.
	 */
	public static String formatCost(int rebirthNumber)
	{
		final Map<Integer, Long> cost = getCost(rebirthNumber);
		if (cost.isEmpty())
			return "Gratis";
		
		final StringBuilder sb = new StringBuilder(96);
		for (Map.Entry<Integer, Long> entry : cost.entrySet())
		{
			if (sb.length() > 0)
				sb.append(", ");
			
			sb.append(StringUtil.formatNumber((int) Math.min(Integer.MAX_VALUE, entry.getValue()))).append(" x ").append(getItemName(entry.getKey()));
		}
		return sb.toString();
	}
	
	/**
	 * @param player The player to inspect.
	 * @param rebirthNumber The rebirth being bought, 1 based.
	 * @return How many of each currency the player has (e.g. {@code 120 x Adena, 5 x Medalla}).
	 */
	public static String formatHave(Player player, int rebirthNumber)
	{
		final Map<Integer, Long> cost = getCost(rebirthNumber);
		if (cost.isEmpty())
			return "-";
		
		final StringBuilder sb = new StringBuilder(96);
		for (Map.Entry<Integer, Long> entry : cost.entrySet())
		{
			if (sb.length() > 0)
				sb.append(", ");
			
			final long have = player.getInventory().getInventoryItemCount(entry.getKey(), 0);
			sb.append(StringUtil.formatNumber((int) Math.min(Integer.MAX_VALUE, have))).append(" x ").append(getItemName(entry.getKey()));
		}
		return sb.toString();
	}
	
	private static String getItemName(int itemId)
	{
		final Item item = ItemTable.getInstance().getTemplate(itemId);
		return item == null ? String.valueOf(itemId) : item.getName();
	}
	
	/**
	 * @param rebirthNumber The rebirth being bought, <b>1 based</b>.
	 * @return The full cost text, valid in both modes: table {@code "100 x Adena, 100 x Medalla"} /
	 *         legacy {@code "1 x Medal"}.
	 */
	public static String getCostText(int rebirthNumber)
	{
		if (hasCostTable())
			return formatCost(rebirthNumber);
		
		return getPrice(rebirthNumber - 1) + " x " + getPriceItemName();
	}
	
	/**
	 * Parses a range line ({@code Range1 = 1-10 | 57:100, 6393:100, 10001:100}).
	 * @param raw The raw config value.
	 * @param index The {@code RangeN} number, only used for logging.
	 * @return The parsed range, or null when it is malformed (a warning is logged).
	 */
	private static CostRange parseRange(String raw, int index)
	{
		if (raw == null || raw.trim().isEmpty())
			return null;
		
		final int separator = raw.indexOf('|');
		if (separator <= 0)
		{
			LOGGER.warn("Rebirth: 'Range{}' invalido. Formato: desde-hasta | itemId:cantidad, itemId:cantidad.", index);
			return null;
		}
		
		final String[] bounds = raw.substring(0, separator).trim().split("-");
		if (bounds.length != 2)
		{
			LOGGER.warn("Rebirth: 'Range{}' no tiene un rango valido. Formato: desde-hasta | coste.", index);
			return null;
		}
		
		try
		{
			final int from = Integer.parseInt(bounds[0].trim());
			final int to = Integer.parseInt(bounds[1].trim());
			
			if (from <= 0 || to < from)
			{
				LOGGER.warn("Rebirth: 'Range{}' ignorado (el rango {} - {} es invalido).", index, from, to);
				return null;
			}
			
			final Map<Integer, Long> cost = parseCost(raw.substring(separator + 1));
			if (cost.isEmpty())
			{
				LOGGER.warn("Rebirth: 'Range{}' ignorado (no tiene ningun coste valido).", index);
				return null;
			}
			
			return new CostRange(from, to, cost);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn("Rebirth: 'Range{}' tiene numeros invalidos en el rango (formato desde-hasta).", index);
			return null;
		}
	}
	
	/**
	 * Parses an {@code itemId:cantidad} list ({@code 57:100, 6393:100, 10001:100}). Invalid entries are skipped
	 * with a warning and repeated itemIds are added together.
	 * @param raw The raw config value.
	 * @return The parsed cost (empty when nothing valid is there).
	 */
	private static Map<Integer, Long> parseCost(String raw)
	{
		final Map<Integer, Long> cost = new LinkedHashMap<>();
		if (raw == null || raw.trim().isEmpty())
			return cost;
		
		for (String part : raw.split("[,;]+"))
		{
			final String entry = part.trim();
			if (entry.isEmpty())
				continue;
			
			final String[] fields = entry.split(":");
			if (fields.length != 2)
			{
				LOGGER.warn("Rebirth: coste invalido '{}'. Formato: itemId:cantidad (ej: 57:100, 6393:100).", entry);
				continue;
			}
			
			try
			{
				final int itemId = Integer.parseInt(fields[0].trim());
				final long amount = Long.parseLong(fields[1].trim());
				if (itemId <= 0 || amount <= 0)
				{
					LOGGER.warn("Rebirth: coste ignorado '{}' (el itemId y la cantidad deben ser mayores a 0).", entry);
					continue;
				}
				
				cost.put(itemId, cost.getOrDefault(itemId, 0L) + amount);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn("Rebirth: coste invalido '{}' (itemId y cantidad deben ser numeros).", entry);
			}
		}
		
		return cost;
	}
	// ------------------------------------------------------------------
	// Character data
	// ------------------------------------------------------------------
	
	public static int getRebirths(Player player)
	{
		return getData(player).rebirths;
	}
	
	public static void store(Player player)
	{
		if (player == null)
			return;
		
		final RebirthData data = DATA.get(player.getObjectId());
		if (data != null && data.dirty)
			save(player.getObjectId(), data);
	}
	
	public static void unload(Player player)
	{
		if (player == null)
			return;
		
		store(player);
		DATA.remove(player.getObjectId());
	}
	
	private static RebirthData getData(Player player)
	{
		return DATA.computeIfAbsent(player.getObjectId(), id -> loadFromDb(id));
	}
	
	private static RebirthData loadFromDb(int objectId)
	{
		final RebirthData data = new RebirthData();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT rebirths FROM character_rebirth WHERE char_id=?"))
		{
			ps.setInt(1, objectId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					data.rebirths = rs.getInt("rebirths");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Rebirth: could not load data for char {} ({}).", objectId, e.getMessage());
		}
		
		return data;
	}
	
	private static void save(int objectId, RebirthData data)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO character_rebirth (char_id, rebirths, power_points) VALUES (?,?,0) ON DUPLICATE KEY UPDATE rebirths=VALUES(rebirths)"))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, data.rebirths);
			ps.executeUpdate();
			
			data.dirty = false;
		}
		catch (Exception e)
		{
			LOGGER.warn("Rebirth: could not save data for char {} ({}).", objectId, e.getMessage());
		}
	}
	
	// ------------------------------------------------------------------
	// Stat application - straight into the calculator, no skills
	// ------------------------------------------------------------------
	
	/**
	 * Limpia los bonuses permanentes del Rebirth (per-rebirth + Poderes).<br>
	 * Ya NO re-inyecta stats: el sistema de Poderes quedo desactivado porque MUL mal configurado
	 * bajaba el pAtk (y otros stats) a valores rotos/negativos.
	 */
	public static void refreshBonuses(Player player)
	{
		if (player == null)
			return;
		
		player.removeStatsByOwner(BONUS_OWNER);
		player.updateAndBroadcastStatus(2);
	}
	// ------------------------------------------------------------------
	// Rebirth action
	// ------------------------------------------------------------------
	
	/**
	 * Runs the rebirth for this Player, after checking every requirement.
	 * @param player The Player asking for a rebirth.
	 * @return True when the rebirth was performed.
	 */
	public static boolean doRebirth(Player player)
	{
		if (!ENABLED)
		{
			player.sendMessage("El sistema de Rebirth esta desactivado.");
			return false;
		}
		
		if (player.isAlikeDead() || player.isInOlympiadMode() || player.isInDuel() || player.isInStoreMode())
		{
			player.sendMessage("No podes renacer ahora.");
			return false;
		}
		
		if (player.getLevel() < MIN_LEVEL)
		{
			player.sendMessage("Necesitas nivel " + MIN_LEVEL + " para renacer.");
			return false;
		}
		
		final RebirthData data = getData(player);
		if (MAX_REBIRTHS > 0 && data.rebirths >= MAX_REBIRTHS)
		{
			player.sendMessage("Ya alcanzaste el maximo de " + MAX_REBIRTHS + " renacimientos.");
			return false;
		}
		
		// Payment: the explicit multi-currency table (Price1, Price2, ...) wins; otherwise the legacy price is used.
		final Map<Integer, Long> cost = getCost(data.rebirths + 1);
		if (!cost.isEmpty())
		{
			// Verify-all-then-charge-all: if one currency is missing, nothing is taken.
			if (!chargeCost(player, cost))
				return false;
		}
		else
		{
			final long price = getPrice(data.rebirths);
			if (player.getInventory().getInventoryItemCount(Config.REBIRTH_PRICE_ID, 0) < price)
			{
				player.sendMessage("No tenes " + price + " x " + getPriceItemName() + ".");
				return false;
			}
			
			final long extraPrice = getExtraPrice(data.rebirths);
			if (extraPrice > 0 && player.getInventory().getInventoryItemCount(EXTRA_PRICE_ID, 0) < extraPrice)
			{
				player.sendMessage("No tenes " + extraPrice + " x " + getExtraPriceItemName() + ".");
				return false;
			}
			
			player.destroyItemByItemId("RebirthCost", Config.REBIRTH_PRICE_ID, (int) price, player, true);
			if (extraPrice > 0)
				player.destroyItemByItemId("RebirthExtraCost", EXTRA_PRICE_ID, (int) extraPrice, player, true);
		}
		
		// Counters.
		data.rebirths++;
		data.dirty = true;
		save(player.getObjectId(), data);

		// Level reset.
		player.getStat().setLevel((byte) DELEVEL);
		player.getStat().setExp(player.getStat().getExpForLevel(DELEVEL));
		player.sendSkillList();

		// Clean up any leftover bonus/power state from the old (removed) system.
		refreshBonuses(player);
		
		// Optional reward item (same behaviour as the legacy NPC system).
		if (Config.REBIRTH_REWARD_ID > 0 && Config.REBIRTH_REWARD_COUNT > 0)
		{
			player.getInventory().addItem("RebirthReward", Config.REBIRTH_REWARD_ID, Config.REBIRTH_REWARD_COUNT, player, player.getTarget());
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(Config.REBIRTH_REWARD_ID));
		}
		
		if (ANNOUNCE)
		{
			// Private message first: the player always sees his own running count,
			// even if the global announcement is disabled or he is the only one online.
			final String selfMessage = formatPlaceholders(MESSAGE_SELF, player, data.rebirths);
			if (!selfMessage.isEmpty())
				player.sendMessage(selfMessage);
			
			final String announceMessage = formatPlaceholders(ANNOUNCE_TEXT, player, data.rebirths);
			if (!announceMessage.isEmpty() && !"NONE".equals(ANNOUNCE_CHANNEL))
			{
				switch (ANNOUNCE_CHANNEL)
				{
					case "CRITICAL":
						World.announceToOnlinePlayers(announceMessage, true);
						break;
					
					case "EVENT":
						Broadcast.gameAnnounceToOnlinePlayers(announceMessage);
						break;
					
					case "HERO":
						World.announceToOnlinePlayersHeroVoice(announceMessage);
						break;
					
					default:
						World.announceToOnlinePlayers(announceMessage);
						break;
				}
			}
		}
		
		player.broadcastPacket(new MagicSkillUse(player, player, CAST_SKILL_ID, 1, 1, 0));
		player.broadcastUserInfo();
		player.getInventory().updateDatabase();
		player.sendPacket(new ItemList(player, true));
		
		return true;
	}
	
	// ------------------------------------------------------------------
	// Community Board pages
	// ------------------------------------------------------------------
	
	public static String buildShrineHtml(Player player)
	{
		String html = HtmCache.getInstance().getHtm(CB_PATH + "main.html");
		if (html == null)
			return null;

		html = fillCommon(html, player);
		html = html.replace("%rb_status%", statusOf(player));
		return html;
	}

	private static String fillCommon(String html, Player player)
	{
		final int rebirths = getRebirths(player);
		final long extra = getExtraPrice(rebirths);

		html = html.replace("%rb_count%", String.valueOf(rebirths));
		html = html.replace("%rb_max%", MAX_REBIRTHS > 0 ? String.valueOf(MAX_REBIRTHS) : "-");
		html = html.replace("%rb_next_index%", String.valueOf(rebirths + 1));
		html = html.replace("%rb_level%", String.valueOf(player.getLevel()));
		html = html.replace("%rb_min_level%", String.valueOf(MIN_LEVEL));
		html = html.replace("%rb_delevel%", String.valueOf(DELEVEL));
		// Cost / inventory texts work in both modes (table: "100 x Adena, 100 x Medalla" | legacy: "1 x Medal").
		html = html.replace("%rb_next_price%", getCostText(rebirths + 1));
		html = html.replace("%rb_price_name%", "");
		html = html.replace("%rb_extra_price%", hasCostTable() ? "" : String.valueOf(extra));
		html = html.replace("%rb_extra_price_name%", hasCostTable() || extra <= 0 ? "" : getExtraPriceItemName());
		return html;
	}
	private static String statusOf(Player player)
	{
		if (!ENABLED)
			return "Sistema desactivado.";
		
		final int rebirths = getRebirths(player);
		if (MAX_REBIRTHS > 0 && rebirths >= MAX_REBIRTHS)
			return "Alcanzaste el maximo de renacimientos.";
		
		if (player.getLevel() < MIN_LEVEL)
			return "Necesitas nivel " + MIN_LEVEL + " (tenes " + player.getLevel() + ").";
		
		// Table mode: report every currency that is missing.
		if (hasCostTable())
		{
			final StringBuilder missing = new StringBuilder(64);
			for (Map.Entry<Integer, Long> entry : getCost(rebirths + 1).entrySet())
			{
				final long have = player.getInventory().getInventoryItemCount(entry.getKey(), 0);
				if (have >= entry.getValue())
					continue;
				
				if (missing.length() > 0)
					missing.append(", ");
				
				missing.append("Te faltan ").append(entry.getValue() - have).append(" x ").append(getItemName(entry.getKey()));
			}
			
			return missing.length() == 0 ? "Estas listo para renacer." : missing.append('.').toString();
		}
		
		final long price = getPrice(rebirths);
		final long have = player.getInventory().getInventoryItemCount(Config.REBIRTH_PRICE_ID, 0);
		if (have < price)
			return "Te faltan " + (price - have) + " x " + getPriceItemName() + ".";
		
		final long extra = getExtraPrice(rebirths);
		if (extra > 0)
		{
			final long haveExtra = player.getInventory().getInventoryItemCount(EXTRA_PRICE_ID, 0);
			if (haveExtra < extra)
				return "Te faltan " + (extra - haveExtra) + " x " + getExtraPriceItemName() + ".";
		}
		
		return "Estas listo para renacer.";
	}
	
	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------
	
	/**
	 * Replaces the message placeholders used by {@link #MESSAGE_SELF} and {@link #ANNOUNCE_TEXT}.
	 * @param template The raw template.
	 * @param player The player that just rebirthed.
	 * @param rebirths The new rebirth count.
	 * @return The filled text (empty when the template is empty).
	 */
	private static String formatPlaceholders(String template, Player player, int rebirths)
	{
		if (template == null || template.trim().isEmpty())
			return "";
		
		String text = template;
		text = text.replace("%name%", player.getName());
		text = text.replace("%count%", String.valueOf(rebirths));
		text = text.replace("%max%", MAX_REBIRTHS > 0 ? String.valueOf(MAX_REBIRTHS) : "-");
		text = text.replace("%level%", String.valueOf(DELEVEL));
		text = text.replace("%points%", "0"); // Power Points system removed.
		text = text.replace("%next_price%", getCostText(rebirths + 1));
		text = text.replace("%price_name%", hasCostTable() ? "" : getPriceItemName());
		return text.trim();
	}
	
	private static double[] parseDoubles(String raw)
	{
		if (raw == null || raw.trim().isEmpty())
			return new double[0];
		
		final String[] parts = raw.split("[,;\\s]+");
		final List<Double> values = new ArrayList<>(parts.length);
		for (String part : parts)
		{
			if (part.isEmpty())
				continue;
			
			try
			{
				values.add(Double.parseDouble(part));
			}
			catch (NumberFormatException e)
			{
				// skip invalid entry
			}
		}
		
		final double[] result = new double[values.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = values.get(i);
		
		return result;
	}
	
	private static class RebirthData
	{
		private int rebirths;
		private boolean dirty;
	}
}
