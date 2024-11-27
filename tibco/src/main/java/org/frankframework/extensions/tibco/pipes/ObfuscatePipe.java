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
package org.frankframework.extensions.tibco.pipes;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;

/**
 * Pipe that performs obfuscation on a message, using the tibcrypt library.
 *
 * @author Ali Sihab Akcan
 * @version 1.0
 * @since 8.1
 */
public class ObfuscatePipe extends FixedForwardPipe {

	private @Getter Direction direction = Direction.OBFUSCATE;

	public enum Direction {
		OBFUSCATE, DEOBFUSCATE
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getDirection() == null) {
			throw new ConfigurationException("direction must be one of [OBFUSCATE, DEOBFUSCATE]");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		if (StringUtils.isEmpty(input)) {
			return new PipeRunResult(getSuccessForward(), message);
		}

		return new PipeRunResult(getSuccessForward(), getResult(input));
	}

	private String getResult(String input) throws PipeRunException {
		try {
			if (getDirection() == Direction.DEOBFUSCATE) {
				return ObfuscationEngine.decrypt(input);
			} else {
				return ObfuscationEngine.encrypt(input);
			}
		} catch (Exception e) {
			throw new PipeRunException(this, e.getMessage());
		}
	}
	/**
	 * @ff.default OBFUSCATE
	 */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}
}
