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
package org.l2junity.gameserver.model.instancezone;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.entity.Instance;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Basic instance zone data transfer object.
 * @author Zoey76
 */
public class InstanceWorld
{
	private int _instanceId;
	private int _templateId = -1;
	private final Set<Integer> _allowed = ConcurrentHashMap.newKeySet();
	private final AtomicInteger _status = new AtomicInteger();
	
	public Set<Integer> getAllowed()
	{
		return _allowed;
	}
	
	public void removeAllowed(int id)
	{
		_allowed.remove(id);
	}
	
	public void addAllowed(int id)
	{
		_allowed.add(id);
	}
	
	public boolean isAllowed(int id)
	{
		return _allowed.contains(id);
	}
	
	/**
	 * Gets the dynamically generated instance ID.
	 * @return the instance ID
	 */
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * Sets the instance ID.
	 * @param instanceId the instance ID
	 */
	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
	
	/**
	 * Gets the client's template instance ID.
	 * @return the template ID
	 */
	public int getTemplateId()
	{
		return _templateId;
	}
	
	/**
	 * Sets the template ID.
	 * @param templateId the template ID
	 */
	public void setTemplateId(int templateId)
	{
		_templateId = templateId;
	}
	
	public int getStatus()
	{
		return _status.get();
	}
	
	public boolean isStatus(int status)
	{
		return _status.get() == status;
	}
	
	public void setStatus(int status)
	{
		_status.set(status);
	}
	
	public void incStatus()
	{
		_status.incrementAndGet();
	}
	
	/**
	 * @param killer
	 * @param victim
	 */
	public void onDeath(Creature killer, Creature victim)
	{
		if ((victim != null) && victim.isPlayer())
		{
			final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
			if (instance != null)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.IF_YOU_ARE_NOT_RESURRECTED_WITHIN_S1_MINUTE_S_YOU_WILL_BE_EXPELLED_FROM_THE_INSTANT_ZONE);
				sm.addInt(instance.getEjectTime() / 60 / 1000);
				victim.getActingPlayer().sendPacket(sm);
				instance.addEjectDeadTask(victim.getActingPlayer());
			}
		}
	}
}
