/*****************************************************************
H2H4h

Copyright (C) 2011-2013 The National Center for Telehealth and 
Technology

Eclipse Public License 1.0 (EPL-1.0)

This library is free software; you can redistribute it and/or
modify it under the terms of the Eclipse Public License as
published by the Free Software Foundation, version 1.0 of the 
License.

The Eclipse Public License is a reciprocal license, under 
Section 3. REQUIREMENTS iv) states that source code for the 
Program is available from such Contributor, and informs licensees 
how to obtain it in a reasonable manner on or through a medium 
customarily used for software exchange.

Post your updates and modifications to our GitHub or email to 
t2@tee2.org.

This library is distributed WITHOUT ANY WARRANTY; without 
the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the Eclipse Public License 1.0 (EPL-1.0)
for more details.
 
You should have received a copy of the Eclipse Public License
along with this library; if not, 
visit http://www.opensource.org/licenses/EPL-1.0

*****************************************************************/
package com.t2.h2h4h;

import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Overall H4H class access wrapper
 * @author scott.coleman
 *
 */
public class H2H4h {
	private DataOutHandler sDataOutHandler;
	
	public H2H4h() throws DataOutHandlerException {
		sDataOutHandler = DataOutHandler.getInstance();	
	}
	
	/**
	 * Returns all habits in the database as Habit classes
	 * @return List of Habits
	 * 
	 * @throws DataOutHandlerException
	 */
	public List<DataOutPacket> getHabits() throws DataOutHandlerException {

		List<DataOutPacket> habits = new ArrayList<DataOutPacket>();
		
		ArrayList<DataOutPacket> habitsDOP = sDataOutHandler.getPacketList("StructureType in ('" + DataOutHandlerTags.STRUCTURE_TYPE_HABIT + "')");		

		if (habitsDOP != null) {
			for (DataOutPacket packetDOP : habitsDOP) {
				Habit habit = new Habit(packetDOP);
				habits.add(habit);
			}
		}
		
		return habits;
	}
	
	/**
	 * Returns list of all Checkins in the database as Checkin Classes
	 * 
	 * @return List of Checkins
	 * @throws DataOutHandlerException
	 */
	public List<DataOutPacket> getCheckins() throws DataOutHandlerException {

		List<DataOutPacket> checkins = new ArrayList<DataOutPacket>();
		
		ArrayList<DataOutPacket> habitsDOP = sDataOutHandler.getPacketList("StructureType in ('" + DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN + "')");		

		if (habitsDOP != null) {
			for (DataOutPacket packetDOP : habitsDOP) {
				Checkin checkin = new Checkin(packetDOP);
				checkins.add(checkin);
			}
		}
		
		return checkins;
	}
	
	/**
	 * Returns list of all class types in the database 
	 * 
	 * @return List of class objects
	 * @throws DataOutHandlerException
	 */
	public List<DataOutPacket> getAllClasses() throws DataOutHandlerException {

		List<DataOutPacket> checkins = new ArrayList<DataOutPacket>();
		
		ArrayList<DataOutPacket> habitsDOP = sDataOutHandler.getPacketList(
				"StructureType in ("
						+ "'" + DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN + "'"
						+ "'" + DataOutHandlerTags.STRUCTURE_TYPE_HABIT + "'"
								+ ")");		

		if (habitsDOP != null) {
	
			for (DataOutPacket packetDOP : habitsDOP) {
				Checkin checkin = new Checkin(packetDOP);
				checkins.add(checkin);
			}
		}		
		return checkins;
	}
}
