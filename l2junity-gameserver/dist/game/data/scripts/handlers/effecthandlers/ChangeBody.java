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
package handlers.effecthandlers;

import java.util.HashSet;
import java.util.Set;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.holders.TemplateChanceHolder;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * Transformation type effect, which disables attack or use of skills.
 * @author Nik
 */
public final class ChangeBody extends AbstractEffect
{
	private final Set<TemplateChanceHolder> _transformations = new HashSet<>();
	
	public ChangeBody(StatsSet params)
	{
		for (StatsSet item : params.getList("templates", StatsSet.class))
		{
			_transformations.add(new TemplateChanceHolder(item.getInt(".templateId"), item.getInt(".minChance"), item.getInt(".maxChance")));
		}
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return !info.getEffected().isDoor();
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		final int chance = Rnd.get(100);
		//@formatter:off
		_transformations.stream()
			.filter(t -> t.calcChance(chance)) // Calculate chance for each transformation.
			.mapToInt(TemplateChanceHolder::getTemplateId)
			.findAny()
			.ifPresent(id -> effected.transform(id, false)); // Transform effected to whatever successful random template without adding skills.
		//@formatter:on
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().stopTransformation(false);
	}
}
