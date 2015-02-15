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
package org.l2junity.log.formatter;

import java.util.logging.LogRecord;

import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.skills.Skill;

public class DamageFormatter extends AbstractFormatter
{
	@Override
	public String format(LogRecord record)
	{
		final Object[] params = record.getParameters();
		final StringBuilder output = new StringBuilder(32 + record.getMessage().length() + (params != null ? 10 * params.length : 0));
		output.append(super.format(record));
		
		if (params != null)
		{
			for (Object p : params)
			{
				if (p == null)
				{
					continue;
				}
				
				if (p instanceof Creature)
				{
					final Creature creature = (Creature) p;
					if ((p instanceof Attackable) && ((Attackable) p).isRaid())
					{
						output.append("RaidBoss ");
					}
					
					output.append(creature.getName());
					output.append("(");
					output.append(creature.getObjectId());
					output.append(") ");
					output.append(creature.getLevel());
					output.append(" lvl");
					
					if (p instanceof Summon)
					{
						L2PcInstance owner = ((Summon) p).getOwner();
						if (owner != null)
						{
							output.append(" Owner:");
							output.append(owner.getName());
							output.append("(");
							output.append(owner.getObjectId());
							output.append(")");
						}
					}
				}
				else if (p instanceof Skill)
				{
					output.append(" with skill ");
					output.append(p);
				}
				else
				{
					output.append(p);
				}
			}
		}
		
		output.append(System.lineSeparator());
		return output.toString();
	}
}
