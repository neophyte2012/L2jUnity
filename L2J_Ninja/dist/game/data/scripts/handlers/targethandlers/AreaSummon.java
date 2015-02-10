/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.targethandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.l2jserver.gameserver.handler.ITargetTypeHandler;
import com.l2jserver.gameserver.model.WorldObject;
import com.l2jserver.gameserver.model.actor.Creature;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.skills.targets.L2TargetType;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public class AreaSummon implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<Creature> targetList = new ArrayList<>();
		if ((target == null) || !target.isServitor() || target.isDead())
		{
			return EMPTY_TARGET_LIST;
		}
		
		if (onlyFirst)
		{
			if (activeChar.hasSummon())
			{
				return new Creature[]
				{
					activeChar.getServitors().values().stream().findFirst().orElse(activeChar.getPet())
				};
			}
		}
		
		final boolean srcInArena = (activeChar.isInsideZone(ZoneId.PVP) && !activeChar.isInsideZone(ZoneId.SIEGE));
		final Collection<Creature> objs = target.getKnownList().getKnownCharacters();
		int maxTargets = skill.getAffectLimit();
		
		for (Creature obj : objs)
		{
			if ((obj == null) || (obj == target) || (obj == activeChar))
			{
				continue;
			}
			
			if (!Util.checkIfInRange(skill.getAffectRange(), target, obj, true))
			{
				continue;
			}
			
			if (!(obj.isAttackable() || obj.isPlayable()))
			{
				continue;
			}
			
			if (!Skill.checkForAreaOffensiveSkills(activeChar, obj, skill, srcInArena))
			{
				continue;
			}
			
			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				break;
			}
			
			targetList.add(obj);
		}
		
		if (targetList.isEmpty())
		{
			return EMPTY_TARGET_LIST;
		}
		
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.AREA_SUMMON;
	}
}
