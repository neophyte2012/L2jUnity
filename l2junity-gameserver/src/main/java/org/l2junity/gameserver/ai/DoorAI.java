/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.ai;

import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.DoorInstance;
import org.l2junity.gameserver.model.actor.instance.L2DefenderInstance;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * @author mkizub
 */
public class DoorAI extends CharacterAI
{
	public DoorAI(DoorInstance door)
	{
		super(door);
	}
	
	@Override
	protected void onIntentionIdle()
	{
	}
	
	@Override
	protected void onIntentionActive()
	{
	}
	
	@Override
	protected void onIntentionRest()
	{
	}
	
	@Override
	protected void onIntentionAttack(Creature target)
	{
	}
	
	@Override
	protected void onIntentionCast(Skill skill, WorldObject target, ItemInstance item, boolean forceUse, boolean dontMove)
	{
	}
	
	@Override
	protected void onIntentionMoveTo(Location destination)
	{
	}
	
	@Override
	protected void onIntentionFollow(Creature target)
	{
	}
	
	@Override
	protected void onIntentionPickUp(WorldObject item)
	{
	}
	
	@Override
	protected void onIntentionInteract(WorldObject object)
	{
	}
	
	@Override
	protected void onEvtThink()
	{
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		ThreadPoolManager.getInstance().executeGeneral(new onEventAttackedDoorTask((DoorInstance) _actor, attacker));
	}
	
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}
	
	@Override
	protected void onEvtActionBlocked(Creature attacker)
	{
	}
	
	@Override
	protected void onEvtRooted(Creature attacker)
	{
	}
	
	@Override
	protected void onEvtReadyToAct()
	{
	}
	
	@Override
	protected void onEvtArrived()
	{
	}
	
	@Override
	protected void onEvtArrivedRevalidate()
	{
	}
	
	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_loc)
	{
	}
	
	@Override
	protected void onEvtForgetObject(WorldObject object)
	{
	}
	
	@Override
	protected void onEvtCancel()
	{
	}
	
	@Override
	protected void onEvtDead()
	{
	}
	
	private class onEventAttackedDoorTask implements Runnable
	{
		private final DoorInstance _door;
		private final Creature _attacker;
		
		public onEventAttackedDoorTask(DoorInstance door, Creature attacker)
		{
			_door = door;
			_attacker = attacker;
		}
		
		@Override
		public void run()
		{
			World.getInstance().forEachVisibleObject(_door, L2DefenderInstance.class, guard ->
			{
				if (_actor.isInsideRadius(guard, guard.getTemplate().getClanHelpRange(), true, true))
				{
					guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 15);
				}
			});
		}
	}
	
}
