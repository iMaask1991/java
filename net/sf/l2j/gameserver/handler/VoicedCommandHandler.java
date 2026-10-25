package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.usercommandhandlers.DressMe;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.BankingCommand;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.CastleManagers;
import net.sf.l2j.gameserver.events.multievent.MultiEventConfig;
import net.sf.l2j.gameserver.events.multievent.VoicedMultiEvent;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Event;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.GainXpSp;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Menu;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Online;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.PlayerStats;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.AioStatus;
import net.sf.l2j.gameserver.events.bossevent.KTBConfig;
import net.sf.l2j.gameserver.events.bossevent.VoicedEventKTB;
import net.sf.l2j.gameserver.events.ctf.CTFConfig;
import net.sf.l2j.gameserver.events.ctf.VoicedCTFEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMConfig;
import net.sf.l2j.gameserver.events.deathmatch.VoicedDMEvent;
import net.sf.l2j.gameserver.events.lastman.LMConfig;
import net.sf.l2j.gameserver.events.lastman.VoicedEventLastMan;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.RaidInfoCommand;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.SellBuffCommand;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoPot;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedClass;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedInstanceFarmExit;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedInstanceFarmStats;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedTimeLeft;

public class VoicedCommandHandler
{
	private final Map<Integer, IVoicedCommandHandler> _datatable = new HashMap<>();
	
	public static VoicedCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected VoicedCommandHandler()
	{
		registerHandler(new VoicedAutoFarm());
		registerHandler(new VoicedAutoPot());
		registerHandler(new VoicedInstanceFarmExit());
		registerHandler(new VoicedInstanceFarmStats());
		registerHandler(new VoicedTimeLeft());
		registerHandler(new Menu());
		registerHandler(new PlayerStats());
		registerHandler(new BankingCommand());
		registerHandler(new CastleManagers());
		registerHandler(new Online());
		registerHandler(new GainXpSp());
		registerHandler(new Event());
		if (MultiEventConfig.ENABLED)
			registerHandler(new VoicedMultiEvent());
		registerHandler(new AioStatus());
		registerHandler(new DressMe());

		if (Config.SELLBUFF_ENABLED)
			registerHandler(new SellBuffCommand());

		registerHandler(new RaidInfoCommand());
		registerHandler(new VoicedClass()); // .class (cambio de clase + premios, mod ruso)

		if (CTFConfig.ALLOW_CTF_COMMANDS)
			registerHandler(new VoicedCTFEvent());

		if (DMConfig.ALLOW_DM_COMMANDS)
			registerHandler(new VoicedDMEvent());

		if (LMConfig.ALLOW_EVENT_COMMANDS)
			registerHandler(new VoicedEventLastMan());

		if (KTBConfig.ALLOW_EVENT_KTB_COMMANDS)
			registerHandler(new VoicedEventKTB());
	}
	
	public void registerHandler(IVoicedCommandHandler handler)
	{
		String[] ids = handler.getVoicedCommandList();
		
		for (int i = 0; i < ids.length; i++)
			_datatable.put(ids[i].hashCode(), handler);
	}
	
	public IVoicedCommandHandler getHandler(String voicedCommand)
	{
		String command = voicedCommand;
		
		if (voicedCommand.indexOf(" ") != -1)
			command = voicedCommand.substring(0, voicedCommand.indexOf(" "));
		
		return _datatable.get(command.hashCode());
	}
	
	public int size()
	{
		return _datatable.size();
	}
	
	private static class SingletonHolder
	{
		protected static final VoicedCommandHandler _instance = new VoicedCommandHandler();
	}
}