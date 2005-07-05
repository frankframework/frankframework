/*
 * $Log: ServiceListener.java,v $
 * Revision 1.6  2005-07-05 11:17:18  europe\L190409
 * corrected version-string
 *
 * Revision 1.5  2005/07/05 11:15:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added deprecation message
 *
 */
package nl.nn.adapterframework.receivers;

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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IReceiver Receiver} (not a {@link nl.nn.adapterframework.core.IPullingListener listener}!),
 * that enable <code>IAdapters</code> to receive messages by generic services or by web-services.
 * <p>Note: at this moment the state of the adapter is not handled. Only the result
 * from calling the adapter (<code>PipeLineResult.getResult()</code> is retrieved,
 * not <code>PipeLineResult.getState()</code>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.ServiceListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnIfStopped(String) returnIfStopped}</td><td>message to be returned in case the listener is closed</td><td>continue</td></tr>
 * </table>
 * @version Id
 * @author Johan Verrips IOS
 * @see nl.nn.adapterframework.core.IAdapter
 * @see nl.nn.adapterframework.core.IReceiver
 * @see nl.nn.adapterframework.core.PipeLineResult
 * @deprecated Please use GenericReceiver with nested WebServiceListener instead.
 */
public class ServiceListener  implements IReceiver, IReceiverStatistics, HasSender, ServiceClient, Serializable {
	public static final String version = "$RCSfile: ServiceListener.java,v $ $Revision: 1.6 $ $Date: 2005-07-05 11:17:18 $";
        	
	private IAdapter adapter;
	private RunStateManager runState = new RunStateManager();
	private String name;
	protected Logger log = Logger.getLogger(this.getClass());;
	private String returnIfStopped="";
	private ISender sender;
	private Counter msgReceived= new Counter(0);

	private StatisticsKeeper processStatistics = new StatisticsKeeper("general");
/**
 * WebServiceListener constructor comment.
 */
public ServiceListener() {
	
	super();
}
/**
 * initialize listener and register <code>this</code> to the JNDI
 */
public void configure() throws ConfigurationException {
	try {
	    log.debug("registering listener ["+name+"] with ServiceDispatcher");
		adapter.getMessageKeeper().add("WARNING: the class nl.nn.adapterframework.receivers.ServiceListener is deprecated. "+
										"Please use GenericReceiver with nested WebServiceListener instead.");
 
        ServiceDispatcher.getInstance().registerServiceListener(this);
    
	} catch (Exception e){
		throw new ConfigurationException(e);
	
	}
}
public Iterator getIdleStatisticsIterator() {
    return null;
}
public long getMessagesReceived() {
    return msgReceived.getValue();
}
/**
 * Returns the name of the Listener. 
 */
public String getName() {
	return name;
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
    public ISender getSender(){
      return sender;
    }
/**
 * Process the received message with {@link #processRequest(String, String)}.
 * A messageId is generated that is unique and consists of the name of this listener and a GUID
 */
public String processRequest(String request) {
 	String correlationId=getName()+"-"+Misc.createSimpleUUID();
	    if (log.isDebugEnabled()) 
		    log.debug("["+getName()+"] generating correlationId ["+correlationId+"] for request ["+request+"]");
 

    return processRequest(correlationId, request);
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
    
    if (getRunState().equals(RunStateEnum.STARTED)) {
    	PipeLineResult pipeLineResult=adapter.processMessage(correlationId, request);
        answer = pipeLineResult.getResult();
        state = pipeLineResult.getState();
	    if (log.isDebugEnabled()) 
	        log.debug("["+getName()+"] correlationId ["+correlationId+"] execution of pipeline returned result ["+answer+"] with exit-state ["+state+"]");
    }
    else
        answer = returnIfStopped;

    if (getSender() != null) {
        try {
            getSender().sendMessage(correlationId, answer);
        } catch (Exception se) {
            log.error(
                "ServiceListener [" + name + "] Error occured on sendMessage. closing down adapter",se);
            adapter.getMessageKeeper().add(
                "Error occured while sending message. Closing adapter.");
            if (adapter != null) {
                try {
                    adapter.stopRunning();
                } catch (Exception e) {
                    log.error(
                        "ServiceListener-receiver [" + getName() + "] failed to close adapter [" + adapter.getName() + "] afte errors on sender.", e);
                }
            }
            log.error("closing ServiceListener-receiver [" + getName() + "] after errors on sender"); 
            stopRunning();

        }
    }
	if (answer==null) {
		log.warn("ServiceListener [" + name + "] has null as a answer, setting to empty string");
		answer="";
	}
    long duration = System.currentTimeMillis() - startProcessingTimestamp;

   	synchronized (processStatistics) {
		processStatistics.addValue(duration);
	}

    return answer;	
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
 * Sets the name of the Listener. Under this name the receiver is stored
 * in the JNDI
 */
public void setName(String name) {
	this.name=name;
	}
	/**
	 * Return this value when this receiver is stopped.
	 */
	public void setReturnIfStopped (String returnIfStopped){
		this.returnIfStopped=returnIfStopped;
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
                    + name
                    + "] on adapter ["
                    + adapter.getName()
                    + "] was tried to start, but the adapter is in state ["+currentRunState+"]. Ignoring command.");
            adapter.getMessageKeeper().add(
                "ignored start command on [" + name + "]; adapter is in state ["+currentRunState+"]");
            return;
        }
   }

   runState.setRunState(RunStateEnum.STARTED);

   if (adapter != null) {
        adapter.getMessageKeeper().add("[" + name + "] started listening");
   }
}
 /**
     * The receiver is ordered to stop listening by someone.
     */
public void stopRunning() {

	runState.setRunState(RunStateEnum.STOPPED);
	if (adapter!=null) {
		adapter.getMessageKeeper().add("receiver ["+getName()+"] stopped");
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
  
	public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
		runState.waitForRunState(requestedRunState);
	}

	public boolean waitForRunState(RunStateEnum requestedRunState, long timeout) throws InterruptedException {
		return runState.waitForRunState(requestedRunState, timeout);
	}
}
