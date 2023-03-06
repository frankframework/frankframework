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
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.jmx.JmxAttribute;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CounterStatistic;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * The Adapter is the central manager in the IBIS Adapterframework, that has knowledge
 * and uses {@link Receiver Receivers} and a {@link PipeLine}.
 * <br/>
 * <b>Responsibilities</b><br/>
 * <ul>
 *   <li>keeping and gathering statistics</li>
 *   <li>processing messages, retrieved from IReceivers</li>
 *   <li>starting and stoppping IReceivers</li>
 *   <li>delivering error messages in a specified format</li>
 * </ul>
 * All messages from IReceivers pass through the adapter (multi threaded).
 * Multiple receivers may be attached to one adapter.<br/>
 * <br/>
 * The actual processing of messages is delegated to the {@link PipeLine}
 * object, which returns a {@link PipeLineResult}. If an error occurs during
 * the pipeline execution, the state in the <code>PipeLineResult</code> is set
 * to the state specified by <code>setErrorState</code>, which defaults to "ERROR".
 *
 * @author Johan Verrips
 * @see    Receiver
 * @see    PipeLine
 * @see    StatisticsKeeper
 * @see    DateUtils
 * @see    MessageKeeper
 * @see    PipeLineResult
 *
 */
public class Adapter implements IAdapter, NamedBean {
	private @Getter @Setter ApplicationContext applicationContext;

	private Logger log = LogUtil.getLogger(this);
	protected Logger msgLog = LogUtil.getLogger("MSG");

	private Level MSGLOG_LEVEL_TERSE = Level.toLevel("TERSE");

	public static final String PROCESS_STATE_OK = "OK";
	public static final String PROCESS_STATE_ERROR = "ERROR";

	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private AppConstants APP_CONSTANTS = AppConstants.getInstance(configurationClassLoader);

