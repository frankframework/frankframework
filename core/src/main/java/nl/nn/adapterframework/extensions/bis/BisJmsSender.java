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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

/**
 * Bis (Business Integration Services) extension of JmsSender.
 * <br/>
 * For example request and reply, see {@link BisJmsListener}.
 * <br/>
 * If synchronous=true and one of the following conditions is true a SenderException is thrown:
 * - Result/Status in the reply soap body equals 'ERROR'
 * - faultcode in the reply soap fault is not empty
 * <br/>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.bis.BisSoapJmsSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoap(boolean) soap}</td><td>when <code>true</code>, messages sent are put in a SOAP envelope</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setResponseXPath(String) responseXPath}</td><td>xpath expression to extract the message from the reply which is passed to the pipeline. When soap=true the initial message is the content of the soap body. If empty, the content of the soap body is passed (without the root body)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResponseNamespaceDefs(String) responseNamespaceDefs}</td><td>namespace defintions for responseXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageHeaderInSoapBody(boolean) messageHeaderInSoapBody}</td><td>when <code>true</code>, the MessageHeader of the request is put in the SOAP body instead of in the SOAP header (first one is the old BIS standard)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setResultInPayload(boolean) resultInPayload}</td><td>when <code>true</code>, the Result tag in the reply will be put in the payload (as last child in root tag) instead of in the SOAP body as sibling of the payload (last one is the old BIS standard)</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setErrorListSessionKey(String) errorListSessionKey}</td><td>key of session variable to store ErrorList in when Result/Status in the reply equals 'ERROR'</td><td>bisErrorList</td></tr>
 * <tr><td>{@link #setConversationIdSessionKey(String) conversationIdSessionKey}</td><td>key of session variable in which ConversationId is stored; used in the MessageHeader of the request</td><td>bisConversationId</td></tr>
 * <tr><td>{@link #setExternalRefToMessageIdSessionKey(String) externalRefToMessageIdSessionKey}</td><td>key of session variable in which ExternalRefToMessageId is stored; used in the MessageHeader of the request</td><td>bisExternalRefToMessageId</td></tr>
 * <tr><td>{@link #setRequestNamespace(String) requestNamespace}</td><td>if not empty, this namespace is added to the request (this functionality will be used during migration from IFSA to TIBCO)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveResponseNamespaces(boolean) removeResponseNamespaces}</td><td>when set <code>true</code> namespaces (and prefixes) in the response are removed (this functionality will be used during migration from IFSA to TIBCO)</td><td>false</td></tr>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 * @deprecated Please use JmsSender combined with BisWrapperPipe
 */
@Deprecated
@ConfigurationWarning("Please change to JmsSender combined with BisWrapperPipe")
public class BisJmsSender extends JmsSender {

	private String responseXPath = null;
	private String responseNamespaceDefs = null;
	private boolean messageHeaderInSoapBody = false;
	private boolean resultInPayload = true;
	private String errorListSessionKey = "bisErrorList";
	private String conversationIdSessionKey = "bisConversationId";
	private String externalRefToMessageIdSessionKey = "bisExternalRefToMessageId";
	private String requestNamespace = null;
	private boolean removeResponseNamespaces = false;

	private TransformerPool bisErrorTp;
	private TransformerPool responseTp;
	private TransformerPool bisErrorListTp;

	private BisUtils bisUtils = null;

