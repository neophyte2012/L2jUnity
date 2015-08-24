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
package ai.npc.AdventureGuildsman;

import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.send.ExShowQuestInfo;

import ai.npc.AbstractNpcAI;

/**
 * Adventurers Guidsman AI.
 * @author St3eT
 */
public final class AdventureGuildsman extends AbstractNpcAI
{
	// NPCs
	// @formatter:off
	private static final int[] ADVENTURERS_GUILDSMAN =
	{
		31787, 31788, 31789,
		31790, 31791, 31792,
		31793, 31794, 31795,
		31796, 31797, 31798,
		31799, 31800, 31804,
		31805, 31806, 31808,
		31809, 31814, 31815,
		31816, 31818, 31822,
		31823, 31824, 31825,
		31826, 31827, 31828,
		31829, 31830, 31831,
		31832, 31833, 31834,
		31835, 31837, 31838,
		31840, 31841, 31991,
		31992, 31993, 31995,
		33946,
	};
	// @formatter:on
	// Skills
	
	private AdventureGuildsman()
	{
		super(AdventureGuildsman.class.getSimpleName(), "ai/npc");
		addStartNpc(ADVENTURERS_GUILDSMAN);
		addTalkId(ADVENTURERS_GUILDSMAN);
		addFirstTalkId(ADVENTURERS_GUILDSMAN);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		
		switch (event)
		{
			case "AdventureGuildsman-Aden.html":
			case "AdventureGuildsman-Dion.html":
			case "AdventureGuildsman-Giran.html":
			case "AdventureGuildsman-Gludin.html":
			case "AdventureGuildsman-Gludio.html":
			case "AdventureGuildsman-Godard.html":
			case "AdventureGuildsman-Heine.html":
			case "AdventureGuildsman-Hunter.html":
			case "AdventureGuildsman-Oren.html":
			case "AdventureGuildsman-Rune.html":
			case "AdventureGuildsman-Schuttgart.html":
			case "AdventureGuildsman-Help.html":
			case "AdventureGuildsman-ManagerHelp.html":
			case "AdventureGuildsman-RaidbossInfo.html":
			{
				htmltext = event;
				break;
			}
			case "questList":
			{
				player.sendPacket(ExShowQuestInfo.STATIC_PACKET);
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new AdventureGuildsman();
	}
}