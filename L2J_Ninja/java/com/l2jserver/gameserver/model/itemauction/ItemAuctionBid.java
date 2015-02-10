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
package com.l2jserver.gameserver.model.itemauction;

import com.l2jserver.gameserver.model.World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Forsaiken
 */
public final class ItemAuctionBid
{
	private final int _playerObjId;
	private long _lastBid;
	
	public ItemAuctionBid(final int playerObjId, final long lastBid)
	{
		_playerObjId = playerObjId;
		_lastBid = lastBid;
	}
	
	public final int getPlayerObjId()
	{
		return _playerObjId;
	}
	
	public final long getLastBid()
	{
		return _lastBid;
	}
	
	final void setLastBid(final long lastBid)
	{
		_lastBid = lastBid;
	}
	
	final void cancelBid()
	{
		_lastBid = -1;
	}
	
	final boolean isCanceled()
	{
		return _lastBid <= 0;
	}
	
	final L2PcInstance getPlayer()
	{
		return World.getInstance().getPlayer(_playerObjId);
	}
}