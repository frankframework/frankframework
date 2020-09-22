/*
   Copyright 2013, 2015, 2016, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IBulkDataListener;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IKnowsDeliveryCount;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionRequirements;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.monitoring.EventHandler;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.senders.ConfigurationAware;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CompactSaxHandler;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.CounterStatistic;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.SpringTxManagerProxy;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * This {@link IReceiver Receiver} may be used as a base-class for developing receivers.
 *
 * <p>
 * THE FOLLOWING TO BE UPDATED, attribute 'transacted' replaced by 'transactionAttribute'. 
 * <table border="1">
 * <tr><th>{@link #setTransactionAttribute(String) transactionAttribute}</th><th>{@link #setTransacted(boolean) transacted}</th></tr>
 * <tr><td>Required</td><td>true</td></tr>
 * <tr><td>RequiresNew</td><td>true</td></tr>
 * <tr><td>Mandatory</td><td>true</td></tr>
 * <tr><td>otherwise</td><td>false</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <tr><th>nested elements (accessible in descender-classes)</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.IPullingListener listener}</td><td>the listener used to receive messages from</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ITransactionalStorage inProcessStorage}</td><td>mandatory for {@link #setTransacted(boolean) transacted} receivers: place to store messages during processing.</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ITransactionalStorage errorStorage}</td><td>optional for {@link #setTransacted(boolean) transacted} receivers: place to store messages if message processing has gone wrong. If no errorStorage is specified, the inProcessStorage is used for errorStorage</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender errorSender}</td><td>optional for {@link #setTransacted(boolean) transacted} receviers: 
 * will be called to store messages that failed to process. If no errorSender is specified, failed messages will remain in inProcessStorage</td></tr>
 * </table>
 * </p>
 * <p><b>Transaction control</b><br>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be received and processed under transaction control.
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
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be either committed or rolled back.
 * All message-processing transactions are committed, unless one or more of the following apply:
 * <ul>
 * <li>The PipeLine is transacted and the exitState of the pipeline is not equal to {@link nl.nn.adapterframework.core.PipeLine#setCommitOnState(String) commitOnState} (that defaults to 'success')</li>
 * <li>a PipeRunException or another runtime-exception has been thrown by any Pipe or by the PipeLine</li>
 * <li>the setRollBackOnly() method has been called on the userTransaction (not accessible by Pipes)</li>
 * </ul>
 * </p>
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class ReceiverBase<M> implements IReceiver<M>, IReceiverStatistics, IMessageHandler<M>, EventThrowing, IbisExceptionListener, HasSender, HasStatistics, IThreadCountControllable, BeanFactoryAware {
	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	public final static TransactionDefinition TXNEW_CTRL = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	public TransactionDefinition TXNEW_PROC;
	public final static TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);

	public static final String RCV_CONFIGURED_MONITOR_EVENT = "Receiver Configured";
	public static final String RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT = "Exception Configuring Receiver";
	public static final String RCV_STARTED_RUNNING_MONITOR_EVENT = "Receiver Started Running";
	public static final String RCV_SHUTDOWN_MONITOR_EVENT = "Receiver Shutdown";
	public static final String RCV_SUSPENDED_MONITOR_EVENT = "Receiver Operation Suspended";
	public static final String RCV_RESUMED_MONITOR_EVENT = "Receiver Operation Resumed";
	public static final String RCV_THREAD_EXIT_MONITOR_EVENT = "Receiver Thread Exited";
	public static final String RCV_MESSAGE_TO_ERRORSTORE_EVENT = "Receiver Moved Message to ErrorStorage";

	public static final String RCV_MESSAGE_LOG_COMMENTS = "log";

	public static final int RCV_SUSPENSION_MESSAGE_THRESHOLD=60;
	// Should be smaller than the transaction timeout as the delay takes place
	// within the transaction. WebSphere default transaction timeout is 120.
	public static final int MAX_RETRY_INTERVAL=100;

	public static final String ONERROR_CONTINUE = "continue";
	public static final String ONERROR_RECOVER = "recover";
	public static final String ONERROR_CLOSE = "close";

	private String returnedSessionKeys=null;
	private String hideRegex = null;
	private String hideMethod = "all";
	private String hiddenInputSessionKeys=null;
	private boolean checkForDuplicates=false;
	private String checkForDuplicatesMethod="MESSAGEID";
	private String correlationIDNamespaceDefs;
	private String correlationIDXPath;
	private String correlationIDStyleSheet;
	private String labelNamespaceDefs;
	private String labelXPath;
	private String labelStyleSheet;
	private String chompCharSize = null;
	private String elementToMove = null;
	private String elementToMoveSessionKey = null;
	private String elementToMoveChain = null;
	private boolean removeCompactMsgNamespaces = true;
	private boolean recover = false;
	private int pollInterval=10;

	private boolean active=true;
	private int transactionTimeout=0;

	private String name;
	private String onError = ONERROR_CONTINUE; 

	// the number of threads that may execute a pipeline concurrently (only for pulling listeners)
	private int numThreads = 1;
	// the number of threads that are actively polling for messages (concurrently, only for pulling listeners)
	private int numThreadsPolling = 1;

	private int maxDeliveries=5;
	private int maxRetries=1;


	private boolean suspensionMessagePending=false;
	private boolean configurationSucceeded = false;
	private BeanFactory beanFactory;

	protected RunStateManager runState = new RunStateManager();
	private PullingListenerContainer<M> listenerContainer;

	private Counter threadsProcessing = new Counter(0);

	private long lastMessageDate = 0;

	// number of messages received
	private CounterStatistic numReceived = new CounterStatistic(0);
	private CounterStatistic numRetried = new CounterStatistic(0);
	private CounterStatistic numRejected = new CounterStatistic(0);

	private List<StatisticsKeeper> processStatistics = new ArrayList<>();
	private List<StatisticsKeeper> idleStatistics = new ArrayList<>();
	private List<StatisticsKeeper> queueingStatistics;

	private StatisticsKeeper messageExtractionStatistics = new StatisticsKeeper("request extraction");

//	private StatisticsKeeper requestSizeStatistics = new StatisticsKeeper("request size");
//	private StatisticsKeeper responseSizeStatistics = new StatisticsKeeper("response size");

	// the adapter that handles the messages and initiates this listener
	private IAdapter adapter;

	private IListener<M> listener;
	private ISender errorSender=null;
	private IMessageBrowser<Serializable> errorStorage=null;
	// See configure() for explanation on this field
	private ITransactionalStorage<Serializable> tmpInProcessStorage=null;
	private ISender sender=null; // answer-sender
	private IMessageBrowser<Serializable> messageLog=null;
	private IMessageBrowser<Serializable> inProcessBrowser=null;
	
	//private boolean transacted=false;
	private int transactionAttribute=TransactionDefinition.PROPAGATION_SUPPORTS;

	private TransformerPool correlationIDTp=null;
	private TransformerPool labelTp=null;
 
	int retryInterval=1;
	private int poisonMessageIdCacheSize = 100;
	private int processResultCacheSize = 100;
   
	private PlatformTransactionManager txManager;

	private EventHandler eventHandler=null;

	/**
	 * The processResultCache acts as a sort of poor-mans error
	 * storage and is always available, even if an error-storage is not.
	 * Thus messages might be lost if they cannot be put in the error
	 * storage, but unless the server crashes, a message that has been
	 * put in the processResultCache will not be reprocessed even if it's
	 * offered again.
	 */
	private Map<String,ProcessResultCacheItem> processResultCache = new LinkedHashMap<String,ProcessResultCacheItem>() {

		@Override
		protected boolean removeEldestEntry(Entry<String,ProcessResultCacheItem> eldest) {
			return size() > getProcessResultCacheSize();
		}

	};

	private class ProcessResultCacheItem {
		int receiveCount;
		Date receiveDate;
		String comments;
	}

	public boolean configurationSucceeded() {
		return configurationSucceeded;
	}

	private IPipeLineSession createProcessingContext(String correlationId, Map<String,Object> threadContext, String messageId) {
		IPipeLineSession pipelineSession = new PipeLineSessionBase();
		if (threadContext != null) {
			pipelineSession.putAll(threadContext);
			if (log.isDebugEnabled()) {
				List<String> hiddenSessionKeys = new ArrayList<>();
				if (getHiddenInputSessionKeys()!=null) {
					StringTokenizer st = new StringTokenizer(getHiddenInputSessionKeys(), " ,;");
					while (st.hasMoreTokens()) {
						String key = st.nextToken();
						hiddenSessionKeys.add(key);
					}
				}

				String contextDump = "PipeLineSession variables for messageId [" + messageId + "] correlationId [" + correlationId + "]:";
				for (Iterator<String> it = pipelineSession.keySet().iterator(); it.hasNext();) {
					String key = it.next();
					Object value = pipelineSession.get(key);
					if (key.equals("messageText")) {
						value = "(... see elsewhere ...)";
					}
					String strValue = String.valueOf(value);
					contextDump += " " + key + "=[" + (hiddenSessionKeys.contains(key)?hide(strValue):strValue) + "]";
				}
				log.debug(getLogPrefix()+contextDump);
			}
		}
		return pipelineSession;
	}

	private String hide(String string) {
		String hiddenString = "";
		for (int i = 0; i < string.toString().length(); i++) {
			hiddenString = hiddenString + "*";
		}
		return hiddenString;
	}

	private void putSessionKeysIntoThreadContext(Map<String,Object>threadContext, IPipeLineSession pipelineSession) {
		if (StringUtils.isNotEmpty(getReturnedSessionKeys()) && threadContext != null) {
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix()+"setting returned session keys [" + getReturnedSessionKeys() + "]");
			}
			StringTokenizer st = new StringTokenizer(getReturnedSessionKeys(), " ,;");
			while (st.hasMoreTokens()) {
				String key = st.nextToken();
				Object value = pipelineSession.get(key);
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix()+"returning session key [" + key + "] value [" + value + "]");
				}
				threadContext.put(key, value);
			}
		}
	}



	protected String getLogPrefix() {
		return "Receiver ["+getName()+"] "; 
	}	

	/** 
	 * sends an informational message to the log and to the messagekeeper of the adapter
	 */
	protected void info(String msg) {
		log.info(getLogPrefix()+msg);
		if (adapter != null)
			adapter.getMessageKeeper().add(getLogPrefix() + msg);
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn(getLogPrefix()+msg);
		if (adapter != null)
			adapter.getMessageKeeper().add("WARNING: " + getLogPrefix() + msg, MessageKeeperLevel.WARN);
	}

	/** 
	 * sends a error message to the log and to the messagekeeper of the adapter
	 */
	protected void error(String msg, Throwable t) {
		log.error(getLogPrefix()+msg, t);
		if (adapter != null)
			adapter.getMessageKeeper().add("ERROR: " + getLogPrefix() + msg+(t!=null?": "+t.getMessage():""), MessageKeeperLevel.ERROR);
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
			if (getErrorStorage()!=null) {
				getErrorStorage().open();
			}
			if (getMessageLog()!=null) {
				getMessageLog().open();
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
		getListener().open();
		throwEvent(RCV_STARTED_RUNNING_MONITOR_EVENT);
		if (getListener() instanceof IPullingListener){
			// start all threads
			listenerContainer.start();
		}
	}

	protected void tellResourcesToStop() {
		// must lead to a 'closeAllResources()'
		// runstate is 'STOPPING'
		// default just calls 'closeAllResources()'
		if (getListener() instanceof IPushingListener) {
			closeAllResources();
		}
		// IPullingListeners stop as their threads finish, as the runstate is set to stopping
		// See PullingListenerContainer that calls receiver.isInRunState(RunStateEnum.STARTED)
		// and receiver.closeAllResources()
	}
	protected void closeAllResources() {
		// on exit resouces must be in a state that runstate can be set to 'STOPPED'
		log.debug(getLogPrefix()+"closing");
		try {
			getListener().close();
		} catch (Throwable t) {
			error("error closing listener", t);
		}
		if (getSender()!=null) {
			try {
				getSender().close();
			} catch (Throwable t) {
				error("error closing sender", t);
			}
		}
		if (getErrorSender()!=null) {
			try {
				getErrorSender().close();
			} catch (Throwable t) {
				error("error closing error sender", t);
			}
		}
		if (getErrorStorage()!=null) {
			try {
				getErrorStorage().close();
			} catch (Throwable t) {
				error("error closing error storage", t);
			}
		}
		if (getMessageLog()!=null) {
			try {
				getMessageLog().close();
			} catch (Throwable t) {
				error("error closing message log", t);
			}
		}
		log.debug(getLogPrefix()+"closed");
		runState.setRunState(RunStateEnum.STOPPED);
		throwEvent(RCV_SHUTDOWN_MONITOR_EVENT);
		resetRetryInterval();
		info("stopped");
	}
	 
	protected void propagateName() {
		IListener<M> listener=getListener();
		if (listener!=null && StringUtils.isEmpty(listener.getName())) {
			listener.setName("listener of ["+getName()+"]");
		}
		ISender errorSender = getErrorSender();
		if (errorSender != null) {
			errorSender.setName("errorSender of ["+getName()+"]");
		}
		ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
		if (errorStorage != null) {
			errorStorage.setName("errorStorage of ["+getName()+"]");
		}
		ISender answerSender = getSender();
		if (answerSender != null) {
			answerSender.setName("answerSender of ["+getName()+"]");
		}
	}

	@Override
	public void configure() throws ConfigurationException {		
		configurationSucceeded = false;
		try {
			if (StringUtils.isEmpty(getName())) {
				if (getListener()!=null) {
					setName(ClassUtils.nameOf(getListener()));
				} else {
					setName(ClassUtils.nameOf(this));
				}
			}
			eventHandler = MonitorManager.getEventHandler();
			registerEvent(RCV_CONFIGURED_MONITOR_EVENT);
			registerEvent(RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
			registerEvent(RCV_STARTED_RUNNING_MONITOR_EVENT);
			registerEvent(RCV_SHUTDOWN_MONITOR_EVENT);
			registerEvent(RCV_SUSPENDED_MONITOR_EVENT);
			registerEvent(RCV_RESUMED_MONITOR_EVENT);
			registerEvent(RCV_THREAD_EXIT_MONITOR_EVENT);
			TXNEW_PROC = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,getTransactionTimeout());
			// Check if we need to use the in-process storage as
			// error-storage.
			// In-process storage is no longer used, but is often
			// still configured to be used as error-storage.
			// The rule is:
			// 1. if error-storage is configured, use it.
			// 2. If error-storage is not configure but an error-sender is,
			//    then use the error-sender.
			// 3. If neither error-storage nor error-sender are configured,
			//    but the in-process storage is, then use the in-process storage
			//    for error-storage.
			// Member variables are accessed directly, to avoid any possible
			// aliasing-effects applied by getter methods. (These have been
			// removed for now, but since the getter-methods were not
			// straightforward in the earlier versions, I felt it was safer
			// to use direct member variables).
			if (this.tmpInProcessStorage != null) {
				if (this.errorSender == null && this.errorStorage == null) {
					this.errorStorage = this.tmpInProcessStorage;
					info("has errorStorage in inProcessStorage, setting inProcessStorage's type to 'errorStorage'. Please update the configuration to change the inProcessStorage element to an errorStorage element, since the inProcessStorage is no longer used.");
					getErrorStorage().setType(IMessageBrowser.StorageType.ERRORSTORAGE.getCode());
				} else {
					info("has inProcessStorage defined but also has an errorStorage or errorSender. InProcessStorage is not used and can be removed from the configuration.");
				}
				// Set temporary in-process storage pointer to null
				this.tmpInProcessStorage = null;
			}

			// Do propagate-name AFTER changing the errorStorage!
			propagateName();
			if (getListener()==null) {
				throw new ConfigurationException(getLogPrefix()+"has no listener");
			}
			if (!StringUtils.isEmpty(getElementToMove()) && !StringUtils.isEmpty(getElementToMoveChain())) {
				throw new ConfigurationException("cannot have both an elementToMove and an elementToMoveChain specified");
			}
			if (!(getHideMethod().equalsIgnoreCase("all")) && (!(getHideMethod().equalsIgnoreCase("firstHalf")))) {
				throw new ConfigurationException(getLogPrefix() + "invalid value for hideMethod [" + getHideMethod() + "], must be 'all' or 'firstHalf'");
			}
			if (getListener() instanceof ReceiverAware) {
				((ReceiverAware)getListener()).setReceiver(this);
			}
			if (getListener() instanceof IPushingListener) {
				IPushingListener<M> pl = (IPushingListener<M>)getListener();
				pl.setHandler(this);
				pl.setExceptionListener(this);
			}
			if (getListener() instanceof IPortConnectedListener) {
				IPortConnectedListener<M> pcl = (IPortConnectedListener<M>) getListener();
				pcl.setReceiver(this);
			}
			if (getListener() instanceof IPullingListener) {
				setListenerContainer(createListenerContainer());
			}
			if (getListener() instanceof JdbcFacade) {
				((JdbcFacade)getListener()).setTransacted(isTransacted());
			}
			if (getListener() instanceof JMSFacade) {
				((JMSFacade)getListener()).setTransacted(isTransacted());
			}
			getListener().configure();
			if (getListener() instanceof HasPhysicalDestination) {
				info("has listener on "+((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
			}
			if (getListener() instanceof HasSender) {
				// only informational
				ISender sender = ((HasSender)getListener()).getSender();
				if (sender instanceof HasPhysicalDestination) {
					info("Listener has answer-sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
			}
			if (getListener() instanceof ITransactionRequirements) {
				ITransactionRequirements tr=(ITransactionRequirements)getListener();
				if (tr.transactionalRequired() && !isTransacted()) {
					ConfigurationWarnings.add(this, log, "listener type ["+ClassUtils.nameOf(getListener())+"] requires transactional processing");
					//throw new ConfigurationException(msg);
				}
			}
			ISender sender = getSender();
			if (sender!=null) {
				sender.configure();
				if (sender instanceof HasPhysicalDestination) {
					info("has answer-sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
				if (sender instanceof ConfigurationAware) {
					((ConfigurationAware)sender).setConfiguration(getAdapter().getConfiguration());
				}
				
			}
			
			ISender errorSender = getErrorSender();
			if (errorSender!=null) {
				if (errorSender instanceof HasPhysicalDestination) {
					info("has errorSender to "+((HasPhysicalDestination)errorSender).getPhysicalDestinationName());
				}
				if (errorSender instanceof ConfigurationAware) {
					((ConfigurationAware)errorSender).setConfiguration(getAdapter().getConfiguration());
				}
				errorSender.configure();
			}
			ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
			if (errorStorage!=null) {
				errorStorage.configure();
				if (errorStorage instanceof HasPhysicalDestination) {
					info("has errorStorage to "+((HasPhysicalDestination)errorStorage).getPhysicalDestinationName());
				}
				registerEvent(RCV_MESSAGE_TO_ERRORSTORE_EVENT);
				if (getListener() instanceof IProvidesMessageBrowsers && ((IProvidesMessageBrowsers)getListener()).getErrorStoreBrowser()!=null) {
					ConfigurationWarnings.add(this, log, "Default errorStorageBrowser provided by listener is overridden by configured errorStorage");
				}
			} else {
				if (getListener() instanceof IProvidesMessageBrowsers) {
					IMessageBrowser errorStoreBrowser = ((IProvidesMessageBrowsers)getListener()).getErrorStoreBrowser();
					if (errorStoreBrowser instanceof IConfigurable) {
						((IConfigurable)errorStoreBrowser).configure();
					}
					this.errorStorage = errorStoreBrowser;
				}
			}
			ITransactionalStorage<Serializable> messageLog = getMessageLog();
			if (messageLog!=null) {
				messageLog.configure();
				if (messageLog instanceof HasPhysicalDestination) {
					info("has messageLog in "+((HasPhysicalDestination)messageLog).getPhysicalDestinationName());
				}
				if (StringUtils.isNotEmpty(getLabelXPath()) || StringUtils.isNotEmpty(getLabelStyleSheet())) {
					labelTp=TransformerPool.configureTransformer0(getLogPrefix(), classLoader, getLabelNamespaceDefs(), getLabelXPath(), getLabelStyleSheet(),"text",false,null,0);
				}
				if (getListener() instanceof IProvidesMessageBrowsers && ((IProvidesMessageBrowsers)getListener()).getMessageLogBrowser()!=null) {
					ConfigurationWarnings.add(this, log, "Default messageLogBrowser provided by listener is overridden by configured messageLog");
				}
			} else {
				if (getListener() instanceof IProvidesMessageBrowsers) {
					IMessageBrowser messageLogBrowser = ((IProvidesMessageBrowsers)getListener()).getMessageLogBrowser();
					if (messageLogBrowser!=null && messageLogBrowser instanceof IConfigurable) {
						((IConfigurable)messageLogBrowser).configure();
					}
					this.messageLog = messageLogBrowser;
				}
			}
			if (getListener() instanceof IProvidesMessageBrowsers) {
				IMessageBrowser inProcessBrowser = ((IProvidesMessageBrowsers)getListener()).getInProcessBrowser();
				if (inProcessBrowser!=null && inProcessBrowser instanceof IConfigurable) {
					((IConfigurable)inProcessBrowser).configure();
				}
				this.inProcessBrowser = inProcessBrowser;
			}
			
			if (isTransacted()) {
//				if (!(getListener() instanceof IXAEnabled && ((IXAEnabled)getListener()).isTransacted())) {
//					warn(getLogPrefix()+"sets transacted=true, but listener not. Transactional integrity is not guaranteed"); 
//				}
				
				if (errorSender==null && errorStorage==null) {
					ConfigurationWarnings.add(this, log, "sets transactionAttribute=" + getTransactionAttribute() + ", but has no errorSender or errorStorage. Messages processed with errors will be lost");
				} else {
//					if (errorSender!=null && !(errorSender instanceof IXAEnabled && ((IXAEnabled)errorSender).isTransacted())) {
//						warn(getLogPrefix()+"sets transacted=true, but errorSender is not. Transactional integrity is not guaranteed"); 
//					}
//					if (errorStorage!=null && !(errorStorage instanceof IXAEnabled && ((IXAEnabled)errorStorage).isTransacted())) {
//						warn(getLogPrefix()+"sets transacted=true, but errorStorage is not. Transactional integrity is not guaranteed"); 
//					}
				}

				if (getTransactionTimeout()>0) {
					String systemTransactionTimeout = Misc.getSystemTransactionTimeout();
					if (systemTransactionTimeout!=null && StringUtils.isNumeric(systemTransactionTimeout)) {
						int stt = Integer.parseInt(systemTransactionTimeout);
						if (getTransactionTimeout()>stt) {
							ConfigurationWarnings.add(this, log, "has a transaction timeout ["+getTransactionTimeout()+"] which exceeds the system transaction timeout ["+stt+"]");
						}
					}
				}
			} 

			if (StringUtils.isNotEmpty(getCorrelationIDXPath()) || StringUtils.isNotEmpty(getCorrelationIDStyleSheet())) {
				correlationIDTp=TransformerPool.configureTransformer0(getLogPrefix(), classLoader, getCorrelationIDNamespaceDefs(), getCorrelationIDXPath(), getCorrelationIDStyleSheet(),"text",false,null,0);
			}
			
			if (StringUtils.isNotEmpty(getHideRegex()) && getErrorStorage()!=null && StringUtils.isEmpty(getErrorStorage().getHideRegex())) {
				getErrorStorage().setHideRegex(getHideRegex());
				getErrorStorage().setHideMethod(getHideMethod());
			}
			if (StringUtils.isNotEmpty(getHideRegex()) && getMessageLog()!=null && StringUtils.isEmpty(getMessageLog().getHideRegex())) {
				getMessageLog().setHideRegex(getHideRegex());
				getMessageLog().setHideMethod(getHideMethod());
			}

			if (adapter != null) {
				adapter.getMessageKeeper().add(getLogPrefix()+"initialization complete");
			}
			throwEvent(RCV_CONFIGURED_MONITOR_EVENT);
			configurationSucceeded = true;
		} catch (Throwable t) {
			ConfigurationException e = null;
			if (t instanceof ConfigurationException) {
				e = (ConfigurationException)t;
			} else {
				e = new ConfigurationException("Exception configuring receiver ["+getName()+"]",t);
			}
			throwEvent(RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
			log.debug(getLogPrefix()+"Errors occured during configuration, setting runstate to ERROR");
			runState.setRunState(RunStateEnum.ERROR);
			throw e;
		}
	}


	@Override
	public void startRunning() {
		try {
			// if this receiver is on an adapter, the StartListening method
			// may only be executed when the adapter is started.
			if (adapter != null) {
				RunStateEnum adapterRunState = adapter.getRunState();
				if (!adapterRunState.equals(RunStateEnum.STARTED)) {
					log.warn(getLogPrefix()+"on adapter ["
							+ adapter.getName()
							+ "] was tried to start, but the adapter is in state ["+adapterRunState+"]. Ignoring command.");
					adapter.getMessageKeeper().add(
						"ignored start command on [" + getName()  + "]; adapter is in state ["+adapterRunState+"]");
					return;
				}
			}
			// See also Adapter.startRunning()
			if (!configurationSucceeded) {
				log.error(
					"configuration of receiver ["
						+ getName()
						+ "] did not succeed, therefore starting the receiver is not possible");
				warn("configuration did not succeed. Starting the receiver ["+getName()+"] is not possible");
				runState.setRunState(RunStateEnum.ERROR);
				return;
			}
			if (adapter.getConfiguration().isUnloadInProgressOrDone()) {
				log.error(
					"configuration of receiver ["
						+ getName()
						+ "] unload in progress or done, therefore starting the receiver is not possible");
				warn("configuration unload in progress or done. Starting the receiver ["+getName()+"] is not possible");
				return;
			}
			synchronized (runState) {
				RunStateEnum currentRunState = getRunState();
				if (!currentRunState.equals(RunStateEnum.STOPPED)) {
					if (currentRunState.equals(RunStateEnum.STARTING) || currentRunState.equals(RunStateEnum.STARTED)) {
						info("receiver already in state [" + currentRunState + "]");
					} else {
						warn(getLogPrefix()+"currently in state [" + currentRunState + "], ignoring start() command");
					}
					return;
				}
				runState.setRunState(RunStateEnum.STARTING);
			}
			String msg=(getLogPrefix()+"starts listening");
			log.info(msg);
			if (adapter != null) { 
				adapter.getMessageKeeper().add(msg);
			}
			openAllResources();
			runState.setRunState(RunStateEnum.STARTED);
		} catch (Throwable t) {
			error("error occured while starting", t);
			runState.setRunState(RunStateEnum.ERROR);
		}
	}
	
	@Override
	public void stopRunning() {
		// See also Adapter.stopRunning()
		synchronized (runState) {
			RunStateEnum currentRunState = getRunState();
			if (currentRunState.equals(RunStateEnum.STARTING)) {
				warn("receiver currently in state [" + currentRunState + "], ignoring stop() command");
				return;
			} else if (currentRunState.equals(RunStateEnum.STOPPING) || currentRunState.equals(RunStateEnum.STOPPED)) {
				info("receiver already in state [" + currentRunState + "]");
				return;
			}
			if (currentRunState.equals(RunStateEnum.ERROR)) {
				warn("receiver currently in state [" + currentRunState + "], stopping immediately");
				runState.setRunState(RunStateEnum.STOPPED);
			} else {
				runState.setRunState(RunStateEnum.STOPPING);
			}
		}
		tellResourcesToStop();
		ThreadContext.removeStack();
	}

	protected void startProcessingMessage(long waitingDuration) {
		synchronized (threadsProcessing) {
			int threadCount = (int) threadsProcessing.getValue();
			
			if (waitingDuration>=0) {
				getIdleStatistics(threadCount).addValue(waitingDuration);
			}
			threadsProcessing.increase();
		}
		log.debug(getLogPrefix()+"starts processing message");
	}

	protected void finishProcessingMessage(long processingDuration) {
		synchronized (threadsProcessing) {
			int threadCount = (int) threadsProcessing.decrease();
			getProcessStatistics(threadCount).addValue(processingDuration);
		}
		log.debug(getLogPrefix()+"finishes processing message");
	}

	private void moveInProcessToErrorAndDoPostProcessing(IListener<M> origin, String messageId, String correlationId, M rawMessage, Message message, Map<String,Object> threadContext, ProcessResultCacheItem prci, String comments) throws ListenerException {
		Date rcvDate;
		if (prci!=null) {
			comments+="; "+prci.comments;
			rcvDate=prci.receiveDate;
		} else {
			rcvDate=new Date();
		}
		if (isTransacted() || (getErrorStorage() != null && (!isCheckForDuplicates() || !getErrorStorage().containsMessageId(messageId) || !isDuplicateAndSkip(getErrorStorage(), messageId, correlationId)))) {
			moveInProcessToError(messageId, correlationId, message, rcvDate, comments, rawMessage, TXREQUIRED);
		}
		PipeLineResult plr = new PipeLineResult();
		Message result=new Message("<error>"+XmlUtils.encodeChars(comments)+"</error>");
		plr.setResult(result);
		plr.setState("ERROR");
		if (getSender()!=null) {
			// TODO correlationId should be technical correlationID!
			String sendMsg = sendResultToSender(correlationId, result);
			if (sendMsg != null) {
				log.warn("problem sending result:"+sendMsg);
			}
		}
		origin.afterMessageProcessed(plr, rawMessage, threadContext);
	}

	private void moveInProcessToError(String originalMessageId, String correlationId, Message message, Date receivedDate, String comments, Object rawMessage, TransactionDefinition txDef) {
		ISender errorSender = getErrorSender();
		ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
		if (errorSender==null && errorStorage==null) {
			log.debug(getLogPrefix()+"has no errorSender or errorStorage, will not move message with id ["+originalMessageId+"] correlationId ["+correlationId+"] to errorSender/errorStorage");
			return;
		}
		throwEvent(RCV_MESSAGE_TO_ERRORSTORE_EVENT);
		log.debug(getLogPrefix()+"moves message with id ["+originalMessageId+"] correlationId ["+correlationId+"] to errorSender/errorStorage");
		TransactionStatus txStatus = null;
		try {
			txStatus = txManager.getTransaction(txDef);
		} catch (Exception e) {
			log.error(getLogPrefix()+"Exception preparing to move input message with id [" + originalMessageId + "] to error sender", e);
			// no use trying again to send message on errorSender, will cause same exception!
			return;
		}
		try {
			if (errorSender!=null) {
				errorSender.sendMessage(message, null);
			}
			Serializable sobj;
			if (rawMessage == null) {
				if (message.isBinary()) {
					sobj = message.asByteArray();
				} else {
					sobj = message.asString();
				}
			} else {
				if (rawMessage instanceof Serializable) {
					sobj=(Serializable)rawMessage;
				} else {
					try {
						sobj = new MessageWrapper(rawMessage, getListener());
					} catch (ListenerException e) {
						log.error(getLogPrefix()+"could not wrap non serializable message for messageId ["+originalMessageId+"]",e);
						sobj=message;
					}
				}
			}
			if (errorStorage!=null) {
				errorStorage.storeMessage(originalMessageId, correlationId, receivedDate, comments, null, sobj);
			} 
			txManager.commit(txStatus);
		} catch (Exception e) {
			log.error(getLogPrefix()+"Exception moving message with id ["+originalMessageId+"] correlationId ["+correlationId+"] to error sender, original message: [" + message.toString() + "]", e);
			try {
				if (!txStatus.isCompleted()) {
					txManager.rollback(txStatus);
				}
			} catch (Exception rbe) {
				log.error(getLogPrefix()+"Exception while rolling back transaction for message  with id ["+originalMessageId+"] correlationId ["+correlationId+"], original message: [" + message.toString() + "]", rbe);
			}
		}
	}

	/**
	 * Process the received message with {@link #processRequest(IListener, String, Object, Message, Map, long)}.
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID
	 */
	@Override
	public Message processRequest(IListener<M> origin, M rawMessage, Message message) throws ListenerException {
		return processRequest(origin, null, rawMessage, message, null, -1);
	}

	@Override
	public Message processRequest(IListener<M> origin, String correlationId, M rawMessage, Message message)  throws ListenerException{
		return processRequest(origin, correlationId, rawMessage, message, null, -1);
	}

	@Override
	public Message processRequest(IListener<M> origin, String correlationId, M rawMessage, Message message, Map<String,Object> context) throws ListenerException {
		return processRequest(origin, correlationId, rawMessage, message, context, -1);
	}

	@Override
	public Message processRequest(IListener<M> origin, String correlationId, M rawMessage, Message message, Map<String,Object> context, long waitingTime) throws ListenerException {
		if (origin!=getListener()) {
			throw new ListenerException("Listener requested ["+origin.getName()+"] is not my Listener");
		}
		if (getRunState() != RunStateEnum.STARTED) {
			throw new ListenerException(getLogPrefix()+"is not started");
		}
		Date tsReceived = null;
		Date tsSent = null;
		if (context!=null) {
			//ClassCasting Exceptions occur when using PipeLineSessionBase.setListenerParameters, hence these silly instanceof's
			Object tsReceivedObj = context.get(IPipeLineSession.tsReceivedKey);
			Object tsSentObj = (Date)context.get(IPipeLineSession.tsSentKey);
			if(tsReceivedObj instanceof Date) {
				tsReceived = (Date) context.get(IPipeLineSession.tsReceivedKey);
			} else if(tsReceivedObj instanceof String) {
				tsReceived = DateUtils.parseToDate((String) tsReceivedObj, DateUtils.FORMAT_FULL_GENERIC);
			}
			if(tsSentObj instanceof Date) {
				tsSent = (Date) context.get(IPipeLineSession.tsSentKey);
			} else if(tsSentObj instanceof String) {
				tsSent = DateUtils.parseToDate((String) tsSentObj, DateUtils.FORMAT_FULL_GENERIC);
			}
		} else {
			context=new HashMap<>();
		}

		PipeLineSessionBase.setListenerParameters(context, null, correlationId, tsReceived, tsSent);
		String messageId = (String) context.get("id");
		return processMessageInAdapter(rawMessage, message, messageId, correlationId, context, waitingTime, false);
	}



	@Override
	public void processRawMessage(IListener<M> origin, M rawMessage) throws ListenerException {
		processRawMessage(origin, rawMessage, null, -1);
	}
	@Override
	public void processRawMessage(IListener<M> origin, M rawMessage, Map<String,Object> context) throws ListenerException {
		processRawMessage(origin, rawMessage, context, -1);
	}

	@Override
	public void processRawMessage(IListener<M> origin, M rawMessage, Map<String,Object>threadContext, long waitingDuration) throws ListenerException {
		if (origin!=getListener()) {
			throw new ListenerException("Listener requested ["+origin.getName()+"] is not my Listener");
		}
		processRawMessage(rawMessage, threadContext, waitingDuration, false);
	}

	/**
	 * All messages that for this receiver are pumped down to this method, so it actually
	 * calls the {@link nl.nn.adapterframework.core.Adapter adapter} to process the message.<br/>

	 * Assumes that a transation has been started where necessary.
	 */
	private void processRawMessage(Object rawMessageOrWrapper, Map<String,Object>threadContext, long waitingDuration, boolean manualRetry) throws ListenerException {
		if (rawMessageOrWrapper==null) {
			log.debug(getLogPrefix()+"received null message, returning directly");
			return;
		}
		long startExtractingMessage = System.currentTimeMillis();
		if (threadContext==null) {
			threadContext = new HashMap<>();
		}

		Message message = null;
		String technicalCorrelationId = null;
		try {
			message = getListener().extractMessage((M)rawMessageOrWrapper, threadContext);
		} catch (Exception e) {
			if(rawMessageOrWrapper instanceof MessageWrapper) { 
				//somehow messages wrapped in MessageWrapper are in the ITransactionalStorage 
				// There are, however, also Listeners that might use MessageWrapper as their raw message type,
				// like JdbcListener
				message = ((MessageWrapper)rawMessageOrWrapper).getMessage();
			} else {
				throw new ListenerException(e);
			}
		}
		try {
			technicalCorrelationId = getListener().getIdFromRawMessage((M)rawMessageOrWrapper, threadContext);
		} catch (Exception e) {
			if(rawMessageOrWrapper instanceof MessageWrapper) { //somehow messages wrapped in MessageWrapper are in the ITransactionalStorage 
				technicalCorrelationId = ((MessageWrapper)rawMessageOrWrapper).getId();
			} else {
				throw new ListenerException(e);
			}
		}
		String messageId = (String)threadContext.get("id");
		long endExtractingMessage = System.currentTimeMillis();
		messageExtractionStatistics.addValue(endExtractingMessage-startExtractingMessage);
		processMessageInAdapter(rawMessageOrWrapper, message, messageId, technicalCorrelationId, threadContext, waitingDuration, manualRetry);
	}

	
	public void retryMessage(String messageId) throws ListenerException {
		if (getErrorStorageBrowser()==null) {
			throw new ListenerException(getLogPrefix()+"has no errorStorage, cannot retry messageId ["+messageId+"]");
		}
		Map<String,Object>threadContext = new HashMap<>();
		if (getErrorStorage()==null) {
			// if there is only a errorStorageBrowser, and no separate and transactional errorStorage,
			// then the management of the errorStorage is left to the listener.
			IMessageBrowser errorStorageBrowser = getErrorStorageBrowser();
			Object msg = errorStorageBrowser.browseMessage(messageId);
			processRawMessage(msg, threadContext, -1, true);
			return;
		}
		PlatformTransactionManager txManager = getTxManager(); 
		//TransactionStatus txStatus = txManager.getTransaction(TXNEW);
		IbisTransaction itx = new IbisTransaction(txManager, TXNEW_PROC, "receiver [" + getName() + "]");
		TransactionStatus txStatus = itx.getStatus();
		Serializable msg=null;
		ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
		try {
			try {
				msg = errorStorage.getMessage(messageId);
				processRawMessage(msg, threadContext, -1, true);
			} catch (Throwable t) {
				txStatus.setRollbackOnly();
				throw new ListenerException(t);
			} finally {
				txManager.commit(txStatus);
			}
		} catch (ListenerException e) {
			txStatus = txManager.getTransaction(TXNEW_CTRL);
			try {	
				if (msg instanceof Serializable) {
					String correlationId = (String)threadContext.get(IPipeLineSession.businessCorrelationIdKey);
					String receivedDateStr = (String)threadContext.get(IPipeLineSession.tsReceivedKey);
					if (receivedDateStr==null) {
						log.warn(getLogPrefix()+IPipeLineSession.tsReceivedKey+" is unknown, cannot update comments");
					} else {
						Date receivedDate = DateUtils.parseToDate(receivedDateStr,DateUtils.FORMAT_FULL_GENERIC);
						errorStorage.deleteMessage(messageId);
						errorStorage.storeMessage(messageId,correlationId,receivedDate,"after retry: "+e.getMessage(),null, msg);	
					}
				} else {
					log.warn(getLogPrefix()+"retried message is not serializable, cannot update comments");
				}
			} catch (SenderException e1) {
				txStatus.setRollbackOnly();
				log.warn(getLogPrefix()+"could not update comments in errorStorage",e1);
			} finally {
				txManager.commit(txStatus);
			}
			throw e;
		}
	}

	/*
	 * Assumes message is read, and when transacted, transaction is still open.
	 */
	private Message processMessageInAdapter(Object rawMessageOrWrapper, Message message, String messageId, String technicalCorrelationId, Map<String,Object>threadContext, long waitingDuration, boolean manualRetry) throws ListenerException {
		long startProcessingTimestamp = System.currentTimeMillis();
//		if (message==null) {
//			requestSizeStatistics.addValue(0);
//		} else {
//			requestSizeStatistics.addValue(message.length());
//		}
		lastMessageDate = startProcessingTimestamp;
		log.debug(getLogPrefix()+"received message with messageId ["+messageId+"] (technical) correlationId ["+technicalCorrelationId+"]");

		if (StringUtils.isEmpty(messageId)) {
			messageId=Misc.createSimpleUUID();
			if (log.isDebugEnabled()) 
				log.debug(getLogPrefix()+"generated messageId ["+messageId+"]");
		}

		if (getChompCharSize() != null || getElementToMove() != null || getElementToMoveChain() != null) {
			log.debug(getLogPrefix()+"compact received message");
			try {
				CompactSaxHandler handler = new CompactSaxHandler();
				handler.setChompCharSize(getChompCharSize());
				handler.setElementToMove(getElementToMove());
				handler.setElementToMoveChain(getElementToMoveChain());
				handler.setElementToMoveSessionKey(getElementToMoveSessionKey());
				handler.setRemoveCompactMsgNamespaces(isRemoveCompactMsgNamespaces());
				handler.setContext(threadContext);
				try {
					XmlUtils.parseXml(message.asInputSource(), handler);
					message = new Message(handler.getXmlString());
				} catch (Exception e) {
					warn("received message could not be compacted: " + e.getMessage());
				}
				handler = null;
			} catch (Exception e) {
				String msg="error during compacting received message to more compact format";
				error(msg, e);
				throw new ListenerException(msg, e);
			}
		}
		
		String businessCorrelationId=null;
		if (correlationIDTp!=null) {
			try {
				message.preserve();
				businessCorrelationId=correlationIDTp.transform(message,null);
			} catch (Exception e) {
				//throw new ListenerException(getLogPrefix()+"could not extract businessCorrelationId",e);
				log.warn(getLogPrefix()+"could not extract businessCorrelationId");
			}
			if (StringUtils.isEmpty(businessCorrelationId)) {
				String cidText;
				if (StringUtils.isNotEmpty(getCorrelationIDXPath())) {
					cidText = "xpathExpression ["+getCorrelationIDXPath()+"]";
				} else {
					cidText = "styleSheet ["+getCorrelationIDStyleSheet()+"]";
				}
				if (StringUtils.isNotEmpty(technicalCorrelationId)) {
					log.info(getLogPrefix()+"did not find correlationId using "+cidText+", reverting to correlationId of transfer ["+technicalCorrelationId+"]");
					businessCorrelationId=technicalCorrelationId;
				}
			}
		} else {
			businessCorrelationId=technicalCorrelationId;
		}
		if (StringUtils.isEmpty(businessCorrelationId)) {
			if (StringUtils.isNotEmpty(messageId)) {
				log.info(getLogPrefix()+"did not find (technical) correlationId, reverting to messageId ["+messageId+"]");
				businessCorrelationId=messageId;
			}
		}
		log.info(getLogPrefix()+"messageId [" + messageId + "] technicalCorrelationId [" + technicalCorrelationId + "] businessCorrelationId [" + businessCorrelationId + "]");
		threadContext.put(IPipeLineSession.businessCorrelationIdKey, businessCorrelationId);
		String label=null;
		if (labelTp!=null) {
			try {
				message.preserve();
				label=labelTp.transform(message,null);
			} catch (Exception e) {
				//throw new ListenerException(getLogPrefix()+"could not extract label",e);
				log.warn(getLogPrefix()+"could not extract label: ("+ClassUtils.nameOf(e)+") "+e.getMessage());
			}
		}
		try {
			if (hasProblematicHistory(messageId, manualRetry, rawMessageOrWrapper, message, threadContext, businessCorrelationId)) {
				if (!isTransacted()) {
					log.warn(getLogPrefix()+"received message with messageId [" + messageId + "] which has a problematic history; aborting processing");
				}
				numRejected.increase();
				setExitState(threadContext, "rejected", 500);
				return Message.nullMessage();
			}
			if (isDuplicateAndSkip(getMessageLog(), messageId, businessCorrelationId)) {
				numRejected.increase();
				setExitState(threadContext, "success", 304);
				return Message.nullMessage();
			}
			if (getCachedProcessResult(messageId)!=null) {
				numRetried.increase();
			}
		} catch (Exception e) { 
			String msg="exception while checking history";
			error(msg, e);
			throw wrapExceptionAsListenerException(e);
		}
		
		int txOption = this.getTransactionAttributeNum();
		TransactionDefinition txDef = SpringTxManagerProxy.getTransactionDefinition(txOption,getTransactionTimeout());
		//TransactionStatus txStatus = txManager.getTransaction(txDef);
		IbisTransaction itx = new IbisTransaction(txManager, txDef, "receiver [" + getName() + "]");
		TransactionStatus txStatus = itx.getStatus();

		// update processing statistics
		// count in processing statistics includes messages that are rolled back to input
		startProcessingMessage(waitingDuration);
		
		IPipeLineSession pipelineSession = null;
		String errorMessage="";
		boolean messageInError = false;
		String result=null;
		PipeLineResult pipeLineResult=null;
		try {
			Message pipelineMessage;
			if (getListener() instanceof IBulkDataListener) {
				try {
					IBulkDataListener bdl = (IBulkDataListener)getListener(); 
					pipelineMessage=new Message(bdl.retrieveBulkData(rawMessageOrWrapper,message,threadContext));
				} catch (Throwable t) {
					errorMessage = t.getMessage();
					messageInError = true;
					error("exception retrieving bulk data", t);
					ListenerException l = wrapExceptionAsListenerException(t);
					throw l;
				}
			} else {
				pipelineMessage=message;
			}
			
			numReceived.increase();
			// Note: errorMessage is used to pass value from catch-clause to finally-clause!
			pipelineSession = createProcessingContext(businessCorrelationId, threadContext, messageId);
//			threadContext=pipelineSession; // this is to enable Listeners to use session variables, for instance in afterProcessMessage()
			try {
				if (getMessageLog()!=null) {
					getMessageLog().storeMessage(messageId, businessCorrelationId, new Date(), RCV_MESSAGE_LOG_COMMENTS, label, pipelineMessage);
				}
				log.debug(getLogPrefix()+"preparing TimeoutGuard");
				TimeoutGuard tg = new TimeoutGuard("Receiver "+getName());
				try {
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"activating TimeoutGuard with transactionTimeout ["+transactionTimeout+"]s");
					tg.activateGuard(getTransactionTimeout());
					pipeLineResult = adapter.processMessageWithExceptions(businessCorrelationId, pipelineMessage, pipelineSession);
					setExitState(threadContext, pipeLineResult.getState(), pipeLineResult.getExitCode());
					pipelineSession.put("exitcode", ""+ pipeLineResult.getExitCode());
					result=pipeLineResult.getResult().asString();
					if(result != null && result.length() > ITransactionalStorage.MAXCOMMENTLEN) {
						errorMessage = "exitState ["+pipeLineResult.getState()+"], result ["+result.substring(0, ITransactionalStorage.MAXCOMMENTLEN)+"]";
					}else {
						errorMessage = "exitState ["+pipeLineResult.getState()+"], result ["+result+"]";
					}
					int status = pipeLineResult.getExitCode();
					if(status > 0) {
						errorMessage += ", exitcode ["+status+"]";
					}
					if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"received result: "+errorMessage); }
					messageInError=txStatus.isRollbackOnly();
				} finally {
					log.debug(getLogPrefix()+"canceling TimeoutGuard, isInterrupted ["+Thread.currentThread().isInterrupted()+"]");
					if (tg.cancel()) {
						errorMessage = "timeout exceeded";
						if (StringUtils.isEmpty(result)) {
							result="<timeout/>";
						}
						messageInError=true;
					}
				}
				if (!messageInError && !isTransacted()) {
					String commitOnState=((Adapter)adapter).getPipeLine().getCommitOnState();
					
					if (StringUtils.isNotEmpty(commitOnState) && 
						!commitOnState.equalsIgnoreCase(pipeLineResult.getState())) {
						messageInError=true;
					}
				}
			} catch (Throwable t) {
				if (TransactionSynchronizationManager.isActualTransactionActive()) {
					log.debug("<*>"+getLogPrefix() + "TX Update: Received failure, transaction " + (txStatus.isRollbackOnly()?"already":"not yet") + " marked for rollback-only");
				}
				error("Exception in message processing", t);
				errorMessage = t.getMessage();
				messageInError = true;
				if (pipeLineResult==null) {
					pipeLineResult=new PipeLineResult();
				}
				if (pipeLineResult.getResult()==null || pipeLineResult.getResult().isEmpty()) {
					String formattedErrorMessage=adapter.formatErrorMessage("exception caught",t,message,messageId,this,startProcessingTimestamp);
					pipeLineResult.setResult(new Message(formattedErrorMessage));
				}
				throw wrapExceptionAsListenerException(t);
			} finally {
				putSessionKeysIntoThreadContext(threadContext, pipelineSession);
			}
//			if (result==null) {
//				responseSizeStatistics.addValue(0);
//			} else {
//				responseSizeStatistics.addValue(result.length());
//			}
			if (getSender()!=null) {
				String sendMsg = sendResultToSender(technicalCorrelationId, new Message(result));
				if (sendMsg != null) {
					errorMessage = sendMsg;
				}
			}
		} finally {
			cacheProcessResult(messageId, errorMessage, new Date(startProcessingTimestamp));
			if (!isTransacted() && messageInError) {
				if (!manualRetry) {
					moveInProcessToError(messageId, businessCorrelationId, message, new Date(startProcessingTimestamp), errorMessage, rawMessageOrWrapper, TXNEW_CTRL);
				}
			}
			try {
				Map<String,Object> afterMessageProcessedMap=threadContext;
				if (pipelineSession!=null) {
					threadContext.putAll(pipelineSession);
				}
				try {
					getListener().afterMessageProcessed(pipeLineResult, rawMessageOrWrapper, afterMessageProcessedMap);
				} catch (Exception e) {
					if (manualRetry) {
						// Somehow messages wrapped in MessageWrapper are in the ITransactionalStorage. 
						// This might cause class cast exceptions.
						// There are, however, also Listeners that might use MessageWrapper as their raw message type,
						// like JdbcListener
						error("Exception post processing after retry of message messageId ["+messageId+"] cid ["+technicalCorrelationId+"]", e); 
					} else {
						error("Exception post processing message messageId ["+messageId+"] cid ["+technicalCorrelationId+"]", e);
					}
					throw wrapExceptionAsListenerException(e);
				}
			} finally {
				long finishProcessingTimestamp = System.currentTimeMillis();
				finishProcessingMessage(finishProcessingTimestamp-startProcessingTimestamp);
				if (!txStatus.isCompleted()) {
					// NB: Spring will take care of executing a commit or a rollback;
					// Spring will also ONLY commit the transaction if it was newly created
					// by the above call to txManager.getTransaction().
					//txManager.commit(txStatus);
					itx.commit();
				} else {
					String msg="Transaction already completed; we didn't expect this";
					warn(msg);
					throw new ListenerException(getLogPrefix()+msg);
				}
			}
		}
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"messageId ["+messageId+"] correlationId ["+businessCorrelationId+"] returning result ["+result+"]");
		return new Message(result);
	}

	private void setExitState(Map<String,Object> threadContext, String state, int code) {
		if (threadContext!=null) {
			threadContext.put(IPipeLineSession.EXIT_STATE_CONTEXT_KEY, state);
			threadContext.put(IPipeLineSession.EXIT_CODE_CONTEXT_KEY, code);
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private synchronized void cacheProcessResult(String messageId, String errorMessage, Date receivedDate) {
		ProcessResultCacheItem cacheItem=getCachedProcessResult(messageId);
		if (cacheItem==null) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"caching first result for messageId ["+messageId+"]");
			cacheItem= new ProcessResultCacheItem();
			cacheItem.receiveCount=1;
			cacheItem.receiveDate=receivedDate;
		} else {
			cacheItem.receiveCount++;
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"increased try count for messageId ["+messageId+"] to ["+cacheItem.receiveCount+"]");
		}
		cacheItem.comments=errorMessage;
		processResultCache.put(messageId, cacheItem);
	}
	private synchronized ProcessResultCacheItem getCachedProcessResult(String messageId) {
		return processResultCache.get(messageId);
	}

	/*
	 * returns true if message should not be processed
	 */
	@SuppressWarnings("unchecked")
	private boolean hasProblematicHistory(String messageId, boolean manualRetry, Object rawMessageOrWrapper, Message message, Map<String,Object>threadContext, String correlationId) throws ListenerException {
		if (!manualRetry) {
			IListener<M> origin = getListener(); // N.B. listener is not used when manualRetry==true
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"checking try count for messageId ["+messageId+"]");
			ProcessResultCacheItem prci = getCachedProcessResult(messageId);
			if (prci==null) {
				if (getMaxDeliveries()!=-1) {
					int deliveryCount=-1;
					if (origin instanceof IKnowsDeliveryCount) {
						deliveryCount = ((IKnowsDeliveryCount<M>)origin).getDeliveryCount((M)rawMessageOrWrapper); // cast to M is done only if !manualRetry
					}
					if (deliveryCount>1) {
						log.warn(getLogPrefix()+"message with messageId ["+messageId+"] has delivery count ["+(deliveryCount)+"]");
					}
					if (deliveryCount>getMaxDeliveries()) {
						warn("message with messageId ["+messageId+"] has already been delivered ["+deliveryCount+"] times, will not process; maxDeliveries=["+getMaxDeliveries()+"]");
						String comments="too many deliveries";
						increaseRetryIntervalAndWait(null,getLogPrefix()+"received message with messageId ["+messageId+"] too many times ["+deliveryCount+"]; maxDeliveries=["+getMaxDeliveries()+"]");
						moveInProcessToErrorAndDoPostProcessing(origin, messageId, correlationId, (M)rawMessageOrWrapper, message, threadContext, prci, comments); // cast to M is done only if !manualRetry
						return true;
					}
				}
				resetRetryInterval();
				return false;
			} else {
				if (getMaxRetries()<0) {
					increaseRetryIntervalAndWait(null,getLogPrefix()+"message with messageId ["+messageId+"] has already been received ["+prci.receiveCount+"] times; maxRetries=["+getMaxRetries()+"]");
					return false;
				}
				if (prci.receiveCount<=getMaxRetries()) {
					log.warn(getLogPrefix()+"message with messageId ["+messageId+"] has already been received ["+prci.receiveCount+"] times, will try again; maxRetries=["+getMaxRetries()+"]");
					resetRetryInterval();
					return false;
				}
				warn("message with messageId ["+messageId+"] has already been received ["+prci.receiveCount+"] times, will not try again; maxRetries=["+getMaxRetries()+"]");
				String comments="too many retries";
				if (prci.receiveCount>getMaxRetries()+1) {
					increaseRetryIntervalAndWait(null,getLogPrefix()+"received message with messageId ["+messageId+"] too many times ["+prci.receiveCount+"]; maxRetries=["+getMaxRetries()+"]");
				}
				moveInProcessToErrorAndDoPostProcessing(origin, messageId, correlationId, (M)rawMessageOrWrapper, message, threadContext, prci, comments); // cast to M is done only if !manualRetry
				prci.receiveCount++; // make sure that the next time this message is seen, the retry interval will be increased
				return true;
			}
		} 
		if (isCheckForDuplicates() && getMessageLog()!= null && getMessageLog().containsMessageId(messageId)) {
			return true;
		}
		return false;
	}

	/*
	 * returns true if message should not be processed
	 */
	private boolean isDuplicateAndSkip(IMessageBrowser<Serializable> transactionStorage, String messageId, String correlationId) throws ListenerException {
		if (isCheckForDuplicates() && transactionStorage != null) {
			if ("CORRELATIONID".equalsIgnoreCase(getCheckForDuplicatesMethod())) {
				if (transactionStorage.containsCorrelationId(correlationId)) {
					warn("message with correlationId [" + correlationId + "] already exists in messageLog, will not process");
					return true;
				}
			} else {
				if (transactionStorage.containsMessageId(messageId)) {
					warn("message with messageId [" + messageId + "] already exists in in messageLog, will not process");
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void exceptionThrown(INamedObject object, Throwable t) {
		String msg = getLogPrefix()+"received exception ["+t.getClass().getName()+"] from ["+object.getName()+"]";
		if (ONERROR_CONTINUE.equalsIgnoreCase(getOnError())) {
//			warn(msg+", will continue processing messages when they arrive: "+ t.getMessage());
			error(msg+", will continue processing messages when they arrive",t);
		} else if (ONERROR_RECOVER.equalsIgnoreCase(getOnError())) {
			// Make JobDef.recoverAdapters() try to recover
			setRunState(RunStateEnum.ERROR);
			error(msg+", will try to recover",t);
		} else {
			error(msg+", stopping receiver", t);
			stopRunning();
		}
	}

	@Override
	public String getEventSourceName() {
		return getLogPrefix().trim();
	}
	protected void registerEvent(String eventCode) {
		if (eventHandler!=null) {
			eventHandler.registerEvent(this,eventCode);
		}		
	}
	protected void throwEvent(String eventCode) {
		if (eventHandler!=null) {
			eventHandler.fireEvent(this,eventCode);
		}
	}

	public void resetRetryInterval() {
		synchronized (this) {
			if (suspensionMessagePending) {
				suspensionMessagePending=false;
				throwEvent(RCV_RESUMED_MONITOR_EVENT);
			}
			retryInterval = 1;
		}
	}

	public void increaseRetryIntervalAndWait(Throwable t, String description) {
		long currentInterval;
		synchronized (this) {
			currentInterval = retryInterval;
			retryInterval = retryInterval * 2;
			if (retryInterval > MAX_RETRY_INTERVAL) {
				retryInterval = MAX_RETRY_INTERVAL;
			}
		}
		if (currentInterval>1) {
			error(description+", will continue retrieving messages in [" + currentInterval + "] seconds", t);
		} else {
			log.warn(getLogPrefix()+"will continue retrieving messages in [" + currentInterval + "] seconds", t);
		}
		if (currentInterval*2 > RCV_SUSPENSION_MESSAGE_THRESHOLD) {
			if (!suspensionMessagePending) {
				suspensionMessagePending=true;
				throwEvent(RCV_SUSPENDED_MONITOR_EVENT);
			}
		}
		while (isInRunState(RunStateEnum.STARTED) && currentInterval-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (Exception e2) {
				error("sleep interupted", e2);
				stopRunning();
			}
		}
	}
	

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		Object recData=hski.openGroup(data,getName(),"receiver");
		hski.handleScalar(recData,"messagesReceived", getMessagesReceived());
		hski.handleScalar(recData,"messagesRetried", getMessagesRetried());
		hski.handleScalar(recData,"messagesRejected", numRejected.getValue());
		hski.handleScalar(recData,"messagesReceivedThisInterval", numReceived.getIntervalValue());
		hski.handleScalar(recData,"messagesRetriedThisInterval", numRetried.getIntervalValue());
		hski.handleScalar(recData,"messagesRejectedThisInterval", numRejected.getIntervalValue());
		numReceived.performAction(action);
		numRetried.performAction(action);
		numRejected.performAction(action);
		messageExtractionStatistics.performAction(action);
		Iterator<StatisticsKeeper> statsIter=getProcessStatisticsIterator();
		Object pstatData=hski.openGroup(recData,null,"procStats");
		if (statsIter != null) {
			while(statsIter.hasNext()) {
				StatisticsKeeper pstat = statsIter.next();
				hski.handleStatisticsKeeper(pstatData,pstat);
				pstat.performAction(action);
			}
		}
		hski.closeGroup(pstatData);

		statsIter = getIdleStatisticsIterator();
		if (statsIter != null) {
			Object istatData=hski.openGroup(recData,null,"idleStats");
			while(statsIter.hasNext()) {
				StatisticsKeeper pstat = statsIter.next();
				hski.handleStatisticsKeeper(istatData,pstat);
				pstat.performAction(action);
			}
			hski.closeGroup(istatData);
		}

		statsIter = getQueueingStatisticsIterator();
		if (statsIter!=null) {
			Object qstatData=hski.openGroup(recData,null,"queueingStats");
			while(statsIter.hasNext()) {
				StatisticsKeeper qstat = statsIter.next();
				hski.handleStatisticsKeeper(qstatData,qstat);
				qstat.performAction(action);
			}
			hski.closeGroup(qstatData);
		}


		hski.closeGroup(recData);
	}



	@Override
	public boolean isThreadCountReadable() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			return tcc.isThreadCountReadable();
		}
		if (getListener() instanceof IPullingListener) {
			return true;
		}
		return false;
	}
	@Override
	public boolean isThreadCountControllable() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			return tcc.isThreadCountControllable();
		}
		if (getListener() instanceof IPullingListener) {
			return true;
		}
		return false;
	}

	@Override
	public int getCurrentThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			return tcc.getCurrentThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			return listenerContainer.getCurrentThreadCount();
		}
		return -1;
	}

	@Override
	public int getMaxThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();

			return tcc.getMaxThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			return listenerContainer.getMaxThreadCount();
		}
		return -1;
	}

	@Override
	public void increaseThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			tcc.increaseThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			listenerContainer.increaseThreadCount();
		}
	}

	@Override
	public void decreaseThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			tcc.decreaseThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			listenerContainer.decreaseThreadCount();
		}
	}

	public void setRunState(RunStateEnum state) {
		runState.setRunState(state);
	}

