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
package org.frankframework.core;

import org.frankframework.receivers.ReceiverAware;

/**
 * Interface extending IPushingListener for listeners which connect to a
 * ListenerPort or other type of named endpoint, from which they receive
 * their messages.
 *
 * Current implementation is the PushingJmsListener.
 *
 * @author Tim van der Leeuw
 *
 */
public interface IPortConnectedListener<M> extends IPushingListener<M>, ReceiverAware<M> {

	IbisExceptionListener getExceptionListener();

	IMessageHandler<M> getHandler();

	default void checkTransactionManagerValidity() {
	}
}
