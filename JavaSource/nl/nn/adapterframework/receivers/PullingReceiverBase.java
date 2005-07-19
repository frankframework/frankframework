package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import javax.transaction.Status;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.transaction.UserTransaction;

/**
 * This {@link IReceiver Receiver} may be used as a base-class for developing 'pulling' receivers.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>name of the class, mostly a class that extends this class</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumThreads(int) numThreads}</td><td>the number of threads listening in parallel for messages</td><td>1</td></tr>
 * <tr><td>{@link #setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply</td><td>continue</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>if set to <code>true, messages will be received and processed under transaction control. If processing fails, messages will be sent to the error-sender. (see below)</code></td><td><code>false</code></td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <tr><th>nested elements (accessible in descender-classes)</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.IPullingListener listener}</td><td>the listener used to receive messages from</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ITransactionalStorage inProcessStorage}</td><td>mandatory for {@link #setTransacted(boolean) transacted} receivers: place to store messages during processing.</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender errorSender}</td><td>optional for {@link #setTransacted(boolean) transacted} receviers: 
 * will be called to store messages that failed to process. If no errorSender is specified, failed messages will remain in inProcessStorage</td></tr>
 * </table>
 * </p>
 * <p><b>Transaction control</b><br>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true, messages will be received and processed under transaction control.
 * This means that after a message has been read and processed and the transaction has ended, one of the following apply:
 * <ul>
 * <table border="1">
 * <tr><th>situation</th><th>input listener</th><th>Pipeline</th><th>inProcess storage</th><th>errorSender</th><th>summary of effect</th></tr>
 * <tr><td>successful</td><td>message read and committed</td><td>message processed</td><td>unchanged</td><td>unchanged</td><td>message processed</td></tr>
 * <tr><td>procesing failed</td><td>message read and committed</td><td>message processing failed and rolled back</td><td>unchanged</td><td>message sent</td><td>message only transferred from listener to errroSender</td></tr>
 * <tr><td>listening failed</td><td>unchanged: listening rolled back</td><td>no processing performed</td><td>unchanged</td><td>unchanged</td><td>no changes, input message remains on input available for listener</td></tr>
 * <tr><td>transfer to inprocess storage failed</td><td>unchanged: listening rolled back</td><td>no processing performed</td><td>unchanged</td><td>unchanged</td><td>no changes, input message remains on input available for listener</td></tr>
 * <tr><td>transfer to errorSender failed</td><td>message read and committed</td><td>message processing failed and rolled back</td><td>message present</td><td>unchanged</td><td>message only transferred from listener to inProcess storage</td></tr>
 * </table> 
 * If the application or the server crashes in the middle of one or more transactions, these transactions 
 * will be recovered and rolled back after the server/application is restarted. Then allways exactly one of 
 * the following applies for any message touched at any time by Ibis by a transacted receiver:
 * <ul>
 * <li>It is processed correctly by the pipeline and removed from the input-queue, 
 *     not present in inProcess storage and not send to the errorSender</li> 
 * <li>It is not processed at all by the pipeline, or processing by the pipeline has been rolled back; 
 *     the message is removed from the input queue and either (one of) still in inProcess storage <i>or</i> sent to the errorSender</li>
 * </ul>
 * </p>
 *
 * <p><b>commit or rollback</b><br>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true, messages will be either committed or rolled back.
 * All message-processing transactions are committed, unless one or more of the following apply:
 * <ul>
 * <li>The PipeLine is transacted and the exitState of the pipeline is not equal to {@link nl.nn.adapterframework.core.PipeLine#setCommitOnState(String) commitOnState} (that defaults to 'success')</li>
 * <li>a PipeRunException or another runtime-exception has been thrown by any Pipe or by the PipeLine</li>
 * <li>the setRollBackOnly() method has been called on the userTransaction (not accessible by Pipes)</li>
 * </ul>
 * </p>
 *
 * @version Id
 * @author     Gerrit van Brakel
 * @since 4.0
 * @deprecated Please use {@link nl.nn.adapterframework.receivers.ReceiverBase ReceiverBase} instead
 */
public class PullingReceiverBase
    implements IReceiver, IReceiverStatistics, Runnable, HasSender {
	public static final String version="$Id: PullingReceiverBase.java,v 1.17 2005-07-19 13:02:45 europe\L190409 Exp $";
    	

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
    private ITransactionalStorage inProcessStorage=null;
    private ISender errorSender=null;
    
    private boolean transacted=false;
    

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
    if (inProcessStorage != null) {
    	try {
			inProcessStorage.close();
		} catch (Exception e) {
			log.error("Receiver [" + getName()+ "]: error closing inProcessStorage", e);
		}
    }
	if (errorSender != null) {
		try {
			errorSender.close();
		} catch (Exception e) {
			log.error("Receiver [" + getName()+ "]: error closing errorSender", e);
		}
	}
	if (getSender() != null) {
		try {
			getSender().close();
		} catch (Exception e) {
			log.error("Receiver [" + getName()+ "]: error closing errorSender", e);
		}
	}
}

