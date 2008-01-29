/*
 * $Log: SapFunctionHandler.java,v $
 * Revision 1.2  2008-01-29 15:40:20  europe\L190409
 * added support for idocs
 *
 * Revision 1.1  2004/07/06 07:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SAP functionality to extensions
 *
 * Revision 1.1  2004/06/22 06:56:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.extensions.sap;

import com.sap.mw.jco.JCO;
import com.sap.mw.idoc.IDoc;

/**
 * The interface clients (users) of a SAP function must implement.
 *
 * @version Id
 * @author Gerrit van Brakel
 */
public interface SapFunctionHandler {
	public static final String version="$Id: SapFunctionHandler.java,v 1.2 2008-01-29 15:40:20 europe\L190409 Exp $";
	
	public void processFunctionCall(JCO.Function function) throws SapException;
	public void processIDoc(IDoc.Document idoc) throws SapException;
}
