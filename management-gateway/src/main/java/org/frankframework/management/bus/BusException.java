/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.management.bus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.IbisException;

/**
 * Serialized and send as an ExceptionMessage over the Spring Bus
 */
public class BusException extends RuntimeException {
	private static final Logger LOG = LogManager.getLogger(BusException.class);

	private static final long serialVersionUID = 1L;

	/**
	 * Seen as WARNING
	 */
	public BusException(String message) {
		this(message, null);
	}

	/**
	 * Seen as ERROR
	 * Stacktrace information is logged but not passed to the parent to limit sensitive information being sent over the 'bus'.
	 */
	public BusException(String message, Throwable exception) {
		super(new IbisException(message, exception).getMessage());
		if(exception == null) {
			LOG.warn(super.getMessage()); // expanded message is logged directly
		} else {
			LOG.error(message, exception); // normal message, expanded by printing the stacktrace
		}
	}
}
