/*
 * $Log: ConfigurationWarnings.java,v $
 * Revision 1.2  2009-11-24 08:32:00  m168309
 * excluded ${property.key} values from default value check
 *
 * Revision 1.1  2008/12/30 17:01:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 */
package nl.nn.adapterframework.configuration;

import java.util.LinkedList;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * Singleton class that has the configuration warnings for this application.
 * 
 * @version Id
 * @author Peter Leeuwenburgh
 */
public final class ConfigurationWarnings extends LinkedList {
	public static final String version = "$Id: ConfigurationWarnings.java,v 1.2 2009-11-24 08:32:00 m168309 Exp $";

	private static ConfigurationWarnings self = null;
	private Vector defaultValueExceptions = new Vector();

	public static synchronized ConfigurationWarnings getInstance() {
		if (self == null) {
			self = new ConfigurationWarnings();
		}
		return self;
	}

	public boolean add(Logger log, String msg) {
		log.warn(msg);
		return super.add(msg);
	}

	public boolean containsDefaultValueExceptions(String key) {
		return defaultValueExceptions.contains(key);
	}

	public boolean addDefaultValueExceptions(String key) {
		if (containsDefaultValueExceptions(key)) {
			return true;
		} else {
			return defaultValueExceptions.add(key);
		}
	}
}