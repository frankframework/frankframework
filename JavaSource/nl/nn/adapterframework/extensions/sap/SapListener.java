/*
 * $Log: SapListener.java,v $
 * Revision 1.10  2007-06-07 15:18:01  europe\L190409
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
package nl.nn.adapterframework.extensions.sap;

import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.sap.mw.jco.JCO;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IPushingListener},
 * that enables a GenericReceiver to receive messages from SAP-systems. In SAP the function to be called is a RFC-function to the destination
 * that is registered using progid.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.sap.SapListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the Listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProgid(String) progid}</td><td>Name of the RFC-destination to be registered in the SAP system</td><td>&nbsp;</td></tr>
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
 * @since 4.2
 * @see   http://help.sap.com/saphelp_nw04/helpdata/en/09/c88442a07b0e53e10000000a155106/frameset.htm
 */
public class SapListener extends SapFunctionFacade implements IPushingListener, SapFunctionHandler, JCO.ServerExceptionListener, JCO.ServerErrorListener {
	public static final String version="$RCSfile: SapListener.java,v $  $Revision: 1.10 $ $Date: 2007-06-07 15:18:01 $";

	private String progid;	 // progid of the RFC-destination
        	
	private SapServer sapServer;
	private IMessageHandler handler;
	private IbisExceptionListener exceptionListener;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getProgid())) {
			throw new ConfigurationException("attribute progid must be specified");
		}
		super.configure();
		
	}


	public void open() throws ListenerException {
		try {
			openFacade();
			sapServer = new SapServer(getSapSystem(), getProgid(), this);
			sapServer.start();
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

	public String getPhysicalDestinationName() {
		return "progid ["+getProgid()+"] on "+super.getPhysicalDestinationName();
	}


	public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		log.debug("SapListener.getCorrelationIdFromField");
		return getCorrelationIdFromField((JCO.Function) rawMessage);
	}

	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		log.debug("SapListener.getStringFromRawMessage");
		return functionCall2message((JCO.Function) rawMessage);
	}

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap threadContext) throws ListenerException {
		try {
			log.debug("SapListener.afterMessageProcessed");
			message2FunctionResult((JCO.Function) rawMessage, processResult.getResult());
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
	public void processFunctionCall(JCO.Function function) throws SapException {
		try {
			log.debug("SapListener.procesFunctionCall()");
			handler.processRawMessage(this, function, null);
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
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getProgid() {
		return progid;
	}

	public void setProgid(String string) {
		progid = string;
	}


	public void setHandler(IMessageHandler handler) {
		this.handler=handler;
	}

	public void setExceptionListener(IbisExceptionListener listener) {
		exceptionListener = listener;
		JCO.addServerExceptionListener(this);
		JCO.addServerErrorListener(this);
	}

	public void serverExceptionOccurred(JCO.Server server, Exception e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException(getLogPrefix()+"exception in SapServer ["+server.getProgID()+"]",e));
		}
	}

	public void serverErrorOccurred(JCO.Server server, Error e) {
		if (exceptionListener!=null) {
			exceptionListener.exceptionThrown(this, new SapException(getLogPrefix()+"error in SapServer ["+server.getProgID()+"]",e));
		}
	}



}
