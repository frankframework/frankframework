/*
 * $Log: SapMessagePusher.java,v $
 * Revision 1.2  2004-07-15 07:47:12  L190409
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
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;

import com.sap.mw.jco.*;
import com.sap.mw.jco.JCO.Server;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IMessagePusher pushing listener},
 * that enables a PushingReceiverBase to receive messages from SAP-systems. In SAP the function to be called is a RFC-function to the destination
 * that is registered using progid.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProgid(String) progid}</td><td>Name of the RFC-destination to be registered in the SAP system</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) sapSystemName}</td><td>name of the SapSystem used by this object</td><td>&nbsp;</td></tr>
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
 * @since 4.2
 */
public class SapMessagePusher extends SapFunctionFacade implements IPushingListener, SapFunctionHandler, JCO.ServerExceptionListener, JCO.ServerErrorListener {
	public static final String version="$Id: SapMessagePusher.java,v 1.2 2004-07-15 07:47:12 L190409 Exp $";

	private String progid;	 // progid of the RFC-destination
        	
	private SapServer sapServer;
	private ServiceClient handler;
	private IbisExceptionListener exceptionListener;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		try {
			super.configure();
			sapServer = new SapServer(getSapSystem(), getProgid(), this);
	
	 	} catch (Exception e){
			throw new ConfigurationException(e);
		
		}
	}

	public void open() throws ListenerException {
		try {
			openFacade();
			sapServer.setTrace(true);
			sapServer.start();
		} catch (Exception e) {
			throw new ListenerException("["+getName()+"] could not start", e);
		}
	}
	
	public void close() throws ListenerException {
		try {
			sapServer.suspend();
			closeFacade();
		} catch (Exception e) {
			throw new ListenerException("["+getName()+"] could not stop", e);
		}
	}


	public void processFunctionCall(JCO.Function function) throws SapException {
		String request = functionCall2message(function);
		String correlationId = getCorrelationIdFromField(function);
		String result = handler.processRequest(correlationId, request);
		message2FunctionResult(function, result);
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


	public void setHandler(ServiceClient handler) {
		this.handler=handler;
	}

	public void setExceptionListener(IbisExceptionListener listener) {
		exceptionListener = listener;
		JCO.addServerExceptionListener(this);
		JCO.addServerErrorListener(this);
	}

	public void serverExceptionOccurred(JCO.Server server, Exception e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException("exception in SapServer ["+server.getProgID()+"]",e));
		}
	}

	public void serverErrorOccurred(JCO.Server server, Error e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException("error in SapServer ["+server.getProgID()+"]",e));
		}
	}

	public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		return null;
	}

	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		return functionCall2message((JCO.Function) rawMessage);
	}

}
