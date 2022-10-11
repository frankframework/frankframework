/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
	protected Logger msgLog = LogUtil.getLogger("MSG");

	public static final Level MSGLOG_LEVEL_TERSE = Level.toLevel("TERSE");
	public static final Level MSGLOG_LEVEL_TERSE_EFF = Level.DEBUG;

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

	private RunStateManager runState = new RunStateManager();
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
		log.debug("configuring adapter [" + getName() + "]");
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

		log.info("Adapter [" + name + "] is initializing receiver [" + receiver.getName() + "]");
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
		log.warn("Adapter [" + getName() + "] "+msg);
		getMessageKeeper().warn(msg);
	}

	/**
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void addErrorMessageToMessageKeeper(String msg, Throwable t) {
		log.error("Adapter [" + getName() + "] " + msg, t);
		if (!(t instanceof IbisException)) {
			msg += " (" + t.getClass().getName() + ")";
		}
		getMessageKeeper().error(msg + ": " + t.getMessage());
	}


	/**
	 * Increase the number of messages in process
	 */
	private void incNumOfMessagesInProcess(long startTime) {
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
	}
	/**
	 * Decrease the number of messages in process
	 */
	private void decNumOfMessagesInProcess(long duration, boolean processingSuccess) {
		synchronized (statsMessageProcessingDuration) {
			numOfMessagesInProcess--;
			numOfMessagesProcessed.increase();
			statsMessageProcessingDuration.addValue(duration);
			if (processingSuccess) {
				lastMessageProcessingState = PROCESS_STATE_OK;
			} else {
				lastMessageProcessingState = PROCESS_STATE_ERROR;
			}
			statsMessageProcessingDuration.notifyAll();
		}
	}
	/**
	 * The number of messages for which processing ended unsuccessfully.
	 */
	private void incNumOfMessagesInError() {
		synchronized (statsMessageProcessingDuration) {
			numOfMessagesInError.increase();
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
			if (sles!=null) {
				if ("timeout".equals(sles.lastExitState)) {
					return sles.lastExitStateDate;
				}
			}
			return 0;
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
				msgLog.log(MSGLOG_LEVEL_TERSE_EFF, "formatted errormessage, result [{}]", resultOrSize);
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

//	public void forEachStatisticsKeeper(StatisticsKeeperIterationHandler hski, Date now, Date mainMark, Date detailMark, Action action) throws SenderException {
//		Object root=hski.start(now,mainMark,detailMark);
//		try {
//			iterateOverStatistics(hski,root,action);
//		} finally {
//			hski.end(root);
//		}
//	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		Object adapterData=hski.openGroup(data,getName(),"adapter");
		hski.handleScalar(adapterData,"upSince", getStatsUpSinceDate());
		hski.handleScalar(adapterData,"lastMessageDate", getLastMessageDateDate());

		if (action!=Action.FULL &&
			action!=Action.SUMMARY) {
			synchronized (statsMessageProcessingDuration) {
				iterateOverStatisticsBody(hski,adapterData,action);
			}
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
		hski.handleScalar(pipelineData,"messagesProcessedThisInterval", numOfMessagesProcessed.getIntervalValue());
		hski.handleScalar(pipelineData,"messagesInErrorThisInterval", numOfMessagesInError.getIntervalValue());
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
		synchronized (statsMessageProcessingDuration) {
			return numOfMessagesInError.getValue();
		}
	}
	@JmxAttribute(description = "# Messages in process")
	public int getNumOfMessagesInProcess() {
		synchronized (statsMessageProcessingDuration) {
			return numOfMessagesInProcess;
		}
	}

	public long[] getNumOfMessagesStartProcessingByHour() {
		synchronized (statsMessageProcessingDuration) {
			return numOfMessagesStartProcessingByHour;
		}
	}
	/**
	 * Total of messages processed
	 * @return long total messages processed
	 */
	@JmxAttribute(description = "# Messages Processed")
	public long getNumOfMessagesProcessed() {
		synchronized (statsMessageProcessingDuration) {
			return numOfMessagesProcessed.getValue();
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
		return runState.getRunState();
	}

	@JmxAttribute(description = "RunState")
	public String getRunStateAsString() {
		return runState.getRunState().toString();
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

	private void messageLogResult(String messageId, PipeLineResult result, String duration) {
		Map<String,String> mdcValues = new HashMap<>();
		mdcValues.put("exitState", result.getState().name());
		if (result.getExitCode()!=0) {
			mdcValues.put("exitCode", Integer.toString(result.getExitCode()));
		}
		if (duration!=null) {
			mdcValues.put("duration", duration);
		}
		mdcValues.put("size", getFileSizeAsBytes(result.getResult()));
		if (!isMsgLogHidden()) {
			mdcValues.put("result", result.getResult().toString());
		}

		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.putAll(mdcValues)) {
			if(msgLog.isEnabled(MSGLOG_LEVEL_TERSE)) {
				msgLog.log(MSGLOG_LEVEL_TERSE_EFF, "returned from PipeLine");
			}
			if (log.isDebugEnabled()) {
				String exitCode = ", exit-code ["+result.getExitCode()+"]";
				String format = "got exit-state [{}]"+(result.getExitCode()!=0 ? exitCode : "" ) +" and result [{}] from PipeLine";
				log.debug("Adapter [{}] messageId [{}] "+format, getName(), messageId, result.getState(), result.getResult());
			}
		}
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
			messageLogResult(messageId, result, null);
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
			try (final CloseableThreadContext.Instance ctc1 = ndcChanged ? CloseableThreadContext.push(newNDC): null) {
				try (final CloseableThreadContext.Instance ctc2 = CloseableThreadContext.put("Adapter", getName()).put("mid", messageId).put("cid", pipeLineSession.getCorrelationId())) {
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

						if(msgLog.isEnabled(MSGLOG_LEVEL_TERSE)) {
							String messageOrSize = (isMsgLogHidden()) ? "SIZE="+getFileSizeAsBytes(message) : message.toString();
							msgLog.log(MSGLOG_LEVEL_TERSE_EFF, "received message [{}]{}", messageOrSize, additionalLogging);
						}
						log.info("Adapter [{}] received message with messageId [{}]{}", getName(), messageId, additionalLogging);

						if (Message.isEmpty(message) && isReplaceNullMessage()) {
							log.debug("Adapter [" + getName() + "] replaces null message with messageId [" + messageId + "] by empty message");
							message = new Message("");
						}
						result = pipeline.process(messageId, message, pipeLineSession);

						String duration;
						if(msgLogHumanReadable) {
							duration = Misc.getAge(startTime);
						} else {
							duration = Misc.getDurationInMs(startTime);
						}
						messageLogResult(messageId, result, duration);
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
					}
				}
			}
		} finally {
			if (ThreadContext.getDepth() == 0) {
				ThreadContext.removeStack();
			}
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
		log.debug("Adapter [" + name + "] registered receiver [" + receiver.getName() + "] with properties [" + receiver.toString() + "]");
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
		log.debug("Adapter [" + name + "] registered pipeline [" + pipeline.toString() + "]");
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
				log.warn("cannot start adapter ["+getName()+"] that is stopping, starting or already started");
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
						log.error("configuration of adapter [" + getName() + "] did not succeed, therefore starting the adapter is not possible");
						warn("configuration did not succeed. Starting the adapter ["+getName()+"] is not possible");
						runState.setRunState(RunState.ERROR);
						return;
					}
					if (configuration.isUnloadInProgressOrDone()) {
						log.error("configuration of adapter [" + getName() + "] unload in progress or done, therefore starting the adapter is not possible");
						warn("configuration unload in progress or done. Starting the adapter ["+getName()+"] is not possible");
						return;
					}
					synchronized (runState) {
						RunState currentRunState = getRunState();
						if (currentRunState!=RunState.STOPPED) {
							String msg = "currently in state [" + currentRunState + "], ignoring start() command";
							warn(msg);
							return;
						}
						runState.setRunState(RunState.STARTING);
					}

					// start the pipeline
					try {
						log.debug("Adapter [" + getName() + "] is starting pipeline");
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
					log.info("Adapter [" + getName() + "] up and running");

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
							log.warn("adapter ["+getName()+"] currently in state [" + getRunState() + "], ignoring stop() command");
							return;
						default:
							break;
					}
					runState.setRunState(RunState.STOPPING);
					log.debug("Adapter [" + name + "] is stopping receivers");
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
							log.debug("Adapter [" + getName() + "] waiting for receiver [" + receiver.getName() + "] in state ["+receiver.getRunState()+"] to stop");
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								log.warn("Interrupted waiting for threads of receiver [" + receiver.getName() + "] to end", e);
							}
						}
						log.info("Adapter [" + getName() + "] successfully stopped receiver [" + receiver.getName() + "]");
					}
					int currentNumOfMessagesInProcess = getNumOfMessagesInProcess();
					if (currentNumOfMessagesInProcess > 0) {
						String msg = "Adapter [" + name + "] is being stopped while still processing " + currentNumOfMessagesInProcess + " messages, waiting for them to finish";
						warn(msg);
					}
					waitForNoMessagesInProcess();
					log.debug("Adapter [" + name + "] is stopping pipeline");
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
		synchronized (statsMessageProcessingDuration) {
			while (getNumOfMessagesInProcess() > 0) {
				statsMessageProcessingDuration.wait(); // waits for notification from decNumOfMessagesInProcess()
			}
		}
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
