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
 * The Class ConditionLogicNot.
 * @author mkizub
 */
public class ConditionLogicNot extends Condition
{
	private final Condition _condition;
	
	/**
	 * Instantiates a new condition logic not.
	 * @param condition the condition
	 */
	public ConditionLogicNot(Condition condition)
	{
		_condition = condition;
		if (getListener() != null)
		{
			_condition.setListener(this);
		}
	}
	
	@Override
	void setListener(ConditionListener listener)
	{
		if (listener != null)
		{
			_condition.setListener(this);
		}
		else
		{
			_condition.setListener(null);
		}
		super.setListener(listener);
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, L2Item item)
	{
		return !_condition.test(effector, effected, skill, item);
	}
}
