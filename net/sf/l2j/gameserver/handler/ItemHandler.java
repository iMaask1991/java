package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.handler.itemhandlers.AioCoin;
import net.sf.l2j.gameserver.handler.itemhandlers.AttributeAlchemyOrb;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion1;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion10;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion11;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion12;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion2;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion3;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion4;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion5;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion6;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion7;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion8;
import net.sf.l2j.gameserver.handler.itemhandlers.Agathion9;
import net.sf.l2j.gameserver.handler.itemhandlers.BeastSoulShot;
import net.sf.l2j.gameserver.handler.itemhandlers.BeastSpice;
import net.sf.l2j.gameserver.handler.itemhandlers.BeastSpiritShot;
import net.sf.l2j.gameserver.handler.itemhandlers.BlessedSpiritShot;
import net.sf.l2j.gameserver.handler.itemhandlers.BlockedBuff;
import net.sf.l2j.gameserver.handler.itemhandlers.Book;
import net.sf.l2j.gameserver.handler.itemhandlers.Calculator;
import net.sf.l2j.gameserver.handler.itemhandlers.CambiarSexo;
import net.sf.l2j.gameserver.handler.itemhandlers.ClanFull;
import net.sf.l2j.gameserver.handler.itemhandlers.CustomManaPotion;
import net.sf.l2j.gameserver.handler.itemhandlers.CustomBox;
import net.sf.l2j.gameserver.handler.itemhandlers.Elfo;
import net.sf.l2j.gameserver.handler.itemhandlers.ElfoOscuro;
import net.sf.l2j.gameserver.handler.itemhandlers.Elixir;
import net.sf.l2j.gameserver.handler.itemhandlers.Enano;
import net.sf.l2j.gameserver.handler.itemhandlers.EnchantScrolls;
import net.sf.l2j.gameserver.handler.itemhandlers.FastAugmentation;
import net.sf.l2j.gameserver.handler.itemhandlers.FishShots;
import net.sf.l2j.gameserver.handler.itemhandlers.Harvester;
import net.sf.l2j.gameserver.handler.itemhandlers.Humano;
import net.sf.l2j.gameserver.handler.itemhandlers.HumanoMago;
import net.sf.l2j.gameserver.handler.itemhandlers.ItemSkills;
import net.sf.l2j.gameserver.handler.itemhandlers.Keys;
import net.sf.l2j.gameserver.handler.itemhandlers.Maps;
import net.sf.l2j.gameserver.handler.itemhandlers.MercTicket;
import net.sf.l2j.gameserver.handler.itemhandlers.NamesTradeables;
import net.sf.l2j.gameserver.handler.itemhandlers.Noblesse;
import net.sf.l2j.gameserver.handler.itemhandlers.Orco;
import net.sf.l2j.gameserver.handler.itemhandlers.OrcoMago;
import net.sf.l2j.gameserver.handler.itemhandlers.PaganKeys;
import net.sf.l2j.gameserver.handler.itemhandlers.PetFood;
import net.sf.l2j.gameserver.handler.itemhandlers.Recipes;
import net.sf.l2j.gameserver.handler.itemhandlers.RollingDice;
import net.sf.l2j.gameserver.handler.itemhandlers.ScrollOfResurrection;
import net.sf.l2j.gameserver.handler.itemhandlers.SeedHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.SevenSignsRecord;
import net.sf.l2j.gameserver.handler.itemhandlers.SoulCrystals;
import net.sf.l2j.gameserver.handler.itemhandlers.SoulShots;
import net.sf.l2j.gameserver.handler.itemhandlers.SpecialXMas;
import net.sf.l2j.gameserver.handler.itemhandlers.SpiritShot;
import net.sf.l2j.gameserver.handler.itemhandlers.StreamerItem1;
import net.sf.l2j.gameserver.handler.itemhandlers.StreamerItem2;
import net.sf.l2j.gameserver.handler.itemhandlers.StreamerItem3;
import net.sf.l2j.gameserver.handler.itemhandlers.StreamerItem4;
import net.sf.l2j.gameserver.handler.itemhandlers.StreamerItem5;
import net.sf.l2j.gameserver.handler.itemhandlers.SummonItems;
import net.sf.l2j.gameserver.handler.itemhandlers.GoldBar;
import net.sf.l2j.gameserver.handler.itemhandlers.HeroCoin;
import net.sf.l2j.gameserver.handler.itemhandlers.VipCoin;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;

