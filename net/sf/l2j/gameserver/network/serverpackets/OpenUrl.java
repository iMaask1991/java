package net.sf.l2j.gameserver.network.serverpackets;

/**
 * @author Elfocrash
 *
 */
public class OpenUrl extends L2GameServerPacket
{
    private final String _url;
    
    public OpenUrl(String url)
    {
        _url = url;
    }
    
    @Override
    protected final void writeImpl()
    {
        writeC(0x70);
        writeS(_url);
    }
}