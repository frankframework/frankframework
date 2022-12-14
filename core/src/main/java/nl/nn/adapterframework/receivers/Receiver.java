/*
   Copyright 2013, 2015, 2016, 2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IBulkDataListener;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IHasProcessState;
import nl.nn.adapterframework.core.IKnowsDeliveryCount;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IManagable;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowser.HideMethod;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionRequirements;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.core.TransactionAttributes;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.Protected;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.monitoring.EventPublisher;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.statistics.CounterStatistic;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CompactSaxHandler;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Wrapper for a listener that specifies a channel for the incoming messages of a specific {@link Adapter}.
 * By choosing a listener, the Frank developer determines how the messages are received.
 * For example, an {@link nl.nn.adapterframework.http.rest.ApiListener} receives RESTful HTTP requests and a
 * {@link JavaListener} receives messages from direct Java calls.
 * <br/><br/>
 * Apart from wrapping the listener, a {@link Receiver} can be configured
 * to store received messages and to keep track of the processed / failed
 * status of these messages.
 * <br/><br/>
 * There are two kinds of listeners: synchronous listeners and asynchronous listeners.
 * Synchronous listeners are expected to return a response. The system that triggers the
 * receiver typically waits for a response before proceeding its operation. When a
 * {@link nl.nn.adapterframework.http.rest.ApiListener} receives a HTTP request, the listener is expected to return a
 * HTTP response. Asynchronous listeners are not expected to return a response. The system that
 * triggers the listener typically continues without waiting for the adapter to finish. When a
 * receiver contains an asynchronous listener, it can have a sender that sends the transformed
 * message to its destination. Receivers with an asynchronous listener can also have an error sender that is used
 * by the receiver to send error messages. In other words: if the result state is SUCCESS then the
 * message is sent by the ordinary sender, while the error sender is used if the result state
 * is ERROR.
 * <br/><br/>
 * <b>Transaction control</b><br/><br/>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be received and processed under transaction control.
 * This means that after a message has been read and processed and the transaction has ended, one of the following apply:
 * <table border="1">
 * <tr><th>situation</th><th>input listener</th><th>Pipeline</th><th>inProcess storage</th><th>errorSender</th><th>summary of effect</th></tr>
 * <tr><td>successful</td><td>message read and committed</td><td>message processed</td><td>unchanged</td><td>unchanged</td><td>message processed</td></tr>
 * <tr><td>procesing failed</td><td>message read and committed</td><td>message processing failed and rolled back</td><td>unchanged</td><td>message sent</td><td>message only transferred from listener to errroSender</td></tr>
 * <tr><td>listening failed</td><td>unchanged: listening rolled back</td><td>no processing performed</td><td>unchanged</td><td>unchanged</td><td>no changes, input message remains on input available for listener</td></tr>
 * <tr><td>transfer to inprocess storage failed</td><td>unchanged: listening rolled back</td><td>no processing performed</td><td>unchanged</td><td>unchanged</td><td>no changes, input message remains on input available for listener</td></tr>
 * <tr><td>transfer to errorSender failed</td><td>message read and committed</td><td>message processing failed and rolled back</td><td>message present</td><td>unchanged</td><td>message only transferred from listener to inProcess storage</td></tr>
 * </table>
 * If the application or the server crashes in the middle of one or more transactions, these transactions
 * will be recovered and rolled back after the server/application is restarted. Then always exactly one of
 * the following applies for any message touched at any time by Ibis by a transacted receiver:
 * <ul>
 * <li>It is processed correctly by the pipeline and removed from the input-queue,
 *     not present in inProcess storage and not send to the errorSender</li>
 * <li>It is not processed at all by the pipeline, or processing by the pipeline has been rolled back;
 *     the message is removed from the input queue and either (one of) still in inProcess storage <i>or</i> sent to the errorSender</li>
 * </ul>
 *
 * <p><b>commit or rollback</b><br/>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be either committed or rolled back.
 * All message-processing transactions are committed, unless one or more of the following apply:
 * <ul>
 * <li>The PipeLine is transacted and the exitState of the pipeline is not equal to SUCCESS</li>
 * <li>a PipeRunException or another runtime-exception has been thrown by any Pipe or by the PipeLine</li>
 * <li>the setRollBackOnly() method has been called on the userTransaction (not accessible by Pipes)</li>
 * </ul>
 * </p>
 *
 * @author Gerrit van Brakel
 * @since 4.2
 *
 */
/*
 * The receiver is the trigger and central communicator for the framework.
 * <br/>
 * The main responsibilities are:
 * <ul>
 *    <li>receiving messages</li>
 *    <li>for asynchronous receivers (which have a separate sender):<br/>
 *            <ul><li>initializing ISender objects</li>
 *                <li>stopping ISender objects</li>
 *                <li>sending the message with the ISender object</li>
 *            </ul>
 *    <li>synchronous receivers give the result directly</li>
 *    <li>take care of connection, sessions etc. to startup and shutdown</li>
 * </ul>
 * Listeners call the IAdapter.processMessage(String correlationID,String message)
 * to do the actual work, which returns a <code>{@link PipeLineResult}</code>. The receiver
 * may observe the status in the <code>{@link PipeLineResult}</code> to perform committing
 * requests.
 *
 */
@Category("Basic")
public class Receiver<M> extends TransactionAttributes implements IManagable, IReceiverStatistics, IMessageHandler<M>, IProvidesMessageBrowsers<Object>, EventThrowing, IbisExceptionListener, HasSender, HasStatistics, IThreadCountControllable, BeanFactoryAware {
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	public static final TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
	public static final TransactionDefinition TXNEW_CTRL = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	public TransactionDefinition TXNEW_PROC;

	public static final String THREAD_CONTEXT_KEY_NAME = "listener";
	public static final String THREAD_CONTEXT_KEY_TYPE = "listener.type";

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
	public static final String RETRY_FLAG_SESSION_KEY="retry"; // a session variable with this key will be set "true" if the message is manually retried, is redelivered, or it's messageid has been seen before

	public enum OnError {
		/** Don't stop the receiver when an error occurs.*/
		CONTINUE,

		/**
		 * If an error occurs (eg. connection is lost) the receiver will be stopped and marked as ERROR
		 * Once every <code>recover.adapters.interval</code> it will be attempted to (re-) start the receiver.
		 */
		RECOVER,

		/** Stop the receiver when an error occurs. */
		CLOSE
	};

	/** Currently, this feature is only implemented for {@link IPushingListener}s, like Tibco and SAP. */
	private @Getter OnError onError = OnError.CONTINUE;

	private @Getter String name;

	// the number of threads that may execute a pipeline concurrently (only for pulling listeners)
	private @Getter int numThreads = 1;
	// the number of threads that are actively polling for messages (concurrently, only for pulling listeners)
	private @Getter int numThreadsPolling = 1;
	private @Getter int pollInterval=10;
	private @Getter int startTimeout=60;
	private @Getter int stopTimeout=60;

	private @Getter boolean forceRetryFlag = false;
	private @Getter boolean checkForDuplicates=false;
	public enum CheckForDuplicatesMethod { MESSAGEID, CORRELATIONID };
	private @Getter CheckForDuplicatesMethod checkForDuplicatesMethod=CheckForDuplicatesMethod.MESSAGEID;
	private @Getter int maxDeliveries=5;
	private @Getter int maxRetries=1;
	private @Getter int processResultCacheSize = 100;
	private @Getter boolean supportProgrammaticRetry=false;

	private @Getter String correlationIDXPath;
	private @Getter String correlationIDNamespaceDefs;
	private @Getter String correlationIDStyleSheet;

	private @Getter String labelXPath;
	private @Getter String labelNamespaceDefs;
	private @Getter String labelStyleSheet;

	private @Getter String chompCharSize = null;
	private @Getter String elementToMove = null;
	private @Getter String elementToMoveSessionKey = null;
	private @Getter String elementToMoveChain = null;
	private @Getter boolean removeCompactMsgNamespaces = true;

