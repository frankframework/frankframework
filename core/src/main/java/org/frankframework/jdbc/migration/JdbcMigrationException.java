/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.jdbc.migration;

import liquibase.exception.LiquibaseException;

import org.frankframework.core.IbisException;

/**
 * Wrapper for JDBC migration related exceptions.
 *
 */
public class JdbcMigrationException extends IbisException {
	private final String originalMessage;

	public JdbcMigrationException() {
		super();
		originalMessage = null;
	}

	public JdbcMigrationException(String message) {
		super(message);
		originalMessage = message;
	}

	public JdbcMigrationException(String message, Throwable cause) {
		super(message, cause);
		originalMessage = message;
	}

	public JdbcMigrationException(Throwable cause) {
		super(cause);
		originalMessage = null;
	}

	@Override
	public String getMessage() {
		if (originalMessage == null) {
			return super.getMessage();
		}

		return IbisException.expandMessage(originalMessage, this, e -> e instanceof IbisException || e instanceof LiquibaseException);
	}
}
