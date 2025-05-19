/*
   Copyright 2013 Nationale-Nederlanden, 2024 WeAreFrank!

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
package org.frankframework.extensions.bis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Some utilities for working with BIS.
 *
 * @author Peter Leeuwenburgh
 * @deprecated Please use BisWrapperPipe
 */
@Deprecated
public class BisUtils {
	protected Logger log = LogUtil.getLogger(this);

	private static final String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private static final String soapHeaderXPath = "soapenv:Envelope/soapenv:Header";
	private static final String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private static final String bisNamespaceDefs = "bis=http://www.ing.com/CSP/XSD/General/Message_2";
	private static final String messageHeaderConversationIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:ConversationId";
	private static final String messageHeaderExternalRefToMessageIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:MessageId";
	//resultInPayload=false (old)
	private static final String oldBisErrorXPath = "soapenv:Envelope/soapenv:Body/bis:Result/bis:Status='ERROR' or string-length(soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode)&gt;0";
	private static final String oldBisErrorListXPath = "soapenv:Envelope/soapenv:Body/bis:Result/bis:ErrorList";
	//resultInPayload=true
	private static final String bisErrorXPath = "soapenv:Envelope/soapenv:Body/*/bis:Result/bis:Status='ERROR' or string-length(soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode)&gt;0";
	private static final String bisErrorListXPath = "soapenv:Envelope/soapenv:Body/*/bis:Result/bis:ErrorList";

	private static final String[][] BISERRORS = { { "ERR6002", "Service Interface Request Time Out" }, {
			"ERR6003", "Invalid Request Message" }, {
			"ERR6004", "Invalid Backend system response" }, {
			"ERR6005", "Backend system failure response" }, {
			"ERR6999", "Unspecified Errors" }
	};

	private TransformerPool messageHeaderConversationIdTp;
	private TransformerPool messageHeaderExternalRefToMessageIdTp;
	private TransformerPool oldMessageHeaderConversationIdTp;
	private TransformerPool oldMessageHeaderExternalRefToMessageIdTp;

	private static BisUtils self = null;

	private void init() throws ConfigurationException {
		try {
			// messageHeaderInSoapBody=true (old)
			oldMessageHeaderConversationIdTp = TransformerPool.getXPathTransformerPool(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderConversationIdXPath, OutputType.TEXT);
			oldMessageHeaderExternalRefToMessageIdTp = TransformerPool.getXPathTransformerPool(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderExternalRefToMessageIdXPath, OutputType.TEXT);
			// messageHeaderInSoapBody=false
			messageHeaderConversationIdTp = TransformerPool.getXPathTransformerPool(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapHeaderXPath + "/" + messageHeaderConversationIdXPath, OutputType.TEXT);
			messageHeaderExternalRefToMessageIdTp = TransformerPool.getXPathTransformerPool(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapHeaderXPath + "/" + messageHeaderExternalRefToMessageIdXPath, OutputType.TEXT);
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create SOAP transformer", e);
		}
	}

	public static BisUtils getInstance() throws ConfigurationException {
		if (self == null) {
			self = new BisUtils();
			self.init();
		}
		return self;
	}

	public String prepareMessageHeader(String originalMessageText, boolean messageHeaderInSoapBody) throws SAXException, IOException, TransformerException {
		return prepareMessageHeader(originalMessageText, messageHeaderInSoapBody, null, null);
	}

	public String prepareMessageHeader(String originalMessageText, boolean messageHeaderInSoapBody, String conversationId, String externalRefToMessageId) throws SAXException, IOException, TransformerException {
		XmlBuilder messageHeaderElement = new XmlBuilder("MessageHeader");
		messageHeaderElement.addAttribute("xmlns", "http://www.ing.com/CSP/XSD/General/Message_2");
		XmlBuilder fromElement = new XmlBuilder("From");
		XmlBuilder idElement = new XmlBuilder("Id");
		idElement.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
		fromElement.addSubElement(idElement);
		messageHeaderElement.addSubElement(fromElement);
		XmlBuilder headerFieldsElement = new XmlBuilder("HeaderFields");
		XmlBuilder conversationIdElement = new XmlBuilder("ConversationId");
		if (originalMessageText == null) {
			conversationIdElement.setValue(conversationId);
		} else {
			if (messageHeaderInSoapBody) {
				if (oldMessageHeaderConversationIdTp != null) {
					conversationIdElement.setValue(oldMessageHeaderConversationIdTp.transformToString(originalMessageText, null, true));
				}
			} else {
				if (messageHeaderConversationIdTp != null) {
					conversationIdElement.setValue(messageHeaderConversationIdTp.transformToString(originalMessageText, null, true));
				}
			}
		}
		headerFieldsElement.addSubElement(conversationIdElement);
		XmlBuilder messageIdElement = new XmlBuilder("MessageId");
		messageIdElement.setValue(MessageUtils.generateMessageId("MSG"));
		headerFieldsElement.addSubElement(messageIdElement);
		XmlBuilder externalRefToMessageIdElement = new XmlBuilder("ExternalRefToMessageId");
		if (originalMessageText == null) {
			externalRefToMessageIdElement.setValue(externalRefToMessageId);
		} else {
			if (messageHeaderInSoapBody) {
				if (oldMessageHeaderExternalRefToMessageIdTp != null) {
					externalRefToMessageIdElement.setValue(oldMessageHeaderExternalRefToMessageIdTp.transformToString(originalMessageText, null, true));
				}
			} else {
				if (messageHeaderExternalRefToMessageIdTp != null) {
					externalRefToMessageIdElement.setValue(messageHeaderExternalRefToMessageIdTp.transformToString(originalMessageText, null, true));
				}
			}
		}
		headerFieldsElement.addSubElement(externalRefToMessageIdElement);
		XmlBuilder timestampElement = new XmlBuilder("Timestamp");
		timestampElement.setValue(DateFormatUtils.now(DateFormatUtils.FULL_ISO_FORMATTER));
		headerFieldsElement.addSubElement(timestampElement);
		messageHeaderElement.addSubElement(headerFieldsElement);
		return messageHeaderElement.asXmlString();
	}