	private String name;
	private @Getter String description;
	private @Getter boolean autoStart = APP_CONSTANTS.getBoolean("adapters.autoStart", true);
	private @Getter boolean replaceNullMessage = false;
	private @Getter int messageKeeperSize = 10; //default length of MessageKeeper
	private Level msgLogLevel = Level.toLevel(APP_CONSTANTS.getProperty("msg.log.level.default", "BASIC"));
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
		// you never can trust an implementation, so try/catch!
		try {
			Message formattedErrorMessage= errorMessageFormatter.format(errorMessage, t, objectInError, originalMessage, messageID, receivedTime);

			if(msgLog.isEnabled(MSGLOG_LEVEL_TERSE)) {
				String resultOrSize = (isMsgLogHidden()) ? "SIZE="+getFileSizeAsBytes(formattedErrorMessage) : formattedErrorMessage.toString();
				msgLog.log(MSGLOG_LEVEL_TERSE, String.format("Adapter [%s] messageId [%s] formatted errormessage, result [%s]", getName(), messageID, resultOrSize));
			}

			return formattedErrorMessage;
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

	public void forEachStatisticsKeeper(StatisticsKeeperIterationHandler hski, Date now, Date mainMark, Date detailMark, int action) throws SenderException {
		Object root=hski.start(now,mainMark,detailMark);
		try {
			forEachStatisticsKeeperBody(hski,root,action);
		} finally {
			hski.end(root);
		}
	}

	private void doForEachStatisticsKeeperBody(StatisticsKeeperIterationHandler hski, Object adapterData, int action) throws SenderException {
		hski.handleScalar(adapterData,"messagesInProcess", getNumOfMessagesInProcess());
		hski.handleScalar(adapterData,"messagesProcessed", getNumOfMessagesProcessed());
		hski.handleScalar(adapterData,"messagesInError", getNumOfMessagesInError());
		log.trace("Get Adapter messagesProcessedThisInterval, synchronize (lock) on numOfMessagesProcessed[{}]", numOfMessagesProcessed);
		hski.handleScalar(adapterData,"messagesProcessedThisInterval", numOfMessagesProcessed.getIntervalValue());
		log.trace("Got Adapter messagesProcessedThisInterval, lock released on numOfMessagesProcessed[{}]", numOfMessagesProcessed);

		log.trace("Get Adapter messagesInErrorThisInterval, synchronize (lock) on numOfMessagesInError[{}]", numOfMessagesInError);
		hski.handleScalar(adapterData,"messagesInErrorThisInterval", numOfMessagesInError.getIntervalValue());
		log.trace("Got Adapter messagesInErrorThisInterval, lock released on numOfMessagesInError[{}]", numOfMessagesInError);

		hski.handleStatisticsKeeper(adapterData, statsMessageProcessingDuration);
		statsMessageProcessingDuration.performAction(action);
		numOfMessagesProcessed.performAction(action);
		numOfMessagesInError.performAction(action);

		Object hourData=hski.openGroup(adapterData,getName(),"processing by hour");
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

		boolean showDetails = action == HasStatistics.STATISTICS_ACTION_FULL
				|| action == HasStatistics.STATISTICS_ACTION_MARK_FULL
				|| action == HasStatistics.STATISTICS_ACTION_RESET;
		if (showDetails) {
			Object recsData=hski.openGroup(adapterData,null,"receivers");
			for (Receiver<?> receiver: receivers) {
				receiver.iterateOverStatistics(hski,recsData,action);
			}
			hski.closeGroup(recsData);

			ICache<String,String> cache=pipeline.getCache();
			if (cache instanceof HasStatistics) {
				((HasStatistics) cache).iterateOverStatistics(hski, recsData, action);
			}

			Object pipelineData=hski.openGroup(adapterData,null,"pipeline");
			getPipeLine().iterateOverStatistics(hski, pipelineData, action);
			hski.closeGroup(pipelineData);
		}
	}

	@Override
	public void forEachStatisticsKeeperBody(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		Object adapterData=hski.openGroup(data,getName(),"adapter");
		hski.handleScalar(adapterData,"upSince", getStatsUpSinceDate());
		hski.handleScalar(adapterData,"lastMessageDate", getLastMessageDateDate());

		if (action!=HasStatistics.STATISTICS_ACTION_FULL &&
			action!=HasStatistics.STATISTICS_ACTION_SUMMARY) {
			log.trace("Iterating over statistics with lock - synchronize (lock) on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
			synchronized (statsMessageProcessingDuration) {
				doForEachStatisticsKeeperBody(hski,adapterData,action);
			}
			log.trace("Iterated over statistics - lock released on statsMessageProcessingDuration[{}]", statsMessageProcessingDuration);
		} else {
			doForEachStatisticsKeeperBody(hski,adapterData,action);
		}
		hski.closeGroup(adapterData);
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
		log.trace("Get Adapter RunState, synchronize (lock) on runState[{}]", runState);
		try {
			return runState.getRunState();
		} finally {
			log.trace("Got Adapter RunState, lock released on runState[{}]", runState);
		}
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

	@Override
	public PipeLineResult processMessage(String messageId, Message message, PipeLineSession pipeLineSession) {
		long startTime = System.currentTimeMillis();
		try {
			return processMessageWithExceptions(messageId, message, pipeLineSession);
		} catch (Throwable t) {
			PipeLineResult result = new PipeLineResult();
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
			//if (isRequestReplyLogging()) {
			String exitCode = ", exit-code ["+result.getExitCode()+"]";
			String format = "Adapter [%s] messageId [%s] got exit-state [%s]"+(result.getExitCode()!=0 ? exitCode : "" ) +" and result [%s] from PipeLine";
			if(msgLog.isEnabled(MSGLOG_LEVEL_TERSE)) {
				String resultOrSize = (isMsgLogHidden()) ? "SIZE="+getFileSizeAsBytes(result.getResult()) : result.getResult().toString();
				msgLog.log(MSGLOG_LEVEL_TERSE, String.format(format, getName(), messageId, result.getState(), resultOrSize));
			}
			if (log.isDebugEnabled()) {
				log.debug(String.format(format, getName(), messageId, result.getState(), result.getResult()));
			}
			return result;
		}
	}

	@Override
	public PipeLineResult processMessageWithExceptions(String messageId, Message message, PipeLineSession pipeLineSession) throws ListenerException {

		PipeLineResult result = new PipeLineResult();

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
		String lastNDC= ThreadContext.peek();
		String newNDC="mid [" + messageId + "]";
		boolean ndcChanged=!newNDC.equals(lastNDC);

		try {
			if (ndcChanged) {
				ThreadContext.push(newNDC);
			}

			if (StringUtils.isNotEmpty(composedHideRegex)) {
				IbisMaskingLayout.addToThreadLocalReplace(composedHideRegex);
			}

			StringBuilder additionalLogging = new StringBuilder();

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

			String format = "Adapter [%s] received message [%s] with messageId [%s]";
			if(msgLog.isEnabled(MSGLOG_LEVEL_TERSE)) {
				String messageOrSize = (isMsgLogHidden()) ? "SIZE="+getFileSizeAsBytes(message) : message.toString();
				msgLog.log(MSGLOG_LEVEL_TERSE, String.format(format, getName(), messageOrSize, messageId) + additionalLogging);
			}
			if (log.isDebugEnabled()) {
				log.debug(String.format(format, getName(), message, messageId) + additionalLogging);
			} else if(log.isInfoEnabled()) {
				log.info(String.format("Adapter [%s] received message with messageId [%s]" + additionalLogging, getName(), messageId));
			}

			if (Message.isEmpty(message) && isReplaceNullMessage()) {
				log.debug("Adapter [{}] replaces null message with messageId [{}] by empty message", name, messageId);
				message = new Message("");
			}
			result = pipeline.process(messageId, message, pipeLineSession);

			String duration;
			if(msgLogHumanReadable) {
				duration = Misc.getAge(startTime);
			} else {
				duration = Misc.getDurationInMs(startTime);
			}
			String exitCode = ", exit-code ["+result.getExitCode()+"]";
			String format2 = "Adapter [%s] messageId [%s] duration [%s] got exit-state [%s]"+(result.getExitCode()!=0 ? exitCode : "" )+" and result [%s] from PipeLine";
			if(msgLog.isEnabled(MSGLOG_LEVEL_TERSE)) {
				String resultOrSize = (isMsgLogHidden()) ? "SIZE="+getFileSizeAsBytes(result.getResult()) : result.toString();
				msgLog.log(MSGLOG_LEVEL_TERSE, String.format(format2, getName(), messageId, duration, result.getState(), resultOrSize));
			}
			if (log.isDebugEnabled()) {
				log.debug(String.format(format2, getName(), messageId, duration, result.getState(), result.getResult()));
			}
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
			throw e;
		} finally {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			//reset the InProcess fields, and increase processedMessagesCount
			decNumOfMessagesInProcess(duration, processingSuccess);

			if (log.isDebugEnabled()) { // for performance reasons
				log.debug("Adapter: [" + getName()
						+ "] STAT: Finished processing message with messageId [" + messageId
						+ "] exit-state [" + result.getState()
						+ "] started " + DateUtils.format(new Date(startTime), DateUtils.FORMAT_FULL_GENERIC)
						+ " finished " + DateUtils.format(new Date(endTime), DateUtils.FORMAT_FULL_GENERIC)
						+ " total duration: " + duration + " msecs");
			} else {
				log.info("Adapter [" + getName() + "] completed message with messageId [" + messageId + "] with exit-state [" + result.getState() + "]");
			}
			IbisMaskingLayout.removeThreadLocalReplace();
			if (ndcChanged) {
				ThreadContext.pop();
			}
			if (ThreadContext.getDepth() == 0) {
				ThreadContext.removeStack();
			}
		}
	}

	// technically, a Receiver is not mandatory, but no useful adapter can do without it.
	/**
	 * Register a receiver for this Adapter
	 * @ff.mandatory
	 */
	public void registerReceiver(Receiver<?> receiver) {
		receivers.add(receiver);
		// Cast arguments to String before invocation so that we do not have recursive call to logger when trace-level logging is enabled
		if (log.isDebugEnabled()) log.debug("Adapter [{}] registered receiver [{}] with properties [{}]", name, receiver.getName(), receiver.toString());
	}

	/**
	 * Register a <code>ErrorMessageFormatter</code> as the formatter
	 * for this <code>adapter</code>
	 * @see IErrorMessageFormatter
	 */
	public void setErrorMessageFormatter(IErrorMessageFormatter errorMessageFormatter) {
		this.errorMessageFormatter = errorMessageFormatter;
	}

	/**
	 * Register a PipeLine at this adapter. On registering, the adapter performs
	 * a <code>Pipeline.configurePipes()</code>, as to configure the individual pipes.
	 * @see PipeLine
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
						while (receiver.getRunState() != RunState.STOPPED) {
							// Passing receiver.getRunState() as supplier could cause recursive log invocation so should be avoided
							if (log.isDebugEnabled()) log.debug("Adapter [{}] waiting for receiver [{}] in state [{}] to stop", name, receiver.getName(), receiver.getRunState());
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								if (log.isWarnEnabled()) log.warn("Interrupted waiting for threads of receiver [{}] to end", receiver.getName(), e);
							}
						}
						log.info("Adapter [{}] successfully stopped receiver [{}]", name, receiver.getName());
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

	@Override
	public String getAdapterConfigurationAsString() {
		String loadedConfig = getConfiguration().getLoadedConfiguration();
		String encodedName = StringUtils.replace(getName(), "'", "''");
		String xpath = "//adapter[@name='" + encodedName + "']";

		return XmlUtils.copyOfSelect(loadedConfig, xpath);
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

	/**
	 * Defines behaviour for logging messages. Configuration is done in the MSG appender in log4j4ibis.properties.
	 * Possible values are: <table border='1'><tr><th>msgLogLevel</th><th>messages which are logged</th></tr>
	 * <tr><td colspan='1'>Off</td> <td>No logging</td></tr>
	 * <tr><td colspan='1'>Basic</td><td>Logs information from adapter level messages </td></tr>
	 * <tr><td colspan='1'>Terse</td><td>Logs information from pipe messages.</td></tr>
	 * <tr><td colspan='1'>All</td> <td>Logs all messages.</td></tr></table>
	 * @ff.default <code>BASIC</code>
	 */
	public void setMsgLogLevel(String level) throws ConfigurationException {
		Level toSet = Level.toLevel(level);
		if (toSet.name().equalsIgnoreCase(level)) //toLevel falls back to DEBUG, so to make sure the level has been changed this explicity check is used.
			msgLogLevel = toSet;
		else
			throw new ConfigurationException("illegal value for msgLogLevel ["+level+"]");
	}

	@Deprecated
	public void setRequestReplyLogging(boolean requestReplyLogging) {
		if (requestReplyLogging) {
			ConfigurationWarnings.add(this, log, "implementing setting of requestReplyLogging=true as msgLogLevel=Terse");
			msgLogLevel = MSGLOG_LEVEL_TERSE;
		} else {
			ConfigurationWarnings.add(this, log, "implementing setting of requestReplyLogging=false as msgLogLevel=None");
			msgLogLevel = Level.toLevel("OFF");
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
