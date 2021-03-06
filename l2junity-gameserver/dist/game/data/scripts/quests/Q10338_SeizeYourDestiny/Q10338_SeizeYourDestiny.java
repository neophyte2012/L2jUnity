/*
 * Copyright (C) 2004-2016 L2J Unity
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
package quests.Q10338_SeizeYourDestiny;

import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.Movie;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

/**
 * Seize Your Destiny (10338)
 * @author Sdw
 */
public final class Q10338_SeizeYourDestiny extends Quest
{
	// NPCs
	private static final int CELLPHINE = 33477;
	private static final int HADEL = 33344;
	private static final int HERMUNCUS = 33340;
	// Monsters
	private static final int HARNAKS_WRAITH = 27445;
	// Items
	private static final ItemHolder SCROLL_OF_AFTERLIFE = new ItemHolder(17600, 1);
	private static final ItemHolder STEEL_DOOR_GUILD_COIN = new ItemHolder(37045, 400);
	// Locations
	private static final Location RELIQUARY_OF_THE_GIANT = new Location(-114962, 226564, -2864);
	// Misc
	private static final int MIN_LV = 85;
	
	public Q10338_SeizeYourDestiny()
	{
		super(10338);
		addStartNpc(CELLPHINE);
		addTalkId(CELLPHINE, HADEL, HERMUNCUS);
		addKillId(HARNAKS_WRAITH);
		addCondNotRace(Race.ERTHEIA, "33477-08.htm");
		addCondNotClassId(ClassId.JUDICATOR, "");
		addCondIsNotSubClassActive("");
		addCondMinLevel(MIN_LV, "33477-07.htm");
		addCondInCategory(CategoryType.FOURTH_CLASS_GROUP, "33477-07.htm");
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		
		switch (event)
		{
			case "TELEPORT":
			{
				if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "";
					break;
				}
				player.teleToLocation(RELIQUARY_OF_THE_GIANT, null);
				playMovie(player, Movie.SC_AWAKENING_VIEW);
				break;
			}
			case "33477-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "33344-05.htm":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "33340-02.htm":
			{
				if (qs.isCond(3))
				{
					showOnScreenMsg(player, NpcStringId.YOU_MAY_USE_SCROLL_OF_AFTERLIFE_FROM_HERMUNCUS_TO_AWAKEN, ExShowScreenMessage.TOP_CENTER, 10000);
					giveItems(player, SCROLL_OF_AFTERLIFE);
					rewardItems(player, STEEL_DOOR_GUILD_COIN);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "33344-02.htm":
			case "33344-03.htm":
			case "33344-04.htm":
			case "33477-02.htm":
			{
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (npc.getId())
		{
			case CELLPHINE:
			{
				if (qs.isStarted())
				{
					htmltext = "33477-06.htm";
				}
				if (hasQuestItems(player, SCROLL_OF_AFTERLIFE.getId()) || qs.isCompleted())
				{
					htmltext = "33477-05.htm";
				}
				else if (qs.isCreated())
				{
					htmltext = "33477-01.htm";
				}
				break;
			}
			case HADEL:
			{
				if (qs.isCompleted() || player.isInCategory(CategoryType.AWAKEN_GROUP) || hasQuestItems(player, SCROLL_OF_AFTERLIFE.getId()))
				{
					htmltext = "33344-07.htm";
				}
				else if (player.getLevel() < 85)
				{
					htmltext = "33344-06.htm";
				}
				else if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "33344-09.htm";
				}
				else
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "33344-01.htm";
							break;
						}
						case 2:
						{
							htmltext = "33344-08.htm";
							break;
						}
						case 3:
						{
							htmltext = "33344-07.htm";
							break;
						}
					}
				}
				break;
			}
			case HERMUNCUS:
			{
				if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "33340-04.htm";
					break;
				}
				else if (qs.isCond(3))
				{
					htmltext = "33340-01.htm";
					break;
				}
				else if (hasQuestItems(player, SCROLL_OF_AFTERLIFE.getId()))
				{
					htmltext = "33340-03.htm";
					break;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		if (npc.getId() == HARNAKS_WRAITH)
		{
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isCond(2))
			{
				qs.setCond(3, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
}