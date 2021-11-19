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
package nl.nn.adapterframework.jdbc.migration;

import liquibase.logging.core.AbstractLogger;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.logging.log4j.Logger;

/**
 * @author alisihab
 *
 */
public class LiquibaseLogger extends AbstractLogger {
	private static Logger log;

	public LiquibaseLogger(String name) {
		log = LogUtil.getLogger(name);
	}

	@Override
	public void setName(String name) {
		// logger will be initialized with the name no need to set it here
	}

	@Override
	public void severe(String message) {
		log.error(message);
	}

	@Override
	public void severe(String message, Throwable e) {
		log.error(message, e);
	}

	@Override
	public void warning(String message) {
		log.warn(message);
	}

	@Override
	public void warning(String message, Throwable e) {
		log.warn(message, e);
	}

	@Override
	public void info(String message) {
		log.info(message);
	}

	@Override
	public void info(String message, Throwable e) {
		log.info(message, e);
	}

	@Override
	public void debug(String message) {
		if(log.isTraceEnabled()) {// prints too much unnecessary information. Only print if trace enabled
			log.debug(message);
		}
	}

	@Override
	public void debug(String message, Throwable e) {
		log.debug(message, e);
	}

	@Override
	public int getPriority() {
		return 1;
	}

	@Override
	public void setLogLevel(String logLevel, String logFile) {
		// log level is managed by log4j see log4j4ibis.xml
	}
}