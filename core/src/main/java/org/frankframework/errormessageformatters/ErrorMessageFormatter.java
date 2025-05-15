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
import java.util.Date;

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
import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.IDocumentBuilder;
import org.frankframework.documentbuilder.INodeBuilder;
import org.frankframework.documentbuilder.ObjectBuilder;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlEncodingUtils;

/**
 * This class wraps an error in an XML string.
 * <p>
 * Sample xml:
 * <pre>{@code
 * <errorMessage>
 *    <message timestamp="Mon Oct 13 12:01:57 CEST 2003"
 *             originator="NN IOS AdapterFramework(set from 'application.name' and 'application.version')"
 *             message="message describing the error that occurred">
 *    <location class="org.frankframework.pipes.XmlSwitch" name="ServiceSwitch"/>
 *    <details>detailed information of the error</details>
 *    <originalMessage messageId="..." receivedTime="Mon Oct 27 12:10:18 CET 2003" >
 *        <![CDATA[contents of message for which the error occurred]]>
 *    </originalMessage>
 * </errorMessage>
 * }</pre>
 *
 * @author  Gerrit van Brakel
 */
@Log4j2
public class ErrorMessageFormatter implements IErrorMessageFormatter, IScopeProvider {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private @Getter @Nonnull DocumentFormat messageFormat = DocumentFormat.XML;

	/**
	 * Format the available parameters into a XML-message.
	 * <b/>
	 * Override this method in subclasses to obtain the required behaviour.
	 */
	@Override
	public Message format(String errorMessage, Throwable t, HasName location, Message originalMessage, String messageId, long receivedTime) {

		String details = null;
		errorMessage = getErrorMessage(errorMessage, t);
		if (t != null) {
			details = ExceptionUtils.getStackTrace(t);
		}
		String prefix=location!=null ? ClassUtils.nameOf(location) : null;
		if (StringUtils.isNotEmpty(messageId)) {
			prefix = StringUtil.concatStrings(prefix, " ", "msgId ["+messageId+"]");
		}
		errorMessage = StringUtil.concatStrings(prefix, ": ", errorMessage);

		String originator = AppConstants.getInstance().getProperty("application.name")+" "+ AppConstants.getInstance().getProperty("application.version");
		// Build a Base document
		try {
			MessageBuilder messageBuilder = new MessageBuilder();
			IDocumentBuilder documentBuilder = DocumentBuilderFactory.startDocument(messageFormat, "errorMessage", messageBuilder, true);

			ObjectBuilder errorXml = documentBuilder.asObjectBuilder();
			errorXml.addAttribute("timestamp", new Date().toString());
			errorXml.addAttribute("originator", originator);
			errorXml.addAttribute("message", XmlEncodingUtils.replaceNonValidXmlCharacters(errorMessage));

			if (location != null) {
				ObjectBuilder locationXml = errorXml.addObjectField("location");
				locationXml.addAttribute("class", location.getClass().getName());
				locationXml.addAttribute("name", location.getName());
				locationXml.close();
			}

			if (StringUtils.isNotEmpty(details)) {
				errorXml.add("details", XmlEncodingUtils.replaceNonValidXmlCharacters(details));
			}

			INodeBuilder nodeBuilder = errorXml.addField(PipeLineSession.ORIGINAL_MESSAGE_KEY);
			ObjectBuilder originalMessageXml = nodeBuilder.startObject();

			originalMessageXml.addAttribute("messageId", messageId);
			if (receivedTime != 0) {
				originalMessageXml.addAttribute("receivedTime", new Date(receivedTime).toString());
			}
			String originalMessageAsString = getMessageAsString(originalMessage, messageId);
			if (messageFormat == DocumentFormat.XML) {
				nodeBuilder.setValue(originalMessageAsString);
			} else {
				originalMessageXml.add("message", originalMessageAsString);
			}
			originalMessageXml.close();

			errorXml.close();
			documentBuilder.close();
			return messageBuilder.build();
		} catch (IOException | SAXException e) {
			if (t != null) {
				e.addSuppressed(t);
			}
			throw new RuntimeException("Cannot create formatted error message for error [" + errorMessage + "]", e);
		}
	}

	@Nullable
	private String getMessageAsString(Message originalMessage, String messageId) {
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
