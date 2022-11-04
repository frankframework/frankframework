/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.stream.Message;

/**
 * Interface for handlers in SenderWrapper processor chain.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public interface SenderWrapperProcessor {

	public SenderResult sendMessage(SenderWrapperBase senderWrapperBase, Message message, PipeLineSession session) throws SenderException, TimeoutException;

}
