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
package quests.Q10406_BeforeDarknessBearsFruit;

import java.util.HashSet;
import java.util.Set;

import org.l2junity.gameserver.enums.QuestSound;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.NpcLogListHolder;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;

import quests.Q10405_KartiasSeed.Q10405_KartiasSeed;

/**
 * Before Darkness Bears Fruit (10406)
 * @author St3eT
 */
public final class Q10406_BeforeDarknessBearsFruit extends Quest
{
	// NPCs
	private static final int SHUVANN = 33867;
	private static final int KARTIAS_FLOWER = 19470;
	// Items
	private static final int EAA = 730; // Scroll: Enchant Armor (A-grade)
	private static final int STEEL_COIN = 37045; // Steel Door Guild Coin
	// Misc
	private static final int MIN_LEVEL = 61;
	private static final int MAX_LEVEL = 65;
	
	public Q10406_BeforeDarknessBearsFruit()
	{
		super(10406, Q10406_BeforeDarknessBearsFruit.class.getSimpleName(), "Before Darkness Bears Fruit");
		addStartNpc(SHUVANN);
		addTalkId(SHUVANN);
		addKillId(KARTIAS_FLOWER);
		addCondNotRace(Race.ERTHEIA, "33867-08.html");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33867-09.htm");
		addCondCompletedQuest(Q10405_KartiasSeed.class.getSimpleName(), "33867-09.htm");
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState st = getQuestState(player, false);
		
		if (st == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "33867-02.htm":
			case "33867-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33867-04.htm":
			{
				st.startQuest();
				htmltext = event;
				break;
			}
			case "33867-07.html":
			{
				if (st.isCond(2))
				{
					st.exitQuest(false, true);
					giveItems(player, EAA, 3);
					giveItems(player, STEEL_COIN, 10);
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExpAndSp(player, 3_125_586, 750);
					}
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				htmltext = "33867-01.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = st.isCond(1) ? "33867-05.html" : "33867-06.html";
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState st = getQuestState(killer, false);
		
		if ((st != null) && st.isStarted() && st.isCond(1))
		{
			int killCount = st.getInt("KILLED_COUNT");
			
			if (killCount < 10)
			{
				killCount++;
				st.set("KILLED_COUNT", killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			
			if (killCount == 10)
			{
				st.setCond(2, true);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(PlayerInstance activeChar)
	{
		final QuestState st = getQuestState(activeChar, false);
		if ((st != null) && st.isStarted() && st.isCond(1))
		{
			final Set<NpcLogListHolder> npcLogList = new HashSet<>(1);
			npcLogList.add(new NpcLogListHolder(KARTIAS_FLOWER, false, st.getInt("KILLED_COUNT")));
			return npcLogList;
		}
		return super.getNpcLogList(activeChar);
	}
}