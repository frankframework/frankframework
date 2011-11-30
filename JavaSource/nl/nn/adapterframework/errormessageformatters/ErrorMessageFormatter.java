/*
 * $Log: ErrorMessageFormatter.java,v $
 * Revision 1.11  2011-11-30 13:52:03  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.9  2011/09/16 12:05:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved getting message to separate method
 *
 * Revision 1.8  2010/04/16 11:31:55  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed bug CDATA in CDATA
 *
 * Revision 1.7  2007/02/12 14:30:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.6  2007/02/12 13:44:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.5  2004/09/01 11:26:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made receivedTime optional (only used if non-zero)
 *
 * Revision 1.4  2004/03/30 07:30:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.errormessageformatters;

import java.util.Date;

import nl.nn.adapterframework.core.IErrorMessageFormatter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
/**
 * This <code>ErrorMessageFormatter</code> wraps an error in an XML string.
 *
 * <br/>
 * Sample xml:
 * <br/>
 * <code><pre>
 * &lt;errorMessage&gt;
 *    &lt;message timestamp="Mon Oct 13 12:01:57 CEST 2003" 
 *             originator="NN IOS AdapterFramework(set from 'application.name' and 'application.version')"
 *             message="<i>message describing the error that occurred</i>" &gt;
 *    &lt;location class="nl.nn.adapterframework.pipes.XmlSwitch" name="ServiceSwitch"/&gt
 *    &lt;details&gt<i>detailed information of the error</i>&lt;/details&gt
 *    &lt;originalMessage messageId="..." receivedTime="Mon Oct 27 12:10:18 CET 2003" &gt;
 *        &lt;![CDATA[<i>contents of message for which the error occurred</i>]]&gt;
 *    &lt;/originalMessage&gt;
 * &lt;/errorMessage&gt;
 * </pre></code>
 * 
 * @version Id
 * @author  Gerrit van Brakel
 */
public class ErrorMessageFormatter implements IErrorMessageFormatter {
	public static final String version = "$RCSfile: ErrorMessageFormatter.java,v $ $Revision: 1.11 $ $Date: 2011-11-30 13:52:03 $";
    protected Logger log = LogUtil.getLogger(this);
	
	/**
	 * Format the available parameters into a XML-message.
	 *
	 * Override this method in descender-classes to obtain the required behaviour.
	 */
	public String format(
	    String message,
	    Throwable t,
	    INamedObject location,
	    String originalMessage,
	    String messageId,
	    long receivedTime) {
	
		String details = null;
		message = getMessage(message, t);
		if (t != null) {
			details = ExceptionUtils.getStackTrace(t);
		}
		 
		String originator = AppConstants.getInstance().getProperty("application.name")+" "+
	            			AppConstants.getInstance().getProperty("application.version");
	    // Build a Base xml
	    XmlBuilder errorXml = new XmlBuilder("errorMessage");
	    errorXml.addAttribute("timestamp", new Date().toString());
	    errorXml.addAttribute("originator", originator);
	    errorXml.addAttribute("message", message);
	
	    if (location!=null) {
		    XmlBuilder locationXml = new XmlBuilder("location");
		    locationXml.addAttribute("class", location.getClass().getName());
		    locationXml.addAttribute("name", location.getName());
			errorXml.addSubElement(locationXml);
	    }
	    
	    if (details != null && !details.equals("")) {
		    XmlBuilder detailsXml = new XmlBuilder("details");
	    	//detailsXml.setCdataValue(details);
	    	detailsXml.setValue(details, true);
		    errorXml.addSubElement(detailsXml);
		}
			
	    XmlBuilder originalMessageXml = new XmlBuilder("originalMessage");
	    originalMessageXml.addAttribute("messageId", messageId);
	    if (receivedTime!=0) {
			originalMessageXml.addAttribute("receivedTime", new Date(receivedTime).toString());
	    }
	   	//originalMessageXml.setCdataValue(originalMessage);
		originalMessageXml.setValue(originalMessage, true);
	    errorXml.addSubElement(originalMessageXml);
	
	    return errorXml.toXML();
	}

	protected String getMessage(String message, Throwable t) {
		if (t != null) {
			if (message == null || message.equals("")) {
				message = t.getMessage();
			} else {
				message += ": "+t.getMessage();
			}
		}
		return message;
	}
}