public class ItemHandler
{
	private final Map<Integer, IItemHandler> _entries = new HashMap<>();
	
	protected ItemHandler()
	{
		registerHandler(new BeastSoulShot());
		registerHandler(new BeastSpice());
		registerHandler(new BeastSpiritShot());
		registerHandler(new BlessedSpiritShot());
		registerHandler(new Book());
		registerHandler(new Calculator());
		registerHandler(new Elixir());
		registerHandler(new EnchantScrolls());
		registerHandler(new FastAugmentation());
		registerHandler(new FishShots());
		registerHandler(new Harvester());
		registerHandler(new ItemSkills());
		registerHandler(new Keys());
		registerHandler(new Maps());
		registerHandler(new MercTicket());
		registerHandler(new PaganKeys());
		registerHandler(new PetFood());
		registerHandler(new Recipes());
		registerHandler(new RollingDice());
		registerHandler(new ScrollOfResurrection());
		registerHandler(new SeedHandler());
		registerHandler(new SevenSignsRecord());
		registerHandler(new SoulShots());
		registerHandler(new SpecialXMas());
		registerHandler(new SoulCrystals());
		registerHandler(new SpiritShot());
		registerHandler(new SummonItems());
		registerHandler(new Humano());
		registerHandler(new HumanoMago());
		registerHandler(new Elfo());
		registerHandler(new ElfoOscuro());
		registerHandler(new Orco());
		registerHandler(new OrcoMago());
		registerHandler(new Enano());
		registerHandler(new CambiarSexo());
		registerHandler(new NamesTradeables());
		registerHandler(new StreamerItem1());
		registerHandler(new StreamerItem2());
		registerHandler(new StreamerItem3());
		registerHandler(new StreamerItem4());
		registerHandler(new StreamerItem5());
		registerHandler(new ClanFull());
		registerHandler(new BlockedBuff());
		registerHandler(new VipCoin());
		registerHandler(new HeroCoin());
		registerHandler(new GoldBar());
		registerHandler(new Noblesse());
		registerHandler(new CustomManaPotion());
		registerHandler(new AioCoin());
		registerHandler(new CustomBox());
		registerHandler(new AttributeAlchemyOrb()); // Attribute Alchemy (Sockets)
		registerHandler(new net.sf.l2j.gameserver.handler.itemhandlers.ElementStone()); // Elementos de items
		//Agathions
		registerHandler(new Agathion());
		registerHandler(new Agathion1());
		registerHandler(new Agathion2());
		registerHandler(new Agathion3());
		registerHandler(new Agathion4());
		registerHandler(new Agathion5());
		registerHandler(new Agathion6());
		registerHandler(new Agathion7());
		registerHandler(new Agathion8());
		registerHandler(new Agathion9());
		registerHandler(new Agathion10());
		registerHandler(new Agathion11());
		registerHandler(new Agathion12());

	}
	
	private void registerHandler(IItemHandler handler)
	{
		_entries.put(handler.getClass().getSimpleName().intern().hashCode(), handler);
	}
	
	public IItemHandler getHandler(EtcItem item)
	{
		if (item == null || item.getHandlerName() == null)
			return null;
		
		return _entries.get(item.getHandlerName().hashCode());
	}
	
	public int size()
	{
		return _entries.size();
	}
	
	public static ItemHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemHandler INSTANCE = new ItemHandler();
	}
}
