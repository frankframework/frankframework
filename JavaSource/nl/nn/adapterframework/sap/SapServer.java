/*
 * $Log: SapServer.java,v $
 * Revision 1.1  2004-06-22 06:56:44  L190409
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.sap;

import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.log4j.Logger;

import com.sap.mw.jco.*;

/**
 * Object that acts as a SAP-server. Currently used to receive RFC-function calls from SAP.
 * @author Gerrit van Brakel
 * @since 4.1.1
 */
public class SapServer extends JCO.Server {
	public static final String version="$Id: SapServer.java,v 1.1 2004-06-22 06:56:44 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());
	
	private SapFunctionHandler handler = null;
	
	public SapServer(SapSystem system, String progid, SapFunctionHandler handler) {
		super(system.getGwhost(), system.getGwserv(), progid, system.getRepository());
		this.handler = handler;
		log.info("SapServer connected to ["+system.getGwhost()+":"+system.getGwserv()+"] using progid ["+progid+"]");
	}

	/*
	 *  Not really necessary to override this function but for demonstration purposes...
	 */
	protected JCO.Function getFunction(String function_name)
	{
	  JCO.Function function = super.getFunction(function_name);
	  return function;
	}

	/*
	 *  Not really necessary to override this method but for demonstration purposes...
	 */
	protected boolean checkAuthorization(String function_name, int authorization_mode,
		String authorization_partner, byte[] authorization_key)
	{
	  /*Simply allow everyone to invoke the services */
	  return true;
	}



	protected void handleRequest(JCO.Function function)
	{
		try {
			log.info("sap function called:"+function.getName());
			handler.processFunctionCall(function);
		} catch (SapException e) {
			throw new JCO.AbapException("IbisException", e.getMessage());
		}
	}
 
}
