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
package nl.nn.adapterframework.management.gateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;

/**
 * Converter which (when used in combination with a PublishSubscribeChannel) retrieves
 * the payload from the response message, logs the exception, and returns a sanitized
 * exception message with status code 400.
 */
public class ErrorMessageConverter extends AbstractReplyProducingMessageHandler {
	private final Logger log = LogManager.getLogger(ErrorMessageConverter.class);
	private static final HttpStatus ERROR_STATUS = HttpStatus.BAD_REQUEST;

	public ErrorMessageConverter() {
		setRequiresReply(true);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String body = null;
		if(requestMessage.getPayload() instanceof Exception) {
			Exception e = (Exception) requestMessage.getPayload();
			if(!(e instanceof MessageTimeoutException)) {
				body = e.getMessage();
			}
			log.error("an error occurred while handling frank-management-bus request", e);
		}
		return new ResponseEntity<String>(body, ERROR_STATUS);
	}

}
