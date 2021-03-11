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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.Vector;


/**
 * Base class for the configuration warnings.
 * 
 * @author Peter Leeuwenburgh
 */
public class BaseConfigurationWarnings extends LinkedList<String> {
	protected Vector<String> defaultValueExceptions = new Vector<String>();

	protected boolean add(Logger log, String msg, Throwable t, String messageSuffixForLog, boolean onlyOnce) {
		String logMsg = StringUtils.isNotEmpty(messageSuffixForLog) ? msg + messageSuffixForLog : msg;
		if (t == null) {
			log.warn(logMsg);
		} else {
			log.warn(logMsg, t);
		}
		if (!onlyOnce || !super.contains(msg)) {
			return super.add(msg);
		} else {
			return false;
		}
	}

	/**
	 * These are the exceptions thrown when a setter is invoked but the default value has not been changed.
	 */
	public boolean containsDefaultValueException(String key) {
		return defaultValueExceptions.contains(key);
	}

	public void addDefaultValueException(String key) {
		if (!containsDefaultValueException(key)) {
			defaultValueExceptions.add(key);
		}
	}
}