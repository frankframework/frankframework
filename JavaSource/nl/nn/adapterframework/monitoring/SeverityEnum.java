/*
 * $Log: SeverityEnum.java,v $
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
 * Enumeration of Severities for monitoring.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
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
