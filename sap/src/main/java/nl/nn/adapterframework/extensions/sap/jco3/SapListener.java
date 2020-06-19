/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.sap.jco3;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.extensions.sap.ISapListener;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.stream.Message;

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
 * <tr><td>{@link #setConnectionCount(String) connectionCount}</td><td>The number of connections that should be registered at the gateway</td><td>2</td></tr>
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
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 * @since 5.0
 * @see "http://help.sap.com/saphelp_nw04/helpdata/en/09/c88442a07b0e53e10000000a155106/frameset.htm"
 */
public class SapListener extends SapFunctionFacade implements ISapListener<JCoFunction>, JCoServerFunctionHandler, JCoServerTIDHandler, JCoIDocHandlerFactory, JCoIDocHandler, JCoQueuedIDocHandler, JCoServerExceptionListener, JCoServerErrorListener, ServerDataProvider {

	private String progid;	 // progid of the RFC-destination
	private String connectionCount = "2"; // used in SAP examples

	private SapSystem sapSystem;
	private IMessageHandler<JCoFunction> handler;
	private IbisExceptionListener exceptionListener;

	private DefaultServerHandlerFactory.FunctionHandlerFactory functionHandlerFactory;
	private ServerDataEventListener serverDataEventListener;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getProgid())) {
			throw new ConfigurationException("attribute progid must be specified");
		}
		super.configure();
		sapSystem=SapSystem.getSystem(getSapSystemName());
		functionHandlerFactory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
		functionHandlerFactory.registerGenericHandler(this);
	}

	@Override
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

	@Override
	public void close() throws ListenerException {
		try {
			log.debug(getLogPrefix()+"stop server");
			serverDataEventListener.deleted(getName());

			JCoServer server = JCoServerFactory.getServer(getName());
			server.stop();

			log.debug(getLogPrefix()+"unregister ServerDataProvider");
			// Delete doesn't work after stopping the server, when calling
			// delete first the stop method will fail.
			// serverDataEventListener.deleted(getName());
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"could not stop", e);
		} finally {
			try {
				Environment.unregisterServerDataProvider(this);
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"could not unregister", e);
			} finally {
				closeFacade();
			}
		}
	}

	@Override
	public Properties getServerProperties(String arg0) {
		Properties serverProperties = new Properties();
		serverProperties.setProperty(ServerDataProvider.JCO_GWHOST, sapSystem.getGwhost());
		serverProperties.setProperty(ServerDataProvider.JCO_GWSERV, sapSystem.getGwserv());
		serverProperties.setProperty(ServerDataProvider.JCO_PROGID, progid);
		serverProperties.setProperty(ServerDataProvider.JCO_REP_DEST, sapSystem.getName());
		serverProperties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, connectionCount);

		if(sapSystem.isSncEncrypted()) {
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_MODE, "1");
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_LIBRARY, sapSystem.getSncLibrary());
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_MYNAME, sapSystem.getMyName());
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_QOP, sapSystem.getSncQop());
		}

		return serverProperties;
	}

	@Override
	public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx) {
		return this;
	}

	@Override
	public void setServerDataEventListener(ServerDataEventListener serverDataEventListener) {
		log.debug("setting new serverDataEventListener ["+serverDataEventListener.toString()+"]");
		this.serverDataEventListener = serverDataEventListener;
	}

	@Override
	public boolean supportsEvents() {
		return true;
	}

	@Override
	public String getPhysicalDestinationName() {
		return "progid ["+getProgid()+"] on "+super.getPhysicalDestinationName();
	}


	@Override
	public String getIdFromRawMessage(JCoFunction rawMessage, Map<String,Object> threadContext) throws ListenerException {
		return getCorrelationIdFromField(rawMessage);
	}

	@Override
	public Message extractMessage(JCoFunction rawMessage, Map<String,Object> threadContext) throws ListenerException {
		return functionCall2message(rawMessage);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String,Object> threadContext) throws ListenerException {
		try {
			if (rawMessageOrWrapper instanceof JCoFunction) {
				message2FunctionResult((JCoFunction)rawMessageOrWrapper, processResult.getResult().asString());
			}
		} catch (SapException | IOException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void handleRequest(JCoServerContext jcoServerContext, JCoFunction jcoFunction) throws AbapException, AbapClassException {
		try {
			handler.processRawMessage(this, jcoFunction, null);
		} catch (Throwable t) {
			log.warn(getLogPrefix()+"Exception caught and handed to SAP",t);
			throw new AbapException("IbisException", t.getMessage());
		}
	}

	@Override
	public void handleRequest(JCoServerContext serverCtx, IDocDocumentList documentList) {
		if(log.isDebugEnabled()) log.debug(getLogPrefix()+"Incoming IDoc list request containing " + documentList.getNumDocuments() + " documents...");
		IDocXMLProcessor xmlProcessor = JCoIDoc.getIDocFactory().getIDocXMLProcessor();
		IDocDocumentIterator iterator = documentList.iterator();
		IDocDocument doc = null;
		while (iterator.hasNext()) {
			doc = iterator.next();
			if(log.isTraceEnabled()) log.trace(getLogPrefix()+"Processing document no. [" + doc.getIDocNumber() + "] of type ["+doc.getIDocType()+"]");
			try {
				handler.processRequest(this, null, new Message(xmlProcessor.render(doc)));
			} catch (Throwable t) {
				log.warn(getLogPrefix()+"Exception caught and handed to SAP",t);
				throw new JCoRuntimeException(JCoException.JCO_ERROR_APPLICATION_EXCEPTION, "IbisException", t.getMessage());
			}
		}
	}

	@Override
	public void handleRequest(JCoIDocServerContext idocServerCtx, IDocDocumentList documentList) {
		handleRequest(idocServerCtx.getJCoServerContext(), documentList);
	}

	/**
	 * The <code>toString()</code> method retrieves its value
  	 * by reflection.
  	 * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
  	 *
  	 **/
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getProgid() {
		return progid;
	}

	@Override
	public void setProgid(String string) {
		progid = string;
	}

	public String getConnectionCount() {
		return connectionCount;
	}

	@Override
	public void setConnectionCount(String connectionCount) {
		this.connectionCount = connectionCount;
	}

	@Override
	public void setHandler(IMessageHandler<JCoFunction> handler) {
		this.handler=handler;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		exceptionListener = listener;
	}

	@Override
	public void serverExceptionOccurred(JCoServer server, String connectionID, JCoServerContextInfo serverCtx, Exception e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException(getLogPrefix()+"exception in SapServer ["+progid+"]",e));
		}
	}

	@Override
	public void serverErrorOccurred(JCoServer server, String connectionID, JCoServerContextInfo serverCtx, Error e) {
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
	@Override
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
	@Override
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
	@Override
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
	@Override
	public void rollback(JCoServerContext serverCtx, String tid) {
		if (log.isDebugEnabled()) {
			log.warn(getLogPrefix()+"is requested to rollback TID ["+tid+"]; (currently ignored)");
		}
	}

	/**
	 * We don't use functions when receiving SAP messages
	 */
	@Override
	protected String getFunctionName() {
		return null;
	}
}
