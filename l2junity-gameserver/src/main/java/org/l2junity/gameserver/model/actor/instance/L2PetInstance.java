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
package org.l2junity.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.data.sql.impl.CharSummonTable;
import org.l2junity.gameserver.data.sql.impl.SummonEffectsTable;
import org.l2junity.gameserver.data.sql.impl.SummonEffectsTable.SummonEffect;
import org.l2junity.gameserver.data.xml.impl.PetDataTable;
import org.l2junity.gameserver.data.xml.impl.SkillData;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.InstanceType;
import org.l2junity.gameserver.enums.ItemLocation;
import org.l2junity.gameserver.enums.PartyDistributionType;
import org.l2junity.gameserver.handler.IItemHandler;
import org.l2junity.gameserver.handler.ItemHandler;
import org.l2junity.gameserver.instancemanager.CursedWeaponsManager;
import org.l2junity.gameserver.instancemanager.FortSiegeManager;
import org.l2junity.gameserver.instancemanager.ItemsOnGroundManager;
import org.l2junity.gameserver.model.PetData;
import org.l2junity.gameserver.model.PetLevelData;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.stat.PetStat;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.itemcontainer.PetInventory;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.AbnormalType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.EffectScope;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.gameserver.network.client.send.ExChangeNpcState;
import org.l2junity.gameserver.network.client.send.InventoryUpdate;
import org.l2junity.gameserver.network.client.send.PetInventoryUpdate;
import org.l2junity.gameserver.network.client.send.StopMove;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.taskmanager.DecayTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2PetInstance extends Summon
{
	protected static final Logger _logPet = LoggerFactory.getLogger(L2PetInstance.class);
	
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_pet_skills_save (petObjItemId,skill_id,skill_level,remaining_time,buff_index) VALUES (?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT petObjItemId,skill_id,skill_level,remaining_time,buff_index FROM character_pet_skills_save WHERE petObjItemId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_pet_skills_save WHERE petObjItemId=?";
	
	private int _curFed;
	private final PetInventory _inventory;
	private final int _controlObjectId;
	private boolean _respawned;
	private final boolean _mountable;
	private Future<?> _feedTask;
	private PetData _data;
	private PetLevelData _leveldata;
	
	/** The Experience before the last Death Penalty */
	private long _expBeforeDeath = 0;
	private int _curWeightPenalty = 0;
	
	public final PetLevelData getPetLevelData()
	{
		if (_leveldata == null)
		{
			_leveldata = PetDataTable.getInstance().getPetLevelData(getTemplate().getId(), getStat().getLevel());
		}
		
		return _leveldata;
	}
	
	public final PetData getPetData()
	{
		if (_data == null)
		{
			_data = PetDataTable.getInstance().getPetData(getTemplate().getId());
		}
		
		return _data;
	}
	
	public final void setPetData(PetLevelData value)
	{
		_leveldata = value;
	}
	
	/**
	 * Manage Feeding Task.<BR>
	 * Feed or kill the pet depending on hunger level.<br>
	 * If pet has food in inventory and feed level drops below 55% then consume food from inventory.<br>
	 * Send a broadcastStatusUpdate packet for this L2PetInstance
	 */
	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				final Summon pet = getOwner().getPet();
				if ((getOwner() == null) || (pet == null) || (pet.getObjectId() != getObjectId()))
				{
					stopFeed();
					return;
				}
				else if (getCurrentFed() > getFeedConsume())
				{
					setCurrentFed(getCurrentFed() - getFeedConsume());
				}
				else
				{
					setCurrentFed(0);
				}
				
				broadcastStatusUpdate();
				
				List<Integer> foodIds = getPetData().getFood();
				if (foodIds.isEmpty())
				{
					if (isUncontrollable())
					{
						// Owl Monk remove PK
						if ((getTemplate().getId() == 16050) && (getOwner() != null))
						{
							getOwner().setPkKills(Math.max(0, getOwner().getPkKills() - Rnd.get(1, 6)));
						}
						sendPacket(SystemMessageId.THE_PET_IS_NOW_LEAVING);
						deleteMe(getOwner());
					}
					else if (isHungry())
					{
						sendPacket(SystemMessageId.THERE_IS_NOT_MUCH_TIME_REMAINING_UNTIL_THE_PET_LEAVES);
					}
					return;
				}
				
				ItemInstance food = null;
				for (int id : foodIds)
				{
					food = getInventory().getItemByItemId(id);
					if (food != null)
					{
						break;
					}
				}
				
				if ((food != null) && isHungry())
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
					if (handler != null)
					{
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOUR_PET_WAS_HUNGRY_SO_IT_ATE_S1);
						sm.addItemName(food.getId());
						sendPacket(sm);
						handler.useItem(L2PetInstance.this, food, false);
					}
				}
				
				if (isUncontrollable())
				{
					sendPacket(SystemMessageId.YOUR_PET_IS_STARVING_AND_WILL_NOT_OBEY_UNTIL_IT_GETS_IT_S_FOOD_FEED_YOUR_PET);
				}
			}
			catch (Exception e)
			{
				_logPet.error("Pet [ObjectId: " + getObjectId() + "] a feed task error has occurred", e);
			}
		}
		
		private int getFeedConsume()
		{
			// if pet is attacking
			if (isAttackingNow())
			{
				return getPetLevelData().getPetFeedBattle();
			}
			return getPetLevelData().getPetFeedNormal();
		}
	}
	
	public synchronized static L2PetInstance spawnPet(L2NpcTemplate template, PlayerInstance owner, ItemInstance control)
	{
		if (World.getInstance().getPet(owner.getObjectId()) != null)
		{
			return null; // owner has a pet listed in world
		}
		final PetData data = PetDataTable.getInstance().getPetData(template.getId());
		
		L2PetInstance pet = restore(control, template, owner);
		// add the pet instance to world
		if (pet != null)
		{
			pet.setTitle(owner.getName());
			if (data.isSynchLevel() && (pet.getLevel() != owner.getLevel()))
			{
				pet.getStat().setLevel((byte) owner.getLevel());
				pet.getStat().setExp(pet.getStat().getExpForLevel(owner.getLevel()));
			}
			World.getInstance().addPet(owner.getObjectId(), pet);
		}
		return pet;
	}
	
	/**
	 * Constructor for new pet
	 * @param template
	 * @param owner
	 * @param control
	 */
	public L2PetInstance(L2NpcTemplate template, PlayerInstance owner, ItemInstance control)
	{
		this(template, owner, control, (byte) (template.getDisplayId() == 12564 ? owner.getLevel() : template.getLevel()));
	}
	
	/**
	 * Constructor for restored pet
	 * @param template
	 * @param owner
	 * @param control
	 * @param level
	 */
	public L2PetInstance(L2NpcTemplate template, PlayerInstance owner, ItemInstance control, byte level)
	{
		super(template, owner);
		setInstanceType(InstanceType.L2PetInstance);
		
		_controlObjectId = control.getObjectId();
		
		getStat().setLevel((byte) Math.max(level, PetDataTable.getInstance().getPetMinLevel(template.getId())));
		
		_inventory = new PetInventory(this);
		_inventory.restore();
		
		int npcId = template.getId();
		_mountable = PetDataTable.isMountable(npcId);
		getPetData();
		getPetLevelData();
	}
	
	@Override
	public PetStat getStat()
	{
		return (PetStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PetStat(this));
	}
	
	public boolean isRespawned()
	{
		return _respawned;
	}
	
	@Override
	public int getSummonType()
	{
		return 2;
	}
	
	@Override
	public int getControlObjectId()
	{
		return _controlObjectId;
	}
	
	public ItemInstance getControlItem()
	{
		return getOwner().getInventory().getItemByObjectId(_controlObjectId);
	}
	
	public int getCurrentFed()
	{
		return _curFed;
	}
	
	public void setCurrentFed(int num)
	{
		if (num <= 0)
		{
			sendPacket(new ExChangeNpcState(getObjectId(), 0x64));
		}
		else if ((_curFed <= 0) && (num > 0))
		{
			sendPacket(new ExChangeNpcState(getObjectId(), 0x65));
		}
		_curFed = num > getMaxFed() ? getMaxFed() : num;
	}
	
	/**
	 * Returns the pet's currently equipped weapon instance (if any).
	 */
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getItems(item -> (item.getItemLocation() == ItemLocation.PET_EQUIP) && (item.getItem().getBodyPart() == L2Item.SLOT_R_HAND)).stream().findAny().orElse(null);
	}
	
	/**
	 * Returns the pet's currently equipped weapon (if any).
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		final ItemInstance weapon = getActiveWeaponInstance();
		if (weapon == null)
		{
			return null;
		}
		
		return (Weapon) weapon.getItem();
	}
	
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		// temporary? unavailable
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		// temporary? unavailable
		return null;
	}
	
	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT2);
			}
			
			return false;
		}
		
		// Send Pet inventory update packet
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);
		
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_S_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addLong(count);
				sendPacket(sm);
			}
			else
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT2);
			}
			return false;
		}
		
		// Send Pet inventory update packet
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);
		
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_S_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addLong(count);
				sendPacket(sm);
			}
			else
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	@Override
	public void doPickupItem(WorldObject object)
	{
		if (isDead())
		{
			return;
		}
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		broadcastPacket(new StopMove(this));
		
		if (!(object instanceof ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_logPet.warn(this + " trying to pickup wrong target." + object);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		boolean follow = getFollowStatus();
		final ItemInstance target = (ItemInstance) object;
		
		// Cursed weapons
		if (CursedWeaponsManager.getInstance().isCursed(target.getId()))
		{
			SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
			smsg.addItemName(target.getId());
			sendPacket(smsg);
			return;
		}
		else if (FortSiegeManager.getInstance().isCombat(target.getId()))
		{
			return;
		}
		
		SystemMessage smsg = null;
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isSpawned())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if (((isInParty() && (getParty().getDistributionType() == PartyDistributionType.FINDERS_KEEPERS)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getOwner().getObjectId()) && !getOwner().isInLooterParty(target.getOwnerId()))
			{
				if (target.getId() == Inventory.ADENA_ID)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
					smsg.addLong(target.getCount());
				}
				else if (target.getCount() > 1)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addLong(target.getCount());
				}
				else
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
					smsg.addItemName(target);
				}
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(smsg);
				return;
			}
			
			if ((target.getItemLootShedule() != null) && ((target.getOwnerId() == getOwner().getObjectId()) || getOwner().isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Remove from the ground!
			target.pickupMe(this);
			
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Herbs
		if (target.getItem().hasExImmediateEffect())
		{
			IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler == null)
			{
				_log.warn("No item handler registered for item ID: " + target.getId() + ".");
			}
			else
			{
				handler.useItem(this, target, false);
			}
			
			ItemTable.getInstance().destroyItem("Consume", target, getOwner(), null);
			broadcastStatusUpdate();
		}
		else
		{
			if (target.getId() == Inventory.ADENA_ID)
			{
				smsg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1_ADENA);
				smsg.addLong(target.getCount());
				sendPacket(smsg);
			}
			else if (target.getEnchantLevel() > 0)
			{
				smsg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1_S2);
				smsg.addInt(target.getEnchantLevel());
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			else if (target.getCount() > 1)
			{
				smsg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S2_S1_S);
				smsg.addLong(target.getCount());
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			else
			{
				smsg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			
			// If owner is in party and it isnt finders keepers, distribute the item instead of stealing it -.-
			if (getOwner().isInParty() && (getOwner().getParty().getDistributionType() != PartyDistributionType.FINDERS_KEEPERS))
			{
				getOwner().getParty().distributeItem(getOwner(), target);
			}
			else
			{
				final ItemInstance item = getInventory().addItem("Pickup", target, getOwner(), this);
				// sendPacket(new PetItemList(getInventory().getItems()));
				sendPacket(new PetInventoryUpdate(item));
			}
		}
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		if (follow)
		{
			followOwner();
		}
	}
	
	@Override
	public void deleteMe(PlayerInstance owner)
	{
		getInventory().transferItemsToOwner();
		super.deleteMe(owner);
		destroyControlItem(owner, false); // this should also delete the pet from the db
		CharSummonTable.getInstance().getPets().remove(getOwner().getObjectId());
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		PlayerInstance owner = getOwner();
		if ((owner != null) && !owner.isInDuel() && (!isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE)))
		{
			deathPenalty();
		}
		if (!super.doDie(killer, true))
		{
			return false;
		}
		stopFeed();
		sendPacket(SystemMessageId.THE_PET_HAS_BEEN_KILLED_IF_YOU_DON_T_RESURRECT_IT_WITHIN_24_HOURS_THE_PET_S_BODY_WILL_DISAPPEAR_ALONG_WITH_ALL_THE_PET_S_ITEMS);
		DecayTaskManager.getInstance().add(this);
		// do not decrease exp if is in duel, arena
		return true;
	}
	
	@Override
	public void doRevive()
	{
		getOwner().removeReviving();
		
		super.doRevive();
		
		// stopDecay
		DecayTaskManager.getInstance().cancel(this);
		startFeed();
		if (!isHungry())
		{
			setRunning();
		}
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		// Restore the pet's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}
	
	/**
	 * Transfers item to another inventory
	 * @param process string identifier of process triggering this action
	 * @param objectId Item Identifier of the item to be transfered
	 * @param count Quantity of items to be transfered
	 * @param target
	 * @param actor the player requesting the item transfer
	 * @param reference Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public ItemInstance transferItem(String process, int objectId, long count, Inventory target, PlayerInstance actor, WorldObject reference)
	{
		ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
		ItemInstance playerOldItem = target.getItemByItemId(oldItem.getId());
		ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);
		
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		if ((oldItem.getCount() > 0) && (oldItem != newItem))
		{
			petIU.addModifiedItem(oldItem);
		}
		else
		{
			petIU.addRemovedItem(oldItem);
		}
		sendPacket(petIU);
		
		// Send target update packet
		if (!newItem.isStackable())
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addNewItem(newItem);
			sendInventoryUpdate(iu);
		}
		else if ((playerOldItem != null) && newItem.isStackable())
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(newItem);
			sendInventoryUpdate(iu);
		}
		
		return newItem;
	}
	
	/**
	 * Remove the Pet from DB and its associated item from the player inventory
	 * @param owner The owner from whose inventory we should delete the item
	 * @param evolve
	 */
	public void destroyControlItem(PlayerInstance owner, boolean evolve)
	{
		// remove the pet instance from world
		World.getInstance().removePet(owner.getObjectId());
		
		// delete from inventory
		try
		{
			ItemInstance removedItem;
			if (evolve)
			{
				removedItem = owner.getInventory().destroyItem("Evolve", getControlObjectId(), 1, getOwner(), this);
			}
			else
			{
				removedItem = owner.getInventory().destroyItem("PetDestroy", getControlObjectId(), 1, getOwner(), this);
				if (removedItem != null)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(removedItem);
					owner.sendPacket(sm);
				}
			}
			
			if (removedItem == null)
			{
				_log.warn("Couldn't destroy pet control item for " + owner + " pet: " + this + " evolve: " + evolve);
			}
			else
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(removedItem);
				
				owner.sendInventoryUpdate(iu);
				owner.broadcastUserInfo();
				
				World.getInstance().removeObject(removedItem);
			}
		}
		catch (Exception e)
		{
			_logPet.warn("Error while destroying control item: " + e.getMessage(), e);
		}
		
		// pet control item no longer exists, delete the pet from the db
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?"))
		{
			statement.setInt(1, getControlObjectId());
			statement.execute();
		}
		catch (Exception e)
		{
			_logPet.error("Failed to delete Pet [ObjectId: " + getObjectId() + "]", e);
		}
	}
	
	public void dropAllItems()
	{
		try
		{
			for (ItemInstance item : getInventory().getItems())
			{
				dropItemHere(item);
			}
		}
		catch (Exception e)
		{
			_logPet.warn("Pet Drop Error: " + e.getMessage(), e);
		}
	}
	
	public void dropItemHere(ItemInstance dropit, boolean protect)
	{
		dropit = getInventory().dropItem("Drop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);
		
		if (dropit != null)
		{
			if (protect)
			{
				dropit.getDropProtection().protect(getOwner());
			}
			_logPet.debug("Item id to drop: " + dropit.getId() + " amount: " + dropit.getCount());
			dropit.dropMe(this, getX(), getY(), getZ() + 100);
		}
	}
	
	public void dropItemHere(ItemInstance dropit)
	{
		dropItemHere(dropit, false);
	}
	
	/**
	 * @return Returns the mount able.
	 */
	@Override
	public boolean isMountable()
	{
		return _mountable;
	}
	
	private static L2PetInstance restore(ItemInstance control, L2NpcTemplate template, PlayerInstance owner)
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?"))
		{
			L2PetInstance pet;
			statement.setInt(1, control.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				if (!rset.next())
				{
					return new L2PetInstance(template, owner, control);
				}
				
				pet = new L2PetInstance(template, owner, control, rset.getByte("level"));
				
				pet._respawned = true;
				pet.setName(rset.getString("name"));
				
				long exp = rset.getLong("exp");
				PetLevelData info = PetDataTable.getInstance().getPetLevelData(pet.getId(), pet.getLevel());
				// DS: update experience based by level
				// Avoiding pet delevels due to exp per level values changed.
				if ((info != null) && (exp < info.getPetMaxExp()))
				{
					exp = info.getPetMaxExp();
				}
				
				pet.getStat().setExp(exp);
				pet.getStat().setSp(rset.getInt("sp"));
				
				pet.getStatus().setCurrentHp(rset.getInt("curHp"));
				pet.getStatus().setCurrentMp(rset.getInt("curMp"));
				pet.getStatus().setCurrentCp(pet.getMaxCp());
				if (rset.getDouble("curHp") < 1)
				{
					pet.setIsDead(true);
					pet.stopHpMpRegeneration();
				}
				
				pet.setCurrentFed(rset.getInt("fed"));
			}
			return pet;
		}
		catch (Exception e)
		{
			_logPet.warn("Could not restore pet data for owner: " + owner + " - " + e.getMessage(), e);
		}
		return null;
	}
	
	@Override
	public void setRestoreSummon(boolean val)
	{
		_restoreSummon = val;
	}
	
	@Override
	public final void stopSkillEffects(boolean removed, int skillId)
	{
		super.stopSkillEffects(removed, skillId);
		List<SummonEffect> effects = SummonEffectsTable.getInstance().getPetEffects().get(getControlObjectId());
		if ((effects != null) && !effects.isEmpty())
		{
			for (SummonEffect effect : effects)
			{
				if (effect.getSkill().getId() == skillId)
				{
					SummonEffectsTable.getInstance().getPetEffects().get(getControlObjectId()).remove(effect);
				}
			}
		}
	}
	
	@Override
	public void storeMe()
	{
		if (getControlObjectId() == 0)
		{
			// this is a summon, not a pet, don't store anything
			return;
		}
		
		if (!Config.RESTORE_PET_ON_RECONNECT)
		{
			_restoreSummon = false;
		}
		
		String req;
		if (!isRespawned())
		{
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,ownerId,restore,item_obj_id) " + "VALUES (?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,ownerId=?,restore=? " + "WHERE item_obj_id = ?";
		}
		
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(req))
		{
			statement.setString(1, getName());
			statement.setInt(2, getStat().getLevel());
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, getStat().getExp());
			statement.setLong(6, getStat().getSp());
			statement.setInt(7, getCurrentFed());
			statement.setInt(8, getOwner().getObjectId());
			statement.setString(9, String.valueOf(_restoreSummon)); // True restores pet on login
			statement.setInt(10, getControlObjectId());
			statement.executeUpdate();
			
			_respawned = true;
			
			if (_restoreSummon)
			{
				CharSummonTable.getInstance().getPets().put(getOwner().getObjectId(), getControlObjectId());
			}
			else
			{
				CharSummonTable.getInstance().getPets().remove(getOwner().getObjectId());
			}
		}
		catch (Exception e)
		{
			_logPet.error("Failed to store Pet [ObjectId: " + getObjectId() + "] data", e);
		}
		
		ItemInstance itemInst = getControlItem();
		if ((itemInst != null) && (itemInst.getEnchantLevel() != getStat().getLevel()))
		{
			itemInst.setEnchantLevel(getStat().getLevel());
			itemInst.updateDatabase();
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.SUMMON_STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		// Clear list for overwrite
		SummonEffectsTable.getInstance().getPetEffects().getOrDefault(getControlObjectId(), Collections.emptyList()).clear();
		
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement ps2 = con.prepareStatement(ADD_SKILL_SAVE))
		{
			// Delete all current stored effects for summon to avoid dupe
			ps1.setInt(1, getControlObjectId());
			ps1.execute();
			
			int buff_index = 0;
			
			final Set<Integer> storedSkills = new HashSet<>();
			
			// Store all effect data along with calculated remaining
			if (storeEffects)
			{
				for (BuffInfo info : getEffectList().getEffects())
				{
					if (info == null)
					{
						continue;
					}
					
					final Skill skill = info.getSkill();
					
					// Do not store those effects.
					if (skill.isDeleteAbnormalOnLeave())
					{
						continue;
					}
					
					// Do not save heals.
					if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
					{
						continue;
					}
					
					// Toggles are skipped, unless they are necessary to be always on.
					if (skill.isToggle() && !skill.isNecessaryToggle())
					{
						continue;
					}
					
					// Dances and songs are not kept in retail.
					if (skill.isDance() && !Config.ALT_STORE_DANCES)
					{
						continue;
					}
					
					if (!storedSkills.add(skill.getReuseHashCode()))
					{
						continue;
					}
					
					ps2.setInt(1, getControlObjectId());
					ps2.setInt(2, skill.getId());
					ps2.setInt(3, skill.getLevel());
					ps2.setInt(4, info.getTime());
					ps2.setInt(5, ++buff_index);
					ps2.execute();
					
					SummonEffectsTable.getInstance().getPetEffects().computeIfAbsent(getControlObjectId(), k -> new CopyOnWriteArrayList<>()).add(new SummonEffect(skill, info.getTime()));
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Could not store pet effect data: ", e);
		}
	}
	
	@Override
	public void restoreEffects()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps1 = con.prepareStatement(RESTORE_SKILL_SAVE);
			PreparedStatement ps2 = con.prepareStatement(DELETE_SKILL_SAVE))
		{
			if (!SummonEffectsTable.getInstance().getPetEffects().containsKey(getControlObjectId()))
			{
				ps1.setInt(1, getControlObjectId());
				try (ResultSet rset = ps1.executeQuery())
				{
					while (rset.next())
					{
						int effectCurTime = rset.getInt("remaining_time");
						
						final Skill skill = SkillData.getInstance().getSkill(rset.getInt("skill_id"), rset.getInt("skill_level"));
						if (skill == null)
						{
							continue;
						}
						
						if (skill.hasEffects(EffectScope.GENERAL))
						{
							SummonEffectsTable.getInstance().getPetEffects().computeIfAbsent(getControlObjectId(), k -> new CopyOnWriteArrayList<>()).add(new SummonEffect(skill, effectCurTime));
						}
					}
				}
			}
			
			ps2.setInt(1, getControlObjectId());
			ps2.executeUpdate();
		}
		catch (Exception e)
		{
			_log.warn("Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			if (SummonEffectsTable.getInstance().getPetEffects().get(getControlObjectId()) == null)
			{
				return;
			}
			
			for (SummonEffect se : SummonEffectsTable.getInstance().getPetEffects().get(getControlObjectId()))
			{
				if (se != null)
				{
					se.getSkill().applyEffects(this, this, false, se.getEffectCurTime());
				}
			}
		}
	}
	
	public synchronized void stopFeed()
	{
		if (_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
		}
	}
	
	public synchronized void startFeed()
	{
		// stop feeding task if its active
		
		stopFeed();
		if (!isDead() && (getOwner().getPet() == this))
		{
			_feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
		}
	}
	
	@Override
	public synchronized void unSummon(PlayerInstance owner)
	{
		stopFeed();
		stopHpMpRegeneration();
		super.unSummon(owner);
		
		if (!isDead())
		{
			if (getInventory() != null)
			{
				getInventory().deleteMe();
			}
			World.getInstance().removePet(owner.getObjectId());
		}
	}
	
	/**
	 * Restore the specified % of experience this L2PetInstance has lost.<BR>
	 * <BR>
	 * @param restorePercent
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getStat().getExp()) * restorePercent) / 100));
			_expBeforeDeath = 0;
		}
	}
	
	private void deathPenalty()
	{
		// TODO: Need Correct Penalty
		
		int lvl = getStat().getLevel();
		double percentLost = (-0.07 * lvl) + 6.5;
		
		// Calculate the Experience loss
		long lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);
		
		// Get the Experience before applying penalty
		_expBeforeDeath = getStat().getExp();
		
		// Set the new Experience value of the L2PetInstance
		getStat().addExp(-lostExp);
	}
	
	@Override
	public void addExpAndSp(long addToExp, long addToSp)
	{
		if (getId() == 12564)
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
		}
		else
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
		}
	}
	
	@Override
	public long getExpForThisLevel()
	{
		return getStat().getExpForLevel(getLevel());
	}
	
	@Override
	public long getExpForNextLevel()
	{
		return getStat().getExpForLevel(getLevel() + 1);
	}
	
	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}
	
	public int getMaxFed()
	{
		return getStat().getMaxFeed();
	}
	
	@Override
	public int getCriticalHit()
	{
		return getStat().getCriticalHit();
	}
	
	@Override
	public int getMAtk()
	{
		return getStat().getMAtk();
	}
	
	@Override
	public int getMDef()
	{
		return getStat().getMDef();
	}
	
	@Override
	public final int getSkillLevel(int skillId)
	{
		if (getKnownSkill(skillId) == null)
		{
			return -1;
		}
		
		final int lvl = getLevel();
		return lvl > 70 ? 7 + ((lvl - 70) / 5) : lvl / 10;
	}
	
	public void updateRefOwner(PlayerInstance owner)
	{
		int oldOwnerId = getOwner().getObjectId();
		
		setOwner(owner);
		World.getInstance().removePet(oldOwnerId);
		World.getInstance().addPet(oldOwnerId, this);
	}
	
	public int getInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_PET;
	}
	
	public void refreshOverloaded()
	{
		int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			long weightproc = (((getCurrentLoad() - getBonusWeightPenalty()) * 1000) / maxLoad);
			int newWeightPenalty;
			if ((weightproc < 500) || getOwner().getDietMode())
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0)
				{
					addSkill(SkillData.getInstance().getSkill(4270, newWeightPenalty));
					setIsOverloaded(getCurrentLoad() >= maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), true);
					setIsOverloaded(false);
				}
			}
		}
	}
	
	@Override
	public void updateAndBroadcastStatus(int val)
	{
		refreshOverloaded();
		super.updateAndBroadcastStatus(val);
	}
	
	@Override
	public final boolean isHungry()
	{
		return getCurrentFed() < ((getPetData().getHungryLimit() / 100f) * getPetLevelData().getPetMaxFeed());
	}
	
	/**
	 * Verifies if a pet can be controlled by it's owner.<br>
	 * Starving pets cannot be controlled.
	 * @return {@code true} if the per cannot be controlled
	 */
	public boolean isUncontrollable()
	{
		return getCurrentFed() <= 0;
	}
	
	@Override
	public final int getWeapon()
	{
		ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}
	
	@Override
	public final int getArmor()
	{
		ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}
	
	public final int getJewel()
	{
		ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}
	
	@Override
	public short getSoulShotsPerHit()
	{
		return getPetLevelData().getPetSoulShot();
	}
	
	@Override
	public short getSpiritShotsPerHit()
	{
		return getPetLevelData().getPetSpiritShot();
	}
	
	@Override
	public void setName(String name)
	{
		final ItemInstance controlItem = getControlItem();
		if (controlItem != null)
		{
			if (controlItem.getCustomType2() == (name == null ? 1 : 0))
			{
				// name not set yet
				controlItem.setCustomType2(name != null ? 1 : 0);
				controlItem.updateDatabase();
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(controlItem);
				sendInventoryUpdate(iu);
			}
		}
		else
		{
			_log.warn("Pet control item null, for pet: " + toString());
		}
		super.setName(name);
	}
	
	public boolean canEatFoodId(int itemId)
	{
		return _data.getFood().contains(itemId);
	}
	
	@Override
	public boolean isPet()
	{
		return true;
	}
	
	@Override
	public final double getRunSpeed()
	{
		return super.getRunSpeed() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	@Override
	public final double getWalkSpeed()
	{
		return super.getWalkSpeed() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	@Override
	public final double getMovementSpeedMultiplier()
	{
		return super.getMovementSpeedMultiplier() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	@Override
	public final double getMoveSpeed()
	{
		if (isInsideZone(ZoneId.WATER))
		{
			return isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		return isRunning() ? getRunSpeed() : getWalkSpeed();
	}
}
