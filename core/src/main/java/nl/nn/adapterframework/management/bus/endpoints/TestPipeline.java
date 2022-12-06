/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.xml.transform.Transformer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.TEST_PIPELINE)
public class TestPipeline extends BusEndpointBase {

	protected Logger secLog = LogUtil.getLogger("SEC");
	private boolean writeSecurityLogMessage = AppConstants.getInstance().getBoolean("sec.log.includeMessage", false);

	public static final String RESULT_STATE_HEADER="state";

	private static AtomicInteger requestCount = new AtomicInteger();

	@Data
	public static class PostedSessionKey {
		int index;
		String key;
		String value;
	}

	@ActionSelector(BusAction.UPLOAD)
	public Message<Object> runTestPipeline(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, "configuration");
		String adapterName = BusMessageUtils.getHeader(message, "adapter");
		IAdapter adapter = getAdapterByName(configurationName, adapterName);

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

	//Does not support async requests because receiver requests are synchronous
	private Message<Object> processMessage(IAdapter adapter, String payload, Map<String, String> threadContext, boolean expectsReply) {
		String messageId = "testmessage" + Misc.createSimpleUUID();
		String correlationId = "Test a Pipeline " + requestCount.incrementAndGet();
		try (PipeLineSession pls = new PipeLineSession()) {
			if(threadContext != null) {
				pls.putAll(threadContext);
			}

			Date now = new Date();
			PipeLineSession.setListenerParameters(pls, messageId, correlationId, now, now);

			secLog.info("testing pipeline of adapter [{}] {}", adapter.getName(), (writeSecurityLogMessage ? "message [" + payload + "]" : ""));

			try {
				PipeLineResult plr = adapter.processMessage(messageId, new nl.nn.adapterframework.stream.Message(payload), pls);

				if(!expectsReply) {
					return null; //Abort here, we do not need a reply.
				}

				plr.getResult().unscheduleFromCloseOnExitOf(pls);
				return ResponseMessage.Builder.create().withPayload(plr.getResult()).setHeader(RESULT_STATE_HEADER, plr.getState().name()).raw();
			} catch (Exception e) {
				throw new BusException("an exception occurred while processing the message", e);
			}
		}
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
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"text\"/>"
			+ "<xsl:template match=\"/\">"
			+ "<xsl:for-each select=\"processing-instruction('ibiscontext')\">"
			+ "<xsl:variable name=\"ic\" select=\"normalize-space(.)\"/>"
			+ "<xsl:text>{</xsl:text>"
			+ "<xsl:value-of select=\"string-length($ic)\"/>"
			+ "<xsl:text>}</xsl:text>"
			+ "<xsl:value-of select=\"$ic\"/>"
			+ "</xsl:for-each>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}
}
