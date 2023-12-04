/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022-2023 WeAreFrank!

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

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocDocumentIterator;
import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.IDocXMLProcessor;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.idoc.jco.JCoIDocHandlerFactory;
import com.sap.conn.idoc.jco.JCoIDocServer;
import com.sap.conn.idoc.jco.JCoIDocServerContext;
import com.sap.conn.jco.AbapClassException;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoRuntimeException;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerTIDHandler;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.Mandatory;
import nl.nn.adapterframework.extensions.sap.ISapListener;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;

/**
 * Implementation of a {@link IPushingListener},
 * that enables a GenericReceiver to receive messages from SAP-systems.
 *
 * In SAP the function to be called is a RFC-function to the destination that is registered using <code>progid</code>.
 * </b>
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted to xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted from xml.
 * </p>
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 * @since 5.0
 * @see "http://help.sap.com/saphelp_nw04/helpdata/en/09/c88442a07b0e53e10000000a155106/frameset.htm"
 */
public abstract class SapListenerImpl extends SapFunctionFacade implements ISapListener<JCoFunction>, JCoServerFunctionHandler, JCoServerTIDHandler, JCoIDocHandlerFactory, JCoIDocHandler, JCoServerExceptionListener, JCoServerErrorListener {

	private @Getter String progid;	 // progid of the RFC-destination
	private @Getter String connectionCount = "2"; // used in SAP examples

	private IMessageHandler<JCoFunction> handler;
	private IbisExceptionListener exceptionListener;

	private DefaultServerHandlerFactory.FunctionHandlerFactory functionHandlerFactory;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getProgid())) {
			throw new ConfigurationException("attribute progid must be specified");
		}
		super.configure();
		if (SapSystemImpl.getSystem(getSapSystemName())==null) {
			throw new ConfigurationException("unknown SapSystem ["+getSapSystemName()+"]");
		}
		functionHandlerFactory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
		functionHandlerFactory.registerGenericHandler(this);
	}

	@Override
	public void open() throws ListenerException {
		try {
			openFacade();
			SapServerDataProvider serverDataProvider = SapServerDataProvider.getInstance();
			log.debug(getLogPrefix()+"start server");
			serverDataProvider.registerListener(this);
			JCoIDocServer server = JCoIDoc.getServer(getName());
			server.setCallHandlerFactory(functionHandlerFactory);
			server.setIDocHandlerFactory(this);
			server.setTIDHandler(this);
			server.addServerErrorListener(this);
			server.addServerExceptionListener(this);
			server.start();
			serverDataProvider.getServerDataEventListener().updated(getName());
		} catch (Exception e) {
			try {
				close();
			} catch (Exception e2) {
				e.addSuppressed(e2);
			}
			throw new ListenerException(getLogPrefix()+"could not start", e);
		}
	}

	@Override
	public void close() throws ListenerException {
		try {
			log.debug(getLogPrefix()+"stop server");
			SapServerDataProvider.getInstance().getServerDataEventListener().deleted(getName());

			JCoServer server = JCoServerFactory.getServer(getName());
			server.stop();

		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"could not stop", e);
		} finally {
			closeFacade();
		}
	}

	@Override
	public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx) {
		return this;
	}

	@Override
	public String getPhysicalDestinationName() {
		return "progid ["+getProgid()+"] on "+super.getPhysicalDestinationName();
	}

	@Override
	public RawMessageWrapper<JCoFunction> wrapRawMessage(JCoFunction jcoFunction, PipeLineSession session) {
		return new RawMessageWrapper<>(jcoFunction, getCorrelationIdFromField(jcoFunction), null);
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<JCoFunction> rawMessageWrapper, @Nonnull Map<String,Object> context) {
		return functionCall2message(rawMessageWrapper.getRawMessage());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<JCoFunction> rawMessageWrapper, PipeLineSession pipeLineSession) throws ListenerException {
		try {
			if (rawMessageWrapper.getRawMessage() != null) {
				message2FunctionResult(rawMessageWrapper.getRawMessage(), processResult.getResult().asString());
			}
		} catch (SapException | IOException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void handleRequest(JCoServerContext jcoServerContext, JCoFunction jcoFunction) throws AbapException, AbapClassException {
		try (PipeLineSession session = new PipeLineSession()) {
			handler.processRawMessage(this, wrapRawMessage(jcoFunction, session), session, false);
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
		while (iterator.hasNext()) {
			IDocDocument doc = iterator.next();
			if(log.isTraceEnabled()) log.trace(getLogPrefix()+"Processing document no. [" + doc.getIDocNumber() + "] of type ["+doc.getIDocType()+"]");
			try (PipeLineSession session = new PipeLineSession()) {
				String rawMessage = xmlProcessor.render(doc);
				RawMessageWrapper rawMessageWrapper = new RawMessageWrapper<>(rawMessage, doc.getIDocNumber(), null);
				//noinspection unchecked
				handler.processRequest(this, rawMessageWrapper, new Message(rawMessage), session);
			} catch (Throwable t) {
				log.warn(getLogPrefix()+"Exception caught and handed to SAP",t);
				throw new JCoRuntimeException(JCoException.JCO_ERROR_APPLICATION_EXCEPTION, "IbisException", t.getMessage());
			}
		}
	}


	@Override
	@Mandatory
	public void setSapSystemName(String string) {
		super.setSapSystemName(string);
	}

	/** Name of the RFC-destination to be registered in the SAP system */
	@Override
	@Mandatory
	public void setProgid(String string) {
		progid = string;
	}

	/**
	  * The number of connections that should be registered at the gateway
	  * @ff.default 2
	  */
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
