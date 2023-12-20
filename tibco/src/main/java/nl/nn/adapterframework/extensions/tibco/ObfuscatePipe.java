/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.extensions.tibco;

import java.io.IOException;

import com.tibco.security.AXSecurityException;
import com.tibco.security.ObfuscationEngine;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;

public class ObfuscatePipe extends FixedForwardPipe {

		private Direction direction = Direction.OBFUSCATE;

		public enum Direction {
			OBFUSCATE, DEOBFUSCATE;
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
			String result = input;
			
			if (getDirection() == Direction.DEOBFUSCATE) {
				try {
					result = new String(ObfuscationEngine.decrypt(input));
				} catch (AXSecurityException e) {
					new PipeRunException(this, e.getMessage());
				}
			} else {
				try {
					result = new String(ObfuscationEngine.encrypt(input.toCharArray()));
				} catch (AXSecurityException e) {
					new PipeRunException(this, e.getMessage());
				}
			}

			return new PipeRunResult(getSuccessForward(), result);
		}

		public void setDirection(Direction direction) {
			this.direction = direction;
		}

		public Direction getDirection() {
			return direction;
		}
}