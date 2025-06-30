/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.errormessageformatters;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.HasName;
import org.frankframework.core.IErrorMessageFormatter;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.documentbuilder.ArrayBuilder;
import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.IDocumentBuilder;
import org.frankframework.documentbuilder.INodeBuilder;
import org.frankframework.documentbuilder.ObjectBuilder;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlEncodingUtils;

/**
 * This is the default {@link IErrorMessageFormatter} implementation that is used when no specific {@code ErrorMessageFormatter} has
 * been configured. It wraps an error in an XML or JSON string. XML is the default.
 * <p>
 *     If the exception is a {@link PipeRunException} that has parameters set on it, then these parameters
 *     are added to a {@code params} element in the error message. These parameters can be set from an
 *     {@link org.frankframework.pipes.ExceptionPipe}.
 * </p>
 * <p>
 *     If you need more control over the layout of the error message, then configure your {@link org.frankframework.core.Adapter} or
 *     {@link org.frankframework.configuration.Configuration} with a {@link IErrorMessageFormatter} implementation
 *     that can reformat the error message to the desired layout, the {@link XslErrorMessageFormatter} for XML error formats or the
 *     {@link DataSonnetErrorMessageFormatter} for JSON error messages.
 * </p>
 * <p>
 * Sample xml:
 * <pre>{@code
 * <errorMessage>
 *    <message timestamp="Mon Oct 13 12:01:57 CEST 2003"
 *             originator="NN IOS AdapterFramework(set from 'application.name' and 'application.version')"
 *             message="message describing the error that occurred">
 *    <location class="org.frankframework.pipes.SwitchPipe" name="ServiceSwitch"/>
 *    <details>Exception and stacktrace</details>
 *    <params>
 *      <param name="sampleParam>paramValue</param>
 *    </params>
 *    <originalMessage messageId="..." receivedTime="Mon Oct 27 12:10:18 CET 2003" >
 *        <![CDATA[contents of message for which the error occurred]]>
 *    </originalMessage>
 * </errorMessage>
 * }</pre>
 * </p>
 * <p>
 *     Sample JSON:
 *     <pre>{@code
 *         {
 *             "errorMessage": {
 *                 "timestamp": "Mon Oct 13 12:01:57 CEST 2003",
 *                 "originator": "IAF 9.2",
 *                 "message": "Message describing error and location",
 *                 "location": {
 *                     "class": "org.frankframework.pipes.SwitchPipe",
 *                     "name": "ServiceSwitch"
 *                 },
 *                 "details": "Exception and stacktrace",
 *                 "params": {
 *                     "sampleParam": "paramValue"
 *                 },
 *                 "originalMessage": {
 *                     "messageId": "...",
 *                     "receivedTime": "Mon Oct 27 12:10:18 CET 2003",
 *                     "message": "contents of message for which the error occurred"
 *                 }
 *             }
 *         }
 *     }</pre>
 * </p>
 * @author  Gerrit van Brakel
 */
@Log4j2
public class ErrorMessageFormatter implements IErrorMessageFormatter, IScopeProvider {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private @Getter @Nonnull DocumentFormat messageFormat = DocumentFormat.XML;

	/**
	 * Format the available parameters into an XML or JSON message.
	 * <br/>
	 * Override this method in subclasses to obtain the required behaviour.
	 */
	@Override
	public @Nonnull Message format(@Nullable String errorMessage, @Nullable Throwable t, @Nullable HasName location, @Nullable Message originalMessage, @Nonnull PipeLineSession session) {

		errorMessage = getErrorMessage(errorMessage, t);
		String details;
		if (t != null) {
			details = ExceptionUtils.getStackTrace(t);
		} else {
			details = null;
		}
		Map<String, Object> exceptionParams = getPipeRunExceptionParams(t);
		HasName locationToUse = getLocation(location, t);
		String messageId = session.getMessageId();
		String correlationId = session.getCorrelationId();
		String msgIdToUse = StringUtils.isNotEmpty(messageId) && !MessageUtils.isFallbackMessageId(messageId) ? messageId : correlationId;
		String prefix = locationToUse != null ? ClassUtils.nameOf(locationToUse) : null;
		if (StringUtils.isNotEmpty(msgIdToUse)) {
			prefix = StringUtil.concatStrings(prefix, " ", "msgId [" + msgIdToUse + "]");
		}
		errorMessage = StringUtil.concatStrings(prefix, ": ", errorMessage);

		String originator = AppConstants.getInstance().getProperty("application.name")+" "+ AppConstants.getInstance().getProperty("application.version");
		// Build a Base document
		try {
			MessageBuilder messageBuilder = new MessageBuilder();
			IDocumentBuilder documentBuilder = DocumentBuilderFactory.startDocument(messageFormat, "errorMessage", messageBuilder, true);

			ObjectBuilder errorObject;
			ObjectBuilder rootObjectBuilder;
			if (messageFormat == DocumentFormat.XML) {
				rootObjectBuilder = null;
				errorObject = documentBuilder.asObjectBuilder();
			} else {
				rootObjectBuilder = documentBuilder.asObjectBuilder();
				errorObject = rootObjectBuilder.addObjectField("errorMessage");
			}
			errorObject.addAttribute("timestamp", new Date().toString());
			errorObject.addAttribute("originator", originator);
			errorObject.addAttribute("message", XmlEncodingUtils.replaceNonValidXmlCharacters(errorMessage));

			addLocation(locationToUse, errorObject);

			if (StringUtils.isNotEmpty(details)) {
				errorObject.add("details", XmlEncodingUtils.replaceNonValidXmlCharacters(details));
			}
			addParams(exceptionParams, errorObject);

			addOriginalMessageObject(originalMessage, session, errorObject, messageId);

			errorObject.close();
			if (rootObjectBuilder != null) {
				rootObjectBuilder.close();
			}
			documentBuilder.close();
			return messageBuilder.build();
		} catch (IOException | SAXException e) {
			if (t != null) {
				e.addSuppressed(t);
			}
			throw new FormatterException("Cannot create formatted error message for error [" + errorMessage + "]", e);
		}
	}

