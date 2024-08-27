/*
   Copyright 2013, 2015, 2016, 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.receivers;

import static org.frankframework.functional.FunctionalUtil.logValue;
import static org.frankframework.functional.FunctionalUtil.supplier;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.Adapter;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.HasSender;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IHasProcessState;
import org.frankframework.core.IKnowsDeliveryCount;
import org.frankframework.core.IListener;
import org.frankframework.core.IManagable;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IMessageBrowser.HideMethod;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.INamedObject;
import org.frankframework.core.IPortConnectedListener;
import org.frankframework.core.IProvidesMessageBrowsers;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IRedeliveringListener;
import org.frankframework.core.ISender;
import org.frankframework.core.IThreadCountControllable;
import org.frankframework.core.ITransactionRequirements;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.IbisTransaction;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.core.TransactionAttributes;
import org.frankframework.doc.Category;
import org.frankframework.doc.Protected;
import org.frankframework.jdbc.JdbcFacade;
import org.frankframework.jdbc.MessageStoreListener;
import org.frankframework.jms.JMSFacade;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.logging.IbisMaskingLayout;
import org.frankframework.monitoring.EventPublisher;
import org.frankframework.monitoring.EventThrowing;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.HasStatistics;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CompactSaxHandler;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.RunState;
import org.frankframework.util.RunStateEnquiring;
import org.frankframework.util.RunStateManager;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.UUIDUtil;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.xml.sax.SAXException;

import io.micrometer.core.instrument.DistributionSummary;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for a listener that specifies a channel for the incoming messages of a specific {@link Adapter}.
 * By choosing a listener, the Frank developer determines how the messages are received.
 * For example, an {@link org.frankframework.http.rest.ApiListener} receives RESTful HTTP requests and a
 * {@link JavaListener} receives messages from direct Java calls.
 * <br/><br/>
 * Apart from wrapping the listener, a {@link Receiver} can be configured
 * to store received messages and to keep track of the processed / failed
 * status of these messages.
 * <br/><br/>
 * There are two kinds of listeners: synchronous listeners and asynchronous listeners.
 * Synchronous listeners are expected to return a response. The system that triggers the
 * receiver typically waits for a response before proceeding its operation. When a
 * {@link org.frankframework.http.rest.ApiListener} receives a HTTP request, the listener is expected to return a
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
 * Listeners call the Receiver#processRawMessage(). Internally the Receiver calls Adapter#processMessageWithException()
 * to do the actual work, which returns a <code>{@link PipeLineResult}</code>. The receiver
 * may observe the status in the <code>{@link PipeLineResult}</code> to perform committing
 * requests.
 *
 */
@Category("Basic")
public class Receiver<M> extends TransactionAttributes implements IManagable, IMessageHandler<M>, IProvidesMessageBrowsers<M>, EventThrowing, IbisExceptionListener, HasSender, HasStatistics, IThreadCountControllable {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	public static final TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
	public static final TransactionDefinition TXNEW_CTRL = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	private TransactionDefinition newTransaction;

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
		 * If an error occurs (e.g. connection is lost) the receiver will be stopped and marked as ERROR
		 * Once every <code>recover.adapters.interval</code> it attempts to (re-) start the receiver.
		 */
		RECOVER,

