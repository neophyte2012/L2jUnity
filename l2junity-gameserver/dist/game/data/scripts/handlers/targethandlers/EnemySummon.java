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

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;

/**
 * @author UnAfraid
 */
public class EnemySummon implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		if (target.isSummon())
		{
			final Summon targetSummon = (Summon) target;
			if ((activeChar.isPlayer() && (activeChar.getPet() != targetSummon) && activeChar.hasServitor(targetSummon.getObjectId()) && !targetSummon.isDead() && ((targetSummon.getOwner().getPvpFlag() != 0) || (targetSummon.getOwner().getReputation() < 0))) || (targetSummon.getOwner().isInsideZone(ZoneId.PVP) && activeChar.getActingPlayer().isInsideZone(ZoneId.PVP)) || (targetSummon.getOwner().isInDuel() && activeChar.getActingPlayer().isInDuel() && (targetSummon.getOwner().getDuelId() == activeChar.getActingPlayer().getDuelId())))
			{
				return new Creature[]
				{
					targetSummon
				};
			}
		}
		return EMPTY_TARGET_LIST;
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.ENEMY_SUMMON;
	}
}
