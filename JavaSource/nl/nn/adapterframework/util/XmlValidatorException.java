/*
 * $Log: XmlValidatorException.java,v $
 * Revision 1.1  2012-09-19 09:49:58  m00f069
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 */
package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.IbisException;

public class XmlValidatorException extends IbisException {
	private static final long serialVersionUID = 1L;

	XmlValidatorException(String cause, Throwable t) {
		super(cause,t);
	}

	XmlValidatorException(String cause) {
		super(cause);
	}
}
