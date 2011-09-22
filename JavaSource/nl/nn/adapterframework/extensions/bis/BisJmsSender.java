/*
 * $Log: BisJmsSender.java,v $
 * Revision 1.9  2011-09-22 14:17:07  europe\m168309
 * Deprecated BisJmsSender/BisJmsListener
 *
 * Revision 1.8  2011/09/12 07:23:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * BisJmsSender/BisJmsListener: added functionality on behalf of DINN (migration from IFSA to TIBCO)
 *
 * Revision 1.7  2011/08/31 13:39:28  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved result tag from first child of root to last child of root
 *
 * Revision 1.6  2011/07/07 12:13:24  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added resultInPayload attribute
 *
 * Revision 1.5  2011/06/06 12:27:26  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * BisJmsSender/BisJmsListener: added messageHeaderInSoapBody attribute
 *
 * Revision 1.4  2011/03/30 14:51:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added MessageHeader to request
 *
 * Revision 1.3  2011/03/29 12:02:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * cosmetic change
 *
 * Revision 1.2  2011/03/28 14:20:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added check on soap fault
 *
 * Revision 1.1  2011/03/21 14:55:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.extensions.bis;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.DomBuilderException;
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
 * <tr><td>classname</td><td>nl.nn.adapterframework.extensions.bis.BisSoapJmsSender</td><td>&nbsp;</td></tr>
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
 * @version Id
 * @deprecated Please use JmsSender combined with BisWrapperPipe
 */
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
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to JmsSender combined with BisWrapperPipe";
		configWarnings.add(log, msg);
		super.configure();
		if (!isSoap()) {
			throw new ConfigurationException(getLogPrefix() + "soap must be true");
		}
		try {
			bisUtils = BisUtils.getInstance();
			bisErrorTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(bisUtils.getSoapNamespaceDefs() + "\n" + bisUtils.getBisNamespaceDefs(), (isResultInPayload() ? bisUtils.getBisErrorXPath() : bisUtils.getOldBisErrorXPath()), "text"));
			responseTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(StringUtils.isNotEmpty(getResponseNamespaceDefs()) ? bisUtils.getSoapNamespaceDefs() + "\n" + getResponseNamespaceDefs() : bisUtils.getSoapNamespaceDefs(), StringUtils.isNotEmpty(getResponseXPath()) ? bisUtils.getSoapBodyXPath() + "/" + getResponseXPath() : bisUtils.getSoapBodyXPath() + "/*", "xml"));
			bisErrorListTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(bisUtils.getSoapNamespaceDefs() + "\n" + bisUtils.getBisNamespaceDefs(), (isResultInPayload() ? bisUtils.getBisErrorListXPath() : bisUtils.getOldBisErrorListXPath()), "xml"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot create transformer", e);
		}
	}

	public String extractMessageBody(String rawMessageText, Map context, SoapWrapper soapWrapper) throws DomBuilderException, TransformerException, IOException {
		return rawMessageText;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String messageHeader;
		try {
			messageHeader = bisUtils.prepareMessageHeader(null, isMessageHeaderInSoapBody(), (String) prc.getSession().get(getConversationIdSessionKey()), (String) prc.getSession().get(getExternalRefToMessageIdSessionKey()));
		} catch (Exception e) {
			throw new SenderException(e);
		}
		String payload;
		try {
			payload = bisUtils.prepareReply(message, isMessageHeaderInSoapBody() ? messageHeader : null, null, false);
			if (StringUtils.isNotEmpty(getRequestNamespace())) {
				payload = XmlUtils.addRootNamespace(payload, getRequestNamespace());
			}
		} catch (Exception e) {
			throw new SenderException(e);
		}
		String replyMessage = super.sendMessage(correlationID, payload, prc, isMessageHeaderInSoapBody() ? null : messageHeader);
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
				prc.getSession().put(getErrorListSessionKey(), bisErrorList);
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
				return replyMessage;

			} catch (Exception e) {
				throw new SenderException(e);
			}

		} else {
			return replyMessage;
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
