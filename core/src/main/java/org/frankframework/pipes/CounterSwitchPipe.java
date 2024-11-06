/*
   Copyright 2016, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Default;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Forward;
import org.frankframework.stream.Message;

import io.micrometer.core.instrument.DistributionSummary;

/**
 * Selects an exitState, based on the number of received messages by this pipe.
 *
 * The exitState is the difference (subtraction) between the <code>divisor</code> and
 * the remainder of [number of received messages] modulus <code>divisor</code>.
 * This will always be an integer between 1 and <code>divisor</code>, inclusive.
 *
 *
 * @author  Peter Leeuwenburgh
 */

@Forward(name = "*", description = "the exitState, based on the number of received messages")
@EnterpriseIntegrationPattern(Type.ROUTER)
public class CounterSwitchPipe extends FixedForwardPipe {
	private int divisor = 2;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getDivisor() < 2) {
			throw new ConfigurationException("divisor [" + getDivisor() + "] should be greater than or equal to 2");
		}

		for (int i = 1; i <= getDivisor(); i++) {
			if (null == findForward("" + i))
				throw new ConfigurationException("forward [" + i + "] is not defined");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String forward = "";
		PipeForward pipeForward = null;

		DistributionSummary summary = getPipeLine().getPipeStatistics(this);
		if (summary != null) {
			long count = summary.count();
			forward = "" + (getDivisor() - (count % getDivisor()));
		}

		log.debug("determined forward [{}]", forward);

		pipeForward = findForward(forward);

		if (pipeForward == null) {
			throw new PipeRunException(this, "cannot find forward or pipe named [" + forward + "]");
		}
		log.debug("resolved forward [{}] to path [{}]", forward, pipeForward.getPath());
		return new PipeRunResult(pipeForward, message);
	}

	public int getDivisor() {
		return divisor;
	}

	@Default ("2")
	public void setDivisor(int i) {
		divisor = i;
	}
}
