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

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.stream.Message;

/**
 * ForPipe is a wrapper to use another pipe a fixed number of times. This can be accomplished by something like:
 *
 * <pre>{@code
 * 		<ForPipe name="forPipe" incrementSessionKey="i" startAt="0" max="10">
 * 		 	<Forward name="stop" path="EXIT" />
 * 		 	<Forward name="continue" path="echoPipe"/>
 * 		</ForPipe>
 *
 * 		<EchoPipe name="echoPipe" getInputFromSessionKey="i">
 * 		 	<Forward name="success" path="forPipe"/>
 * 		</EchoPipe>
 * }</pre>
 * <p>
 * This should call the echoPipe for i=0 until i=10.
 * <p>
 * After completing the for loop, the `incrementSessionKey` will be removed.
 *
 * @author evandongen
 */
@ElementType(ElementTypes.ITERATOR)
public class ForPipe extends FixedForwardPipe {

	static final String STOP_FORWARD_NAME = "stop";
	static final String CONTINUE_FORWARD_NAME = "continue";
	private @Getter String incrementSessionKey = "i";
	private @Getter int startAt = 0;
	private @Getter Integer stopAt = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		// Mandatory parameters
		if (stopAt == null) {
			throw new ConfigurationException("Value for 'max' is mandatory to break out of the for loop pipe");
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
		// Get the value from the session, default to the startAt value
		int i = session.get(incrementSessionKey, startAt);

		// If i < max, increment and continue
		if (i < stopAt) {
			PipeForward continueForward = findForward(CONTINUE_FORWARD_NAME);

			session.put(incrementSessionKey, ++i);

			return new PipeRunResult(continueForward, message);
		}

		// Else, remove the session key and forward to the stop forward
		session.remove(incrementSessionKey);
		return new PipeRunResult(findForward(STOP_FORWARD_NAME), message);
	}

	/**
	 * Sets the session key which holds the value to be incremented.
	 *
	 * @ff.default i
	 */
	public void setIncrementSessionKey(String incrementSessionKey) {
		this.incrementSessionKey = incrementSessionKey;
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
	 */
	public void setStopAt(Integer stopAt) {
		this.stopAt = stopAt;
	}
}