		/** Stop the receiver when an error occurs. */
		CLOSE
	}

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
	public enum CheckForDuplicatesMethod { MESSAGEID, CORRELATIONID }

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
	private Pattern hideRegexPattern = null;
	private @Getter HideMethod hideMethod = HideMethod.ALL;
	private @Getter String hiddenInputSessionKeys=null;

	private final AtomicInteger numberOfExceptionsCaughtWithoutMessageBeingReceived = new AtomicInteger();
	private int numberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold = 5;
	private @Getter boolean numberOfExceptionsCaughtWithoutMessageBeingReceivedThresholdReached=false;

	private int retryInterval=1;

	private boolean suspensionMessagePending=false;
	private boolean configurationSucceeded = false;

	protected final RunStateManager runState = new RunStateManager();
	private PullingListenerContainer<M> listenerContainer;

	private final AtomicInteger threadsProcessing = new AtomicInteger();

	private long lastMessageDate = 0;

	// number of messages received
	private io.micrometer.core.instrument.Counter numReceived;
	private io.micrometer.core.instrument.Counter numRetried;
	private io.micrometer.core.instrument.Counter numRejected;

	private final List<DistributionSummary> processStatistics = new ArrayList<>();
	private final List<DistributionSummary> idleStatistics = new ArrayList<>();

	// the adapter that handles the messages and initiates this listener
	private @Getter @Setter Adapter adapter;

	private @Getter IListener<M> listener;
	private @Getter ISender errorSender=null;
	// See configure() for explanation on this field
	private @Getter ITransactionalStorage<Serializable> messageLog=null;
	private @Getter ITransactionalStorage<Serializable> errorStorage=null;
	private @Getter ISender sender=null; // reply-sender
	private final Map<ProcessState,IMessageBrowser<?>> messageBrowsers = new EnumMap<>(ProcessState.class);

	private TransformerPool correlationIDTp=null;
	private TransformerPool labelTp=null;

	private @Setter MetricsInitializer configurationMetrics;

	private @Getter @Setter PlatformTransactionManager txManager;

	private @Setter EventPublisher eventPublisher;

	private final Set<ProcessState> knownProcessStates = new LinkedHashSet<>();
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new EnumMap<>(ProcessState.class);

	/**
	 * The processResultCache acts as a sort of poor-mans error
	 * storage and is always available, even if an error-storage is not.
	 * Thus messages might be lost if they cannot be put in the error
	 * storage, but unless the server crashes, a message that has been
	 * put in the processResultCache will not be reprocessed even if it's
	 * offered again.
	 */
	private final Map<String,ProcessResultCacheItem> processResultCache = new LinkedHashMap<>() {

		@Override
		protected boolean removeEldestEntry(Entry<String,ProcessResultCacheItem> eldest) {
			return size() > getProcessResultCacheSize();
		}

	};

	private static class ProcessResultCacheItem {
		private int receiveCount;
		private Instant receiveDate;
		private String comments;
	}

	public boolean configurationSucceeded() {
		return configurationSucceeded;
	}

	private void showProcessingContext(String messageId, String correlationId, PipeLineSession session) {
		if (log.isDebugEnabled()) {
			Set<String> hiddenSessionKeys = new HashSet<>();
			if (getHiddenInputSessionKeys()!=null) {
				Collections.addAll(hiddenSessionKeys, getHiddenInputSessionKeys().split("[ ;,]+"));
			}

			StringBuilder contextDump = new StringBuilder().append("PipeLineSession variables for messageId [")
				.append(messageId).append("] correlationId [").append(correlationId).append("]:");

			session.forEach((key, value) -> {
				String strValue = "messageText".equals(key) ? "(... see elsewhere ...)" : String.valueOf(value);
				contextDump.append(" ").append(key).append("=[").append(hiddenSessionKeys.contains(key) ? StringUtil.hide(strValue) : strValue).append("]");
			});
			log.debug("{}{}", getLogPrefix(), contextDump);
		}
	}

	protected String getLogPrefix() {
		return "Receiver ["+getName()+"] ";
	}

	/**
	 * sends an informational message to the log and to the messagekeeper of the adapter
	 */
	protected void info(String msg) {
		log.info("{}{}", getLogPrefix(), msg);
		if (adapter != null) {
			adapter.getMessageKeeper().add(getLogPrefix() + msg);
		}
	}

	/**
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn("{}{}", getLogPrefix(), msg);
		if (adapter != null) {
			adapter.getMessageKeeper().add("WARNING: " + getLogPrefix() + msg, MessageKeeperLevel.WARN);
		}
	}

	/**
	 * sends a error message to the log and to the messagekeeper of the adapter
	 */
	protected void error(String msg, Throwable t) {
		log.error("{}{}", getLogPrefix(), msg, t);
		if (adapter != null) {
			adapter.getMessageKeeper().add("ERROR: " + getLogPrefix() + msg+(t!=null?": "+t.getMessage():""), MessageKeeperLevel.ERROR);
		}
	}


	protected void openAllResources() throws ListenerException, TimeoutException {
		// on exit resources must be in a state that runstate is or can be set to 'STARTED'
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
		log.debug("{}closing", getLogPrefix());
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
				// I want extra logging for the call-stack from where the timed-out request to stop came from.
				// Force an exception and catch it, so we have a stacktrace.
				Throwable t = new Throwable("Timeout Stopping Receiver [" + getName() + "] in thread [" + Thread.currentThread().getName() + "]");
				t.fillInStackTrace();
				log.warn("{}timeout stopping", getLogPrefix(), t);
			} else {
				log.debug("{}closed", getLogPrefix());
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
	 * This method is called by the <code>Adapter</code> to let the
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

			numReceived = configurationMetrics.createCounter(this, FrankMeterType.RECEIVER_RECEIVED);
			numRetried = configurationMetrics.createCounter(this, FrankMeterType.RECEIVER_RETRIED);
			numRejected = configurationMetrics.createCounter(this, FrankMeterType.RECEIVER_REJECTED);

			registerEvent(RCV_CONFIGURED_MONITOR_EVENT);
			registerEvent(RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
			registerEvent(RCV_STARTED_RUNNING_MONITOR_EVENT);
			registerEvent(RCV_SHUTDOWN_MONITOR_EVENT);
			registerEvent(RCV_SUSPENDED_MONITOR_EVENT);
			registerEvent(RCV_RESUMED_MONITOR_EVENT);
			registerEvent(RCV_THREAD_EXIT_MONITOR_EVENT);
			newTransaction = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,getTransactionTimeout());

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
			if (getListener() instanceof IPushingListener<M> pl) {
				pl.setHandler(this);
				pl.setExceptionListener(this);
			}
			if (getListener() instanceof IPortConnectedListener<M> pcl) {
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
				if (sender instanceof HasPhysicalDestination destination) {
					info("Listener has answer-sender on "+destination.getPhysicalDestinationName());
				}
			}
			if (getListener() instanceof ITransactionRequirements tr) {
				if (tr.transactionalRequired() && !isTransacted()) {
					ConfigurationWarnings.add(this, log, "listener type ["+ClassUtils.nameOf(getListener())+"] requires transactional processing", SuppressKeys.TRANSACTION_SUPPRESS_KEY, getAdapter());
					//throw new ConfigurationException(msg);
				}
			}
			ISender sender = getSender();
			if (sender!=null) {
				sender.configure();
				if (sender instanceof HasPhysicalDestination destination) {
					info("has answer-sender on "+destination.getPhysicalDestinationName());
				}
			}

			ISender errorSender = getErrorSender();
			if (errorSender!=null) {
				if (errorSender instanceof HasPhysicalDestination destination) {
					info("has errorSender to "+destination.getPhysicalDestinationName());
				}
				errorSender.configure();
			}

			if (getListener() instanceof IHasProcessState) {
				knownProcessStates.addAll(((IHasProcessState<?>)getListener()).knownProcessStates());
				targetProcessStates = ((IHasProcessState<?>)getListener()).targetProcessStates();
				supportProgrammaticRetry = knownProcessStates.contains(ProcessState.INPROCESS);
			}


			ITransactionalStorage<Serializable> messageLog = getMessageLog();
			if (messageLog!=null) {
				if (getListener() instanceof IProvidesMessageBrowsers && ((IProvidesMessageBrowsers<?>)getListener()).getMessageBrowser(ProcessState.DONE)!=null) {
					throw new ConfigurationException("listener with built-in messageLog cannot have external messageLog too");
				}
				messageLog.setName("messageLog of ["+getName()+"]");
				if (StringUtils.isEmpty(messageLog.getSlotId())) {
					messageLog.setSlotId(getName());
				}
				messageLog.setType(IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode());
				messageLog.configure();
				if (messageLog instanceof HasPhysicalDestination destination) {
					info("has messageLog in "+destination.getPhysicalDestinationName());
				}
				knownProcessStates.add(ProcessState.DONE);
				messageBrowsers.put(ProcessState.DONE, messageLog);
				if (StringUtils.isNotEmpty(getLabelXPath()) || StringUtils.isNotEmpty(getLabelStyleSheet())) {
					labelTp=TransformerPool.configureTransformer0(this, getLabelNamespaceDefs(), getLabelXPath(), getLabelStyleSheet(), OutputType.TEXT,false,null,0);
				}
			}
			ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
			if (errorStorage!=null) {
				if (getListener() instanceof IProvidesMessageBrowsers && ((IProvidesMessageBrowsers<?>)getListener()).getMessageBrowser(ProcessState.ERROR)!=null) {
					throw new ConfigurationException("listener with built-in errorStorage cannot have external errorStorage too");
				}
				errorStorage.setName("errorStorage of ["+getName()+"]");
				if (StringUtils.isEmpty(errorStorage.getSlotId())) {
					errorStorage.setSlotId(getName());
				}
				errorStorage.setType(IMessageBrowser.StorageType.ERRORSTORAGE.getCode());
				errorStorage.configure();
				if (errorStorage instanceof HasPhysicalDestination destination) {
					info("has errorStorage to "+destination.getPhysicalDestinationName());
				}
				knownProcessStates.add(ProcessState.ERROR);
				messageBrowsers.put(ProcessState.ERROR, errorStorage);
				registerEvent(RCV_MESSAGE_TO_ERRORSTORE_EVENT);
			}
			if (getListener() instanceof IProvidesMessageBrowsers) {
				for (ProcessState state: knownProcessStates) {
					IMessageBrowser<?> messageBrowser = ((IProvidesMessageBrowsers<?>)getListener()).getMessageBrowser(state);
					if (messageBrowser instanceof IConfigurable configurable) {
						configurable.configure();
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

			if (StringUtils.isNotEmpty(hideRegex)) {
				hideRegexPattern = Pattern.compile(hideRegex);

				if (getErrorStorage() != null && StringUtils.isEmpty(getErrorStorage().getHideRegex())) {
					getErrorStorage().setHideRegex(getHideRegex());
					getErrorStorage().setHideMethod(getHideMethod());
				}
				if (getMessageLog() != null && StringUtils.isEmpty(getMessageLog().getHideRegex())) {
					getMessageLog().setHideRegex(getHideRegex());
					getMessageLog().setHideMethod(getHideMethod());
				}
			}
		} catch (Throwable t) {
			ConfigurationException e;
			if (t instanceof ConfigurationException exception) {
				e = exception;
			} else {
				e = new ConfigurationException("Exception configuring receiver ["+getName()+"]",t);
			}
			throwEvent(RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
			log.debug("{} Errors occurred during configuration, setting runstate to ERROR", this::getLogPrefix);
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
					log.warn("{}on adapter [{}] was tried to start, but the adapter is in state [{}]. Ignoring command.", getLogPrefix(), adapter.getName(), adapterRunState);
					adapter.getMessageKeeper().add("ignored start command on [" + getName()  + "]; adapter is in state ["+adapterRunState+"]");
					return;
				}
			}
			// See also Adapter.startRunning()
			if (!configurationSucceeded) {
				log.error("configuration of receiver [{}] did not succeed, therefore starting the receiver is not possible", getName());
				warn("configuration did not succeed. Starting the receiver ["+getName()+"] is not possible");
				runState.setRunState(RunState.ERROR);
				return;
			}
			if (adapter.getConfiguration().isUnloadInProgressOrDone()) {
				log.error("configuration of receiver [{}] unload in progress or done, therefore starting the receiver is not possible", getName());
				warn("configuration unload in progress or done. Starting the receiver ["+getName()+"] is not possible");
				return;
			}
			log.trace("{} Receiver StartRunning - synchronize (lock) on Receiver runState[{}]", this::getLogPrefix, runState::toString);
			synchronized (runState) {
				RunState currentRunState = getRunState();
				if (currentRunState!=RunState.STOPPED
						&& currentRunState!=RunState.EXCEPTION_STOPPING
						&& currentRunState!=RunState.EXCEPTION_STARTING
						&& currentRunState!=RunState.ERROR
						&& configurationSucceeded()) { // Only start the receiver if it is properly configured, and is not already starting or still stopping
					if (currentRunState==RunState.STARTING || currentRunState==RunState.STARTED) {
						log.info("already in state [{}]", currentRunState);
					} else {
						log.warn("currently in state [{}], ignoring start() command", currentRunState);
					}
					return;
				}
				runState.setRunState(RunState.STARTING);
			}
			log.trace("{} Receiver StartRunning - lock on Receiver runState[{}] released", this::getLogPrefix, runState::toString);

			openAllResources();

			info("starts listening"); // Don't log that it's ready before it's ready!?
			runState.setRunState(RunState.STARTED);
			resetNumberOfExceptionsCaughtWithoutMessageBeingReceived();
		} catch (Throwable t) {
			error("error occurred while starting", t);

			runState.setRunState(RunState.EXCEPTION_STARTING);
			closeAllResources(); //Close potential dangling resources, don't change state here..
		}
	}

	//after successfully closing all resources the state should be set to stopped
	@Override
	public void stopRunning() {
		// See also Adapter.stopRunning() and PullingListenerContainer.ControllerTask
		log.trace("{} Receiver StopRunning - synchronize (lock) on Receiver runState[{}]", this::getLogPrefix, runState::toString);
		synchronized (runState) {
			RunState currentRunState = getRunState();
			switch (currentRunState) {
				case STARTING:
					log.warn("receiver currently in state [{}], ignoring stop() command", currentRunState);
					return;
				case STOPPED:
				case STOPPING:
				case EXCEPTION_STOPPING:
					log.info("receiver already in state [{}]", currentRunState);
					return;
				case EXCEPTION_STARTING:
					if (getListener() instanceof IPullingListener) {
						runState.setRunState(RunState.STOPPING); //Nothing ever started, directly go to stopped
						closeAllResources();
						ThreadContext.clearAll(); //Clean up receiver ThreadContext
						return; //Prevent tellResourcesToStop from being called
					}
					runState.setRunState(RunState.STOPPING);
					break;
				case STARTED:
					runState.setRunState(RunState.STOPPING);
					break;
				case ERROR:
					//Don't change the runstate when in ERROR
					break;
				default:
					throw new IllegalStateException("Runstate [" + currentRunState + "] not handled in Stopping Receiver");
			}
		}
		log.trace("{} Receiver StopRunning - lock on Receiver runState[{}] released", this::getLogPrefix, runState::toString);

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
	public RawMessageWrapper<M> changeProcessState(RawMessageWrapper<M> message, ProcessState toState, String reason) throws ListenerException {
		if (toState==ProcessState.AVAILABLE) {
			String id = message.getId();
			resetProblematicHistory(id);
		}
		return ((IHasProcessState<M>)getListener()).changeProcessState(message, toState, reason); // Cast is safe because changeProcessState will only be executed in internal MessageBrowser
	}

	@Override
	public IMessageBrowser<M> getMessageBrowser(ProcessState state) {
		return (IMessageBrowser<M>)messageBrowsers.get(state);
	}


	protected void startProcessingMessage() {
		threadsProcessing.getAndIncrement();
		log.debug("{} starts processing message", this::getLogPrefix);
	}

	protected void finishProcessingMessage(long processingDuration) {
		int threadCount = threadsProcessing.decrementAndGet();
		getProcessStatistics(threadCount).record(processingDuration);
		log.debug("{} finishes processing message", this::getLogPrefix);
	}

	private void moveInProcessToErrorAndDoPostProcessing(IListener<M> origin, MessageWrapper<M> messageWrapper, PipeLineSession session, ProcessResultCacheItem prci, String comments) throws ListenerException {
		String messageId = messageWrapper.getId();
		String correlationId = messageWrapper.getCorrelationId();
		try {
			Instant rcvDate;
			if (prci!=null) {
				comments+="; "+prci.comments;
				rcvDate=prci.receiveDate;
			} else {
				rcvDate=Instant.now();
			}
			if (isTransacted() || getListener() instanceof IHasProcessState ||
					(getErrorStorage() != null &&
						(!isCheckForDuplicates() || !getErrorStorage().containsMessageId(messageId) || !isDuplicateAndSkip(getMessageBrowser(ProcessState.ERROR), messageId, correlationId))
					)
				) {
				moveInProcessToError(messageWrapper, session, rcvDate, comments, TXREQUIRED);
			}
			PipeLineResult plr = new PipeLineResult();
			Message result=new Message("<error>"+ XmlEncodingUtils.encodeChars(comments)+"</error>");
			plr.setResult(result);
			plr.setState(ExitState.REJECTED);
			if (getSender()!=null) {
				String sendMsg = sendResultToSender(result);
				if (sendMsg != null) {
					log.warn("problem sending result: {}", sendMsg);
				}
			}
			origin.afterMessageProcessed(plr, messageWrapper, session);
		} catch (ListenerException e) {
			String errorDescription;
			if (prci != null) {
				errorDescription = getLogPrefix() + "received message with messageId [" + messageId + "] too many times [" + prci.receiveCount + "]; maxRetries=[" + getMaxRetries() + "]. Error occurred while moving message to error store.";
			} else {
				errorDescription = getLogPrefix() + "received message with messageId [" + messageId + "] too many times [" + getListenerDeliveryCount(messageWrapper, origin) + "]; maxDeliveries=[" + getMaxDeliveries() + "]. Error occurred while moving message to error store.";
			}
			increaseRetryIntervalAndWait(e, errorDescription);
			throw e;
		}
	}

	/**
	 * Move a message from the "in process" state or storage, to the error state or storage.
	 *
	 * @param rawMessageWrapper Wrapper for the raw message, may be an instance of {@link RawMessageWrapper} or {@link MessageWrapper}. If an instance of {@link RawMessageWrapper} then
	 *                          the {@link IListener} will be used to extract the full {@link Message} object to be sent to the error storage.
	 * @param context Context of the process. Can be either the thread context of a {@link IPullingListener}, or the current {@link PipeLineSession}.
	 * @param receivedDate Timestamp of when the message was received.
	 * @param comments Processing comments and error message regarding the reason the message was rejected.
	 * @param txDef {@link TransactionDefinition} for the transaction to be used for moving the message to error state / storage.
	 */
	public void moveInProcessToError(RawMessageWrapper<M> rawMessageWrapper, Map<String, Object> context, Instant receivedDate, String comments, TransactionDefinition txDef) {
		if (getListener() instanceof IHasProcessState && !knownProcessStates.isEmpty()) {
			ProcessState targetState = knownProcessStates.contains(ProcessState.ERROR) ? ProcessState.ERROR : ProcessState.DONE;
			try {
				changeProcessState(rawMessageWrapper, targetState, comments);
			} catch (ListenerException e) {
				log.error("{} Could not set process state to ERROR", supplier(this::getLogPrefix), e);
			}
		}

		String originalMessageId = rawMessageWrapper.getId();
		String correlationId;
		if (rawMessageWrapper.getCorrelationId() != null) {
			correlationId = rawMessageWrapper.getCorrelationId();
		} else {
			correlationId = (String) context.get(PipeLineSession.CORRELATION_ID_KEY);
		}

		final ISender errorSender = getErrorSender();
		final ITransactionalStorage<Serializable> errorStorage = getErrorStorage();

		// Bail out now if we have no error sender/storage, so we do not have to create a TX and get message from supplier.
		if (errorSender==null && errorStorage==null) {
			if (!(getListener() instanceof IHasProcessState) || knownProcessStates.isEmpty()) {
				log.debug("{} has no errorSender, errorStorage or knownProcessStates, will not move message with id [{}] correlationId [{}] to errorSender/errorStorage", this::getLogPrefix, () -> originalMessageId, () -> correlationId);
			}
			return;
		}
		log.debug("{} moves message with id [{}] correlationId [{}] to errorSender/errorStorage", this::getLogPrefix, ()->originalMessageId, ()->correlationId);
		TransactionStatus txStatus;
		try {
			txStatus = txManager.getTransaction(txDef);
		} catch (RuntimeException e) {
			log.error("{} Exception preparing to move input message with id [{}] correlationId [{}] to error sender", getLogPrefix(), originalMessageId, correlationId, e);
			// no use trying again to send message on errorSender, will cause same exception!

			// NB: Why does this case return, instead of re-throwing?
			return;
		}

		try {
			final Message message;
			if (rawMessageWrapper instanceof MessageWrapper) {
				message = ((MessageWrapper<M>) rawMessageWrapper).getMessage();
			} else {
				message = getListener().extractMessage(rawMessageWrapper, context);
			}
			throwEvent(RCV_MESSAGE_TO_ERRORSTORE_EVENT, message);
			if (errorSender != null) {
				try(PipeLineSession session = new PipeLineSession(); Message senderResult = errorSender.sendMessageOrThrow(message, session)) {
					log.debug("error-sender result [{}]", senderResult);
				}
			}
			if (errorStorage!=null) {
				Serializable sobj = serializeMessageObject(rawMessageWrapper, message);
				errorStorage.storeMessage(originalMessageId, correlationId, new Date(receivedDate.toEpochMilli()), comments, null, sobj);
			}
			txManager.commit(txStatus);
		} catch (Exception e) {
			log.error("{} Exception moving message with id [{}] correlationId [{}] to error sender or error storage, original message: [{}]", getLogPrefix(), originalMessageId, correlationId, rawMessageWrapper, e);
			try {
				if (!txStatus.isCompleted()) {
					txManager.rollback(txStatus);
				}
			} catch (Exception rbe) {
				log.error("{} Exception while rolling back transaction for message  with id [{}] correlationId [{}], original message: [{}]", getLogPrefix(), originalMessageId, correlationId, rawMessageWrapper, rbe);
			}
		}
	}

	private Serializable serializeMessageObject(RawMessageWrapper<M> rawMessageWrapper, Message message) {
		final Serializable sobj;

		if (rawMessageWrapper instanceof MessageWrapper<?> wrapper) {
			sobj = wrapper;
		} else {
			sobj = new MessageWrapper<>(rawMessageWrapper, message);
		}

		return sobj;
	}

	/**
	 * Process the received message with {@link #processRequest(IListener, RawMessageWrapper, Message, PipeLineSession)}.
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID
	 * N.B. callers of this method should clear the remaining ThreadContext if it's not to be returned to their callers.
	 */
	@Override
	public Message processRequest(IListener<M> origin, @Nonnull RawMessageWrapper<M> rawMessage, @Nonnull Message message, @Nonnull PipeLineSession session) throws ListenerException {
		Objects.requireNonNull(session, "Session can not be null");
		try (final CloseableThreadContext.Instance ignored = getLoggingContext(getListener(), session)) {
			if (origin!=getListener()) {
				throw new ListenerException("Listener requested ["+origin.getName()+"] is not my Listener");
			}
			if (getRunState() != RunState.STARTED) {
				throw new ListenerException(getLogPrefix()+"is not started");
			}

			Instant tsReceived = PipeLineSession.getTsReceived(session);
			Instant tsSent = PipeLineSession.getTsSent(session);
			PipeLineSession.updateListenerParameters(session, null, null, tsReceived, tsSent);

			String messageId = rawMessage.getId() != null ? rawMessage.getId() : session.getMessageId();
			String correlationId = rawMessage.getCorrelationId() != null ? rawMessage.getCorrelationId() : session.getCorrelationId();
			MessageWrapper<M> messageWrapper = rawMessage instanceof MessageWrapper ? (MessageWrapper<M>) rawMessage : new MessageWrapper<>(rawMessage, message, messageId, correlationId);

			boolean manualRetry = session.get(PipeLineSession.MANUAL_RETRY_KEY, false);

			final Message result;
			try {
				result = processMessageInAdapter(messageWrapper, session, manualRetry, manualRetry); // If manual retry, history is checked by original caller
			} catch (ListenerException e) {
				exceptionThrown("exception processing message", e);
				throw e;
			}

			if(!Message.isNull(result)) {
				result.unscheduleFromCloseOnExitOf(session);
			}
			return result;
		}
	}

	@Override
	public void processRawMessage(IListener<M> origin, RawMessageWrapper<M> rawMessage, @Nonnull PipeLineSession session, boolean duplicatesAlreadyChecked) throws ListenerException {
		if (origin!=getListener()) {
			throw new ListenerException("Listener requested ["+origin.getName()+"] is not my Listener");
		}
		processRawMessage(rawMessage, session, false, duplicatesAlreadyChecked);
	}

	/**
	 * All messages that for this receiver are pumped down to this method, so it actually calls the {@link Adapter} to process the message.<br/>
	 * Assumes that a transaction has been started where necessary.
	 */
	private void processRawMessage(RawMessageWrapper<M> rawMessageWrapper, @Nonnull PipeLineSession session, boolean manualRetry, boolean duplicatesAlreadyChecked) throws ListenerException {
		if (rawMessageWrapper == null) {
			log.debug("{} Received null message, returning directly", this::getLogPrefix);
			return;
		}
		Objects.requireNonNull(session, "Session can not be null");
		try (final CloseableThreadContext.Instance ctc = getLoggingContext(getListener(), session)) {
			if(isForceRetryFlag()) {
				session.put(Receiver.RETRY_FLAG_SESSION_KEY, "true");
			}

			String messageId = rawMessageWrapper.getId();
			String correlationId = rawMessageWrapper.getCorrelationId();
			session.putAll(rawMessageWrapper.getContext());
			LogUtil.setIdsToThreadContext(ctc, messageId, correlationId);

			MessageWrapper<M> messageWrapper;
			if (rawMessageWrapper instanceof MessageWrapper && !(getListener() instanceof MessageStoreListener)) {
				//somehow messages wrapped in MessageWrapper are in the ITransactionalStorage
				// There are, however, also Listeners that might use MessageWrapper as their raw message type,
				// like JdbcListener
				messageWrapper = (MessageWrapper<M>) rawMessageWrapper;
			} else {
				try {
					Message message = getListener().extractMessage(rawMessageWrapper, session);
					messageWrapper = new MessageWrapper<>(rawMessageWrapper, message);
				} catch (Exception e) {
					throw new ListenerException(e);
				}
			}

			Message output = processMessageInAdapter(messageWrapper, session, manualRetry, duplicatesAlreadyChecked);
			try { //Only catch IOExceptions on Message#close, processMessageInAdapter throws Exceptions, which should not be caught!!
				output.close();
				log.debug("Closing result message [{}]", output);
			} catch (IOException e) {
				log.warn("Could not close result message", e);
			}

			resetNumberOfExceptionsCaughtWithoutMessageBeingReceived();
		}
		ThreadContext.clearAll();
	}

	private CloseableThreadContext.Instance getLoggingContext(@Nonnull IListener<M> listener, @Nonnull PipeLineSession session) {
		CloseableThreadContext.Instance result = LogUtil.getThreadContext(adapter, session.getMessageId(), session);
		result.put(THREAD_CONTEXT_KEY_NAME, listener.getName());
		result.put(THREAD_CONTEXT_KEY_TYPE, ClassUtils.classNameOf(listener));
		return result;
	}


	public void retryMessage(String storageKey) throws ListenerException {
		if (!messageBrowsers.containsKey(ProcessState.ERROR)) {
			throw new ListenerException(getLogPrefix()+"has no errorStorage, cannot retry storageKey ["+storageKey+"]");
		}
		try (PipeLineSession session = new PipeLineSession()) {
			session.put(PipeLineSession.MANUAL_RETRY_KEY, true);
			if (getErrorStorage()==null) {
				// if there is only a errorStorageBrowser, and no separate and transactional errorStorage,
				// then the management of the errorStorage is left to the listener.
				IMessageBrowser<?> errorStorageBrowser = messageBrowsers.get(ProcessState.ERROR);
				RawMessageWrapper<?> msg = errorStorageBrowser.browseMessage(storageKey);
				//noinspection unchecked
				processRawMessage((RawMessageWrapper<M>) msg, session, true, false);
				return;
			}
			PlatformTransactionManager txManager = getTxManager();
			IbisTransaction itx = new IbisTransaction(txManager, newTransaction, "receiver [" + getName() + "]");
			RawMessageWrapper<Serializable> msg = null;
			ITransactionalStorage<Serializable> errorStorage = getErrorStorage();
			try {
				try {
					msg = errorStorage.getMessage(storageKey);
					//noinspection ReassignedVariable
					processRawMessage((RawMessageWrapper<M>) msg, session, true, false);
				} catch (Throwable t) {
					itx.setRollbackOnly();
					throw new ListenerException(t);
				} finally {
					itx.complete();
				}
			} catch (ListenerException e) {
				IbisTransaction itxErrorStorage = new IbisTransaction(txManager, TXNEW_CTRL, "errorStorage of receiver [" + getName() + "]");
				try {
					String messageId = session.getMessageId();
					String correlationId = session.getCorrelationId();
					Instant receivedDate = session.getTsReceived();
					if (receivedDate == null) {
						log.warn("{} {} is unknown, cannot update comments", this::getLogPrefix, logValue(PipeLineSession.TS_RECEIVED_KEY));
					} else {
						errorStorage.deleteMessage(storageKey);
						errorStorage.storeMessage(messageId, correlationId,Date.from(receivedDate),"after retry: "+e.getMessage(),null, msg.rawMessage);
					}
				} catch (SenderException e1) {
					itxErrorStorage.setRollbackOnly();
					log.warn("{} could not update comments in errorStorage", supplier(this::getLogPrefix), e1);
				} finally {
					itxErrorStorage.complete();
				}
				throw e;
			}
		}
	}

	/*
	 * Assumes message is read, and when transacted, transaction is still open.
	 */
	private Message processMessageInAdapter(MessageWrapper<M> messageWrapper, PipeLineSession session, boolean manualRetry, boolean duplicatesAlreadyChecked) throws ListenerException {
		final long startProcessingTimestamp = System.currentTimeMillis();
		final String logPrefix = getLogPrefix();
		// Add all hideRegexes at the same point so sensitive information is hidden in a consistent manner
		try (final CloseableThreadContext.Instance ignored = LogUtil.getThreadContext(getAdapter(), messageWrapper.getId(), session);
			 final IbisMaskingLayout.HideRegexContext ignored2 = IbisMaskingLayout.pushToThreadLocalReplace(hideRegexPattern);
			 final IbisMaskingLayout.HideRegexContext ignored3 = IbisMaskingLayout.pushToThreadLocalReplace(getAdapter().getComposedHideRegexPattern());
		) {
			lastMessageDate = startProcessingTimestamp;
			log.debug("{} received message with messageId [{}] correlationId [{}]", logPrefix, messageWrapper.getId(), messageWrapper.getCorrelationId());

			String messageId = ensureMessageIdNotEmpty(messageWrapper.getId());
			final String businessCorrelationId = getBusinessCorrelationId(messageWrapper, messageId, session);
			session.put(PipeLineSession.CORRELATION_ID_KEY, businessCorrelationId);

			MessageWrapper<M> messageWithMessageIdAndCorrelationId = new MessageWrapper<>(messageWrapper, messageWrapper.getMessage(), messageId, businessCorrelationId);

			boolean exitWithoutProcessing = checkMessageHistory(messageWithMessageIdAndCorrelationId, session, manualRetry, duplicatesAlreadyChecked);
			if (exitWithoutProcessing) {
				return Message.nullMessage();
			}

			IbisTransaction itx = new IbisTransaction(txManager, getTxDef(), "receiver [" + getName() + "]");

			// update processing statistics
			// count in processing statistics includes messages that are rolled back to input
			startProcessingMessage();

			String errorMessage = "";
			boolean messageInError = false;
			Message result = null;
			PipeLineResult pipeLineResult = null;
			try {
				final Message compactedMessage = compactMessageIfRequired(messageWrapper.getMessage(), session);

				numReceived.increment();
				showProcessingContext(messageId, businessCorrelationId, session);
	//			threadContext=pipelineSession; // this is to enable Listeners to use session variables, for instance in afterProcessMessage()
				try {
					if (getMessageLog() != null) {
						final String label = extractLabel(compactedMessage);
						getMessageLog().storeMessage(messageId, businessCorrelationId, new Date(), RCV_MESSAGE_LOG_COMMENTS, label, messageWithMessageIdAndCorrelationId);
					}
					log.debug("{} preparing TimeoutGuard", logPrefix);
					TimeoutGuard tg = new TimeoutGuard("Receiver "+getName());
					try {
						log.debug("{} activating TimeoutGuard with transactionTimeout [{}]s", logPrefix, getTransactionTimeout());
						tg.activateGuard(getTransactionTimeout());

						pipeLineResult = adapter.processMessageWithExceptions(messageId, compactedMessage, session);

						session.setExitState(pipeLineResult);
						result=pipeLineResult.getResult();

						errorMessage = "exitState ["+pipeLineResult.getState()+"], result [";
						if(!Message.isEmpty(result) && result.isRepeatable() && result.size() > ITransactionalStorage.MAXCOMMENTLEN - errorMessage.length()) { //Since we can determine the size, assume the message is preserved
							String resultString = result.asString();
							errorMessage += resultString.substring(0, Math.min(ITransactionalStorage.MAXCOMMENTLEN - errorMessage.length(), resultString.length()));
						} else {
							errorMessage += result;
						}
						errorMessage += "]";

						int status = pipeLineResult.getExitCode();
						if(status > 0) {
							errorMessage += ", exitcode ["+status+"]";
						}

						log.debug("{} received result: {}", logPrefix, errorMessage);
						messageInError=itx.isRollbackOnly();
					} finally {
						log.debug("{} canceling TimeoutGuard, isInterrupted [{}]", () -> logPrefix, () -> Thread.currentThread().isInterrupted());
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
						log.debug("<*>{}TX Update: Received failure, transaction {} marked for rollback-only", logPrefix, (itx.isRollbackOnly() ? "already" : "not yet"));
					}
					error("Exception in message processing", t);
					errorMessage = t.getMessage();
					messageInError = true;
					if (pipeLineResult==null) {
						pipeLineResult=new PipeLineResult();
					}
					if (Message.isEmpty(pipeLineResult.getResult())) {
						pipeLineResult.setResult(adapter.formatErrorMessage("exception caught",t,compactedMessage,messageId,this,startProcessingTimestamp));
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
				ProcessResultCacheItem prci = cacheProcessResult(messageId, errorMessage, Instant.ofEpochMilli(startProcessingTimestamp));
				try {
					if (!isTransacted() && messageInError && !manualRetry
							&& !(getListener() instanceof IRedeliveringListener<?> redeliveringListener && redeliveringListener.messageWillBeRedeliveredOnExitStateError())) {
						moveInProcessToError(messageWithMessageIdAndCorrelationId, session, Instant.ofEpochMilli(startProcessingTimestamp), errorMessage, TXNEW_CTRL);
					}
					try {
						RawMessageWrapper<M> messageForAfterMessageProcessed = messageWithMessageIdAndCorrelationId;
						if (getListener() instanceof IHasProcessState && !itx.isRollbackOnly()) {
							ProcessState targetState = messageInError && knownProcessStates.contains(ProcessState.ERROR) ? ProcessState.ERROR : ProcessState.DONE;
							RawMessageWrapper<M> movedMessage = changeProcessState(messageWithMessageIdAndCorrelationId, targetState, messageInError ? errorMessage : null);
							if (movedMessage!=null) {
								messageForAfterMessageProcessed = movedMessage;
							}
						}
						getListener().afterMessageProcessed(pipeLineResult, messageForAfterMessageProcessed, session);
					} catch (Exception e) {
						if (manualRetry) {
							// Somehow messages wrapped in MessageWrapper are in the ITransactionalStorage. This might cause class cast exceptions.
							// There are, however, also Listeners that might use MessageWrapper as their raw message type, like JdbcListener
							error("Exception post processing after retry of message messageId ["+messageId+"] cid ["+messageWrapper.getCorrelationId()+"]", e);
						} else {
							error("Exception post processing message messageId ["+messageId+"] cid ["+messageWrapper.getCorrelationId()+"]", e);
						}
						throw wrapExceptionAsListenerException(e);
					}
				} finally {
					try {
						long finishProcessingTimestamp = System.currentTimeMillis();
						finishProcessingMessage(finishProcessingTimestamp-startProcessingTimestamp);
						if (!itx.isCompleted()) {
							// NB: Spring will take care of executing a commit or a rollback.
							// Spring will also ONLY commit the transaction if it was newly created
							// by the above call to txManager.getTransaction().
							itx.complete();
						} else {
							String msg="Transaction already completed; we didn't expect this";
							warn(msg);
							throw new ListenerException(logPrefix +msg);
						}
					} finally {
						getAdapter().logToMessageLogWithMessageContentsOrSize(Level.INFO, "Adapter "+(!messageInError ? "Success" : "Error"), "result", result);
						if (messageInError && !duplicatesAlreadyChecked && retryCountNotReached(messageWrapper, prci)) {
							// Only do this if history has not already been checked previously by the caller.
							// If it has, then the caller is also responsible for handling the retry-interval.
							increaseRetryIntervalAndWait(null, getLogPrefix() + "message with messageId [" + messageId + "] has already been received [" + prci.receiveCount + "] times; maxRetries=[" + getMaxRetries() + "]; error in procesing: [" + errorMessage + "]");
						}
					}
				}
			}
			if (log.isDebugEnabled()) log.debug("{} messageId [{}] correlationId [{}] returning result [{}]", logPrefix, messageId, businessCorrelationId, result);
			return result;
		}
	}

	private boolean retryCountNotReached(@Nonnull final MessageWrapper<M> messageWrapper, @Nullable final ProcessResultCacheItem prci) {
		final IListener<M> origin = getListener();
		final int receiveCount;
		if (origin instanceof IKnowsDeliveryCount<M> knowsDeliveryCount) {
			receiveCount = knowsDeliveryCount.getDeliveryCount(messageWrapper);
		} else if (prci != null) {
			receiveCount = prci.receiveCount;
		} else {
			receiveCount = 1;
		}
		if (origin instanceof IRedeliveringListener<M> redeliveringListener && redeliveringListener.messageWillBeRedeliveredOnExitStateError()) {
			return receiveCount < maxDeliveries;
		}
		return receiveCount < maxRetries;
	}

	private String ensureMessageIdNotEmpty(String messageId) {
		if (StringUtils.isEmpty(messageId)) {
			messageId ="synthetic-message-id-" + UUIDUtil.createSimpleUUID();
			if (log.isDebugEnabled())
				log.debug("{} Message without message id; generated messageId [{}]", getLogPrefix(), messageId);
		}
		return messageId;
	}

	private Message compactMessageIfRequired(Message message, PipeLineSession session) throws ListenerException {
		if (getChompCharSize() != null || getElementToMove() != null || getElementToMoveChain() != null) {
			log.debug("{} compact received message", getLogPrefix());
			try {
				message.preserve();
				message = compactMessage(message, session);
			} catch (IOException | SAXException e) {
				String msg = "error during compacting received message to more compact format";
				error(msg, e);
				throw new ListenerException(msg, e);
			}
		}
		return message;
	}

	/**
	 * Check the message processing history and update status and exit-state accordingly.
	 * If message processing should be aborted, return {@code true}. If message should still be processed, return {@code false}.
	 *
	 * @param messageWrapper            Wrapped message object
	 * @param session                  {@link PipeLineSession} in which message is processed
	 * @param manualRetry              Indicator if this is a manually retried message.
	 * @param duplicatesAlreadyChecked Indicator if duplicates have already been previously checked
	 * @return {@code true} if message has history and should not be processed; {@code false} if the message should be processed.
	 * @throws ListenerException If an exception happens during processing.
	 */
	private boolean checkMessageHistory(MessageWrapper<M> messageWrapper, PipeLineSession session, boolean manualRetry, boolean duplicatesAlreadyChecked) throws ListenerException {
		String logPrefix = getLogPrefix();
		String messageId = messageWrapper.getId();
		String correlationId = messageWrapper.getCorrelationId();
		try {
			Optional<ProcessResultCacheItem> cachedProcessResult = getCachedProcessResult(messageId);
			if (!duplicatesAlreadyChecked && isDeliveryRetryLimitExceeded(messageId, correlationId, messageWrapper, session, manualRetry)) {
				if (!isTransacted()) {
					log.warn("{} received message with messageId [{}] which has a problematic history; aborting processing", logPrefix, messageId);
				}
				if (!isSupportProgrammaticRetry()) {
					cachedProcessResult.ifPresent(prci -> prci.receiveCount++);
					ProcessResultCacheItem prci = cachedProcessResult.orElse(null);
					IListener<M> origin = getListener();
					moveInProcessToErrorAndDoPostProcessing(origin, messageWrapper, session, prci, "too many redeliveries or retries");
				}
				numRejected.increment();
				session.setExitState(ExitState.REJECTED, 500);
				return true;
			}
			resetRetryInterval();
			if (isDuplicateAndSkip(getMessageBrowser(ProcessState.DONE), messageId, correlationId)) {
				session.setExitState(ExitState.SUCCESS, 304);
				return true;
			}
			if (cachedProcessResult.isPresent()) {
				numRetried.increment();
			}
		} catch (Exception e) {
			String msg="exception while checking history";
			error(msg, e);
			throw wrapExceptionAsListenerException(e);
		}
		return false;
	}

	private String extractLabel(Message message) {
		if (labelTp != null) {
			try {
				message.preserve();
				return labelTp.transform(message);
			} catch (Exception e) {
				log.warn("{} could not extract label: ({}) {}", this::getLogPrefix, ()-> ClassUtils.nameOf(e), e::getMessage);
			}
		}
		return null;
	}

	private String getBusinessCorrelationId(MessageWrapper<M> messageWrapper, String messageId, PipeLineSession session) {
		String logPrefix = getLogPrefix();
		String businessCorrelationId = session.get(PipeLineSession.CORRELATION_ID_KEY, messageWrapper.getCorrelationId());
		if (correlationIDTp != null) {
			try {
				messageWrapper.getMessage().preserve();
				businessCorrelationId = correlationIDTp.transform(messageWrapper.getMessage());
			} catch (Exception e) {
				log.warn("{} could not extract businessCorrelationId", logPrefix);
			}
			if (StringUtils.isEmpty(businessCorrelationId)) {
				String cidText;
				if (StringUtils.isNotEmpty(getCorrelationIDXPath())) {
					cidText = "xpathExpression ["+getCorrelationIDXPath()+"]";
				} else {
					cidText = "styleSheet ["+getCorrelationIDStyleSheet()+"]";
				}
				if (StringUtils.isNotEmpty(messageWrapper.getCorrelationId())) {
					log.info("{} did not find correlationId using [{}], reverting to correlationId of transfer [{}]", logPrefix, cidText, messageWrapper.getCorrelationId());
					businessCorrelationId = messageWrapper.getCorrelationId();
				}
			}
		}
		if (StringUtils.isEmpty(businessCorrelationId) && StringUtils.isNotEmpty(messageId)) {
			log.info("{} did not find (technical) correlationId, reverting to messageId [{}]", logPrefix, messageId);
			businessCorrelationId = messageId;
		}
		log.info("{} messageId [{}] correlationId [{}] businessCorrelationId [{}]", logPrefix, messageId, messageWrapper.getCorrelationId(), businessCorrelationId);
		return businessCorrelationId;
	}

	private Message compactMessage(Message message, PipeLineSession session) throws IOException, SAXException {
		MessageBuilder msgBuilder = new MessageBuilder();
		CompactSaxHandler handler = new CompactSaxHandler(msgBuilder.asXmlWriter());
		handler.setChompCharSize(getChompCharSize());
		handler.setElementToMove(getElementToMove());
		handler.setElementToMoveChain(getElementToMoveChain());
		handler.setElementToMoveSessionKey(getElementToMoveSessionKey());
		handler.setRemoveCompactMsgNamespaces(isRemoveCompactMsgNamespaces());
		handler.setContext(session);

		XmlUtils.parseXml(message.asInputSource(), handler);
		return msgBuilder.build();
	}

	@SuppressWarnings("synthetic-access")
	public synchronized ProcessResultCacheItem cacheProcessResult(String messageId, String errorMessage, Instant receivedDate) {
		final ProcessResultCacheItem prci = processResultCache.computeIfAbsent(messageId, key -> {
			log.debug("{} caching first result for messageId [{}]", this::getLogPrefix, ()->messageId);
			ProcessResultCacheItem item = new ProcessResultCacheItem();
			item.receiveDate = receivedDate;
			return item;
		});
		prci.receiveCount++;
		if (prci.receiveCount > 1) {
			log.debug("{} increased try count for messageId [{}] to [{}]", this::getLogPrefix, ()->messageId, ()->prci.receiveCount);
		}
		prci.comments = errorMessage;
		processResultCache.put(messageId, prci);
		return prci;
	}

	private synchronized Optional<ProcessResultCacheItem> getCachedProcessResult(String messageId) {
		return Optional.ofNullable(processResultCache.get(messageId));
	}

	public String getCachedErrorMessage(String messageId) {
		Optional<ProcessResultCacheItem> oprci = getCachedProcessResult(messageId);
		return oprci.map(prci -> prci.comments).orElse(null);
	}

	public int getDeliveryCount(RawMessageWrapper<M> rawMessage) {
		IListener<M> origin = getListener(); // N.B. listener is not used when manualRetry==true
		log.debug("{} checking delivery count for messageId [{}]", this::getLogPrefix, rawMessage::getId);
		if (origin instanceof IKnowsDeliveryCount<M> knowsDeliveryCount) {
			return knowsDeliveryCount.getDeliveryCount(rawMessage)-1;
		}
		Optional<ProcessResultCacheItem> oprci = getCachedProcessResult(rawMessage.getId());
		return oprci.map(prci -> prci.receiveCount + 1).orElse(1);
	}

	/*
	 * returns true if message should not be processed
	 */
	private boolean isDeliveryRetryLimitExceeded(String messageId, String correlationId, RawMessageWrapper<M> rawMessageWrapper, Map<String,Object> threadContext, boolean manualRetry) throws ListenerException {
		if (manualRetry) {
			threadContext.put(RETRY_FLAG_SESSION_KEY, "true");
			return isDuplicateAndSkip(getMessageBrowser(ProcessState.DONE), messageId, correlationId);
		}

		final IListener<M> origin = getListener(); // N.B. listener is not used when manualRetry==true
		final String logPrefix = getLogPrefix();
		log.debug("{} checking try count for messageId [{}]", logPrefix, messageId);
		Optional<ProcessResultCacheItem> oprci = getCachedProcessResult(messageId);

		final boolean isProblematic = oprci.map(prci -> {
			if (prci.receiveCount > 1) {
				log.warn("{} message with messageId [{}] has receive count [{}]", logPrefix, messageId, prci.receiveCount);
				threadContext.put(RETRY_FLAG_SESSION_KEY, "true");
			}
			if (getMaxRetries() < 0) return false;
			return prci.receiveCount >= getMaxRetries();
		}).orElseGet(() -> {
			int deliveryCount = getListenerDeliveryCount(rawMessageWrapper, origin);
			if (deliveryCount > 1) {
				log.warn("{} message with messageId [{}] has delivery count [{}]", logPrefix, messageId, deliveryCount);
				threadContext.put(RETRY_FLAG_SESSION_KEY, "true");
			}
			return deliveryCount > getMaxDeliveries();
		});

		if (isProblematic) {
			log.debug("{} message with ID [{}] / correlation ID [{}] has problematic history", logPrefix, messageId, correlationId);
		}
		return isProblematic;
	}

	private int getListenerDeliveryCount(RawMessageWrapper<M> rawMessageWrapper, IListener<M> origin) {
		//noinspection unchecked
		return origin instanceof IKnowsDeliveryCount<?> ? ((IKnowsDeliveryCount<M>) origin).getDeliveryCount(rawMessageWrapper) : -1;
	}

	private void resetProblematicHistory(String messageId) {
		Optional<ProcessResultCacheItem> prci = getCachedProcessResult(messageId);
		prci.ifPresent(processResultCacheItem -> processResultCacheItem.receiveCount = 0);
	}

	/*
	 * returns true if message should not be processed
	 */
	private boolean isDuplicateAndSkip(IMessageBrowser<M> transactionStorage, String messageId, String correlationId) throws ListenerException {
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
				if(numberOfExceptionsCaughtWithoutMessageBeingReceived.incrementAndGet() > numberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold) {
					numberOfExceptionsCaughtWithoutMessageBeingReceivedThresholdReached=true;
					log.warn("number of exceptions caught without message being received threshold is reached; changing the adapter status to 'warning'");
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
	private void registerEvent(String eventCode) {
		if (eventPublisher != null) {
			eventPublisher.registerEvent(this, eventCode);
		}
	}
	protected void throwEvent(String eventCode) {
		throwEvent(eventCode, null);
	}
	private void throwEvent(String eventCode, Message eventMessage) {
		if (eventPublisher != null) {
			eventPublisher.fireEvent(this, eventCode, eventMessage);
		}
	}

	public void resetRetryInterval() {
		log.trace("Reset retry interval - synchronize (lock) on Receiver {}", this::toString);
		synchronized (this) {
			if (suspensionMessagePending) {
				suspensionMessagePending=false;
				throwEvent(RCV_RESUMED_MONITOR_EVENT);
			}
			retryInterval = 1;
		}
		log.trace("Reset retry interval - lock on Receiver {} released", this::toString);
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
			log.info("{}, will continue retrieving messages in [{}] seconds. Details: {}", description, currentInterval, t != null ? t.getMessage() : "NA");
		}
		synchronized (this) {
			if (currentInterval*2 > RCV_SUSPENSION_MESSAGE_THRESHOLD && !suspensionMessagePending) {
				suspensionMessagePending=true;
				throwEvent(RCV_SUSPENDED_MONITOR_EVENT);
			}
		}
		while (isInRunState(RunState.STARTED) && currentInterval-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (Exception e2) {
				error("sleep interrupted", e2);
				stopRunning();
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public boolean isThreadCountReadable() {
		if (getListener() instanceof IThreadCountControllable tcc) {
			return tcc.isThreadCountReadable();
		}
		return getListener() instanceof IPullingListener;
	}
	@Override
	public boolean isThreadCountControllable() {
		if (getListener() instanceof IThreadCountControllable tcc) {
			return tcc.isThreadCountControllable();
		}
		return getListener() instanceof IPullingListener;
	}

	@Override
	public int getCurrentThreadCount() {
		if (getListener() instanceof IThreadCountControllable tcc) {
			return tcc.getCurrentThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			return listenerContainer.getCurrentThreadCount();
		}
		return -1;
	}

	@Override
	public int getMaxThreadCount() {
		if (getListener() instanceof IThreadCountControllable tcc) {
			return tcc.getMaxThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			return listenerContainer.getMaxThreadCount();
		}
		return -1;
	}

	@Override
	public void increaseThreadCount() {
		if (getListener() instanceof IThreadCountControllable tcc) {
			tcc.increaseThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			listenerContainer.increaseThreadCount();
		}
	}

	@Override
	public void decreaseThreadCount() {
		if (getListener() instanceof IThreadCountControllable tcc) {
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
		if(RunState.ERROR == state) {
			log.debug("{} Set RunState to ERROR -> Stop Running", this::getLogPrefix);
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
		RunState currentRunState = runState.getRunState();
		log.trace("Receiver [{}] check if runState=[{}] - current runState=[{}]", name, someRunState, currentRunState);
		return currentRunState==someRunState;
	}

	private String sendResultToSender(Message result) {
		String errorMessage = null;
		try(PipeLineSession session = new PipeLineSession()) {
			log.debug("Receiver [{}] sending result to configured sender [{}]", this::getName, this::getSender);
			getSender().sendMessageOrThrow(result, session); // sending correlated responses via a receiver embedded sender is not supported
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
	public Message formatException(String extraInfo, String messageId, Message message, Throwable t) {
		return getAdapter().formatErrorMessage(extraInfo,t,message,messageId,null,0);
	}



	private ListenerException wrapExceptionAsListenerException(Throwable t) {
		ListenerException l;
		if (t instanceof ListenerException exception) {
			l = exception;
		} else {
			l = new ListenerException(t);
		}
		return l;
	}

	public PullingListenerContainer<M> getListenerContainer() {
		return listenerContainer;
	}

	public void setListenerContainer(PullingListenerContainer<M> listenerContainer) {
		this.listenerContainer = listenerContainer;
	}

	public PullingListenerContainer<M> createListenerContainer() {
		@SuppressWarnings("unchecked")
		PullingListenerContainer<M> plc = applicationContext.getBean("listenerContainer", PullingListenerContainer.class);
		plc.setReceiver(this);
		plc.setMetricsInitializer(configurationMetrics);
		plc.configure();
		return plc;
	}

	protected synchronized DistributionSummary getProcessStatistics(int threadsProcessing) {
		DistributionSummary result;
		try {
			result = processStatistics.get(threadsProcessing);
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}

		if (result==null) {
			while (processStatistics.size()<threadsProcessing+1) {
				int threadNumber = processStatistics.size()+1;
				result = configurationMetrics.createThreadBasedDistributionSummary(this, FrankMeterType.RECEIVER_DURATION, threadNumber);
				processStatistics.add(processStatistics.size(), result);
			}
		}

		return processStatistics.get(threadsProcessing);
	}

	public boolean isOnErrorContinue() {
		return OnError.CONTINUE == getOnError();
	}

	/**
	 * get the number of messages received by this receiver.
	 */
	public double getMessagesReceived() {
		return numReceived == null ? 0 : numReceived.count();
	}

	/**
	 * get the number of duplicate messages received this receiver.
	 */
	public double getMessagesRetried() {
		return numRetried == null ? 0 : numRetried.count();
	}

	/**
	 * Get the number of messages rejected (discarded or put in errorStorage).
	 */
	public double getMessagesRejected() {
		return numRejected == null ? 0 : numRejected.count();
	}

	public long getLastMessageDate() {
		return lastMessageDate;
	}

	public void resetNumberOfExceptionsCaughtWithoutMessageBeingReceived() {
		if(log.isDebugEnabled()) log.debug("resetting [numberOfExceptionsCaughtWithoutMessageBeingReceived] to 0");
		numberOfExceptionsCaughtWithoutMessageBeingReceived.set(0);
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
		if (listener instanceof RunStateEnquiring enquiring)  {
			enquiring.SetRunStateEnquirer(runState);
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
	 * Sender that will send the result in case the PipeLineExit state was not <code>SUCCESS</code>.
	 * Applies if the receiver has an asynchronous listener.
	 */
	public void setErrorSender(ISender errorSender) {
		this.errorSender = errorSender;
		errorSender.setName("errorSender of ["+getName()+"]");
	}

	/** Storage to keep track of messages that failed processing */
	public void setErrorStorage(ITransactionalStorage<Serializable> errorStorage) {
		this.errorStorage = errorStorage;
	}


	/** Storage to keep track of all messages processed correctly */
	public void setMessageLog(ITransactionalStorage<Serializable> messageLog) {
		this.messageLog = messageLog;
	}

	/**
	 * Sets the name of the Receiver, as known to the Adapter.
	 * If the listener implements the {@link INamedObject name} interface and <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 */
	@Override
	public void setName(String newName) {
		name = newName;
		propagateName();
	}

	/**
	 * One of 'continue', 'recover' or 'close'. Controls the behaviour of the Receiver, when it encounters an error during processing of a message.
	 * @ff.default CONTINUE
	 */
	public void setOnError(OnError value) {
		this.onError = value;
	}

	/**
	 * The number of threads that may execute a Pipeline concurrently (only for pulling listeners)
	 * @ff.default 1
	 */
	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}

	/**
	 * The number of threads that are actively polling for messages concurrently. '0' means 'limited only by <code>numthreads</code>' (only for pulling listeners)
	 * @ff.default 1
	 */
	public void setNumThreadsPolling(int i) {
		numThreadsPolling = i;
	}

	/**
	 * The number of seconds waited after an unsuccessful poll attempt, before another poll attempt is made. Only for polling listeners, not for e.g. jms, webservice or javaListeners
	 * @ff.default 10
	 */
	public void setPollInterval(int i) {
		pollInterval = i;
	}

	/**
	 *  timeout (in seconds) to start receiver. If this timeout is exceeded, the Receiver startup is
	 *  aborted and all resources closed and the receiver will be in state {@code EXCEPTION_STARTING}
	 *  and a new start command may be issued again.
	 */
	public void setStartTimeout(int i) {
		startTimeout = i;
	}
	/**
	 *  timeout (in seconds) to stop receiver. If this timeout is exceeded, stopping will be aborted
	 *  and the receiver will be in state {@code EXCEPTION_STOPPING}.
	 *  The receiver will no longer be running but some resources might not have been cleaned up properly.
	 */
	public void setStopTimeout(int i) {
		stopTimeout = i;
	}

	/**
	 * If set to <code>true</code>, each message is checked for presence in the messageLog. If already present, it is not processed again. Only required for non XA compatible messaging. Requires messageLog!
	 * @ff.default false
	 */
	public void setCheckForDuplicates(boolean b) {
		checkForDuplicates = b;
	}

	/**
	 * (Only used when <code>checkForDuplicates=true</code>) Indicates whether the messageid or the correlationid is used for checking presence in the message log
	 * @ff.default MESSAGEID
	 */
	public void setCheckForDuplicatesMethod(CheckForDuplicatesMethod method) {
		checkForDuplicatesMethod=method;
	}

	/**
	 * The maximum delivery count after which to stop processing the message (only for listeners that know the delivery count of received messages). If -1 the delivery count is ignored
	 * @ff.default 5
	 */
	public void setMaxDeliveries(int i) {
		maxDeliveries = i;
	}

	/**
	 * The number of times a processing attempt is automatically retried after an exception is caught or rollback is experienced. If <code>maxRetries &lt; 0</code> the number of attempts is infinite
	 * @ff.default 1
	 */
	public void setMaxRetries(int i) {
		maxRetries = i;
	}

	/**
	 * Size of the cache to keep process results, used by maxRetries
	 * @ff.default 100
	 */
	public void setProcessResultCacheSize(int processResultCacheSize) {
		this.processResultCacheSize = processResultCacheSize;
	}

	@Deprecated(forRemoval = true, since = "7.9.0")
	@ConfigurationWarning("attribute is no longer used. Please use attribute returnedSessionKeys of the JavaListener if the set of sessionsKeys that can be returned to callers session must be limited.")
	public void setReturnedSessionKeys(String string) {
		// no longer used
	}

	/** XPath expression to extract correlationid from message */
	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}

	/** Namespace defintions for correlationIDXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions */
	public void setCorrelationIDNamespaceDefs(String correlationIDNamespaceDefs) {
		this.correlationIDNamespaceDefs = correlationIDNamespaceDefs;
	}

	/** Stylesheet to extract correlationID from message */
	public void setCorrelationIDStyleSheet(String string) {
		correlationIDStyleSheet = string;
	}

	/** XPath expression to extract label from message */
	public void setLabelXPath(String string) {
		labelXPath = string;
	}

	/** Namespace defintions for labelXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions */
	public void setLabelNamespaceDefs(String labelNamespaceDefs) {
		this.labelNamespaceDefs = labelNamespaceDefs;
	}

	/** Stylesheet to extract label from message */
	public void setLabelStyleSheet(String string) {
		labelStyleSheet = string;
	}

	/** If set (>=0) and the character data length inside a xml element exceeds this size, the character data is chomped (with a clear comment) */
	public void setChompCharSize(String string) {
		chompCharSize = string;
	}

	/** If set, the character data in this XML element is stored inside a session key and in the message it is replaced by a reference to this session key: <code>{sessionKey: elementToMoveSessionKey}</code> */
	public void setElementToMove(String string) {
		elementToMove = string;
	}

	/**
	 * (Only used when <code>elementToMove</code> or <code>elementToMoveChain</code> is set) Name of the session key wherein the character data is stored
	 * @ff.default ref_ + the name of the element
	 */
	public void setElementToMoveSessionKey(String string) {
		elementToMoveSessionKey = string;
	}

	/** Like <code>elementToMove</code> but element is preceded with all ancestor elements and separated by semicolons (e.g. adapter;pipeline;pipe) */
	public void setElementToMoveChain(String string) {
		elementToMoveChain = string;
	}

	public void setRemoveCompactMsgNamespaces(boolean b) {
		removeCompactMsgNamespaces = b;
	}

	/** Regular expression to mask strings in the errorStore/logStore and logfiles. Every character between to the strings in this expression will be replaced by a '*'. For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every character between keys &lt;party&gt; and &lt;/party&gt; */
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}

	/**
	 * Only used when hideRegex is not empty
	 * @ff.default all
	 */
	public void setHideMethod(HideMethod hideMethod) {
		this.hideMethod = hideMethod;
	}

	/** Comma separated list of keys of session variables which are available when the <code>PipelineSession</code> is created and of which the value will not be shown in the log (replaced by asterisks) */
	public void setHiddenInputSessionKeys(String string) {
		hiddenInputSessionKeys = string;
	}

	/**
	 * If set to <code>true</code>, every message read will be processed as if it is being retried, by setting a session variable to {@value #RETRY_FLAG_SESSION_KEY}.
	 * @ff.default false
	 */
	public void setForceRetryFlag(boolean b) {
		forceRetryFlag = b;
	}

	/**
	 * Number of connection attempts to put the adapter in warning status
	 * @ff.default 5
	 */
	public void setNumberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold(int number) {
		this.numberOfExceptionsCaughtWithoutMessageBeingReceivedThreshold = number;
	}
}
