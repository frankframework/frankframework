/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.lifecycle;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import lombok.extern.log4j.Log4j2;

/**
 * Should be called directly after the Environment has been initialized.
 * Finds properties that start with `logging.level.` just like Spring Boot does.
 */
@Log4j2
public class LogPropertiesConfigurer implements PriorityOrdered, EnvironmentAware {

	private Environment environment;
	private LoggerContext context = LoggerContext.getContext(false);
	private static final String LOGGER_PROPERTY_PREFIX = "logging.level.";

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void setEnvironment(Environment environment) {
		if (environment instanceof AbstractEnvironment abstractEnv) {
			this.environment = environment;

			abstractEnv.getPropertySources().stream()
				.filter(MapPropertySource.class::isInstance)
				.map(MapPropertySource.class::cast)
				.flatMap(LogPropertiesConfigurer::mapPropertyNamesToStream)
				.filter(LogPropertiesConfigurer::isLoggingProperty)
				.distinct()
				.forEach(this::configureLogger);
		}
	}

	private static Stream<String> mapPropertyNamesToStream(MapPropertySource propertySource) {
		return Stream.of(propertySource.getPropertyNames());
	}

	private static boolean isLoggingProperty(String name) {
		return name.startsWith(LOGGER_PROPERTY_PREFIX);
	}

	private static String getLoggerName(String name) {
		return name.substring(LOGGER_PROPERTY_PREFIX.length());
	}

	private void configureLogger(String propertyName) {
		String value = environment.getProperty(propertyName);
		if(StringUtils.isNotBlank(value)) {
			String loggerName = getLoggerName(propertyName);
			Level logLevel = Level.getLevel(value);
			log.debug("setting custom logger [{}] to level [{}]", loggerName, logLevel);
			context.getLogger(loggerName).setLevel(logLevel);
		}
	}
}
