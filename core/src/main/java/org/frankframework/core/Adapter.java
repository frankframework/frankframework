/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NamedBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.TaskExecutor;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.AopProxyBeanFactoryPostProcessor;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationAware;
import org.frankframework.configuration.ConfigurationAwareBeanPostProcessor;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.doc.Category;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.errormessageformatters.ErrorMessageFormatter;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.lifecycle.ConfiguringLifecycleProcessor;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.logging.IbisMaskingLayout;
import org.frankframework.receivers.Receiver;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.RunState;
import org.frankframework.util.RunStateManager;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.util.flow.SpringContextFlowDiagramProvider;

/**
 * The Adapter is the central manager in the framework. It has knowledge of both
 * {@link Receiver}s as well as the {@link PipeLine} and statistics.
 * The Adapter is the class that is responsible for configuring, initializing and
 * accessing/activating Receivers, Pipelines, statistics etc.
 * <br/>
 * An Adapter receives a specific type of messages and processes them. It has {@link Receiver Receivers}
 * that receive the messages and a {@link PipeLine} that transforms the incoming messages. Each adapter is part of a {@link Configuration}.
 * <br/>
 * If an adapter can receive its messages through multiple channels (e.g. RESTful HTTP requests, incoming files, etc),
 * each channel appears as a separate {@link Receiver} nested in the adapter. Each {@link Receiver} is also responsible
 * for dealing with
 * the result of its received messages; the result is the output of the {@link PipeLine}. The result
 * consists of the transformed message and a state. The Frank!Framework distinguishes between exit states
 * SUCCESS and ERROR. There is also a state REJECTED for messages that are not accepted by the Frank!Framework
 * and that are not processed by the {@link PipeLine}. If the exit state is ERROR, the result message may
 * not be usable by the calling system. This can be fixed by adding an
 * errorMessageFormatter that formats the result message if the state is ERROR.
 * <br/><br/>
 * Adapters gather statistics about the messages they process.
 * <br/>
 * Adapters can process messages in parallel. They are thread-safe.
 *
 * @author Niels Meijer
 */