	private @Getter String hideRegex = null;
	private @Getter HideMethod hideMethod = HideMethod.ALL;
	private @Getter String hiddenInputSessionKeys=null;

	private Counter numberOfExceptionsCaughtWithoutMessageBeingReceived = new Counter(0);
	private int numberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold = 5;
	private @Getter boolean numberOfExceptionsCaughtWithoutMessageBeingReceivedThresholdReached=false;

	private int retryInterval=1;

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
	private @Getter @Setter Adapter adapter;

	private @Getter IListener<M> listener;
	private @Getter ISender errorSender=null;
	// See configure() for explanation on this field
	private ITransactionalStorage<Serializable> tmpInProcessStorage=null;
	private @Getter ITransactionalStorage<Serializable> messageLog=null;
	private @Getter ITransactionalStorage<Serializable> errorStorage=null;
	private @Getter ISender sender=null; // reply-sender
	private Map<ProcessState,IMessageBrowser<?>> messageBrowsers = new HashMap<>();

	private TransformerPool correlationIDTp=null;
	private TransformerPool labelTp=null;


	private @Getter @Setter PlatformTransactionManager txManager;

	private @Setter EventPublisher eventPublisher;

	private Set<ProcessState> knownProcessStates = new LinkedHashSet<ProcessState>();
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new HashMap<>();


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
		private int receiveCount;
		private Date receiveDate;
		private String comments;
	}

	public boolean configurationSucceeded() {
		return configurationSucceeded;
	}

	private void showProcessingContext(String messageId, String correlationId, PipeLineSession session) {
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
			for (Iterator<String> it = session.keySet().iterator(); it.hasNext();) {
				String key = it.next();
				Object value = session.get(key);
				if (key.equals("messageText")) {
					value = "(... see elsewhere ...)";
				}
				String strValue = String.valueOf(value);
				contextDump += " " + key + "=[" + (hiddenSessionKeys.contains(key)?hide(strValue):strValue) + "]";
			}
			log.debug(getLogPrefix()+contextDump);
		}
	}

	private String hide(String string) {
		String hiddenString = "";
		for (int i = 0; i < string.length(); i++) {
			hiddenString = hiddenString + "*";
		}
		return hiddenString;
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


	protected void openAllResources() throws ListenerException, TimeoutException {
		// on exit resouces must be in a state that runstate is or can be set to 'STARTED'
		TimeoutGuard timeoutGuard = new TimeoutGuard(getStartTimeout(), "starting receiver ["+getName()+"]");
		try {
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
		} finally {
			if (timeoutGuard.cancel()) {
				throw new TimeoutException("timeout exceeded while starting receiver");
			}
		}
		if (getListener() instanceof IPullingListener){
			// start all threads. Also sets runstate=STARTED
			listenerContainer.start();
		}
		throwEvent(RCV_STARTED_RUNNING_MONITOR_EVENT);
	}

	/**
	 * must lead to a 'closeAllResources()' and runstate must be 'STOPPING'
	 * if IPushingListener -> call closeAllResources()
	 * if IPullingListener -> PullingListenerContainer has to call closeAllResources();
	 */
	protected void tellResourcesToStop() {
		if (getListener() instanceof IPushingListener) {
			closeAllResources();
		}
		// IPullingListeners stop as their threads finish, as the runstate is set to stopping
		// See PullingListenerContainer that calls receiver.isInRunState(RunStateEnum.STARTED)
		// and receiver.closeAllResources()
	}

	/**
	 * Should only close resources when in state stopping (or error)! this should be the only trigger to change the state to stopped
	 * On exit resources must be 'closed' so the receiver RunState can be set to 'STOPPED'
	 */
	protected void closeAllResources() {
		TimeoutGuard timeoutGuard = new TimeoutGuard(getStopTimeout(), "stopping receiver ["+getName()+"]");
		log.debug(getLogPrefix()+"closing");
		try {
			try {
				getListener().close();
			} catch (Exception e) {
				error("error closing listener", e);
			}
			if (getSender()!=null) {
				try {
					getSender().close();
				} catch (Exception e) {
					error("error closing sender", e);
				}
			}
			if (getErrorSender()!=null) {
				try {
					getErrorSender().close();
				} catch (Exception e) {
					error("error closing error sender", e);
				}
			}
			if (getErrorStorage()!=null) {
				try {
					getErrorStorage().close();
				} catch (Exception e) {
					error("error closing error storage", e);
				}
			}
			if (getMessageLog()!=null) {
				try {
					getMessageLog().close();
				} catch (Exception e) {
					error("error closing message log", e);
				}
			}
		} finally {
			if (timeoutGuard.cancel()) {
				if(!isInRunState(RunState.EXCEPTION_STARTING)) { //Don't change the RunState when failed to start
					runState.setRunState(RunState.EXCEPTION_STOPPING);
				}
				log.warn(getLogPrefix()+"timeout stopping");
			} else {
				log.debug(getLogPrefix()+"closed");
				if (isInRunState(RunState.STOPPING) || isInRunState(RunState.EXCEPTION_STOPPING)) {
					runState.setRunState(RunState.STOPPED);
				}
				if(!isInRunState(RunState.EXCEPTION_STARTING)) { //Don't change the RunState when failed to start
					throwEvent(RCV_SHUTDOWN_MONITOR_EVENT);
					resetRetryInterval();

					info("stopped");
				}
			}
		}
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

	/**
	 * This method is called by the <code>IAdapter</code> to let the
	 * receiver do things to initialize itself before the <code>startListening</code>
	 * method is called.
	 * @see #startRunning
	 * @throws ConfigurationException when initialization did not succeed.
	 */
	@Override
	public void configure() throws ConfigurationException {
		configurationSucceeded = false;
		try {
			super.configure();
			if (StringUtils.isEmpty(getName())) {
				if (getListener()!=null) {
					setName(ClassUtils.nameOf(getListener()));
				} else {
					setName(ClassUtils.nameOf(this));
				}
			}
			if(getName().contains("/")) {
				throw new ConfigurationException("It is not allowed to have '/' in receiver name ["+getName()+"]");
			}

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
				if (this.errorSender == null && messageBrowsers.get(ProcessState.ERROR) == null) {
					messageBrowsers.put(ProcessState.ERROR, this.tmpInProcessStorage);
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
					ConfigurationWarnings.add(this, log, "listener type ["+ClassUtils.nameOf(getListener())+"] requires transactional processing", SuppressKeys.TRANSACTION_SUPPRESS_KEY, getAdapter());
					//throw new ConfigurationException(msg);
				}
			}
			ISender sender = getSender();
			if (sender!=null) {
				sender.configure();
				if (sender instanceof HasPhysicalDestination) {
					info("has answer-sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
			}

			ISender errorSender = getErrorSender();
			if (errorSender!=null) {
				if (errorSender instanceof HasPhysicalDestination) {
					info("has errorSender to "+((HasPhysicalDestination)errorSender).getPhysicalDestinationName());
				}
				errorSender.configure();
			}

			if (getListener() instanceof IHasProcessState) {
				knownProcessStates.addAll(((IHasProcessState)getListener()).knownProcessStates());
				targetProcessStates = ((IHasProcessState)getListener()).targetProcessStates();
				supportProgrammaticRetry = knownProcessStates.contains(ProcessState.INPROCESS);
			}


			ITransactionalStorage<Serializable> messageLog = getMessageLog();
			if (messageLog!=null) {
				if (getListener() instanceof IProvidesMessageBrowsers && ((IProvidesMessageBrowsers)getListener()).getMessageBrowser(ProcessState.DONE)!=null) {
					throw new ConfigurationException("listener with built-in messageLog cannot have external messageLog too");
				}
				messageLog.setName("messageLog of ["+getName()+"]");
				if (StringUtils.isEmpty(messageLog.getSlotId())) {
					messageLog.setSlotId(getName());
				}
				messageLog.setType(IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode());
				messageLog.configure();
				if (messageLog instanceof HasPhysicalDestination) {
					info("has messageLog in "+((HasPhysicalDestination)messageLog).getPhysicalDestinationName());
				}
				knownProcessStates.add(ProcessState.DONE);
				messageBrowsers.put(ProcessState.DONE, messageLog);
				if (StringUtils.isNotEmpty(getLabelXPath()) || StringUtils.isNotEmpty(getLabelStyleSheet())) {
					labelTp=TransformerPool.configureTransformer0(this, getLabelNamespaceDefs(), getLabelXPath(), getLabelStyleSheet(), OutputType.TEXT,false,null,0);
				}
			}
			ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
			if (errorStorage!=null) {
				if (getListener() instanceof IProvidesMessageBrowsers && ((IProvidesMessageBrowsers)getListener()).getMessageBrowser(ProcessState.ERROR)!=null) {
					throw new ConfigurationException("listener with built-in errorStorage cannot have external errorStorage too");
				}
				errorStorage.setName("errorStorage of ["+getName()+"]");
				if (StringUtils.isEmpty(errorStorage.getSlotId())) {
					errorStorage.setSlotId(getName());
				}
				errorStorage.setType(IMessageBrowser.StorageType.ERRORSTORAGE.getCode());
				errorStorage.configure();
				if (errorStorage instanceof HasPhysicalDestination) {
					info("has errorStorage to "+((HasPhysicalDestination)errorStorage).getPhysicalDestinationName());
				}
				knownProcessStates.add(ProcessState.ERROR);
				messageBrowsers.put(ProcessState.ERROR, errorStorage);
				registerEvent(RCV_MESSAGE_TO_ERRORSTORE_EVENT);
			}
			if (getListener() instanceof IProvidesMessageBrowsers) {
				for (ProcessState state: knownProcessStates) {
					IMessageBrowser messageBrowser = ((IProvidesMessageBrowsers)getListener()).getMessageBrowser(state);
					if (messageBrowser!=null && messageBrowser instanceof IConfigurable) {
						((IConfigurable)messageBrowser).configure();
					}
					messageBrowsers.put(state, messageBrowser);
				}
			}
			if (targetProcessStates==null) {
				targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates);
			}

			if (isTransacted() && errorSender==null && errorStorage==null && !knownProcessStates().contains(ProcessState.ERROR)) {
				ConfigurationWarnings.add(this, log, "sets transactionAttribute=" + getTransactionAttribute() + ", but has no errorSender or errorStorage. Messages processed with errors will be lost", SuppressKeys.TRANSACTION_SUPPRESS_KEY, getAdapter());
			}

			if (StringUtils.isNotEmpty(getCorrelationIDXPath()) || StringUtils.isNotEmpty(getCorrelationIDStyleSheet())) {
				correlationIDTp=TransformerPool.configureTransformer0(this, getCorrelationIDNamespaceDefs(), getCorrelationIDXPath(), getCorrelationIDStyleSheet(), OutputType.TEXT,false,null,0);
			}

			if (StringUtils.isNotEmpty(getHideRegex()) && getErrorStorage()!=null && StringUtils.isEmpty(getErrorStorage().getHideRegex())) {
				getErrorStorage().setHideRegex(getHideRegex());
				getErrorStorage().setHideMethod(getHideMethod());
			}
			if (StringUtils.isNotEmpty(getHideRegex()) && getMessageLog()!=null && StringUtils.isEmpty(getMessageLog().getHideRegex())) {
				getMessageLog().setHideRegex(getHideRegex());
				getMessageLog().setHideMethod(getHideMethod());
			}
		} catch (Throwable t) {
			ConfigurationException e = null;
			if (t instanceof ConfigurationException) {
				e = (ConfigurationException)t;
			} else {
				e = new ConfigurationException("Exception configuring receiver ["+getName()+"]",t);
			}
			throwEvent(RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
			log.debug(getLogPrefix()+"Errors occured during configuration, setting runstate to ERROR");
			runState.setRunState(RunState.ERROR);
			throw e;
		}

		if (adapter != null) {
			adapter.getMessageKeeper().add(getLogPrefix()+"initialization complete");
		}
		throwEvent(RCV_CONFIGURED_MONITOR_EVENT);
		configurationSucceeded = true;

		if(isInRunState(RunState.ERROR)) { // if the adapter was previously in state ERROR, after a successful configure, reset it's state
			runState.setRunState(RunState.STOPPED);
		}
	}


	@Override
	public void startRunning() {
		try {
			// if this receiver is on an adapter, the StartListening method
			// may only be executed when the adapter is started.
			if (adapter != null) {
				RunState adapterRunState = adapter.getRunState();
				if (adapterRunState!=RunState.STARTED) {
					log.warn(getLogPrefix()+"on adapter [" + adapter.getName() + "] was tried to start, but the adapter is in state ["+adapterRunState+"]. Ignoring command.");
					adapter.getMessageKeeper().add("ignored start command on [" + getName()  + "]; adapter is in state ["+adapterRunState+"]");
					return;
				}
			}
			// See also Adapter.startRunning()
			if (!configurationSucceeded) {
				log.error("configuration of receiver [" + getName() + "] did not succeed, therefore starting the receiver is not possible");
				warn("configuration did not succeed. Starting the receiver ["+getName()+"] is not possible");
				runState.setRunState(RunState.ERROR);
				return;
			}
			if (adapter.getConfiguration().isUnloadInProgressOrDone()) {
				log.error( "configuration of receiver [" + getName() + "] unload in progress or done, therefore starting the receiver is not possible");
				warn("configuration unload in progress or done. Starting the receiver ["+getName()+"] is not possible");
				return;
			}
			synchronized (runState) {
				RunState currentRunState = getRunState();
				if (currentRunState!=RunState.STOPPED
						&& currentRunState!=RunState.EXCEPTION_STOPPING
						&& currentRunState!=RunState.EXCEPTION_STARTING
						&& currentRunState!=RunState.ERROR
						&& configurationSucceeded()) { // Only start the receiver if it is properly configured, and is not already starting or still stopping
					if (currentRunState==RunState.STARTING || currentRunState==RunState.STARTED) {
						log.info("already in state [" + currentRunState + "]");
					} else {
						log.warn("currently in state [" + currentRunState + "], ignoring start() command");
					}
					return;
				}
				runState.setRunState(RunState.STARTING);
			}

			openAllResources();

			info("starts listening"); // Don't log that it's ready before it's ready!?
			runState.setRunState(RunState.STARTED);
			resetNumberOfExceptionsCaughtWithoutMessageBeingReceived();
		} catch (Throwable t) {
			error("error occured while starting", t);

			runState.setRunState(RunState.EXCEPTION_STARTING);
			closeAllResources(); //Close potential dangling resources, don't change state here..
		}
	}

	//after successfully closing all resources the state should be set to stopped
	@Override
	public void stopRunning() {
		// See also Adapter.stopRunning() and PullingListenerContainer.ControllerTask
		synchronized (runState) {
			RunState currentRunState = getRunState();
			if (currentRunState==RunState.STARTING) {
				log.warn("receiver currently in state [" + currentRunState + "], ignoring stop() command");
				return;
			} else if (currentRunState==RunState.STOPPING || currentRunState==RunState.STOPPED) {
				log.info("receiver already in state [" + currentRunState + "]");
				return;
			}

			if(currentRunState == RunState.EXCEPTION_STARTING && getListener() instanceof IPullingListener) {
				runState.setRunState(RunState.STOPPING); //Nothing ever started, directly go to stopped
				closeAllResources();
				ThreadContext.clearAll(); //Clean up receiver ThreadContext
				return; //Prevent tellResourcesToStop from being called
			}

			if (currentRunState!=RunState.ERROR) {
				runState.setRunState(RunState.STOPPING); //Don't change the runstate when in ERROR
			}
		}

		tellResourcesToStop();
		ThreadContext.clearAll(); //Clean up receiver ThreadContext
	}

	@Override
	public Set<ProcessState> knownProcessStates() {
		return knownProcessStates;
	}

	@Override
	public Map<ProcessState,Set<ProcessState>> targetProcessStates() {
		return targetProcessStates;
	}

	@Override
	public M changeProcessState(Object message, ProcessState toState, String reason) throws ListenerException {
		if (toState==ProcessState.AVAILABLE) {
			String id = getListener().getIdFromRawMessage((M)message, null);
			resetProblematicHistory(id);
		}
		return ((IHasProcessState<M>)getListener()).changeProcessState((M)message, toState, reason); // Cast is safe because changeProcessState will only be executed in internal MessageBrowser
	}

	@Override
	public IMessageBrowser<Object> getMessageBrowser(ProcessState state) {
		return (IMessageBrowser<Object>)messageBrowsers.get(state);
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

	private void moveInProcessToErrorAndDoPostProcessing(IListener<M> origin, String messageId, String correlationId, M rawMessage, ThrowingSupplier<Message,ListenerException> messageSupplier, Map<String,Object> threadContext, ProcessResultCacheItem prci, String comments) throws ListenerException {
		Date rcvDate;
		if (prci!=null) {
			comments+="; "+prci.comments;
			rcvDate=prci.receiveDate;
		} else {
			rcvDate=new Date();
		}
		if (isTransacted() || getListener() instanceof IHasProcessState ||
				(getErrorStorage() != null &&
					(!isCheckForDuplicates() || !getErrorStorage().containsMessageId(messageId) || !isDuplicateAndSkip(getMessageBrowser(ProcessState.ERROR), messageId, correlationId))
				)
			) {
			moveInProcessToError(messageId, correlationId, messageSupplier, rcvDate, comments, rawMessage, TXREQUIRED);
		}
		PipeLineResult plr = new PipeLineResult();
		Message result=new Message("<error>"+XmlUtils.encodeChars(comments)+"</error>");
		plr.setResult(result);
		plr.setState(ExitState.ERROR);
		if (getSender()!=null) {
			String sendMsg = sendResultToSender(result);
			if (sendMsg != null) {
				log.warn("problem sending result:"+sendMsg);
			}
		}
		origin.afterMessageProcessed(plr, rawMessage, threadContext);
	}

	public void moveInProcessToError(String originalMessageId, String correlationId, ThrowingSupplier<Message,ListenerException> messageSupplier, Date receivedDate, String comments, Object rawMessage, TransactionDefinition txDef) {
		if (getListener() instanceof IHasProcessState) {
			ProcessState targetState = knownProcessStates.contains(ProcessState.ERROR) ? ProcessState.ERROR : ProcessState.DONE;
			try {
				changeProcessState(rawMessage, targetState, comments);
			} catch (ListenerException e) {
				log.error(getLogPrefix()+"Could not set process state to ERROR", e);
			}
		}
		ISender errorSender = getErrorSender();
		ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
		if (errorSender==null && errorStorage==null && knownProcessStates().isEmpty()) {
			log.debug(getLogPrefix()+"has no errorSender, errorStorage or knownProcessStates, will not move message with id ["+originalMessageId+"] correlationId ["+correlationId+"] to errorSender/errorStorage");
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

		Message message=null;
		try {
			if (errorSender!=null) {
				message = messageSupplier.get();
				errorSender.sendMessageOrThrow(message, null);
			}
			if (errorStorage!=null) {
				Serializable sobj;
				if (rawMessage == null) {
					if (message==null) {
						message = messageSupplier.get();
					}
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
							if (message==null) {
								message = messageSupplier.get();
							}
							sobj=new MessageWrapper(message, originalMessageId);
						}
					}
				}
				errorStorage.storeMessage(originalMessageId, correlationId, receivedDate, comments, null, sobj);
			}
			txManager.commit(txStatus);
		} catch (Exception e) {
			log.error(getLogPrefix()+"Exception moving message with id ["+originalMessageId+"] correlationId ["+correlationId+"] to error sender or error storage, original message: [" + message + "]", e);
			try {
				if (!txStatus.isCompleted()) {
					txManager.rollback(txStatus);
				}
			} catch (Exception rbe) {
				log.error(getLogPrefix()+"Exception while rolling back transaction for message  with id ["+originalMessageId+"] correlationId ["+correlationId+"], original message: [" + message + "]", rbe);
			}
		}
	}

	/**
	 * Process the received message with {@link #processRequest(IListener, Object, Message, PipeLineSession)}.
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID
	 * N.B. callers of this method should clear the remaining ThreadContext if its not to be returned to their callers.
	 */
	@Override
	public Message processRequest(IListener<M> origin, M rawMessage, Message message, PipeLineSession session) throws ListenerException {
		try (final CloseableThreadContext.Instance ctc = getLoggingContext(getListener(), session)) {
			if (origin!=getListener()) {
				throw new ListenerException("Listener requested ["+origin.getName()+"] is not my Listener");
			}
			if (getRunState() != RunState.STARTED) {
				throw new ListenerException(getLogPrefix()+"is not started");
			}

			boolean sessionCreated=false;
			try {
				if (session==null) {
					session = new PipeLineSession();
					sessionCreated=true;
				}

				Date tsReceived = PipeLineSession.getTsReceived(session);
				Date tsSent = PipeLineSession.getTsSent(session);
				PipeLineSession.setListenerParameters(session, null, null, tsReceived, tsSent);
				String messageId = session.getMessageId();
				String correlationId = session.getCorrelationId();
				Message result = processMessageInAdapter(rawMessage, message, messageId, correlationId, session, -1, false, false);
				result.unscheduleFromCloseOnExitOf(session);
				return result;
			} finally {
				if (sessionCreated) {
					session.close();
				}
			}
		}
	}



	@Override
	public void processRawMessage(IListener<M> origin, M rawMessage) throws ListenerException {
		processRawMessage(origin, rawMessage, null, -1, false);
	}
	@Override
	public void processRawMessage(IListener<M> origin, M rawMessage, PipeLineSession session, boolean duplicatesAlreadyChecked) throws ListenerException {
		processRawMessage(origin, rawMessage, session, -1, duplicatesAlreadyChecked);
	}

	@Override
	public void processRawMessage(IListener<M> origin, M rawMessage, PipeLineSession session, long waitingDuration, boolean duplicatesAlreadyChecked) throws ListenerException {
		if (origin!=getListener()) {
			throw new ListenerException("Listener requested ["+origin.getName()+"] is not my Listener");
		}
		processRawMessage(rawMessage, session, waitingDuration, false, duplicatesAlreadyChecked);
	}

	/**
	 * All messages that for this receiver are pumped down to this method, so it actually calls the {@link Adapter} to process the message.<br/>
	 * Assumes that a transaction has been started where necessary.
	 */
	private void processRawMessage(Object rawMessageOrWrapper, PipeLineSession session, long waitingDuration, boolean manualRetry, boolean duplicatesAlreadyChecked) throws ListenerException {
		try (final CloseableThreadContext.Instance ctc = getLoggingContext(getListener(), session)) {
			if (rawMessageOrWrapper==null) {
				log.debug(getLogPrefix()+"received null message, returning directly");
				return;
			}
			long startExtractingMessage = System.currentTimeMillis();
			boolean sessionCreated=false;
			if (session==null) {
				session = new PipeLineSession();
				sessionCreated = true;
			}
			try {
				if(isForceRetryFlag()) {
					session.put(Receiver.RETRY_FLAG_SESSION_KEY, "true");
				}

				Message message = null;
				String messageId = null;
				try {
					message = getListener().extractMessage((M)rawMessageOrWrapper, session);
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
					messageId = getListener().getIdFromRawMessage((M)rawMessageOrWrapper, session);
				} catch (Exception e) {
					if(rawMessageOrWrapper instanceof MessageWrapper) { //somehow messages wrapped in MessageWrapper are in the ITransactionalStorage
						MessageWrapper<M> wrapper = (MessageWrapper)rawMessageOrWrapper;
						messageId = wrapper.getId();
						session.putAll(wrapper.getContext());
					} else {
						throw new ListenerException(e);
					}
				}
				String correlationId = session.getCorrelationId();
				LogUtil.setIdsToThreadContext(ctc, messageId, correlationId);
				long endExtractingMessage = System.currentTimeMillis();
				messageExtractionStatistics.addValue(endExtractingMessage-startExtractingMessage);
				Message output = processMessageInAdapter(rawMessageOrWrapper, message, messageId, correlationId, session, waitingDuration, manualRetry, duplicatesAlreadyChecked);
				try {
					output.close();
				} catch (Exception e) {
					log.warn("Could not close result message ["+output+"]", e);
				}
				resetNumberOfExceptionsCaughtWithoutMessageBeingReceived();
			} finally {
				if (sessionCreated) {
					session.close();
				}
			}
		}
		ThreadContext.clearAll();
	}

	private CloseableThreadContext.Instance getLoggingContext(IListener listener, PipeLineSession session) {
		CloseableThreadContext.Instance result = LogUtil.getThreadContext(adapter, session.getMessageId(), session);
		result.put(THREAD_CONTEXT_KEY_NAME, listener.getName()).put(THREAD_CONTEXT_KEY_TYPE, ClassUtils.classNameOf(listener));
		return result;
	}


	public void retryMessage(String storageKey) throws ListenerException {
		if (!messageBrowsers.containsKey(ProcessState.ERROR)) {
			throw new ListenerException(getLogPrefix()+"has no errorStorage, cannot retry storageKey ["+storageKey+"]");
		}
		try (PipeLineSession session = new PipeLineSession()) {
			if (getErrorStorage()==null) {
				// if there is only a errorStorageBrowser, and no separate and transactional errorStorage,
				// then the management of the errorStorage is left to the listener.
				IMessageBrowser<?> errorStorageBrowser = messageBrowsers.get(ProcessState.ERROR);
				Object msg = errorStorageBrowser.browseMessage(storageKey);
				processRawMessage(msg, session, -1, true, false);
				return;
			}
			PlatformTransactionManager txManager = getTxManager();
			//TransactionStatus txStatus = txManager.getTransaction(TXNEW);
			IbisTransaction itx = new IbisTransaction(txManager, TXNEW_PROC, "receiver [" + getName() + "]");
			Serializable msg=null;
			ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
			try {
				try {
					msg = errorStorage.getMessage(storageKey);
					processRawMessage(msg, session, -1, true, false);
				} catch (Throwable t) {
					itx.setRollbackOnly();
					throw new ListenerException(t);
				} finally {
					itx.commit();
				}
			} catch (ListenerException e) {
				IbisTransaction itxErrorStorage = new IbisTransaction(txManager, TXNEW_CTRL, "errorStorage of receiver [" + getName() + "]");
				try {
					String messageId = session.getMessageId();
					String correlationId = session.getCorrelationId();
					String receivedDateStr = (String)session.get(PipeLineSession.TS_RECEIVED_KEY);
					if (receivedDateStr==null) {
						log.warn(getLogPrefix()+PipeLineSession.TS_RECEIVED_KEY+" is unknown, cannot update comments");
					} else {
						Date receivedDate = DateUtils.parseToDate(receivedDateStr,DateUtils.FORMAT_FULL_GENERIC);
						errorStorage.deleteMessage(storageKey);
						errorStorage.storeMessage(messageId, correlationId,receivedDate,"after retry: "+e.getMessage(),null, msg);
					}
				} catch (SenderException e1) {
					itxErrorStorage.setRollbackOnly();
					log.warn(getLogPrefix()+"could not update comments in errorStorage",e1);
				} finally {
					itxErrorStorage.commit();
				}
				throw e;
			}
		}
	}

	/*
	 * Assumes message is read, and when transacted, transaction is still open.
	 */
	private Message processMessageInAdapter(Object rawMessageOrWrapper, Message message, String messageId, String correlationId, PipeLineSession session, long waitingDuration, boolean manualRetry, boolean duplicatesAlreadyChecked) throws ListenerException {
		long startProcessingTimestamp = System.currentTimeMillis();
		try (final CloseableThreadContext.Instance ctc = LogUtil.getThreadContext(getAdapter(), messageId, session)) {
			lastMessageDate = startProcessingTimestamp;
			log.debug(getLogPrefix()+"received message with messageId ["+messageId+"] correlationId ["+correlationId+"]");

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
					handler.setContext(session);
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

			String businessCorrelationId=session.get(PipeLineSession.correlationIdKey, correlationId);
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
					if (StringUtils.isNotEmpty(correlationId)) {
						log.info(getLogPrefix()+"did not find correlationId using "+cidText+", reverting to correlationId of transfer ["+correlationId+"]");
						businessCorrelationId=correlationId;
					}
				}
			}
			if (StringUtils.isEmpty(businessCorrelationId) && StringUtils.isNotEmpty(messageId)) {
				log.info(getLogPrefix()+"did not find (technical) correlationId, reverting to messageId ["+messageId+"]");
				businessCorrelationId=messageId;
			}
			log.info(getLogPrefix()+"messageId [" + messageId + "] correlationId [" + correlationId + "] businessCorrelationId [" + businessCorrelationId + "]");
			session.put(PipeLineSession.correlationIdKey, businessCorrelationId);
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
				final Message messageFinal = message;
				if (!duplicatesAlreadyChecked && hasProblematicHistory(messageId, manualRetry, rawMessageOrWrapper, () -> messageFinal, session, businessCorrelationId)) {
					if (!isTransacted()) {
						log.warn(getLogPrefix()+"received message with messageId [" + messageId + "] which has a problematic history; aborting processing");
					}
					numRejected.increase();
					setExitState(session, ExitState.REJECTED, 500);
					return Message.nullMessage();
				}
				if (isDuplicateAndSkip(getMessageBrowser(ProcessState.DONE), messageId, businessCorrelationId)) {
					setExitState(session, ExitState.SUCCESS, 304);
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

			IbisTransaction itx = new IbisTransaction(txManager, getTxDef(), "receiver [" + getName() + "]");

			// update processing statistics
			// count in processing statistics includes messages that are rolled back to input
			startProcessingMessage(waitingDuration);

			String errorMessage="";
			boolean messageInError = false;
			Message result = null;
			PipeLineResult pipeLineResult=null;
			try {
				Message pipelineMessage;
				if (getListener() instanceof IBulkDataListener) {
					try {
						IBulkDataListener<M> bdl = (IBulkDataListener<M>)getListener();
						pipelineMessage=new Message(bdl.retrieveBulkData(rawMessageOrWrapper,message,session));
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
				showProcessingContext(messageId, businessCorrelationId, session);
	//			threadContext=pipelineSession; // this is to enable Listeners to use session variables, for instance in afterProcessMessage()
				try {
					if (getMessageLog()!=null) {
						getMessageLog().storeMessage(messageId, businessCorrelationId, new Date(), RCV_MESSAGE_LOG_COMMENTS, label, new MessageWrapper(pipelineMessage, messageId));
					}
					log.debug(getLogPrefix()+"preparing TimeoutGuard");
					TimeoutGuard tg = new TimeoutGuard("Receiver "+getName());
					try {
						if (log.isDebugEnabled()) log.debug(getLogPrefix()+"activating TimeoutGuard with transactionTimeout ["+getTransactionTimeout()+"]s");
						tg.activateGuard(getTransactionTimeout());
						pipeLineResult = adapter.processMessageWithExceptions(messageId, pipelineMessage, session);
						setExitState(session, pipeLineResult.getState(), pipeLineResult.getExitCode());
						session.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, ""+ pipeLineResult.getExitCode());
						result=pipeLineResult.getResult();

						errorMessage = "exitState ["+pipeLineResult.getState()+"], result [";
						if(!Message.isEmpty(result) && result.size() > ITransactionalStorage.MAXCOMMENTLEN) { //Since we can determine the size, assume the message is preserved
							errorMessage += result.asString().substring(0, ITransactionalStorage.MAXCOMMENTLEN);
						} else {
							errorMessage += result;
						}
						errorMessage += "]";

						int status = pipeLineResult.getExitCode();
						if(status > 0) {
							errorMessage += ", exitcode ["+status+"]";
						}

						if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"received result: "+errorMessage); }
						messageInError=itx.isRollbackOnly();
					} finally {
						log.debug(getLogPrefix()+"canceling TimeoutGuard, isInterrupted ["+Thread.currentThread().isInterrupted()+"]");
						if (tg.cancel()) {
							errorMessage = "timeout exceeded";
							if (Message.isEmpty(result)) {
								result = new Message("<timeout/>");
							}
							messageInError=true;
						}
					}
					if (!messageInError && !isTransacted()) {
						messageInError = !pipeLineResult.isSuccessful();
					}
				} catch (Throwable t) {
					if (TransactionSynchronizationManager.isActualTransactionActive()) {
						log.debug("<*>"+getLogPrefix() + "TX Update: Received failure, transaction " + (itx.isRollbackOnly()?"already":"not yet") + " marked for rollback-only");
					}
					error("Exception in message processing", t);
					errorMessage = t.getMessage();
					messageInError = true;
					if (pipeLineResult==null) {
						pipeLineResult=new PipeLineResult();
					}
					if (Message.isEmpty(pipeLineResult.getResult())) {
						pipeLineResult.setResult(adapter.formatErrorMessage("exception caught",t,message,messageId,this,startProcessingTimestamp));
					}
					throw wrapExceptionAsListenerException(t);
				}
				if (getSender()!=null) {
					String sendMsg = sendResultToSender(result);
					if (sendMsg != null) {
						errorMessage = sendMsg;
					}
				}
			} finally {
				try {
					cacheProcessResult(messageId, errorMessage, new Date(startProcessingTimestamp));
					if (!isTransacted() && messageInError && !manualRetry) {
						final Message messageFinal = message;
						moveInProcessToError(messageId, businessCorrelationId, () -> messageFinal, new Date(startProcessingTimestamp), errorMessage, rawMessageOrWrapper, TXNEW_CTRL);
					}
					Map<String,Object> afterMessageProcessedMap=session;
					try {
						Object messageForAfterMessageProcessed = rawMessageOrWrapper;
						if (getListener() instanceof IHasProcessState && !itx.isRollbackOnly()) {
							ProcessState targetState = messageInError && knownProcessStates.contains(ProcessState.ERROR) ? ProcessState.ERROR : ProcessState.DONE;
							Object movedMessage = changeProcessState(rawMessageOrWrapper, targetState, messageInError ? errorMessage : null);
							if (movedMessage!=null) {
								messageForAfterMessageProcessed = movedMessage;
							}
						}
						getListener().afterMessageProcessed(pipeLineResult, messageForAfterMessageProcessed, afterMessageProcessedMap);
					} catch (Exception e) {
						if (manualRetry) {
							// Somehow messages wrapped in MessageWrapper are in the ITransactionalStorage. This might cause class cast exceptions.
							// There are, however, also Listeners that might use MessageWrapper as their raw message type, like JdbcListener
							error("Exception post processing after retry of message messageId ["+messageId+"] cid ["+correlationId+"]", e);
						} else {
							error("Exception post processing message messageId ["+messageId+"] cid ["+correlationId+"]", e);
						}
						throw wrapExceptionAsListenerException(e);
					}
				} finally {
					try {
						long finishProcessingTimestamp = System.currentTimeMillis();
						finishProcessingMessage(finishProcessingTimestamp-startProcessingTimestamp);
						if (!itx.isCompleted()) {
							// NB: Spring will take care of executing a commit or a rollback;
							// Spring will also ONLY commit the transaction if it was newly created
							// by the above call to txManager.getTransaction().
							itx.commit();
						} else {
							String msg="Transaction already completed; we didn't expect this";
							warn(msg);
							throw new ListenerException(getLogPrefix()+msg);
						}
					} finally {
						getAdapter().logToMessageLogWithMessageContentsOrSize(Level.INFO, "Adapter "+(!messageInError ? "Success" : "Error"), "result", result);
					}
				}
			}
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"messageId ["+messageId+"] correlationId ["+businessCorrelationId+"] returning result ["+result+"]");
			return result;
		}
	}

	private void setExitState(Map<String,Object> threadContext, ExitState state, int code) {
		if (threadContext!=null) {
			threadContext.put(PipeLineSession.EXIT_STATE_CONTEXT_KEY, state);
			threadContext.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, Integer.toString(code));
		}
	}

	@SuppressWarnings("synthetic-access")
	public synchronized void cacheProcessResult(String messageId, String errorMessage, Date receivedDate) {
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

	public String getCachedErrorMessage(String messageId) {
		ProcessResultCacheItem prci = getCachedProcessResult(messageId);
		return prci!=null ? prci.comments : null;
	}

	public int getDeliveryCount(String messageId, M rawMessage) {
		IListener<M> origin = getListener(); // N.B. listener is not used when manualRetry==true
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"checking delivery count for messageId ["+messageId+"]");
		if (origin instanceof IKnowsDeliveryCount) {
			return ((IKnowsDeliveryCount<M>)origin).getDeliveryCount(rawMessage)-1;
		}
		ProcessResultCacheItem prci = getCachedProcessResult(messageId);
		if (prci==null) {
			return 1;
		}
		return prci.receiveCount+1;
	}
	/*
	 * returns true if message should not be processed
	 */
	@SuppressWarnings("unchecked")
	public boolean hasProblematicHistory(String messageId, boolean manualRetry, Object rawMessageOrWrapper, ThrowingSupplier<Message,ListenerException> messageSupplier, Map<String,Object>threadContext, String correlationId) throws ListenerException {
		if (manualRetry) {
			threadContext.put(RETRY_FLAG_SESSION_KEY, "true");
		} else {
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
						threadContext.put(RETRY_FLAG_SESSION_KEY, "true");
					}
					if (deliveryCount>getMaxDeliveries()) {
						warn("message with messageId ["+messageId+"] has already been delivered ["+deliveryCount+"] times, will not process; maxDeliveries=["+getMaxDeliveries()+"]");
						String comments="too many deliveries";
						increaseRetryIntervalAndWait(null,getLogPrefix()+"received message with messageId ["+messageId+"] too many times ["+deliveryCount+"]; maxDeliveries=["+getMaxDeliveries()+"]");
						moveInProcessToErrorAndDoPostProcessing(origin, messageId, correlationId, (M)rawMessageOrWrapper, messageSupplier, threadContext, prci, comments); // cast to M is done only if !manualRetry
						return true;
					}
				}
				resetRetryInterval();
				return false;
			}
			threadContext.put(RETRY_FLAG_SESSION_KEY, "true");
			if (getMaxRetries()<0) {
				increaseRetryIntervalAndWait(null,getLogPrefix()+"message with messageId ["+messageId+"] has already been received ["+prci.receiveCount+"] times; maxRetries=["+getMaxRetries()+"]");
				return false;
			}
			if (prci.receiveCount<=getMaxRetries()) {
				log.warn(getLogPrefix()+"message with messageId ["+messageId+"] has already been received ["+prci.receiveCount+"] times, will try again; maxRetries=["+getMaxRetries()+"]");
				resetRetryInterval();
				return false;
			}
			if (isSupportProgrammaticRetry()) {
				warn("message with messageId ["+messageId+"] has been received ["+prci.receiveCount+"] times, but programmatic retries supported; maxRetries=["+getMaxRetries()+"]");
				return true; // message will be retried because supportProgrammaticRetry=true, but when it fails, it will be moved to error state.
			}
			warn("message with messageId ["+messageId+"] has been received ["+prci.receiveCount+"] times, will not try again; maxRetries=["+getMaxRetries()+"]");
			String comments="too many retries";
			if (prci.receiveCount>getMaxRetries()+1) {
				increaseRetryIntervalAndWait(null,getLogPrefix()+"received message with messageId ["+messageId+"] too many times ["+prci.receiveCount+"]; maxRetries=["+getMaxRetries()+"]");
			}
			moveInProcessToErrorAndDoPostProcessing(origin, messageId, correlationId, (M)rawMessageOrWrapper, messageSupplier, threadContext, prci, comments); // cast to M is done only if !manualRetry
			prci.receiveCount++; // make sure that the next time this message is seen, the retry interval will be increased
			return true;
		}
		return isCheckForDuplicates() && getMessageLog()!= null && getMessageLog().containsMessageId(messageId);
	}

	private void resetProblematicHistory(String messageId) {
		ProcessResultCacheItem prci = getCachedProcessResult(messageId);
		if (prci!=null) {
			prci.receiveCount=0;
		}
	}

	/*
	 * returns true if message should not be processed
	 */
	private boolean isDuplicateAndSkip(IMessageBrowser<Object> transactionStorage, String messageId, String correlationId) throws ListenerException {
		if (isCheckForDuplicates() && transactionStorage != null) {
			if (getCheckForDuplicatesMethod()== CheckForDuplicatesMethod.CORRELATIONID) {
				if (transactionStorage.containsCorrelationId(correlationId)) {
					warn("message with correlationId [" + correlationId + "] already exists in messageLog, will not process");
					return true;
				}
			} else {
				if (transactionStorage.containsMessageId(messageId)) {
					warn("message with messageId [" + messageId + "] already exists in messageLog, will not process");
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void exceptionThrown(INamedObject object, Throwable t) {
		String msg = getLogPrefix()+"received exception ["+t.getClass().getName()+"] from ["+object.getName()+"]";
		exceptionThrown(msg, t);
	}

	public void exceptionThrown(String errorMessage, Throwable t) {
		switch (getOnError()) {
			case CONTINUE:
				if(numberOfExceptionsCaughtWithoutMessageBeingReceived.increase() > numberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold) {
					numberOfExceptionsCaughtWithoutMessageBeingReceivedThresholdReached=true;
					log.warn("numberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold is reached, changing the adapter status to 'warning'");
				}
				error(errorMessage+", will continue processing messages when they arrive", t);
				break;
			case RECOVER:
				// Make JobDef.recoverAdapters() try to recover
				error(errorMessage+", will try to recover",t);
				setRunState(RunState.ERROR); //Setting the state to ERROR automatically stops the receiver
				break;
			case CLOSE:
				error(errorMessage+", stopping receiver", t);
				stopRunning();
				break;
		}
	}

	@Override
	public String getEventSourceName() {
		return getLogPrefix().trim();
	}
	protected void registerEvent(String eventCode) {
		if (eventPublisher != null) {
			eventPublisher.registerEvent(this, eventCode);
		}
	}
	protected void throwEvent(String eventCode) {
		if (eventPublisher != null) {
			eventPublisher.fireEvent(this, eventCode);
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
		if (currentInterval*2 > RCV_SUSPENSION_MESSAGE_THRESHOLD && !suspensionMessagePending) {
			suspensionMessagePending=true;
			throwEvent(RCV_SUSPENDED_MONITOR_EVENT);
		}
		while (isInRunState(RunState.STARTED) && currentInterval-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (Exception e2) {
				error("sleep interupted", e2);
				stopRunning();
			}
		}
	}


	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		Object recData=hski.openGroup(data,getName(),"receiver");
		try {
			hski.handleScalar(recData,"messagesReceived", numReceived);
			hski.handleScalar(recData,"messagesRetried", numRetried);
			hski.handleScalar(recData,"messagesRejected", numRejected);
			hski.handleScalar(recData,"messagesReceivedThisInterval", numReceived.getIntervalValue());
			hski.handleScalar(recData,"messagesRetriedThisInterval", numRetried.getIntervalValue());
			hski.handleScalar(recData,"messagesRejectedThisInterval", numRejected.getIntervalValue());
			messageExtractionStatistics.performAction(action);
			Object pstatData=hski.openGroup(recData,null,"procStats");
			for(StatisticsKeeper pstat:getProcessStatistics()) {
				hski.handleStatisticsKeeper(pstatData,pstat);
				pstat.performAction(action);
			}
			hski.closeGroup(pstatData);

			Object istatData=hski.openGroup(recData,null,"idleStats");
			for(StatisticsKeeper istat:getIdleStatistics()) {
				hski.handleStatisticsKeeper(istatData,istat);
				istat.performAction(action);
			}
			hski.closeGroup(istatData);

			Iterable<StatisticsKeeper> statsIter = getQueueingStatistics();
			if (statsIter!=null) {
				Object qstatData=hski.openGroup(recData,null,"queueingStats");
				for(StatisticsKeeper qstat:statsIter) {
					hski.handleStatisticsKeeper(qstatData,qstat);
					qstat.performAction(action);
				}
				hski.closeGroup(qstatData);
			}

		} finally {
			hski.closeGroup(recData);
		}
	}



	@Override
	public boolean isThreadCountReadable() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();

			return tcc.isThreadCountReadable();
		}
		return getListener() instanceof IPullingListener;
	}
	@Override
	public boolean isThreadCountControllable() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();

			return tcc.isThreadCountControllable();
		}
		return getListener() instanceof IPullingListener;
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

	/**
	 * Changes runstate.
	 * Always stops the receiver when state is `**ERROR**`
	 */
	@Protected
	public void setRunState(RunState state) {
		if(RunState.ERROR.equals(state)) {
			stopRunning();
		}

		synchronized (runState) {
			runState.setRunState(state);
		}
	}

	/**
	 * Get the {@link RunState runstate} of this receiver.
	 */
	@Override
	public RunState getRunState() {
		return runState.getRunState();
	}

	public boolean isInRunState(RunState someRunState) {
		return runState.getRunState()==someRunState;
	}
	private String sendResultToSender(Message result) {
		String errorMessage = null;
		try {
			if (getSender() != null) {
				if (log.isDebugEnabled()) { log.debug("Receiver ["+getName()+"] sending result to configured sender"); }
				getSender().sendMessageOrThrow(result, null); // sending correlated responses via a receiver embedded sender is not supported
			}
		} catch (Exception e) {
			String msg = "caught exception in message post processing";
			error(msg, e);
			errorMessage = msg + ": " + e.getMessage();
			if (OnError.CLOSE == getOnError()) {
				log.info("closing after exception in post processing");
				stopRunning();
			}
		}
		return errorMessage;
	}

	@Override
	public Message formatException(String extrainfo, String messageId, Message message, Throwable t) {
		return getAdapter().formatErrorMessage(extrainfo,t,message,messageId,null,0);
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
	public Iterable<StatisticsKeeper> getProcessStatistics() {
		return processStatistics;
	}

	/**
	 * Returns an iterator over the idle-statistics
	 * @return iterator
	 */
	@Override
	public Iterable<StatisticsKeeper> getIdleStatistics() {
		return idleStatistics;
	}
	public Iterable<StatisticsKeeper> getQueueingStatistics() {
		return queueingStatistics;
	}


	public boolean isOnErrorContinue() {
		return OnError.CONTINUE == getOnError();
	}

	/**
	 * get the number of messages received by this receiver.
	 */
	public long getMessagesReceived() {
		return numReceived.getValue();
	}

	/**
	 * get the number of duplicate messages received this receiver.
	 */
	public long getMessagesRetried() {
		return numRetried.getValue();
	}

	/**
	 * Get the number of messages rejected (discarded or put in errorStorage).
	 */
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



	public void resetNumberOfExceptionsCaughtWithoutMessageBeingReceived() {
		if(log.isDebugEnabled()) log.debug("resetting [numberOfExceptionsCaughtWithoutMessageBeingReceived] to 0");
		numberOfExceptionsCaughtWithoutMessageBeingReceived.setValue(0);
		numberOfExceptionsCaughtWithoutMessageBeingReceivedThresholdReached=false;
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
	 * Sets the listener used to receive messages from.
	 *
	 * @ff.mandatory
	 */
	public void setListener(IListener<M> newListener) {
		listener = newListener;
		if (listener instanceof RunStateEnquiring)  {
			((RunStateEnquiring) listener).SetRunStateEnquirer(runState);
		}
	}

	/**
	 * Sender to which the response (output of {@link PipeLine}) should be sent. Applies if the receiver
	 * has an asynchronous listener.
	 * N.B. Sending correlated responses via this sender is not supported.
	 */
	public void setSender(ISender sender) {
		this.sender = sender;
	}

	/**
	 * Sets the inProcessStorage.
	 * @param inProcessStorage The inProcessStorage to set
	 * @deprecated
	 */
	@Deprecated
	@ConfigurationWarning("In-Process Storage no longer exists")
	public void setInProcessStorage(ITransactionalStorage<Serializable> inProcessStorage) {
		// We do not use an in-process storage anymore, but we temporarily
		// store it if it's set by the configuration.
		// During configure, we check if we need to use the in-process storage
		// as error-storage.
		this.tmpInProcessStorage = inProcessStorage;
	}



	/**
	 * Sender that will send the result in case the PipeLineExit state was not <code>SUCCESS</code>.
	 * Applies if the receiver has an asynchronous listener.
	 */
	public void setErrorSender(ISender errorSender) {
		this.errorSender = errorSender;
		errorSender.setName("errorSender of ["+getName()+"]");
	}

	@IbisDoc({"Storage to keep track of messages that failed processing"})
	public void setErrorStorage(ITransactionalStorage<Serializable> errorStorage) {
		this.errorStorage = errorStorage;
	}


	@IbisDoc({"Storage to keep track of all messages processed correctly"})
	public void setMessageLog(ITransactionalStorage<Serializable> messageLog) {
		this.messageLog = messageLog;
	}


	/**
	 * Sets the name of the Receiver.
	 * If the listener implements the {@link INamedObject name} interface and <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 */
	@IbisDoc({"Name of the Receiver as known to the Adapter", ""})
	@Override
	public void setName(String newName) {
		name = newName;
		propagateName();
	}

	@IbisDoc({"One of 'continue' or 'close'. Controls the behaviour of the Receiver when it encounters an error sending a reply or receives an exception asynchronously", "CONTINUE"})
	public void setOnError(OnError value) {
		this.onError = value;
	}

	/**
	 * The number of threads that this receiver is configured to work with.
	 */
	@IbisDoc({"The number of threads that may execute a Pipeline concurrently (only for pulling listeners)", "1"})
	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}

	@IbisDoc({"The number of threads that are actively polling for messages concurrently. '0' means 'limited only by <code>numthreads</code>' (only for pulling listeners)", "1"})
	public void setNumThreadsPolling(int i) {
		numThreadsPolling = i;
	}

	@IbisDoc({"The number of seconds waited after an unsuccesful poll attempt before another poll attempt is made. Only for polling listeners, not for e.g. ifsa, jms, webservice or javaListeners", "10"})
	public void setPollInterval(int i) {
		pollInterval = i;
	}

	/** timeout to start receiver. If this timeout is reached, the Receiver may be stopped again */
	public void setStartTimeout(int i) {
		startTimeout = i;
	}
	/** timeout to stopped receiver. If this timeout is reached, a new stop command may be issued */
	public void setStopTimeout(int i) {
		stopTimeout = i;
	}

	@IbisDoc({"If set to <code>true</code>, each message is checked for presence in the messageLog. If already present, it is not processed again. Only required for non XA compatible messaging. Requires messageLog!", "false"})
	public void setCheckForDuplicates(boolean b) {
		checkForDuplicates = b;
	}

	@IbisDoc({"(Only used when <code>checkForDuplicates=true</code>) Indicates whether the messageid or the correlationid is used for checking presence in the message log", "MESSAGEID"})
	public void setCheckForDuplicatesMethod(CheckForDuplicatesMethod method) {
		checkForDuplicatesMethod=method;
	}

	@IbisDoc({"The maximum delivery count after which to stop processing the message (only for listeners that know the delivery count of received messages). If -1 the delivery count is ignored", "5"})
	public void setMaxDeliveries(int i) {
		maxDeliveries = i;
	}

	@IbisDoc({"The number of times a processing attempt is automatically retried after an exception is caught or rollback is experienced. If <code>maxRetries &lt; 0</code> the number of attempts is infinite", "1"})
	public void setMaxRetries(int i) {
		maxRetries = i;
	}

	@IbisDoc({"Size of the cache to keep process results, used by maxRetries", "100"})
	public void setProcessResultCacheSize(int processResultCacheSize) {
		this.processResultCacheSize = processResultCacheSize;
	}

	@Deprecated
	@ConfigurationWarning("attribute is no longer used. Please use attribute returnedSessionKeys of the JavaListener if the set of sessionsKeys that can be returned to callers session must be limited.")
	public void setReturnedSessionKeys(String string) {
		// no longer used
	}

	@IbisDoc({"XPath expression to extract correlationid from message", ""})
	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}

	@IbisDoc({"Namespace defintions for correlationIDXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setCorrelationIDNamespaceDefs(String correlationIDNamespaceDefs) {
		this.correlationIDNamespaceDefs = correlationIDNamespaceDefs;
	}

	@IbisDoc({"Stylesheet to extract correlationID from message", ""})
	public void setCorrelationIDStyleSheet(String string) {
		correlationIDStyleSheet = string;
	}

	@IbisDoc({"XPath expression to extract label from message", ""})
	public void setLabelXPath(String string) {
		labelXPath = string;
	}

	@IbisDoc({"Namespace defintions for labelXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setLabelNamespaceDefs(String labelNamespaceDefs) {
		this.labelNamespaceDefs = labelNamespaceDefs;
	}

	@IbisDoc({"Stylesheet to extract label from message", ""})
	public void setLabelStyleSheet(String string) {
		labelStyleSheet = string;
	}

	@IbisDoc({"If set (>=0) and the character data length inside a xml element exceeds this size, the character data is chomped (with a clear comment)", ""})
	public void setChompCharSize(String string) {
		chompCharSize = string;
	}

	@IbisDoc({"If set, the character data in this element is stored under a session key and in the message replaced by a reference to this session key: {sessionkey: + <code>elementToMoveSessionKey</code> + }", ""})
	public void setElementToMove(String string) {
		elementToMove = string;
	}

	@IbisDoc({"(Only used when <code>elementToMove</code> is set) Name of the session key under which the character data is stored", "ref_ + the name of the element"})
	public void setElementToMoveSessionKey(String string) {
		elementToMoveSessionKey = string;
	}

	@IbisDoc({"Like <code>elementToMove</code> but element is preceded with all ancestor elements and separated by semicolons (e.g. adapter;pipeline;pipe)", ""})
	public void setElementToMoveChain(String string) {
		elementToMoveChain = string;
	}

	public void setRemoveCompactMsgNamespaces(boolean b) {
		removeCompactMsgNamespaces = b;
	}

	@IbisDoc({"Regular expression to mask strings in the errorStore/logStore. Every character between to the strings in this expression will be replaced by a '*'. For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every character between keys &lt;party&gt; and &lt;/party&gt;", ""})
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}

	@IbisDoc({"Only used when hideRegex is not empty", "all"})
	public void setHideMethod(HideMethod hideMethod) {
		this.hideMethod = hideMethod;
	}

	@IbisDoc({"Comma separated list of keys of session variables which are available when the <code>PipelineSession</code> is created and of which the value will not be shown in the log (replaced by asterisks)", ""})
	public void setHiddenInputSessionKeys(String string) {
		hiddenInputSessionKeys = string;
	}

	@IbisDoc({"If set to <code>true</code>, every message read will be processed as if it is being retried, by setting a session variable '"+Receiver.RETRY_FLAG_SESSION_KEY+"'", "false"})
	public void setForceRetryFlag(boolean b) {
		forceRetryFlag = b;
	}

	@IbisDoc({"Number of connection attemps to put the adapter in warning status", "5"})
	public void setNumberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold(int number) {
		this.numberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold = number;
	}
}
