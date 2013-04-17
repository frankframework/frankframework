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
package nl.nn.adapterframework.configuration;

import java.util.LinkedList;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * Singleton class that has the configuration warnings for this application.
 * 
 * @version $Id$
 * @author Peter Leeuwenburgh
 */
public final class ConfigurationWarnings extends LinkedList {
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