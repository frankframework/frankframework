/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;


/**
 * Base class for the configuration warnings.
 * 
 * @author Peter Leeuwenburgh
 */
public class BaseConfigurationWarnings extends LinkedList<String> {

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
}