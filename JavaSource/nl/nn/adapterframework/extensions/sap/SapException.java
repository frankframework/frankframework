/*
 * $Log: SapException.java,v $
 * Revision 1.3  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 15:00:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2004/07/06 07:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SAP functionality to extensions
 *
 * Revision 1.1  2004/06/22 06:56:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version of SAP package
 *
 *
 */
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception thrown by classes in the sap-package (implementation) to notify
 * various problems.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 */
public class SapException extends IbisException {
		
	public SapException() {
		super();
	}
	public SapException(String errMsg) {
		super(errMsg);
	}
	public SapException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public SapException(Throwable t) {
		super(t);
	}
}
