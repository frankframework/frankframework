/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
 * Additional behaviour for pulling listeners that are able to listen to a specific
 * message, specified by a correlation ID.
 * @param <M> the raw message type
 *
 * @author  Gerrit van Brakel
 * @since   4.0
 */
public interface ICorrelatedPullingListener<M> extends IPullingListener<M> {

	/**
	 * Retrieves messages from queue or other channel, but retrieves only
	 * messages with the specified correlationId.
	 */
	RawMessageWrapper<M> getRawMessage(String correlationId, Map<String,Object> threadContext) throws ListenerException, TimeoutException;
}
