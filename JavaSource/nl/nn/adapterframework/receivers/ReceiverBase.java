/*
 * $Log: ReceiverBase.java,v $
 * Revision 1.8  2005-02-10 08:17:34  L190409
 * included context dump in debug
 *
 * Revision 1.7  2005/01/13 08:56:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Make threadContext-attributes available in PipeLineSession
 *
 * Revision 1.6  2004/10/12 15:14:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.5  2004/08/25 09:11:33  unknown <unknown@ibissource.org>
 * Add waitForRunstate with timeout
 *
 * Revision 1.4  2004/08/23 13:10:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.3  2004/08/16 14:09:58  unknown <unknown@ibissource.org>
 * Return returnIfStopped value in case adapter is stopped
 *
 * Revision 1.2  2004/08/09 13:46:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * various changes
 *
 * Revision 1.1  2004/08/03 13:04:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of GenericReceiver
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import javax.transaction.Status;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.transaction.UserTransaction;

/**
 * This {@link IReceiver Receiver} may be used as a base-class for developing receivers.
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
 * @since 4.2
 */
public class ReceiverBase
    implements IReceiver, IReceiverStatistics, Runnable, IMessageHandler, IbisExceptionListener, HasSender {
	public static final String version="$Id: ReceiverBase.java,v 1.8 2005-02-10 08:17:34 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());
 
	private String returnIfStopped="";

	public static final String ONERROR_CONTINUE = "continue";
	public static final String ONERROR_CLOSE = "close";


  	private String name;
  	private String onError = ONERROR_CONTINUE; 
    protected RunStateManager runState = new RunStateManager();

	// the number of threads that listen in parallel to the listener, only for pulling listeners
	private int numThreads = 1;
    
	private Counter threadsProcessing = new Counter(0);
	private Counter threadsRunning = new Counter(0);
	        
	// number of messages received
    private Counter numReceived = new Counter(0);
	private ArrayList processStatistics = new ArrayList();
	private ArrayList idleStatistics = new ArrayList();

    // the adapter that handles the messages and initiates this listener
    private IAdapter adapter;

	private IListener listener;
    private ITransactionalStorage inProcessStorage=null;
    private ISender errorSender=null;
	private ISender sender=null; // answer-sender
    
    private boolean transacted=false;
    
	protected String getLogPrefix() {
		return "Receiver ["+getName()+"] "; 
	}	

	/** 
	 * sends an informational message to the log and to the messagekeeper of the adapter
	 */
	protected void info(String msg) {
		log.info(msg);
		if (adapter != null)
			adapter.getMessageKeeper().add(msg);
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn(msg);
		if (adapter != null)
			adapter.getMessageKeeper().add("WARNING: " + msg);
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void error(String msg, Throwable t) {
		log.error(msg, t);
		if (adapter != null)
			adapter.getMessageKeeper().add("ERROR: " + msg+": "+t.getMessage());
	}


	protected void openAllResources() throws ListenerException {	
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
		} catch (SenderException e) {
			throw new ListenerException(e);
		}
		getListener().open();
		if (getListener() instanceof IPullingListener){
			// start all threads
			if (getNumThreads() > 1) {
				for (int i = 1; i <= getNumThreads(); i++) {
					addThread("[" + i+"]");
				}
			} else {
				addThread(null);
			}
		}
	}

	private void addThread(String nameSuffix) {
		if (getListener() instanceof IPullingListener){
			Thread t = new Thread(this, getName() + (nameSuffix==null ? "" : nameSuffix));
			t.start();
		}
	}


	protected void tellResourcesToStop() throws ListenerException {
		 // must lead to a 'closeAllResources()'
		 // runstate is 'STOPPING'
		 // default just calls 'closeAllResources()'
		 closeAllResources();
	}
	protected void closeAllResources() {
		// on exit resouces must be in a state that runstate can be set to 'STOPPED'
		try {
			log.debug("closing Receiver ["+ getName()+ "]");
			getListener().close();
			if (getSender()!=null) {
				getSender().close();
			}
			if (getErrorSender()!=null) {
				getErrorSender().close();
			}
			if (getInProcessStorage()!=null) {
				getInProcessStorage().close();
			}
	
			log.info("closed Receiver ["+ getName()+ "]");
		} catch (Exception e) {
			log.error(
				"Receiver [" + getName()+ "]: error closing connection", e);
		}
		runState.setRunState(RunStateEnum.STOPPED);
		info("Receiver [" + getName() + "] stopped");
	}
	 
	protected void propagateName() {
		IListener listener=getListener();
		if (listener!=null && StringUtils.isEmpty(listener.getName())) {
			listener.setName("listener of ["+getName()+"]");
		}
		ITransactionalStorage inProcess = getInProcessStorage();
		if (inProcess != null) {
			inProcess.setName("inProcessStorage of ["+getName()+"]");
		}
		ISender errorSender = getErrorSender();
		if (errorSender != null) {
			errorSender.setName("errorSender of ["+getName()+"]");
		}
	}

	public void configure() throws ConfigurationException {		
 		try {
 			propagateName();
			if (getListener()==null) {
				throw new ConfigurationException("Receiver ["+getName()+"] has no listener");
			}
			if (getListener() instanceof IPushingListener) {
				IPushingListener pl = (IPushingListener)getListener();
				pl.setHandler(this);
				pl.setExceptionListener(this);
			}
			getListener().configure();
			if (getListener() instanceof HasPhysicalDestination) {
				info("Receiver ["+getName()+"] has listener on "+((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
			}
			if (getListener() instanceof HasSender) {
				// only informational
				ISender sender = ((HasSender)getListener()).getSender();
				if (sender instanceof HasPhysicalDestination) {
					info("Listener of receiver ["+getName()+"] has answer-sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
			}
			ISender sender = getSender();
			if (sender!=null) {
				sender.configure();
				if (sender instanceof HasPhysicalDestination) {
					info("receiver ["+getName()+"] has answer-sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
			}
			if (getErrorSender()!=null) {
				getErrorSender().configure();
				if (getErrorSender() instanceof HasPhysicalDestination) {
					info("Receiver ["+getName()+"] has errorSender to "+((HasPhysicalDestination)getErrorSender()).getPhysicalDestinationName());
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
				} else {
					if (!(getErrorSender() instanceof IXAEnabled && ((IXAEnabled)getErrorSender()).isTransacted())) {
						warn("Receiver ["+getName()+"] sets transacted=true, but errorSender is not. Transactional integrity is not guaranteed"); 
					}
				}
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
			openAllResources();
			runState.setRunState(RunStateEnum.STARTED);
            
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
	
		if (!getRunState().equals(RunStateEnum.ERROR)) { 
			runState.setRunState(RunStateEnum.STOPPING);
			try {
				tellResourcesToStop();
			} catch (ListenerException e) {
				warn("exception stopping receiver: "+e.getMessage());
			}
		}
		else {
			closeAllResources();
			runState.setRunState(RunStateEnum.STOPPED);
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
			IPullingListener listener = (IPullingListener)getListener();		
			HashMap threadContext = listener.openThread();
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
					processRawMessage(listener,rawMessage,threadContext,finishProcessingTimestamp-startProcessingTimestamp);
					finishProcessingTimestamp = System.currentTimeMillis();
				} 
			}
			listener.closeThread(threadContext);
	
		} catch (Throwable e) {
			error("error occured in receiver [" + getName() + "]",e);
		} finally {
			long stillRunning=threadsRunning.decrease();
	
			if (stillRunning>0) {
				log.info("a thread of Receiver ["+getName()+"] exited, ["+stillRunning+"] are still running");
				return;
			}
			log.info("the last thread of Receiver ["+getName()+"] exited, cleaning up");
			closeAllResources();
		}
	}


	protected void startProcessingMessage(long waitingDuration) {
		synchronized (threadsProcessing) {
			int threadCount = (int) threadsProcessing.getValue();
			
			if (waitingDuration>=0) {
				getIdleStatistics(threadCount).addValue(waitingDuration);
			}
			threadsProcessing.increase();
		}
		log.debug("receiver ["+getName()+"] starts processing message");
	}

	protected void finishProcessingMessage(long processingDuration) {
		synchronized (threadsProcessing) {
			int threadCount = (int) threadsProcessing.decrease();
			getProcessStatistics(threadCount).addValue(processingDuration);
		}
		log.debug("receiver ["+getName()+"] finishes processing message");
	}

	public Object getRawMessage(HashMap threadContext) throws ListenerException {
		IPullingListener listener = (IPullingListener)getListener();

		if (isTransacted()) {
			Object rawMessage;
			
			UserTransaction utx = null;
	
			try {
				utx = getAdapter().getUserTransaction();
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
			} catch (Exception e) {
				try {
					utx.rollback();
				} catch (Exception rbe) {
					log.error("["+getName()+"] Exception while rolling back transaction after catching exception", rbe);
				}
				throw new ListenerException("["+getName()+"] Exception retrieving message under transaction control",e);
				// no need to send message on errorSender, message will remain on input channel due to rollback
			}
			return rawMessage;
		} else {
			return listener.getRawMessage(threadContext);
		}
	}


	private void prepareToProcessMessageTransacted(UserTransaction utx, String messageId, String message) throws ListenerException {
		log.info("receiver ["+getName()+"] moves message ["+messageId+"] to inProcess");
		
		try {
			getInProcessStorage().storeMessage(messageId,message);
			log.debug("["+getName()+"] commiting transfer of message ["+messageId+"] to inProcessStorage");
			utx.commit();
		} catch (Exception e) {
			log.error("["+getName()+"] Exception transfering message with id ["+messageId+"] to inProcessStorage, original message: ["+message+"]",e);
			try {
				utx.rollback();
			} catch (Exception rbe) {
				log.error("["+getName()+"] Exception while rolling back transaction for message ["+messageId+"] after catching exception", rbe);
			}
			throw new ListenerException("["+getName()+"] Exception retrieving/storing message ["+messageId+"] under transaction control",e);
			// no need to send message on errorSender, message will remain on input channel due to rollback
		}
		try {
			utx.begin();
			log.debug("["+getName()+"] deleting message from inProcessStorage as part of message processing transaction");
			getInProcessStorage().deleteMessage(messageId);
		} catch (Exception e) {
			log.error("["+getName()+"] Exception processing message ["+messageId+"] under transaction control",e);
			try {
				utx.rollback();
			} catch (Exception rbe) {
				log.error("["+getName()+"] Exception while rolling back transaction for message ["+messageId+"] after catching exception", rbe);
			}
			throw new ListenerException("["+getName()+"] Exception processing message ["+messageId+"] under transaction control",e);
		}
	}

	private void finishTransactedProcessingOfMessage(UserTransaction utx, String messageId, String message) {
		try {
			if (utx.getStatus()==Status.STATUS_ACTIVE){
				try {
					log.info("receiver [" + getName() + "] got active transaction from pipeline, committing transaction ["+utx+"] for messageid ["+messageId+"]");
								
					utx.commit();
				} catch (Exception e) {
					log.error("receiver [" + getName() + "] exception committing transaction", e);
					moveInProcessToError(utx,messageId,message);
					if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
						log.info("receiver [" + getName() + "] closing after exception in committing transaction");
						stopRunning();
					}
				}
			} else {
				log.warn("receiver [" + getName() + "] got transaction with state  ["+JtaUtil.displayTransactionStatus(utx)+"] from pipeline, rolling back transaction ["+utx+"] for messageid ["+messageId+"]");
				try {
					utx.rollback();
				} catch (Exception e) {
					log.error("receiver [" + getName() + "] exception rolling back transaction", e);
				}
				moveInProcessToError(utx,messageId,message);
			}
	
		} catch (Exception e) {
			log.error("["+getName()+"] Exception processing message under transaction control",e);
			try {
				utx.rollback();
			} catch (Exception rbe) {
				log.error("["+getName()+"] Exception while rolling back transaction after catching exception", rbe);
			}
		}
	}

	private void moveInProcessToError(UserTransaction utx, String messageId, String message) {
	
		log.info("receiver ["+getName()+"] moves message ["+messageId+"] to errorSender");
		ISender sender = getErrorSender();
		if (sender==null) {
			log.warn("["+getName()+"] has no errorSender, message with id ["+messageId+"] will remain in inProcessStorage");
			return;
		}
		
		try {
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

	/**
	 * Process the received message with {@link #processRequest(IListener, String, String)}.
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID
	 */
	public String processRequest(IListener origin, String message) throws ListenerException {
		return processRequest(origin, null,message);
	}

	public String processRequest(IListener origin, String correlationId, String message)  throws ListenerException{
		if (getRunState() == RunStateEnum.STOPPED || getRunState() == RunStateEnum.STOPPING)
			return getReturnIfStopped();
			
		UserTransaction utx = null;
		if (isTransacted()) {
			try {
				utx = adapter.getUserTransaction();
			} catch (Exception e) {
				throw new ListenerException("["+getName()+"] Exception obtaining usertransaction", e);
			}
		}
		return processMessageInAdapter(utx, origin, message, message, correlationId, null, -1);
	}


	public void processRawMessage(IListener origin, Object message) throws ListenerException {
		processRawMessage(origin, message, null, -1);
	}
	public void processRawMessage(IListener origin, Object message, HashMap context) throws ListenerException {
		processRawMessage(origin, message, context, -1);
	}

    /**
     * All messages that for this receiver are pumped down to this method, so it actually
     * calls the {@link nl.nn.adapterframework.core.Adapter adapter} to process the message.<br/>

	 * Assumes that a transation has been started where necessary
	 */
	public void processRawMessage(IListener origin, Object rawMessage, HashMap threadContext, long waitingDuration) throws ListenerException {
		UserTransaction utx = null;
		
		if (isTransacted()) {
			try {
				utx = adapter.getUserTransaction();
			} catch (Exception e) {
				throw new ListenerException("["+getName()+"] Exception obtaining usertransaction", e);
			}
			if (rawMessage==null) {
				try {
					utx.rollback();
				} catch (Exception e) {
					log.warn("["+getName()+"] Exception while rolling back transaction after timeout on retrieving message", e);
				}
				return;
			}
		}
		if (rawMessage==null) {
			return;
		}		
		
		if (threadContext==null) {
			threadContext = new HashMap();
		}
		
		String message = origin.getStringFromRawMessage(rawMessage, threadContext);
		String id = origin.getIdFromRawMessage(rawMessage, threadContext);
		processMessageInAdapter(utx, origin, rawMessage, message, id, threadContext, waitingDuration);
	}

	/*
	 * assumes message is read, and when transacted, transation is still open to be able to store it in InProcessStore
	 */
	protected String processMessageInAdapter(UserTransaction utx, IListener origin, Object rawMessage, String message, String id, HashMap threadContext, long waitingDuration) throws ListenerException {
		String result=null;
		long startProcessingTimestamp = System.currentTimeMillis();
		log.debug(getLogPrefix()+"received message correlationId ["+id+"]");

		startProcessingMessage(waitingDuration);
		numReceived.increase();

		try {
			if (StringUtils.isEmpty(id)) {
				id=getName()+"-"+Misc.createSimpleUUID();
				if (log.isDebugEnabled()) 
					log.debug(getLogPrefix()+"generating correlationId ["+id+"]");
			}
			if (isTransacted()) {
				prepareToProcessMessageTransacted(utx,id,message);
			}
			PipeLineSession pipelineSession = new PipeLineSession();
			if (threadContext!=null) {
					pipelineSession.putAll(threadContext);
					if (log.isDebugEnabled()) {
						String contextDump = "PipeLineSession variables for correlationId ["+id+"]:";
						for (Enumeration en=pipelineSession.keys(); en.hasMoreElements();) {
							String key = (String)en.nextElement();
							Object value = pipelineSession.get(key);
							if (key.equals("messageText")) {
								value = "(... see elsewhere ...)";
							}
							contextDump+=" "+key+"=["+value.toString()+"]";
						}
						log.debug(contextDump);
					}
			}
			PipeLineResult pipeLineResult = adapter.processMessage(id, message, pipelineSession);
			result=pipeLineResult.getResult();
			
			try {
				if (getSender()!=null) {
					getSender().sendMessage(id,result);
				}
				origin.afterMessageProcessed(pipeLineResult,rawMessage, threadContext);
			} catch (Exception e) {
				String msg = "receiver [" + getName() + "] caught exception in message post processing";
				error(msg, e);
				if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
					log.info("receiver [" + getName() + "] closing after exception in post processing");
					stopRunning();
				}
			}
			if (isTransacted()) {
				finishTransactedProcessingOfMessage(utx,id,message);
			}
		} finally {
			long finishProcessingTimestamp = System.currentTimeMillis();
			finishProcessingMessage(finishProcessingTimestamp-startProcessingTimestamp);
		}
		log.debug(getLogPrefix()+"returning result ["+result+"] for message correlationId ["+id+"]");
		return result;
	}

/*
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
							moveInProcessToError(id,message);
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
						moveInProcessToError(id,message);
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

*/


	/*
	 * pulling only
	public Object getRawMessage(HashMap threadContext) throws ListenerException {
		if (isTransacted()) {
			String message;
			String messageId;
			Object rawMessage;
			IListener listener = getListener();
		
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
				getInProcessStorage().storeMessage(messageId,message);
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
	 */
  
    
    
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
    
    /*
     * pulling only
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
	
	*/
	
	
	public void exceptionThrown(INamedObject object, Throwable t) {
		String msg = getLogPrefix()+"received exception ["+t.getClass().getName()+"] from ["+object.getName()+"], stopping receiver";
		error(msg,t);
		stopRunning();
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
	
	
	
	protected synchronized StatisticsKeeper getProcessStatistics(int threadsProcessing) {
		StatisticsKeeper result;
		try {
			result = ((StatisticsKeeper)processStatistics.get(threadsProcessing));
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}
	
		if (result==null) {
			result = new StatisticsKeeper(threadsProcessing+1+" threads processing");
			processStatistics.add(threadsProcessing, result);
		}
		return result;
	}
	
	protected synchronized StatisticsKeeper getIdleStatistics(int threadsProcessing) {
		StatisticsKeeper result;
		try {
			result = ((StatisticsKeeper)idleStatistics.get(threadsProcessing));
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}

		if (result==null) {
			result = new StatisticsKeeper(threadsProcessing+" threads processing");
			idleStatistics.add(threadsProcessing, result);
		}
		return result;
	}
	
	/**
	 * Returns an iterator over the process-statistics
	 * @return iterator
	 */
	public Iterator getProcessStatisticsIterator() {
		return processStatistics.iterator();
	}
	
	/**
	 * Returns an iterator over the idle-statistics
	 * @return iterator
	 */
	public Iterator getIdleStatisticsIterator() {
		return idleStatistics.iterator();
	}
	
	
	public ISender getSender() {
		return sender;
	}
	
	protected void setSender(ISender sender) {
		this.sender = sender;
	}

	public void setAdapter(IAdapter adapter) {
		this.adapter = adapter;
	}
	
	
	
	/**
	 * Returns the listener
	 * @return IPullingListener
	 */
	public IListener getListener() {
		return listener;
	}/**
	 * Sets the listener. If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and no <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 * Creation date: (04-11-2003 12:04:05)
	 * @param newListener IPullingListener
	 */
	protected void setListener(IListener newListener) {
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
	protected void setInProcessStorage(ITransactionalStorage inProcessStorage) {
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
	 * Get the number of messages received.
	  * @return long
	 */
	public long getMessagesReceived() {
		return numReceived.getValue();
	}
	


	/**
	 * Sets the name of the Receiver. 
	 * If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 */
	public void setName(String newName) {
		name = newName;
		propagateName();
	}


	public String getName() {
		return name;
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


	public void setOnError(String newOnError) {
		onError = newOnError;
	}
	public String getOnError() {
		return onError;
	}
	protected IAdapter getAdapter() {
		return adapter;
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

	
	/**
	 * Return this value when this receiver is stopped.
	 */
	public String getReturnIfStopped() {
		return returnIfStopped;
	}
	/**
	 * Return this value when this receiver is stopped.
	 */
	public void setReturnIfStopped (String returnIfStopped){
		this.returnIfStopped=returnIfStopped;
	}

	/**
	 * The number of threads that this receiver is configured to work with.
	 */
	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}
	public int getNumThreads() {
		return numThreads;
	}

	public String formatException(String extrainfo, String correlationId, String message, Throwable t) {
		return getAdapter().formatErrorMessage(extrainfo,t,message,correlationId,null,0);
	}


}
