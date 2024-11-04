/*
   Copyright 2016, 2020 Nationale-Nederlanden, 2021, 2023 WeAreFrank!

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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.stream.Message;
import org.frankframework.util.PasswordHash;

/**
 * Hash a password or validate a password against a hash using PasswordHash.java
 * from <a href="https://crackstation.net/hashing-security.htm">https://crackstation.net/hashing-security.htm</a>.
 * Input of the pipe is expected to be the password. In case hashSessionKey
 * isn't used a hash of the password is returned. In case hashSessionKey is used
 * it is validated against the hash in the session key which will determine
 * the forward to be used (success or failure).
 *
 *
 * @author Jaco de Groot
 */
@Forward(name = "failure", description = "when hashSessionKey is used and password doesn't validate against the hash")
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class PasswordHashPipe extends FixedForwardPipe {
	private static final String FAILURE_FORWARD_NAME = "failure";
	private String hashSessionKey;
	private int rounds = 40000;
	private String roundsSessionKey = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getHashSessionKey())) {
			if (findForward(FAILURE_FORWARD_NAME) == null) {
				ConfigurationWarnings.add(this, log, "has hashSessionKey attribute but forward failure is not configured");
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Object result;
		PipeForward pipeForward;
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		if (StringUtils.isEmpty(getHashSessionKey())) {
			try {
				if (getRoundsSessionKey() == null) {
					result = PasswordHash.createHash(input.toCharArray(), getRounds());
				} else {
					result = PasswordHash.createHash(input.toCharArray(), session.getInteger(getRoundsSessionKey()));
				}
				pipeForward = getSuccessForward();
			} catch (Exception e) {
				throw new PipeRunException(this, "Could not hash password", e);
			}
		} else {
			try {
				result = message;
				if (PasswordHash.validatePassword(input, session.getString(getHashSessionKey()))) {
					pipeForward = getSuccessForward();
				} else {
					pipeForward = findForward(FAILURE_FORWARD_NAME);
				}
			} catch (Exception e) {
				throw new PipeRunException(this, "Could not validate password", e);
			}
		}
		return new PipeRunResult(pipeForward, result);
	}

	public String getHashSessionKey() {
		return hashSessionKey;
	}

	/** name of sessionkey that holds the hash which will be used to validate the password (input of the pipe) */
	public void setHashSessionKey(String hashSessionKey) {
		this.hashSessionKey = hashSessionKey;
	}

	public int getRounds() {
		return rounds;
	}

	public void setRounds(int rounds) {
		this.rounds = rounds;
	}

	public String getRoundsSessionKey() {
		return roundsSessionKey;
	}

	public void setRoundsSessionKey(String roundsSessionKey) {
		this.roundsSessionKey = roundsSessionKey;
	}

}
