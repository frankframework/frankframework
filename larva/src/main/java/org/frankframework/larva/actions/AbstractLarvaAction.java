/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.larva.actions;

import java.util.Map;
import java.util.Properties;

import org.springframework.context.Lifecycle;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.jdbc.AbstractJdbcQuerySender.QueryType;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.parameters.IParameter;
import org.frankframework.util.EnumUtils;

/**
 * This class is used to create and manage the lifecycle of Larva actions.
 * 
 * This class is a wrapper around the IConfigurable interface and handles the read and write operations.
 * 
 * @author Niels Meijer
 */
@Log4j2
public abstract class AbstractLarvaAction<T extends IConfigurable> implements Lifecycle, IConfigurable, LarvaScenarioAction {

	private static final String CONVERT_MESSAGE_TO_EXCEPTION_PROPERTY_KEY = "convertExceptionToMessage";
	private final T configurable;

	private @Getter boolean convertExceptionToMessage = false;
	private @Getter PipeLineSession session = new PipeLineSession();

	protected AbstractLarvaAction(T configurable) {
		this.configurable = configurable;
	}

	protected final T peek() {
		return configurable;
	}

	@Override
	public void configure() throws ConfigurationException {
		log.trace("configuring [{}]", configurable);
		configurable.configure();
	}

	@Override
	public void start() {
		if (configurable instanceof Lifecycle lifecycle) {
			log.trace("starting [{}]", lifecycle);
			lifecycle.start();
		}
	}

	@Override
	public void stop() {
		if (configurable instanceof Lifecycle lifecycle) {
			log.trace("stopping [{}]", lifecycle);
			lifecycle.stop();
		}
	}

	@Override
	public void close() throws Exception {
		log.trace("closing [{}]", configurable);
		stop();
		session.close();
		session = null;
		if (configurable instanceof AutoCloseable autoCloseable) {
			autoCloseable.close();
		}
	}

	@Override
	public boolean isRunning() {
		return session != null;
	}

	public void invokeSetters(int defaultTimeout, Properties properties) {
		log.trace("invoking setters on [{}]", configurable);
		LarvaActionUtils.invokeSetters(configurable, properties);

		String convertException = properties.getProperty(CONVERT_MESSAGE_TO_EXCEPTION_PROPERTY_KEY);
		convertExceptionToMessage = Boolean.parseBoolean(convertException);

		mapParameters(properties);

		// Keeps properties backwards compatible
		if (configurable instanceof FixedQuerySender jdbcSender) {
			String queryType = properties.getProperty("queryType", "select");
			jdbcSender.setQueryType(EnumUtils.parse(QueryType.class, queryType));

			String readQuery = properties.getProperty("readQuery");
			jdbcSender.setQuery(readQuery);
		}
	}

	private void mapParameters(Properties properties) {
		if(configurable instanceof IWithParameters withParameters) {
			Map<String, IParameter> paramPropertiesMap = LarvaActionUtils.createParametersMapFromParamProperties(properties, getSession());
			log.trace("found parameters [{}] on [{}]", paramPropertiesMap::keySet, () -> configurable);
			paramPropertiesMap.values().forEach(withParameters::addParameter);
		}
	}
}
