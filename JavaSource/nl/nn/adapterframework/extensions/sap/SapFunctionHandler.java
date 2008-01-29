/*
 * $Log: SapFunctionHandler.java,v $
 * Revision 1.3  2008-01-29 15:41:13  europe\L190409
 * removed version string
 *
 * Revision 1.2  2008/01/29 15:40:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @author  Gerrit van Brakel
 * @version Id
 */
public interface SapFunctionHandler {

	public void processFunctionCall(JCO.Function function) throws SapException;
	public void processIDoc(IDoc.Document idoc) throws SapException;
}
