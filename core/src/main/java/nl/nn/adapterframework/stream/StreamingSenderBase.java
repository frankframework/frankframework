/*
   Copyright 2019, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.stream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.SenderWithParametersBase;

public abstract class StreamingSenderBase extends SenderWithParametersBase implements IStreamingSender {

	private boolean canProvideOutputStream;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		canProvideOutputStream = getParameterList()==null || !getParameterList().isInputValueOrContextRequiredForResolution();
	}

	@Override
	// can make this sendMessage() 'final', debugging handled by IStreamingSender.sendMessage(), that includes the MessageOutputStream
	public final SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		PipeRunResult result = sendMessage(message, session, null);
		return new SenderResult(result.getResult());
	}

	/**
	 * returns true when there are no parameters that require the input value or context 
	 * (or other side effects inhibiting providing an outputstream, to be determined by descendants)
	 */
	protected boolean canProvideOutputStream() {
		return canProvideOutputStream;
	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return true;
	}
}
