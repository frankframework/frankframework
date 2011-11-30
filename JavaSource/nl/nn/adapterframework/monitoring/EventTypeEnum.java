/*
 * $Log: EventTypeEnum.java,v $
 * Revision 1.4  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2011/07/06 06:54:19  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * replaced <commons-lang-2.0.jar> by <commons-lang-2.6.jar>
 *
 * Revision 1.1  2007/09/27 12:55:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of monitoring
 *
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