/** 
 * sends a warning to the log and to the messagekeeper of the adapter
 */
public void warn(String msg) {
	log.warn(msg);
	if (adapter != null)
		adapter.getMessageKeeper().add("WARNING: " + msg);
}

/** 
 * sends an informational message to the log and to the messagekeeper of the adapter
 */
public void info(String msg) {
	log.info(msg);
	if (adapter != null)
		adapter.getMessageKeeper().add(msg);
}

public void configure() throws ConfigurationException {
  try {
 	
 	warn("Receiver ["+getName()+"] is using class ["+getClass().getName()+"] which is deprecated. Please consider using [nl.nn.adapterframework.receivers.GenericReceiver] or a class based on [nl.nn.adapterframework.receivers.ReceiverBase] instead");
 	
	if (getListener()==null) {
		throw new ConfigurationException("Receiver ["+getName()+"] has no listener");
	}
	getListener().configure();
	if (getListener() instanceof HasPhysicalDestination) {
		info("Receiver ["+getName()+"] has listener on "+((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
	}
	if (getListener() instanceof HasSender) {
		ISender sender = ((HasSender)getListener()).getSender();
		if (sender instanceof HasPhysicalDestination) {
			info("Listener of receiver ["+getName()+"] has sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
		}
	}
	if (isTransacted()) {
		if (!(getListener() instanceof IXAEnabled && ((IXAEnabled)getListener()).isTransacted())) {
			warn("Receiver ["+getName()+"] sets transacted=true, but listener not. Transactional integrity is not guaranteed"); 
		}
		if (getInProcessStorage()==null) {
			throw new ConfigurationException("Receiver ["+getName()+"] sets transacted=true, but has no inProcessStorage.");
		}
		if (!(getInProcessStorage() instanceof IXAEnabled && ((IXAEnabled)getInProcessStorage()).isTransacted())) {
			warn("Receiver ["+getName()+"] sets transacted=true, but inProcessStorage not. Transactional integrity is not guaranteed"); 
		}
		getInProcessStorage().configure();
		if (getInProcessStorage() instanceof HasPhysicalDestination) {
			info("Receiver ["+getName()+"] has inProcessStorage in "+((HasPhysicalDestination)getInProcessStorage()).getPhysicalDestinationName());
		}
		if (getErrorSender()==null) {
			warn("Receiver ["+getName()+"] sets transacted=true, but has no error sender. Messages processed with errors will get lost");
		}
		else {
			getErrorSender().configure();
			if (getErrorSender() instanceof HasPhysicalDestination) {
				info("Receiver ["+getName()+"] has errorSender to "+((HasPhysicalDestination)getErrorSender()).getPhysicalDestinationName());
			}
			if (!(getErrorSender() instanceof IXAEnabled && ((IXAEnabled)getErrorSender()).isTransacted())) {
				warn("Receiver ["+getName()+"] sets transacted=true, but errorSender is not. Transactional integrity is not guaranteed"); 
			}
		}
	}
		
	processStatistics.ensureCapacity(getNumThreads());
	idleStatistics.ensureCapacity(getNumThreads());

	for (int i=0; i<getNumThreads(); i++) {
		processStatistics.add(i, new StatisticsKeeper(i+1+" threads processing"));
		idleStatistics.add(i, new StatisticsKeeper(i+" threads processing"));
	}
		
	if (adapter != null) {
		adapter.getMessageKeeper().add("Receiver ["+getName()+"] initialization complete");
	}
 } catch(ConfigurationException e){
 	log.debug("Errors occured during configuration, setting runstate to ERROR");
	runState.setRunState(RunStateEnum.ERROR);
	throw e;
 }
}
protected void finishProcessingMessage(long processingDuration) {
	synchronized (threadsProcessing) {
		int threadCount = (int) threadsProcessing.decrease();
		((StatisticsKeeper)processStatistics.get(threadCount)).addValue(processingDuration);
	}
	log.debug("receiver ["+getName()+"] finishes processing message");
}
/**
 * Returns an iterator over the idle-statistics
 * @return iterator
 */
public Iterator getIdleStatisticsIterator() {
	return idleStatistics.iterator();
}

/**
 * Get the number of messages received.
  * @return long
 */
    public long getMessagesReceived() {
	    return numReceived.getValue();
    }
public Iterator getProcessStatisticsIterator() {
	return processStatistics.iterator();
}


protected void moveInProcessToError(String message, String messageId) {
	UserTransaction utx;

	log.info("receiver ["+getName()+"] moves message ["+messageId+"] to errorSender");
	ISender sender = getErrorSender();
	if (sender==null) {
		log.warn("["+getName()+"] has no errorSender, message with id ["+messageId+"] will remain in inProcessStorage");
		return;
	}
	
	try {
		utx = adapter.getUserTransaction();
		utx.begin();
	} catch (Exception e) {
		log.error("["+getName()+"] Exception preparing to move input message to error sender", e);
		// no use trying again to send message on errorSender, will cause same exception!
		return;
	}
	try {
		getInProcessStorage().deleteMessage(messageId);
		sender.sendMessage(messageId, message);
		utx.commit();
	} catch (Exception e) {
		log.error("["+getName()+"] Exception moving message with id ["+messageId+"] to error sender, original message: ["+message+"]",e);
		try {
			utx.rollback();
		} catch (Exception rbe) {
			log.error("["+getName()+"] Exception while rolling back transaction for message  with id ["+messageId+"], original message: ["+message+"]", rbe);
		}
	}
}

public Object getRawMessage(HashMap threadContext) throws ListenerException {
	if (isTransacted()) {
		String message;
		String messageId;
		Object rawMessage;
		IPullingListener listener = getListener();
		
		UserTransaction utx = null;

		try {
			utx = adapter.getUserTransaction();
			utx.begin();
		
		} catch (Exception e) {
			throw new ListenerException("["+getName()+"] Exception preparing to read input message", e);
			// no need to send message on errorSender, did not even try to read message
		}
		try {
			rawMessage = listener.getRawMessage(threadContext);
			if (rawMessage==null) {
				try {
					utx.rollback();
				} catch (Exception e) {
					log.warn("["+getName()+"] Exception while rolling back transaction after timeout on retrieving message", e);
				}
				return null;
			}
			message = listener.getStringFromRawMessage(rawMessage,threadContext);
			messageId = listener.getIdFromRawMessage(rawMessage,threadContext);
			//TODO: received date preciezer doen
			getInProcessStorage().storeMessage(messageId,messageId,new Date(),"in process",(Serializable)rawMessage);
			log.debug("["+getName()+"] commiting transfer of message to inProcessStorage");
			utx.commit();
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception rbe) {
				log.error("["+getName()+"] Exception while rolling back transaction after catching exception", rbe);
			}
			throw new ListenerException("["+getName()+"] Exception retrieving/storing message under transaction control",e);
			// no need to send message on errorSender, message will remain on input channel due to rollback
		}
		return rawMessage;
	} else {
	    return getListener().getRawMessage(threadContext);
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
    private PipeLineResult onMessage(String message, String id, HashMap threadContext) {

	    PipeLineResult result = null;
	    String state = "";

		if (null!=adapter) {
			if (isTransacted()) {
				UserTransaction utx = null;

				try {
					log.debug("["+getName()+"] starting transaction for processing of message");
					utx = adapter.getUserTransaction();
					utx.begin();
					log.debug("["+getName()+"] deleting message from inProcessStorage as part of message processing transaction");
					getInProcessStorage().deleteMessage(id);
					result = adapter.processMessage(id, message);
					state = result.getState();
					if (utx.getStatus()==Status.STATUS_ACTIVE){
						try {
							log.info("receiver [" + getName() + "] got exitState ["+state+"] from pipeline, committing transaction ["+utx+"] for messageid ["+id+"]");
							
							utx.commit();
						} catch (Exception e) {
							log.error("receiver [" + getName() + "] exception committing transaction", e);
							moveInProcessToError(message,id);
							if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
								log.info("receiver [" + getName() + "] closing after exception in committing transaction");
								stopRunning();
							}
						}
					} else {
						log.warn("receiver [" + getName() + "] got exitState ["+state+"] from pipeline, rolling back transaction ["+utx+"] for messageid ["+id+"]");
						try {
							utx.rollback();
						} catch (Exception e) {
							log.error("receiver [" + getName() + "] exception rolling back transaction", e);
						}
						moveInProcessToError(message,id);
					}

				} catch (Exception e) {
					log.error("["+getName()+"] Exception processing message under transaction control",e);
					try {
						utx.rollback();
					} catch (Exception rbe) {
						log.error("["+getName()+"] Exception while rolling back transaction after catching exception", rbe);
					}
				}
			} else {
				try {
					result = adapter.processMessage(id, message);
					state = result.getState();
					log.debug("["+getName()+"] proccessed request with exitState ["+state+"]");	        
        		} catch (Throwable e) {
	        		log.error("Receiver [" + getName() + "]:"+ToStringBuilder.reflectionToString(e,ToStringStyle.MULTI_LINE_STYLE), e);
        		}
			}
		} else {
			log.warn("["+getName()+"] has no adapter to process message");
		}
        return result;
    }
    
	private void afterMessageProcessed(PipeLineResult pipeLineResult, Object rawMessage, String id, HashMap threadContext) {
		try {
			getListener().afterMessageProcessed(pipeLineResult, rawMessage,threadContext);
		} catch (ListenerException e) {
			String msg = "receiver [" + getName() + "] caught exception in message post processing";
			log.error(msg, e);
			if (null != adapter) {
				adapter.getMessageKeeper().add(msg+":" + e.getMessage());
			}
			if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
				log.info("receiver [" + getName() + "] closing after exception in post processing");
				stopRunning();
			}
		}
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
		        
				afterMessageProcessed(pipeLineResult,rawMessage, id, threadContext);

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



protected void startProcessingMessage(long waitingDuration) {
	synchronized (threadsProcessing) {
		int threadCount = (int) threadsProcessing.getValue();
		((StatisticsKeeper)idleStatistics.get(threadCount)).addValue(waitingDuration);
		threadsProcessing.increase();
	}
	log.debug("receiver ["+getName()+"] starts processing message");
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
	// on exit resouces must be in a state that runstate is or can be set to 'STARTED'
	try {
		if (getSender()!=null) {
			getSender().open();
		}
		if (getErrorSender()!=null) {
			getErrorSender().open();
		}
		if (getInProcessStorage()!=null) {
			getInProcessStorage().open();
		}

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

            
    } catch (Exception e) {
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
        ToStringBuilder ts=new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        ts.append("name", getName() );
        result += ts.toString();
        result+=" listener ["+(listener==null ? "-none-" : listener.toString())+"]";
        return result;
    }
public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
	runState.waitForRunState(requestedRunState);
}
public boolean waitForRunState(RunStateEnum requestedRunState, long timeout) throws InterruptedException {
	return runState.waitForRunState(requestedRunState, timeout);
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
 * Returns the listener
 * @return IPullingListener
 */
public IPullingListener getListener() {
	return listener;
}/**
 * Sets the listener. If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and no <code>getName()</code>
 * of the listener is empty, the name of this object is given to the listener.
 * Creation date: (04-11-2003 12:04:05)
 * @param newListener IPullingListener
 */
protected void setListener(IPullingListener newListener) {
	listener = newListener;
	if (listener instanceof INamedObject)  {
		
		if (StringUtils.isEmpty(((INamedObject)listener).getName())) {
			((INamedObject) listener).setName("listener of ["+getName()+"]");
		}
	}
	
}
	/**
	 * Returns the inProcessStorage.
	 * @return ITransactionalStorage
	 */
	public ITransactionalStorage getInProcessStorage() {
		return inProcessStorage;
	}

	/**
	 * Sets the inProcessStorage.
	 * @param inProcessStorage The inProcessStorage to set
	 */
	public void setInProcessStorage(ITransactionalStorage inProcessStorage) {
		this.inProcessStorage = inProcessStorage;
		inProcessStorage.setName("inProcessStorage of ["+getName()+"]");
	}

	/**
	 * Returns the errorSender.
	 * @return ISender
	 */
	public ISender getErrorSender() {
		return errorSender;
	}

	/**
	 * Sets the errorSender.
	 * @param errorSender The errorSender to set
	 */
	protected void setErrorSender(ISender errorSender) {
		this.errorSender = errorSender;
		errorSender.setName("errorSender of ["+getName()+"]");
	}

	/**
	 * Controls the use of XA-transactions.
	 */
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}
	public boolean isTransacted() {
		return transacted;
	}

	
	
	/**
	 * Sets the name of the Receiver. 
	 * If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 */
	public void setName(String newName) {
		name = newName;
		IPullingListener listener=getListener();
		if (listener instanceof INamedObject)  {
				if (StringUtils.isEmpty(((INamedObject)listener).getName())) {
					((INamedObject) listener).setName("listner of ["+newName+"]");
				}
		} 
		ITransactionalStorage inProcess = getInProcessStorage();
		if (inProcess != null) {
			inProcess.setName("inProcessStorage of ["+newName+"]");
		}
		ISender errorSender = getErrorSender();
		if (errorSender != null) {
			errorSender.setName("errorSender of ["+newName+"]");
		}
	}
	public String getName() {
		return name;
	}
	
	/**
	 * The number of threads that this receiver is working with.
	 */
	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}
	public int getNumThreads() {
		return numThreads;
	}



	public void setOnError(String newOnError) {
		onError = newOnError;
	}
	public String getOnError() {
		return onError;
	}

}
