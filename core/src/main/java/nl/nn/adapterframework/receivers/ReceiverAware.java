/*
   Copyright 2017 Integration Partners

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

import nl.nn.adapterframework.core.IReceiver;


/**
 * When a listener implements this interface it will get a reference to it's
 * parent receiver.
 * 
 * @author Jaco de Groot
 *
 */
public interface ReceiverAware {

	public void setReceiver(IReceiver receiver);
	public IReceiver getReceiver();

}
