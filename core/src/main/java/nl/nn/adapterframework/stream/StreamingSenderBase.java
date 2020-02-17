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

import java.io.IOException;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.SenderWithParametersBase;

public abstract class StreamingSenderBase extends SenderWithParametersBase implements IStreamingSender {

	@Override
	public abstract PipeRunResult sendMessage(String correlationID, Message message, IPipeLineSession session, IOutputStreamingSupport nextProvider) throws SenderException, TimeOutException;
	@Override
	public abstract MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, IOutputStreamingSupport nextProvider) throws StreamingException;

	
	@Override
	// can make this sendMessage() 'final', debugging handled by the new abstract sendMessage() above, that includes the MessageOutputStream
	public final Message sendMessage(String correlationID, Message message, IPipeLineSession session) throws SenderException, TimeOutException, IOException {
		PipeRunResult result = sendMessage(correlationID, new Message(message), session, null);
		return result==null?null:new Message(result.getResult());
	}

	
	@Override
	public boolean supportsOutputStreamPassThrough() {
		return true;
	}
	

}
