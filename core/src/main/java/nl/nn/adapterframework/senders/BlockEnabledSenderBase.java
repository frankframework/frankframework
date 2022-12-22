/*
   Copyright 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.IBlockEnabledSender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;

public abstract class BlockEnabledSenderBase<H> extends SenderWithParametersBase implements IBlockEnabledSender<H> {

	@Override
	public final SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		H blockHandle = openBlock(session);
		try {
			return sendMessage(blockHandle, message, session);
		} finally {
			closeBlock(blockHandle, session);
		}
	}


}
