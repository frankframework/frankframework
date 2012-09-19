/*
 * $Log: XmlValidatorErrorHandler.java,v $
 * Revision 1.1  2012-09-19 09:49:58  m00f069
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 */
package nl.nn.adapterframework.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class XmlValidatorErrorHandler implements ErrorHandler {
	private Logger log = LogUtil.getLogger(this);
	private boolean errorOccured = false;
	private String reasons;
	private XMLReader parser;
	private XmlBuilder xmlReasons = new XmlBuilder("reasons");


	public XmlValidatorErrorHandler(XMLReader parser) {
		this.parser = parser;
	}

	public void addReason(String message, String location) {
		try {
			ContentHandler ch = parser.getContentHandler();
			XmlValidatorContentHandler xvch = (XmlValidatorContentHandler)ch;

			XmlBuilder reason = new XmlBuilder("reason");
			XmlBuilder detail;

			detail = new XmlBuilder("message");;
			detail.setValue(message);
			reason.addSubElement(detail);

			detail = new XmlBuilder("elementName");;
			detail.setValue(xvch.getElementName());
			reason.addSubElement(detail);

			detail = new XmlBuilder("xpath");;
			detail.setValue(xvch.getXpath());
			reason.addSubElement(detail);

			xmlReasons.addSubElement(reason);
		} catch (Throwable t) {
			log.error("Exception handling errors",t);

			XmlBuilder reason = new XmlBuilder("reason");
			XmlBuilder detail;

			detail = new XmlBuilder("message");;
			detail.setCdataValue(t.getMessage());
			reason.addSubElement(detail);

			xmlReasons.addSubElement(reason);
		}

		if (StringUtils.isNotEmpty(location)) {
			message = location + ": " + message;
		}
		errorOccured = true;
		if (reasons == null) {
			 reasons = message;
		 } else {
			 reasons = reasons + "\n" + message;
		 }
	}

	public void addReason(Throwable t) {
		String message = null;
		String location = null;
		if (t instanceof SAXParseException) {
			SAXParseException spe = (SAXParseException)t;
			location = "at ("+spe.getLineNumber()+ ","+spe.getColumnNumber()+")";
		}
		if (t instanceof SAXException) {
			message = t.getMessage();
		} else {
			message = t.getClass().toString() + ": " + t.getMessage();
		}
		addReason(message,location);
	}

	public void warning(SAXParseException exception) {
		addReason(exception);
	}
    public void error(SAXParseException exception) {
    	addReason(exception);
    }
    public void fatalError(SAXParseException exception) {
		addReason(exception);
    }

    public boolean hasErrorOccured() {
        return errorOccured;
    }

     public String getReasons() {
        return reasons;
    }

	public String getXmlReasons() {
	   return xmlReasons.toXML();
   }
}

