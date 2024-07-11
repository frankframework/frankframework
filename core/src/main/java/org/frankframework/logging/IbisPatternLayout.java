/*
   Copyright 2020, 2023-2024 WeAreFrank!

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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.LocationAware;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;

/**
 * Implementation of {@link IbisMaskingLayout} to serialize given log events
 * according to the given pattern.
 */
@Plugin(name = "IbisPatternLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class IbisPatternLayout extends IbisMaskingLayout {

	private static final String DEFAULT_PATTERN = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%t] %X %c{2} - %m%n";
	private final Serializer serializer;

	/**
	 * @param pattern the pattern to use or DEFAULT when null
	 * @param alwaysWriteExceptions defaults to true
	 * @param disableAnsi defaults to false
	 * @param noConsoleNoAnsi defaults to false
	 */
	IbisPatternLayout(final Configuration config, final String pattern, final Charset charset, final boolean alwaysWriteExceptions, final boolean disableAnsi, final boolean noConsoleNoAnsi) {
		super(config, charset);

		try {
			final PatternParser parser = PatternLayout.createPatternParser(configuration);
			final List<PatternFormatter> list = parser.parse(pattern, alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
			final PatternFormatter[] formatters = list.toArray(new PatternFormatter[0]);
			serializer = new PatternSerializer(formatters);
		} catch (final RuntimeException ex) {
			throw new IllegalArgumentException("Cannot parse pattern '" + pattern + "'", ex);
		}
	}

	@Override
	protected String serializeEvent(LogEvent event) {
		return serializer.toSerializable(event);
	}

	@PluginFactory
	public static IbisPatternLayout createLayout(
			@PluginAttribute(value = "pattern", defaultString = IbisPatternLayout.DEFAULT_PATTERN) final String pattern,
			@PluginConfiguration final Configuration config,
			// LOG4J2-783 use platform default by default, so do not specify defaultString for charset
			@PluginAttribute(value = "charset") final Charset charset,
			@PluginAttribute(value = "alwaysWriteExceptions", defaultBoolean = true) final boolean alwaysWriteExceptions,
			@PluginAttribute(value = "noConsoleNoAnsi") final boolean noConsoleNoAnsi,
			@PluginAttribute(value = "disableAnsi") final boolean disableAnsi) {
		return new IbisPatternLayout(config, pattern, charset, alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
	}

	private static class PatternSerializer implements Serializer, Serializer2, LocationAware {

		private final PatternFormatter[] formatters;

		protected PatternSerializer(final PatternFormatter[] formatters) {
			super();
			this.formatters = formatters;
		}

		@Override
		public String toSerializable(final LogEvent event) {
			final StringBuilder sb = getStringBuilder();
			try {
				return toSerializable(event, sb).toString();
			} finally {
				trimToMaxSize(sb);
			}
		}

		@Override
		public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
			Arrays.stream(formatters).forEach(formatter -> formatter.format(event, buffer));
			return buffer;
		}

		@Override
		public boolean requiresLocation() {
			return Arrays.stream(formatters).anyMatch(PatternFormatter::requiresLocation);
		}

		@Override
		public String toString() {
			return super.toString() + "[formatters=" + Arrays.toString(formatters) + "]";
		}
	}
}
