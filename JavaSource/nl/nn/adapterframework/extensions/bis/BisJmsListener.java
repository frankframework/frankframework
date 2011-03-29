/*
 * $Log: BisJmsListener.java,v $
 * Revision 1.2  2011-03-29 13:01:48  m168309
 * cosmetic change
 *
 * Revision 1.1  2011/03/21 14:55:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.extensions.bis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Bis (Business Integration Services) extension of JmsListener.
 * <br/>
 * Example request:<br/><code><pre>
 *	&lt;soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"&gt;
 *		&lt;soap:Header/&gt;
 *		&lt;soap:Body&gt;
 *			&lt;bis:MessageHeader xmlns:bis="http://www.ing.com/CSP/XSD/General/Message_2"&gt;
 *				&lt;bis:From&gt;
 *					&lt;bis:Id&gt;PolicyConversion_01_ServiceAgents_01&lt;/bis:Id&gt;
 *				&lt;/bis:From&gt;
 *				&lt;bis:HeaderFields&gt;
 *					&lt;bis:ConversationId&gt;1790257_10000050_04&lt;/bis:ConversationId&gt;
 *					&lt;bis:MessageId&gt;1790257&lt;/bis:MessageId&gt;
 *					&lt;bis:Timestamp&gt;2011-03-02T10:26:31.464+01:00&lt;/bis:Timestamp&gt;
 *				&lt;/bis:HeaderFields&gt;
 *			&lt;/bis:MessageHeader&gt;
 *			<i>&lt;pcr:GetRequest xmlns:pcr="http://www.ing.com/nl/pcretail/ts/migrationauditdata_01"&gt;
 *				&lt;pcr:PolicyDetails&gt;
 *					&lt;pcr:RVS_PARTY_ID&gt;1790257&lt;/pcr:RVS_PARTY_ID&gt;
 *					&lt;pcr:RVS_POLICY_NUMBER&gt;10000050&lt;/pcr:RVS_POLICY_NUMBER&gt;
 *					&lt;pcr:RVS_BRANCH_CODE&gt;04&lt;/pcr:RVS_BRANCH_CODE&gt;
 *				&lt;/pcr:PolicyDetails&gt;
 *			&lt;/pcr:GetRequest&gt;</i>
 *		&lt;/soap:Body&gt;
 *	&lt;/soap:Envelope&gt;
 * </pre></code><br/>
 * The element MessageHeader in the soap body is mandatory.
 * <br/>
 * Example reply:<br/><code><pre>
 *	&lt;soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"&gt;
 *		&lt;soap:Header/&gt;
 *		&lt;soap:Body&gt;
 *			&lt;bis:MessageHeader xmlns:bis="http://www.ing.com/CSP/XSD/General/Message_2"&gt;
 *				&lt;bis:From&gt;
 *					&lt;bis:Id&gt;IJA_DB4CONV&lt;/bis:Id&gt;
 *				&lt;/bis:From&gt;
 *				&lt;bis:HeaderFields&gt;
 *					&lt;bis:ConversationId&gt;1790257_10000050_04&lt;/bis:ConversationId&gt;
 *					&lt;bis:MessageId&gt;rn09ce_0a3b8d2d--33192359_12e588118c1_-612f&lt;/bis:MessageId&gt;
 *					&lt;bis:ExternalRefToMessageId&gt;1790257&lt;/bis:ExternalRefToMessageId&gt;
 *					&lt;bis:Timestamp&gt;2011-03-02T10:26:31&lt;/bis:Timestamp&gt;
 *				&lt;/bis:HeaderFields&gt;
 *			&lt;/bis:MessageHeader&gt;
 *			<i>&lt;pcr:GetResponse xmlns:pcr="http://www.ing.com/nl/pcretail/ts/migrationauditdata_01"/&gt;</i>
 *			&lt;bis:Result xmlns:bis="http://www.ing.com/CSP/XSD/General/Message_2"&gt;
 *				&lt;bis:Status&gt;OK&lt;/bis:Status&gt;
 *			&lt;/bis:Result&gt;
 *		&lt;/soap:Body&gt;
 *	&lt;/soap:Envelope&gt;
 * </pre></code><br/>
 * The elements MessageHeader and Result in the soap body are mandatory.
 * <br/>
 * Example element Result in case of an error reply:<br/><code><pre>
 *	&lt;bis:Result xmlns:bis="http://www.ing.com/CSP/XSD/General/Message_2"&gt;
 *		&lt;bis:Status&gt;ERROR&lt;/bis:Status&gt;
 *		&lt;bis:ErrorList&gt;
 *			&lt;bis:Error&gt;
 *				&lt;bis:Code&gt;102&lt;/bis:Code&gt;
 *				&lt;bis:Reason&gt;Message not according to XSD definition&lt;/bis:Reason&gt;
 *				&lt;bis:Service&gt;
 *					&lt;bis:Name&gt;migrationauditdata_01&lt;/bis:Name&gt;
 *					&lt;bis:Context&gt;1&lt;/bis:Context&gt;
 *					&lt;bis:Action&gt;
 *						&lt;bis:Name&gt;SetPolicyDetails_Action&lt;/bis:Name&gt;
 *						&lt;bis:Version&gt;1&lt;/bis:Version&gt;
 *					&lt;/bis:Action&gt;
 *				&lt;/bis:Service&gt;
 *				&lt;bis:DetailList&gt;
 *					&lt;bis:Detail&gt;
 *						&lt;bis:Code/&gt;
 *						&lt;bis:Text&gt;Pipe [Validate tibco request] msgId [Test Tool correlation id] got invalid xml according to schema [....&lt;/bis:Text&gt;
 *					&lt;/bis:Detail&gt;
 *				&lt;/bis:DetailList&gt;
 *			&lt;/bis:Error&gt;
 *		&lt;/bis:ErrorList&gt;
 *	&lt;/bis:Result&gt;
 * </pre></code><br/>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.bis.BisSoapJmsListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoap(boolean) soap}</td><td>when <code>true</code>, messages sent are put in a SOAP envelope</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setRequestXPath(String) requestXPath}</td><td>xpath expression to extract the message which is passed to the pipeline. When soap=true the initial message is the content of the soap body</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRequestNamespaceDefs(String) requestNamespaceDefs}</td><td>namespace defintions for requestXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setErrorCodeSessionKey(String) errorCodeSessionKey}</td><td>key of session variable to store error code in (if an error occurs)</td><td>bisErrorCode</td></tr>
 * <tr><td>{@link #setErrorTextSessionKey(String) errorTextSessionKey}</td><td>key of session variable to store error text in (if an error occurs). If not specified, the following error text is derived from the error code: 
 *   <table border="1">
 *   <tr><th>errorCode</th><th>errorText</th></tr>
 *   <tr><td>ERR6002</td><td>Service Interface Request Time Out</td></tr>
 *   <tr><td>ERR6003</td><td>Invalid Request Message</td></tr>
 *   <tr><td>ERR6004</td><td>Invalid Backend system response</td></tr>
 *   <tr><td>ERR6005</td><td>Backend system failure response</td></tr>
 *   <tr><td>ERR6999</td><td>Unspecified Errors</td></tr>
 *  </table></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setErrorReasonKey(String) errorReasonSessionKey}</td><td>key of session variable to store error reason in (if an error occurs)</td><td>bisErrorReason</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>name of the service; used in the error reply</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setActionName(String) actionName}</td><td>name of the operation; used in the error reply</td><td>&nbsp;</td></tr>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class BisJmsListener extends JmsListener {

	private final static String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private final static String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private final static String bisNamespaceDefs = "bis=http://www.ing.com/CSP/XSD/General/Message_2";
	private final static String messageHeaderConversationIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:ConversationId";
	private final static String messageHeaderExternalRefToMessageIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:MessageId";

	private final static String[][] BISERRORS = { { "ERR6002", "Service Interface Request Time Out" }, {
			"ERR6003", "Invalid Request Message" }, {
			"ERR6004", "Invalid Backend system response" }, {
			"ERR6005", "Backend system failure response" }, {
			"ERR6999", "Unspecified Errors" }
	};

	private String requestXPath = null;
	private String requestNamespaceDefs = null;
	private String errorCodeSessionKey = "bisErrorCode";
	private String errorTextSessionKey = null;
	private String errorReasonSessionKey = "bisErrorReason";
	private String serviceName = null;
	private String actionName = null;

	private TransformerPool requestTp;
	private TransformerPool messageHeaderConversationIdTp;
	private TransformerPool messageHeaderExternalRefToMessageIdTp;

	public BisJmsListener() {
		setSoap(true);
	}

	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSoap()) {
			throw new ConfigurationException(getLogPrefix() + "soap must be true");
		}
		try {
			messageHeaderConversationIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderConversationIdXPath, "text"));
			messageHeaderExternalRefToMessageIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, soapBodyXPath + "/" + messageHeaderExternalRefToMessageIdXPath, "text"));
			if (StringUtils.isNotEmpty(getRequestXPath())) {
				requestTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + getRequestNamespaceDefs(), soapBodyXPath + "/" + getRequestXPath(), "xml"));
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot create transformer", e);
		}
	}

	public String extractMessageBody(String rawMessageText, Map context, SoapWrapper soapWrapper) throws DomBuilderException, TransformerException, IOException {
		context.put("messageText", rawMessageText);
		return requestTp.transform(rawMessageText, null, true);
	}

	public String prepareReply(String rawReply, Map threadContext) throws ListenerException {
		String originalSoapBody = (String) threadContext.get("messageText");
		ArrayList messages = new ArrayList();
		String errorCode = null;
		if (StringUtils.isNotEmpty(getErrorCodeSessionKey())) {
			errorCode = (String) threadContext.get(getErrorCodeSessionKey());
		}
		try {
			messages.add(prepareMessageHeader(originalSoapBody));
			messages.add(rawReply);
			messages.add(prepareResult(errorCode, threadContext));
		} catch (Exception e) {
			throw new ListenerException(e);
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < messages.size(); i++) {
			sb.append((String) messages.get(i));
		}
		return super.prepareReply(sb.toString(), threadContext);
	}

	public String prepareMessageHeader(String originalSoapBody) throws DomBuilderException, IOException, TransformerException {
		XmlBuilder messageHeader = new XmlBuilder("MessageHeader");
		messageHeader.addAttribute("xmlns", "http://www.ing.com/CSP/XSD/General/Message_2");
		XmlBuilder from = new XmlBuilder("From");
		XmlBuilder id = new XmlBuilder("Id");
		id.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
		from.addSubElement(id);
		messageHeader.addSubElement(from);
		XmlBuilder headerFields = new XmlBuilder("HeaderFields");
		XmlBuilder conversationId = new XmlBuilder("ConversationId");
		if (messageHeaderConversationIdTp != null) {
			conversationId.setValue(messageHeaderConversationIdTp.transform(originalSoapBody, null, true));
		} else {
			conversationId.setValue("?");
		}
		headerFields.addSubElement(conversationId);
		XmlBuilder messageId = new XmlBuilder("MessageId");
		messageId.setValue(Misc.getHostname() + "_" + Misc.createSimpleUUID());
		headerFields.addSubElement(messageId);
		XmlBuilder externalRefToMessageId = new XmlBuilder("ExternalRefToMessageId");
		if (messageHeaderExternalRefToMessageIdTp != null) {
			externalRefToMessageId.setValue(messageHeaderExternalRefToMessageIdTp.transform(originalSoapBody, null, true));
		} else {
			externalRefToMessageId.setValue("?");
		}
		headerFields.addSubElement(externalRefToMessageId);
		XmlBuilder timestamp = new XmlBuilder("Timestamp");
		timestamp.setValue(DateUtils.format(new Date(), "yyyy-MM-dd'T'HH:mm:ss"));
		headerFields.addSubElement(timestamp);
		messageHeader.addSubElement(headerFields);
		return messageHeader.toXML();
	}

	public String prepareResult(String errorCode, Map threadContext) throws DomBuilderException, IOException, TransformerException {
		XmlBuilder result = new XmlBuilder("Result");
		result.addAttribute("xmlns", "http://www.ing.com/CSP/XSD/General/Message_2");
		XmlBuilder status = new XmlBuilder("Status");
		if (errorCode == null) {
			status.setValue("OK");
		} else {
			status.setValue("ERROR");
		}
		result.addSubElement(status);
		if (errorCode != null) {
			XmlBuilder errorList = new XmlBuilder("ErrorList");
			XmlBuilder error = new XmlBuilder("Error");
			XmlBuilder code = new XmlBuilder("Code");
			code.setValue(errorCode);
			error.addSubElement(code);
			XmlBuilder reason = new XmlBuilder("Reason");
			if (StringUtils.isNotEmpty(getErrorTextSessionKey())) {
				String et = (String) threadContext.get(getErrorTextSessionKey());
				if (et != null) {
					reason.setCdataValue(et);
				}
			} else {
				String errorText = errorCodeToText(errorCode);
				if (errorText != null) {
					reason.setValue(errorText);
				}
			}
			error.addSubElement(reason);
			XmlBuilder service = new XmlBuilder("Service");
			XmlBuilder serviceName = new XmlBuilder("Name");
			if (StringUtils.isNotEmpty(getServiceName())) {
				String sn = (String) threadContext.get(getServiceName());
				if (sn != null) {
					serviceName.setValue(sn);
				}
			}
			service.addSubElement(serviceName);
			XmlBuilder serviceContext = new XmlBuilder("Context");
			serviceContext.setValue("1");
			service.addSubElement(serviceContext);
			XmlBuilder action = new XmlBuilder("Action");
			XmlBuilder actionName = new XmlBuilder("Name");
			if (StringUtils.isNotEmpty(getActionName())) {
				String an = (String) threadContext.get(getActionName());
				if (an != null) {
					actionName.setValue(an);
				}
			}
			action.addSubElement(actionName);
			XmlBuilder actionVersion = new XmlBuilder("Version");
			actionVersion.setValue("1");
			action.addSubElement(actionVersion);
			service.addSubElement(action);
			error.addSubElement(service);
			XmlBuilder detailList = new XmlBuilder("DetailList");
			XmlBuilder detail = new XmlBuilder("Detail");
			XmlBuilder detailCode = new XmlBuilder("Code");
			detail.addSubElement(detailCode);
			XmlBuilder detailText = new XmlBuilder("Text");
			if (StringUtils.isNotEmpty(getErrorReasonSessionKey())) {
				String er = (String) threadContext.get(getErrorReasonSessionKey());
				if (er != null) {
					detailText.setCdataValue(er);
				}
			}
			detail.addSubElement(detailText);
			detailList.addSubElement(detail);
			error.addSubElement(detailList);
			errorList.addSubElement(error);
			result.addSubElement(errorList);
		}
		return result.toXML();
	}

	private String errorCodeToText(String errorCode) {
		for (int i = 0; i < BISERRORS.length; i++) {
			if (errorCode.equals(BISERRORS[i][0])) {
				return BISERRORS[i][1];
			}
		}
		return null;
	}

	public void setRequestXPath(String requestXPath) {
		this.requestXPath = requestXPath;
	}

	public String getRequestXPath() {
		return requestXPath;
	}

	public void setRequestNamespaceDefs(String requestNamespaceDefs) {
		this.requestNamespaceDefs = requestNamespaceDefs;
	}

	public String getRequestNamespaceDefs() {
		return requestNamespaceDefs;
	}

	public void setErrorCodeSessionKey(String errorCodeSessionKey) {
		this.errorCodeSessionKey = errorCodeSessionKey;
	}

	public String getErrorCodeSessionKey() {
		return errorCodeSessionKey;
	}

	public void setErrorTextSessionKey(String errorTextSessionKey) {
		this.errorTextSessionKey = errorTextSessionKey;
	}

	public String getErrorTextSessionKey() {
		return errorTextSessionKey;
	}

	public void setErrorReasonSessionKey(String errorReasonSessionKey) {
		this.errorReasonSessionKey = errorReasonSessionKey;
	}

	public String getErrorReasonSessionKey() {
		return errorReasonSessionKey;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public String getActionName() {
		return actionName;
	}
}
