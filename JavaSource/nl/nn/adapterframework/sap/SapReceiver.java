package nl.nn.adapterframework.sap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.StatisticsKeeper;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.Iterator;

import com.sap.mw.jco.*;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IReceiver Receiver} (not a {@link nl.nn.adapterframework.core.IPullingListener listener}!),
 * that enable <code>IAdapters</code> to receive messages from SAP-systems. In SAP the function to be called is a RFC-function to the destination
 * that is registerd using progid.
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
 * @since 4.1.1
 */
public class SapReceiver extends SapFunctionFacade implements IReceiver, IReceiverStatistics, SapFunctionHandler {
	public static final String version="$Id: SapReceiver.java,v 1.1 2004-06-22 06:56:44 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;

	private String progid;	 // progid of the RFC-destination
        	
	private IAdapter adapter;
	private RunStateManager runState = new RunStateManager();
	private String returnIfStopped="";
	private Counter msgReceived= new Counter(0);

	private StatisticsKeeper processStatistics = new StatisticsKeeper("general");
	private SapServer sapServer;

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

/**
 * Start the receiver. The thread-name will be set to the receiver name
 */
public void startRunning() {
   if (adapter != null) {
		RunStateEnum currentRunState = adapter.getRunState();
		if (!currentRunState.equals(RunStateEnum.STARTED)) {
			log.warn(
				"Receiver ["
					+ getName()
					+ "] on adapter ["
					+ adapter.getName()
					+ "] was tried to start, but the adapter is in state ["+currentRunState+"]. Ignoring command.");
			adapter.getMessageKeeper().add(
				"ignored start command on [" + getName() + "]; adapter is in state ["+currentRunState+"]");
			return;
		}
   }
   try {
		openFacade();
		sapServer.setTrace(true);
	   	sapServer.start();
	   	runState.setRunState(RunStateEnum.STARTED);
		if (adapter != null) {
			adapter.getMessageKeeper().add("[" + getName() + "] started listening");
		}
   }
   catch (Exception e) {
		runState.setRunState(RunStateEnum.ERROR);
	 	log.error("Could not start SapServer " + sapServer.getProgID(), e);
	if (adapter != null) {
		 adapter.getMessageKeeper().add("[" + getName() + "] could not start listening: "+e.getMessage());
   }



   }
}
 /**
	 * The receiver is ordered to stop listening by someone.
	 */
public void stopRunning() {
	sapServer.suspend();
	closeFacade();
	runState.setRunState(RunStateEnum.STOPPED);
	if (adapter!=null) {
		adapter.getMessageKeeper().add("receiver ["+getName()+"] stopped");
	}
}

public void processFunctionCall(JCO.Function function) throws SapException {
	String request = functionCall2message(function);
	String correlationId = getCorrelationIdFromField(function);
	String result = processRequest(correlationId, request);
	message2FunctionResult(function, result);
}


/**
 * Process the received message.
 * @since 4.0
 */	
public String processRequest(String correlationId, String request) {
	String answer = "";
	String state = "";

	long startProcessingTimestamp = System.currentTimeMillis();
	msgReceived.increase();
    
    if (StringUtils.isEmpty(correlationId)) {
		correlationId=getName()+"-"+Misc.createSimpleUUID();
				if (log.isDebugEnabled()) 
					log.debug("["+getName()+"] generating correlationId ["+correlationId+"] for request ["+request+"]");
     }
    
	if (getRunState().equals(RunStateEnum.STARTED)) {
		PipeLineResult pipeLineResult=adapter.processMessage(correlationId, request);
		answer = pipeLineResult.getResult();
		state = pipeLineResult.getState();
		if (log.isDebugEnabled()) 
			log.debug("["+getName()+"] correlationId ["+correlationId+"] execution of pipeline returned result ["+answer+"] with exit-state ["+state+"]");
	}
	else
		answer = returnIfStopped;

	if (answer==null) {
		log.warn("SapReceiver [" + getName() + "] has null as a answer, setting to empty string");
		answer="";
	}
	long duration = System.currentTimeMillis() - startProcessingTimestamp;

	synchronized (processStatistics) {
		processStatistics.addValue(duration);
	}

	return answer;	
}


public Iterator getIdleStatisticsIterator() {
    return null;
}
public long getMessagesReceived() {
    return msgReceived.getValue();
}


public Iterator getProcessStatisticsIterator() {

	class StatisticsIterator implements Iterator {
		private Object p;

		StatisticsIterator(Object p) {
			super();
			this.p = p;
		}

		public boolean hasNext() {
			return p!=null;
		}

		public Object next() {
			Object result = p;
			p=null;
			return result;
		}
		
		public void remove() {
			p=null;
		}
	}
	
    return new StatisticsIterator(processStatistics);
}
	/**
	 * Return this value when this receiver is stopped.
	 */
	public String getReturnIfStopped() {
		return returnIfStopped;
	}
public RunStateEnum getRunState() {
	return runState.getRunState();
}


 /**
     * The processing of messages must be delegated to the <code>Adapter</code>
     * object.
     * @see nl.nn.adapterframework.core.IAdapter
     */
public void setAdapter(IAdapter adapter) {
     this.adapter=adapter;
 }
	/**
	 * Return this value when this receiver is stopped.
	 */
	public void setReturnIfStopped (String returnIfStopped){
		this.returnIfStopped=returnIfStopped;
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
public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
	runState.waitForRunState(requestedRunState);
}
	/**
	 * @return
	 */
	public String getProgid() {
		return progid;
	}

	/**
	 * @param string
	 */
	public void setProgid(String string) {
		progid = string;
	}

}
