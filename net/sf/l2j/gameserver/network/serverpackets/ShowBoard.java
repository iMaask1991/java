package net.sf.l2j.gameserver.network.serverpackets;

import java.util.List;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.communitybbs.CommunityBoardTopButtons;

public class ShowBoard extends L2GameServerPacket
{
	public static final ShowBoard STATIC_SHOWBOARD_102 = new ShowBoard(null, "102");
	public static final ShowBoard STATIC_SHOWBOARD_103 = new ShowBoard(null, "103");
	public static final ShowBoard STATIC_CLOSE = new ShowBoard(false);
	
	private final StringBuilder _htmlCode = new StringBuilder();
	private final boolean _show;
	
	public ShowBoard(String htmlCode, String id)
	{
		_show = true;
		StringUtil.append(_htmlCode, id, "\u0008", htmlCode);
	}
	
	public ShowBoard(List<String> arg)
	{
		_show = true;
		_htmlCode.append("1002\u0008");
		for (String str : arg)
			StringUtil.append(_htmlCode, str, " \u0008");
	}
	
	private ShowBoard(boolean show)
	{
		_show = show;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x6e);
		writeC(_show ? 0x01 : 0x00); // 1 to show, 0 to hide
		writeS(CommunityBoardTopButtons.getTop());
		writeS(CommunityBoardTopButtons.getFavorite());
		writeS(CommunityBoardTopButtons.getRegion());
		writeS(CommunityBoardTopButtons.getClan());
		writeS(CommunityBoardTopButtons.getMemo());
		writeS(CommunityBoardTopButtons.getMail());
		writeS(CommunityBoardTopButtons.getFriends());
		writeS(CommunityBoardTopButtons.getAddFavorite());
		writeS(_htmlCode.toString());
	}
}
