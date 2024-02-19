/*
   Copyright 2013 Nationale-Nederlanden

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
package org.frankframework.core;

import lombok.Getter;

/**
 * Exception thrown by the ISender (implementation) to notify
 * that the sending did not succeed.
 *
 * @author  Gerrit van Brakel
 */
public class ParameterException extends IbisException {
	private final @Getter String parameterName;

	public ParameterException(String parameterName) {
		super();
		this.parameterName = parameterName;
	}
	public ParameterException(String parameterName, String errMsg) {
		super(errMsg);
		this.parameterName = parameterName;
	}
	public ParameterException(String parameterName, String errMsg, Throwable t) {
		super(errMsg, t);
		this.parameterName = parameterName;
	}
	public ParameterException(String parameterName, Throwable t) {
		super(t);
		this.parameterName = parameterName;
	}
}
