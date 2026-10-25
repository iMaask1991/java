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

package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.taskmanager.AutoGoldBar;

/** @author Mega @version 1.0
 */

public class Menu implements IVoicedCommandHandler

{
	
	private static final String[] _voicedCommands =
	
	{
		
		"menu",
		"setPartyRefuse",
		"setTradeRefuse",
		"setMessageRefuse",
		"setAutoGb",
		"setAutoFarm",
	
	};
	
//	private static final String ACTIVED = "<font color=00FF00>ON</font>";
//	private static final String DESATIVED = "<font color=FF0000>OFF</font>";
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	
	{
		
		if (command.equals("menu") && Config.MENU_PANEL)
			showHtml(activeChar);
		
		else if (command.equals("setPartyRefuse"))
		
		{
			
			if (activeChar.isPartyInRefuse())
				
				activeChar.setIsPartyInRefuse(false);
			
			else
				
				activeChar.setIsPartyInRefuse(true);
			
			showHtml(activeChar);
			
		}
		
		else if (command.equals("setTradeRefuse"))
		
		{
			
			if (activeChar.getTradeRefusal())
				
				activeChar.setTradeRefusal(false);
			else
				
				activeChar.setTradeRefusal(true);
			
			showHtml(activeChar);
			
		}
		
		else if (command.equals("setMessageRefuse"))
		
		{
			
			if (activeChar.isInRefusalMode())
				
				activeChar.setInRefusalMode(false);
			
			else
				
				activeChar.setInRefusalMode(true);
			
			showHtml(activeChar);
			
		}
		
		else if (command.equalsIgnoreCase("setAutoGb"))
		{
			if (activeChar.isAutoGb())
			{
				activeChar.setAutoGb(false);
				AutoGoldBar.getInstance().remove(activeChar);
			}
			else
			{
				activeChar.setAutoGb(true);
				AutoGoldBar.getInstance().add(activeChar);
			}
			showHtml(activeChar);
		}
		
		
		else if (command.equalsIgnoreCase("setAutofarm"))
		{
			if (!Config.AUTOFARM_ENABLED)
			{
				activeChar.sendMessage("Auto Farm is disabled.");
				return true;
			}
			VoicedAutoFarm.showAutoFarm(activeChar);
		}
		
		return true;
		
	}
	
	private static void showHtml(Player activeChar)
	
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		
		html.setFile("data/html/mods/menu/menu.htm");
		
//		html.replace("%partyRefusal%", activeChar.isPartyInRefuse() ? ACTIVED : DESATIVED);
		html.replace("%partyRefusal%", activeChar.isPartyInRefuse() ? "checked" : "unable");		
		
//		html.replace("%tradeRefusal%", activeChar.getTradeRefusal() ? ACTIVED : DESATIVED);
		html.replace("%tradeRefusal%", activeChar.getTradeRefusal() ? "checked" : "unable");
		
//		html.replace("%messageRefusal%", activeChar.isInRefusalMode() ? ACTIVED : DESATIVED);
		html.replace("%messageRefusal%", activeChar.isInRefusalMode() ? "checked" : "unable");
		
//		html.replace("%setAutoGb%", activeChar.isAutoGb() ? ACTIVED : DESATIVED);
		html.replace("%setAutoGb%", activeChar.isAutoGb() ? "checked" : "unable");
		
		activeChar.sendPacket(html);
		
	}
	
	@Override
	public String[] getVoicedCommandList()
	
	{
		
		return _voicedCommands;
		
	}
	
}