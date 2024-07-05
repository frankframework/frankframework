/*
   Copyright 2013 Nationale-Nederlanden

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
package org.frankframework.core;

import java.util.Map;

import org.frankframework.receivers.RawMessageWrapper;

/**
 * The <code>IPostboxListener</code> is responsible for querying a message
 * from a postbox.
 *
 * @author  John Dekker
  */
@Deprecated(forRemoval = true, since = "7.7.0")
public interface IPostboxListener<M> extends IPullingListener<M> {
	/**
	 * Retrieves the first message found from queue or other channel, that matches the
	 * specified <code>messageSelector</code>.
	 * <p>
	 *
	 * @param messageSelector search criteria for messages. Not that the format of the selector
	 *                        changes per listener, for example a JMSListener's messageSelector follows the JMS specification.
	 * @param threadContext   context in which the method is called
	 */
	RawMessageWrapper<M> retrieveRawMessage(String messageSelector, Map<String,Object> threadContext) throws ListenerException, TimeoutException;

}
