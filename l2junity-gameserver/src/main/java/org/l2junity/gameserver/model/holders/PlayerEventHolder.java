/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.holders;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

/**
 * Player event holder, meant for restoring player after event has finished.<br>
 * Allows you to restore following information about player:
 * <ul>
 * <li>Name</li>
 * <li>Title</li>
 * <li>Clan</li>
 * <li>Location</li>
 * <li>PvP Kills</li>
 * <li>PK Kills</li>
 * <li>Karma</li>
 * </ul>
 * @author Nik, xban1x
 */
public final class PlayerEventHolder
{
	private final PlayerInstance _player;
	private final String _name;
	private final String _title;
	private final int _clanId;
	private final Location _loc;
	private final int _pvpKills;
	private final int _pkKills;
	private final int _reputation;
	
	private final Map<PlayerInstance, Integer> _kills = new ConcurrentHashMap<>();
	private boolean _sitForced;
	
	public PlayerEventHolder(PlayerInstance player)
	{
		this(player, false);
	}
	
	public PlayerEventHolder(PlayerInstance player, boolean sitForced)
	{
		_player = player;
		_name = player.getName();
		_title = player.getTitle();
		_clanId = player.getClanId();
		_loc = new Location(player);
		_pvpKills = player.getPvpKills();
		_pkKills = player.getPkKills();
		_reputation = player.getReputation();
		_sitForced = sitForced;
	}
	
	public void restorePlayerStats()
	{
		_player.setName(_name);
		_player.setTitle(_title);
		_player.setClan(ClanTable.getInstance().getClan(_clanId));
		_player.teleToLocation(_loc, true);
		_player.setPvpKills(_pvpKills);
		_player.setPkKills(_pkKills);
		_player.setReputation(_reputation);
		
	}
	
	public void setSitForced(boolean sitForced)
	{
		_sitForced = sitForced;
	}
	
	public boolean isSitForced()
	{
		return _sitForced;
	}
	
	public Map<PlayerInstance, Integer> getKills()
	{
		return Collections.unmodifiableMap(_kills);
	}
	
	public void addKill(PlayerInstance player)
	{
		_kills.merge(player, 1, Integer::sum);
	}
}
