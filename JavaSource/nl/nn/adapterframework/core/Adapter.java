/*
   Copyright 2013 Nationale-Nederlanden

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

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.CounterStatistic;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.MsgLogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.springframework.beans.factory.NamedBean;
import org.springframework.core.task.TaskExecutor;
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
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.AbstractPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDescription(String) description}</td><td>description of the Adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAutoStart(boolean) autoStart}</td><td>controls whether Adapters starts when configuration loads</td><td>true</td></tr>
 * <tr><td>{@link #setActive(boolean) active}</td>  <td>controls whether Adapter is included in configuration. When set <code>false</code> or set to something else as "true", (even set to the empty string), the receiver is not included in the configuration</td><td>true</td></tr>
 * <tr><td>{@link #setErrorMessageFormatter(String) errorMessageFormatter}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setErrorState(String) errorState}</td><td>If an error occurs during
 * the pipeline execution, the state in the <code>PipeLineResult</code> is set to this state</td><td>ERROR</td></tr>
 * <tr><td>{@link #setMessageKeeperSize(int) messageKeeperSize}</td><td>number of message displayed in IbisConsole</td><td>10</td></tr>
 * <tr><td>{@link #setMsgLogLevel(String) msgLogLevel}</td><td>defines behaviour for logging messages. Configuration is done in the MSG appender in log4j4ibis.properties. Possible values are: 
 *   <table border="1">
 *   <tr><th>msgLogLevel</th><th>messages which are logged</th></tr>
 *   <tr><td colspan="1">None</td> <td>none</td></tr>
 *   <tr><td colspan="1">Terse</td><td>at adapter level</td></tr>
 *   <tr><td colspan="1">Basic</td><td>at adapter and sending pipe level (not yet available; only at adapter level)</td></tr>
 *   <tr><td colspan="1">Full</td> <td>at adapter and pipe level (not yet available; only at adapter level)</td></tr>
 *  </table></td><td>application default (None)</td></tr>
 * </table>
 * 
 * @version $Id$
 * @author Johan Verrips
 * @see    nl.nn.adapterframework.core.IReceiver
 * @see    nl.nn.adapterframework.core.PipeLine
 * @see    nl.nn.adapterframework.util.StatisticsKeeper
 * @see    nl.nn.adapterframework.util.DateUtils
 * @see    nl.nn.adapterframework.util.MessageKeeper
 * @see    nl.nn.adapterframework.core.PipeLineResult
 * 
 */
public class Adapter implements IAdapter, NamedBean {
	private Logger log = LogUtil.getLogger(this);
	protected Logger msgLog = LogUtil.getLogger("MSG");

	private String name;
	private String targetDesignDocument;
	private boolean active=true;

	private Vector receivers = new Vector();
	private long lastMessageDate = 0;
	private PipeLine pipeline;

	private int numOfMessagesInProcess = 0;
   
	private CounterStatistic numOfMessagesProcessed = new CounterStatistic(0);
	private CounterStatistic numOfMessagesInError = new CounterStatistic(0);
	
	private long[] numOfMessagesStartProcessingByHour = new long[24];
	
	private StatisticsKeeper statsMessageProcessingDuration = null;

	private long statsUpSince = System.currentTimeMillis();
	private IErrorMessageFormatter errorMessageFormatter;

	private RunStateManager runState = new RunStateManager();
	private boolean configurationSucceeded = false;
	private String description;
	private MessageKeeper messageKeeper; //instantiated in configure()
	private int messageKeeperSize = 10; //default length
	private boolean autoStart = true;
	private int msgLogLevel = MsgLogUtil.getMsgLogLevelByDefault();

	// state to put in PipeLineResult when a PipeRunException occurs;
	private String errorState = "ERROR";


    
    private TaskExecutor taskExecutor;
    
