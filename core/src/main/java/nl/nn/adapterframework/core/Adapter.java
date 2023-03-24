/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.beans.factory.NamedBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.cache.ICache;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.jmx.JmxAttribute;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.statistics.CounterStatistic;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.RunStateManager;

/**
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
 * @author Johan Verrips
 */
@Category("Basic")
public class Adapter implements IAdapter, NamedBean {
	private @Getter @Setter ApplicationContext applicationContext;

	private Logger log = LogUtil.getLogger(this);
	protected Logger msgLog = LogUtil.getLogger(LogUtil.MESSAGE_LOGGER);

	public static final String PROCESS_STATE_OK = "OK";
	public static final String PROCESS_STATE_ERROR = "ERROR";

	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private AppConstants APP_CONSTANTS = AppConstants.getInstance(configurationClassLoader);

	private String name;
	private @Getter String description;
	private @Getter boolean autoStart = APP_CONSTANTS.getBoolean("adapters.autoStart", true);
	private @Getter boolean replaceNullMessage = false;
	private @Getter int messageKeeperSize = 10; //default length of MessageKeeper
	private Level msgLogLevel = Level.toLevel(APP_CONSTANTS.getProperty("msg.log.level.default", "INFO"));
	private @Getter boolean msgLogHidden = APP_CONSTANTS.getBoolean("msg.log.hidden.default", true);
	private @Getter String targetDesignDocument;

	private @Getter Configuration configuration;

	private ArrayList<Receiver<?>> receivers = new ArrayList<>();
	private long lastMessageDate = 0;
	private @Getter String lastMessageProcessingState; //"OK" or "ERROR"
	private PipeLine pipeline;

	private Map<String, SenderLastExitState> sendersLastExitState = new HashMap<String, SenderLastExitState>();

	private class SenderLastExitState {
		private String lastExitState = null;
		private long lastExitStateDate = 0;

		public SenderLastExitState (long lastExitStateDate, String lastExitState) {
			this.lastExitStateDate = lastExitStateDate;
			this.lastExitState = lastExitState;
		}
	}

	private int numOfMessagesInProcess = 0;

	private CounterStatistic numOfMessagesProcessed = new CounterStatistic(0);
	private CounterStatistic numOfMessagesInError = new CounterStatistic(0);

	private int hourOfLastMessageProcessed=-1;
	private long[] numOfMessagesStartProcessingByHour = new long[24];

	private StatisticsKeeper statsMessageProcessingDuration = null;

	private long statsUpSince = System.currentTimeMillis();
	private IErrorMessageFormatter errorMessageFormatter;

	private final RunStateManager runState = new RunStateManager();
	private @Getter boolean configurationSucceeded = false;
	private MessageKeeper messageKeeper; //instantiated in configure()
	private boolean msgLogHumanReadable = APP_CONSTANTS.getBoolean("msg.log.humanReadable", false);


	private @Getter @Setter TaskExecutor taskExecutor;

	private String composedHideRegex;


