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
package org.l2junity.gameserver.model.stats;

import org.l2junity.gameserver.model.actor.Creature;

/**
 * Basic property type of skills. <BR>
 * Before Goddess of Destruction, BaseStats was used. CON for physical, MEN for magical, and others for special cases. <BR>
 * After, only 3 types are used: physical, magic and none. <BR>
 * <BR>
 * @author Nik
 */
public enum BasicProperty
{
	NONE,
	PHYSICAL,
	MAGIC;
	
	public int calcBonus(Creature creature)
	{
		return creature.getTemplate().getBasicPropertyValue(this);
	}
}