@Log4j2
@Category(Category.Type.BASIC)
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class Adapter extends GenericApplicationContext implements ManagableLifecycle, FrankElement, InitializingBean, NamedBean, NameAware {
	protected Logger msgLog = LogUtil.getLogger(LogUtil.MESSAGE_LOGGER);

	public static final String PROCESS_STATE_OK = "OK";
	public static final String PROCESS_STATE_ERROR = "ERROR";

	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private final AppConstants appConstants = AppConstants.getInstance(configurationClassLoader);

	private String name;
	private @Getter String description;
	private Boolean autoStart = null;
	private @Getter boolean replaceNullMessage = false;
	private @Getter int messageKeeperSize = 10; // Default length of MessageKeeper
	private Level msgLogLevel = Level.toLevel(appConstants.getProperty("msg.log.level.default", "INFO"));
	private @Getter boolean msgLogHidden = appConstants.getBoolean("msg.log.hidden.default", true);
	private @Setter @Getter String targetDesignDocument;

	private @Getter Configuration configuration;

	private final ArrayList<Receiver<?>> receivers = new ArrayList<>();
	private long lastMessageDate = 0;
	private @Getter String lastMessageProcessingState; // "OK" or "ERROR"
	private PipeLine pipeline;

	private final Map<String, SenderLastExitState> sendersLastExitState = new HashMap<>();

	private int numOfMessagesInProcess = 0;

	private @Setter MetricsInitializer configurationMetrics;
	private io.micrometer.core.instrument.Counter numOfMessagesProcessed;
	private io.micrometer.core.instrument.Counter numOfMessagesInError;

	private int hourOfLastMessageProcessed=-1;
	private final long[] numOfMessagesStartProcessingByHour = new long[24];

	private DistributionSummary statsMessageProcessingDuration = null;
	private final Object statisticsLock = new Object();

	private long statsUpSince = System.currentTimeMillis();
	private IErrorMessageFormatter errorMessageFormatter;

	private final RunStateManager runState = new RunStateManager();
	private @Getter boolean isConfigured = false;
	private MessageKeeper messageKeeper; // Instantiated in configure()
	private final boolean msgLogHumanReadable = appConstants.getBoolean("msg.log.humanReadable", false);

	private @Getter @Setter TaskExecutor taskExecutor;

	private @Getter String composedHideRegex;
	private @Getter Pattern composedHideRegexPattern;

	private static class SenderLastExitState {
		private final String lastExitState;
		private final long lastExitStateDate;

		public SenderLastExitState(long lastExitStateDate, String lastExitState) {
			this.lastExitStateDate = lastExitStateDate;
			this.lastExitState = lastExitState;
		}
	}

	@Override
	protected void initLifecycleProcessor() {
		ConfiguringLifecycleProcessor defaultProcessor = new ConfiguringLifecycleProcessor();
		defaultProcessor.setBeanFactory(getBeanFactory());
		getBeanFactory().registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, defaultProcessor);
		super.initLifecycleProcessor();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if(!(applicationContext instanceof Configuration config)) {
			throw new IllegalStateException();
		}

		setParent(applicationContext);
		this.configuration = config;
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return this;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isActive()) {
			throw new LifecycleException("unable to refresh, AdapterContext is already active");
		}

		if (getEnvironment().matchesProfiles("aop")) {
			addBeanFactoryPostProcessor(new AopProxyBeanFactoryPostProcessor());
		}

		refresh();

		SpringContextFlowDiagramProvider bean = SpringUtils.createBean(this);
		getBeanFactory().registerSingleton("FlowGenerator", bean);
	}

	/**
	 * Enables the {@link Autowired} annotation and {@link ConfigurationAware} objects.
	 */
	@Override
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		super.registerBeanPostProcessors(beanFactory);

		// Append @Autowired PostProcessor to allow automatic type-based Spring wiring.
		AutowiredAnnotationBeanPostProcessor postProcessor = new AutowiredAnnotationBeanPostProcessor();
		postProcessor.setAutowiredAnnotationType(Autowired.class);
		postProcessor.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(postProcessor);

		if (configuration != null) {
			beanFactory.addBeanPostProcessor(new ConfigurationAwareBeanPostProcessor(configuration));
		}
	}

	/**
	 * Instruct the adapter to configure itself. The adapter will call the pipeline
	 * to configure itself, the pipeline will call the individual pipes to configure
	 * themselves.
	 *
	 * @see org.frankframework.pipes.AbstractPipe#configure()
	 * @see PipeLine#configure()
	 */
	/*
	 * This function is called by Configuration.addAdapter,
	 * to make configuration information available to the Adapter. <br/><br/>
	 * This method also performs
	 * a <code>Pipeline.configurePipes()</code>, as to configure the individual pipes.
	 * @see org.frankframework.core.Pipeline#configurePipes
	 */
	@Override
	@SuppressWarnings("java:S4792") // Changing the logger level is not a security-sensitive operation, because roles originate from the properties file
	public void configure() throws ConfigurationException {
		if (!isActive()) {
			throw new LifecycleException("context is not active");
		}
		if (isConfigured) {
			throw new LifecycleException("already configured");
		}

		if (pipeline == null) {
			String msg = "No pipeline configured for adapter [" + getName() + "]";
			getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
			throw new ConfigurationException(msg);
		}

		LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
		if (!(lifecycle instanceof ConfigurableLifecycle configurableLifecycle)) {
			throw new ConfigurationException("wrong lifecycle processor found, unable to configure beans");
		}

		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(LogUtil.MDC_ADAPTER_KEY, getName())) {
			long startTime = System.currentTimeMillis();
			log.debug("configuring adapter");

			msgLog = LogUtil.getMsgLogger(this);
			Configurator.setLevel(msgLog.getName(), msgLogLevel);

			// Trigger a configure on all (Configurable) Lifecycle beans
			configurableLifecycle.configure();

			numOfMessagesProcessed = configurationMetrics.createCounter(this, FrankMeterType.PIPELINE_PROCESSED);
			numOfMessagesInError = configurationMetrics.createCounter(this, FrankMeterType.PIPELINE_IN_ERROR);
			configurationMetrics.createGauge(this, FrankMeterType.PIPELINE_IN_PROCESS, () -> numOfMessagesInProcess);
			statsMessageProcessingDuration = configurationMetrics.createDistributionSummary(this, FrankMeterType.PIPELINE_DURATION);

			if(!pipeline.configurationSucceeded()) { // only reconfigure pipeline when it hasn't been configured yet!
				try {
					pipeline.configure();
					getMessageKeeper().add("pipeline successfully configured");
				}
				catch (ConfigurationException e) {
					getMessageKeeper().error("error initializing pipeline, " + e.getMessage());
					throw e;
				}
			}

			// Receivers must be configured for the adapter to start up, but they don't need to start
			for (Receiver<?> receiver: receivers) {
				configureReceiver(receiver);
			}

			if (errorMessageFormatter instanceof IConfigurable configurable) {
				configurable.configure();
			}

			log.info("configured adapter in {}", () -> Misc.getDurationInMs(startTime));
		}

		composedHideRegex = computeCombinedHideRegex();
		if (StringUtils.isNotEmpty(composedHideRegex)) {
			composedHideRegexPattern = Pattern.compile(composedHideRegex);
		}
		if(runState.getRunState() == RunState.ERROR) { // if the adapter was previously in state ERROR, after a successful configure, reset it's state
			runState.setRunState(RunState.STOPPED);
		}

		isConfigured = true; // Only if there are no errors mark the adapter as `isConfigured`!
	}

	@Nonnull
	String computeCombinedHideRegex() {
		String combinedHideRegex = pipeline.getPipes().stream()
				.map(IPipe::getHideRegex)
				.filter(StringUtils::isNotEmpty)
				.distinct()
				.collect(Collectors.joining(")|(", "(", ")"));

		if ("()".equals(combinedHideRegex)) {
			return "";
		}
		return combinedHideRegex;
	}

	public void configureReceiver(Receiver<?> receiver) throws ConfigurationException {
		if(receiver.isConfigured()) { // It's possible when an adapter has multiple receivers that the last one fails. The others have already been configured the 2nd time the adapter tries to configure it self
			log.debug("already configured receiver, skipping");
		}

		log.info("Adapter [{}] is initializing receiver [{}]", name, receiver.getName());
		try {
			receiver.configure();
			getMessageKeeper().info(receiver, "successfully configured");
		} catch (ConfigurationException e) {
			getMessageKeeper().error(this, "error initializing " + ClassUtils.nameOf(receiver) + ": " + e.getMessage());
			throw e;
		}
	}

	/**
	 * send a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn("Adapter [{}] {}", name, msg);
		getMessageKeeper().warn(msg);
	}

	/**
	 * send an error to the log and to the messagekeeper of the adapter
	 */
	protected void addErrorMessageToMessageKeeper(String msg, Throwable t) {
		log.error("Adapter [{}] {}", name, msg, t);
		if (!(t instanceof IbisException)) {
			msg += " (" + t.getClass().getName() + ")";
		}
		getMessageKeeper().error(msg + ": " + t.getMessage());
	}


	/**
	 * Increase the number of messages in process
	 */
	private void incNumOfMessagesInProcess(long startTime) {
		log.trace("Increase nr messages in processing, using synchronized statisticsLock [{}]", statisticsLock);
		synchronized (statisticsLock) {
			numOfMessagesInProcess++;
			lastMessageDate = startTime;
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(startTime);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			if (hourOfLastMessageProcessed!=hour) {
				if (hourOfLastMessageProcessed>=0) {
					if (hourOfLastMessageProcessed<hour) {
						for(int i=hourOfLastMessageProcessed+1; i<=hour; i++) {
							numOfMessagesStartProcessingByHour[i]=0;
						}
					} else {
						for(int i=hourOfLastMessageProcessed+1; i<24; i++) {
							numOfMessagesStartProcessingByHour[i]=0;
						}
						for(int i=0; i<=hour; i++) {
							numOfMessagesStartProcessingByHour[i]=0;
						}
					}
				}
				hourOfLastMessageProcessed=hour;
			}
			numOfMessagesStartProcessingByHour[hour]++;
		}
		log.trace("Messages in processing increased, statisticsLock [{}] has been released", statisticsLock);
	}

	/**
	 * Decrease the number of messages in process
	 */
	private void decNumOfMessagesInProcess(long duration, boolean processingSuccess) {
		synchronized (statisticsLock) {
			numOfMessagesInProcess--;
			numOfMessagesProcessed.increment();
			statsMessageProcessingDuration.record(duration);
			if (processingSuccess) {
				lastMessageProcessingState = PROCESS_STATE_OK;
			} else {
				lastMessageProcessingState = PROCESS_STATE_ERROR;
			}
			statisticsLock.notifyAll();
		}
	}
	/**
	 * The number of messages for which processing ended unsuccessfully.
	 */
	public void incNumOfMessagesInError() {
		synchronized (statisticsLock) {
			numOfMessagesInError.increment();
		}
	}

	public void setLastExitState(String pipeName, long lastExitStateDate, String lastExitState) {
		synchronized (sendersLastExitState) {
			sendersLastExitState.put(pipeName, new SenderLastExitState(lastExitStateDate, lastExitState));
		}
	}

	public long getLastExitIsTimeoutDate(String pipeName) {
		synchronized (sendersLastExitState) {
			SenderLastExitState sles = sendersLastExitState.get(pipeName);
			if (sles != null && "timeout".equals(sles.lastExitState)) {
				return sles.lastExitStateDate;
			}
			return 0;
		}
	}

	public Message formatErrorMessage(String errorMessage, Throwable t, Message originalMessage, PipeLineSession session, HasName objectInError) {
		try {
			if (errorMessageFormatter == null) {
				if (getConfiguration().getErrorMessageFormatter() != null) {
					errorMessageFormatter = getConfiguration().getErrorMessageFormatter();
				} else {
					errorMessageFormatter = SpringUtils.createBean(this, ErrorMessageFormatter.class);
					if (errorMessageFormatter instanceof IConfigurable configurable) {
						configurable.configure();
					}
				}
			}
			return errorMessageFormatter.format(errorMessage, t, objectInError, originalMessage, session);
		} catch (Exception e) {
			String msg = "got error while formatting errormessage, original errorMessage [" + errorMessage + "]";
			msg = msg + " from [" + (objectInError == null ? "unknown-null" : objectInError.getName()) + "]";
			addErrorMessageToMessageKeeper(msg, e);

			return new Message(errorMessage);
		}
	}
	/**
	 * retrieve the date and time of the last message.
	 */
	public Date getLastMessageDateDate() {
		Date result = null;
		if (lastMessageDate != 0) {
			result = new Date(lastMessageDate);
		}
		return result;
	}
	/**
	 * the MessageKeeper is for keeping the last <code>messageKeeperSize</code>
	 * messages available, for instance for displaying it in the webcontrol
	 * @see MessageKeeper
	 */
	public synchronized MessageKeeper getMessageKeeper() {
		if (messageKeeper == null)
			messageKeeper = new MessageKeeper(getMessageKeeperSize() < 1 ? 1 : getMessageKeeperSize());
		return messageKeeper;
	}

	/**
	 * The number of messages for which processing ended unsuccessfully.
	 */
	public double getNumOfMessagesInError() {
		log.trace("Get Adapter num messages in error, using synchronized statisticsLock [{}]", statisticsLock);
		try {
			synchronized (statisticsLock) {
				return numOfMessagesInError.count();
			}
		} finally {
			log.trace("Got Adapter num messages in error, statisticsLock [{}] has been released", statisticsLock);
		}
	}
	public int getNumOfMessagesInProcess() {
		log.trace("Get Adapter num messages in process, using synchronized statisticsLock [{}]", statisticsLock);
		try {
			synchronized (statisticsLock) {
				return numOfMessagesInProcess;
			}
		} finally {
			log.trace("Got Adapter num messages in process, statisticsLock [{}] has been released", statisticsLock);
		}
	}

	public long[] getNumOfMessagesStartProcessingByHour() {
		log.trace("Get Adapter hourly statistics, using synchronized statisticsLock [{}]", statisticsLock);
		try {
			synchronized (statisticsLock) { // help, why is this synchronized
				return numOfMessagesStartProcessingByHour;
			}
		} finally {
			log.trace("Got Adapter hourly statistics, statisticsLock [{}] has been released", statisticsLock);
		}
	}
	/**
	 * Total of messages processed
	 * @return long total messages processed
	 */
	public double getNumOfMessagesProcessed() {
		log.trace("Get Adapter number of processed messages, using synchronized statisticsLock [{}]", statisticsLock);
		try {
			synchronized (statisticsLock) {
				return numOfMessagesProcessed.count();
			}
		} finally {
			log.trace("Got Adapter number of processed messages, statisticsLock [{}] has been released", statisticsLock);
		}
	}

	public Receiver<?> getReceiverByName(String receiverName) {
		for (Receiver<?> receiver: receivers) {
			if (receiver.getName().equalsIgnoreCase(receiverName)) {
				return receiver;
			}
		}
		return null;
	}

	public Iterable<Receiver<?>> getReceivers() {
		return receivers;
	}

	@Override
	public RunState getRunState() {
		RunState state = runState.getRunState();
		log.trace("Adapter [{}] runstate: [{}]", name, state);
		return state;
	}

	/**
	 * return the date and time since active
	 * Creation date: (19-02-2003 12:16:53)
	 * @return String  Date
	 */
	public Date getStatsUpSinceDate() {
		return new Date(statsUpSince);
	}

	public void logToMessageLogWithMessageContentsOrSize(Level level, String logMessage, String dataPrefix, Message data) {
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(dataPrefix+".size", getFileSizeAsBytes(data))) {
			if (!isMsgLogHidden()) {
				ctc.put(dataPrefix, data.toString());
			}
			msgLog.log(level, logMessage);
		}
	}

	/**
	 * Direct call to the Adapter PipeLine, foregoing any listeners and receivers. This method
	 * does an amount of setup which is otherwise done by the {@link Receiver}.
	 * <br/>
	 * This method does not throw any exceptions, and will always return a {@link PipeLineResult}. If an
	 * error occurred, the error information is in the {@code PipeLineResult}.
	 *
	 * @param messageId ID of the message
	 * @param message {@link Message} to be processed
	 * @param pipeLineSession {@link PipeLineSession} session in which message is to be processed
	 * @return The {@link PipeLineResult} from processing the message, or indicating what error occurred.
	 */
	public PipeLineResult processMessageDirect(String messageId, Message message, PipeLineSession pipeLineSession) {
		if (StringUtils.isEmpty(messageId)) {
			messageId = MessageUtils.generateFallbackMessageId();
			log.info("messageId not set, creating synthetic id [{}]", messageId);
			pipeLineSession.put(PipeLineSession.MESSAGE_ID_KEY, messageId);
		}
		try (final CloseableThreadContext.Instance ignored = LogUtil.getThreadContext(this, messageId, pipeLineSession);
			IbisMaskingLayout.HideRegexContext ignored2 = IbisMaskingLayout.pushToThreadLocalReplace(composedHideRegexPattern)
		) {
			PipeLineResult result = new PipeLineResult();
			boolean success = false;
			try {
				result = processMessageWithExceptions(messageId, message, pipeLineSession);
				success = true;
			} catch (Throwable t) {
				log.warn("Adapter [{}] error processing message with ID [{}]", name, messageId, t);
				result.setState(ExitState.ERROR);
				String msg = "Illegal exception ["+t.getClass().getName()+"]";
				HasName objectInError = null;
				if (t instanceof ListenerException) {
					Throwable cause = t.getCause();
					if  (cause instanceof PipeRunException pre) {
						msg = "error during pipeline processing";
						objectInError = pre.getPipeInError();
					} else if (cause instanceof ManagedStateException) {
						msg = "illegal state";
						objectInError = this;
					}
				}
				result.setResult(formatErrorMessage(msg, t, message, pipeLineSession, objectInError));
			} finally {
				logToMessageLogWithMessageContentsOrSize(Level.INFO, "Pipeline "+(success ? "Success" : "Error"), "result", result.getResult());
			}
			return result;
		} finally {
			if (ThreadContext.getDepth() == 0) {
				ThreadContext.clearAll();
			}
		}
	}

	/**
	 * This method does the real processing of messages by the adapter. This method is to
	 * be called either from the {@link Receiver}, or from {@link #processMessageDirect(String, Message, PipeLineSession)}.
	 * <br/>
	 * <em>NB: This method expects most setup to already have been done by the caller! LoggingContext and masking of sensitive information
	 * is among things that should be set up by the caller.</em>
	 * <br/>
	 * This method will return a {@link PipeLineResult} with results of the processing. This
	 * might indicate an error if the message could not be processed successfully. If there
	 * was an exception from the {@link PipeLine}, a {@link ListenerException} might be thrown.
	 *
	 * @param messageId ID of the message
	 * @param message {@link Message} to be processed
	 * @param pipeLineSession {@link PipeLineSession} in which the message is to be processed
	 * @return {@link PipeLineResult} with result from processing the message in the {@link PipeLine}.
	 * @throws ListenerException If there was an exception, throws a {@link ListenerException}.
	 */
	public PipeLineResult processMessageWithExceptions(String messageId, Message message, PipeLineSession pipeLineSession) throws ListenerException {
		boolean processingSuccess = true;
		// prevent executing a stopped adapter
		// the receivers should implement this, but you never know....
		RunState currentRunState = getRunState();
		if (currentRunState!=RunState.STARTED && currentRunState!=RunState.STOPPING) {
			String msgAdapterNotOpen = "Adapter [" + getName() + "] in state [" + currentRunState + "], cannot process message";
			throw new ListenerException(new ManagedStateException(msgAdapterNotOpen));
		}

		long startTime = System.currentTimeMillis();
		incNumOfMessagesInProcess(startTime);

		PipeLineResult result = null;
		try {
			String additionalLogging = getAdditionalLogging(pipeLineSession);
			if (msgLog.isDebugEnabled()) {
				logToMessageLogWithMessageContentsOrSize(Level.DEBUG, "Pipeline started" + additionalLogging, "request", message);
			}
			log.info("Adapter [{}] received message with messageId [{}]{}", getName(), messageId, additionalLogging);

			if (Message.isEmpty(message) && isReplaceNullMessage()) {
				log.debug("Adapter [{}] replaces null message with messageId [{}] by empty message", name, messageId);
				message = new Message("");
				message.closeOnCloseOf(pipeLineSession);
			}
			result = pipeline.process(messageId, message, pipeLineSession);
			return result;
		} catch (Throwable t) {
			ListenerException e = new ListenerException(t);

			processingSuccess = false;
			incNumOfMessagesInError();
			warn("error processing message with messageId [" + messageId + "]: " + e.getMessage());
			result = new PipeLineResult();
			result.setState(ExitState.ERROR);
			result.setResult(new Message(e.getMessage()));
			throw e;
		} finally {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			// Reset the InProcess fields, and increase processedMessagesCount
			decNumOfMessagesInProcess(duration, processingSuccess);
			Objects.requireNonNull(result, "'result' should never be NULL here, programming error.");
			ThreadContext.put(LogUtil.MDC_EXIT_STATE_KEY, result.getState().name());
			if (result.getExitCode() != null) {
				ThreadContext.put(LogUtil.MDC_EXIT_CODE_KEY, Integer.toString(result.getExitCode()));
			}
			ThreadContext.put("pipeline.duration", msgLogHumanReadable ? Misc.getAge(startTime) : Long.toString(duration));
			if (log.isDebugEnabled()) {
				log.debug("Adapter: [{}] STAT: Pipeline finished processing message with messageId [{}] exit-state [{}] started {} finished {} total duration: {} ms",
						getName(), messageId, result.getState(),
						DateFormatUtils.format(startTime, DateFormatUtils.FULL_GENERIC_FORMATTER),
						DateFormatUtils.format(endTime, DateFormatUtils.FULL_GENERIC_FORMATTER),
						duration
				);
			} else {
				log.info("Adapter [{}] Pipeline finished processing message with messageId [{}] with exit-state [{}]", getName(), messageId, result.getState());
			}
		}
	}

	private static String getAdditionalLogging(final PipeLineSession pipeLineSession) {
		// xPathLogKeys is an EsbJmsListener thing
		String additionalLogging;
		String xPathLogKeys = (String) pipeLineSession.get("xPathLogKeys");
		if(StringUtils.isNotEmpty(xPathLogKeys)) {
			additionalLogging = StringUtil.splitToStream(xPathLogKeys)
					.map(logName -> logName + " [" + pipeLineSession.get(logName) + "]")
					.collect(Collectors.joining(" and "));
		} else {
			additionalLogging = "";
		}
		return additionalLogging;
	}

	/**
	 * Receives incoming messages. If an adapter can receive messages through multiple channels, then add a receiver for each channel.
	 * @ff.mandatory
	 */
	@SuppressWarnings("java:S3457") // Cast arguments to String before invocation so that we do not have a recursive call to logger when trace-level logging is enabled
	public void addReceiver(Receiver<?> receiver) {
		receivers.add(receiver);
		if (log.isDebugEnabled()) log.debug("Adapter [{}] registered receiver [{}] with properties [{}]", name, receiver.getName(), receiver.toString());
	}

	/**
	 * Set an {@link IErrorMessageFormatter} that will be used to format an error-message when an exception occurs in this adapter.
	 * If not set, then, when an exception occurs, the adapter will first check the {@link Configuration#setErrorMessageFormatter(IErrorMessageFormatter)}
	 * to see if a configuration-wide default error message formatter is set and otherwise create a new instance of {@link ErrorMessageFormatter} as default.
	 *
	 * @see IErrorMessageFormatter for general information on error message formatters.
	 */
	public void setErrorMessageFormatter(IErrorMessageFormatter errorMessageFormatter) {
		this.errorMessageFormatter = errorMessageFormatter;
	}

	/**
	 * The {@link PipeLine}.
	 *
	 * @ff.mandatory
	 */
	public void setPipeLine(PipeLine pipeline) {
		this.pipeline = pipeline;
		log.debug("Adapter [{}] registered pipeline [{}]", name, pipeline);
	}

	public PipeLine getPipeLine() {
		return pipeline;
	}

	/**
	 * Start the adapter. The thread-name will be set to the adapter's name.
	 * The run method, called by t.start(), will call the startRunning method
	 * of the IReceiver. The Adapter will be a new thread, as this interface
	 * extends the <code>Runnable</code> interface. The actual starting is done
	 * in the <code>run</code> method.
	 * @see Receiver#start()
	 */
	@Override
	public void start() {
		switch(getRunState()) {
			case STARTING,
				EXCEPTION_STARTING,
				STARTED,
				STOPPING,
				EXCEPTION_STOPPING:
				log.warn("cannot start adapter [{}] that is stopping, starting or already started", name);
				return;
			default:
				break;
		}

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(LogUtil.MDC_ADAPTER_KEY, getName())) {
					// See also Receiver.startRunning()
					if (!isConfigured) {
						log.error("configuration of adapter [{}] did not succeed, therefore starting the adapter is not possible", name);
						warn("configuration did not succeed. Starting the adapter ["+getName()+"] is not possible");
						runState.setRunState(RunState.ERROR);
						return;
					}
					if (configuration.isUnloadInProgressOrDone()) {
						log.error("configuration of adapter [{}] unload in progress or done, therefore starting the adapter is not possible", name);
						warn("configuration unload in progress or done. Starting the adapter ["+getName()+"] is not possible");
						return;
					}

					log.info("starting adapter");
					log.trace("Start Adapter thread - synchronize (lock) on Adapter runState[{}]", runState);

					synchronized (runState) {
						RunState currentRunState = getRunState();
						if (currentRunState!=RunState.STOPPED) {
							String msg = "currently in state [" + currentRunState + "], ignoring start() command";
							warn(msg);
							return;
						}
						runState.setRunState(RunState.STARTING);
					}
					log.trace("Start Adapter thread - lock released on Adapter runState[{}]", runState);

					// start the pipeline
					try {
						log.debug("Adapter [{}] is starting pipeline", name);
						pipeline.start();
					} catch (LifecycleException lifecycleException) {
						addErrorMessageToMessageKeeper("got error starting PipeLine", lifecycleException);
						runState.setRunState(RunState.ERROR);
						return;
					}

					// Update the adapter uptime.
					statsUpSince = System.currentTimeMillis();

					// as from version 3.0 the adapter is started,
					// regardless of receivers are correctly started.
					// this allows the use of test-pipeline without (running) receivers
					runState.setRunState(RunState.STARTED);
					getMessageKeeper().add("Adapter [" + getName() + "] up and running");
					log.info("Adapter [{}] up and running", name);

					// starting receivers
					for (Receiver<?> receiver: receivers) {
						receiver.start();
					}

					log.trace("Start Adapter thread - finished and completed");
				} catch (Throwable t) {
					addErrorMessageToMessageKeeper("got error starting Adapter", t);
					runState.setRunState(RunState.ERROR);
				}
			}

			@Override
			public String toString() {
				return getName();
			}
		};

		// Since we are catching all exceptions in the thread, the super start will always be called,
		// not a problem for now but something we should look into in the future...
		CompletableFuture.runAsync(runnable, taskExecutor) // Start all smart-lifecycles
				.thenRun(super::start); // Then start the adapter it self
	}

	@Override
	public int getPhase() {
		return 100;
	}

	@Override
	public boolean isRunning() {
		return runState.getRunState() == RunState.STARTED && super.isRunning();
	}

	/**
	 * Stop the <code>Adapter</code> and close all elements like receivers,
	 * Pipeline, pipes etc.
	 * The adapter will call the <code>IReceiver</code> to <code>stopListening</code>
	 * <p>Also the {@link PipeLine#stop} method will be called, closing all registered pipes. </p>
	 *
	 * @see Receiver#stop()
	 * @see PipeLine#stop
	 */
	@Override
	public void stop(@Nonnull Runnable callback) {
		Objects.requireNonNull(callback, "callback may not be null");

		log.info("Stopping Adapter named [{}] with {} receivers", this::getName, receivers::size);
		Runnable runnable = new Runnable() {
			// Cannot use a closable ThreadContext as it's cleared in the Receiver STOP method.
			@Override
			public void run() {
				try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(LogUtil.MDC_ADAPTER_KEY, getName())) {
					log.trace("Adapter.stopRunning - stop adapter thread for [{}] starting", () -> getName());

					// See also Receiver.stopRunning()
					switch(getRunState()) {
						case STARTING:
						case STOPPING:
						case STOPPED:
							if (log.isWarnEnabled()) log.warn("adapter [{}] currently in state [{}], ignoring stop() command", getName(), getRunState());
							return;
						default:
							break;
					}
					runState.setRunState(RunState.STOPPING);
					log.debug("Adapter [{}] is stopping receivers", name);
					for (Receiver<?> receiver: receivers) {
						// Will not stop receivers that are in state "STARTING"
						log.debug("Adapter.stopRunning: Stopping receiver [{}] in state [{}]", receiver::getName, receiver::getRunState);
						receiver.stop();
					}
					// IPullingListeners might still be running, see also
					// comment in method Receiver.tellResourcesToStop()
					for (Receiver<?> receiver: receivers) {
						if(receiver.getRunState() == RunState.ERROR) {
							continue; // We don't need to stop the receiver as it's already stopped...
						}
						long sleepDelay = 25L;
						while (!receiver.getRunState().isStopped()) {
							if (receiver.getRunState() == RunState.STARTED || receiver.getRunState() == RunState.EXCEPTION_STARTING) {
								log.debug("Adapter [{}] stopping receiver [{}] which was still starting when stop() command was received", ()->name, receiver::getName);
								receiver.stop();
							}
							log.debug("Adapter [{}] waiting for receiver [{}] in state [{}] to stop", ()->name, receiver::getName, receiver::getRunState);
							try {
								Thread.sleep(sleepDelay);
								if (sleepDelay < 1000L) sleepDelay = sleepDelay * 2L;
							} catch (InterruptedException e) {
								if (log.isWarnEnabled()) log.warn("Interrupted waiting for threads of receiver [{}] to end", receiver.getName(), e);
								Thread.currentThread().interrupt();
							}
						}
						log.info("Adapter [{}] stopped receiver [{}] {}.", name, receiver.getName(), receiver.isInRunState(RunState.EXCEPTION_STOPPING) ? "with error" : "successfully");
					}
					int currentNumOfMessagesInProcess = getNumOfMessagesInProcess();
					if (currentNumOfMessagesInProcess > 0) {
						String msg = "Adapter [" + name + "] is being stopped while still processing " + currentNumOfMessagesInProcess + " messages, waiting for them to finish";
						warn(msg);
					}
					waitForNoMessagesInProcess();
					log.debug("Adapter [{}] is stopping pipeline", name);
					pipeline.stop();
					// Set the adapter uptime to 0 as the adapter is stopped.
					statsUpSince = 0;
					runState.setRunState(RunState.STOPPED);
					getMessageKeeper().add("Adapter [" + name + "] stopped");
					log.debug("Adapter [{}] now in state STOPPED", name);
				} catch (Throwable t) {
					addErrorMessageToMessageKeeper("got error stopping Adapter", t);
					runState.setRunState(RunState.ERROR);
					log.warn("Adapter [{}] in state ERROR", name, t);
				} finally {
					log.trace("Adapter.stop - stop adapter thread for Adapter [{}] finished and completed", name);
				}
			}

			@Override
			public String toString() {
				return getName();
			}
		};

		CompletableFuture.runAsync(runnable, taskExecutor) // Stop asynchronous from other adapters
				.thenRun(super::stop) // Stop other LifeCycle aware beans
				.thenRun(callback); // Call the callback 'CountDownLatch' to confirm we've stopped
	}

	/**
	 * This method should ideally not be called directly.
	 * Since this is a {@link SmartLifecycle} the {@link #stop(Runnable)} must be called instead.
	 * Delegates to {@link #stop(Runnable)} which calls `super.stop()`.
	 */
	@Override
	public void stop() {
		stop(() -> log.info("stopped adapter [{}]", getName()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(" ");
		sb.append("[receivers=");
		for (Receiver<?> receiver: receivers) {
			sb.append(" ").append(receiver.getName());
		}
		sb.append("]");
		sb.append("[pipeLine=").append(pipeline != null ? pipeline.toString() : "none registered").append("][started=").append(getRunState()).append("]");
		return sb.toString();
	}

	private String getFileSizeAsBytes(Message message) {
		if (Message.isEmpty(message)) {
			return null;
		}
		if(message.size() == -1) {
			return "unknown";
		}

		return Misc.toFileSize(message.size());
	}

	public void waitForNoMessagesInProcess() throws InterruptedException {
		log.trace("Wait until no messages in process - synchronize (lock) on statsMessageProcessingDuration {}", statsMessageProcessingDuration);
		synchronized (statisticsLock) {
			while (getNumOfMessagesInProcess() > 0) {
				statisticsLock.wait(); // waits for notification from decNumOfMessagesInProcess()
			}
		}
		log.trace("No more messages in process - lock released on statsMessageProcessingDuration {}", statsMessageProcessingDuration);
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.NamedBean#getBeanName()
	 */
	@Override
	public String getBeanName() {
		return name;
	}

	/**
	 * name of the adapter
	 * @ff.mandatory
	 */
	@Override
	public void setName(String name) {
		if(name.contains("/")) {
			throw new IllegalStateException("It is not allowed to have '/' in adapter name ["+name+"]");
		}

		setDisplayName("AdapterContext [" + name + "]");
		this.name = name;
		setId(name);
	}
	@Override
	public String getName() {
		return getId();
	}

	/** some functional description of the <code>Adapter</code> */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * AutoStart indicates that the adapter should be started when the configuration
	 * is started.
	 * @ff.default <code>true</code>
	 */
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	@Override
	public boolean isAutoStartup() {
		if (!isConfigured) return false; // Don't startup until configured

		if (autoStart == null && getClassLoader() != null) {
			autoStart = AppConstants.getInstance(getClassLoader()).getBoolean("adapters.autoStart", true);
		}
		return autoStart;
	}

	/**
	 * If <code>true</code> a null message is replaced by an empty message
	 * @ff.default <code>false</code>
	 */
	public void setReplaceNullMessage(boolean b) {
		replaceNullMessage = b;
	}

	/**
	 * number of message displayed in ibisconsole
	 * @ff.default 10
	 */
	public void setMessageKeeperSize(int size) {
		this.messageKeeperSize = size;
	}

	private enum MessageLogLevel {
		/** No logging */
		OFF(Level.OFF),
		/** Logs information from adapter level messages */
		INFO(Level.INFO),
		/** Same as INFO */
		@Deprecated
		BASIC(Level.INFO),
		/** Logs information from pipe messages */
		DEBUG(Level.DEBUG),
		/** Same as DEBUG */
		@Deprecated
		TERSE(Level.DEBUG);

		private final @Getter Level effectiveLevel;

		MessageLogLevel(Level effectiveLevel) {
			this.effectiveLevel = effectiveLevel;
		}
	}

	/**
	 * Defines behaviour for logging messages. Configuration is done in the MSG appender in log4j4ibis.properties.
	 * @ff.default <code>INFO</code>, unless overridden by property <code>msg.log.level.default</code>
	 */
	public void setMsgLogLevel(MessageLogLevel level) {
		msgLogLevel = level.getEffectiveLevel();
	}

	@Deprecated
	public void setRequestReplyLogging(boolean requestReplyLogging) {
		if (requestReplyLogging) {
			ConfigurationWarnings.add(this, log, "implementing setting of requestReplyLogging=true as msgLogLevel=DEBUG");
			msgLogLevel = Level.DEBUG;
		} else {
			ConfigurationWarnings.add(this, log, "implementing setting of requestReplyLogging=false as msgLogLevel=OFF");
			msgLogLevel = Level.OFF;
		}
	}

	/**
	 * If set to <code>true</code>, the length of the message is shown in the msg log instead of the content of the message
	 * @ff.default <code>false</code>
	 */
	public void setMsgLogHidden(boolean b) {
		msgLogHidden = b;
	}
}
