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
package org.frankframework.management.gateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;

import org.frankframework.management.bus.BusException;

/**
 * Converter which (when used in combination with a PublishSubscribeChannel) retrieves
 * the payload from the response message, logs the exception, and returns a sanitized
 * exception message with status code 400/500.
 */
public class ErrorMessageConverter extends AbstractReplyProducingMessageHandler {
	private final Logger log = LogManager.getLogger(ErrorMessageConverter.class);

	public ErrorMessageConverter() {
		setRequiresReply(true);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		if (requestMessage.getPayload() instanceof Exception e) {
			log.error("an error occurred while handling frank-management-bus request", e);

			if(e instanceof MessageDeliveryException) { //hide timeouts
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			// For the correct mapping the status code should match SpringBusExceptionHandler.BusException
			// And to make the Exception more readable, return the cause's message.
			if (e.getCause() instanceof BusException ex) {
				return new ResponseEntity<>(ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode()));
			}

			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

}
