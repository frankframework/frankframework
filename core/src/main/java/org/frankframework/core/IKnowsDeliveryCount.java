/*
   Copyright 2013 Nationale-Nederlanden, 2023-2024 WeAreFrank!

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

import org.frankframework.receivers.RawMessageWrapper;

/**
 * Interface to be implemented by Listeners that can find out the delivery count
 * of the messages they receive.
 *
 * @author  Gerrit van Brakel
 * @since	4.9
 */
public interface IKnowsDeliveryCount<M> extends IListener<M> {

	int getDeliveryCount(RawMessageWrapper<M> rawMessage);

}
