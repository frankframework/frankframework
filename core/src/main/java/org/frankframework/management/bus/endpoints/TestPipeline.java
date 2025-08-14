/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.transform.Transformer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SpringSecurityHandler;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.XmlUtils;

@Log4j2
@BusAware("frank-management-bus")
@TopicSelector(BusTopic.TEST_PIPELINE)
public class TestPipeline extends BusEndpointBase {

	protected Logger secLog = LogUtil.getLogger("SEC");
	private boolean writeSecurityLogMessage = AppConstants.getInstance().getBoolean("sec.log.includeMessage", false);

	private static AtomicInteger requestCount = new AtomicInteger();

	@Data
	public static class PostedSessionKey {
		String key;
		String value;
	}

	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed("IbisTester")
	public MessageBase<?> runTestPipeline(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, "configuration");
		String adapterName = BusMessageUtils.getHeader(message, "adapter");
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		boolean expectsReply = message.getHeaders().containsKey("replyChannel");

		if(!(message.getPayload() instanceof String)) {
			throw new BusException("payload is not instance of String");
		}

		Map<String, String> threadContext = new HashMap<>();

		String sessionKeys = BusMessageUtils.getHeader(message, "sessionKeys");
		if(StringUtils.isNotEmpty(sessionKeys)) {
			threadContext.putAll(getSessionKeysFromHeader(sessionKeys));
		}

		String payload = (String) message.getPayload();
		threadContext.putAll(getSessionKeysFromPayload(payload));

		return processMessage(adapter, payload, threadContext, expectsReply);
	}

	// Does not support async requests because receiver requests are synchronous
	private MessageBase<?> processMessage(Adapter adapter, String payload, Map<String, String> threadContext, boolean expectsReply) {
		String messageId = MessageUtils.generateMessageId("testmessage");
		try (PipeLineSession pls = new PipeLineSession()) {
			// Make sure the pipeline session has a security handler
			pls.setSecurityHandler(new SpringSecurityHandler());

			if(threadContext != null) {
				pls.putAll(threadContext);
			}

			String correlationId = null;
			if (!pls.containsKey(PipeLineSession.CORRELATION_ID_KEY)) {
				correlationId = "Test a Pipeline " + requestCount.incrementAndGet();
			}

			PipeLineSession.updateListenerParameters(pls, messageId, correlationId);

			secLog.info("testing pipeline of adapter [{}] {}", adapter.getName(), (writeSecurityLogMessage ? "message [" + payload + "]" : ""));

			try {
				org.frankframework.stream.Message message = org.frankframework.stream.Message.nullMessage();
				if (StringUtils.isNotEmpty(payload)) {
					message = new org.frankframework.stream.Message(payload);
				}

				PipeLineResult plr = adapter.processMessageDirect(messageId, message, pls);

				// Only send a reply if we expect one, else it's wasted traffic...
				return expectsReply ? convertPipelineResult(plr) : null;
			} catch (Exception e) {
				throw new BusException("an exception occurred while processing the message", e);
			}
		}
	}

	private MessageBase<?> convertPipelineResult(PipeLineResult plr) throws IOException {
		final MessageBase<?> response;
		if (org.frankframework.stream.Message.isEmpty(plr.getResult())) {
			response = EmptyMessage.noContent();
		} else {
			response = new BinaryMessage(plr.getResult().asInputStream());
		}

		response.setHeader(MessageBase.STATE_KEY, plr.getState().name());
		return response;
	}

	/**
	 * Parses SessionKeys from the sessionKeys header.
	 * Format: [{"index":1,"key":"test","value":"123"}]
	 */
	@Nonnull
	protected Map<String, String> getSessionKeysFromHeader(String sessionKeys) {
		Map<String, String> context = new HashMap<>();
		try {
			PostedSessionKey[] postedSessionKeys = new ObjectMapper().readValue(sessionKeys, PostedSessionKey[].class);
			for(PostedSessionKey psk : postedSessionKeys) {
				context.put(psk.getKey(), psk.getValue());
			}
		} catch (Exception e) {
			throw new BusException("An exception occurred while parsing session keys", e);
		}
		return context;
	}

	/**
	 * Parses the 'ibiscontext' processing instruction defined in the input
	 */
	@Nonnull
	protected Map<String, String> getSessionKeysFromPayload(String input) {
		String str = findProcessingInstructions(input);
		if(StringUtils.isEmpty(str)) {
			return Collections.emptyMap();
		}

		try {
			LinkedHashMap<String, String> ibisContexts = new LinkedHashMap<>();
			int indexBraceOpen = str.indexOf("{");
			int indexBraceClose = 0;
			int indexStartNextSearch = 0;
			while (indexBraceOpen >= 0) {
				indexBraceClose = str.indexOf("}", indexBraceOpen+1);
				if (indexBraceClose > indexBraceOpen) {
					String ibisContextLength = str.substring(indexBraceOpen+1, indexBraceClose);
					int icLength = Integer.parseInt(ibisContextLength);
					if (icLength > 0) {
						indexStartNextSearch = indexBraceClose + 1 + icLength;
						String ibisContext = str.substring(indexBraceClose+1, indexStartNextSearch);
						int indexEqualSign = ibisContext.indexOf("=");
						String key;
						String value;
						if (indexEqualSign < 0) {
							key = ibisContext;
							value = "";
						} else {
							key = ibisContext.substring(0,indexEqualSign);
							value = ibisContext.substring(indexEqualSign+1);
						}
						ibisContexts.put(key, value);
					} else {
						indexStartNextSearch = indexBraceClose + 1;
					}
				} else {
					indexStartNextSearch = indexBraceOpen + 1;
				}
				indexBraceOpen = str.indexOf("{", indexStartNextSearch);
			}
			return Collections.unmodifiableMap(ibisContexts);
		} catch (Exception e) {
			throw new BusException("unable to create thread context", e);
		}
	}

	/**
	 * Checks if the input is valid XML, and returns processing instructions if any
	 */
	private String findProcessingInstructions(String input) {
		if (StringUtils.isEmpty(input) || !XmlUtils.isWellFormed(input)) {
			return null;
		}

		String xslt = findProcessingInstructionsXslt();
		try {
			Transformer t = XmlUtils.createTransformer(xslt);
			String str = XmlUtils.transformXml(t, input);
			log.debug("found processing instructions [{}]", str);
			return str;
		} catch (Exception e) {
			throw new BusException("unable to create thread context", e);
		}
	}

	private String findProcessingInstructionsXslt() {
		return
		"""
		<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">\
		<xsl:output method="text"/>\
		<xsl:template match="/">\
		<xsl:for-each select="processing-instruction('ibiscontext')">\
		<xsl:variable name="ic" select="normalize-space(.)"/>\
		<xsl:text>{</xsl:text>\
		<xsl:value-of select="string-length($ic)"/>\
		<xsl:text>}</xsl:text>\
		<xsl:value-of select="$ic"/>\
		</xsl:for-each>\
		</xsl:template>\
		</xsl:stylesheet>\
		""";
	}
}