//	public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
//		runState.waitForRunState(requestedRunState);
//	}
//	public boolean waitForRunState(RunStateEnum requestedRunState, long timeout) throws InterruptedException {
//		return runState.waitForRunState(requestedRunState, timeout);
//	}
	
		/**
		 * Get the {@link RunStateEnum runstate} of this receiver.
		 */
	@Override
	public RunStateEnum getRunState() {
		return runState.getRunState();
	}
	
	public boolean isInRunState(RunStateEnum someRunState) {
		return runState.isInState(someRunState);
	}

	protected synchronized StatisticsKeeper getProcessStatistics(int threadsProcessing) {
		StatisticsKeeper result;
		try {
			result = processStatistics.get(threadsProcessing);
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}
	
		if (result==null) {
			while (processStatistics.size()<threadsProcessing+1){
				result = new StatisticsKeeper((processStatistics.size()+1)+" threads processing");
				processStatistics.add(processStatistics.size(), result);
			}
		}
		
		return processStatistics.get(threadsProcessing);
	}
	
	protected synchronized StatisticsKeeper getIdleStatistics(int threadsProcessing) {
		StatisticsKeeper result;
		try {
			result = idleStatistics.get(threadsProcessing);
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}

		if (result==null) {
			while (idleStatistics.size()<threadsProcessing+1){
			result = new StatisticsKeeper((idleStatistics.size())+" threads processing");
				idleStatistics.add(idleStatistics.size(), result);
			}
		}
		return idleStatistics.get(threadsProcessing);
	}
	
	/**
	 * Returns an iterator over the process-statistics
	 * @return iterator
	 */
	@Override
	public Iterator<StatisticsKeeper> getProcessStatisticsIterator() {
		return processStatistics.iterator();
	}
	
	/**
	 * Returns an iterator over the idle-statistics
	 * @return iterator
	 */
	@Override
	public Iterator<StatisticsKeeper> getIdleStatisticsIterator() {
		return idleStatistics.iterator();
	}
	public Iterator<StatisticsKeeper> getQueueingStatisticsIterator() {
		if (queueingStatistics==null) {
			return null;
		}
		return queueingStatistics.iterator();
	}		
	
	@Override
	public ISender getSender() {
		return sender;
	}
	
	protected void setSender(ISender sender) {
		this.sender = sender;
	}

	@Override
	public void setAdapter(IAdapter adapter) {
		this.adapter = adapter;
	}
	
	
	
	/**
	 * Returns the listener
	 * @return IListener
	 */
	@Override
	public IListener<M> getListener() {
		return listener;
	}/**
	 * Sets the listener. If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and no <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 * Creation date: (04-11-2003 12:04:05)
	 * @param newListener IListener
	 */
	protected void setListener(IListener<M> newListener) {
		listener = newListener;
		if (StringUtils.isEmpty(listener.getName())) {
			listener.setName("listener of ["+getName()+"]");
		}
		if (listener instanceof RunStateEnquiring)  {
			((RunStateEnquiring) listener).SetRunStateEnquirer(runState);
		}
	}

	/**
	 * Sets the inProcessStorage.
	 * @param inProcessStorage The inProcessStorage to set
	 * @deprecated
	 */
	@Deprecated
	@ConfigurationWarning("In-Process Storage no longer exists")
	protected void setInProcessStorage(ITransactionalStorage<Serializable> inProcessStorage) {
		// We do not use an in-process storage anymore, but we temporarily
		// store it if it's set by the configuration.
		// During configure, we check if we need to use the in-process storage
		// as error-storage.
		this.tmpInProcessStorage = inProcessStorage;
	}

	/**
	 * Returns the errorSender.
	 * @return ISender
	 */
	public ISender getErrorSender() {
		return errorSender;
	}

	/**
	 * returns a browser for the errorStorage, either provided as a {@link IMessageBrowser} by the listener itself, or as a {@link ITransactionalStorage} in the configuration. 
	 */
	public IMessageBrowser<Serializable> getErrorStorageBrowser() {
		return errorStorage;
	}
	/**
	 * returns the {@link ITransactionalStorage} if it is provided in the configuration. It is used to store failed messages. If present, this storage will be managed by the Receiver.
	 */
	public ITransactionalStorage<Serializable> getErrorStorage() {
		return errorStorage!=null && errorStorage instanceof ITransactionalStorage ? (ITransactionalStorage)errorStorage: null;
	}

	/**
	 * Sets the errorSender.
	 * @param errorSender The errorSender to set
	 */
	protected void setErrorSender(ISender errorSender) {
		this.errorSender = errorSender;
		errorSender.setName("errorSender of ["+getName()+"]");
	}

	protected void setErrorStorage(ITransactionalStorage<Serializable> errorStorage) {
		if (errorStorage.isActive()) {
			this.errorStorage = errorStorage;
			errorStorage.setName("errorStorage of ["+getName()+"]");
			if (StringUtils.isEmpty(errorStorage.getSlotId())) {
				errorStorage.setSlotId(getName());
			}
			errorStorage.setType(IMessageBrowser.StorageType.ERRORSTORAGE.getCode());
		}
	}
	
	/**
	 * Sets the messageLog.
	 */
	protected void setMessageLog(ITransactionalStorage<Serializable> messageLog) {
		if (messageLog.isActive()) {
			this.messageLog = messageLog;
			messageLog.setName("messageLog of ["+getName()+"]");
			if (StringUtils.isEmpty(messageLog.getSlotId())) {
				messageLog.setSlotId(getName());
			}
			messageLog.setType(IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode());
		}
	}

	/**
	 * returns a browser for the messageLog, either provided as a {@link IMessageBrowser} by the listener itself, or as a {@link ITransactionalStorage messageLog} in the configuration. 
	 */
	public IMessageBrowser<Serializable> getMessageLogBrowser() {
		return messageLog;
	}
	/**
	 * returns the {@link ITransactionalStorage} if it is provided in the configuration. It is used to store messages that have been processed successfully. If present, this storage will be managed by the Receiver.
	 */
	public ITransactionalStorage<Serializable> getMessageLog() {
		return messageLog!=null && messageLog instanceof ITransactionalStorage ? (ITransactionalStorage)messageLog: null;
	}
	/**
	 * returns a browser of messages inProcess, if provided as a {@link IMessageBrowser} by the listener itself. 
	 */
	public IMessageBrowser<Serializable> getInProcessBrowser() {
		return inProcessBrowser;
	}


	/**
	 * Get the number of messages received.
	  * @return long
	 */
	@Override
	public long getMessagesReceived() {
		return numReceived.getValue();
	}

	/**
	 * Get the number of messages retried.
	  * @return long
	 */
	@Override
	public long getMessagesRetried() {
		return numRetried.getValue();
	}

	/**
	 * Get the number of messages rejected (discarded or put in errorstorage).
	  * @return long
	 */
	@Override
	public long getMessagesRejected() {
		return numRejected.getValue();
	}

	public long getLastMessageDate() {
		return lastMessageDate;
	}
	
