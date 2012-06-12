/*
 * $Log: SapListener.java,v $
 * Revision 1.2  2012-06-12 15:08:29  m00f069
 * Implement JCoQueuedIDocHandler
 *
 * Revision 1.1  2012/02/06 14:33:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 * Revision 1.14  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.12  2008/01/29 15:39:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for idocs
 *
 * Revision 1.11  2007/10/03 08:35:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.10  2007/06/07 15:18:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now implements HasPhysicalDestination
 *
 * Revision 1.9  2006/01/05 13:59:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2005/12/28 08:42:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.7  2005/08/10 12:44:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * do close() if open() fails
 *
 * Revision 1.6  2005/08/08 09:42:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked SAP classes to provide better refresh of repository when needed
 *
 * Revision 1.5  2005/03/14 17:27:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increased logging
 *
 * Revision 1.4  2005/03/10 14:48:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed trace setting
 *
 * Revision 1.3  2004/10/05 10:41:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.2  2004/08/23 13:11:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.1  2004/08/23 07:12:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed SapMessagePusher to SapListener
 *
 * Revision 1.4  2004/08/09 13:56:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ServiceClient to MessageHandler
 *
 * Revision 1.3  2004/07/19 09:45:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getLogPrefix()
 *
 * Revision 1.2  2004/07/15 07:47:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed IMessagePusher to IPushingListener
 *
 * Revision 1.1  2004/07/06 07:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SAP functionality to extensions
 *
 * Revision 1.2  2004/06/30 12:38:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * included exceptionlistener
 *
 * Revision 1.1  2004/06/22 12:19:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of SAP message pusher
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3;

import java.util.Map;
import java.util.Properties;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocDocumentIterator;
import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.IDocXMLProcessor;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.idoc.jco.JCoIDocHandlerFactory;
import com.sap.conn.idoc.jco.JCoIDocServer;
import com.sap.conn.idoc.jco.JCoIDocServerContext;
import com.sap.conn.idoc.jco.JCoQueuedIDocHandler;
import com.sap.conn.jco.AbapClassException;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoRuntimeException;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerTIDHandler;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IPushingListener},
 * that enables a GenericReceiver to receive messages from SAP-systems. 
 * 
 * In SAP the function to be called is a RFC-function to the destination that is registered using <code>progid</code>.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.sap.SapListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the Listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProgid(String) progid}</td><td>Name of the RFC-destination to be registered in the SAP system</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConnectionCount(String) connectionCount}</td><td>The number of connections that should be registered at the gateway</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the SapSystem used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldIndex(int) correlationIdFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>0</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldName(String) correlationIdFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRequestFieldIndex(int) requestFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>0</td></tr>
 * <tr><td>{@link #setRequestFieldName(String) requestFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplyFieldIndex(int) replyFieldIndex}</td><td>Index of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>0</td></tr>
 * <tr><td>{@link #setReplyFieldName(String) replyFieldName}</td><td>Name of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>&nbsp;</td></tr>
 * </table>
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted to xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted from xml. 
 * </p>
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 * @see   http://help.sap.com/saphelp_nw04/helpdata/en/09/c88442a07b0e53e10000000a155106/frameset.htm
 */
public class SapListener extends SapFunctionFacade implements IPushingListener, JCoServerFunctionHandler, JCoServerTIDHandler, JCoIDocHandlerFactory, JCoIDocHandler, JCoQueuedIDocHandler, JCoServerExceptionListener, JCoServerErrorListener, ServerDataProvider {
	public static final String version="$RCSfile: SapListener.java,v $  $Revision: 1.2 $ $Date: 2012-06-12 15:08:29 $";

	private String progid;	 // progid of the RFC-destination
	private String connectionCount = "2"; // used in SAP examples

	private SapSystem sapSystem;
	private IMessageHandler handler;
	private IbisExceptionListener exceptionListener;

