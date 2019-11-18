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
package nl.nn.adapterframework.extensions.sap.jco2;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.sap.mw.idoc.IDoc.Document;
import com.sap.mw.jco.JCO;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc; 
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.extensions.sap.ISapListener;
import nl.nn.adapterframework.extensions.sap.SapException;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IPushingListener},
 * that enables a GenericReceiver to receive messages from SAP-systems. 
 * 
 * In SAP the function to be called is a RFC-function to the destination that is registered using <code>progid</code>.
 * 
 * @author  Gerrit van Brakel
 * @since   4.2
 * @see   "http://help.sap.com/saphelp_nw04/helpdata/en/09/c88442a07b0e53e10000000a155106/frameset.htm"
 */
public class SapListener extends SapFunctionFacade implements ISapListener<JCO.Function>, SapFunctionHandler, JCO.ServerExceptionListener, JCO.ServerErrorListener {

	private String progid;	 // progid of the RFC-destination
        	
	private SapServer sapServer;
	private IMessageHandler<JCO.Function> handler;
	private IbisExceptionListener exceptionListener;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getProgid())) {
			throw new ConfigurationException("attribute progid must be specified");
		}
		super.configure();
	}


	@Override
	public void open() throws ListenerException {
		try {
			openFacade();
			sapServer = new SapServer(getSapSystem(), getProgid(), this);
			sapServer.start();
			if (log.isDebugEnabled()) {
				String pi[][] = sapServer.getPropertyInfo();
				log.debug(getLogPrefix()+"properties:");
				for (int i=0; i<pi.length; i++) {
					log.debug(getLogPrefix()+"property ["+pi[i][0]+"] ("+pi[i][1]+") value ("+sapServer.getProperty(pi[i][0])+")");
				}
			}
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
			if (sapServer!=null) {
				sapServer.stop();
				sapServer = null;
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"could not stop", e);
		} finally {
			closeFacade();
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "progid ["+getProgid()+"] on "+super.getPhysicalDestinationName();
	}


	@Override
	public String getIdFromRawMessage(JCO.Function rawMessage, Map<String,Object> threadContext) throws ListenerException {
		log.debug("SapListener.getCorrelationIdFromField");
		return getCorrelationIdFromField(rawMessage);
	}

	@Override
	public String getStringFromRawMessage(JCO.Function rawMessage, Map<String,Object> threadContext) throws ListenerException {
		log.debug("SapListener.getStringFromRawMessage");
		return functionCall2message(rawMessage);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, JCO.Function rawMessage, Map<String,Object> threadContext) throws ListenerException {
		try {
			log.debug("SapListener.afterMessageProcessed");
			message2FunctionResult(rawMessage, processResult.getResult());
		} catch (SapException e) {
			throw new ListenerException(e);
		}
	}

/*
	public void processFunctionCall(JCO.Function function) throws SapException {
		String request = functionCall2message(function);
		String correlationId = getCorrelationIdFromField(function);
		String result = handler.processRequest(correlationId, request);
		message2FunctionResult(function, result);
	}
*/
	@Override
	public void processFunctionCall(JCO.Function function) throws SapException {
		try {
			log.debug("SapListener.procesFunctionCall()");
			handler.processRawMessage(this, function, null);
		} catch (ListenerException e) {
			throw new SapException(e);
		}
	}

	@Override
	public void processIDoc(Document idoc) throws SapException {
		try {
			log.debug("SapListener.processIDoc()");
			handler.processRequest(this, idoc.toXML());
		} catch (ListenerException e) {
			throw new SapException(e);
		}
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
	@IbisDoc({"name of the rfc-destination to be registered in the sap system", " "})
	public void setProgid(String string) {
		progid = string;
	}


	@Override
	public void setHandler(IMessageHandler<JCO.Function> handler) {
		this.handler=handler;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		exceptionListener = listener;
		JCO.addServerExceptionListener(this);
		JCO.addServerErrorListener(this);
	}

	@Override
	public void serverExceptionOccurred(JCO.Server server, Exception e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException(getLogPrefix()+"exception in SapServer ["+server.getProgID()+"]",e));
		}
	}

	@Override
	public void serverErrorOccurred(JCO.Server server, Error e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException(getLogPrefix()+"error in SapServer ["+server.getProgID()+"]",e));
		}
	}


	@Override
	public void setConnectionCount(String connectionCount) {
		log.warn("setConnectionCount() not used by JCo2");
	}

}
