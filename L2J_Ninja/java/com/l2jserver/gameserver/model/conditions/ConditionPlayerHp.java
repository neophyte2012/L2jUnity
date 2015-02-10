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
package com.l2jserver.gameserver.model.conditions;

import com.l2jserver.gameserver.model.actor.Creature;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.skills.Skill;

/**
 * The Class ConditionPlayerHp.
 * @author mr
 */
public class ConditionPlayerHp extends Condition
{
	private final int _hp;
	
	/**
	 * Instantiates a new condition player hp.
	 * @param hp the hp
	 */
	public ConditionPlayerHp(int hp)
	{
		_hp = hp;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, L2Item item)
	{
		return (effector != null) && (((effector.getCurrentHp() * 100) / effector.getMaxHp()) <= _hp);
	}
}
