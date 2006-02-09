/*
 * $Log: ReceiverBase.java,v $
 * Revision 1.22  2006-02-09 07:57:47  europe\L190409
 * METT tracing support
 *
 * Revision 1.21  2005/10/27 08:46:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced RunStateEnquiries
 *
 * Revision 1.20  2005/10/26 08:52:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for transacted="true" without inProcessStorage, (ohne Gewähr!)
 *
 * Revision 1.19  2005/10/17 11:29:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed nullpointerexception in startRunning
 *
 * Revision 1.18  2005/09/26 11:42:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added fileNameIfStopped attribute and replace from/to processing when stopped
 *
 * Revision 1.17  2005/09/13 15:42:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved handling of non-serializable messages like Poison-messages
 *
 * Revision 1.16  2005/08/08 09:44:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * start transactions if needed and not already started
 *
 * Revision 1.15  2005/07/19 15:27:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified closing procedure
 * added errorStorage
 * modified implementation of transactionalStorage
 * allowed exceptions to bubble up
 * assume rawmessages to be serializable for transacted processing
 * added ibis42compatibility attribute, avoiding exception bubbling
 *
 * Revision 1.14  2005/07/05 12:54:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to set parameters from context for processRequest() methods
 *
 * Revision 1.13  2005/06/02 11:52:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * limited number of actively polling threads to value of attriubte numThreadsPolling
 *
 * Revision 1.12  2005/04/13 12:53:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.11  2005/03/31 08:22:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in getIdleStatistics
 *
 * Revision 1.10  2005/03/07 11:04:36  Johan Verrips <johan.verrips@ibissource.org>
 * PipeLineSession became a extension of HashMap, using other iterator
 *
 * Revision 1.9  2005/03/04 08:53:29  Johan Verrips <johan.verrips@ibissource.org>
 * Fixed IndexOutOfBoundException in getProcessStatistics  due to multi threading.
 * Adjusted this too for getIdleStatistics
 *
 * Revision 1.8  2005/02/10 08:17:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.core.MettHook;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.Semaphore;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import com.ing.coins.mett.application.MonitorAccessor;

import javax.transaction.Status;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
 * <tr><td>{@link #setNumThreads(int) numThreads}</td><td>the number of threads that may execute a pipeline concurrently (only for pulling listeners)</td><td>1</td></tr>
 * <tr><td>{@link #setNumThreadsPolling(int) numThreadsPolling}</td><td>the number of threads that are activily polling for messages concurrently. '0' means 'limited only by <code>numThreads</code>' (only for pulling listeners)</td><td>1</td></tr>
 * <tr><td>{@link #setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply</td><td>continue</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>if set to <code>true, messages will be received and processed under transaction control. If processing fails, messages will be sent to the error-sender. (see below)</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setIbis42compatibility(boolean) ibis42compatibility}</td><td>if set to <code>true, the result of a failed processing of a message is a formatted errormessage. Otherwise a listener specific error handling is performed</code></td><td><code>false</code></td></tr>
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
public class ReceiverBase implements IReceiver, IReceiverStatistics, Runnable, IMessageHandler, IbisExceptionListener, HasSender, MettHook {
	public static final String version="$RCSfile: ReceiverBase.java,v $ $Revision: 1.22 $ $Date: 2006-02-09 07:57:47 $";
	protected Logger log = Logger.getLogger(this.getClass());
 
	private String returnIfStopped="";
	private String fileNameIfStopped = null;
	private String replaceFrom = null;
	private String replaceTo = null;

	public static final String ONERROR_CONTINUE = "continue";
	public static final String ONERROR_CLOSE = "close";


  	private String name;
  	private String onError = ONERROR_CONTINUE; 
    protected RunStateManager runState = new RunStateManager();
    
    private boolean ibis42compatibility=false;

	// the number of threads that may execute a pipeline concurrently (only for pulling listeners)
	private int numThreads = 1;
	// the number of threads that are activily polling for messages (concurrently, only for pulling listeners)
	private int numThreadsPolling = 1;
   
	private Counter threadsProcessing = new Counter(0);
	private Counter threadsRunning = new Counter(0);
	private Semaphore pollToken = null;
	        
	// number of messages received
    private Counter numReceived = new Counter(0);
	private ArrayList processStatistics = new ArrayList();
	private ArrayList idleStatistics = new ArrayList();

    // the adapter that handles the messages and initiates this listener
    private IAdapter adapter;

	private IListener listener;
    private ITransactionalStorage inProcessStorage=null;
    private ISender errorSender=null;
	private ITransactionalStorage errorStorage=null;
	private ISender sender=null; // answer-sender
    
    private boolean transacted=false;
 
 	// METT event numbers
	private int beforeEvent=-1;
	private int afterEvent=-1;
	private int exceptionEvent=-1;


    
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
		 if (getListener() instanceof IPushingListener) {
			closeAllResources();
		 }
		 // IPullingListeners stop as their threads finish, as the runstate is set to stopping
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
			if (getErrorStorage()!=null && getErrorStorage()!=getInProcessStorage()) {
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
		ITransactionalStorage errorStorage = getErrorStorage();
		if (errorStorage != null && errorStorage != inProcess) {
			errorStorage.setName("errorStorage of ["+getName()+"]");
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
			if (getListener() instanceof IPullingListener) {
				if (getNumThreadsPolling()>0 && getNumThreadsPolling()<getNumThreads()) {
					pollToken = new Semaphore(getNumThreadsPolling());
				}
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
			ISender errorSender = getErrorSender();
			ITransactionalStorage errorStorage = getErrorStorage();
			if (errorSender!=null) {
				errorSender.configure();
				if (errorSender instanceof HasPhysicalDestination) {
					info("Receiver ["+getName()+"] has errorSender to "+((HasPhysicalDestination)errorSender).getPhysicalDestinationName());
				}
			}
			if (errorStorage!=null && errorStorage != getInProcessStorage()) {
				errorStorage.configure();
				if (errorStorage instanceof HasPhysicalDestination) {
					info("Receiver ["+getName()+"] has errorStorage to "+((HasPhysicalDestination)errorStorage).getPhysicalDestinationName());
				}
			}
			if (isTransacted()) {
				if (!(getListener() instanceof IXAEnabled && ((IXAEnabled)getListener()).isTransacted())) {
					warn("Receiver ["+getName()+"] sets transacted=true, but listener not. Transactional integrity is not guaranteed"); 
				}
				if (getInProcessStorage()==null) {
//					throw new ConfigurationException("Receiver ["+getName()+"] sets transacted=true, but has no inProcessStorage.");
					warn("Receiver ["+getName()+"] sets transacted=true, but has no inProcessStorage. Transactional integrity is not guaranteed");
				} else {
					if (!(getInProcessStorage() instanceof IXAEnabled && ((IXAEnabled)getInProcessStorage()).isTransacted())) {
						warn("Receiver ["+getName()+"] sets transacted=true, but inProcessStorage not. Transactional integrity is not guaranteed"); 
					}
					getInProcessStorage().configure();
					if (getInProcessStorage() instanceof HasPhysicalDestination) {
						info("Receiver ["+getName()+"] has inProcessStorage in "+((HasPhysicalDestination)getInProcessStorage()).getPhysicalDestinationName());
					}
				}
				
				if (errorSender==null && errorStorage==null) {
					warn("Receiver ["+getName()+"] sets transacted=true, but has no errorSender or errorStorage. Messages processed with errors will be lost");
				} else {
					if (errorSender!=null && !(errorSender instanceof IXAEnabled && ((IXAEnabled)errorSender).isTransacted())) {
						warn("Receiver ["+getName()+"] sets transacted=true, but errorSender is not. Transactional integrity is not guaranteed"); 
					}
					if (errorStorage!=null && !(errorStorage instanceof IXAEnabled && ((IXAEnabled)errorStorage).isTransacted())) {
						warn("Receiver ["+getName()+"] sets transacted=true, but errorStorage is not. Transactional integrity is not guaranteed"); 
					}
					if (errorStorage==inProcessStorage) {
						info("Receiver ["+getName()+"] has errorStorage in inProcessStorage."); 
					}
				}
			} 

			if (StringUtils.isNotEmpty(getFileNameIfStopped())) {
				try {
					setReturnIfStopped(Misc.resourceToString(ClassUtils.getResourceURL(this,fileNameIfStopped), SystemUtils.LINE_SEPARATOR));
				} catch (Throwable e) {
					throw new ConfigurationException("Receiver ["+getName()+"] got exception loading ["+getFileNameIfStopped()+"]", e);
				}
			}

			if (StringUtils.isNotEmpty(getReplaceFrom())) {
				setReturnIfStopped(Misc.replace(getReturnIfStopped(), getReplaceFrom(), getReplaceTo()));
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
			if (adapter != null) { 
				adapter.getMessageKeeper().add(msg);
			}
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
				if (pollToken!=null) {
					pollToken.acquire();
				}
				Object rawMessage=null;
				try {
					if (getRunState().equals(RunStateEnum.STARTED)) {
						rawMessage = getRawMessage(threadContext);
					}
				} finally {
					if (pollToken!=null) {
						pollToken.release();
					}
				}
				if (rawMessage!=null) {

					try {
						eventOccurred(getBeforeEvent());

						startProcessingTimestamp = System.currentTimeMillis();
						try {
							processRawMessage(listener,rawMessage,threadContext,finishProcessingTimestamp-startProcessingTimestamp);
						} catch (ListenerException e) {
							eventOccurred(getExceptionEvent());
							if ("continue".equalsIgnoreCase(getOnError())) {
								error("caught Exception processing message, will continue processing next message", e);
							} else {
								error("stopping receiver after exception in processing message",e);
								stopRunning();
							}
						}
						finishProcessingTimestamp = System.currentTimeMillis();
					} finally {
						eventOccurred(getAfterEvent());
					}
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


	private String prepareToProcessMessageTransacted(UserTransaction utx, String originalMessageId, String correlationId, Object rawMessage) throws ListenerException {
		log.info("receiver ["+getName()+"] moves message with originalMessageId ["+originalMessageId+"] correlationId ["+correlationId+"] to inProcess");
		String newMessageId=null;
		try {
			if (getInProcessStorage() == null) {
				log.warn(getLogPrefix()+"has no inProcessStorage, cannot store message before processing. Will commit read of message, and start a new transaction");
				utx.commit();
				utx.begin();
				return null;
			}
			if (rawMessage instanceof Serializable) {
				//TODO: received date preciezer doen
				newMessageId = getInProcessStorage().storeMessage(originalMessageId,correlationId,new Date(),"in process",(Serializable)rawMessage);
				log.debug("["+getName()+"] committing transfer of message with messageId ["+originalMessageId+"] to inProcessStorage, newMessageId ["+newMessageId+"]");
				utx.commit();
			} else {
				log.warn("["+getName()+"] received message of type ["+rawMessage.getClass().getName()+"] is not serializable, cannot be stored in inProcessStorage; will only commit its reception");
				utx.commit();
				throw new ListenerException("["+getName()+"] received non serializable message of type ["+rawMessage.getClass().getName()+"], contents ["+rawMessage.toString()+"]");
			}
		} catch (Exception e) {
			log.error("["+getName()+"] Exception transfering message with messageId ["+originalMessageId+"] to inProcessStorage, original message: ["+rawMessage+"]",e);
			try {
				utx.rollback();
			} catch (Exception rbe) {
				log.error("["+getName()+"] Exception while rolling back transaction for message with messageId ["+originalMessageId+"] after catching exception", rbe);
			}
			log.warn("["+getName()+"] stopping receiver as message cannot be stored in inProcessStorage");
			stopRunning();
			throw new ListenerException("["+getName()+"] Exception retrieving/storing message with messageId ["+originalMessageId+"] under transaction control",e);
			// no need to send message on errorSender, message will remain on input channel due to rollback
		}
		try {
			utx.begin();
			log.debug("["+getName()+"] deleting message ["+newMessageId+"] correlationId ["+correlationId+"] from inProcessStorage as part of message processing transaction");
			getInProcessStorage().deleteMessage(newMessageId);
			return newMessageId;
		} catch (Exception e) {
			log.error("["+getName()+"] Exception processing message ["+newMessageId+"] correlationId ["+correlationId+"] under transaction control",e);
			try {
				utx.rollback();
			} catch (Exception rbe) {
				log.error("["+getName()+"] Exception while rolling back transaction for message ["+newMessageId+"] correlationId ["+correlationId+"] after catching exception", rbe);
			}
			throw new ListenerException("["+getName()+"] Exception in preparation of transacted processing of message ["+newMessageId+"] correlationId ["+correlationId+"]",e);
		}
	}

	private void finishTransactedProcessingOfMessage(UserTransaction utx, String inProcessMessageId, String originalMessageId, String correlationId, String message, Date receivedDate, String comments, Serializable rawMessage) {
		try {
			if (utx.getStatus()==Status.STATUS_ACTIVE){
				try {
					log.info("receiver [" + getName() + "] got active transaction from pipeline, committing transaction ["+utx+"] for messageid ["+inProcessMessageId+"]");
								
					utx.commit();
				} catch (Exception e) {
					log.error("receiver [" + getName() + "] exception committing transaction", e);
					moveInProcessToError(utx, inProcessMessageId, originalMessageId, correlationId, message, receivedDate, "exception committing transaction: "+ e.getMessage(), rawMessage);
					if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
						log.info("receiver [" + getName() + "] closing after exception in committing transaction");
						stopRunning();
					}
				}
			} else {
				log.warn("receiver [" + getName() + "] got transaction with state  ["+JtaUtil.displayTransactionStatus(utx)+"] from pipeline, rolling back transaction ["+utx+"] for messageid ["+inProcessMessageId+"]");
				try {
					utx.rollback();
				} catch (Exception e) {
					log.error("receiver [" + getName() + "] exception rolling back transaction", e);
				}
				moveInProcessToError(utx, inProcessMessageId, originalMessageId, correlationId,message, receivedDate, comments, rawMessage);
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

	private void moveInProcessToError(UserTransaction utx, String inProcessMessageId, String originalMessageId, String correlationId, String message, Date receivedDate, String comments, Serializable rawMessage) {
	
		log.info("receiver ["+getName()+"] moves message id ["+originalMessageId+"] correlationId ["+correlationId+"] from inProcess ["+inProcessMessageId+"] to errorSender/errorStorage");
		ISender errorSender = getErrorSender();
		ITransactionalStorage errorStorage = getErrorStorage();
		if (errorSender==null && errorStorage==null) {
			log.warn("["+getName()+"] has no errorSender or errorStorage, message with id ["+inProcessMessageId+"] will remain in inProcessStorage");
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
			getInProcessStorage().deleteMessage(inProcessMessageId);
			if (errorSender!=null) {
				errorSender.sendMessage(correlationId, message);
			} 
			if (errorStorage!=null) {
				errorStorage.storeMessage(originalMessageId, correlationId, receivedDate, comments, rawMessage);
			} 
			utx.commit();
		} catch (Exception e) {
			log.error("["+getName()+"] Exception moving message with inprocess id ["+inProcessMessageId+"] correlationId ["+correlationId+"] to error sender, original message: ["+message+"]",e);
			try {
				utx.rollback();
			} catch (Exception rbe) {
				log.error("["+getName()+"] Exception while rolling back transaction for message  with inprocess id ["+inProcessMessageId+"] correlationId ["+correlationId+"], original message: ["+message+"]", rbe);
			}
		}
	}

	/**
	 * Process the received message with {@link #processRequest(IListener, String, String)}.
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID
	 */
	public String processRequest(IListener origin, String message) throws ListenerException {
		return processRequest(origin, null, message, null, -1);
	}

	public String processRequest(IListener origin, String correlationId, String message)  throws ListenerException{
		return processRequest(origin, correlationId, message, null, -1);
	}

	public String processRequest(IListener origin, String correlationId, String message, HashMap context) throws ListenerException {
		return processRequest(origin, correlationId, message, context, -1);
	}

	public String processRequest(IListener origin, String correlationId, String message, HashMap context, long waitingTime) throws ListenerException {
		if (getRunState() == RunStateEnum.STOPPED || getRunState() == RunStateEnum.STOPPING)
			return getReturnIfStopped();
			
		UserTransaction utx = null;
		if (isTransacted()) {
			try {
				utx = adapter.getUserTransaction();
				if (!adapter.inTransaction()) {
					log.debug("Receiver ["+getName()+"] starts transaction as no one is yet present");
					utx.begin();
				}
			} catch (Exception e) {
				throw new ListenerException("["+getName()+"] Exception obtaining usertransaction", e);
			}
		}
		return processMessageInAdapter(utx, origin, message, message, null, correlationId, context, waitingTime);
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
				if (!adapter.inTransaction()) {
					log.debug("Receiver ["+getName()+"] starts transaction as no one is yet present");
					utx.begin();
				}
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
		String correlationId = origin.getIdFromRawMessage(rawMessage, threadContext);
		String messageId = (String)threadContext.get("id");
		processMessageInAdapter(utx, origin, rawMessage, message, messageId, correlationId, threadContext, waitingDuration);
	}


	/*
	 * assumes message is read, and when transacted, transation is still open to be able to store it in InProcessStore
	 */
	private String processMessageInAdapter(UserTransaction utx, IListener origin, Object rawMessage, String message, String messageId, String correlationId, HashMap threadContext, long waitingDuration) throws ListenerException {
		String result=null;
		long startProcessingTimestamp = System.currentTimeMillis();
		log.debug(getLogPrefix()+"received message with messageId ["+messageId+"] correlationId ["+correlationId+"]");

		startProcessingMessage(waitingDuration);
		numReceived.increase();

		String errorMessage="";
		String inProcessMessageId=null;
		
		try {
			if (StringUtils.isEmpty(correlationId)) {
				correlationId=getName()+"-"+Misc.createSimpleUUID();
				if (log.isDebugEnabled()) 
					log.debug(getLogPrefix()+"generated correlationId ["+correlationId+"]");
			}
			if (StringUtils.isEmpty(messageId)) {
				messageId = correlationId;
			}
			
			if (isTransacted()) {
				inProcessMessageId = prepareToProcessMessageTransacted(utx,messageId,correlationId,rawMessage);
			}
			PipeLineSession pipelineSession = new PipeLineSession();
			if (threadContext!=null) {
					pipelineSession.putAll(threadContext);
					if (log.isDebugEnabled()) {
						String contextDump = "PipeLineSession variables for messageId ["+messageId+"] correlationId ["+correlationId+"]:";
						for (Iterator it=pipelineSession.keySet().iterator(); it.hasNext();) {
							String key = (String)it.next();
							Object value = pipelineSession.get(key);
							if (key.equals("messageText")) {
								value = "(... see elsewhere ...)";
							}
							contextDump+=" "+key+"=["+(value==null? "null": value.toString())+"]";
						}
						log.debug(contextDump);
					}
			}
			PipeLineResult pipeLineResult;
			if (isIbis42compatibility()) {
				pipeLineResult = adapter.processMessage(correlationId, message, pipelineSession);
				result=pipeLineResult.getResult();
				errorMessage = result;
			} else {
				try {
					pipeLineResult = adapter.processMessageWithExceptions(correlationId, message, pipelineSession);
					result=pipeLineResult.getResult();
					errorMessage = "exitState ["+pipeLineResult.getState()+"], result ["+result+"]";
				} catch (Throwable t) {
					if (isTransacted()) {
						try {
							adapter.getUserTransaction().setRollbackOnly();
						} catch (Throwable t2) {
							log.error("caught exception trying to invalidate transaction", t);
						}
					}
					ListenerException l;
					if (t instanceof ListenerException) {
						l = (ListenerException)t;
					} else {
						l = new ListenerException(t);
					}
					String msg = "receiver [" + getName() + "] caught exception in message processing";
					error(msg, l);
					errorMessage = l.getMessage();
					throw l;
				}
			}
			try {
				if (getSender()!=null) {
					getSender().sendMessage(correlationId,result);
				}
				origin.afterMessageProcessed(pipeLineResult,rawMessage, threadContext);
			} catch (Exception e) {
				String msg = "receiver [" + getName() + "] caught exception in message post processing";
				error(msg, e);
				errorMessage = msg+": "+e.getMessage();
				if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
					log.info("receiver [" + getName() + "] closing after exception in post processing");
					stopRunning();
				}
			}
		} finally {
//			if (isTransacted() && inProcessMessageId!=null) {
			if (isTransacted()) {
				finishTransactedProcessingOfMessage(utx,inProcessMessageId,messageId,correlationId,message, new Date(startProcessingTimestamp), errorMessage, (Serializable)rawMessage);
			}
			long finishProcessingTimestamp = System.currentTimeMillis();
			finishProcessingMessage(finishProcessingTimestamp-startProcessingTimestamp);
		}
		log.debug(getLogPrefix()+"returning result ["+result+"] for message ["+inProcessMessageId+"] correlationId ["+correlationId+"]");
		return result;
	}


  
    
	
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
			while (processStatistics.size()<threadsProcessing+1){
				result = new StatisticsKeeper((processStatistics.size()+1)+" threads processing");
				processStatistics.add(processStatistics.size(), result);
			}
		}
		
		return (StatisticsKeeper) processStatistics.get(threadsProcessing);
	}
	
	protected synchronized StatisticsKeeper getIdleStatistics(int threadsProcessing) {
		StatisticsKeeper result;
		try {
			result = ((StatisticsKeeper)idleStatistics.get(threadsProcessing));
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}

		if (result==null) {
			while (idleStatistics.size()<threadsProcessing+1){
			result = new StatisticsKeeper((idleStatistics.size())+" threads processing");
				idleStatistics.add(idleStatistics.size(), result);
			}
		}
		return (StatisticsKeeper) idleStatistics.get(threadsProcessing);
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
		if (listener instanceof RunStateEnquiring)  {
			((RunStateEnquiring) listener).SetRunStateEnquirer(runState);
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

	public ITransactionalStorage getErrorStorage() {
		if (errorStorage!=null) { 
			return errorStorage;
		}
		if (errorSender==null) {
			return inProcessStorage;
		}
		return null;
	}

	/**
	 * Sets the errorSender.
	 * @param errorSender The errorSender to set
	 */
	protected void setErrorSender(ISender errorSender) {
		this.errorSender = errorSender;
		errorSender.setName("errorSender of ["+getName()+"]");
	}

	protected void setErrorStorage(ITransactionalStorage errorStorage) {
		this.errorStorage = errorStorage;
		errorStorage.setName("errorStorage of ["+getName()+"]");
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


	public int getNumThreadsPolling() {
		return numThreadsPolling;
	}

	public void setNumThreadsPolling(int i) {
		numThreadsPolling = i;
	}

	public boolean isIbis42compatibility() {
		return ibis42compatibility;
	}

	public void setIbis42compatibility(boolean b) {
		ibis42compatibility = b;
	}
	

	public void setFileNameIfStopped(String fileNameIfStopped) {
		this.fileNameIfStopped = fileNameIfStopped;
	}
	public String getFileNameIfStopped() {
		return fileNameIfStopped;
	}


	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}
	public String getReplaceFrom() {
		return replaceFrom;
	}


	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}
	public String getReplaceTo() {
		return replaceTo;
	}


	// METT events
	
	public void eventOccurred(int eventNr) {
		if (eventNr>=0) {
			try {
				MonitorAccessor.eventOccurred(eventNr);
			} catch (Throwable t) {
				log.warn("Exception occured posting METT event",t);
			}
		}
	}

	public int getAfterEvent() {
		return afterEvent;
	}

	public int getBeforeEvent() {
		return beforeEvent;
	}

	public int getExceptionEvent() {
		return exceptionEvent;
	}

	public void setAfterEvent(int i) {
		afterEvent = i;
	}

	public void setBeforeEvent(int i) {
		beforeEvent = i;
	}

	public void setExceptionEvent(int i) {
		exceptionEvent = i;
	}


}
