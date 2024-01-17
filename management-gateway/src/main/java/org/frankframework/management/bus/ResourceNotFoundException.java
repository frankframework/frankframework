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
package org.frankframework.management.bus;

import org.frankframework.core.IbisException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String message) {
		this(message, null);
	}

	/**
	 * Seen as ERROR
	 * Stacktrace information is logged but not passed to the parent to limit sensitive information being sent over the 'bus'.
	 */
	public ResourceNotFoundException(String message, Throwable exception) {
		super(new IbisException(message, exception).getMessage());
		if (exception == null) {
			log.warn(super.getMessage()); // expanded message is logged directly
		} else {
			log.error(message, exception); // normal message, expanded by printing the stacktrace
		}
	}
}
