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
package nl.nn.adapterframework.processors;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.stream.Message;

/**
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public class InputOutputSenderWrapperProcessor extends SenderWrapperProcessorBase {

	@Override
	public SenderResult sendMessage(SenderWrapperBase senderWrapperBase, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Message senderInput=message;
		if (StringUtils.isNotEmpty(senderWrapperBase.getStoreInputInSessionKey())) {
			try {
				message.preserve();
			} catch (IOException e) {
				throw new SenderException("Could not preserve input",e);
			}
			session.put(senderWrapperBase.getStoreInputInSessionKey(), message);
		}
		if (StringUtils.isNotEmpty(senderWrapperBase.getGetInputFromSessionKey())) {
			if (!session.containsKey(senderWrapperBase.getGetInputFromSessionKey())) {
				throw new SenderException("getInputFromSessionKey ["+senderWrapperBase.getGetInputFromSessionKey()+"] is not present in session");
			}
			senderInput=session.getMessage(senderWrapperBase.getGetInputFromSessionKey());
			if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"set contents of session variable ["+senderWrapperBase.getGetInputFromSessionKey()+"] as input ["+senderInput+"]");
		} else {
			if (StringUtils.isNotEmpty(senderWrapperBase.getGetInputFromFixedValue())) {
				senderInput=new Message(senderWrapperBase.getGetInputFromFixedValue());
				if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"set input to fixed value ["+senderInput+"]");
			}
		}
		if (senderWrapperBase.isPreserveInput() && message==senderInput) { // test if it is the same object, not if the contents is the same
			try {
				message.preserve();
			} catch (IOException e) {
				throw new SenderException("Could not preserve input",e);
			}
		}
		SenderResult result = senderWrapperProcessor.sendMessage(senderWrapperBase, senderInput, session);
		if (result.isSuccess()) {
			if (StringUtils.isNotEmpty(senderWrapperBase.getStoreResultInSessionKey())) {
				if (!senderWrapperBase.isPreserveInput()) {
					try {
						message.preserve();
					} catch (IOException e) {
						throw new SenderException("Could not preserve result",e);
					}
				}
				if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"storing results in session variable ["+senderWrapperBase.getStoreResultInSessionKey()+"]");
				session.put(senderWrapperBase.getStoreResultInSessionKey(), result.getResult());
			}
			if (senderWrapperBase.isPreserveInput()) {
				return new SenderResult(message);
			}
		}
		return result;
	}

}