	public BisJmsSender() {
		setSoap(true);
	}

	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSoap()) {
			throw new ConfigurationException(getLogPrefix() + "soap must be true");
		}
		try {
			bisUtils = BisUtils.getInstance();
			bisErrorTp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(bisUtils.getSoapNamespaceDefs() + "\n" + bisUtils.getBisNamespaceDefs(), (isResultInPayload() ? bisUtils.getBisErrorXPath() : bisUtils.getOldBisErrorXPath()), "text"));
			responseTp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(StringUtils.isNotEmpty(getResponseNamespaceDefs()) ? bisUtils.getSoapNamespaceDefs() + "\n" + getResponseNamespaceDefs() : bisUtils.getSoapNamespaceDefs(), StringUtils.isNotEmpty(getResponseXPath()) ? bisUtils.getSoapBodyXPath() + "/" + getResponseXPath() : bisUtils.getSoapBodyXPath() + "/*", "xml"));
			bisErrorListTp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(bisUtils.getSoapNamespaceDefs() + "\n" + bisUtils.getBisNamespaceDefs(), (isResultInPayload() ? bisUtils.getBisErrorListXPath() : bisUtils.getOldBisErrorListXPath()), "xml"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot create transformer", e);
		}
	}

	@Override
	public String extractMessageBody(String rawMessageText, Map<String,Object> context, SoapWrapper soapWrapper) throws TransformerException, IOException {
		return rawMessageText;
	}

	@Override
	public Message sendMessage(Message input, IPipeLineSession session) throws SenderException, TimeOutException {
		String messageHeader;
		try {
			messageHeader = bisUtils.prepareMessageHeader(null, isMessageHeaderInSoapBody(), (String) session.get(getConversationIdSessionKey()), (String) session.get(getExternalRefToMessageIdSessionKey()));
		} catch (Exception e) {
			throw new SenderException(e);
		}
		String replyMessage;
		try {
			Message payload = bisUtils.prepareReply(input, isMessageHeaderInSoapBody() ? messageHeader : null, null, false);
			if (StringUtils.isNotEmpty(getRequestNamespace())) {
				payload = new Message(XmlUtils.addRootNamespace(payload.asString(), getRequestNamespace()));
			}
			replyMessage = super.sendMessage(payload, session, isMessageHeaderInSoapBody() ? null : messageHeader).asString();
		} catch (Exception e) {
			throw new SenderException(e);
		}
		if (isSynchronous()) {
			String bisError;
			String bisErrorList;
			try {
				bisError = bisErrorTp.transform(replyMessage, null, true);
				bisErrorList = bisErrorListTp.transform(replyMessage, null, true);
			} catch (Exception e) {
				throw new SenderException(e);
			}
			if (Boolean.valueOf(bisError).booleanValue()) {
				log.debug("put in session [" + getErrorListSessionKey() + "] [" + bisErrorList + "]");
				session.put(getErrorListSessionKey(), bisErrorList);
				throw new SenderException("bisErrorXPath [" + (isResultInPayload() ? bisUtils.getBisErrorXPath() : bisUtils.getOldBisErrorXPath()) + "] returns true");
			}
			try {
				replyMessage = responseTp.transform(replyMessage, null, true);
				if (isRemoveResponseNamespaces()) {
					replyMessage = XmlUtils.removeNamespaces(replyMessage);
				}
				if (isResultInPayload()) {
					Element soapBodyElement = XmlUtils.buildElement(replyMessage, true);
					Element resultElement = XmlUtils.getFirstChildTag(soapBodyElement, "Result");
					if (resultElement != null) {
						soapBodyElement.removeChild(resultElement);
					}
					replyMessage = XmlUtils.nodeToString(soapBodyElement);
				}
				return new Message(replyMessage);

			} catch (Exception e) {
				throw new SenderException(e);
			}

		} else {
			return new Message(replyMessage);
		}
	}

	public void setResponseXPath(String responseXPath) {
		this.responseXPath = responseXPath;
	}

	public String getResponseXPath() {
		return responseXPath;
	}

	public void setResponseNamespaceDefs(String responseNamespaceDefs) {
		this.responseNamespaceDefs = responseNamespaceDefs;
	}

	public String getResponseNamespaceDefs() {
		return responseNamespaceDefs;
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

	public void setErrorListSessionKey(String errorListSessionKey) {
		this.errorListSessionKey = errorListSessionKey;
	}

	public String getErrorListSessionKey() {
		return errorListSessionKey;
	}

	public void setExternalRefToMessageIdSessionKey(String externalRefToMessageIdSessionKey) {
		this.externalRefToMessageIdSessionKey = externalRefToMessageIdSessionKey;
	}

	public String getExternalRefToMessageIdSessionKey() {
		return externalRefToMessageIdSessionKey;
	}

	public void setConversationIdSessionKey(String conversationIdSessionKey) {
		this.conversationIdSessionKey = conversationIdSessionKey;
	}

	public String getConversationIdSessionKey() {
		return conversationIdSessionKey;
	}

	public void setRequestNamespace(String requestNamespace) {
		this.requestNamespace = requestNamespace;
	}

	public String getRequestNamespace() {
		return requestNamespace;
	}

	public void setRemoveResponseNamespaces(boolean b) {
		removeResponseNamespaces = b;
	}

	public boolean isRemoveResponseNamespaces() {
		return removeResponseNamespaces;
	}
}
