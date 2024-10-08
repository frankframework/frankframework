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

package org.frankframework.pipes;

import java.util.Optional;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * ForPipe is a wrapper to use another pipe a fixed number of times. This can be accomplished by something like:
 *
 * <pre>{@code
 * 		<ForPipe name="forPipe" startAt="0" stopAt="10">
 * 		 	<Forward name="stop" path="EXIT" />
 * 		 	<Forward name="continue" path="echoPipe"/>
 * 		</ForPipe>
 *
 * 		<EchoPipe name="echoPipe" getInputFromSessionKey="forPipe.iteration">
 * 		 	<Forward name="success" path="forPipe"/>
 * 		</EchoPipe>
 * }</pre>
 * <p>
 * This should call the echoPipe for i=0 until i=10.
 * <p>
 *
 * @author evandongen
 * @ff.info After completing the for loop, the sessionKey containing the for loop iteration state, will be removed.
 * @ff.info The default format for the session key is "pipeName-iteration"
 * @ff.note If both attribute and parameter are set for 'stopAt', the attribute value will be used.
 */
@ElementType(ElementTypes.ITERATOR)
public class ForPipe extends FixedForwardPipe {

	static final String STOP_FORWARD_NAME = "stop";
	static final String CONTINUE_FORWARD_NAME = "continue";
	static final String STOP_AT_PARAMETER_VALUE = "stopAt";
	private static final String INCREMENT_SESSION_KEY_SUFFIX = "iteration";
	private @Getter int startAt = 0;
	private @Getter Integer stopAt = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		// Mandatory stopAt attribute / parameter
		if (stopAt == null && !getParameterList().hasParameter(STOP_AT_PARAMETER_VALUE)) {
			throw new ConfigurationException("Value for 'stopAt' is mandatory to break out of the for loop pipe");
		}

		// Mandatory forwards
		PipeForward stopForward = findForward(STOP_FORWARD_NAME);
		PipeForward continueForward = findForward(CONTINUE_FORWARD_NAME);

		if (stopForward == null) {
			throw new ConfigurationException("has no forward with name [" + STOP_FORWARD_NAME + "]");
		}

		if (continueForward == null) {
			throw new ConfigurationException("has no forward with name [" + CONTINUE_FORWARD_NAME + "]");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Integer stopAtValue = determineStopAtValue(message, session);
		String sessionKey = getIncrementSessionKey();

		// Get the value from the session, default to the startAt value
		int i = session.get(sessionKey, startAt);

		// If i < stopAtValue, increment and continue
		if (i < stopAtValue) {
			PipeForward continueForward = findForward(CONTINUE_FORWARD_NAME);

			session.put(sessionKey, ++i);

			return new PipeRunResult(continueForward, message);
		}

		// Else, remove the session key and forward to the stop forward
		session.remove(sessionKey);
		return new PipeRunResult(findForward(STOP_FORWARD_NAME), message);
	}

	String getIncrementSessionKey() {
		return String.format("%s.%s", getName(), INCREMENT_SESSION_KEY_SUFFIX);
	}

	private Integer determineStopAtValue(Message message, PipeLineSession session) throws PipeRunException {
		if (stopAt != null) {
			return stopAt;
		}

		Optional<ParameterValueList> optionalParameterValueList = getParameterValueList(message, session);

		if (optionalParameterValueList.isPresent()) {
			ParameterValue stopAtParameter = optionalParameterValueList.get().get(STOP_AT_PARAMETER_VALUE);

			if (stopAtParameter.getValue() != null && StringUtils.isNotBlank(stopAtParameter.asStringValue())) {
				return stopAtParameter.asIntegerValue(0);
			}
		}

		throw new PipeRunException(this, "Can't determine 'stopAt' value");
	}

	private Optional<ParameterValueList> getParameterValueList(Message message, PipeLineSession session) throws PipeRunException {
		if (getParameterList() != null) {
			try {
				return Optional.of(getParameterList().getValues(message, session));
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}

		return Optional.empty();
	}

	/**
	 * Starts counting at this value.
	 *
	 * @ff.default 0
	 */
	public void setStartAt(int startAt) {
		this.startAt = startAt;
	}

	/**
	 * Break from the loop when incrementSessionKey equals this value
	 *
	 * @ff.mandatory
	 */
	public void setStopAt(Integer stopAt) {
		this.stopAt = stopAt;
	}
}