	private DefaultServerHandlerFactory.FunctionHandlerFactory functionHandlerFactory;
	private ServerDataEventListener serverDataEventListener;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getProgid())) {
			throw new ConfigurationException("attribute progid must be specified");
		}
		super.configure();
		sapSystem=SapSystem.getSystem(getSapSystemName());
		functionHandlerFactory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
		functionHandlerFactory.registerGenericHandler(this);
	}


	public void open() throws ListenerException {
		try {
			openFacade();
			log.debug(getLogPrefix()+"register ServerDataProvider");
			Environment.registerServerDataProvider(this);
			serverDataEventListener.updated(getName());
			log.debug(getLogPrefix()+"start server");
			JCoIDocServer server = JCoIDoc.getServer(getName()); 
			server.setCallHandlerFactory(functionHandlerFactory);
			server.setIDocHandlerFactory(this);
			server.setTIDHandler(this);
			server.addServerErrorListener(this);
			server.addServerExceptionListener(this);
			server.start();
		} catch (Exception e) {
			try {
				close();
			} catch (Exception e2) {
				log.warn("exception closing SapListener after exception opening listener",e2);
			}
			throw new ListenerException(getLogPrefix()+"could not start", e);
		}
	}

	public void close() throws ListenerException {
		try {
			log.debug(getLogPrefix()+"stop server");
			JCoServer server = JCoServerFactory.getServer(getName());
			server.stop();
			log.debug(getLogPrefix()+"unregister ServerDataProvider");
			// Delete doesn't work after stopping the server, when calling
			// delete first the stop method will fail.
			// serverDataEventListener.deleted(getName());
			Environment.unregisterServerDataProvider(this);
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"could not stop", e);
		} finally {
			closeFacade();
		}
	}

	public Properties getServerProperties(String arg0) {
		Properties serverProperties = new Properties();
		serverProperties.setProperty(ServerDataProvider.JCO_GWHOST, sapSystem.getGwhost());
		serverProperties.setProperty(ServerDataProvider.JCO_GWSERV, sapSystem.getGwserv());
		serverProperties.setProperty(ServerDataProvider.JCO_PROGID, progid);
		serverProperties.setProperty(ServerDataProvider.JCO_REP_DEST, sapSystem.getName());
		serverProperties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, connectionCount);
		return serverProperties;
	}

	public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx) {
		return this;
	}

	public void setServerDataEventListener(ServerDataEventListener serverDataEventListener) {
		this.serverDataEventListener = serverDataEventListener;
	}

	public boolean supportsEvents() {
		return true;
	}

	public String getPhysicalDestinationName() {
		return "progid ["+getProgid()+"] on "+super.getPhysicalDestinationName();
	}


	public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		log.debug("SapListener.getCorrelationIdFromField");
		return getCorrelationIdFromField((JCoFunction) rawMessage);
	}

	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		log.debug("SapListener.getStringFromRawMessage");
		return functionCall2message((JCoFunction) rawMessage);
	}

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map threadContext) throws ListenerException {
		try {
			log.debug("SapListener.afterMessageProcessed");
			message2FunctionResult((JCoFunction) rawMessage, processResult.getResult());
		} catch (SapException e) {
			throw new ListenerException(e);
		}
	}

	public void handleRequest(JCoServerContext jcoServerContext, JCoFunction jcoFunction)
			throws AbapException, AbapClassException {
		try {
			log.debug("SapListener.handleRequest()");
			handler.processRawMessage(this, jcoFunction, null);
		} catch (Throwable t) {
			log.warn(getLogPrefix()+"Exception caught and handed to SAP",t);
			throw new AbapException("IbisException", t.getMessage());
		}
	}

	public void handleRequest(JCoServerContext serverCtx, IDocDocumentList documentList) {
		log.debug(getLogPrefix()+"Incoming IDoc list request containing " + documentList.getNumDocuments() + " documents...");
		IDocXMLProcessor xmlProcessor = JCoIDoc.getIDocFactory().getIDocXMLProcessor();
		IDocDocumentIterator iterator = documentList.iterator();
		IDocDocument doc = null;
		while (iterator.hasNext()) {
			doc = iterator.next();
			log.debug(getLogPrefix()+"Processing document no. [" + doc.getIDocNumber() + "] of type ["+doc.getIDocType()+"]");
			try {
				handler.processRequest(this, xmlProcessor.render(doc));
			} catch (Throwable t) {
				log.warn(getLogPrefix()+"Exception caught and handed to SAP",t);
				throw new JCoRuntimeException(JCoException.JCO_ERROR_APPLICATION_EXCEPTION, "IbisException", t.getMessage());
			}
		}
	}

	public void handleRequest(JCoIDocServerContext serverCtx, IDocDocumentList documentList) {
		handleRequest(serverCtx, documentList);
	}

	/**
	 * The <code>toString()</code> method retrieves its value
  	 * by reflection.
  	 * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
  	 *
  	 **/
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getProgid() {
		return progid;
	}

	public void setProgid(String string) {
		progid = string;
	}

	public String getConnectionCount() {
		return connectionCount;
	}

	public void setConnectionCount(String connectionCount) {
		this.connectionCount = connectionCount;
	}

	public void setHandler(IMessageHandler handler) {
		this.handler=handler;
	}

	public void setExceptionListener(IbisExceptionListener listener) {
		exceptionListener = listener;
	}

	public void serverExceptionOccurred(JCoServer server, String connectionID,
			JCoServerContextInfo serverCtx, Exception e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException(getLogPrefix()+"exception in SapServer ["+progid+"]",e));
		}
	}

	public void serverErrorOccurred(JCoServer server, String connectionID,
			JCoServerContextInfo serverCtx, Error e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException(getLogPrefix()+"error in SapServer ["+progid+"]",e));
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
	public boolean checkTID(JCoServerContext serverCtx, String tid) {
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
	public void confirmTID(JCoServerContext serverCtx, String tid) {
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
	public void commit(JCoServerContext serverCtx, String tid) {
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
	public void rollback(JCoServerContext serverCtx, String tid) {
		if (log.isDebugEnabled()) {
			log.warn(getLogPrefix()+"is requested to rollback TID ["+tid+"]; (currently ignored)");
		}
	}

}
