/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.franchus;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.OpenUrl;

public class Franchus
{
	public static final Franchus getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void LinkFace(String command, Player player)
	{
		if (command.startsWith("linkface"))
		{
			player.sendPacket(new OpenUrl(Config.OPEN_URL_FACE));
			closeWindow(player);
		}
	}
	
	public void LinkWeb(String command, Player player)
	{
		if (command.startsWith("linkweb"))
		{
			player.sendPacket(new OpenUrl(Config.OPEN_URL_WEB));
			closeWindow(player);
		}
	}
	
	public void LinkDiscord(String command, Player player)
	{
		if (command.startsWith("linkdiscord"))
		{
			player.sendPacket(new OpenUrl(Config.OPEN_URL_DISCORD));
			closeWindow(player);
		}
	}
	
	public void LinkGrupo(String command, Player player)
	{
		if (command.startsWith("linkgrupo"))
		{
			player.sendPacket(new OpenUrl(Config.OPEN_URL_GRUPO));
			closeWindow(player);
		}
	}
	
	private static void closeWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml("<html><body></body></html>");
		player.sendPacket(html);
	}
	
	private static class SingletonHolder
	{
		protected static final Franchus _instance = new Franchus();
	}
}
