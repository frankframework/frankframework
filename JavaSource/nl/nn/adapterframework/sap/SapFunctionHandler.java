/*
 * $Log: SapFunctionHandler.java,v $
 * Revision 1.1  2004-06-22 06:56:45  L190409
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.sap;

import com.sap.mw.jco.JCO;

/**
 * The interface clients (users) of a SAP function must implement.
 *
 * @version Id
 * @author Gerrit van Brakel
 */
public interface SapFunctionHandler {
	public static final String version="$Id: SapFunctionHandler.java,v 1.1 2004-06-22 06:56:45 L190409 Exp $";
	
public void processFunctionCall(JCO.Function function) throws SapException;
}
