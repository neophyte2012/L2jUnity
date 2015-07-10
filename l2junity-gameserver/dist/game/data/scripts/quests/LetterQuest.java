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
package quests;

import org.l2junity.gameserver.enums.HtmlActionScope;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerBypass;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLevelChanged;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogin;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerPressTutorialMark;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.PlaySound;
import org.l2junity.gameserver.network.client.send.TutorialCloseHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowQuestionMark;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Abstract class for quests "Letters from the Queen" and "Kekropus' Letter"
 * @author malyelfik
 */
public abstract class LetterQuest extends Quest
{
	private int startSOE;
	private Location startTeleport;
	private NpcStringId startMessage;
	private String startQuestSound;
	
	public LetterQuest(int questId, String name, String descr)
	{
		super(questId, name, descr);
	}
	
	/**
	 * Sets additional conditions that must be met to show question mark or start quest.
	 * @param player player trying start quest
	 * @return {@code true} when additional conditions are met, otherwise {@code false}
	 */
	public boolean canShowTutorialMark(PlayerInstance player)
	{
		return true;
	}
	
	/**
	 * Sets start quest sound.<br>
	 * This sound will be played when player clicks on the tutorial question mark.
	 * @param sound name of sound
	 */
	public final void setStartQuestSound(String sound)
	{
		startQuestSound = sound;
	}
	
	/**
	 * Sets quest level restrictions.
	 * @param min minimum player's level to start quest
	 * @param max maximum player's level to start quest
	 */
	public final void setLevel(int min, int max)
	{
		addCondLevel(min, max, "");
	}
	
	/**
	 * Sets start location of quest.<br>
	 * When player starts quest, he will receive teleport scroll with id {@code itemId}.<br>
	 * When tutorial window is displayed, he can also teleport to location {@code loc} using HTML bypass.
	 * @param itemId id of item which player gets on quest start
	 * @param loc place where player will be teleported
	 */
	public final void setStartLocation(int itemId, Location loc)
	{
		startSOE = itemId;
		startTeleport = loc;
	}
	
	/**
	 * Sets if quest is only for Ertheia characters or not.
	 * @param val {@code true} means {@code Race.ERTHEIA}, {@code false} means other
	 */
	public final void setIsErtheiaQuest(boolean val)
	{
		if (val)
		{
			addCondRace(Race.ERTHEIA, "");
			startMessage = NpcStringId.QUEEN_NAVARI_HAS_SENT_A_LETTER_NCLICK_THE_QUESTION_MARK_ICON_TO_READ;
		}
		else
		{
			addCondNotRace(Race.ERTHEIA, "");
			startMessage = NpcStringId.KEKROPUS_LETTER_HAS_ARRIVED_NCLICK_THE_QUESTION_MARK_ICON_TO_READ;
		}
	}
	
	/**
	 * Gets teleport command associated with current quest.
	 * @return command in form Q<i>questId</i>_teleport (<i>questId</i> is replaced with original quest id)
	 */
	private String getTeleportCommand()
	{
		return "Q" + getId() + "_teleport";
	}
	
	@Override
	public boolean canStartQuest(PlayerInstance player)
	{
		return canShowTutorialMark(player) && super.canStartQuest(player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		final PlayerInstance player = event.getActiveChar();
		if ((event.getMarkId() == getId()) && canStartQuest(player))
		{
			final String html = getHtm(player.getHtmlPrefix(), "popup.html").replace("%teleport%", getTeleportCommand());
			final QuestState st = getQuestState(player, true);
			st.startQuest();
			
			player.sendPacket(new PlaySound(3, startQuestSound, 0, 0, 0, 0, 0));
			player.sendPacket(new TutorialShowHtml(html));
			giveItems(player, startSOE, 1);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerBypass(OnPlayerBypass event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if (event.getCommand().equals(getTeleportCommand()))
		{
			if ((st != null) && st.isCond(1) && hasQuestItems(player, startSOE))
			{
				if (player.isTransformed())
				{
					showOnScreenMsg(player, NpcStringId.YOU_CANNOT_TELEPORT_WHILE_IN_A_TRANSFORMED_STATE, ExShowScreenMessage.TOP_CENTER, 5000);
				}
				else if (player.isInCombat())
				{
					showOnScreenMsg(player, NpcStringId.YOU_CANNOT_TELEPORT_IN_COMBAT, ExShowScreenMessage.TOP_CENTER, 5000);
				}
				else
				{
					player.teleToLocation(startTeleport);
					takeItems(player, startSOE, -1);
				}
			}
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if ((st == null) && (event.getOldLevel() < event.getNewLevel()) && canStartQuest(player))
		{
			showOnScreenMsg(player, startMessage, ExShowScreenMessage.TOP_CENTER, 10000);
			player.sendPacket(new TutorialShowQuestionMark(getId()));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if ((st == null) && canStartQuest(player))
		{
			showOnScreenMsg(player, startMessage, ExShowScreenMessage.TOP_CENTER, 10000);
			player.sendPacket(new TutorialShowQuestionMark(getId()));
		}
	}
	
	@Override
	public void onQuestAborted(PlayerInstance player)
	{
		final QuestState st = getQuestState(player, true);
		
		st.startQuest();
		player.sendPacket(SystemMessageId.THIS_QUEST_CANNOT_BE_DELETED);
	}
}