//	public StatisticsKeeper getRequestSizeStatistics() {
//		return requestSizeStatistics;
//	}
//	public StatisticsKeeper getResponseSizeStatistics() {
//		return responseSizeStatistics;
//	}


	/**
	 * Sets the name of the Receiver. 
	 * If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 */
	@IbisDoc({"name of the receiver as known to the adapter", ""})
	@Override
	public void setName(String newName) {
		name = newName;
		propagateName();
	}
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * Controls the use of XA-transactions.
	 */
	@IbisDoc({"if set to <code>true</code>, messages will be received and processed under transaction control. if processing fails, messages will be sent to the error-sender. (see below)", "<code>false</code>"})
	public void setTransacted(boolean transacted) {
//		this.transacted = transacted;
		if (transacted) {
			ConfigurationWarnings.add(this, log, "implementing setting of transacted=true as transactionAttribute=Required");
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_REQUIRED);
		} else {
			ConfigurationWarnings.add(this, log, "implementing setting of transacted=false as transactionAttribute=Supports");
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_SUPPORTS);
		}
	}
	public boolean isTransacted() {
//		return transacted;
		int txAtt = getTransactionAttributeNum();
		return  txAtt==TransactionDefinition.PROPAGATION_REQUIRED || 
				txAtt==TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				txAtt==TransactionDefinition.PROPAGATION_MANDATORY;
	}

	@IbisDoc({"The transactionAttribute declares transactional behavior of the receiver. "
			+ "It applies both to database transactions and XA transactions. "
			+ "The receiver uses this to start a new transaction or suspend the current one when required. "
			+ "For developers: it is equal "
			+ "to <a href=\"http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494\">EJB transaction attribute</a>. "
			+ "Possible values for transactionAttribute: "
			+ "  <table border=\"1\">"
			+ "	<tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>"
			+ "	<tr><td colspan=\"1\" rowspan=\"2\">Required</td> <td>none</td><td>T2</td></tr>"
			+ "												  <tr><td>T1</td>  <td>T1</td></tr>"
			+ "	<tr><td colspan=\"1\" rowspan=\"2\">RequiresNew</td> <td>none</td><td>T2</td></tr>"
			+ "												  <tr><td>T1</td>  <td>T2</td></tr>"
			+ "	<tr><td colspan=\"1\" rowspan=\"2\">Mandatory</td>   <td>none</td><td>error</td></tr>"
			+ "												  <tr><td>T1</td>  <td>T1</td></tr>"
			+ "	<tr><td colspan=\"1\" rowspan=\"2\">NotSupported</td><td>none</td><td>none</td></tr>"
			+ "												  <tr><td>T1</td>  <td>none</td></tr>"
			+ "	<tr><td colspan=\"1\" rowspan=\"2\">Supports</td>	<td>none</td><td>none</td></tr>"
			+ " 											  <tr><td>T1</td>  <td>T1</td></tr>"
			+ "	<tr><td colspan=\"1\" rowspan=\"2\">Never</td>	   <td>none</td><td>none</td></tr>"
			+ "												  <tr><td>T1</td>  <td>error</td></tr>"
			+ "  </table>", "Supports"})
	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			throw new ConfigurationException("illegal value for transactionAttribute ["+attribute+"]");
		}
	}
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}
	
	@IbisDoc({"Like <code>transactionAttribute</code>, but the chosen "
			+ "option is represented with a number. The numbers mean:"
			+ "<table>"
			+ "<tr><td>0</td><td>Required</td></tr>"
			+ "<tr><td>1</td><td>Supports</td></tr>"
			+ "<tr><td>2</td><td>Mandatory</td></tr>"
			+ "<tr><td>3</td><td>RequiresNew</td></tr>"
			+ "<tr><td>4</td><td>NotSupported</td></tr>"
			+ "<tr><td>5</td><td>Never</td></tr>"
			+ "</table>", "1"})
	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	@IbisDoc({"one of 'continue' or 'close'. controls the behaviour of the receiver when it encounters an error sending a reply or receives an exception asynchronously", "continue"})
	public void setOnError(String newOnError) {
		onError = newOnError;
	}
	public String getOnError() {
		return onError;
	}

	public boolean isOnErrorContinue() {
		return ONERROR_CONTINUE.equalsIgnoreCase(getOnError());
	}
	@Override
	public IAdapter getAdapter() {
		return adapter;
	}


	/**
	 *  Returns a toString of this class by introspection and the toString() value of its listener.
	 *
	 * @return Description of the Return Value
	 */
	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts=new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName() );
		result += ts.toString();
		result+=" listener ["+(listener==null ? "-none-" : listener.toString())+"]";
		return result;
	}

	
	/**
	 * The number of threads that this receiver is configured to work with.
	 */
	@IbisDoc({"the number of threads that may execute a pipeline concurrently (only for pulling listeners)", "1"})
	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}
	public int getNumThreads() {
		return numThreads;
	}

	@Override
	public String formatException(String extrainfo, String correlationId, Message message, Throwable t) {
		return getAdapter().formatErrorMessage(extrainfo,t,message,correlationId,null,0);
	}


	public int getNumThreadsPolling() {
		return numThreadsPolling;
	}

	@IbisDoc({"the number of threads that are activily polling for messages concurrently. '0' means 'limited only by <code>numthreads</code>' (only for pulling listeners)", "1"})
	public void setNumThreadsPolling(int i) {
		numThreadsPolling = i;
	}

	public int getMaxDeliveries() {
		return maxDeliveries;
	}

	@IbisDoc({"the maximum delivery count after which to stop processing the message. when -1 the delivery count is ignored", "5"})
	public void setMaxDeliveries(int i) {
		maxDeliveries = i;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	@IbisDoc({"the number of times a processing attempt is retried after an exception is caught or rollback is experienced (only applicable for transacted receivers). if maxretries &lt; 0 the number of attempts is infinite", "1"})
	public void setMaxRetries(int i) {
		maxRetries = i;
	}
	
	@IbisDoc({"when set <code>false</code> or set to something else as <code>true</code>, (even set to the empty string), the receiver is not included in the configuration", "true"})
	public void setActive(boolean b) {
		active = b;
	}
	public boolean isActive() {
		return active;
	}

	@IbisDoc({"comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erronous results. (only for listeners that support it, like javalistener)", ""})
	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}
	public String getReturnedSessionKeys() {
		return returnedSessionKeys;
	}

	@IbisDoc({"comma separated list of keys of session variables which are available when the <code>pipelinesession</code> is created and of which the value will not be shown in the log (replaced by asterisks)", ""})
	public void setHiddenInputSessionKeys(String string) {
		hiddenInputSessionKeys = string;
	}
	public String getHiddenInputSessionKeys() {
		return hiddenInputSessionKeys;
	}



	public void setTxManager(PlatformTransactionManager manager) {
		txManager = manager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}


	private String sendResultToSender(String correlationId, Message result) {
		String errorMessage = null;
		try {
			if (getSender() != null) {
				if (log.isDebugEnabled()) { log.debug("Receiver ["+getName()+"] sending result to configured sender"); }
				getSender().sendMessage(result, null);
			}
		} catch (Exception e) {
			String msg = "caught exception in message post processing";
			error(msg, e);
			errorMessage = msg + ": " + e.getMessage();
			if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
				log.info("closing after exception in post processing");
				stopRunning();
			}
		}
		return errorMessage;
	}

	private ListenerException wrapExceptionAsListenerException(Throwable t) {
		ListenerException l;
		if (t instanceof ListenerException) {
			l = (ListenerException) t;
		} else {
			l = new ListenerException(t);
		}
		return l;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public PullingListenerContainer<M> getListenerContainer() {
		return listenerContainer;
	}

	public void setListenerContainer(PullingListenerContainer<M> listenerContainer) {
		this.listenerContainer = listenerContainer;
	}

	public PullingListenerContainer<M> createListenerContainer() {
		@SuppressWarnings("unchecked")
		PullingListenerContainer<M> plc = (PullingListenerContainer<M>) beanFactory.getBean("listenerContainer");
		plc.setReceiver(this);
		plc.configure();
		return plc;
	}

	@IbisDoc({"The number of seconds waited after an unsuccesful poll attempt before another poll attempt is made. Only for polling listeners, not for e.g. ifsa, jms, webservice or javaListeners", "10"})
	public void setPollInterval(int i) {
		pollInterval = i;
	}
	public int getPollInterval() {
		return pollInterval;
	}

	@IbisDoc({"If set to <code>true</code>, each message is checked for presence in the message log. If already present, it is not processed again. Only required for non XA compatible messaging. Requires messagelog!", "<code>false</code>"})
	public void setCheckForDuplicates(boolean b) {
		checkForDuplicates = b;
	}
	public boolean isCheckForDuplicates() {
		return checkForDuplicates;
	}

	@IbisDoc({"(Only used when <code>checkForDuplicates=true</code>) Either 'CORRELATIONID' or 'MESSAGEID'. Indicates whether the messageid or the correlationid is used for checking presence in the message log", "MESSAGEID"})
	public void setCheckForDuplicatesMethod(String method) {
		checkForDuplicatesMethod=method;
	}
	public String getCheckForDuplicatesMethod() {
		return checkForDuplicatesMethod;
	}

	@IbisDoc({"Timeout (in seconds) of transaction started to receive and process a message.", "<code>0</code> (use system default)"})
	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
	public int getTransactionTimeout() {
		return transactionTimeout;
	}

	@IbisDoc({"XPath expression to extract correlationid from message", ""})
	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}
	public String getCorrelationIDXPath() {
		return correlationIDXPath;
	}

	public String getCorrelationIDNamespaceDefs() {
		return correlationIDNamespaceDefs;
	}

	@IbisDoc({"Namespace defintions for correlationIDXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setCorrelationIDNamespaceDefs(String correlationIDNamespaceDefs) {
		this.correlationIDNamespaceDefs = correlationIDNamespaceDefs;
	}

	@IbisDoc({"Stylesheet to extract correlationID from message", ""})
	public void setCorrelationIDStyleSheet(String string) {
		correlationIDStyleSheet = string;
	}
	public String getCorrelationIDStyleSheet() {
		return correlationIDStyleSheet;
	}

	@IbisDoc({"XPath expression to extract label from message", ""})
	public void setLabelXPath(String string) {
		labelXPath = string;
	}
	public String getLabelXPath() {
		return labelXPath;
	}

	@IbisDoc({"Namespace defintions for labelXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setLabelNamespaceDefs(String labelNamespaceDefs) {
		this.labelNamespaceDefs = labelNamespaceDefs;
	}
	public String getLabelNamespaceDefs() {
		return labelNamespaceDefs;
	}
	
	@IbisDoc({"Stylesheet to extract label from message", ""})
	public void setLabelStyleSheet(String string) {
		labelStyleSheet = string;
	}
	public String getLabelStyleSheet() {
		return labelStyleSheet;
	}

	@IbisDoc({"If set (>=0) and the character data length inside a xml element exceeds this size, the character data is chomped (with a clear comment)", ""})
	public void setChompCharSize(String string) {
		chompCharSize = string;
	}
	public String getChompCharSize() {
		return chompCharSize;
	}

	@IbisDoc({"If set, the character data in this element is stored under a session key and in the message replaced by a reference to this session key: {sessionkey: + <code>elementtomovesessionkey</code> + }", ""})
	public void setElementToMove(String string) {
		elementToMove = string;
	}
	public String getElementToMove() {
		return elementToMove;
	}

	@IbisDoc({"(Only used when <code>elementtomove</code> is set) Name of the session key under which the character data is stored", "ref_ + the name of the element"})
	public void setElementToMoveSessionKey(String string) {
		elementToMoveSessionKey = string;
	}
	public String getElementToMoveSessionKey() {
		return elementToMoveSessionKey;
	}

	@IbisDoc({"Like <code>elementtomove</code> but element is preceded with all ancestor elements and separated by semicolons (e.g. adapter;pipeline;pipe)", ""})
	public void setElementToMoveChain(String string) {
		elementToMoveChain = string;
	}
	public String getElementToMoveChain() {
		return elementToMoveChain;
	}

	public void setRemoveCompactMsgNamespaces(boolean b) {
		removeCompactMsgNamespaces = b;
	}
	public boolean isRemoveCompactMsgNamespaces() {
		return removeCompactMsgNamespaces;
	}

	public void setPoisonMessageIdCacheSize(int poisonMessageIdCacheSize) {
		this.poisonMessageIdCacheSize = poisonMessageIdCacheSize;
	}
	public int getPoisonMessageIdCacheSize() {
		return poisonMessageIdCacheSize;
	}

	public int getProcessResultCacheSize() {
		return processResultCacheSize;
	}
	public void setProcessResultCacheSize(int processResultCacheSize) {
		this.processResultCacheSize = processResultCacheSize;
	}
	

	public void setRecover(boolean b) {
		recover = b;
	}
	public boolean isRecover() {
		return recover;
	}

	public boolean isRecoverAdapter() {
		IAdapter adapter = getAdapter();
		if (adapter instanceof Adapter) {
			Adapter at = (Adapter) adapter;
			return at.isRecover();
		}
		return false;
	}

	@IbisDoc({"Regular expression to mask strings in the errorStore/logStore. Every character between to the strings in this expression will be replaced by a '*'. For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every character between keys<party> and </party> ", ""})
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}
	public String getHideRegex() {
		return hideRegex;
	}

	@IbisDoc({"(Only used when hideRegex is not empty) either <code>all</code> or <code>firstHalf</code>. When <code>firstHalf</code> only the first half of the string is masked, otherwise (<code>all</code>) the entire string is masked", "all"})
	public void setHideMethod(String hideMethod) {
		this.hideMethod = hideMethod;
	}
	public String getHideMethod() {
		return hideMethod;
	}

}
