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
package handlers.effecthandlers;

import org.l2junity.gameserver.enums.AttributeType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author Sdw
 */
public class AttackAttribute extends AbstractEffect
{
	private final AttributeType _attribute;
	private final double _amount;
	
	public AttackAttribute(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		_amount = params.getDouble("amount", 0);
		_attribute = params.getEnum("stat", AttributeType.class, AttributeType.FIRE);
	}
	
	@Override
	public void pump(Creature effected, Skill skill)
	{
		Stats stat = Stats.FIRE_POWER;
		
		switch (_attribute)
		{
			case WATER:
			{
				stat = Stats.WATER_POWER;
				break;
			}
			case WIND:
			{
				stat = Stats.WIND_POWER;
				break;
			}
			case EARTH:
			{
				stat = Stats.EARTH_POWER;
				break;
			}
			case HOLY:
			{
				stat = Stats.HOLY_POWER;
				break;
			}
			case DARK:
			{
				stat = Stats.DARK_POWER;
				break;
			}
		}
		effected.getStat().mergeAdd(stat, _amount);
	}
}
