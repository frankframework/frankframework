/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.stream.Message;

/* 
 * Interface to be implemented by Senders that beside their proper result return a state, 
 * that can be used to determine a forward. 
 */
public interface IForwardNameProvidingSender extends ISenderWithParameters {

	public SenderResult sendMessageAndProvideForwardName(Message message, PipeLineSession session) throws SenderException, TimeoutException;

	@Override
	default Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		SenderResult senderResult = sendMessageAndProvideForwardName(message, session);
		Message result = senderResult.getResult();
		if (!senderResult.isSuccess()) {
			SenderException se = new SenderException(senderResult.getErrorMessage());
			try {
				result.close();
			} catch (Exception e) {
				se.addSuppressed(e);
			}
			throw (se);
		}
		return result;
	}


}
