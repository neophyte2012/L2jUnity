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
package org.l2junity.gameserver.data.xml.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.model.ActionDataHolder;
import org.l2junity.gameserver.model.StatsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author UnAfraid
 */
public class ActionData implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionData.class);
	
	private final Map<Integer, ActionDataHolder> _actionData = new HashMap<>();
	
	protected ActionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_actionData.clear();
		parseDatapackFile("data/ActionData.xml");
		LOGGER.info("Loaded {} player actions.", _actionData.size());
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "action", actionNode ->
		{
			final NamedNodeMap attrs = actionNode.getAttributes();
			final StatsSet set = new StatsSet();
			for (int i = 0; i < attrs.getLength(); i++)
			{
				Node att = attrs.item(i);
				set.set(att.getNodeName(), att.getNodeValue());
			}
			
			final ActionDataHolder holder = new ActionDataHolder(set);
			_actionData.put(holder.getId(), holder);
		}));
	}
	
	/**
	 * @param id
	 * @return the ActionDataHolder for specified id
	 */
	public ActionDataHolder getActionData(int id)
	{
		return _actionData.get(id);
	}
	
	/**
	 * Gets the single instance of ActionData.
	 * @return single instance of ActionData
	 */
	public static final ActionData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ActionData _instance = new ActionData();
	}
}