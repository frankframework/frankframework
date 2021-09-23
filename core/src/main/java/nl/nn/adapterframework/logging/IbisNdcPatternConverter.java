/*
   Copyright 2021 WeAreFrank!

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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Plugin(name = "IbisNdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"TC"})
public class IbisNdcPatternConverter extends LogEventPatternConverter {
	/**
	 * Singleton.
	 */
	private static final IbisNdcPatternConverter INSTANCE = new IbisNdcPatternConverter();

	/**
	 * Private constructor.
	 */
	private IbisNdcPatternConverter() {
		super("xx", "ndc");
	}

	/**
	 * Obtains an instance of NdcPatternConverter.
	 *
	 * @param options options, may be null.
	 * @return instance of NdcPatternConverter.
	 */
	public static IbisNdcPatternConverter newInstance(final String[] options) {
		return INSTANCE;
	}

	@Override
	@PerformanceSensitive("allocation")
	public void format(final LogEvent event, final StringBuilder toAppendTo) {
		if(event.getContextStack().isEmpty())
			toAppendTo.append("null");
		else
			toAppendTo.append(event.getContextStack());
	}
}
