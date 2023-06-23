/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

/**
 * The interface clients (users) of a service may use.
 */
public interface ServiceClient {

	/**
	 * Method to implement for processing a request. This will usually delegate
	 * to a {@link nl.nn.adapterframework.core.IListener} implementation.
	 *
	 * @param message {@link Message} to process
	 * @param session {@link PipeLineSession} of the request.
	 * @return Resulting {@link Message}.
	 * @throws ListenerException Thrown if an exception occurs.
	 */
	Message processRequest(Message message, PipeLineSession session) throws ListenerException;
}
