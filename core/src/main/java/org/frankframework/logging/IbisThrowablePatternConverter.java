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
package org.frankframework.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.util.Strings;

@Plugin(name = "IbisThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "iEx", "iThrowable" })
public final class IbisThrowablePatternConverter extends ThrowablePatternConverter {

	private static final String CAUSED_BY = "Caused by: ";

	private IbisThrowablePatternConverter(final Configuration config, final String[] options) {
		super("IbisThrowablePatternConverter", "throwable", options, config);
	}

	public static IbisThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
		return new IbisThrowablePatternConverter(config, options);
	}

	@Override
	public void format(final LogEvent event, final StringBuilder buffer) {
		final Throwable throwable = event.getThrown();
		if (throwable != null) {
			throwablePrinter(throwable, buffer, 0);
		}
	}

	/** Recursively prints the trace */
	private void throwablePrinter(final Throwable throwable, final StringBuilder buffer, final int commonElementCount) {
		final StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));

		final ThrowableProxy proxy = new ThrowableProxy(throwable);

		final ExtendedStackTraceElement[] elements = proxy.getExtendedStackTrace();
		final String[] linesToBePrinted = writer.toString().split(Strings.LINE_SEPARATOR);

		for (int i = 0; i <= elements.length-commonElementCount; ++i) {
			buffer.append(linesToBePrinted[i]);
			if (i != 0 && i <= elements.length) {
				buffer.append(" ");
				buffer.append(elements[i-1].getExtraClassInfo().toString());
			}
			buffer.append(Strings.LINE_SEPARATOR);
		}
		if (commonElementCount != 0) {
			buffer.append("\t ... " + (commonElementCount) + " more" + Strings.LINE_SEPARATOR);
		}
		if (throwable.getCause() != null) {
			buffer.append(CAUSED_BY);
			throwablePrinter(throwable.getCause(), buffer, proxy.getCauseProxy().getCommonElementCount());
		}
	}

}
