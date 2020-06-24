/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.beans.factory.NamedBean;
import org.springframework.core.task.TaskExecutor;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.configuration.ClassLoaderManager;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CounterStatistic;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * The Adapter is the central manager in the IBIS Adapterframework, that has knowledge
 * and uses {@link IReceiver IReceivers} and a {@link PipeLine}.
 *
 * <b>responsibility</b><br/>
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
 * <tr><td>{@link #setReplaceNullMessage(boolean) replaceNullMessage}</td><td>when <code>true</code> a null message is replaced by an empty message</td><td>false</td></tr>
 * </table>
 * 
 * @author Johan Verrips
 * @see    IReceiver
 * @see    PipeLine
 * @see    StatisticsKeeper
 * @see    DateUtils
 * @see    MessageKeeper
 * @see    PipeLineResult
 * 
 */
public class Adapter implements IAdapter, NamedBean {
	private Logger log = LogUtil.getLogger(this);
	protected Logger msgLog = LogUtil.getLogger("MSG");

	private Level MSGLOG_LEVEL_TERSE = Level.toLevel("TERSE");

	public static final String PROCESS_STATE_OK = "OK";
	public static final String PROCESS_STATE_ERROR = "ERROR";

	private ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private String name;
	private Configuration configuration;
	private String targetDesignDocument;
	private boolean active=true;

	private ArrayList<IReceiver> receivers = new ArrayList<IReceiver>();
	private long lastMessageDate = 0;
	private String lastMessageProcessingState; //"OK" or "ERROR"
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
	private boolean configurationSucceeded = false;
	private String description;
	private MessageKeeper messageKeeper; //instantiated in configure()
	private int messageKeeperSize = 10; //default length
	private AppConstants APP_CONSTANTS = AppConstants.getInstance(configurationClassLoader);
	private boolean autoStart = APP_CONSTANTS.getBoolean("adapters.autoStart", true);
	private boolean recover = false;
	private boolean replaceNullMessage = false;
	private boolean msgLogHumanReadable = APP_CONSTANTS.getBoolean("msg.log.humanReadable", false);

	// state to put in PipeLineResult when a PipeRunException occurs;
	private String errorState = "ERROR";

	private TaskExecutor taskExecutor;

	private String composedHideRegex;

	private Level msgLogLevel = Level.toLevel(APP_CONSTANTS.getProperty("msg.log.level.default", "BASIC"));
	private boolean msgLogHidden = APP_CONSTANTS.getBoolean("msg.log.hidden.default", true);

	/**
	 * Indicates whether the configuration succeeded.
	 */
	public boolean configurationSucceeded() {
		return configurationSucceeded;
	}

	/*
	 * This function is called by Configuration.registerAdapter,
	 * to make configuration information available to the Adapter. <br/><br/>
	 * This method also performs
	 * a <code>Pipeline.configurePipes()</code>, as to configure the individual pipes.
	 * @see nl.nn.adapterframework.core.Pipeline#configurePipes
	 */
	@Override
	public void configure() throws ConfigurationException {
		msgLog = LogUtil.getMsgLogger(this);
		Configurator.setLevel(msgLog.getName(), msgLogLevel);
		configurationSucceeded = false;
		log.debug("configuring adapter [" + getName() + "]");
		messageKeeper = getMessageKeeper();
		statsMessageProcessingDuration = new StatisticsKeeper(getName());
		if (pipeline == null) {
			String msg = "No pipeline configured for adapter [" + getName() + "]";
			messageKeeper.add(msg, MessageKeeperLevel.ERROR);
			throw new ConfigurationException(msg);
		}
		try {
			pipeline.setAdapter(this);
			pipeline.configure();
			messageKeeper.add("Adapter [" + name + "] pipeline successfully configured");
			Iterator<IReceiver> it = receivers.iterator();
			while (it.hasNext()) {
				IReceiver receiver = it.next();
				configureReceiver(receiver);
			}
			configurationSucceeded = true;
		}
		catch (ConfigurationException e) {
			error(true, "error initializing pipeline", e);
		}

		List<String> hrs = new ArrayList<String>();
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
	}

	public void configureReceiver(IReceiver receiver) {
		log.info("Adapter [" + name + "] is initializing receiver [" + receiver.getName() + "]");
		receiver.setAdapter(this);
		try {
			receiver.configure();
			getMessageKeeper().add("Receiver [" + receiver.getName() + "] successfully configured");
		} catch (ConfigurationException e) {
			error(true, "error initializing receiver [" + receiver.getName() + "]",e);
		}
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn("Adapter [" + getName() + "] "+msg);
		getMessageKeeper().add("WARNING: " + msg, MessageKeeperLevel.WARN);
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void error(boolean critical, String msg, Throwable t) {
		log.error("Adapter [" + getName() + "] " + msg, t);
		if (!(t instanceof IbisException)) {
			msg += " (" + t.getClass().getName() + ")";
		}
		getMessageKeeper().add("ERROR: " + msg + ": " + t.getMessage(), MessageKeeperLevel.ERROR);
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
	private synchronized void decNumOfMessagesInProcess(long duration, boolean processingSuccess) {
		synchronized (statsMessageProcessingDuration) {
			numOfMessagesInProcess--;
			numOfMessagesProcessed.increase();
			statsMessageProcessingDuration.addValue(duration);
			if (processingSuccess) {
				lastMessageProcessingState = PROCESS_STATE_OK;
			} else {
				lastMessageProcessingState = PROCESS_STATE_ERROR;
			}
			notifyAll();
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
	public synchronized String formatErrorMessage(String errorMessage, Throwable t, Message originalMessage, String messageID, INamedObject objectInError, long receivedTime) {
		if (errorMessageFormatter == null) {
			errorMessageFormatter = new ErrorMessageFormatter();
		}
		// you never can trust an implementation, so try/catch!
		try {
			String formattedErrorMessage= errorMessageFormatter.format(
				errorMessage,
				t,
				objectInError,
				originalMessage,
				messageID,
				receivedTime);

			if(msgLog.isEnabled(MSGLOG_LEVEL_TERSE)) {
				String resultOrSize = (isMsgLogHidden()) ? "SIZE="+getFileSizeAsBytes(new Message(formattedErrorMessage)) : formattedErrorMessage;
				msgLog.log(MSGLOG_LEVEL_TERSE, String.format("Adapter [%s] messageId [%s] formatted errormessage, result [%s]", getName(), messageID, resultOrSize));
			}

			return formattedErrorMessage;
		}
		catch (Exception e) {
			String msg = "got error while formatting errormessage, original errorMessage [" + errorMessage + "]";
			msg = msg + " from [" + (objectInError == null ? "unknown-null" : objectInError.getName()) + "]";
			error(false, "got error while formatting errormessage", e);
			return errorMessage;
		}
	}
	/**
	 * retrieve the date and time of the last message.
	 */
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
			messageKeeper = new MessageKeeper(messageKeeperSize < 1 ? 1 : messageKeeperSize);
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
		hski.handleScalar(adapterData,"messagesProcessedThisInterval", numOfMessagesProcessed.getIntervalValue());
		hski.handleScalar(adapterData,"messagesInErrorThisInterval", numOfMessagesInError.getIntervalValue());
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
			Iterator<IReceiver> recIt = getReceiverIterator();
			if (recIt.hasNext()) {
				while (recIt.hasNext()) {
					IReceiver receiver = recIt.next();
					receiver.iterateOverStatistics(hski,recsData,action);
				}
			}
			hski.closeGroup(recsData);

			ICacheAdapter<String,String> cache=pipeline.getCache();
			if (cache!=null && cache instanceof HasStatistics) {
				((HasStatistics)cache).iterateOverStatistics(hski, recsData, action);
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
			synchronized (statsMessageProcessingDuration) {
				doForEachStatisticsKeeperBody(hski,adapterData,action);
			}
		} else {
			doForEachStatisticsKeeperBody(hski,adapterData,action);
		}
		hski.closeGroup(adapterData);
	}

	/**
	 * the functional name of this adapter
	 * @return  the name of the adapter
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * The number of messages for which processing ended unsuccessfully.
	 */
	public long getNumOfMessagesInError() {
		synchronized (statsMessageProcessingDuration) {
			return numOfMessagesInError.getValue();
		}
	}
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
	public long getNumOfMessagesProcessed() {
		synchronized (statsMessageProcessingDuration) {
			return numOfMessagesProcessed.getValue();
		}
	}

	@Override
	public IReceiver getReceiverByName(String receiverName) {
		Iterator<IReceiver> it = receivers.iterator();
		while (it.hasNext()) {
			IReceiver receiver = it.next();
			if (receiver.getName().equalsIgnoreCase(receiverName)) {
				return receiver;
			}

		}
		return null;
	}

	public IReceiver getReceiverByNameAndListener(String receiverName, Class<?> listenerClass) {
		if (listenerClass == null) {
			return getReceiverByName(receiverName);
		}
		Iterator<IReceiver> it = receivers.iterator();
		while (it.hasNext()) {
			IReceiver receiver = it.next();
			if (receiver.getName().equalsIgnoreCase(receiverName)) {
				if (receiver instanceof ReceiverBase) {
					ReceiverBase receiverBase = (ReceiverBase) receiver;
					if (listenerClass.equals(receiverBase.getListener().getClass())) {
						return receiver;
					}
				}
			}
		}
		return null;
	}

	@Override
	public Iterator<IReceiver> getReceiverIterator() {
		return receivers.iterator();
	}

	public PipeLine getPipeLine() {
		return pipeline;
	}
	
	@Override
	public RunStateEnum getRunState() {
		return runState.getRunState();
	}

	public String getRunStateAsString() {
		return runState.getRunState().toString();
	}

	public String getLastMessageProcessingState() {
		return lastMessageProcessingState;
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
	public PipeLineResult processMessage(String messageId, Message message, IPipeLineSession pipeLineSession) {
		long startTime = System.currentTimeMillis();
		try {
			return processMessageWithExceptions(messageId, message, pipeLineSession);
		} catch (Throwable t) {
			PipeLineResult result = new PipeLineResult();
			result.setState(getErrorState());
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
			result.setResult(new Message(formatErrorMessage(msg, t, message, messageId, objectInError, startTime)));
			//if (isRequestReplyLogging()) {

			String format = "Adapter [%s] messageId [%s] got exit-state [%s] and result [%s] from PipeLine";
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
	public PipeLineResult processMessageWithExceptions(String messageId, Message message, IPipeLineSession pipeLineSession) throws ListenerException {

		PipeLineResult result = new PipeLineResult();

		long startTime = System.currentTimeMillis();
		boolean processingSuccess = true;
		// prevent executing a stopped adapter
		// the receivers should implement this, but you never now....
		RunStateEnum currentRunState = getRunState();
		if (!currentRunState.equals(RunStateEnum.STARTED) && !currentRunState.equals(RunStateEnum.STOPPING)) {

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

			if ((message == null || message.isEmpty()) && isReplaceNullMessage()) {
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

			String format2 = "Adapter [%s] messageId [%s] duration [%s] got exit-state [%s] and result [%s] from PipeLine";
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
			error(false, "error processing message with messageId [" + messageId+"]: ",e);
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

	/**
	 * Register a PipeLine at this adapter. On registering, the adapter performs
	 * a <code>Pipeline.configurePipes()</code>, as to configure the individual pipes.
	 * @see PipeLine
	 */
	@Override
	public void registerPipeLine(PipeLine pipeline) throws ConfigurationException {
		this.pipeline = pipeline;
		pipeline.setAdapter(this);
		log.debug("Adapter [" + name + "] registered pipeline [" + pipeline.toString() + "]");
	}

	/**
	 * Register a receiver for this Adapter
	 * @see IReceiver
	 */
	public void registerReceiver(IReceiver receiver) {
		boolean receiverActive=true;
		if (receiver instanceof ReceiverBase) {
			receiverActive=((ReceiverBase)receiver).isActive();
		}
		if (receiverActive) {
			receivers.add(receiver);
			log.debug("Adapter ["	+ name 	+ "] registered receiver [" + receiver.getName() + "] with properties [" + receiver.toString() + "]");
		} else {
			log.debug("Adapter ["	+ name 	+ "] did not register inactive receiver [" + receiver.getName() + "] with properties [" + receiver.toString() + "]");
		}
	}

	/**
	 *  some functional description of the <code>Adapter</code>/
	 */
	@IbisDoc({"description of the adapter", ""})
	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public String getDescription() {
		return this.description;
	}
	/**
	 * Register a <code>ErrorMessageFormatter</code> as the formatter
	 * for this <code>adapter</code>
	 * @see IErrorMessageFormatter
	 */
	@IbisDoc({" ", ""})
	public void setErrorMessageFormatter(IErrorMessageFormatter errorMessageFormatter) {
		this.errorMessageFormatter = errorMessageFormatter;
	}
	/**
	 * state to put in PipeLineResult when a PipeRunException occurs
	 * @see PipeLineResult
	 */
	public void setErrorState(String newErrorState) {
		errorState = newErrorState;
	}
	/**
	* state to put in PipeLineResult when a PipeRunException occurs.
	*/
	@Override
	public String getErrorState() {
		return errorState;
	}
	/**
	 * Set the number of messages that are kept on the screen.
	 * @see MessageKeeper
	 */
	@IbisDoc({"number of message displayed in ibisconsole", "10"})
	public void setMessageKeeperSize(int size) {
		this.messageKeeperSize = size;
	}
	/**
	 * the functional name of this adapter
	 */
	@IbisDoc({"name of the adapter", ""})
	@Override
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * the configuration this adapter belongs to
	 */
	@Override
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}
	/**
	 * Start the adapter. The thread-name will be set to the adapter's name.
	 * The run method, called by t.start(), will call the startRunning method
	 * of the IReceiver. The Adapter will be a new thread, as this interface
	 * extends the <code>Runnable</code> interface. The actual starting is done
	 * in the <code>run</code> method.
	 * @see IReceiver#startRunning()
	 */
	@Override
	public void startRunning() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("starting Adapter "+getName());
				try {
					// See also ReceiverBase.startRunning()
					if (!configurationSucceeded) {
						log.error("configuration of adapter [" + getName() + "] did not succeed, therefore starting the adapter is not possible");
						warn("configuration did not succeed. Starting the adapter ["+getName()+"] is not possible");
						runState.setRunState(RunStateEnum.ERROR);
						return;
					}
					if (configuration.isUnloadInProgressOrDone()) {
						log.error("configuration of adapter [" + getName() + "] unload in progress or done, therefore starting the adapter is not possible");
						warn("configuration unload in progress or done. Starting the adapter ["+getName()+"] is not possible");
						return;
					}
					synchronized (runState) {
						RunStateEnum currentRunState = getRunState();
						if (!currentRunState.equals(RunStateEnum.STOPPED)) {
							String msg = "currently in state [" + currentRunState + "], ignoring start() command";
							warn(msg);
							return;
						}
						runState.setRunState(RunStateEnum.STARTING);
					}
					// start the pipeline
					try {
						log.debug("Adapter [" + getName() + "] is starting pipeline");
						pipeline.start();
					} catch (PipeStartException pre) {
						error(true, "got error starting PipeLine", pre);
						runState.setRunState(RunStateEnum.ERROR);
						return;
					}
					//Update the adapter uptime.
					statsUpSince = System.currentTimeMillis();
					// as from version 3.0 the adapter is started,
					// regardless of receivers are correctly started.
					runState.setRunState(RunStateEnum.STARTED);
					getMessageKeeper().add("Adapter [" + getName() + "] up and running");
					log.info("Adapter [" + getName() + "] up and running");
					// starting receivers
					Iterator<IReceiver> it = receivers.iterator();
					while (it.hasNext()) {
						IReceiver receiver = it.next();
						receiver.startRunning();
					}
				} catch (Throwable t) {
					error(true, "got error starting Adapter", t);
					runState.setRunState(RunStateEnum.ERROR);
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
	 * closing alle registered pipes. </p>
	 * @see IReceiver#stopRunning
	 * @see PipeLine#stop
	 */
	@Override
	public void stopRunning() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("stopping Adapter " +getName());
				try {
					// See also ReceiverBase.stopRunning()
					synchronized (runState) {
						RunStateEnum currentRunState = getRunState();
						if (currentRunState.equals(RunStateEnum.STARTING)
								|| currentRunState.equals(RunStateEnum.STOPPING)
								|| currentRunState.equals(RunStateEnum.STOPPED)) {
							String msg = "currently in state [" + currentRunState + "], ignoring stop() command";
							warn(msg);
							return;
						}
						runState.setRunState(RunStateEnum.STOPPING);
					}
					log.debug("Adapter [" + name + "] is stopping receivers");
					Iterator<IReceiver> it = receivers.iterator();
					while (it.hasNext()) {
						IReceiver receiver = it.next();
						receiver.stopRunning();
					}
					// IPullingListeners might still be running, see also
					// comment in method ReceiverBase.tellResourcesToStop()
					it = receivers.iterator();
					while (it.hasNext()) {
						IReceiver receiver = (IReceiver) it.next();
						while (receiver.getRunState() != RunStateEnum.STOPPED) {
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
					runState.setRunState(RunStateEnum.STOPPED);
					getMessageKeeper().add("Adapter stopped");
				} catch (Throwable t) {
					error(true, "got error stopping Adapter", t);
					runState.setRunState(RunStateEnum.ERROR);
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
		Iterator<IReceiver> it = receivers.iterator();
		sb.append("[receivers=");
		while (it.hasNext()) {
			IReceiver receiver = it.next();
			sb.append(" " + receiver.getName());

		}
		sb.append("]");
		sb.append("[pipeLine="+ ((pipeline != null) ? pipeline.toString() : "none registered") + "][started=" + getRunState() + "]");

		return sb.toString();
	}

	private String getFileSizeAsBytes(Message message) {
		if (message==null || message.isEmpty()) {
			return null;
		}
		if (message.asObject() instanceof String) {
			return Misc.toFileSize(((String)message.asObject()).length());
		}
		if (message.asObject() instanceof byte[]) {
			return Misc.toFileSize(((byte[])message.asObject()).length);
		}
		return "unknown";
	}

	@Override
	public String getAdapterConfigurationAsString() {
		String loadedConfig = getConfiguration().getLoadedConfiguration();
		String encodedName = StringUtils.replace(getName(), "'", "''");
		String xpath = "//adapter[@name='" + encodedName + "']";

		return XmlUtils.copyOfSelect(loadedConfig, xpath);
	}

	public void waitForNoMessagesInProcess() throws InterruptedException {
		synchronized (statsMessageProcessingDuration) {
			while (getNumOfMessagesInProcess() > 0) {
				wait();
			}
		}
	}

	/**
	 * AutoStart indicates that the adapter should be started when the configuration
	 * is started. AutoStart defaults to <code>true</code>
	 * @since 4.1.1
	 */
	@IbisDoc({"controls whether adapters starts when configuration loads", "true"})
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}
	@Override
	public boolean isAutoStart() {
		return autoStart;
	}

	public void setRequestReplyLogging(boolean requestReplyLogging) {
		if (requestReplyLogging) {
			ConfigurationWarnings.add(this, log, "implementing setting of requestReplyLogging=true as msgLogLevel=Terse");
			msgLogLevel = MSGLOG_LEVEL_TERSE;
		} else {
			ConfigurationWarnings.add(this, log, "implementing setting of requestReplyLogging=false as msgLogLevel=None");
			msgLogLevel = Level.toLevel("OFF");
		}
	}

	@IbisDoc({"controls whether adapter is included in configuration. when set <code>false</code> or set to something else as <code>true</code>, (even set to the empty string), the receiver is not included in the configuration", "true"})
	public void setActive(boolean b) {
		active = b;
	}
	public boolean isActive() {
		return active;
	}

	public void setTargetDesignDocument(String string) {
		targetDesignDocument = string;
	}
	public String getTargetDesignDocument() {
		return targetDesignDocument;
	}

	public void setTaskExecutor(TaskExecutor executor) {
		taskExecutor = executor;
	}
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.NamedBean#getBeanName()
	 */
	@Override
	public String getBeanName() {
		return name;
	}

	@IbisDoc({"defines behaviour for logging messages. Configuration is done in the MSG appender in log4j4ibis.properties. " +
			"Possible values are: <table border='1'><tr><th>msgLogLevel</th><th>messages which are logged</th></tr>" +
			"<tr><td colspan='1'>Off</td> <td>No logging</td></tr>" +
			"<tr><td colspan='1'>Basic</td><td>Logs information from adapter level messages </td></tr>" +
			"<tr><td colspan='1'>Terse</td><td>Logs information from pipe messages.</td></tr>" +
			"<tr><td colspan='1'>All</td> <td>Logs all messages.</td></tr></table>", "BASIC"})
	public void setMsgLogLevel(String level) throws ConfigurationException {
		Level toSet = Level.toLevel(level);
		if (toSet.name().equalsIgnoreCase(level)) //toLevel falls back to DEBUG, so to make sure the level has been changed this explicity check is used.
			msgLogLevel = toSet;
		else
			throw new ConfigurationException("illegal value for msgLogLevel ["+level+"]");
	}

	public String getMsgLogLevel() {
		return msgLogLevel.name();
	}

	@IbisDoc({"if set to <code>true</code>, the length of the message is shown in the msg log instead of the content of the message", "false"})
	public void setMsgLogHidden(boolean b) {
		msgLogHidden = b;
	}
	public boolean isMsgLogHidden() {
		return msgLogHidden;
	}

	public void setRecover(boolean b) {
		recover = b;
	}

	public boolean isRecover() {
		return recover;
	}

	@IbisDoc({"when <code>true</code> a null message is replaced by an empty message", "false"})
	public void setReplaceNullMessage(boolean b) {
		replaceNullMessage = b;
	}

	public boolean isReplaceNullMessage() {
		return replaceNullMessage;
	}

	/**
	 * This ClassLoader is set upon creation of the pipe, used to retrieve resources configured by the Ibis application.
	 * @return returns the ClassLoader created by the {@link ClassLoaderManager ClassLoaderManager}.
	 */
	public ClassLoader getConfigurationClassLoader() {
		return configurationClassLoader;
	}
}
