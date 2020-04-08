/*
   Copyright 2019 Integration Partners, 2020 WeAreFrank!

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
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.BlockEnabledSenderBase;

public abstract class StreamingSenderBase<H> extends BlockEnabledSenderBase<H> implements IStreamingSender<H> {

	@Override
	public abstract PipeRunResult sendMessage(H blockHandle, Message message, IPipeLineSession session, IOutputStreamingSupport nextProvider) throws SenderException, TimeOutException;
	@Override
	public abstract MessageOutputStream provideOutputStream(IPipeLineSession session, IOutputStreamingSupport nextProvider) throws StreamingException;

	
	@Override
	// can make this sendMessage() 'final', debugging handled by the new abstract sendMessage() above, that includes the MessageOutputStream
	public final Message sendMessage(H blockHandle, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		PipeRunResult result = sendMessage(blockHandle, message, session, null);
		return Message.asMessage(result.getResult());
	}

	@Override
	public final PipeRunResult sendMessage(Message message, IPipeLineSession session, IOutputStreamingSupport next) throws SenderException, TimeOutException {
		H blockHandle = openBlock(session);
		try {
			return sendMessage(blockHandle, message, session, next);
		} finally {
			closeBlock(blockHandle, session);
		}
	}

	
	@Override
	public boolean supportsOutputStreamPassThrough() {
		return true;
	}
	
	@Override
	public H openBlock(IPipeLineSession session) throws SenderException, TimeOutException {
		// provide default implementation of openBlock()
		return null;
	}
	@Override
	public void closeBlock(H blockHandle, IPipeLineSession session) throws SenderException {
		// provide default implementation of closeBlock()
	}
	

}
