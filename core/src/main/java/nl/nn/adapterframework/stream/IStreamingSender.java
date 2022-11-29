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

import nl.nn.adapterframework.core.ProcessBlockResult;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;

public interface IStreamingSender extends ISenderWithParameters, IOutputStreamingSupport {

	/**
	 * IStreamingSender can return one of:
	 * <ul>
	 * <li>a SenderResult, with possibly a local forwardName suggestion, to be resolved by the enclosing pipe,</li>
	 * <li>a PipeRunResult with a resolved forward, e.g. when the sender provided a MessageOutputStream, or it has sent it's result via a MessageOutputStream.</li>
	 * </ul>
	 * 
	 */
	public ProcessBlockResult sendMessage(Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeoutException;

}
