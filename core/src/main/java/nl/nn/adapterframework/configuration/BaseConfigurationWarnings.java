/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
 * Base class for the configuration warnings.
 * 
 * @author Peter Leeuwenburgh
 */
public class BaseConfigurationWarnings extends LinkedList {
	protected Vector defaultValueExceptions = new Vector();

	public boolean add(Logger log, String msg) {
		return add(log, msg, null, false);
	}

	public boolean add(Logger log, String msg, Throwable t) {
		return add(log, msg, t, false);
	}

	public boolean add(Logger log, String msg, boolean onlyOnce) {
		return add(log, msg, null, onlyOnce);
	}

	public boolean add(Logger log, String msg, Throwable t, boolean onlyOnce) {
		if (t == null) {
			log.warn(msg);
		} else {
			log.warn(msg, t);
		}
		if (!onlyOnce || !super.contains(msg)) {
			return super.add(msg);
		} else {
			return false;
		}
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