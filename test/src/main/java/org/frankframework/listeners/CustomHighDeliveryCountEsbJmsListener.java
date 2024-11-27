/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.listeners;

import java.lang.reflect.Field;

import jakarta.jms.Message;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.extensions.esb.EsbJmsListener;
import org.frankframework.jta.narayana.NarayanaTransactionHelper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;

/**
 * This is an integration test that ensures that the {@link NarayanaTransactionHelper} allows connections to be closed.
 * See PR #4517 - Ensure connections are closed properly when using Narayana.
 * <p/>
 * <p/>
 * This class creates a scenario where a message with a high Delivery-Count (JMS header) is offered to the Frank!Application.
 * While attempting to read the message of the Queue and put it in an ErrorStorage, the TX timeout should kick in, resulting
 * in a message that keeps bouncing between EMS and the application.
 *
 * Don't forget to change the default transaction timeout to &lt; 30 seconds, to speed up testing.
 */
public class CustomHighDeliveryCountEsbJmsListener extends EsbJmsListener {

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			Field retryIntervalField = Receiver.class.getDeclaredField("retryInterval");
			retryIntervalField.setAccessible(true);
			retryIntervalField.set(getReceiver(), 100);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public int getDeliveryCount(RawMessageWrapper<Message> rawMessage) {
		return 100;
	}
}
