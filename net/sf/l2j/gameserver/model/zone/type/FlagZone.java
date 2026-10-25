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
package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.ZoneType;

public class FlagZone extends ZoneType
{
	public FlagZone(int id)
	{
		super(id);
	}
	
    @Override
    protected void onEnter(Creature character)
    {
        if (character instanceof Player)
        {
        ((Player) character).setPvpFlag(1);
        ((Player) character).broadcastUserInfo();
        }
        character.setInsideZone(ZoneId.FLAG_ZONE, true);
    }
    
    @Override
    protected void onExit(Creature character)
    {
        if (character instanceof Player)
        {
        	 ((Player) character).setPvpFlag(0);
        	 ((Player) character).broadcastUserInfo();
        }
        character.setInsideZone(ZoneId.FLAG_ZONE, false);
    }
}