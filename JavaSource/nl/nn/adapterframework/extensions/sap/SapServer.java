/* 
 * $Log: SapServer.java,v $
 * Revision 1.11  2010-04-26 14:08:01  m168309
 * support Unicode SAP system
 *
 * Revision 1.10  2008/01/30 14:42:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified javadoc
 *
 * Revision 1.9  2008/01/29 15:36:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for idocs
 *
 * Revision 1.8  2007/02/12 13:47:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.7  2005/08/10 11:31:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * no abort() before stopping server
 *
 * Revision 1.6  2005/08/08 09:42:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked SAP classes to provide better refresh of repository when needed
 *
 * Revision 1.5  2005/08/02 13:03:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.4  2005/08/02 13:01:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * included logging in transaction handling functions
 *
 * Revision 1.3  2005/03/14 17:27:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increased logging
 *
 * Revision 1.2  2004/10/05 10:41:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.1  2004/07/06 07:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SAP functionality to extensions
 *
 * Revision 1.3  2004/06/30 12:38:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed self from exceptionlisteners
 *
 * Revision 1.2  2004/06/23 11:40:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * included error-logging and added transaction-related function-stubs
 *
 * Revision 1.1  2004/06/22 06:56:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.extensions.sap.SapFunctionHandler;
import nl.nn.adapterframework.extensions.sap.SapSystem;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

import com.sap.mw.idoc.IDoc;
import com.sap.mw.idoc.jco.JCoIDoc;
import com.sap.mw.jco.JCO;


/**
 * Object that acts as a SAP.server to receive iDocs and RFC-function from SAP.
 * 
 * @author  Gerrit van Brakel
 * @since   4.2
 * @version Id
 */
public class SapServer extends JCoIDoc.Server implements JCO.ServerExceptionListener, JCO.ServerErrorListener {
	protected Logger log = LogUtil.getLogger(this);
	
	private SapFunctionHandler handler = null;
	private SapSystem system;
	
	public SapServer(SapSystem system, String progid, SapFunctionHandler handler) {
		super(system.getGwhost(), system.getGwserv(), progid, system.getJcoRepository(), system.getIDocRepository());
		this.setProperty("jco.server.unicode", system.isUnicode()?"1":"0");
		this.handler = handler;
		this.system=system;
		log.info(getLogPrefix()+"connected to ["+system.getGwhost()+":"+system.getGwserv()+"] with unicode ["+system.isUnicode()+"]");

//		JCO.addServerExceptionListener(this);
//		JCO.addServerErrorListener(this);
	}
  	
	public void stop() {
		//abort("Ibis disconnects");
		super.stop();
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
			log.info(getLogPrefix()+"sap function called: "+function.getName());
			handler.processFunctionCall(function);
		} catch (Throwable t) {
			log.warn(getLogPrefix()+"Exception caught and handed to SAP",t);
			throw new JCO.AbapException("IbisException", t.getMessage());
		}
	}
	
	protected void handleRequest(IDoc.DocumentList documentList)
	{
		log.debug(getLogPrefix()+"Incoming IDoc list request containing " + documentList.getNumDocuments() + " documents...");

		IDoc.DocumentIterator iterator = documentList.iterator();
		IDoc.Document doc = null;

		while (iterator.hasNext())
		{
			doc = iterator.nextDocument();
			log.debug(getLogPrefix()+"Processing document no. [" + doc.getIDocNumber() + "] of type ["+doc.getIDocType()+"]");

			try {
				handler.processIDoc(doc);
			} catch (Throwable t) {
				log.warn(getLogPrefix()+"Exception caught and handed to SAP",t);
				throw new JCO.AbapException("IbisException", t.getMessage());
			}
		}
	}
	

	/**
	 *  SAP JCo.Server javadoc says:
	 *  This function will be invoked when a transactional RFC is being called from a
	 *  SAP R/3 system. The function has to store the TID in permanent storage and return <code>true</code>.
	 *  The method has to return <code>false</code> if the a transaction with this ID has already
	 *  been process. Throw an exception if anything goes wrong. The transaction processing will be
	 *  aborted thereafter.<b>
	 *  Derived servers must override this method to actually implement the transaction ID management.
	 *  @param tid the transaction ID
	 *  @return <code>true</code> if the ID is valid and not in use otherwise, <code>false</code> otherwise
	 */
	protected boolean onCheckTID(String tid)
	{
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"is requested to check TID ["+tid+"]; (currently ignored)");
		}
		return true;
	}

	/**
	 *  SAP JCo.Server javadoc says:
	 *  This function will be called after the <em>local</em> transaction has been completed.
	 *  All resources assiciated with this TID can be released.<b>
	 *  Derived servers must override this method to actually implement the transaction ID management.
	 *  @param tid the transaction ID
	 */
	protected void onConfirmTID(String tid)
	{
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"is requested to confirm TID ["+tid+"]; (currently ignored)");
		}
	}

	/**
	 *  SAP JCo.Server javadoc says:
	 *  This function will be called after <em>all</em> RFC functions belonging to a certain transaction
	 *  have been successfully completed. <b>
	 *  Derived servers can override this method to locally commit the transaction.
	 *  @param tid the transaction ID
	 */
	protected void onCommit(String tid)
	{
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"is requested to commit TID ["+tid+"]; (currently ignored)");
		}
	}

	/**
	 *  SAP JCo.Server javadoc says:
	 *  This function will be called if an error in one of the RFC functions belonging to
	 *  a certain transaction has occurred.<b>
	 *  Derived servers can override this method to locally rollback the transaction.
	 *  @param tid the transaction ID
	 */
	protected void onRollback(String tid)
	{
		if (log.isDebugEnabled()) {
			log.warn(getLogPrefix()+"is requested to rollback TID ["+tid+"]; (currently ignored)");
		}
	}

	

	public void serverExceptionOccurred(JCO.Server server, Exception e) {
		log.error(getLogPrefix()+"exception occurred", e);
	}

	public void serverErrorOccurred(JCO.Server server, Error err)
	{
		log.error(getLogPrefix()+"error occurred", err);
	}

	protected String getLogPrefix() {
		return system.getLogPrefix()+ "server ["+getProgID()+"] ";
	}

}