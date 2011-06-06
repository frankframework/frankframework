/*
 * $Log: BisUtils.java,v $
 * Revision 1.2  2011-06-06 12:27:26  m168309
 * BisJmsSender/BisJmsListener: added messageHeaderInSoapBody attribute
 *
 * Revision 1.1  2011/03/30 14:48:57  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved prepareMessageHeader() and prepareResult() to BisUtils
 *
 */
package nl.nn.adapterframework.extensions.bis;

import java.io.IOException;
import java.util.Date;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Some utilities for working with BIS. 
 * 
 * @author Peter Leeuwenburgh
 * @version Id
 */

public class BisUtils {
	public static final String version = "$RCSfile: BisUtils.java,v $ $Revision: 1.2 $ $Date: 2011-06-06 12:27:26 $";

	private final static String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private final static String soapHeaderXPath = "soapenv:Envelope/soapenv:Header";
	private final static String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private final static String bisNamespaceDefs = "bis=http://www.ing.com/CSP/XSD/General/Message_2";
	private final static String messageHeaderConversationIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:ConversationId";
	private final static String messageHeaderExternalRefToMessageIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:MessageId";

	private TransformerPool messageHeaderConversationIdTp;
	private TransformerPool messageHeaderExternalRefToMessageIdTp;
	private TransformerPool messageHeaderOldConversationIdTp;
	private TransformerPool messageHeaderOldExternalRefToMessageIdTp;

	private static BisUtils self = null;

	private void init() throws ConfigurationException {
		try {
			messageHeaderOldConversationIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderConversationIdXPath, "text"));
			messageHeaderOldExternalRefToMessageIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderExternalRefToMessageIdXPath, "text"));
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

	public String prepareMessageHeader(String originalSoapBody, boolean messageHeaderInSoapBody) throws DomBuilderException, IOException, TransformerException {
		return prepareMessageHeader(originalSoapBody, messageHeaderInSoapBody, null, null);
	}

	public String prepareMessageHeader(String originalSoapBody, boolean messageHeaderInSoapBody, String conversationId, String externalRefToMessageId) throws DomBuilderException, IOException, TransformerException {
		XmlBuilder messageHeaderElement = new XmlBuilder("MessageHeader");
		messageHeaderElement.addAttribute("xmlns", "http://www.ing.com/CSP/XSD/General/Message_2");
		XmlBuilder fromElement = new XmlBuilder("From");
		XmlBuilder idElement = new XmlBuilder("Id");
		idElement.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
		fromElement.addSubElement(idElement);
		messageHeaderElement.addSubElement(fromElement);
		XmlBuilder headerFieldsElement = new XmlBuilder("HeaderFields");
		XmlBuilder conversationIdElement = new XmlBuilder("ConversationId");
		if (originalSoapBody == null) {
			conversationIdElement.setValue(conversationId);
		} else {
			if (messageHeaderInSoapBody) {
				if (messageHeaderOldConversationIdTp != null) {
					conversationIdElement.setValue(messageHeaderOldConversationIdTp.transform(originalSoapBody, null, true));
				}
			} else {
				if (messageHeaderConversationIdTp != null) {
					conversationIdElement.setValue(messageHeaderConversationIdTp.transform(originalSoapBody, null, true));
				}
			}
		}
		headerFieldsElement.addSubElement(conversationIdElement);
		XmlBuilder messageIdElement = new XmlBuilder("MessageId");
		messageIdElement.setValue(Misc.getHostname() + "_" + Misc.createSimpleUUID());
		headerFieldsElement.addSubElement(messageIdElement);
		XmlBuilder externalRefToMessageIdElement = new XmlBuilder("ExternalRefToMessageId");
		if (originalSoapBody == null) {
			externalRefToMessageIdElement.setValue(externalRefToMessageId);
		} else {
			if (messageHeaderInSoapBody) {
				if (messageHeaderOldExternalRefToMessageIdTp != null) {
					externalRefToMessageIdElement.setValue(messageHeaderOldExternalRefToMessageIdTp.transform(originalSoapBody, null, true));
				}
			} else {
				if (messageHeaderExternalRefToMessageIdTp != null) {
					externalRefToMessageIdElement.setValue(messageHeaderExternalRefToMessageIdTp.transform(originalSoapBody, null, true));
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
}
