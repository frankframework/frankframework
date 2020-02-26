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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * Extension of StringMatchFilter with the facility of executing a regular
 * expression on the name of the current thread.
 * 
 * @author Peter Leeuwenburgh
 * @author Murat Kaan Meral
 */
@Plugin(name = "IbisThreadFilter", category = "Core", elementType = "filter", printObject = true)
public class IbisThreadFilter extends AbstractFilter {
	protected String regex;
	protected Level level;

	private IbisThreadFilter(Level level, String regex, Result onMatch, Result onMismatch) {
		super(onMatch, onMismatch);
		this.level = level;
		this.regex = regex;
	}
	
	@Override
	public Filter.Result filter(LogEvent event) {
		if (level == null || event.getLevel().isLessSpecificThan(level))
			return getOnMismatch();

		String tn = event.getThreadName();

		if (tn == null || regex == null)
			return getOnMismatch();

		if (tn.matches(regex)) {
			return getOnMatch();
		} else {
			return getOnMismatch();
		}
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public void setlevel(Level level) {
		this.level = level;
	}

	@PluginFactory
	public static IbisThreadFilter createFilter(@PluginAttribute(value = "regex") String regex,
												@PluginAttribute(value = "level", defaultString = "WARN") Level level,
												@PluginAttribute(value = "onMatch", defaultString = "NEUTRAL") Result onMatch,
												@PluginAttribute(value = "onMismatch", defaultString = "DENY") Result onMismatch) {
		return new IbisThreadFilter(level, regex, onMatch, onMismatch);
	}
}