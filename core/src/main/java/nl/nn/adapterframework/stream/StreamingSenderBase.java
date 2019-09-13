/*
   Copyright 2019 Integration Partners

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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

public abstract class StreamingSenderBase extends SenderWithParametersBase implements IOutputStreamingSupport {

//	private final boolean TEST_STREAMING_VIA_SEND_MESSAGE=false;
	
	public abstract String sendMessage(String correlationID, String message, ParameterResolutionContext prc, MessageOutputStream target) throws SenderException, TimeOutException;
	@Override
	public abstract MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException;

	
	@Override
	public final String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
//		if (TEST_STREAMING_VIA_SEND_MESSAGE && canProvideOutputStream()) {
//			try {
//				MessageOutputStream target = provideOutputStream(correlationID, prc.getSession(), null);
//				try (Writer writer = target.asWriter()) {
//					writer.write(message);
//				}
//				return target.getResponseAsString();
//			} catch (StreamingException|IOException e) {
//				throw new SenderException(e);
//			}
//		}
		return sendMessage(correlationID, message, prc, null);
	}

	@Override
	public boolean canStreamToTarget() {
		return true;
	}

	@Override
	public boolean canProvideOutputStream() {
		return true;
	}

}
