/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.monitoring;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.enums.Enum;
/**
 * Enumeration of Event Types for monitoring.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version $Id$
 */
public class EventTypeEnum extends Enum {

	public static final EventTypeEnum TECHNICAL  = new EventTypeEnum("TECHNICAL");
	public static final EventTypeEnum FUNCTIONAL = new EventTypeEnum("FUNCTIONAL");
	public static final EventTypeEnum HEARTBEAT  = new EventTypeEnum("HEARTBEAT");
	public static final EventTypeEnum CLEARING   = new EventTypeEnum("CLEARING");

	/**
	 * RunStateEnum constructor 
	 * @param arg1 Value of new enumeration item
	 */
	protected EventTypeEnum(String stateDescriptor) {
		super(stateDescriptor);
	}
	public static EventTypeEnum getEnum(String stateDescriptor) {
		return (EventTypeEnum)getEnum(EventTypeEnum.class, stateDescriptor);
	}
	public static List getEnumList() {
		return getEnumList(EventTypeEnum.class);
	}
	public static Map getEnumMap() {
		return getEnumMap(EventTypeEnum.class);
	}
	public static String getNames() {
		String result = "[";
		for (Iterator i = iterator(EventTypeEnum.class); i.hasNext();) {
			EventTypeEnum c = (EventTypeEnum)i.next();
			result += c.getName();
			if (i.hasNext())
				result += ",";
		}
		result += "]";
		return result;

	}
	public boolean isState(String state) {
		return this.equals(getEnum(state.trim()));
	}
	public static Iterator iterator() {
		return iterator(EventTypeEnum.class);
	}
	public String toString() {
		return getName().trim();
	}
}
