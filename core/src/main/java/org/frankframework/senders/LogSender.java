/*
   Copyright 2013 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package org.frankframework.senders;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

/**
 * Sender that just logs its message.
 *
 * @author Gerrit van Brakel
 * @since  4.9
 */
@Category(Category.Type.ADVANCED)
public class LogSender extends AbstractSenderWithParameters {
	private @Getter String logLevel = "info";
	private Level defaultLogLevel = Level.DEBUG;
	private String logCategory = null;
	private static final String LOG_LEVEL_ATTRIBUTE_NAME = "logLevel";

	protected Logger logger;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		logger = LogUtil.getLogger(getLogCategory());
		checkStringAttributeOrParameter(LOG_LEVEL_ATTRIBUTE_NAME, logLevel, LOG_LEVEL_ATTRIBUTE_NAME);

		if(StringUtils.isNotEmpty(logLevel)) {
			defaultLogLevel = Level.valueOf(logLevel);
		}
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl = getParameterValueList(message, session);
		String calculatedLevel = getParameterOverriddenAttributeValue(pvl, LOG_LEVEL_ATTRIBUTE_NAME, logLevel);
		Level level = Level.toLevel(calculatedLevel, defaultLogLevel);
		try {
			logger.log(level, message.asString());
		} catch (IOException io) {
			log.warn("unable to log message: {}", message);
		}
		if (getParameterList() != null && pvl != null) {
			for (ParameterValue param : pvl) {
				if(!LOG_LEVEL_ATTRIBUTE_NAME.equals(param.getName())) {
					logger.log(level, "parameter [{}] value [{}]", param.getName(), param.getValue());
				}
			}
		}
		return new SenderResult(message);
	}

	public String getLogCategory() {
		if (StringUtils.isNotEmpty(logCategory)) {
			return logCategory;
		}
		if (StringUtils.isNotEmpty(getName())) {
			return getName();
		}
		return this.getClass().getName();
	}

	/**
	 * category under which messages are logged
	 * @ff.default name of the sender
	 */
	public void setLogCategory(String string) {
		logCategory = string;
	}

	/**
	 * level on which messages are logged
	 * @ff.default info
	 */
	public void setLogLevel(String level) {
		logLevel = level;
	}

	@Override
	public String toString() {
		String level = getParameterList() != null && getParameterList().hasParameter(LOG_LEVEL_ATTRIBUTE_NAME) ? "dynamic" : logLevel;
		return "LogSender ["+getName()+"] logLevel ["+level+"] logCategory ["+logCategory+"]";
	}

}
