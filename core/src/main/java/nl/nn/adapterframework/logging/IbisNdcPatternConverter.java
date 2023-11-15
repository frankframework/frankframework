/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.logging;

import java.util.Map.Entry;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Plugin(name = "IbisNdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"TC"})
public class IbisNdcPatternConverter extends LogEventPatternConverter {

	private IbisNdcPatternConverter() {
		super("IbisNdcPatternConverter", "ndc");
	}

	public static IbisNdcPatternConverter newInstance() {
		return new IbisNdcPatternConverter();
	}

	@Override
	@PerformanceSensitive("allocation")
	public void format(final LogEvent event, final StringBuilder stringBuilder) {
		if(!event.getContextData().isEmpty()) {
			for(Entry<String, String> entry : event.getContextData().toMap().entrySet()) {
				String key = convertKey(entry.getKey());
				stringBuilder.append(key);
				stringBuilder.append(" [");
				stringBuilder.append(entry.getValue());
				stringBuilder.append("] ");
			}

			// Remove the last space
			int lenght = stringBuilder.length();
			if(stringBuilder.charAt(lenght-1) == ' ') {
				stringBuilder.deleteCharAt(lenght - 1);
			}
		}
	}

	private String convertKey(String key) {
		return key.replace('.', '-');
	}
}