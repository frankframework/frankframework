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
 * Enumeration of Severities for monitoring.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version $Id$
 */
public class SeverityEnum extends Enum {

	public static final SeverityEnum HARMLESS = new SeverityEnum("HARMLESS");
	public static final SeverityEnum WARNING  = new SeverityEnum("WARNING");
	public static final SeverityEnum CRITICAL = new SeverityEnum("CRITICAL");
	public static final SeverityEnum FATAL    = new SeverityEnum("FATAL");

	protected SeverityEnum(String stateDescriptor) {
		super(stateDescriptor);
	}
	public static SeverityEnum getEnum(String stateDescriptor) {
		return (SeverityEnum)getEnum(SeverityEnum.class, stateDescriptor);
	}
	public static List getEnumList() {
		return getEnumList(SeverityEnum.class);
	}
	public static Map getEnumMap() {
		return getEnumMap(SeverityEnum.class);
	}
	public static String getNames() {
		String result = "[";
		for (Iterator i = iterator(SeverityEnum.class); i.hasNext();) {
			SeverityEnum c = (SeverityEnum)i.next();
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
		return iterator(SeverityEnum.class);
	}
	public String toString() {
		return getName().trim();
	}
}
