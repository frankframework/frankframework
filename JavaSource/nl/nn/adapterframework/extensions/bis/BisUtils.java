/*
 * $Log: BisUtils.java,v $
 * Revision 1.4  2011-09-22 14:17:07  europe\m168309
 * Deprecated BisJmsSender/BisJmsListener
 *
 * Revision 1.3  2011/09/12 07:23:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * BisJmsSender/BisJmsListener: added functionality on behalf of DINN (migration from IFSA to TIBCO)
 *
 * Revision 1.2  2011/06/06 12:27:26  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * BisJmsSender/BisJmsListener: added messageHeaderInSoapBody attribute
 *
 * Revision 1.1  2011/03/30 14:48:57  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved prepareMessageHeader() and prepareResult() to BisUtils
 *
 */
package nl.nn.adapterframework.extensions.bis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Some utilities for working with BIS. 
 * 
 * @author Peter Leeuwenburgh
 * @version Id
 * @deprecated Please use BisWrapperPipe
 */

public class BisUtils {
	public static final String version = "$RCSfile: BisUtils.java,v $ $Revision: 1.4 $ $Date: 2011-09-22 14:17:07 $";
	protected Logger log = LogUtil.getLogger(this);

	private final static String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private final static String soapHeaderXPath = "soapenv:Envelope/soapenv:Header";
	private final static String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private final static String bisNamespaceDefs = "bis=http://www.ing.com/CSP/XSD/General/Message_2";
	private final static String messageHeaderConversationIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:ConversationId";
	private final static String messageHeaderExternalRefToMessageIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:MessageId";
	//resultInPayload=false (old)
	private final static String oldBisErrorXPath = "soapenv:Envelope/soapenv:Body/bis:Result/bis:Status='ERROR' or string-length(soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode)&gt;0";
	private final static String oldBisErrorListXPath = "soapenv:Envelope/soapenv:Body/bis:Result/bis:ErrorList";
	//resultInPayload=true
	private final static String bisErrorXPath = "soapenv:Envelope/soapenv:Body/*/bis:Result/bis:Status='ERROR' or string-length(soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode)&gt;0";
	private final static String bisErrorListXPath = "soapenv:Envelope/soapenv:Body/*/bis:Result/bis:ErrorList";

	private final static String[][] BISERRORS = { { "ERR6002", "Service Interface Request Time Out" }, {
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
			oldMessageHeaderConversationIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderConversationIdXPath, "text"));
			oldMessageHeaderExternalRefToMessageIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderExternalRefToMessageIdXPath, "text"));
			// messageHeaderInSoapBody=false
			messageHeaderConversationIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapHeaderXPath + "/" + messageHeaderConversationIdXPath, "text"));
			messageHeaderExternalRefToMessageIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapHeaderXPath + "/" + messageHeaderExternalRefToMessageIdXPath, "text"));
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

	public String prepareMessageHeader(String originalMessageText, boolean messageHeaderInSoapBody) throws DomBuilderException, IOException, TransformerException {
		return prepareMessageHeader(originalMessageText, messageHeaderInSoapBody, null, null);
	}

	public String prepareMessageHeader(String originalMessageText, boolean messageHeaderInSoapBody, String conversationId, String externalRefToMessageId) throws DomBuilderException, IOException, TransformerException {
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
					conversationIdElement.setValue(oldMessageHeaderConversationIdTp.transform(originalMessageText, null, true));
				}
			} else {
				if (messageHeaderConversationIdTp != null) {
					conversationIdElement.setValue(messageHeaderConversationIdTp.transform(originalMessageText, null, true));
				}
			}
		}
		headerFieldsElement.addSubElement(conversationIdElement);
		XmlBuilder messageIdElement = new XmlBuilder("MessageId");
		messageIdElement.setValue(Misc.getHostname() + "_" + Misc.createSimpleUUID());
		headerFieldsElement.addSubElement(messageIdElement);
		XmlBuilder externalRefToMessageIdElement = new XmlBuilder("ExternalRefToMessageId");
		if (originalMessageText == null) {
			externalRefToMessageIdElement.setValue(externalRefToMessageId);
		} else {
			if (messageHeaderInSoapBody) {
				if (oldMessageHeaderExternalRefToMessageIdTp != null) {
					externalRefToMessageIdElement.setValue(oldMessageHeaderExternalRefToMessageIdTp.transform(originalMessageText, null, true));
				}
			} else {
				if (messageHeaderExternalRefToMessageIdTp != null) {
					externalRefToMessageIdElement.setValue(messageHeaderExternalRefToMessageIdTp.transform(originalMessageText, null, true));
				}
			}
		}
		headerFieldsElement.addSubElement(externalRefToMessageIdElement);
		XmlBuilder timestampElement = new XmlBuilder("Timestamp");
		timestampElement.setValue(DateUtils.format(new Date(), "yyyy-MM-dd'T'HH:mm:ss"));
		headerFieldsElement.addSubElement(timestampElement);
		messageHeaderElement.addSubElement(headerFieldsElement);
		return messageHeaderElement.toXML();
	}

	public String prepareResult(String errorCode, String errorText, String serviceName, String actionName, String detailText) throws DomBuilderException, IOException, TransformerException {
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
		return resultElement.toXML();
	}
	public String prepareReply(String rawReply, String messageHeader, String result, boolean resultInPayload) throws DomBuilderException, IOException, TransformerException {
		ArrayList messages = new ArrayList();
		if (messageHeader != null) {
			messages.add(messageHeader);
		}
		messages.add(rawReply);

		String payload = null;
		if (result == null) {
			payload = Misc.listToString(messages);
		} else {
			if (resultInPayload) {
				String message = Misc.listToString(messages);
				Document messageDoc = XmlUtils.buildDomDocument(message);
				Node messageRootNode = messageDoc.getFirstChild();
				Node resultNode = messageDoc.importNode(XmlUtils.buildNode(result), true);
				messageRootNode.appendChild(resultNode);
				payload = XmlUtils.nodeToString(messageDoc);
			} else {
				messages.add(result);
				payload = Misc.listToString(messages);
			}
		}
		return payload;
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
