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

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.logging.core.AbstractLogger;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.logging.log4j.Logger;

/**
 * @author alisihab
 *
 */
public class LiquibaseLogger extends AbstractLogger {
	private static final Logger log = LogUtil.getLogger(LiquibaseLogger.class);

	@Override
	public void setName(String name) {
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
		if(log.isWarnEnabled()) {
			log.warn(message);
		}
	}

	@Override
	public void warning(String message, Throwable e) {
		if(log.isWarnEnabled()) {
			log.warn(message, e);
		}
	}

	@Override
	public void info(String message) {
		if(log.isInfoEnabled()) {
			log.info(message);
		}
	}

	@Override
	public void info(String message, Throwable e) {
		if(log.isInfoEnabled()) {
			log.info(message, e);
		}
	}

	@Override
	public void debug(String message) {
		if(log.isTraceEnabled()) {// prints too much unnecessary only print if trace enabled
			log.debug(message);
		}
	}

	@Override
	public void debug(String message, Throwable e) {
		if(log.isDebugEnabled()) {
			log.debug(message, e);
		}
	}

	@Override
	public void setLogLevel(String logLevel, String logFile) {
	}

	@Override
	public void setChangeLog(DatabaseChangeLog databaseChangeLog) {
	}

	@Override
	public void setChangeSet(ChangeSet changeSet) {
	}

	@Override
	public int getPriority() {
		return 1;
	}
}