	private void addOriginalMessageObject(@Nullable Message originalMessage, @Nonnull PipeLineSession session, @Nonnull ObjectBuilder errorObject, @Nullable String messageId) throws SAXException {
		INodeBuilder originalMessageNode = errorObject.addField(PipeLineSession.ORIGINAL_MESSAGE_KEY);
		ObjectBuilder originalMessageObject = originalMessageNode.startObject();

		originalMessageObject.addAttribute("messageId", messageId);
		Instant tsReceived = session.getTsReceived();
		if (tsReceived != null && tsReceived.toEpochMilli() != 0) {
			originalMessageObject.addAttribute("receivedTime", Date.from(tsReceived).toString());
		}
		String originalMessageAsString = getMessageAsString(originalMessage, messageId);
		if (messageFormat == DocumentFormat.XML) {
			originalMessageNode.setValue(originalMessageAsString);
		} else {
			originalMessageObject.add("message", originalMessageAsString);
		}
		originalMessageObject.close();
	}

	private void addParams(@Nonnull Map<String, Object> exceptionParams, @Nonnull ObjectBuilder errorObject) throws SAXException, IOException {
		if (!exceptionParams.isEmpty()) {
			// Sort the entries in the map by key, basically because it makes testing easier.
			Collection<Map.Entry<String, Object>> entries = exceptionParams.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.toList();

			if (messageFormat == DocumentFormat.XML) {
				ArrayBuilder paramsArray = errorObject.addArrayField("params", "param");
				for (Map.Entry<String, Object> entry : entries) {
					INodeBuilder paramNode = paramsArray.addElement();
					ObjectBuilder paramObject = paramNode.startObject();
					paramObject.addAttribute("name", entry.getKey());

					Object value = entry.getValue();
					if (value instanceof Message message) {
						paramNode.setValue(message.asString());
					} else {
						paramNode.setValue(value.toString());
					}
					paramObject.close();
				}
				paramsArray.close();
			} else {
				ObjectBuilder paramsObject = errorObject.addObjectField("params");
				for (Map.Entry<String, Object> entry : entries) {
					String key = entry.getKey();
					Object value = entry.getValue();
					if (value instanceof Message message) {
						paramsObject.add(key, message.asString());
					} else if (value instanceof Number number) {
						paramsObject.add(key, number);
					} else if (value instanceof Boolean bool) {
						paramsObject.add(key, bool);
					} else {
						paramsObject.add(key, entry.getValue().toString());
					}
				}
				paramsObject.close();
			}
		}
	}

	private static void addLocation(@Nullable HasName location, @Nonnull ObjectBuilder errorObject) throws SAXException {
		if (location != null) {
			ObjectBuilder locationObject = errorObject.addObjectField("location");
			locationObject.addAttribute("class", location.getClass().getName());
			locationObject.addAttribute("name", location.getName());
			locationObject.close();
		}
	}

	/**
	 * Get the location, either from a nested PipeRunException if present or from
	 * the location passed to the ErrorMessageFormatter.
	 */
	private static @Nullable HasName getLocation(@Nullable HasName location, Throwable t) {
		PipeRunException pre = extractPipeRunException(t);
		if (pre != null && pre.getPipeInError() != null) {
			return pre.getPipeInError();
		}
		return location;
	}

	/**
	 * Extract parameters from (nested) PipeRunException, or empty map.
	 */
	private static @Nonnull Map<String, Object> getPipeRunExceptionParams(@Nullable Throwable t) {
		PipeRunException pre = extractPipeRunException(t);
		if (pre == null) {
			return Map.of();
		}
		return pre.getParameters();
	}

	private static @Nullable PipeRunException extractPipeRunException(@Nullable Throwable t) {
		if (t == null) {
			return null;
		}
		if (t instanceof PipeRunException pre) {
			return pre;
		}
		return extractPipeRunException(t.getCause());
	}

	private @Nullable String getMessageAsString(@Nullable Message originalMessage, @Nullable String messageId) {
		String originalMessageAsString;
		try {
			originalMessageAsString = originalMessage != null ? originalMessage.asString() : null;
		} catch (IOException e) {
			log.warn("Could not convert originalMessage for messageId [{}]", messageId, e);
			originalMessageAsString = originalMessage.toString();
		}
		return originalMessageAsString;
	}

	protected @Nullable String getErrorMessage(@Nullable String message, @Nullable Throwable t) {
		if (t == null) {
			return message;
		}
		if (StringUtils.isEmpty(message)) {
			return t.getMessage();
		}
		return  message + ": "+t.getMessage();
	}

	/**
	 * Format the error message as XML or as JSON.
	 *
	 * ff.default XML
	 */
	public void setMessageFormat(@Nonnull DocumentFormat messageFormat) {
		this.messageFormat = messageFormat;
	}
}