	/**
	 * Indicates wether the configuration succeeded.
	 * @return boolean
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
	public void configure() throws ConfigurationException {
		configurationSucceeded = false;
		log.debug("configuring adapter [" + getName() + "]");
		MessageKeeper messageKeeper = getMessageKeeper();
		statsMessageProcessingDuration = new StatisticsKeeper(getName());
		if (pipeline == null) {
			String msg = "No pipeline configured for adapter [" + getName() + "]";
			messageKeeper.add(msg, MessageKeeperMessage.ERROR_LEVEL);
			throw new ConfigurationException(msg);
		}

		try {
			pipeline.setAdapter(this);
			pipeline.configure();

			messageKeeper.add("pipeline successfully configured");
			Iterator it = receivers.iterator();
			while (it.hasNext()) {
				IReceiver receiver = (IReceiver) it.next();
				
				log.info("Adapter [" + name + "] is initializing receiver [" + receiver.getName() + "]");
				receiver.setAdapter(this);
				try {
					receiver.configure();
					messageKeeper.add("receiver [" + receiver.getName() + "] successfully configured");
				} catch (ConfigurationException e) {
					error(true, "error initializing receiver [" + receiver.getName() + "]",e);
				}

			}
			configurationSucceeded = true;
		}
		catch (ConfigurationException e) {
			error(true, "error initializing pipeline", e);
		}
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn("Adapter [" + getName() + "] "+msg);
		getMessageKeeper().add("WARNING: " + msg, MessageKeeperMessage.WARN_LEVEL);
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void error(boolean critical, String msg, Throwable t) {
		log.error("Adapter [" + getName() + "] "+msg, t);
		if (!(t instanceof IbisException)) {
			msg+=" (" + t.getClass().getName()+")";
		}
		getMessageKeeper().add("ERROR: " + msg+": "+t.getMessage(), MessageKeeperMessage.ERROR_LEVEL);
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
			numOfMessagesStartProcessingByHour[hour]++;
		}
	}
	/**
	 * Decrease the number of messages in process
	 */
	private synchronized void decNumOfMessagesInProcess(long duration) {
		synchronized (statsMessageProcessingDuration) {
			numOfMessagesInProcess--;
			numOfMessagesProcessed.increase();
			statsMessageProcessingDuration.addValue(duration);
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

	public synchronized String formatErrorMessage(
		String errorMessage,
		Throwable t,
		String originalMessage,
		String messageID,
		INamedObject objectInError,
		long receivedTime) {
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
			//if (isRequestReplyLogging()) {
			String logMsg = "Adapter [" + getName() + "] messageId[" + messageID + "] formatted errormessage, result [" + formattedErrorMessage + "]";
			if (isMsgLogTerseEnabled()) {
				msgLog.info(logMsg);
			}
			if (log.isDebugEnabled()) {
				log.debug(logMsg);
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
		String result = "";
		if (lastMessageDate != 0)
			result = DateUtils.format(new Date(lastMessageDate), DateUtils.FORMAT_FULL_GENERIC);
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
	* @see nl.nn.adapterframework.util.MessageKeeper
	 */
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

		boolean showDetails=(action==HasStatistics.STATISTICS_ACTION_FULL || 
							 action==HasStatistics.STATISTICS_ACTION_MARK_FULL ||
							 action==HasStatistics.STATISTICS_ACTION_RESET);
		if (showDetails) {
			Object recsData=hski.openGroup(adapterData,null,"receivers");
			Iterator recIt=getReceiverIterator();
			if (recIt.hasNext()) {
				while (recIt.hasNext()) {
					IReceiver receiver=(IReceiver) recIt.next();
					receiver.iterateOverStatistics(hski,recsData,action);
				}
			}
			hski.closeGroup(recsData);

			ICacheAdapter cache=pipeline.getCache();
			if (cache!=null && cache instanceof HasStatistics) {
				((HasStatistics)cache).iterateOverStatistics(hski, recsData, action);
			}
			
			Object pipelineData=hski.openGroup(adapterData,null,"pipeline");
			getPipeLine().iterateOverStatistics(hski, pipelineData, action);
			hski.closeGroup(pipelineData);
		}
	}
	
	public void forEachStatisticsKeeperBody(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		Object adapterData=hski.openGroup(data,getName(),"adapter");
//		hski.handleScalar(adapterData,"name", getName());
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

	public IReceiver getReceiverByName(String receiverName) {
		Iterator it = receivers.iterator();
		while (it.hasNext()) {
			IReceiver receiver = (IReceiver) it.next();
			if (receiver.getName().equalsIgnoreCase(receiverName)) {
				return receiver;
			}

		}
		return null;
	}
	public Iterator getReceiverIterator() {
		return receivers.iterator();
	}
	
	public PipeLine getPipeLine() {
		return pipeline;
	}
	
	public RunStateEnum getRunState() {
		return runState.getRunState();
	}

	public String getRunStateAsString() {
		return runState.getRunState().toString();
	}
	/**
	 * Return the total processing duration as a StatisticsKeeper
	 * @see nl.nn.adapterframework.util.StatisticsKeeper
	 * @return nl.nn.adapterframework.util.StatisticsKeeper
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
		return DateUtils.format(new Date(statsUpSince), DateUtils.FORMAT_FULL_GENERIC);
	}
	public Date getStatsUpSinceDate() {
		return new Date(statsUpSince);
	}


	public PipeLineResult processMessage(String messageId, String message, IPipeLineSession pipeLineSession) {
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
			result.setResult(formatErrorMessage(msg, t, message, messageId, objectInError, startTime));
			//if (isRequestReplyLogging()) {
			String logMsg = "Adapter [" + getName() + "] messageId [" + messageId + "] got exit-state [" + result.getState() + "] and result [" + result.getResult() + "] from PipeLine";
			if (isMsgLogTerseEnabled()) {
				msgLog.info(logMsg);
			}
			if (log.isDebugEnabled()) {
				log.debug(logMsg);
			}
			return result;
		}
	}
	
	public PipeLineResult processMessageWithExceptions(String messageId, String message, IPipeLineSession pipeLineSession) throws ListenerException {

		PipeLineResult result = new PipeLineResult();

		long startTime = System.currentTimeMillis();
		// prevent executing a stopped adapter
		// the receivers should implement this, but you never now....
		RunStateEnum currentRunState = getRunState();
		if (!currentRunState.equals(RunStateEnum.STARTED) && !currentRunState.equals(RunStateEnum.STOPPING)) {

			String msgAdapterNotOpen =
				"Adapter [" + getName() + "] in state [" + currentRunState + "], cannot process message";
			throw new ListenerException(new ManagedStateException(msgAdapterNotOpen));
		}

		incNumOfMessagesInProcess(startTime);
		String lastNDC=NDC.peek();
		String newNDC="cid [" + messageId + "]";
		boolean ndcChanged=!newNDC.equals(lastNDC);
		if (ndcChanged) {
			NDC.push(newNDC);
		}
		
		//if (isRequestReplyLogging()) {
		String logMsg = "Adapter [" + name + "] received message [" + message + "] with messageId [" + messageId + "]";
		if (isMsgLogTerseEnabled()) {
			msgLog.info(logMsg);
		}
		if (log.isDebugEnabled()) { 
			log.debug(logMsg);
		} else {
			log.info("Adapter [" + name + "] received message with messageId [" + messageId + "]");
		}


		try {
			result = pipeline.process(messageId, message,pipeLineSession);
			//if (isRequestReplyLogging()) {
			logMsg = "Adapter [" + getName() + "] messageId[" + messageId + "] got exit-state [" + result.getState() + "] and result [" + result.toString() + "] from PipeLine";
			if (isMsgLogTerseEnabled()) {
				msgLog.info(logMsg);
			}
			if (log.isDebugEnabled()) {
				log.debug(logMsg);
			}
			return result;
	
		} catch (Throwable t) {
			ListenerException e;
			if (t instanceof ListenerException) {
				e = (ListenerException) t;
			} else {
				e = new ListenerException(t);
			}
			incNumOfMessagesInError();
			error(false, "error processing message with messageId [" + messageId+"]: ",e);
			throw e;
		} finally {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			//reset the InProcess fields, and increase processedMessagesCount
			decNumOfMessagesInProcess(duration);
	
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
			if (ndcChanged) {
				NDC.pop();
			}
		}
	}

	/**
	 * Register a PipeLine at this adapter. On registering, the adapter performs
	 * a <code>Pipeline.configurePipes()</code>, as to configure the individual pipes.
	  * @param pipeline
	 * @throws ConfigurationException
	 * @see PipeLine
	 */
	public void registerPipeLine(PipeLine pipeline) throws ConfigurationException {
		this.pipeline = pipeline;
		pipeline.setAdapter(this);
		log.debug("Adapter [" + name + "] registered pipeline [" + pipeline.toString() + "]");
	}

	/**
	 * Register a receiver for this Adapter
	 * @param receiver
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
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return this.description;
	}
	/**
	 * Register a <code>ErrorMessageFormatter</code> as the formatter
	 * for this <code>adapter</code>
	 * @param errorMessageFormatter
	 * @see IErrorMessageFormatter
	 */
	public void setErrorMessageFormatter(IErrorMessageFormatter errorMessageFormatter) {
		this.errorMessageFormatter = errorMessageFormatter;
	}
	/**
	 * state to put in PipeLineResult when a PipeRunException occurs
	 * @param newErrorState java.lang.String
	 * @see PipeLineResult
	 */
	public void setErrorState(java.lang.String newErrorState) {
		errorState = newErrorState;
	}
	/**
	* state to put in PipeLineResult when a PipeRunException occurs.
	*/
	public String getErrorState() {
		return errorState;
	}
	/**
	 * Set the number of messages that are kept on the screen.
	 * @param size
	 * @see nl.nn.adapterframework.util.MessageKeeper
	 */
	public void setMessageKeeperSize(int size) {
		this.messageKeeperSize = size;
	}
	/**
	 * the functional name of this adapter
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * Start the adapter. The thread-name will be set tot the adapter's name.
	 * The run method, called by t.start(), will call the startRunning method
	 * of the IReceiver. The Adapter will be a new thread, as this interface
	 * extends the <code>Runnable</code> interface. The actual starting is done
	 * in the <code>run</code> method.
	 * @see IReceiver#startRunning()
	 * @see Adapter#run
	 */
	public void startRunning() {
		taskExecutor.execute(new Runnable() {
            public void run() {
                Thread.currentThread().setName("starting Adapter "+getName());
                try {
                    if (!configurationSucceeded) {
                        log.error(
                            "configuration of adapter ["
                                + getName()
                                + "] did not succeed, therefore starting the adapter is not possible");
                        warn("configuration did not succeed. Starting the adapter is not possible");
                        runState.setRunState(RunStateEnum.ERROR);
                        return;
                    }

                    synchronized (runState) {
                        RunStateEnum currentRunState = getRunState();
                        if (!currentRunState.equals(RunStateEnum.STOPPED)) {
                            String msg =
                                "currently in state [" + currentRunState + "], ignoring start() command";
                            warn(msg);
                            return;
                        }
                        // start the pipeline
                        runState.setRunState(RunStateEnum.STARTING);
                    }
                    try {
                        log.debug("Adapter [" + getName() + "] is starting pipeline");
                        pipeline.start();
                    }
                    catch (PipeStartException pre) {
                        error(true, "got error starting PipeLine", pre);
                        runState.setRunState(RunStateEnum.ERROR);
                        return;
                    }

                    // as from version 3.0 the adapter is started,
                    // regardless of receivers are correctly started.
                    runState.setRunState(RunStateEnum.STARTED);

                    getMessageKeeper().add("Adapter up and running");
                    log.info("Adapter [" + getName() + "] up and running");

                    // starting receivers
                    Iterator it = receivers.iterator();
                    while (it.hasNext()) {
                        IReceiver receiver = (IReceiver) it.next();
                        if (receiver.getRunState() != RunStateEnum.ERROR) {
                            log.info("Adapter [" + getName() + "] is starting receiver [" + receiver.getName() + "]");
                            receiver.startRunning();
                        }
                        else
                            log.warn("Adapter [" + getName() + "] will NOT start receiver [" + receiver.getName() + "] as it is in state ERROR");
                    } //while

//                    // wait until the stopRunning is called
//                    waitForRunState(RunStateEnum.STOPPING);
            
                }
                catch (Throwable t) {
					error(true, "got error starting Adapter", t);
                    runState.setRunState(RunStateEnum.ERROR);
                }
            } // End Runnable.run()
        }); // End Runnable
	} // End startRunning()
    
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
	public void stopRunning() {

		synchronized (runState) {
			RunStateEnum currentRunState = getRunState();

			if (!currentRunState.equals(RunStateEnum.STARTED) && (!currentRunState.equals(RunStateEnum.ERROR))) {
				warn("in state [" + currentRunState + "] while stopAdapter() command is issued, ignoring command");
				return;
			}
			if (currentRunState.equals(RunStateEnum.ERROR)) {
				runState.setRunState(RunStateEnum.STOPPED);
				return;
			}

            runState.setRunState(RunStateEnum.STOPPING);
        }
        taskExecutor.execute(new Runnable() {
            public void run() {
                Thread.currentThread().setName("stopping Adapter " +getName());
                try {
                    log.debug("Adapter [" + name + "] is stopping receivers");
                    Iterator it = receivers.iterator();
                    while (it.hasNext()) {
                        IReceiver receiver = (IReceiver) it.next();
                        try {
                            receiver.stopRunning();
                            log.info("Adapter [" + name + "] successfully stopped receiver [" + receiver.getName() + "]");

                        }
                        catch (Exception e) {
                            error(false, "received error while stopping receiver [" + receiver.getName() + "], ignoring this, so watch out.", e);
                        }
                    }

                    // stop the adapter
                    log.debug("***stopping adapter");
                    it = receivers.iterator();
                    while (it.hasNext()) {
                        IReceiver receiver = (IReceiver) it.next();
                        receiver.waitForRunState(RunStateEnum.STOPPED);
                        log.info("Adapter [" + getName() + "] stopped [" + receiver.getName() + "]");
                    }

                    int currentNumOfMessagesInProcess = getNumOfMessagesInProcess();
                    if (currentNumOfMessagesInProcess > 0) {
                        String msg =
                            "Adapter ["
                                + name
                                + "] is being stopped while still processing "
                                + currentNumOfMessagesInProcess
                                + " messages, waiting for them to finish";
                        warn(msg);
                    }
                    waitForNoMessagesInProcess();
                    log.debug("Adapter [" + name + "] is stopping pipeline");
                    pipeline.stop();
                    runState.setRunState(RunStateEnum.STOPPED);
                    getMessageKeeper().add("Adapter stopped");
                } catch (Throwable e) {
                    log.error("error running adapter [" + getName() + "] [" + ToStringBuilder.reflectionToString(e) + "]", e);
                    runState.setRunState(RunStateEnum.ERROR);
                }
            } // End of run()
        }); // End of Runnable
	}
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[name=" + name + "]");
		//sb.append("[version=" + version + "]");
		sb.append("[targetDesignDocument=" + targetDesignDocument + "]");
		Iterator it = receivers.iterator();
		sb.append("[receivers=");
		while (it.hasNext()) {
			IReceiver receiver = (IReceiver) it.next();
			sb.append(" " + receiver.getName());

		}
		sb.append("]");
		sb.append(
			"[pipeLine="
				+ ((pipeline != null) ? pipeline.toString() : "none registered")
				+ "]"
				+ "[started="
				+ getRunState()
				+ "]");

		return sb.toString();
	}
	public void waitForNoMessagesInProcess() throws InterruptedException {
		synchronized (statsMessageProcessingDuration) {
			while (getNumOfMessagesInProcess() > 0) {
				wait();
			}
		}
	}
	public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
		runState.waitForRunState(requestedRunState);
	}
	public boolean waitForRunState(RunStateEnum requestedRunState, long maxWait) throws InterruptedException {
		return runState.waitForRunState(requestedRunState, maxWait);
	}

	/**
	 * AutoStart indicates that the adapter should be started when the configuration
	 * is started. AutoStart defaults to <code>true</code>
	 * @since 4.1.1
	 */
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}
	public boolean isAutoStart() {
		return autoStart;
	}
	
	public void setRequestReplyLogging(boolean requestReplyLogging) {
		//this.requestReplyLogging = requestReplyLogging;
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		if (requestReplyLogging) {
			String msg = "Adapter [" + getName() + "] implementing setting of requestReplyLogging=true as msgLogLevel=Terse";
			configWarnings.add(log, msg);
			setMsgLogLevelNum(MsgLogUtil.MSGLOG_LEVEL_TERSE);
		} else {
			String msg = "Adapter [" + getName() + "] implementing setting of requestReplyLogging=false as msgLogLevel=None";
			configWarnings.add(log, msg);
			setMsgLogLevelNum(MsgLogUtil.MSGLOG_LEVEL_NONE);
		}
	}
/*
	public boolean isRequestReplyLogging() {
		return requestReplyLogging;
	}
*/

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
    public String getBeanName() {
        return name;
    }

	public void setMsgLogLevel(String level) throws ConfigurationException {
		msgLogLevel = MsgLogUtil.getMsgLogLevelNum(level);
		if (msgLogLevel<0) {
			throw new ConfigurationException("illegal value for msgLogLevel ["+level+"]");
		}
	}

	public String getMsgLogLevel() {
		return MsgLogUtil.getMsgLogLevelString(msgLogLevel);
	}

	public void setMsgLogLevelNum(int i) {
		msgLogLevel = i;
	}

	private boolean isMsgLogTerseEnabled() {
		if (msgLogLevel>=MsgLogUtil.MSGLOG_LEVEL_TERSE) {
			return true;
		} 
		return false;
	}
}

