/*
 * $Log: BisJmsSender.java,v $
 * Revision 1.7  2011-08-31 13:39:28  m168309
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
import java.util.ArrayList;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
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
 * <tr><td>{@link #setMessageHeaderInSoapBody(boolean) messageHeaderInSoapBody}</td><td>when <code>true</code>, the MessageHeader is put in the SOAP body instead of in the SOAP header (first one is the old BIS standard)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setResultInPayload(boolean) resultInPayload}</td><td>when <code>true</code>, the Result is put in the payload (as last child in root tag) instead of in the SOAP body as sibling of the payload (last one is the old BIS standard)</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setConversationIdSessionKey(String) conversationIdSessionKey}</td><td>key of session variable to store ConversationId in; used in the MessageHeader of the request</td><td>bisConversationId</td></tr>
 * <tr><td>{@link #setExternalRefToMessageIdSessionKey(String) externalRefToMessageIdSessionKey}</td><td>key of session variable to store ExternalRefToMessageId in; used in the MessageHeader of the request</td><td>bisExternalRefToMessageId</td></tr>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class BisJmsSender extends JmsSender {

	private final static String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private final static String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private final static String bisNamespaceDefs = "bis=http://www.ing.com/CSP/XSD/General/Message_2";
	private final static String bisErrorXPath_OLD = "soapenv:Envelope/soapenv:Body/bis:Result/bis:Status='ERROR' or string-length(soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode)&gt;0";
	private final static String bisErrorXPath_NEW = "soapenv:Envelope/soapenv:Body/*/bis:Result/bis:Status='ERROR' or string-length(soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode)&gt;0";

	private String responseXPath = null;
	private String responseNamespaceDefs = null;
	private boolean messageHeaderInSoapBody = false;
	private boolean resultInPayload = true;
	private String conversationIdSessionKey = "bisConversationId";
	private String externalRefToMessageIdSessionKey = "bisExternalRefToMessageId";

	private TransformerPool bisErrorTp;
	private TransformerPool responseTp;

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
			bisErrorTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, (isResultInPayload() ? bisErrorXPath_NEW : bisErrorXPath_OLD), "text"));
			responseTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(StringUtils.isNotEmpty(getResponseNamespaceDefs()) ? soapNamespaceDefs + "\n" + getResponseNamespaceDefs() : soapNamespaceDefs, StringUtils.isNotEmpty(getResponseXPath()) ? soapBodyXPath + "/" + getResponseXPath() : soapBodyXPath + "/*", "xml"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot create transformer", e);
		}
	}

	public String extractMessageBody(String rawMessageText, Map context, SoapWrapper soapWrapper) throws DomBuilderException, TransformerException, IOException {
		return rawMessageText;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		ArrayList messages = new ArrayList();
		String messageHeader;
		try {
			messageHeader = bisUtils.prepareMessageHeader(null, isMessageHeaderInSoapBody(), (String) prc.getSession().get(getConversationIdSessionKey()), (String) prc.getSession().get(getExternalRefToMessageIdSessionKey()));
		} catch (Exception e) {
			throw new SenderException(e);
		}
		if (isMessageHeaderInSoapBody()) {
			messages.add(messageHeader);
			messageHeader = null;
		}
		messages.add(message);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < messages.size(); i++) {
			sb.append((String) messages.get(i));
		}
		String soapBody = super.sendMessage(correlationID, sb.toString(), prc, messageHeader);
		if (isSynchronous()) {
			String bisError;
			try {
				bisError = bisErrorTp.transform(soapBody, null, true);
			} catch (Exception e) {
				throw new SenderException(e);
			}
			if (Boolean.valueOf(bisError).booleanValue()) {
				throw new SenderException("bisErrorXPath [" + (isResultInPayload() ? bisErrorXPath_NEW : bisErrorXPath_OLD) + "] returns true");
			}
			try {
				soapBody = responseTp.transform(soapBody, null, true);
				if (isResultInPayload()) {
					Element soapBodyElement = XmlUtils.buildElement(soapBody, true);
					Element resultElement = XmlUtils.getFirstChildTag(soapBodyElement, "Result");
					if (resultElement != null) {
						soapBodyElement.removeChild(resultElement);
					}
					soapBody = XmlUtils.nodeToString(soapBodyElement);
				}
				return soapBody;

			} catch (Exception e) {
				throw new SenderException(e);
			}

		} else {
			return soapBody;
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
}
