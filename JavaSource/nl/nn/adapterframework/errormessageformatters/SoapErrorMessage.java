/*
 * $Log: SoapErrorMessage.java,v $
 * Revision 1.1  2011-09-16 12:06:15  europe\m168309
 * first version
 *
 *
 */
package nl.nn.adapterframework.errormessageformatters;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.soap.SoapWrapper;

/**
 * ErrorMessageFormatter that returns a soap fault message.
 * 
 * @version Id
 * @author  Peter Leeuwenburgh
 */
public class SoapErrorMessage extends ErrorMessageFormatter {

	public String format(String message, Throwable t, INamedObject location, String originalMessage, String messageId, long receivedTime) {

		try {
			return SoapWrapper.getInstance().createSoapFaultMessage(getMessage(message, t));
		} catch (Exception e) {
			log.error("got error getting soapWrapper instance", e);
			return super.format(message, t, location, originalMessage, messageId, receivedTime);
		}
	}
}
