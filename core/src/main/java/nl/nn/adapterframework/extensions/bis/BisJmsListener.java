/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.bis;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Bis (Business Integration Services) extension of JmsListener.
 * <br/>
 * Example request:<br/><code><pre>
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
 * <br/>
 * Example reply:<br/><code><pre>
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
 * <br/>
 * Example element Result in case of an error reply:<br/><code><pre>
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
 * </pre></code><br/>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.bis.BisSoapJmsListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoap(boolean) soap}</td><td>when <code>true</code>, messages sent are put in a SOAP envelope</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setRequestXPath(String) requestXPath}</td><td>xpath expression to extract the message which is passed to the pipeline. When soap=true the initial message is the content of the soap body. If empty, the content of the soap body is passed (without the root body)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRequestNamespaceDefs(String) requestNamespaceDefs}</td><td>namespace defintions for requestXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageHeaderInSoapBody(boolean) messageHeaderInSoapBody}</td><td>when <code>true</code>, the MessageHeader is put in the SOAP body instead of in the SOAP header (first one is the old BIS standard)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setResultInPayload(boolean) resultInPayload}</td><td>when <code>true</code>, the Result is put in the payload (as last child in root tag) instead of in the SOAP body as sibling of the payload (last one is the old BIS standard)</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setOmitResult(boolean) omitResult}</td><td>when <code>true</code>, the Result is omitted and instead of Result/Status 'ERROR' a ListenerException is thrown (this functionality will be used during migration from IFSA to TIBCO)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setRemoveRequestNamespaces(boolean) removeRequestNamespaces}</td><td>when set <code>true</code> namespaces (and prefixes) in the request are removed (this functionality will be used during migration from IFSA to TIBCO)</td><td>false</td></tr>
 * <tr><td>{@link #setLayByNamespace(boolean) layByNamespace}</td><td>when <code>true</code>, the namespace of the request is laid by and afterwards added to the reply (this functionality will be used during migration from IFSA to TIBCO)</td><td><code>false</code></td></tr>
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
 * <tr><td>{@link #setErrorReasonSessionKey(String) errorReasonSessionKey}</td><td>key of session variable to store error reason in (if an error occurs)</td><td>bisErrorReason</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>name of the service; used in the error reply</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setActionName(String) actionName}</td><td>name of the operation; used in the error reply</td><td>&nbsp;</td></tr>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 * @deprecated Please use JmsListener combined with BisWrapperPipe
 */
@Deprecated
@ConfigurationWarning("Please change to JmsListener combined with BisWrapperPipe")
public class BisJmsListener extends JmsListener {

	private static final String MESSAGETEXT_KEY = "messageText";
	private static final String MESSAGEBODYNAMESPACE_KEY = "messageBodyNamespace";

	private String requestXPath = null;
	private String requestNamespaceDefs = null;
	private boolean messageHeaderInSoapBody = false;
	private boolean resultInPayload = true;
	private boolean omitResult = false;
	private boolean removeRequestNamespaces = false;
	private boolean layByNamespace = false;
	private String errorCodeSessionKey = "bisErrorCode";
	private String errorTextSessionKey = null;
	private String errorReasonSessionKey = "bisErrorReason";
	private String serviceName = null;
	private String actionName = null;

	private TransformerPool requestTp;

	private BisUtils bisUtils = null;

	public BisJmsListener() {
		setSoap(true);
	}

	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSoap()) {
			throw new ConfigurationException(getLogPrefix() + "soap must be true");
		}
		try {
			bisUtils = BisUtils.getInstance();
			requestTp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(StringUtils.isNotEmpty(getRequestNamespaceDefs()) ? bisUtils.getSoapNamespaceDefs() + "\n" + getRequestNamespaceDefs() : bisUtils.getSoapNamespaceDefs(), StringUtils.isNotEmpty(getRequestXPath()) ? bisUtils.getSoapBodyXPath() + "/" + getRequestXPath() : bisUtils.getSoapBodyXPath() + "/*", "xml"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot create transformer", e);
		}
	}

	@Override
	public String extractMessageBody(String rawMessageText, Map<String,Object> context, SoapWrapper soapWrapper) throws SAXException, TransformerException, IOException {
		context.put(MESSAGETEXT_KEY, rawMessageText);
		log.debug("extract messageBody from message [" + rawMessageText + "]");
		String messageBody = requestTp.transform(rawMessageText, null, true);
		if (isLayByNamespace()) {
			String messageBodyNamespace = XmlUtils.getRootNamespace(messageBody);
			if (messageBodyNamespace != null) {
				context.put(MESSAGEBODYNAMESPACE_KEY, messageBodyNamespace);
			}
			if (isRemoveRequestNamespaces()) {
				messageBody = XmlUtils.removeNamespaces(messageBody);
			}
		}
		return messageBody;
	}

	@Override
	public Message prepareReply(Message rawReply, Map<String,Object> threadContext) throws ListenerException {
		String originalMessageText = (String) threadContext.get(MESSAGETEXT_KEY);
		String messageBodyNamespace = (String) threadContext.get(MESSAGEBODYNAMESPACE_KEY);
		try {
			if (isLayByNamespace() && messageBodyNamespace != null) {
				rawReply = new Message(XmlUtils.addRootNamespace(rawReply.asString(), messageBodyNamespace));
			}
			String errorCode = null;
			if (StringUtils.isNotEmpty(getErrorCodeSessionKey())) {
				errorCode = (String) threadContext.get(getErrorCodeSessionKey());
			}
			String result;
			String messageHeader = bisUtils.prepareMessageHeader(originalMessageText, isMessageHeaderInSoapBody());
			if (isOmitResult()) {
				result = null;
			} else {
				result = prepareResult(errorCode, threadContext);
			}
			Message payload = bisUtils.prepareReply(rawReply, isMessageHeaderInSoapBody() ? messageHeader : null, result, isResultInPayload());
			return super.prepareReply(payload, threadContext, isMessageHeaderInSoapBody() ? null : messageHeader);
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public String prepareResult(String errorCode, Map<String,Object> threadContext) throws DomBuilderException, IOException, TransformerException {
		String errorText = null;
		String serviceName = null;
		String actionName = null;
		String detailText = null;
		if (errorCode != null) {
			if (StringUtils.isNotEmpty(getErrorTextSessionKey())) {
				errorText = (String) threadContext.get(getErrorTextSessionKey());
			} else {
				errorText = bisUtils.errorCodeToText(errorCode);
			}
			if (StringUtils.isNotEmpty(getServiceName())) {
				serviceName = (String) threadContext.get(getServiceName());
			}
			if (StringUtils.isNotEmpty(getActionName())) {
				actionName = (String) threadContext.get(getActionName());
			}
			if (StringUtils.isNotEmpty(getErrorReasonSessionKey())) {
				detailText = (String) threadContext.get(getErrorReasonSessionKey());
			}
		}
		return bisUtils.prepareResult(errorCode, errorText, serviceName, actionName, detailText);
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

	public void setMessageHeaderInSoapBody(boolean b) {
		messageHeaderInSoapBody = b;
	}

	public boolean isMessageHeaderInSoapBody() {
		return messageHeaderInSoapBody;
	}

	public void setResultInPayload(boolean b) {
		resultInPayload = b;
	}

	public boolean isResultInPayload() {
		return resultInPayload;
	}

	public void setOmitResult(boolean b) {
		omitResult = b;
	}

	public boolean isOmitResult() {
		return omitResult;
	}

	public void setRemoveRequestNamespaces(boolean b) {
		removeRequestNamespaces = b;
	}

	public boolean isRemoveRequestNamespaces() {
		return removeRequestNamespaces;
	}

	public void setLayByNamespace(boolean b) {
		layByNamespace = b;
	}

	public boolean isLayByNamespace() {
		return layByNamespace;
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
