/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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
package org.frankframework.pipes;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;

/**
 * Provides a base class for a Pipe that always has the same forward.
 * Subclasses should call <code>super.configure()</code> in their <code>configure()</code> methods.
 *
 * @author Gerrit van Brakel
 */
@Forward(name = "success", description = "successful processing of the message of the pipe")
public abstract class FixedForwardPipe extends AbstractPipe {

	private @Getter PipeForward successForward;

	/**
	 * Checks for correct configuration of forward.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		successForward = findForward(PipeForward.SUCCESS_FORWARD_NAME);
		if (successForward == null) {
			throw new ConfigurationException("has no forward with name [" + PipeForward.SUCCESS_FORWARD_NAME + "]");
		}
	}

	protected @Nullable String getParameterValue(@NonNull ParameterValueList pvl, String parameterName) {
		ParameterValue pv = pvl.findParameterValue(parameterName);
		if(pv != null) {
			return pv.asStringValue(null);
		}
		return null;
	}

}
