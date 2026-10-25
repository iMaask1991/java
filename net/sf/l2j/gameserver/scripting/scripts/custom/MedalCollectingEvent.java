package net.sf.l2j.gameserver.scripting.scripts.custom;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class MedalCollectingEvent extends Quest
{
	private static final String qn = "MedalCollectingEvent";
	
	private static final int ROY = 31228;
	private static final int WINNIE = 31229;
	
	private static final int[] BADGES = { 0, 6399, 6400, 6401, 6402 };
	private static final String[] RANK_NAME = { "no level", "Rabbit", "Hyena", "Fox", "Wolf" };
	private static final String[] ROY_SHOP = { "3122801", "3122802", "3122803", "3122804", "3122805" };
	
	private static final SpawnLocation[] ROY_LOCS =
	{
		new SpawnLocation(82247, 148614, -3464, 0),
		new SpawnLocation(147451, 27703, -2270, 0),
		new SpawnLocation(-80889, 149301, -3040, 0),
		new SpawnLocation(18743, 145358, -3047, 0),
		new SpawnLocation(-12686, 122862, -3112, 0),
		new SpawnLocation(111063, 218987, -3537, 0),
		new SpawnLocation(81083, 56202, -1552, 0),
		new SpawnLocation(117147, 76626, -2688, 0),
		new SpawnLocation(148087, -55446, -2728, 0),
		new SpawnLocation(43918, -47713, -792, 0),
		new SpawnLocation(87113, -141339, -1336, 0),
		new SpawnLocation(-84153, 243144, -3729, 0)
	};
	
	private static final SpawnLocation[] WINNIE_LOCS =
	{
		new SpawnLocation(82250, 148560, -3464, 0),
		new SpawnLocation(147485, 27658, -2270, 0),
		new SpawnLocation(-80840, 149255, -3040, 0),
		new SpawnLocation(18790, 145310, -3047, 0),
		new SpawnLocation(-12640, 122820, -3112, 0),
		new SpawnLocation(111110, 218940, -3537, 0),
		new SpawnLocation(81130, 56155, -1552, 0),
		new SpawnLocation(117190, 76580, -2688, 0),
		new SpawnLocation(148130, -55400, -2728, 0),
		new SpawnLocation(43960, -47670, -792, 0),
		new SpawnLocation(87155, -141290, -1336, 0),
		new SpawnLocation(-84100, 243190, -3729, 0)
	};
	
	public MedalCollectingEvent()
	{
		super(-1, "custom");
		
		addStartNpc(ROY, WINNIE);
		addTalkId(ROY, WINNIE);
		
		if (!Config.MEDAL_EVENT_ENABLED)
			return;
		
		for (SpawnLocation loc : ROY_LOCS)
			addSpawn(ROY, loc, false, 0, false);
		for (SpawnLocation loc : WINNIE_LOCS)
			addSpawn(WINNIE, loc, false, 0, false);
		
		Announcement.AnnounceEvents("Medal Collecting Event is active. Hunt monsters and trade medals with Roy the Cat.");
	}
	
	public static void tryDrop(Monster monster, Player player)
	{
		if (!Config.MEDAL_EVENT_ENABLED || monster == null || player == null)
			return;
		
		if (monster.getLevel() < Config.MEDAL_EVENT_MIN_NPC_LEVEL)
			return;
		
		if (!Config.MEDAL_EVENT_ALLOW_RAID && monster.isRaidRelated())
			return;
		
		if (!Config.MEDAL_EVENT_ALLOW_MINIONS && monster.isMinion())
			return;
		
		if (!Config.MEDAL_EVENT_DROP_CUSTOM_MOBS && monster.getNpcId() >= 50000)
			return;
		
		rollAndGive(monster, player, Config.MEDAL_EVENT_MEDAL_ID, Config.MEDAL_EVENT_MEDAL_CHANCE);
		rollAndGive(monster, player, Config.MEDAL_EVENT_GLITTERING_ID, Config.MEDAL_EVENT_GLITTERING_CHANCE);
	}
	
	private static void rollAndGive(Monster monster, Player player, int itemId, double chancePercent)
	{
		if (chancePercent <= 0)
			return;
		
		if (Rnd.get(10000) >= (int) (chancePercent * 100.0))
			return;
		
		player.addItem("MedalEvent", itemId, 1, monster, true);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		
		if (npc.getNpcId() == ROY)
		{
			openRoyShop(st, npc, player);
			return null;
		}
		return tossHtml(st);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		
		if (!Config.MEDAL_EVENT_ENABLED)
			return "<html><body>The Medal Collecting Event is currently closed. Meow!</body></html>";
		
		if ("prizes".equalsIgnoreCase(event) || "medals".equalsIgnoreCase(event))
		{
			openRoyShop(st, npc, player);
			return null;
		}
		
		if ("toss".equalsIgnoreCase(event))
			return tossHtml(st);
		
		if ("game".equalsIgnoreCase(event) || "play".equalsIgnoreCase(event))
			return gameHtml(st);
		
		if ("heads".equalsIgnoreCase(event) || "tails".equalsIgnoreCase(event))
			return doToss(st, event);
		
		if (npc != null && npc.getNpcId() == WINNIE)
			return tossHtml(st);
		
		openRoyShop(st, npc, player);
		return null;
	}
	
	private static int getRank(QuestState st)
	{
		for (int i = BADGES.length - 1; i >= 1; i--)
		{
			if (st.getQuestItemsCount(BADGES[i]) > 0)
				return i;
		}
		return 0;
	}
	
	private static void openRoyShop(QuestState st, Npc npc, Player player)
	{
		int rank = Math.max(0, Math.min(4, getRank(st)));
		MultisellData.getInstance().separateAndSend(ROY_SHOP[rank], player, npc, false);
	}
	
	private static String tossHtml(QuestState st)
	{
		int rank = getRank(st);
		if (rank >= 4)
		{
			return "<html><body>Event Manager Winnie the Cat:<br>"
				+ "A <font color=\"LEVEL\">Wolf</font> level collector, eh? Meow!<br>"
				+ "That's as high as it goes. Take your medals to Roy the Cat for the best prizes!<br>"
				+ "<a action=\"bypass npc_%objectId%_Chat 0\">Back</a></body></html>";
		}
		
		int cost = Config.MEDAL_EVENT_RANK_COST[rank];
		return "<html><body>Event Manager Winnie the Cat:<br>"
			+ "A <font color=\"LEVEL\">" + RANK_NAME[rank] + " collector</font>, eh?<br>"
			+ "<font color=\"LEVEL\">If you'd like to try to raise your level, give me " + cost + " glittering medals and I'll play a game with you! Meow!</font> If you lose, you can always try again!<br>"
			+ "The game will help you develop the qualities you'll need as a collector. Meow! Your abilities are evaluated by the number of glittering medals you collect and your luck is tested by the number of times you win the game.<br>"
			+ "You can play as long as you have glittering medals! Meow!! Would you like to give it a try?<br>"
			+ "<a action=\"bypass -h Quest MedalCollectingEvent game\">I want to play the coin-toss game.</a><br>"
			+ "<a action=\"bypass npc_%objectId%_Chat 0\">Back</a></body></html>";
	}
	
	private static String gameHtml(QuestState st)
	{
		int rank = getRank(st);
		if (rank >= 4)
			return tossHtml(st);
		
		int cost = Config.MEDAL_EVENT_RANK_COST[rank];
		if (st.getQuestItemsCount(Config.MEDAL_EVENT_GLITTERING_ID) < cost)
		{
			return "<html><body>Event Manager Winnie the Cat:<br>"
				+ "You don't have enough glittering medals to play the coin-toss game. Collect some more medals and come back. Meow!!<br>"
				+ "<a action=\"bypass npc_%objectId%_Chat 0\">Back</a></body></html>";
		}
		
		return "<html><body>Event Manager Winnie the Cat:<br>"
			+ "I'll toss a coin, and you call whether it will be heads or tails. Meow!<br>"
			+ "What do you think? Heads or tails? Meow~!<br><br>"
			+ "<a action=\"bypass -h Quest MedalCollectingEvent heads\">Heads!</a><br>"
			+ "<a action=\"bypass -h Quest MedalCollectingEvent tails\">Tails!</a><br><br>"
			+ "<a action=\"bypass -h Quest MedalCollectingEvent toss\">Back</a></body></html>";
	}
	
	private static String doToss(QuestState st, String choice)
	{
		int rank = getRank(st);
		if (rank >= 4)
			return tossHtml(st);
		
		int cost = Config.MEDAL_EVENT_RANK_COST[rank];
		if (st.getQuestItemsCount(Config.MEDAL_EVENT_GLITTERING_ID) < cost)
			return gameHtml(st);
		
		st.takeItems(Config.MEDAL_EVENT_GLITTERING_ID, cost);
		
		boolean win = Rnd.get(100) < Config.MEDAL_EVENT_TOSS_CHANCE;
		String pick = "heads".equalsIgnoreCase(choice) ? "Heads" : "Tails";
		
		if (!win)
		{
			return "<html><body>Event Manager Winnie the Cat:<br>"
				+ "You called <font color=\"LEVEL\">" + pick + "</font>... and you lost! Meow...<br>"
				+ "Try again if you still have glittering medals!<br>"
				+ "<a action=\"bypass -h Quest MedalCollectingEvent toss\">Try again</a><br>"
				+ "<a action=\"bypass npc_%objectId%_Chat 0\">Back</a></body></html>";
		}
		
		int newRank = rank + 1;
		st.setState(STATE_STARTED);
		st.set("rank", String.valueOf(newRank));
		if (rank > 0)
			st.takeItems(BADGES[rank], -1);
		st.giveItems(BADGES[newRank], 1);
		
		return "<html><body>Event Manager Winnie the Cat:<br>"
			+ "You called <font color=\"LEVEL\">" + pick + "</font>... and you won! Meow!!<br>"
			+ "You are now a <font color=\"LEVEL\">" + RANK_NAME[newRank] + "</font> collector.<br>"
			+ "Take your medals to Roy the Cat for better prizes!<br>"
			+ "<a action=\"bypass npc_%objectId%_Chat 0\">Back</a></body></html>";
	}
}
