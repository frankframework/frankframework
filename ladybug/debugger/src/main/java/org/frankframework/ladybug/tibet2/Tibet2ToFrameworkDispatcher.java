/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.ladybug.tibet2;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.wearefrank.ladybug.echo2.reports.ReportsComponent;
import org.wearefrank.ladybug.storage.StorageException;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.JacksonUtils;
import org.frankframework.util.StreamUtil;

public class Tibet2ToFrameworkDispatcher {
	private static final String AUTHORISATION_CHECK_ADAPTER_CONFIG = "main";
	private static final String AUTHORISATION_CHECK_ADAPTER_NAME = "AuthorisationCheck";
	private static final String AUTHORISATION_CHECK_DELETE_ADAPTER_NAME = "DeleteFromExceptionLog";

	private final OutboundGateway gateway;

	public Tibet2ToFrameworkDispatcher(ApplicationContext context) {
		this(context.getBean("outboundGateway", OutboundGateway.class));
	}

	public Tibet2ToFrameworkDispatcher(OutboundGateway gateway) {
		this.gateway = gateway;
	}

	public String authorisationCheck(String storageId, String viewName) {
		try {
			MessageBuilder<String> builder = createRequestMessage("<dummy />", AUTHORISATION_CHECK_ADAPTER_CONFIG, AUTHORISATION_CHECK_ADAPTER_NAME);
			builder.setHeader(BusMessageUtils.HEADER_PREFIX+"sessionKeys", toJson(storageId, viewName));

			@NonNull
			Message<Object> result = sendMessage(builder.build());

			if ("SUCCESS".equalsIgnoreCase(BusMessageUtils.getHeader(result, MessageBase.STATE_KEY))) {
				return ReportsComponent.OPEN_REPORT_ALLOWED;
			} else {
				return "Not allowed. Result of adapter " + AUTHORISATION_CHECK_ADAPTER_NAME + ": " + convertPayload(result.getPayload());
			}
		} catch (IOException | BusException e) {
			return "Not allowed. Result of adapter " + AUTHORISATION_CHECK_ADAPTER_NAME + ": " + e.getMessage();
		}
	}

	public void deleteReport(String checkpointMessage) throws StorageException {
		try {
			MessageBuilder<String> builder = createRequestMessage(checkpointMessage, AUTHORISATION_CHECK_ADAPTER_CONFIG, AUTHORISATION_CHECK_DELETE_ADAPTER_NAME);

			@NonNull
			Message<Object> result = sendMessage(builder.build());

			if (BusMessageUtils.getIntHeader(result, MessageBase.STATUS_KEY, 500) == 200) {
				String stringResult = convertPayload(result.getPayload());
				if (!"<ok/>".equalsIgnoreCase(stringResult)) {
					throw new StorageException("Delete failed: " + stringResult);
				}
			} else {
				throw new StorageException("Delete failed (see logging for more details)");
			}
		} catch (IOException | BusException e) {
			throw new StorageException("Delete failed (see logging for more details)", e);
		}
	}

	/**
	 * Ideally we only get BusExceptions and MessageHandlingException which wrap one.
	 */
	private Message<Object> sendMessage(Message<String> message) throws BusException {
		try {
			return gateway.sendSyncMessage(message);
		} catch (BusException e) {
			throw e;
		} catch (MessageHandlingException t) {
			if (t.getCause() instanceof BusException be) {
				throw be;
			}
			throw new BusException("unable to send message", t);
		}
	}

	@NonNull
	private static MessageBuilder<String> createRequestMessage(BusTopic topic, BusAction action, String payload, @Nullable UUID uuid) {
		MessageBuilder<String> builder = MessageBuilder.withPayload(payload);
		builder.setHeader(BusTopic.TOPIC_HEADER_NAME, topic.name());
		builder.setHeader(BusAction.ACTION_HEADER_NAME, action.name());

		// Optional target parameter, to target a specific backend node.
		if(uuid != null) {
			builder.setHeader(BusMessageUtils.HEADER_TARGET_KEY, uuid);
		}

		return builder;
	}

	@NonNull
	private static MessageBuilder<String> createRequestMessage(String inputMessage, String configName, String adapterName) {
		MessageBuilder<String> builder = createRequestMessage(BusTopic.TEST_PIPELINE, BusAction.UPLOAD, inputMessage, null);
		builder.setHeader(BusMessageUtils.HEADER_PREFIX+BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.setHeader(BusMessageUtils.HEADER_PREFIX+BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapterName);
		return builder;
	}

	@NonNull
	private static String convertPayload(Object payload) throws IOException {
		if (payload instanceof String string) {
			return string;
		} else if (payload instanceof byte[] bytes) {
			return new String(bytes);
		} else if (payload instanceof InputStream stream) {
			try {
				// Convert line endings to \n to show them in the browser as real line feeds
				return StreamUtil.streamToString(stream, "\n", false);
			} catch (IOException e) {
				throw new IOException("unable to read response payload", e);
			}
		}
		throw new IOException("unexpected response payload type [" + payload.getClass().getCanonicalName() + "]");
	}

	private static String toJson(String storageId, String viewName) {
		List<PostedSessionKey> sessionKeys = List.of(
				new PostedSessionKey("StorageId", storageId),
				new PostedSessionKey("View", viewName)
		);

		return JacksonUtils.convertToJson(sessionKeys);
	}

	static record PostedSessionKey(String key, String value) {
	}
}
