package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This {@link IReceiver Receiver} may be used as a base-class for developing 'pulling' receivers.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>name of the class, mostly a class that extends this class</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>"&nbsp;</td></tr>
 * <tr><td>{@link #setNumThreads(int) numThreads}</td><td>the number of threads listening in parallel for messages</td><td>1</td></tr>
 * <tr><td>{@link #setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply</td><td>continue</td></tr>
 * </table>
 * </p>
 * </p>
 * <p>$Id: PullingReceiverBase.java,v 1.3 2004-03-11 09:23:50 NNVZNL01#L180564 Exp $</p>
 * @author     Gerrit van Brakel
 * @since 4.0
 */
public class PullingReceiverBase
    implements IReceiver, IReceiverStatistics, Runnable, HasSender {
	public static final String version="$Id: PullingReceiverBase.java,v 1.3 2004-03-11 09:23:50 NNVZNL01#L180564 Exp $";
    	

	public static final String ONERROR_CONTINUE = "continue";
	public static final String ONERROR_CLOSE = "close";


	protected Logger log = Logger.getLogger(this.getClass());
  	private String name;
  	private String onError = ONERROR_CONTINUE; 
    private RunStateManager runState = new RunStateManager();
	    
	// the number of threads that listen in parallel to the queue
	private int numThreads = 1;
	private Counter threadsRunning = new Counter(0);
	private Counter threadsProcessing = new Counter(0);
    
	// number of messages received
    private Counter numReceived = new Counter(0);
	private ArrayList processStatistics = new ArrayList();
	private ArrayList idleStatistics = new ArrayList();

    // the adapter that handles the messages and initiates this listener
    private IAdapter adapter;

    private IPullingListener listener;
    /**
     *Constructor for the jms.QueueMessageReceiver object
     */
    public PullingReceiverBase() {
        super();
    }
/**
* this method is called from the run method after the last thread has exited !!<br/>
* The receiver and connection is closed and reset. If a sender
* is configured it is stopped also.
*/
private void closeAllResources() {

    /*
 	    // the setStopped is placed here, to enably trying to start the receiver again.
        this.setStopped();
    */

    try {
        log.debug("closing Receiver ["+ getName()+ "]");
        getListener().close();

        log.info("closed Receiver ["+ getName()+ "]");
    } catch (ListenerException e) {
        log.error(
            "Receiver [" + getName()+ "]: error closing connection", e);
    }

}
public void configure() throws ConfigurationException {

	getListener().configure();
		
	processStatistics.ensureCapacity(getNumThreads());
	idleStatistics.ensureCapacity(getNumThreads());

	for (int i=0; i<getNumThreads(); i++) {
		processStatistics.add(i, new StatisticsKeeper(i+1+" threads processing"));
		idleStatistics.add(i, new StatisticsKeeper(i+" threads processing"));
	}
		
	if (adapter != null) {
		adapter.getMessageKeeper().add("Receiver ["+getName()+"] initialization complete");
	}
}
protected void finishProcessingMessage(long processingDuration) {
	synchronized (threadsProcessing) {
		int threadCount = (int) threadsProcessing.decrease();
		((StatisticsKeeper)processStatistics.get(threadCount)).addValue(processingDuration);
	}
}
/**
 * Returns an iterator over the idle-statistics
 * @return iterator
 */
public Iterator getIdleStatisticsIterator() {
	return idleStatistics.iterator();
}
/**
 * Returns the listener
 * @return IPullingListener
 */
public IPullingListener getListener() {
	return listener;
}
/**
 * Get the number of messages received.
  * @return long
 */
    public long getMessagesReceived() {
	    return numReceived.getValue();
    }
/**
 * Get the name of this receiver
 * @return java.lang.String
 */
public java.lang.String getName() {
	return name;
}
/**
 * Get the number of threads that this receiver is working with.
 * Creation date: (28-10-2003 14:51:19)
 * @return int
 */
public int getNumThreads() {
	return numThreads;
}
/**
 * Insert the method's description here.
 * Creation date: (17-11-2003 15:49:10)
 * @return java.lang.String
 */
public java.lang.String getOnError() {
	return onError;
}
public Iterator getProcessStatisticsIterator() {
	return processStatistics.iterator();
}
public Object getRawMessage(HashMap threadContext) throws ListenerException {
    return getListener().getRawMessage(threadContext);
}
    /**
     * Get the {@link RunStateEnum runstate} of this receiver.
     */
public RunStateEnum getRunState() {
	return runState.getRunState();
}
public ISender getSender() {
	IPullingListener listener = getListener();

	if (listener instanceof HasSender) {		
    	return ((HasSender)listener).getSender();
	} else {
		return null;
	}
}
    /**
     * All messages that for this receiver are pumped down to this method, so it actually
     * callst he {@link nl.nn.adapterframework.core.Adapter adapter} to process the message.<br/>
     * This specific implementation delegates the sending of results to a
     * seperate sender, retrieved from the adapter.
     * @see javax.jms.Message
     * @param  message  message that was received.
     */
    protected PipeLineResult onMessage(String message, String id, HashMap threadContext) {

	    PipeLineResult result = null;
	    
	    String state = "";

        try {
	        if (null!=adapter) {
	          // notify the adapter and send the result to the sender
	          result = adapter.processMessage(id, message);
	          state = result.getState();
	          log.debug("["+getName()+"] proccessed request with exit-state ["+state+"]");
	        }


/*	        
	        if (null != sender) {
		        try{
			        
			        if ((sender instanceof JmsMessageSender) && (useReplyTo)) {
				        
				        Destination replyTo = (Destination)threadContext.get("replyTo");
				        String cid = (String)threadContext.get("cid");
			        	if (replyTo !=null) {
				        	log.debug("sending message to JmsMessageSender with correlationID["+cid+"], replyTo ["+replyTo.toString()+"]");
			            	((JmsMessageSender)sender).sendMessage(replyTo, cid, answer);
			        	} else {
					        log.info("no replyTo address found, using default destination");
					        log.debug("sending message with sender ["+sender.getName()+"] correlationID["+id+"] ["+answer+"]");
					        sender.sendMessage(id, answer);
			        	}
			        } else {
				        log.debug("sending message with sender ["+sender.getName()+"] correlationID["+id+"] ["+answer+"]");
				        sender.sendMessage(id, answer);
			        }
	            	
		        } catch (SendException se) {
			        log.error("Receiver ["+getName()+"] Error occured on sendMessage. closing down adapter "+ToStringBuilder.reflectionToString(se), se);
			        adapter.getMessageKeeper().add("Error occured while sending message. Closing adapter:"+se.getMessage());
			        if (adapter!=null){
			        	try {
				        	adapter.stopRunning();
			        	} catch (Exception e){
				        	log.error("Receiver ["+getName()+"] failed to close adapter ["+adapter.getName()+"] after errors occured on sender.");
				        	this.stopRunning();
				        }
			        }
		        	log.error("Receiver ["+getName()+"] closing down, cause errors occured on sender");
		        	this.stopRunning();
			        
			        
		        }
		        
	        }
*/	        
	        
        } catch (Throwable e) {
	        log.error("Receiver [" + getName() + "]:"+ToStringBuilder.reflectionToString(e,ToStringStyle.MULTI_LINE_STYLE), e);
        }
        return result;
    }
    /**
     * Starts the receiver. This method is called by the startRunning method.<br/>
     * Basically:
     * <ul>
     * <li> it opens the threads</li>
     * <li>it calls the getRawMessage method to get a message<li>
     * <li> it performs the onMessage method, resulting a PipeLineResult</li>
     * <li>it calls the afterMessageProcessed() method of the listener<li>
     * <li> it optionally sends the result using the sender</li>
     * </ul>
     */
public void run() {
    threadsRunning.increase();
	try {		
	    HashMap threadContext = getListener().openThread();
	    if (threadContext==null) {
		    threadContext = new HashMap();
	    }
    
	    long startProcessingTimestamp;
	    long finishProcessingTimestamp = System.currentTimeMillis();

	    runState.setRunState(RunStateEnum.STARTED);
        while (getRunState().equals(RunStateEnum.STARTED)) {
	        Object rawMessage = getRawMessage(threadContext);
	        if (rawMessage!=null) {
		        startProcessingTimestamp = System.currentTimeMillis();
		        startProcessingMessage(startProcessingTimestamp-finishProcessingTimestamp);

		        numReceived.increase();
		        String message = getListener().getStringFromRawMessage(rawMessage, threadContext);
		        String id = getListener().getIdFromRawMessage(rawMessage, threadContext);
		        PipeLineResult pipeLineResult = onMessage(message, id, threadContext);
		        try {
	    			getListener().afterMessageProcessed(pipeLineResult,rawMessage,threadContext);
		        } catch (ListenerException e) {
			        String msg = "receiver [" + getName() + "] caught exception in message post processing ["+e.toString()+"]";
			        log.error(msg, e);
			        if (null != adapter) {
			            adapter.getMessageKeeper().add(msg+":" + e.getMessage());
			        }
			        if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
				        log.info("receiver [" + getName() + "] closing after exception in post processing");
				        stopRunning();
			        }
		        }

		        finishProcessingTimestamp = System.currentTimeMillis();
		        finishProcessingMessage(finishProcessingTimestamp-startProcessingTimestamp);
	        } 
        }
        getListener().closeThread(threadContext);

    } catch (Throwable e) {
        log.error("error occured in receiver [" + getName() + "]: ["+e.toString()+"]", e);
        if (null != adapter)
            adapter.getMessageKeeper().add(
                "error occured on receiver [" + getName() + "]:" + e.getMessage());
	} finally {
	    long stillRunning=threadsRunning.decrease();

        if (stillRunning>0) {
	        log.info("a thread of Receiver ["+getName()+"] exited, ["+stillRunning+"] are still running");
	        return;
        }
        log.info("the last thread of Receiver ["+getName()+"] exited, cleaning up");
	    closeAllResources();

	    if (adapter != null) {
     	   adapter.getMessageKeeper().add("Receiver [" + getName() + "] stopped");
    	}
    
		runState.setRunState(RunStateEnum.STOPPED);
    }
}
    public void setAdapter(IAdapter adapter) {
        this.adapter = adapter;
    }
/**
 * Sets the listener. If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and no <code>getName()</code>
 * of the listener is empty, the name of this object is given to the listener.
 * Creation date: (04-11-2003 12:04:05)
 * @param newListener IPullingListener
 */
protected void setListener(IPullingListener newListener) {
	listener = newListener;
	
	if (listener instanceof INamedObject)  {
		
		if (StringUtils.isEmpty(((INamedObject)listener).getName())) {
			((INamedObject) listener).setName(this.getName());
		}
	}
	
}
/**
 * Sets the name of the Receiver. .If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and no <code>getName()</code>
 * of the listener is empty, the name of this object is given to the listener.
 * Creation date: (04-11-2003 12:06:29)
 * @param newName java.lang.String
 */
public void setName(java.lang.String newName) {
	name = newName;
	IPullingListener listener=getListener();
	if (listener instanceof INamedObject)  {
			if (StringUtils.isEmpty(((INamedObject)listener).getName())) {
				((INamedObject) listener).setName(newName);
			}
	} 
}
/**
 * Insert the method's description here.
 * Creation date: (28-10-2003 14:51:19)
 * @param newNumThreads int
 */
public void setNumThreads(int newNumThreads) {
	numThreads = newNumThreads;
}
/**
 * Insert the method's description here.
 * Creation date: (17-11-2003 15:49:10)
 * @param newOnError java.lang.String
 */
public void setOnError(java.lang.String newOnError) {
	onError = newOnError;
}
protected void startProcessingMessage(long waitingDuration) {
	synchronized (threadsProcessing) {
		int threadCount = (int) threadsProcessing.getValue();
		((StatisticsKeeper)idleStatistics.get(threadCount)).addValue(waitingDuration);
		threadsProcessing.increase();
	}
}
/**
 * Start the adapter. The thread-name will be set tot the adapter's name
 */
public void startRunning() {
    // if this receiver is on an adapter, the StartListening method
    // may only be executed when the adapter is started.
    if (adapter != null) {
	    RunStateEnum adapterRunState = adapter.getRunState();
        if (!adapterRunState.equals(RunStateEnum.STARTED)) {
            log.warn(
                "Receiver ["
                    + getName()
                    + "] on adapter ["
                    + adapter.getName()
                    + "] was tried to start, but the adapter is in state ["+adapterRunState+"]. Ignoring command.");
            adapter.getMessageKeeper().add(
                "ignored start command on [" + getName()  + "]; adapter is in state ["+adapterRunState+"]");
            return;
        }
    }
    try {
	    String msg=("Receiver [" + getName()  + "] starts listening.");
	    log.info(msg);
	    adapter.getMessageKeeper().add(msg);
        runState.setRunState(RunStateEnum.STARTING);

        getListener().open();

        // start all threads
	   	if (getNumThreads() > 1) {
			for (int i = 1; i <= getNumThreads(); i++) {
 				Thread t = new Thread(this, getName() + "[" + i+"]");
   	        	t.start();
			}
	    } else {
	        Thread t = new Thread(this, getName());
			t.start();
		}

            
    } catch (ListenerException e) {
        log.error("error occured while starting receiver [" + getName() + "]", e);
        if (null != adapter)
            adapter.getMessageKeeper().add(
                "error occured while starting receiver [" + getName() + "]:" + e.getMessage());
	    runState.setRunState(RunStateEnum.ERROR);            
        
    }    

	    
}
public void stopRunning() {

	if (getRunState().equals(RunStateEnum.STOPPED)){
		return;
	}
	
	if (!getRunState().equals(RunStateEnum.ERROR))
		runState.setRunState(RunStateEnum.STOPPING);
	else {
		closeAllResources();
		runState.setRunState(RunStateEnum.STOPPED);
	}
}
    /**
     *  Returns a toString of this class by introspection and the toString() value of its listener.
     *
     * @return    Description of the Return Value
     */
    public String toString() {
        String result = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.setDefaultStyle(ToStringStyle.MULTI_LINE_STYLE);
        ts.append("name", getName() );
        result += ts.toString();
        result+=" listener ["+listener.toString()+"]";
        return result;
    }
public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
	runState.waitForRunState(requestedRunState);
}
}
