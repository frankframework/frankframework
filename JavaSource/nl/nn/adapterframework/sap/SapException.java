/*
 * $Log: SapException.java,v $
 * Revision 1.1  2004-06-22 06:56:45  L190409
 * First version of SAP package
 *
 *
 */
package nl.nn.adapterframework.sap;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception thrown by classes in the sap-package (implementation) to notify
 * various problems.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 */
public class SapException extends IbisException {
	public static final String version="$Id: SapException.java,v 1.1 2004-06-22 06:56:45 L190409 Exp $";
		
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