	/*
	 * This function is called by Configuration.registerAdapter,
	 * to make configuration information available to the Adapter. <br/><br/>
	 * This method also performs
	 * a <code>Pipeline.configurePipes()</code>, as to configure the individual pipes.
	 * @see nl.nn.adapterframework.core.Pipeline#configurePipes
	 */
	@Override
	public void configure() throws ConfigurationException { //TODO check if we should fail when the adapter has already been configured?
		msgLog = LogUtil.getMsgLogger(this);
		Configurator.setLevel(msgLog.getName(), msgLogLevel);
		configurationSucceeded = false;
		log.debug("configuring adapter [{}]", name);
		if(getName().contains("/")) {
			throw new ConfigurationException("It is not allowed to have '/' in adapter name ["+getName()+"]");
		}

		statsMessageProcessingDuration = new StatisticsKeeper(getName());
		if (pipeline == null) {
			String msg = "No pipeline configured for adapter [" + getName() + "]";
			getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
			throw new ConfigurationException(msg);
		}

		if(!pipeline.configurationSucceeded()) { // only reconfigure pipeline when it hasn't been configured yet!
			try {
				pipeline.setAdapter(this);
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

		List<String> hrs = new ArrayList<>();
		for (IPipe pipe : pipeline.getPipes()) {
			if (pipe instanceof AbstractPipe) {
				AbstractPipe aPipe = (AbstractPipe) pipe;
				if (StringUtils.isNotEmpty(aPipe.getHideRegex())) {
					if (!hrs.contains(aPipe.getHideRegex())) {
						hrs.add(aPipe.getHideRegex());
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String hr : hrs) {
			if (sb.length() > 0) {
				sb.append("|");
			}
			sb.append("(");
			sb.append(hr);
			sb.append(")");
		}
		if (sb.length() > 0) {
			composedHideRegex = sb.toString();
		}

		if(runState.getRunState()==RunState.ERROR) { // if the adapter was previously in state ERROR, after a successful configure, reset it's state
			runState.setRunState(RunState.STOPPED);
		}

		configurationSucceeded = true; //Only if there are no errors mark the adapter as `configurationSucceeded`!
	}

	public void configureReceiver(Receiver<?> receiver) throws ConfigurationException {
		if(receiver.configurationSucceeded()) { //It's possible when an adapter has multiple receivers that the last one fails. The others have already been configured the 2nd time the adapter tries to configure it self
			log.debug("already configured receiver, skipping");
		}

		log.info("Adapter [{}] is initializing receiver [{}]", name, receiver.getName());
		receiver.setAdapter(this);
		try {
			receiver.configure();
			getMessageKeeper().info(receiver, "successfully configured");
		} catch (ConfigurationException e) {
			getMessageKeeper().error(this, "error initializing " + ClassUtils.nameOf(receiver) + ": " + e.getMessage());
			throw e;
		}
	}

	public boolean configurationSucceeded() {
		return configurationSucceeded;
	}

	/**
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn("Adapter [{}] {}", name, msg);
		getMessageKeeper().warn(msg);
	}

	/**
	 * sends a warning to the log and to the messagekeeper of the adapter
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
		log.trace("Increase nr messages in processing, synchronize (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		synchronized (statsMessageProcessingDuration) {
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
		log.trace("Messages in processing increased, lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
	}
	/**
	 * Decrease the number of messages in process
	 */
	private void decNumOfMessagesInProcess(long duration, boolean processingSuccess) {
		log.trace("Decrease nr messages in processing, synchronize (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		synchronized (statsMessageProcessingDuration) {
			numOfMessagesInProcess--;
			log.trace("Increase nr messages processed, synchronize (lock) on numOfMessagesInProcess[{}]", numOfMessagesProcessed);
			numOfMessagesProcessed.increase();
			log.trace("Nr messages processed increased, lock released on numOfMessagesProcessed[{}]", numOfMessagesProcessed);
			statsMessageProcessingDuration.addValue(duration);
			if (processingSuccess) {
				lastMessageProcessingState = PROCESS_STATE_OK;
			} else {
				lastMessageProcessingState = PROCESS_STATE_ERROR;
			}
			statsMessageProcessingDuration.notifyAll();
		}
		log.trace("Messages in processing decreased, lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
	}
	/**
	 * The number of messages for which processing ended unsuccessfully.
	 */
	private void incNumOfMessagesInError() {
		log.trace("Increase nr messages in error, synchronize (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		synchronized (statsMessageProcessingDuration) {
			log.trace("(nested) Increase nr messages in error, synchronize (lock) on numOfMessagesInError[{}]", numOfMessagesInError);
			numOfMessagesInError.increase();
			log.trace("(nested) Messages in error increased, lock released on numOfMessagesInError[{}]", numOfMessagesInError);
		}
		log.trace("Messages in error increased, lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
	}

	public void setLastExitState(String pipeName, long lastExitStateDate, String lastExitState) {
		log.trace("Set last exit state, synchronize (lock) on sendersLastExitState[{}]", sendersLastExitState);
		synchronized (sendersLastExitState) {
			sendersLastExitState.put(pipeName, new SenderLastExitState(lastExitStateDate, lastExitState));
		}
		log.trace("Last exit state set, lock released on sendersLastExitState[{}]", sendersLastExitState);
	}

	public long getLastExitIsTimeoutDate(String pipeName) {
		log.trace("Get last exit state, synchronize (lock) on sendersLastExitState[{}]", sendersLastExitState);
		try {
			synchronized (sendersLastExitState) {
				SenderLastExitState sles = sendersLastExitState.get(pipeName);
				if (sles != null && "timeout".equals(sles.lastExitState)) {
					return sles.lastExitStateDate;
				}
				return 0;
			}
		} finally {
			log.trace("Got Last exit state, lock released on sendersLastExitState[{}]", sendersLastExitState);
		}
	}

	@Override
	public Message formatErrorMessage(String errorMessage, Throwable t, Message originalMessage, String messageID, INamedObject objectInError, long receivedTime) {
		if (errorMessageFormatter == null) {
			errorMessageFormatter = new ErrorMessageFormatter();
		}
		try {
			return errorMessageFormatter.format(errorMessage, t, objectInError, originalMessage, messageID, receivedTime);
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
	@JmxAttribute(description = "The date/time of the last processed message")
	public String getLastMessageDate() {
		return getLastMessageDate(DateUtils.FORMAT_FULL_GENERIC);
	}
	public String getLastMessageDate(String dateFormat) {
		String result;
		if (lastMessageDate != 0)
			result = DateUtils.format(new Date(lastMessageDate), dateFormat);
		else
			result = "-";
		return result;
	}
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
	@Override
	public synchronized MessageKeeper getMessageKeeper() {
		if (messageKeeper == null)
			messageKeeper = new MessageKeeper(getMessageKeeperSize() < 1 ? 1 : getMessageKeeperSize());
		return messageKeeper;
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		Object adapterData=hski.openGroup(data,getName(),"adapter");
		hski.handleScalar(adapterData,"upSince", getStatsUpSinceDate());
		hski.handleScalar(adapterData,"lastMessageDate", getLastMessageDateDate());

		if (action!=Action.FULL &&
			action!=Action.SUMMARY) {
			log.trace("Iterating over statistics with lock - synchronize (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
			synchronized (statsMessageProcessingDuration) {
				iterateOverStatisticsBody(hski,adapterData,action);
			}
			log.trace("Iterated over statistics - lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		} else {
			iterateOverStatisticsBody(hski,adapterData,action);
		}
		hski.closeGroup(adapterData);
	}

	private void iterateOverStatisticsBody(StatisticsKeeperIterationHandler hski, Object adapterData, Action action) throws SenderException {
		Object pipelineData=hski.openGroup(adapterData,null,"pipeline");
		hski.handleScalar(pipelineData,"messagesInProcess", getNumOfMessagesInProcess());
		hski.handleScalar(pipelineData,"messagesProcessed", numOfMessagesProcessed);
		hski.handleScalar(pipelineData,"messagesInError", numOfMessagesInError);
		log.trace("Get Adapter messagesProcessedThisInterval, synchronize (lock) on numOfMessagesProcessed[{}]", numOfMessagesProcessed);
		hski.handleScalar(pipelineData,"messagesProcessedThisInterval", numOfMessagesProcessed.getIntervalValue());
		log.trace("Got Adapter messagesProcessedThisInterval, lock released on numOfMessagesProcessed[{}]", numOfMessagesProcessed);

		log.trace("Get Adapter messagesInErrorThisInterval, synchronize (lock) on numOfMessagesInError[{}]", numOfMessagesInError);
		hski.handleScalar(pipelineData,"messagesInErrorThisInterval", numOfMessagesInError.getIntervalValue());
		log.trace("Got Adapter messagesInErrorThisInterval, lock released on numOfMessagesInError[{}]", numOfMessagesInError);

		Object durationStatsData = hski.openGroup(pipelineData, null, "duration");
		hski.handleStatisticsKeeper(durationStatsData, statsMessageProcessingDuration);
		hski.closeGroup(durationStatsData);
		statsMessageProcessingDuration.performAction(action);

		Object hourData=hski.openGroup(pipelineData,getName(),"processing by hour");
		for (int i=0; i<getNumOfMessagesStartProcessingByHour().length; i++) {
			String startTime;
			if (i<10) {
				startTime = "0" + i + ":00";
			} else {
				startTime = i + ":00";
			}
			hski.handleScalar(hourData, startTime, getNumOfMessagesStartProcessingByHour()[i]);
		}
		hski.closeGroup(hourData);

		hski.closeGroup(pipelineData);

		if (action == Action.FULL || action == Action.MARK_FULL) {
			Object recsData=hski.openGroup(adapterData,null,"receiver");
			for (Receiver<?> receiver: receivers) {
				receiver.iterateOverStatistics(hski,recsData,action);
			}
			hski.closeGroup(recsData);

			ICache<String,String> cache=pipeline.getCache();
			if (cache instanceof HasStatistics) {
				((HasStatistics) cache).iterateOverStatistics(hski, recsData, action);
			}

			Object pipeData=hski.openGroup(adapterData,null,"pipe");
			getPipeLine().iterateOverStatistics(hski, pipeData, action);
			hski.closeGroup(pipeData);
		}
	}


	/**
	 * The number of messages for which processing ended unsuccessfully.
	 */
	@JmxAttribute(description = "# Messages in Error")
	public long getNumOfMessagesInError() {
		log.trace("Get Adapter num messages in error - synchronized (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		try {
			synchronized (statsMessageProcessingDuration) {
				return numOfMessagesInError.getValue();
			}
		} finally {
			log.trace("Got Adapter num messages in error - lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		}
	}
	@JmxAttribute(description = "# Messages in process")
	public int getNumOfMessagesInProcess() {
		log.trace("Get Adapter num messages in process - synchronized (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		try {
			synchronized (statsMessageProcessingDuration) {
				return numOfMessagesInProcess;
			}
		} finally {
			log.trace("Got Adapter num messages in process - lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		}
	}

	public long[] getNumOfMessagesStartProcessingByHour() {
		log.trace("Get Adapter num messages in start processing by hour - synchronized (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		try {
			synchronized (statsMessageProcessingDuration) {
				return numOfMessagesStartProcessingByHour;
			}
		} finally {
			log.trace("Got Adapter num messages in start processing by hour - lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		}
	}
	/**
	 * Total of messages processed
	 * @return long total messages processed
	 */
	@JmxAttribute(description = "# Messages Processed")
	public long getNumOfMessagesProcessed() {
		log.trace("Get Adapter num messages processed - synchronized (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		try {
			synchronized (statsMessageProcessingDuration) {
				return numOfMessagesProcessed.getValue();
			}
		} finally {
			log.trace("Got Adapter num messages processed - lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		}
	}

	@Override
	public Receiver<?> getReceiverByName(String receiverName) {
		for (Receiver<?> receiver: receivers) {
			if (receiver.getName().equalsIgnoreCase(receiverName)) {
				return receiver;
			}
		}
		return null;
	}

	public Receiver<?> getReceiverByNameAndListener(String receiverName, Class<?> listenerClass) {
		if (listenerClass == null) {
			return getReceiverByName(receiverName);
		}
		for (Receiver<?> receiver: receivers) {
			if (receiver.getName().equalsIgnoreCase(receiverName) && listenerClass.equals(receiver.getListener().getClass())) {
				return receiver;
			}
		}
		return null;
	}

	@Override
	public Iterable<Receiver<?>> getReceivers() {
		return receivers;
	}

	@Override
	public RunState getRunState() {
		RunState state = runState.getRunState();
		log.trace("Adapter [{}] runstate: [{}]", name, state);
		return state;
	}

	@JmxAttribute(description = "RunState")
	public String getRunStateAsString() {
		return getRunState().toString();
	}

	/**
	 * Return the total processing duration as a StatisticsKeeper
	 * @see StatisticsKeeper
	 * @return nl.nn.adapterframework.statistics.StatisticsKeeper
	 */
	public StatisticsKeeper getStatsMessageProcessingDuration() {
		return statsMessageProcessingDuration;
	}
	/**
	 * return the date and time since active
	 * Creation date: (19-02-2003 12:16:53)
	 * @return String  Date
	 */
	@JmxAttribute(description = "Up Since")
	public String getStatsUpSince() {
		return getStatsUpSince(DateUtils.FORMAT_FULL_GENERIC);
	}
	public String getStatsUpSince(String dateFormat) {
		return DateUtils.format(new Date(statsUpSince), dateFormat);
	}
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

	@Override
	public PipeLineResult processMessage(String messageId, Message message, PipeLineSession pipeLineSession) {
		long startTime = System.currentTimeMillis();
		try {
			try (final CloseableThreadContext.Instance ctc = LogUtil.getThreadContext(this, messageId, pipeLineSession)) {
				PipeLineResult result = new PipeLineResult();
				boolean success = false;
				try {
					result = processMessageWithExceptions(messageId, message, pipeLineSession);
					success = true;
				} catch (Throwable t) {
					result.setState(ExitState.ERROR);
					String msg = "Illegal exception ["+t.getClass().getName()+"]";
					INamedObject objectInError = null;
					if (t instanceof ListenerException) {
						Throwable cause = ((ListenerException) t).getCause();
						if  (cause instanceof PipeRunException) {
							PipeRunException pre = (PipeRunException) cause;
							msg = "error during pipeline processing";
							objectInError = pre.getPipeInError();
						} else if (cause instanceof ManagedStateException) {
							msg = "illegal state";
							objectInError = this;
						}
					}
					result.setResult(formatErrorMessage(msg, t, message, messageId, objectInError, startTime));
				} finally {
					logToMessageLogWithMessageContentsOrSize(Level.INFO, "Pipeline "+(success ? "Success" : "Error"), "result", result.getResult());
				}
				return result;
			}
		} finally {
			if (ThreadContext.getDepth() == 0) {
				ThreadContext.clearAll();
			}
		}
	}

	@Override
	public PipeLineResult processMessageWithExceptions(String messageId, Message message, PipeLineSession pipeLineSession) throws ListenerException {

		PipeLineResult result = null;

		long startTime = System.currentTimeMillis();
		boolean processingSuccess = true;
		// prevent executing a stopped adapter
		// the receivers should implement this, but you never now....
		RunState currentRunState = getRunState();
		if (currentRunState!=RunState.STARTED && currentRunState!=RunState.STOPPING) {

			String msgAdapterNotOpen = "Adapter [" + getName() + "] in state [" + currentRunState + "], cannot process message";
			throw new ListenerException(new ManagedStateException(msgAdapterNotOpen));
		}

		incNumOfMessagesInProcess(startTime);

		try {
			if (StringUtils.isNotEmpty(composedHideRegex)) {
				IbisMaskingLayout.addToThreadLocalReplace(composedHideRegex);
			}

			StringBuilder additionalLogging = new StringBuilder();

			// xPathLogKeys is an EsbJmsListener thing
			String xPathLogKeys = (String) pipeLineSession.get("xPathLogKeys");
			if(StringUtils.isNotEmpty(xPathLogKeys)) {
				StringTokenizer tokenizer = new StringTokenizer(xPathLogKeys, ",");
				while (tokenizer.hasMoreTokens()) {
					String logName = tokenizer.nextToken();
					String xPathResult = (String) pipeLineSession.get(logName);
					additionalLogging.append(" and ");
					additionalLogging.append(logName);
					additionalLogging.append(" [" + xPathResult + "]");
				}
			}

			if(msgLog.isDebugEnabled()) {
				logToMessageLogWithMessageContentsOrSize(Level.DEBUG, "Pipeline started"+additionalLogging, "request", message);
			}
			log.info("Adapter [{}] received message with messageId [{}]{}", getName(), messageId, additionalLogging);

			if (Message.isEmpty(message) && isReplaceNullMessage()) {
				log.debug("Adapter [{}] replaces null message with messageId [{}] by empty message", name, messageId);
				message = new Message("");
			}
			result = pipeline.process(messageId, message, pipeLineSession);
			return result;

		} catch (Throwable t) {
			ListenerException e;
			if (t instanceof ListenerException) {
				e = (ListenerException) t;
			} else {
				e = new ListenerException(t);
			}
			processingSuccess = false;
			incNumOfMessagesInError();
			addErrorMessageToMessageKeeper("error processing message with messageId [" + messageId+"]: ",e);
			result = new PipeLineResult();
			result.setState(ExitState.ERROR);
			result.setResult(new Message(e.getMessage()));
			throw e;
		} finally {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			//reset the InProcess fields, and increase processedMessagesCount
			decNumOfMessagesInProcess(duration, processingSuccess);
			ThreadContext.put(PipeLineSession.EXIT_STATE_CONTEXT_KEY, result.getState().name());
			if (result.getExitCode()!=0) {
				ThreadContext.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, Integer.toString(result.getExitCode()));
			}
			ThreadContext.put("pipeline.duration", msgLogHumanReadable ? Misc.getAge(startTime) : Long.toString(duration));
			if (log.isDebugEnabled()) {
				log.debug("Adapter: [{}] STAT: Pipeline finished processing message with messageId [{}] exit-state [{}] started {} finished {} total duration: {} ms",
						getName(), messageId, result.getState(),
						DateUtils.format(new Date(startTime), DateUtils.FORMAT_FULL_GENERIC),
						DateUtils.format(new Date(endTime), DateUtils.FORMAT_FULL_GENERIC),
						duration);
			} else {
				log.info("Adapter [{}] Pipeline finished processing message with messageId [{}] with exit-state [{}]", getName(), messageId, result.getState());
			}

			IbisMaskingLayout.removeThreadLocalReplace();
		}
	}

	// technically, a Receiver is not mandatory, but no useful adapter can do without it.
	/**
	 * Receives incoming messages. If an adapter can receive messages through multiple channels,
	 * then add a receiver for each channel.
	 * @ff.mandatory
	 */
	public void registerReceiver(Receiver<?> receiver) {
		receivers.add(receiver);
		// Cast arguments to String before invocation so that we do not have recursive call to logger when trace-level logging is enabled
		if (log.isDebugEnabled()) log.debug("Adapter [{}] registered receiver [{}] with properties [{}]", name, receiver.getName(), receiver.toString());
	}

	/**
	 * Formatter for errors that can occur in this adapter.
	 */
	public void setErrorMessageFormatter(IErrorMessageFormatter errorMessageFormatter) {
		this.errorMessageFormatter = errorMessageFormatter;
	}

	/**
	 * The {@link PipeLine}.
	 *
	 * @ff.mandatory
	 */
	@Override
	public void setPipeLine(PipeLine pipeline) throws ConfigurationException {
		this.pipeline = pipeline;
		pipeline.setAdapter(this);
		log.debug("Adapter [{}] registered pipeline [{}]", name, pipeline);
	}
	@Override
	public PipeLine getPipeLine() {
		return pipeline;
	}

	/**
	 * the configuration this adapter belongs to
	 */
	@Override
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Start the adapter. The thread-name will be set to the adapter's name.
	 * The run method, called by t.start(), will call the startRunning method
	 * of the IReceiver. The Adapter will be a new thread, as this interface
	 * extends the <code>Runnable</code> interface. The actual starting is done
	 * in the <code>run</code> method.
	 * @see Receiver#startRunning()
	 */
	@Override
	public void startRunning() {
		switch(getRunState()) {
			case STARTING:
			case EXCEPTION_STARTING:
			case STARTED:
			case STOPPING:
			case EXCEPTION_STOPPING:
				log.warn("cannot start adapter [{}] that is stopping, starting or already started", name);
				return;
			default:
				break;
		}

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("starting Adapter "+getName());
				try {
					// See also Receiver.startRunning()
					if (!configurationSucceeded) {
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
					} catch (PipeStartException pre) {
						addErrorMessageToMessageKeeper("got error starting PipeLine", pre);
						runState.setRunState(RunState.ERROR);
						return;
					}

					//Update the adapter uptime.
					statsUpSince = System.currentTimeMillis();

					// as from version 3.0 the adapter is started,
					// regardless of receivers are correctly started.
					// this allows the use of test-pipeline without (running) receivers
					runState.setRunState(RunState.STARTED);
					getMessageKeeper().add("Adapter [" + getName() + "] up and running");
					log.info("Adapter [{}] up and running", name);

					// starting receivers
					for (Receiver<?> receiver: receivers) {
						receiver.startRunning();
					}
				} catch (Throwable t) {
					addErrorMessageToMessageKeeper("got error starting Adapter", t);
					runState.setRunState(RunState.ERROR);
				} finally {
					configuration.removeStartAdapterThread(this);
				}
			}

			@Override
			public String toString() {
				return getName();
			}
		};

		configuration.addStartAdapterThread(runnable);
		taskExecutor.execute(runnable);
	}

	/**
	 * Stop the <code>Adapter</code> and close all elements like receivers,
	 * Pipeline, pipes etc.
	 * The adapter
	 * will call the <code>IReceiver</code> to <code>stopListening</code>
	 * <p>Also the <code>PipeLine.close()</code> method will be called,
	 * closing all registered pipes. </p>
	 * @see Receiver#stopRunning
	 * @see PipeLine#stop
	 */
	@Override
	public void stopRunning() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("stopping Adapter " +getName());
				try {
					// See also Receiver.stopRunning()
					switch(getRunState()) {
						case STARTING:
						case STOPPING:
						case STOPPED:
							if (log.isWarnEnabled()) log.warn("adapter [{}] currently in state [{}], ignoring stop() command", getName(), getRunStateAsString());
							return;
						default:
							break;
					}
					runState.setRunState(RunState.STOPPING);
					log.debug("Adapter [{}] is stopping receivers", name);
					for (Receiver<?> receiver: receivers) {
						receiver.stopRunning();
					}
					// IPullingListeners might still be running, see also
					// comment in method Receiver.tellResourcesToStop()
					for (Receiver<?> receiver: receivers) {
						if(receiver.getRunState() == RunState.ERROR) {
							continue; // We don't need to stop the receiver as it's already stopped...
						}
						while (!receiver.isStopped()) {
							// Passing receiver.getRunState() as supplier could cause recursive log invocation so should be avoided
							if (log.isDebugEnabled()) log.debug("Adapter [{}] waiting for receiver [{}] in state [{}] to stop", name, receiver.getName(), receiver.getRunState());
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								if (log.isWarnEnabled()) log.warn("Interrupted waiting for threads of receiver [{}] to end", receiver.getName(), e);
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
					//Set the adapter uptime to 0 as the adapter is stopped.
					statsUpSince = 0;
					runState.setRunState(RunState.STOPPED);
					getMessageKeeper().add("Adapter [" + name + "] stopped");
				} catch (Throwable t) {
					addErrorMessageToMessageKeeper("got error stopping Adapter", t);
					runState.setRunState(RunState.ERROR);
				} finally {
					configuration.removeStopAdapterThread(this);
				}
			}

			@Override
			public String toString() {
				return getName();
			}
		};

		configuration.addStopAdapterThread(runnable);
		taskExecutor.execute(runnable);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[name=" + name + "]");
		sb.append("[targetDesignDocument=" + targetDesignDocument + "]");
		sb.append("[receivers=");
		for (Receiver<?> receiver: receivers) {
			sb.append(" " + receiver.getName());

		}
		sb.append("]");
		sb.append("[pipeLine="+ ((pipeline != null) ? pipeline.toString() : "none registered") + "][started=" + getRunState() + "]");

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
		synchronized (statsMessageProcessingDuration) {
			while (getNumOfMessagesInProcess() > 0) {
				statsMessageProcessingDuration.wait(); // waits for notification from decNumOfMessagesInProcess()
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
		this.name = name;
	}
	@JmxAttribute(description = "Name of the Adapter")
	@Override
	public String getName() {
		return name;
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

		private @Getter Level effectiveLevel;

		private MessageLogLevel(Level effectiveLevel) {
			this.effectiveLevel = effectiveLevel;
		}
	}

	/**
	 * Defines behaviour for logging messages. Configuration is done in the MSG appender in log4j4ibis.properties.
	 * @ff.default <code>INFO, unless overridden by property msg.log.level.default</code>
	 */
	public void setMsgLogLevel(MessageLogLevel level) throws ConfigurationException {
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

	public void setTargetDesignDocument(String string) {
		targetDesignDocument = string;
	}

}
