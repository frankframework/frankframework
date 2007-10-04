/*
 * $Log: EventTypeEnum.java,v $
 * Revision 1.1.2.1  2007-10-04 13:25:57  europe\L190409
 * synchronize with HEAD (4.7.0)
 *
 * Revision 1.1  2007/09/27 12:55:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.enum.Enum;
/**
 * Enumeration of Event Types for monitoring.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
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
