/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.management.bus;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * A Spring Integration Gateway in it's most simplistic form.
 * Put's messages on their respective Channels.
 */
public class LocalGateway<T> extends MessagingGatewaySupport implements OutboundGateway<T> {

	@Override
	protected void onInit() {
		if(getRequestChannel() == null) {
			MessageChannel requestChannel = getApplicationContext().getBean("frank-management-bus", MessageChannel.class);
			setRequestChannel(requestChannel);
		}

		super.onInit();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Message<T> sendSyncMessage(Message<T> in) {
		return (Message<T>) super.sendAndReceiveMessage(in);
	}

	@Override
	public void sendAsyncMessage(Message<T> in) {
		super.send(in);
	}

	/* must (re-)throw exceptions and not publish them to a dead-letter-queue. */
	@Override
	public MessageChannel getErrorChannel() {
		return null;
	}
}
