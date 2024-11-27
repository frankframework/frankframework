/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.processors;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.AbstractSenderWrapper;
import org.frankframework.stream.Message;

/**
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public class InputOutputSenderWrapperProcessor extends AbstractSenderWrapperProcessor {

	@Override
	public SenderResult sendMessage(AbstractSenderWrapper abstractSenderWrapper, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Message senderInput=message;
		if (StringUtils.isNotEmpty(abstractSenderWrapper.getStoreInputInSessionKey())) {
			try {
				message.preserve();
			} catch (IOException e) {
				throw new SenderException("Could not preserve input",e);
			}
			session.put(abstractSenderWrapper.getStoreInputInSessionKey(), message);
		}
		if (StringUtils.isNotEmpty(abstractSenderWrapper.getGetInputFromSessionKey())) {
			if (!session.containsKey(abstractSenderWrapper.getGetInputFromSessionKey())) {
				throw new SenderException("getInputFromSessionKey ["+ abstractSenderWrapper.getGetInputFromSessionKey()+"] is not present in session");
			}
			senderInput=session.getMessage(abstractSenderWrapper.getGetInputFromSessionKey());
			if (log.isDebugEnabled())
				log.debug("set contents of session variable [{}] as input [{}]", abstractSenderWrapper.getGetInputFromSessionKey(), senderInput);
		} else {
			if (StringUtils.isNotEmpty(abstractSenderWrapper.getGetInputFromFixedValue())) {
				senderInput=new Message(abstractSenderWrapper.getGetInputFromFixedValue());
				if (log.isDebugEnabled()) log.debug("set input to fixed value [{}]", senderInput);
			}
		}
		if (abstractSenderWrapper.isPreserveInput() && message==senderInput) { // test if it is the same object, not if the contents is the same
			try {
				message.preserve();
			} catch (IOException e) {
				throw new SenderException("Could not preserve input",e);
			}
		}
		SenderResult result = senderWrapperProcessor.sendMessage(abstractSenderWrapper, senderInput, session);
		if (result.isSuccess()) {
			if (StringUtils.isNotEmpty(abstractSenderWrapper.getStoreResultInSessionKey())) {
				if (!abstractSenderWrapper.isPreserveInput()) {
					try {
						message.preserve();
					} catch (IOException e) {
						throw new SenderException("Could not preserve result",e);
					}
				}
				if (log.isDebugEnabled())
					log.debug("storing results in session variable [{}]", abstractSenderWrapper.getStoreResultInSessionKey());
				session.put(abstractSenderWrapper.getStoreResultInSessionKey(), result.getResult());
			}
			if (abstractSenderWrapper.isPreserveInput()) {
				return new SenderResult(message);
			}
		}
		return result;
	}

}
