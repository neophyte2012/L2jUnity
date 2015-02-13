/*
 * Copyright (C) 2004-2014 L2J Server
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
package org.l2junity.network;

import io.netty.buffer.ByteBuf;

/**
 * @author Nos
 */
public final class PacketWriter
{
	private final ByteBuf _buf;
	
	public PacketWriter(ByteBuf buf)
	{
		_buf = buf;
	}
	
	/**
	 * Writes a byte
	 * @param value the byte (The 24 high-order bits are ignored)
	 */
	public void writeC(int value)
	{
		_buf.writeByte(value);
	}
	
	/**
	 * Writes a short
	 * @param value the short (The 16 high-order bits are ignored)
	 */
	public void writeH(int value)
	{
		_buf.writeShort(value);
	}
	
	/**
	 * Writes an integer
	 * @param value the integer
	 */
	public void writeD(int value)
	{
		_buf.writeInt(value);
	}
	
	/**
	 * Writes a long
	 * @param value the long
	 */
	public void writeQ(long value)
	{
		_buf.writeLong(value);
	}
	
	/**
	 * Writes a float
	 * @param value the float
	 */
	public void writeE(float value)
	{
		_buf.writeFloat(value);
	}
	
	/**
	 * Writes a double
	 * @param value the double
	 */
	public void writeF(double value)
	{
		_buf.writeDouble(value);
	}
	
	/**
	 * Writes a string
	 * @param value the string
	 */
	public void writeS(String value)
	{
		if (value != null)
		{
			for (char ch : value.toCharArray())
			{
				_buf.writeChar(ch);
			}
		}
		
		_buf.writeChar(0);
	}
	
	/**
	 * Writes a string with fixed length specified as first short, then array of chars
	 * @param value
	 */
	public void writeString(String value)
	{
		if (value != null)
		{
			_buf.writeShort(value.toCharArray().length);
			for (char ch : value.toCharArray())
			{
				_buf.writeChar(ch);
			}
		}
		else
		{
			_buf.writeShort(0);
		}
	}
	
	/**
	 * @param bytez
	 */
	public void writeB(byte[] bytez)
	{
		_buf.writeBytes(bytez);
	}
}
