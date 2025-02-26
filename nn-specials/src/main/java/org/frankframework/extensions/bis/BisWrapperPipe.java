/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2024 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.soap.SoapWrapperPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.Misc;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.UUIDUtil;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Pipe to wrap or unwrap a message conformable to the BIS (Business Integration Services) standard.
 * <p>
 * Example request in case of bis provider:<br/><code><pre>
 *	&lt;soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"&gt;
 *		&lt;soap:Header&gt;
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
 *		&lt;/soap:Header&gt;
 *		&lt;soap:Body&gt;
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
 * The element MessageHeader in the soap header is mandatory.
 * </p><p>
 * Example response in case of bis requester:<br/><code><pre>
 *	&lt;soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"&gt;
 *		&lt;soap:Header&gt;
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
 *		&lt;/soap:Header&gt;
 *		&lt;soap:Body&gt;
 *			<i>&lt;GetResponse xmlns="http://www.ing.com/nl/pcretail/ts/migrationcasedata_01"&gt;</i>
 *				<i>&lt;CaseData&gt;...&lt;/CaseData&gt;</i>
 *				&lt;bis:Result xmlns:bis="http://www.ing.com/CSP/XSD/General/Message_2"&gt;
 *					&lt;bis:Status&gt;OK&lt;/bis:Status&gt;
 *				&lt;/bis:Result&gt;
 *			<i>&lt;/GetResponse&gt;</i>
 *		&lt;/soap:Body&gt;
 *	&lt;/soap:Envelope&gt;
 * </pre></code><br/>
 * The elements MessageHeader in the soap header and Result in the soap body are mandatory.
 * </p><p>
 * Example element Result in case of an error response:<br/><code><pre>
 *	&lt;bis:Result xmlns:bis="http://www.ing.com/CSP/XSD/General/Message_2"&gt;
 *		&lt;bis:Status&gt;ERROR&lt;/bis:Status&gt;
 *		&lt;bis:ErrorList&gt;
 *			&lt;bis:Error&gt;
 *				&lt;bis:Code&gt;ERR6003&lt;/bis:Code&gt;
 *				&lt;bis:Reason&gt;Invalid Request Message&lt;/bis:Reason&gt;
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
 * </pre></code>
 * </p><p>
 * If direction=unwrap and one of the following conditions is true a PipeRunException is thrown:
 * <ul><li>Result/Status in the response soap body equals 'ERROR'</li>
 * <li>faultcode in the response soap fault is not empty</li></ul>
 * </p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(Direction) direction}</td><td>either <code>wrap</code> or <code>unwrap</code></td><td>wrap</td></tr>
 * <tr><td>{@link #setInputXPath(String) inputXPath}</td><td>(only used when direction=unwrap) xpath expression to extract the message which is returned. The initial message is the content of the soap body. If empty, the content of the soap body is passed (without the root body)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputNamespaceDefs(String) inputNamespaceDefs}</td><td>(only used when direction=unwrap) namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBisMessageHeaderInSoapBody(boolean) bisMessageHeaderInSoapBody}</td><td>when <code>true</code>, the bis message header is put in the SOAP body instead of in the SOAP header (first one is the old bis standard)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setBisMessageHeaderSessionKey(String) bisMessageHeaderSessionKey}</td><td>
 * <table>
 * <tr><td><code>direction=unwrap</code></td><td>name of the session key to store the bis message header from the request in</td></tr>
 * <tr><td><code>direction=wrap</code></td><td>name of the session key the original bis message header from the request is stored in; used to create the bis message header for the response</td></tr>
 * </table>
 * </td><td>bisMessageHeader</td></tr>
 * <tr><td>{@link #setBisResultInPayload(boolean) bisResultInPayload}</td><td>when <code>true</code>, the bis result is put in the payload (as last child in root tag) instead of in the SOAP body as sibling of the payload (last one is the old bis standard)</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setBisConversationIdSessionKey(String) bisConversationIdSessionKey}</td><td>(only used when direction=wrap and the original bis message header from the request doesn't exist) key of session variable to retrieve ConversationId for the bis message header from</td><td>bisConversationId</td></tr>
 * <tr><td>{@link #setBisExternalRefToMessageIdSessionKey(String) bisExternalRefToMessageIdSessionKey}</td><td>(only used when direction=wrap and the original bis message header from the request doesn't exist) key of session variable to retrieve ExternalRefToMessageId for the bis message header from</td><td>bisExternalRefToMessageId</td></tr>
 * <tr><td>{@link #setBisErrorCodeSessionKey(String) bisErrorCodeSessionKey}</td><td>(only used when direction=wrap) key of session variable to store bis error code in (if an error occurs)</td><td>bisErrorCode</td></tr>
 * <tr><td>{@link #setBisErrorTextSessionKey(String) bisErrorTextSessionKey}</td><td>(only used when direction=wrap) key of session variable to store bis error text in (if an error occurs). If not specified or no value retrieved, the following error text is derived from the error code:
 *   <table border="1">
 *   <tr><th>errorCode</th><th>errorText</th></tr>
 *   <tr><td>ERR6002</td><td>Service Interface Request Time Out</td></tr>
 *   <tr><td>ERR6003</td><td>Invalid Request Message</td></tr>
 *   <tr><td>ERR6004</td><td>Invalid Backend system response</td></tr>
 *   <tr><td>ERR6005</td><td>Backend system failure response</td></tr>
 *   <tr><td>ERR6999</td><td>Unspecified Errors</td></tr>
 *  </table></td><td>bisErrorText</td></tr>
 * <tr><td>{@link #setBisErrorReasonSessionKey(String) bisErrorReasonSessionKey}</td><td>(only used when direction=wrap and an error occurs) key of session variable to store bis error reason in</td><td>bisErrorReason</td></tr>
 * <tr><td>{@link #setOutputRoot(String) outputRoot}</td><td>(only used when direction=wrap and an error occurs) name of output root element in the SOAP body. If empty, the input message is used in the response</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputNamespace(String) outputNamespace}</td><td>(only used when direction=wrap and an error occurs) namespace of the output root element in the SOAP body</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBisServiceName(String) bisServiceName}</td><td>(only used when direction=wrap) name of the bis service; used in the bis error response</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBisActionName(String) bisActionName}</td><td>(only used when direction=wrap) name of the bis operation; used in the bis error response</td><td>&nbsp;</td></tr>
 * </table></p>
 * <p><b>The following attributes are created for the purpose of the migration from IFSA to TIBCO (and will be removed afterwards):</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setRemoveOutputNamespaces(boolean) removeOutputNamespaces}</td><td>(only used when direction=unwrap) when set <code>true</code> namespaces (and prefixes) in the output are removed</td><td>false</td></tr>
 * <tr><td>{@link #setOmitResult(boolean) omitResult}</td><td>(only used when direction=wrap) when <code>true</code>, the Result is omitted and instead of Result/Status 'ERROR' a PipeRunException is thrown</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setAddOutputNamespace(boolean) addOutputNamespace}</td><td>(only used when direction=unwrap) when set <code>true</code> the <code>outputNamespace</code> is added to the output root element in the SOAP body</td><td>false</td></tr>
 * </table></p>
 * @author Peter Leeuwenburgh
 * @deprecated Please replace with org.frankframework.extensions.esb.EsbSoapWrapperPipe (not 1:1)
 */
@Deprecated
@ConfigurationWarning("Please change to EsbSoapWrapperPipe")
public class BisWrapperPipe extends SoapWrapperPipe {
	private static final String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private static final String soapHeaderXPath = "soapenv:Envelope/soapenv:Header";
	private static final String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private static final String soapErrorXPath = "soapenv:Fault/faultcode";
	private static final String bisNamespaceDefs = "bis=http://www.ing.com/CSP/XSD/General/Message_2";
	private static final String bisMessageHeaderXPath = "bis:MessageHeader";
	private static final String bisMessageHeaderConversationIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:ConversationId";
	private static final String bisMessageHeaderExternalRefToMessageIdXPath = "bis:MessageHeader/bis:HeaderFields/bis:MessageId";
	private static final String bisErrorXPath = "bis:Result/bis:Status='ERROR'";

	private static final String[][] BISERRORS = { { "ERR6002", "Service Interface Request Time Out" }, {
			"ERR6003", "Invalid Request Message" }, {
			"ERR6004", "Invalid Backend system response" }, {
			"ERR6005", "Backend system failure response" }, {
			"ERR6999", "Unspecified Errors" }
	};

	private String inputXPath = null;
	private String inputNamespaceDefs = null;
	private String outputRoot = null;
	private String outputNamespace = null;
	private boolean bisMessageHeaderInSoapBody = false;
	private String bisMessageHeaderSessionKey = "bisMessageHeader";
	private boolean bisResultInPayload = true;
	private String bisConversationIdSessionKey = "bisConversationId";
	private String bisExternalRefToMessageIdSessionKey = "bisExternalRefToMessageId";
	private String bisErrorCodeSessionKey = "bisErrorCode";
	private String bisErrorTextSessionKey = "bisErrorText";
	private String bisErrorReasonSessionKey = "bisErrorReason";
	private String bisServiceName = null;
	private String bisActionName = null;
	private boolean removeOutputNamespaces = false;
	private boolean omitResult = false;
	private boolean addOutputNamespace = false;

	private TransformerPool bodyMessageTp;
	private TransformerPool bisMessageHeaderTp;
	private TransformerPool bisMessageHeaderConversationIdTp;
	private TransformerPool bisMessageHeaderExternalRefToMessageIdTp;
	private TransformerPool bisErrorTp;
	private String bisErrorXe;
	private TransformerPool addOutputNamespaceTp;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
			throw new ConfigurationException("soapHeaderSessionKey is not allowed");
		}
		if (StringUtils.isEmpty(getBisMessageHeaderSessionKey())) {
			throw new ConfigurationException("messageHeaderSessionKey must be set");
		}
		if (isAddOutputNamespace() && StringUtils.isEmpty(outputNamespace)) {
			throw new ConfigurationException("outputNamespace must be set when addOutputnamespace=true");
		}
		try {
			if (StringUtils.isNotEmpty(getInputXPath())) {
				String bodyMessageNd = StringUtils.isNotEmpty(getInputNamespaceDefs()) ? soapNamespaceDefs + "\n" + getInputNamespaceDefs() : soapNamespaceDefs;
				String bodyMessageXe = StringUtils.isNotEmpty(getInputXPath()) ? soapBodyXPath + "/" + getInputXPath() : soapBodyXPath + "/*";
				bodyMessageTp = TransformerPool.getXPathTransformerPool(bodyMessageNd, bodyMessageXe, OutputType.XML);
			}
			String bisMessageHeaderNd = soapNamespaceDefs + "\n" + bisNamespaceDefs;
			String bisMessageHeaderXe;
			if (isBisMessageHeaderInSoapBody()) {
				bisMessageHeaderXe = soapBodyXPath + "/" + bisMessageHeaderXPath;
			} else {
				bisMessageHeaderXe = soapHeaderXPath + "/" + bisMessageHeaderXPath;
			}
			bisMessageHeaderTp = TransformerPool.getXPathTransformerPool(bisMessageHeaderNd, bisMessageHeaderXe, OutputType.XML);
			bisMessageHeaderConversationIdTp = TransformerPool.getXPathTransformerPool(bisNamespaceDefs, bisMessageHeaderConversationIdXPath, OutputType.TEXT);
			bisMessageHeaderExternalRefToMessageIdTp = TransformerPool.getXPathTransformerPool(bisNamespaceDefs, bisMessageHeaderExternalRefToMessageIdXPath, OutputType.TEXT);

			String bisErrorNd = soapNamespaceDefs + "\n" + bisNamespaceDefs;
			if (isBisResultInPayload()) {
				bisErrorXe = soapBodyXPath + "/*/" + bisErrorXPath;
			} else {
				bisErrorXe = soapBodyXPath + "/" + bisErrorXPath;
			}
			bisErrorXe = bisErrorXe + " or string-length(" + soapBodyXPath + "/" + soapErrorXPath + ")&gt;0";
			bisErrorTp = TransformerPool.getXPathTransformerPool(bisErrorNd, bisErrorXe, OutputType.TEXT);
			if (isAddOutputNamespace()) {
				addOutputNamespaceTp = XmlUtils.getAddRootNamespaceTransformerPool(getOutputNamespace(), true, false);
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create transformer", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Message result;
		try {
			if (getDirection()== Direction.WRAP) {
				String originalBisMessageHeader = (String) session.get(getBisMessageHeaderSessionKey());
				String bisConversationId = null;
				String bisExternalRefToMessageId = null;
				if (originalBisMessageHeader == null) {
					if (StringUtils.isNotEmpty(getBisConversationIdSessionKey())) {
						bisConversationId = (String) session.get(getBisConversationIdSessionKey());
					}
					if (StringUtils.isNotEmpty(getBisExternalRefToMessageIdSessionKey())) {
						bisExternalRefToMessageId = (String) session.get(getBisExternalRefToMessageIdSessionKey());
					}
				}
				String messageHeader = prepareMessageHeader(originalBisMessageHeader, bisConversationId, bisExternalRefToMessageId);

				String bisErrorCode = null;
				if (StringUtils.isNotEmpty(getBisErrorCodeSessionKey())) {
					bisErrorCode = (String) session.get(getBisErrorCodeSessionKey());
				}
				String bisErrorText = null;
				String bisDetailText = null;
				if (bisErrorCode != null) {
					if (StringUtils.isNotEmpty(getBisErrorTextSessionKey())) {
						bisErrorText = (String) session.get(getBisErrorTextSessionKey());
					}
					if (bisErrorText == null) {
						bisErrorText = errorCodeToText(bisErrorCode);
					}
					if (StringUtils.isNotEmpty(getBisErrorReasonSessionKey())) {
						bisDetailText = (String) session.get(getBisErrorReasonSessionKey());
					}
					if (isOmitResult()) {
						throw new PipeRunException(this, "bisError occurred: errorCode [" + bisErrorCode + "], errorText [" + bisErrorText + "]");
					}
				}
				String bisResult = prepareResult(bisErrorCode, bisErrorText, getBisServiceName(), getBisActionName(), bisDetailText);

				String payload;
				if (bisErrorCode == null || StringUtils.isEmpty(getOutputRoot())) {
					if (addOutputNamespaceTp != null) {
						payload = addOutputNamespaceTp.transformToString(message.asSource());
					} else {
						payload = message.asString();
					}
					payload = prepareReply(payload, isBisMessageHeaderInSoapBody() ? messageHeader : null, bisResult, isBisResultInPayload());
				} else {
					XmlBuilder outputElement = new XmlBuilder(getOutputRoot());
					if (StringUtils.isNotEmpty(getOutputNamespace())) {
						outputElement.addAttribute("xmlns", getOutputNamespace());
					}
					payload = prepareReply(outputElement.asXmlString(), isBisMessageHeaderInSoapBody() ? messageHeader : null, bisResult, isBisResultInPayload());
				}

				result = wrapMessage(new Message(payload), isBisMessageHeaderInSoapBody() ? null : messageHeader, session);
			} else {
				Message body = unwrapMessage(message, session);
				if (Message.isEmpty(body)) {
					throw new PipeRunException(this, "SOAP body is empty or message is not a SOAP message");
				}
				if (bisMessageHeaderTp != null) {
					String messageHeader = bisMessageHeaderTp.transformToString(message.asSource());
					if (messageHeader != null) {
						session.put(getBisMessageHeaderSessionKey(), messageHeader);
						log.debug("stored [{}] in pipeLineSession under key [{}]", messageHeader, getBisMessageHeaderSessionKey());
					}
				}
				if (bisErrorTp != null) {
					String bisError = bisErrorTp.transformToString(message.asSource());
					if (Boolean.valueOf(bisError).booleanValue()) {
						throw new PipeRunException(this, "bisErrorXPath [" + bisErrorXe + "] returns true");
					}
				}
				if (bodyMessageTp != null) {
					result = new Message(bodyMessageTp.transformToString(message.asSource()));
				} else {
					result = body;
				}
				if (isRemoveOutputNamespaces()) {
					result = XmlUtils.removeNamespaces(result);
				}
			}
		} catch (Throwable t) {
			throw new PipeRunException(this, " Unexpected exception during (un)wrapping ", t);

		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	private String prepareMessageHeader(String originalMessageHeader, String conversationId, String externalRefToMessageId) throws SAXException, IOException, TransformerException {
		XmlBuilder messageHeaderElement = new XmlBuilder("MessageHeader");
		messageHeaderElement.addAttribute("xmlns", "http://www.ing.com/CSP/XSD/General/Message_2");
		XmlBuilder fromElement = new XmlBuilder("From");
		XmlBuilder idElement = new XmlBuilder("Id");
		idElement.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
		fromElement.addSubElement(idElement);
		messageHeaderElement.addSubElement(fromElement);
		XmlBuilder headerFieldsElement = new XmlBuilder("HeaderFields");
		XmlBuilder conversationIdElement = new XmlBuilder("ConversationId");
		if (originalMessageHeader == null) {
			conversationIdElement.setValue(conversationId);
		} else {
			if (bisMessageHeaderConversationIdTp != null) {
				conversationIdElement.setValue(bisMessageHeaderConversationIdTp.transformToString(originalMessageHeader, null, true));
			}
		}
		headerFieldsElement.addSubElement(conversationIdElement);
		XmlBuilder messageIdElement = new XmlBuilder("MessageId");
		messageIdElement.setValue(Misc.getHostname() + "_" + UUIDUtil.createSimpleUUID());
		headerFieldsElement.addSubElement(messageIdElement);
		XmlBuilder externalRefToMessageIdElement = new XmlBuilder("ExternalRefToMessageId");
		if (originalMessageHeader == null) {
			externalRefToMessageIdElement.setValue(externalRefToMessageId);
		} else {
			if (bisMessageHeaderExternalRefToMessageIdTp != null) {
				externalRefToMessageIdElement.setValue(bisMessageHeaderExternalRefToMessageIdTp.transformToString(originalMessageHeader, null, true));
			}
		}
		headerFieldsElement.addSubElement(externalRefToMessageIdElement);
		XmlBuilder timestampElement = new XmlBuilder("Timestamp");
		timestampElement.setValue(DateFormatUtils.now(DateFormatUtils.FULL_ISO_FORMATTER));
		headerFieldsElement.addSubElement(timestampElement);
		messageHeaderElement.addSubElement(headerFieldsElement);
		return messageHeaderElement.asXmlString();
	}

	private String prepareResult(String errorCode, String errorText, String serviceName, String actionName, String detailText) {
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

	private String errorCodeToText(String errorCode) {
		for (int i = 0; i < BISERRORS.length; i++) {
			if (errorCode.equals(BISERRORS[i][0])) {
				return BISERRORS[i][1];
			}
		}
		return null;
	}

	private String prepareReply(String rawReply, String messageHeader, String result, boolean resultInPayload) throws DomBuilderException, TransformerException {
		List<String> messages = new ArrayList<>();
		if (messageHeader != null) {
			messages.add(messageHeader);
		}
		messages.add(rawReply);

		String payload = null;
		if (result == null) {
			payload = BisUtils.listToString(messages);
		} else {
			if (resultInPayload) {
				String message = BisUtils.listToString(messages);
				Document messageDoc = XmlUtils.buildDomDocument(message);
				Node messageRootNode = messageDoc.getFirstChild();
				Node resultNode = messageDoc.importNode(XmlUtils.buildNode(result), true);
				messageRootNode.appendChild(resultNode);
				payload = XmlUtils.nodeToString(messageDoc);
			} else {
				messages.add(result);
				payload = BisUtils.listToString(messages);
			}
		}
		return payload;
	}

	public void setInputXPath(String inputXPath) {
		this.inputXPath = inputXPath;
	}

	public String getInputXPath() {
		return inputXPath;
	}

	public void setInputNamespaceDefs(String inputNamespaceDefs) {
		this.inputNamespaceDefs = inputNamespaceDefs;
	}

	public String getInputNamespaceDefs() {
		return inputNamespaceDefs;
	}

	public void setOutputRoot(String outputRoot) {
		this.outputRoot = outputRoot;
	}

	public String getOutputRoot() {
		return outputRoot;
	}

	@Override
	public void setOutputNamespace(String outputNamespace) {
		this.outputNamespace = outputNamespace;
	}

	@Override
	public String getOutputNamespace() {
		return outputNamespace;
	}

	public void setBisMessageHeaderInSoapBody(boolean b) {
		bisMessageHeaderInSoapBody = b;
	}

	public boolean isBisMessageHeaderInSoapBody() {
		return bisMessageHeaderInSoapBody;
	}

	public void setBisMessageHeaderSessionKey(String bisMessageHeaderSessionKey) {
		this.bisMessageHeaderSessionKey = bisMessageHeaderSessionKey;
	}

	public String getBisMessageHeaderSessionKey() {
		return bisMessageHeaderSessionKey;
	}

	public void setBisResultInPayload(boolean b) {
		bisResultInPayload = b;
	}

	public boolean isBisResultInPayload() {
		return bisResultInPayload;
	}

	public void setBisConversationIdSessionKey(String bisConversationIdSessionKey) {
		this.bisConversationIdSessionKey = bisConversationIdSessionKey;
	}

	public String getBisConversationIdSessionKey() {
		return bisConversationIdSessionKey;
	}

	public void setBisExternalRefToMessageIdSessionKey(String bisExternalRefToMessageIdSessionKey) {
		this.bisExternalRefToMessageIdSessionKey = bisExternalRefToMessageIdSessionKey;
	}

	public String getBisExternalRefToMessageIdSessionKey() {
		return bisExternalRefToMessageIdSessionKey;
	}

	public void setBisErrorCodeSessionKey(String bisErrorCodeSessionKey) {
		this.bisErrorCodeSessionKey = bisErrorCodeSessionKey;
	}

	public String getBisErrorCodeSessionKey() {
		return bisErrorCodeSessionKey;
	}

	public void setBisErrorTextSessionKey(String bisErrorTextSessionKey) {
		this.bisErrorTextSessionKey = bisErrorTextSessionKey;
	}

	public String getBisErrorTextSessionKey() {
		return bisErrorTextSessionKey;
	}

	public void setBisErrorReasonSessionKey(String bisErrorReasonSessionKey) {
		this.bisErrorReasonSessionKey = bisErrorReasonSessionKey;
	}

	public String getBisErrorReasonSessionKey() {
		return bisErrorReasonSessionKey;
	}

	public void setBisServiceName(String bisServiceName) {
		this.bisServiceName = bisServiceName;
	}

	public String getBisServiceName() {
		return bisServiceName;
	}

	public void setBisActionName(String bisActionName) {
		this.bisActionName = bisActionName;
	}

	public String getBisActionName() {
		return bisActionName;
	}

	@Override
	public void setRemoveOutputNamespaces(boolean b) {
		removeOutputNamespaces = b;
	}

	@Override
	public boolean isRemoveOutputNamespaces() {
		return removeOutputNamespaces;
	}

	public void setOmitResult(boolean b) {
		omitResult = b;
	}

	public boolean isOmitResult() {
		return omitResult;
	}

	public void setAddOutputNamespace(boolean b) {
		addOutputNamespace = b;
	}

	public boolean isAddOutputNamespace() {
		return addOutputNamespace;
	}
}