	public String prepareResult(String errorCode, String errorText, String serviceName, String actionName, String detailText) {
		XmlBuilder resultElement = new XmlBuilder("Result");
		resultElement.addAttribute("xmlns", "http://www.ing.com/CSP/XSD/General/Message_2");
		XmlBuilder statusElement = new XmlBuilder("Status");
		if (errorCode == null) {
			statusElement.setValue("OK");
		} else {
			statusElement.setValue("ERROR");
		}
		resultElement.addSubElement(statusElement);
		if (errorCode != null) {
			XmlBuilder errorListElement = new XmlBuilder("ErrorList");
			XmlBuilder errorElement = new XmlBuilder("Error");
			XmlBuilder codeElement = new XmlBuilder("Code");
			codeElement.setValue(errorCode);
			errorElement.addSubElement(codeElement);
			XmlBuilder reasonElement = new XmlBuilder("Reason");
			reasonElement.setCdataValue(errorText);
			errorElement.addSubElement(reasonElement);
			XmlBuilder serviceElement = new XmlBuilder("Service");
			XmlBuilder serviceNameElement = new XmlBuilder("Name");
			serviceNameElement.setValue(serviceName);
			serviceElement.addSubElement(serviceNameElement);
			XmlBuilder serviceContextElement = new XmlBuilder("Context");
			serviceContextElement.setValue("1");
			serviceElement.addSubElement(serviceContextElement);
			XmlBuilder actionElement = new XmlBuilder("Action");
			XmlBuilder actionNameElement = new XmlBuilder("Name");
			actionNameElement.setValue(actionName);
			actionElement.addSubElement(actionNameElement);
			XmlBuilder actionVersionElement = new XmlBuilder("Version");
			actionVersionElement.setValue("1");
			actionElement.addSubElement(actionVersionElement);
			serviceElement.addSubElement(actionElement);
			errorElement.addSubElement(serviceElement);
			XmlBuilder detailListElement = new XmlBuilder("DetailList");
			XmlBuilder detailElement = new XmlBuilder("Detail");
			XmlBuilder detailCodeElement = new XmlBuilder("Code");
			detailElement.addSubElement(detailCodeElement);
			XmlBuilder detailTextElement = new XmlBuilder("Text");
			detailTextElement.setCdataValue(detailText);
			detailElement.addSubElement(detailTextElement);
			detailListElement.addSubElement(detailElement);
			errorElement.addSubElement(detailListElement);
			errorListElement.addSubElement(errorElement);
			resultElement.addSubElement(errorListElement);
		}
		return resultElement.asXmlString();
	}
	public Message prepareReply(Message rawReply, String messageHeader, String result, boolean resultInPayload) throws DomBuilderException, IOException, TransformerException {
		List<String> messages = new ArrayList<>();
		if (messageHeader != null) {
			messages.add(messageHeader);
		}
		messages.add(rawReply.asString());

		String payload = null;
		if (result == null) {
			payload = listToString(messages);
		} else {
			if (resultInPayload) {
				String message = listToString(messages);
				Document messageDoc = XmlUtils.buildDomDocument(message);
				Node messageRootNode = messageDoc.getFirstChild();
				Node resultNode = messageDoc.importNode(XmlUtils.buildNode(result), true);
				messageRootNode.appendChild(resultNode);
				payload = XmlUtils.nodeToString(messageDoc);
			} else {
				messages.add(result);
				payload = listToString(messages);
			}
		}
		return new Message(payload);
	}

	public static String listToString(List<String> list) {
		return String.join("", list);
	}

	public String errorCodeToText(String errorCode) {
		for (int i = 0; i < BISERRORS.length; i++) {
			if (errorCode.equals(BISERRORS[i][0])) {
				return BISERRORS[i][1];
			}
		}
		return null;
	}

	public String getSoapNamespaceDefs() {
		return soapNamespaceDefs;
	}

	public String getSoapBodyXPath() {
		return soapBodyXPath;
	}

	public String getBisNamespaceDefs() {
		return bisNamespaceDefs;
	}

	public String getBisErrorXPath() {
		return bisErrorXPath;
	}

	public String getOldBisErrorXPath() {
		return oldBisErrorXPath;
	}

	public String getBisErrorListXPath() {
		return bisErrorListXPath;
	}

	public String getOldBisErrorListXPath() {
		return oldBisErrorListXPath;
	}
}
