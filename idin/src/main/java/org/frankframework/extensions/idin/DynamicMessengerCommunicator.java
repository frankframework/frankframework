/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.extensions.idin;

import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.Configuration;
import net.bankid.merchant.library.IMessenger;

/**
 * Allow us to change the IMessenger.
 * TODO Would be good to cache this...
 */
public class DynamicMessengerCommunicator extends Communicator {

	public DynamicMessengerCommunicator(Configuration idinConfig) {
		super(idinConfig);
	}

	public void setMessenger(IMessenger messenger) {
		this.messenger = messenger;
	}
}
