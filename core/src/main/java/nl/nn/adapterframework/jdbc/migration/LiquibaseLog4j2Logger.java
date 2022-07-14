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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;

import liquibase.logging.core.AbstractLogger;

/**
 * It is important that we're wrapping the Log4j2 Logger and are directly calling logIfEnabled.
 * By calling a log method (<code>logger.warn(String)</code>) directly, it will affect the stack and thus the package.classname log-prefix.
 * 
 * @author Niels Meijer
 */
public class LiquibaseLog4j2Logger extends AbstractLogger {
	private static final String FQCN = LiquibaseLog4j2Logger.class.getName();
	private ExtendedLogger logger;

	public LiquibaseLog4j2Logger(Class<?> clazz) {
		super();

		logger = (ExtendedLogger) LogManager.getLogger(clazz);
	}

	@Override
	public void log(java.util.logging.Level level, String message, Throwable e) {
		logger.logIfEnabled(FQCN, Level.TRACE, null, message, e);
	}

	@Override
	public void severe(String message) {
		logger.logIfEnabled(FQCN, Level.ERROR, null, message);
	}

	@Override
	public void severe(String message, Throwable e) {
		logger.logIfEnabled(FQCN, Level.ERROR, null, message, e);
	}

	@Override
	public void warning(String message) {
		logger.logIfEnabled(FQCN, Level.WARN, null, message);
	}

	@Override
	public void warning(String message, Throwable e) {
		logger.logIfEnabled(FQCN, Level.WARN, null, message, e);
	}

	@Override
	public void info(String message) {
		logger.logIfEnabled(FQCN, Level.INFO, null, message);
	}

	@Override
	public void info(String message, Throwable e) {
		logger.logIfEnabled(FQCN, Level.INFO, null, message, e);
	}

	@Override
	public void config(String message) {
		logger.logIfEnabled(FQCN, Level.INFO, null, message);
	}

	@Override
	public void config(String message, Throwable e) {
		logger.logIfEnabled(FQCN, Level.INFO, null, message, e);
	}

	@Override
	public void debug(String message) {
		logger.logIfEnabled(FQCN, Level.DEBUG, null, message);
	}

	@Override
	public void debug(String message, Throwable e) {
		logger.logIfEnabled(FQCN, Level.DEBUG, null, message, e);
	}

	@Override
	public void fine(String message) {
		logger.logIfEnabled(FQCN, Level.DEBUG, null, message);
	}

	@Override
	public void fine(String message, Throwable e) {
		logger.logIfEnabled(FQCN, Level.DEBUG, null, message, e);
	}
}
