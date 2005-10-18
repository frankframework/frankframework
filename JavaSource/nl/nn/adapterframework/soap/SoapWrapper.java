/*
 * $Log: SoapWrapper.java,v $
 * Revision 1.1  2005-10-18 08:14:48  europe\L190409
 * created separate soap-package
 *
 * Revision 1.4  2005/07/05 12:55:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to set targetObjectNamespace for putInEnvelope
 *
 * Revision 1.3  2005/05/31 09:16:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.2  2005/05/31 09:15:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added catch for DomBuilderException
 *
 * Revision 1.1  2005/02/24 12:15:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SOAP conversion coding to SOAP-wrapper
 *
 */
package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Utility class that wraps and unwraps messages from (and into) a SOAP Envelope.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class SoapWrapper {
	public static final String version="$RCSfile: SoapWrapper.java,v $ $Revision: 1.1 $ $Date: 2005-10-18 08:14:48 $";
	protected Logger log = Logger.getLogger(this.getClass());

	private TransformerPool extractBody;
	private TransformerPool extractFaultCount;
	private TransformerPool extractFaultCode;
	private TransformerPool extractFaultString;
	private final static String extractNamespaces="xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"";
	private final static String extractBodyXPath="/soapenv:Envelope/soapenv:Body/*";
	private final static String extractFaultCountXPath="count(/soapenv:Envelope/soapenv:Body/soapenv:Fault)";
	private final static String extractFaultCodeXPath="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode";
	private final static String extractFaultStringXPath="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultstring";

	private static SoapWrapper self=null;
	
	private SoapWrapper() {
		super();
	}
	
	private void init() throws ConfigurationException {
		try {
			extractBody        = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaces,extractBodyXPath,"xml"));
			extractFaultCount  = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaces,extractFaultCountXPath,"text"));
			extractFaultCode   = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaces,extractFaultCodeXPath,"text"));
			extractFaultString = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaces,extractFaultStringXPath,"text"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create SOAP transformer",e);
		}
	}
	
	public static SoapWrapper getInstance() throws ConfigurationException {
		if (self==null) {
			self=new SoapWrapper();
			self.init();
		}
		return self;
	}

	public void checkForSoapFault(String responseBody, Throwable nested) throws SenderException {
		String faultString=null;
		String faultCode=null;
		int faultCount=0;
		try {
			faultCount = getFaultCount(responseBody);
			log.debug("fault count="+faultCount);
			if (faultCount > 0) {
				faultCode = getFaultCode(responseBody);
				faultString = getFaultString(responseBody);
				log.debug("faultCode="+faultCode+", faultString="+faultString);
			}
		} catch (DomBuilderException e) {
			log.debug("IOException extracting fault message",e);
		} catch (IOException e) {
			log.debug("IOException extracting fault message",e);
		} catch (TransformerException e) {
			log.debug("TransformerException extracting fault message:"+e.getMessageAndLocation());
		}
		if (faultCount > 0) {
			String msg = "SOAP fault ["+faultCode+"]: "+faultString;
			log.info(msg);
			throw new SenderException(msg, nested);
		}
	}

	
	public String getBody(String message) throws DomBuilderException, TransformerException, IOException {
		return extractBody.transform(message,null);
	}
	public String getBody(InputStream request) throws TransformerException, IOException {
		return extractBody.transform(new StreamSource(request),null);
	}

	public int getFaultCount(String message) throws NumberFormatException, DomBuilderException, TransformerException, IOException {
		return Integer.parseInt(extractFaultCount.transform(message,null));
	}
	public String getFaultCode(String message) throws DomBuilderException, TransformerException, IOException {
		return extractFaultCode.transform(message,null);
	}
	public String getFaultString(String message) throws DomBuilderException, TransformerException, IOException {
		return extractFaultString.transform(message,null);
	}
	
	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace) {
		
		String encodingStyle="";
		String targetObjectNamespaceClause="";
		if (!StringUtils.isEmpty(encodingStyleUri)) {
			encodingStyle="soapenv:encodingStyle=\""+ encodingStyleUri+"\" ";
		}
		if (!StringUtils.isEmpty(targetObjectNamespace)) {
			targetObjectNamespaceClause=" xmlns=\""+ targetObjectNamespace+"\" ";
		}
		String soapmsg= 
		"<soapenv:Envelope " + 
			"xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "+encodingStyle +
			targetObjectNamespaceClause +
			"xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" >" + 
			"<soapenv:Body>" + 	
				message +
			"</soapenv:Body>"+
		"</soapenv:Envelope>";
		return soapmsg;
	}

	public String putInEnvelope(String message, String encodingStyleUri) {
		return putInEnvelope(message, encodingStyleUri, null);
	}

}
