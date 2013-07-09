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
package nl.nn.adapterframework.extensions.log4j;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.varia.StringMatchFilter;

/**
 * Extension of StringMatchFilter with the facility of executing a regular
 * expression on the name of the current thread.
 * 
 * @author Peter Leeuwenburgh
 */

public class IbisThreadFilter extends StringMatchFilter {
	protected String regex;
	protected Level levelMin=Level.WARN;
	
	public int decide(LoggingEvent event) {
		if (levelMin == null || event.getLevel().isGreaterOrEqual(levelMin))
			return Filter.NEUTRAL;

		String tn = event.getThreadName();

		if (tn == null || regex == null)
			return Filter.NEUTRAL;

		if (tn.matches(regex)) {
			if (getAcceptOnMatch()) {
				return Filter.ACCEPT;
			} else {
				return Filter.DENY;
			}
		} else {
			return Filter.NEUTRAL;
		}
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public void setLevelMin(Level levelMin) {
		this.levelMin = levelMin;
	}
}