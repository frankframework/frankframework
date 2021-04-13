/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;

/**
 * Pipe that generates an UUID (Universally Unique Identifier).
 *
 * 
 * If {@link #setType(String) type} is set to <code>numeric</code>, a UUID with fixed length 31 will be generated.
 * If {@link #setType(String) type} is set to <code>alphanumeric</code>, the UUID will not have a fixed length which will be about 42.
 * Only type <code>alphanumeric</code> guarantees a 100% unique identifier, type <code>numeric</code> has a 0.01% chance of exactly the same id in case of multiple calls on the same host within a few milliseconds.  
 * 
 * @author Peter Leeuwenburgh
 */
public class UUIDGeneratorPipe extends FixedForwardPipe {

	private String type = "alphanumeric";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String uType = getType();
		if (uType == null) {
			throw new ConfigurationException("type must be set");
		}
		if (!uType.equalsIgnoreCase("alphanumeric")
			&& !uType.equalsIgnoreCase("numeric")) {
			throw new ConfigurationException("illegal value for type ["
					+ uType
					+ "], must be 'alphanumeric' or 'numeric'");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		String result = null;
		if ("alphanumeric".equalsIgnoreCase(getType())) {
			result = Misc.createUUID();
		} else {
			result = Misc.createNumericUUID();
		}

		return new PipeRunResult(getForward(), result);
	}

	public String getType() {
		return type;
	}

	@IbisDoc({"either <code>alphanumeric</code> or <code>numeric</code>", "alphanumeric"})
	public void setType(String type) {
		this.type = type;
	}
}