/*
 * $Log: BisJmsSender.java,v $
 * Revision 1.3  2011-03-29 12:02:14  m168309
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
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

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
 * <tr><td>{@link #setResponseXPath(String) responseXPath}</td><td>xpath expression to extract the message from the reply which is passed to the pipeline. When soap=true the initial message is the content of the soap body</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResponseNamespaceDefs(String) responseNamespaceDefs}</td><td>namespace defintions for responseXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class BisJmsSender extends JmsSender {

	private final static String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private final static String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private final static String bisNamespaceDefs = "bis=http://www.ing.com/CSP/XSD/General/Message_2";
	private final static String bisErrorXPath = "soapenv:Envelope/soapenv:Body/bis:Result/bis:Status='ERROR' or string-length(soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode)&gt;0";

	private String responseXPath = null;
	private String responseNamespaceDefs = null;

	private TransformerPool bisErrorTp;
	private TransformerPool responseTp;

	public BisJmsSender() {
		setSoap(true);
	}

	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSoap()) {
			throw new ConfigurationException(getLogPrefix() + "soap must be true");
		}
		try {
			bisErrorTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + bisNamespaceDefs, bisErrorXPath, "text"));
			responseTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(soapNamespaceDefs + "\n" + getResponseNamespaceDefs(), soapBodyXPath + "/" + getResponseXPath(), "xml"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot create transformer", e);
		}
	}

	public String extractMessageBody(String rawMessageText, Map context, SoapWrapper soapWrapper) throws DomBuilderException, TransformerException, IOException {
		return rawMessageText;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String soapBody = super.sendMessage(correlationID, message, prc);
		if (isSynchronous()) {
			String bisError;
			try {
				bisError = bisErrorTp.transform(soapBody, null, true);
			} catch (Exception e) {
				throw new SenderException(e);
			}
			if (Boolean.valueOf(bisError).booleanValue()) {
				throw new SenderException("bisErrorXPath [" + bisErrorXPath + "] returns true");
			}
			try {
				return responseTp.transform(soapBody, null, true);
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
}
