/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.cache.ICache;
import nl.nn.adapterframework.cache.ICacheEnabled;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.processors.PipeLineProcessor;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.SizeStatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.Misc;

/**
 * Required in each {@link Adapter} to transform incoming messages. A pipeline
 * is a sequence of pipes. A
 * pipeline also defines its allowed end states using the <code>&lt;Exits&gt;</code>
 * tag.
 * <br/><br/>
 * The pipes in a {@link PipeLine} may not be executed in sequential order, see {@link PipeForward}.
 * <br/><br/>
 * A pipeline gathers statistics about the messages it processes.
 * <br/><br/>
 * In the AppConstants there may be a property named <code>log.logIntermediaryResults</code> (true/false)
 * which indicates whether the intermediary results (between calling pipes) have to be logged.
 * <br/><br/>
 * <b>Transaction control</b><br/><br/>
 * THE FOLLOWING TO BE UPDATED, attribute 'transacted' replaced by 'transactionAttribute'
 *
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be processed
 * under transaction control. Processing by XA-compliant pipes (i.e. Pipes that implement the
 * IXAEnabled-interface, set their transacted-attribute to <code>true</code> and use XA-compliant
 * resources) will then either be committed or rolled back in one transaction.
 *
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, either an existing transaction
 * (started by a transactional receiver) is joined, or new one is created (if the message processing request
 * is not initiated by a receiver under transaction control.
 * Messages are only committed or rolled back by the Pipeline if it started the transaction itself. If
 * the pipeline joined an existing transaction, the commit or rollback is left to the object that started
 * the transaction, i.e. the receiver. In the latter case the pipeline can indicate to the receiver that the
 * transaction should be rolled back (by calling UserTransaction.setRollBackOnly()).
 *
 * The choice whether to either commit (by Pipeline or Receiver) or rollback (by Pipeline or Receiver)
 * is made as follows:
 *
 * If the processing of the message concluded without exceptions and the status of the transaction is
 * STATUS_ACTIVE (i.e. normal) the transaction will be committed. Otherwise it will be rolled back,
 * or marked for roll back by the calling party.
 *
 * @author  Johan Verrips
 */
@Category("Basic")
public class PipeLine extends TransactionAttributes implements ICacheEnabled<String,String>, HasStatistics, IConfigurationAware {
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	public static final String INPUT_VALIDATOR_NAME  = "- pipeline inputValidator";
	public static final String OUTPUT_VALIDATOR_NAME = "- pipeline outputValidator";
	public static final String INPUT_WRAPPER_NAME    = "- pipeline inputWrapper";
	public static final String OUTPUT_WRAPPER_NAME   = "- pipeline outputWrapper";

	// If you edit this default exit, please update the JavaDoc of class PipeLineExits as well.
	private static final String DEFAULT_SUCCESS_EXIT_NAME = "READY";

	private @Getter String firstPipe;
	private @Getter int maxThreads = 0;
	private @Getter boolean storeOriginalMessageWithoutNamespaces = false;
	private long messageSizeWarn  = Misc.getMessageSizeWarnByDefault();
	private Message transformNullMessage = null;
	private @Getter String adapterToRunBeforeOnEmptyInput = null;

	private @Getter IValidator inputValidator  = null;
	private @Getter IValidator outputValidator = null;
	private @Getter IWrapperPipe inputWrapper    = null;
	private @Getter IWrapperPipe outputWrapper   = null;
	private @Getter Map<String, PipeLineExit> pipeLineExits = new LinkedHashMap<String, PipeLineExit>();
	private @Getter Map<String, PipeForward> globalForwards = new Hashtable<String, PipeForward>();
	private @Getter Locker locker;
	private @Getter ICache<String,String> cache;

	private Map<String, IPipe> pipesByName = new LinkedHashMap<String, IPipe>();
	private @Getter List<IPipe> pipes	  = new ArrayList<IPipe>();

	private @Getter Adapter adapter;    // for transaction managing
	private @Getter INamedObject owner; // for logging purposes
	private @Setter PipeLineProcessor pipeLineProcessor;

	private Map<String, StatisticsKeeper> pipeStatistics = new Hashtable<String, StatisticsKeeper>(); // needless synchronization?
	private Map<String, StatisticsKeeper> pipeWaitingStatistics = new Hashtable<String, StatisticsKeeper>();
	private @Getter StatisticsKeeper requestSizeStats;
	private Map<String, StatisticsKeeper> pipeSizeStats = new Hashtable<String, StatisticsKeeper>();


	private @Getter List<IPipeLineExitHandler> exitHandlers = new ArrayList<IPipeLineExitHandler>();
	//private CongestionSensorList congestionSensors = new CongestionSensorList();

	private boolean configurationSucceeded = false;
	private boolean inputMessageConsumedMultipleTimes=false;

	public enum ExitState {
		SUCCESS,
		ERROR,
		REJECTED;
	}

	public IPipe getPipe(String pipeName) {
		return pipesByName.get(pipeName);
	}
	public IPipe getPipe(int index) {
		return pipes.get(index);
	}

	public void registerExitHandler(IPipeLineExitHandler exitHandler) {
		exitHandlers.add(exitHandler);
		log.info("registered exithandler ["+exitHandler.getName()+"]");
	}

	/**
	 * Configures the pipes of this Pipeline and does some basic checks. It also
	 * registers the <code>PipeLineSession</code> object at the pipes.
	 * @see IPipe
	 */
	@Override
	public void configure() throws ConfigurationException {
		INamedObject owner = getOwner();
		Adapter adapter = null;
		if (owner instanceof Adapter) {
			adapter = (Adapter)owner;
		}
		if (cache != null) {
			cache.configure(owner.getName() + "-Pipeline");
		}
		if (pipeLineExits.size() < 1) {
			// if no Exits are configured, then insert a default one, named 'READY', with state 'SUCCESS'
			PipeLineExit defaultExit = new PipeLineExit();
			defaultExit.setName(DEFAULT_SUCCESS_EXIT_NAME);
			defaultExit.setState(ExitState.SUCCESS);
			registerPipeLineExit(defaultExit);
			log.debug("Created default Exit named ["+defaultExit.getName()+"], state ["+defaultExit.getState()+"]");
		}
		for (int i=0; i < pipes.size(); i++) {
			IPipe pipe = getPipe(i);

			log.debug(getLogPrefix()+"configuring Pipe ["+pipe.getName()+"]");
			if (pipe instanceof FixedForwardPipe) {
				FixedForwardPipe ffpipe = (FixedForwardPipe)pipe;
				// getSuccessForward will return null if it has not been set. See below configure(pipe)
				if (ffpipe.findForward(PipeForward.SUCCESS_FORWARD_NAME) == null) {
					int i2 = i + 1;
					if (i2 < pipes.size()) {
						String nextPipeName = getPipe(i2).getName();
						PipeForward pf = new PipeForward();
						pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
						pf.setPath(nextPipeName);
						pipe.registerForward(pf);
					} else {
						PipeLineExit plexit = findExitByState(ExitState.SUCCESS);
						if (plexit == null) {
							// if there is no success exit, then appearantly only error exits are configured; Just get the first configured one
							plexit = pipeLineExits.values().iterator().next();
						}
						if (plexit != null) {
							PipeForward pf = new PipeForward();
							pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
							pf.setPath(plexit.getName());
							pipe.registerForward(pf);
						}
					}
				}
			}
			configure(pipe);
		}
		if (pipes.isEmpty()) {
			throw new ConfigurationException("no Pipes in PipeLine");
		}
		if (this.firstPipe == null) {
			firstPipe=pipes.get(0).getName();
		}
		if (getPipe(firstPipe) == null) {
			throw new ConfigurationException("no pipe found for firstPipe [" + firstPipe + "]");
		}

		IValidator inputValidator = getInputValidator();
		IValidator outputValidator = getOutputValidator();
		if (inputValidator!=null && outputValidator==null && inputValidator instanceof IDualModeValidator) {
			outputValidator=((IDualModeValidator)inputValidator).getResponseValidator();
			setOutputValidator(outputValidator);
		}
		if (inputValidator != null) {
			log.debug(getLogPrefix()+"configuring InputValidator");
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
			inputValidator.registerForward(pf);
			inputValidator.setName(INPUT_VALIDATOR_NAME);
			configure(inputValidator);
		}
		if (outputValidator!=null) {
			log.debug(getLogPrefix()+"configuring OutputValidator");
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
			outputValidator.registerForward(pf);
			outputValidator.setName(OUTPUT_VALIDATOR_NAME);
			configure(outputValidator);
		}

		if (getInputWrapper()!=null) {
			log.debug(getLogPrefix()+"configuring InputWrapper");
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
			getInputWrapper().registerForward(pf);
			getInputWrapper().setName(INPUT_WRAPPER_NAME);
			configure(getInputWrapper());
		}
		if (getOutputWrapper()!=null) {
			log.debug(getLogPrefix()+"configuring OutputWrapper");
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);

			getOutputWrapper().registerForward(pf);
			getOutputWrapper().setName(OUTPUT_WRAPPER_NAME);
			if (getOutputWrapper() instanceof EsbSoapWrapperPipe) {
				EsbSoapWrapperPipe eswPipe = (EsbSoapWrapperPipe)getOutputWrapper();
				for (Receiver<?> receiver: adapter.getReceivers()) {
					IListener<?> listener = receiver.getListener();
					try {
						if (eswPipe.retrievePhysicalDestinationFromListener(listener)) {
							break;
						}
					} catch (JmsException e) {
						throw new ConfigurationException(e);
					}
				}
			}
			configure(getOutputWrapper());
		}
		if (getLocker()!=null) {
			log.debug(getLogPrefix()+"configuring Locker");
			getLocker().configure();
		}

		requestSizeStats = new SizeStatisticsKeeper("- pipeline in");

		for(IPipe p:pipes) {
			if (p.consumesSessionVariable("originalMessage")) {
				inputMessageConsumedMultipleTimes = true;
				break;
			}
		}

		super.configure();
		log.debug(getLogPrefix()+"successfully configured");
		configurationSucceeded = true;
	}

	public void configure(IPipe pipe) throws ConfigurationException {
		try {
			if (pipe instanceof IExtendedPipe) {
				IExtendedPipe epipe=(IExtendedPipe)pipe;
				epipe.setPipeLine(this); //Temporary here because of validators and wrappers

				if (epipe.getDurationThreshold() >= 0) {
					epipe.registerEvent(IExtendedPipe.LONG_DURATION_MONITORING_EVENT);
				}
				epipe.registerEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
				if (getMessageSizeWarnNum() >= 0) {
					epipe.registerEvent(IExtendedPipe.MESSAGE_SIZE_MONITORING_EVENT);
				}
				if (epipe.hasSizeStatistics()) {
					if (pipe instanceof AbstractPipe) {
						AbstractPipe aPipe = (AbstractPipe) pipe;
						if (aPipe.getInSizeStatDummyObject() != null) {
							pipeSizeStats.put(aPipe.getInSizeStatDummyObject().getName(), new SizeStatisticsKeeper(aPipe.getInSizeStatDummyObject().getName()));
						}
						if (aPipe.getOutSizeStatDummyObject() != null) {
							pipeSizeStats.put(aPipe.getOutSizeStatDummyObject().getName(), new SizeStatisticsKeeper(aPipe.getOutSizeStatDummyObject().getName()));
						}
					} else {
						pipeSizeStats.put(pipe.getName(), new SizeStatisticsKeeper(pipe.getName()));
					}
				}
			}

			pipe.configure();

			for(PipeForward forward : pipe.getForwards().values()) {
				String path=forward.getPath();
				if (path!=null) {
					PipeLineExit plExit= getPipeLineExits().get(path);
					if (plExit==null && getPipe(path)==null){
						ConfigurationWarnings.add(pipe, log, "has a forward of which the pipe to execute ["+path+"] is not defined");
					}
				}
			}

			if (pipe instanceof MessageSendingPipe) {
				MessageSendingPipe messageSendingPipe = (MessageSendingPipe) pipe;
				if (messageSendingPipe.getMessageLog() != null) {
					pipeStatistics.put(messageSendingPipe.getMessageLog().getName(), new StatisticsKeeper(messageSendingPipe.getMessageLog().getName()));
				}
			}
			pipeStatistics.put(pipe.getName(), new StatisticsKeeper(pipe.getName()));
			//congestionSensors.addSensor(pipe);
		} catch (Throwable t) {
			throw new ConfigurationException("Exception configuring "+ ClassUtils.nameOf(pipe),t);
		}
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"pipe ["+pipe.getName()+"] successfully configured: ["+pipe.toString()+"]");
		}
	}

	public boolean configurationSucceeded() {
		return configurationSucceeded;
	}

	public PipeLineExit findExitByState(ExitState state) {
		for (String exitPath : pipeLineExits.keySet()) {
			PipeLineExit pe = pipeLineExits.get(exitPath);
			if (pe.getState()==state) {
				return pe;
			}
		}
		return null;
	}

	/**
	 * @return the number of pipes in the pipeline
	 */
	public int getPipeLineSize(){
		return pipesByName.size();
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		Object pipeStatsData = hski.openGroup(data, null, "duration");
		handlePipeStat(getInputValidator(),pipeStatistics,pipeStatsData, hski, true, action);
		handlePipeStat(getOutputValidator(),pipeStatistics,pipeStatsData, hski, true, action);
		handlePipeStat(getInputWrapper(),pipeStatistics,pipeStatsData, hski, true, action);
		handlePipeStat(getOutputWrapper(),pipeStatistics,pipeStatsData, hski, true, action);
		for (IPipe pipe : adapter.getPipeLine().getPipes()) {
			handlePipeStat(pipe, pipeStatistics, pipeStatsData, hski, true, action);
			if (pipe instanceof MessageSendingPipe) {
				MessageSendingPipe messageSendingPipe = (MessageSendingPipe) pipe;
				if (messageSendingPipe.getInputValidator() != null) {
					handlePipeStat(messageSendingPipe.getInputValidator(), pipeStatistics, pipeStatsData, hski, true, action);
				}
				if (messageSendingPipe.getOutputValidator() != null) {
					handlePipeStat(messageSendingPipe.getOutputValidator(), pipeStatistics, pipeStatsData, hski, true, action);
				}
				if (messageSendingPipe.getInputWrapper() != null) {
					handlePipeStat(messageSendingPipe.getInputWrapper(), pipeStatistics, pipeStatsData, hski, true, action);
				}
				if (messageSendingPipe.getOutputWrapper() != null) {
					handlePipeStat(messageSendingPipe.getOutputWrapper(), pipeStatistics, pipeStatsData, hski, true, action);
				}
				if (messageSendingPipe.getMessageLog() != null) {
					handlePipeStat(messageSendingPipe.getMessageLog(),pipeStatistics,pipeStatsData,hski, true, action);
				}
			}
		}
		if (pipeWaitingStatistics.size() > 0) {
			Object waitStatsData = hski.openGroup(data, null, "waitStats");
			for (IPipe pipe : adapter.getPipeLine().getPipes()) {
				handlePipeStat(pipe, pipeWaitingStatistics, waitStatsData, hski, false, action);
			}
		}
		hski.closeGroup(pipeStatsData);
		Object sizeStatsData = hski.openGroup(data, null,"size");
		hski.handleStatisticsKeeper(sizeStatsData,getRequestSizeStats());
		for (IPipe pipe : adapter.getPipeLine().getPipes()) {
			if (pipe instanceof AbstractPipe) {
				AbstractPipe aPipe = (AbstractPipe) pipe;
				if (aPipe.getInSizeStatDummyObject() != null) {
					handlePipeStat(aPipe.getInSizeStatDummyObject(), pipeSizeStats, sizeStatsData, hski, false, action);
				}
				if (aPipe.getOutSizeStatDummyObject() != null) {
					handlePipeStat(aPipe.getOutSizeStatDummyObject(), pipeSizeStats, sizeStatsData, hski, false, action);
				}
			} else {
				handlePipeStat(pipe, pipeSizeStats, sizeStatsData, hski, false, action);
			}
		}
		hski.closeGroup(sizeStatsData);
	}

	private void handlePipeStat(INamedObject pipe, Map<String, StatisticsKeeper> pipelineStatistics, Object pipeStatsData, StatisticsKeeperIterationHandler handler, boolean deep, Action action) throws SenderException {
		if (pipe == null || pipe.getName()==null) {
			return;
		}
		StatisticsKeeper pstat = pipelineStatistics.get(pipe.getName());
		handler.handleStatisticsKeeper(pipeStatsData,pstat);
		if (deep && pipe instanceof HasStatistics) {
			((HasStatistics)pipe).iterateOverStatistics(handler,pipeStatsData,action);
		}
	}

	public StatisticsKeeper getPipeStatistics(INamedObject pipe){
		return pipeStatistics.get(pipe.getName());
	}
	public StatisticsKeeper getPipeWaitingStatistics(IPipe pipe){
		return pipeWaitingStatistics.get(pipe.getName());
	}
	public StatisticsKeeper getPipeSizeStatistics(IPipe pipe){
		return pipeSizeStats.get(pipe.getName());
	}
	public StatisticsKeeper getPipeSizeStatistics(INamedObject no){
		return pipeSizeStats.get(no.getName());
	}



	/**
	 * The <code>process</code> method does the processing of a message.<br/>
	 * It retrieves the first pipe to execute from the <code>firstPipe</code field,
	 * the call results in a PipRunResult, containing the next pipe to activate.
	 * While processing the process method keeps statistics.
	 * @param message The message as received from the Listener
	 * @param messageId A unique id for this message, used for logging purposes.
	 * @return the result of the processing.
	 * @throws PipeRunException when something went wrong in the pipes.
	 */
	public PipeLineResult process(String messageId, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
		if (transformNullMessage != null && message.isEmpty()) {
			message = transformNullMessage;
		} else {
			if (inputMessageConsumedMultipleTimes) {
				try {
					message.preserve();
				} catch (IOException e) {
					throw new PipeRunException(null, "Cannot preserve inputMessage", e);
				}
			}
		}
		return pipeLineProcessor.processPipeLine(this, messageId, message, pipeLineSession, firstPipe);
	}


	/**
	 * Find the destination of the forward, i.e. the {@link IForwardTarget object} (Pipe or PipeLineExit) where the forward points to.
	 */
	public IForwardTarget resolveForward(IPipe pipe, PipeForward forward) throws PipeRunException {
		if (forward==null){
			throw new PipeRunException(pipe, "Pipeline of ["+getOwner().getName()+"] got a null forward from pipe ["+pipe.getName()+"].");
		}
		String path = forward.getPath();
		if (StringUtils.isEmpty(path)){
			throw new PipeRunException(pipe, "Pipeline of ["+getOwner().getName()+"] got a forward ["+forward.getName()+"] with a path that equals null or has a zero-length value from pipe ["+pipe.getName()+"]. Check the configuration, probably forwards are not defined for this pipe.");
		}
		PipeLineExit plExit= getPipeLineExits().get(path);
		if (plExit != null ) {
			return plExit;
		}
		IPipe nextPipe=getPipe(path);
		if (nextPipe==null) {
			throw new PipeRunException(pipe, "Pipeline of adapter ["+ getOwner().getName()+"] got an erroneous definition from pipe ["+pipe.getName()+"]. Target to execute ["+path+ "] is not defined as a Pipe or an Exit.");
		}
		return nextPipe;
	}

	/**
	 * Register the adapterName of this Pipelineprocessor.
	 * @param adapter
	 */
	public void setAdapter(Adapter adapter) {
		this.adapter = adapter;
		setOwner(adapter);
	}

	public void setOwner(INamedObject owner) {
		this.owner = owner;
	}


	public void start() throws PipeStartException {
		log.info(getLogPrefix()+"is starting pipeline");

		if (cache!=null) {
			log.debug(getLogPrefix()+"starting cache");
			cache.open();
		}

		if (getInputWrapper()!=null) {
			log.debug(getLogPrefix()+"starting InputWrapper ["+getInputWrapper().getName()+"]");
			getInputWrapper().start();
		}

		if (getInputValidator()!=null) {
			log.debug(getLogPrefix()+"starting InputValidator ["+getInputValidator().getName()+"]");
			getInputValidator().start();
		}

		if (getOutputValidator()!=null) {
			log.debug(getLogPrefix()+"starting OutputValidator ["+getOutputValidator().getName()+"]");
			getOutputValidator().start();
		}

		if (getOutputWrapper()!=null) {
			log.debug(getLogPrefix()+"starting OutputWrapper ["+getOutputWrapper().getName()+"]");
			getOutputWrapper().start();
		}

		for (int i=0; i<pipes.size(); i++) {
			IPipe pipe = getPipe(i);
			String pipeName = pipe.getName();

			log.debug(getLogPrefix()+"starting pipe [" + pipeName+"]");
			pipe.start();
			log.debug(getLogPrefix()+"successfully started pipe [" + pipeName + "]");
		}

		log.info(getLogPrefix()+"is successfully started pipeline");
	}

	/**
	 * Close the pipeline. This will call the <code>stop()</code> method
	 * of all registered <code>Pipes</code>
	 * @see IPipe#stop
	 */
	public void stop() {
		log.info(getLogPrefix()+"is closing pipeline");

		if (getInputWrapper()!=null) {
			log.debug(getLogPrefix()+"stopping InputWrapper ["+getInputWrapper().getName()+"]");
			getInputWrapper().stop();
		}

		if (getInputValidator()!=null) {
			log.debug(getLogPrefix()+"stopping InputValidator ["+getInputValidator().getName()+"]");
			getInputValidator().stop();
		}

		if (getOutputValidator()!=null) {
			log.debug(getLogPrefix()+"stopping OutputValidator ["+getOutputValidator().getName()+"]");
			getOutputValidator().stop();
		}

		if (getOutputWrapper()!=null) {
			log.debug(getLogPrefix()+"stopping OutputWrapper ["+getOutputWrapper().getName()+"]");
			getOutputWrapper().stop();
		}

		for (int i=0; i<pipes.size(); i++) {
			IPipe pipe = getPipe(i);
			String pipeName = pipe.getName();

			log.debug(getLogPrefix()+"is stopping [" + pipeName+"]");
			pipe.stop();
			log.debug(getLogPrefix()+"successfully stopped pipe [" + pipeName + "]");
		}

		if (cache!=null) {
			log.debug(getLogPrefix()+"closing cache");
			cache.close();
		}
		log.debug(getLogPrefix()+"successfully closed pipeline");

	}


	@Override
	public String getName() {
		String name = "PipeLine";
		if(owner != null) {
			name += " of [" + owner.getName() + "]";
		}
		return name;
	}

	private String getLogPrefix() {
		return getName() + " ";
	}

	/**
	 *
	 * @return an enumeration of all pipenames in the pipeline and the
	 * startpipe and endpath
	 * @see #setFirstPipe
	 */
	@Override
	public String toString(){
		// TODO: Should use StringBuilder
		String result = "";
		result+="[ownerName="+(owner==null ? "-none-" : owner.getName())+"]";
		result+="[adapterName="+(adapter==null ? "-none-" : adapter.getName())+"]";
		result+="[startPipe="+firstPipe+"]";
//		result+="[transacted="+transacted+"]";
		result+="[transactionAttribute="+getTransactionAttribute()+"]";
		for (int i=0; i<pipes.size(); i++) {
			result+="pipe"+i+"=["+getPipe(i).getName()+"]";
		}
		for (String exitName : pipeLineExits.keySet()) {
			PipeLineExit pe = pipeLineExits.get(exitName);
			result += "[name:" + pe.getName() + " state:" + pe.getState() + "]";
		}
		return result;
	}


	/** Request validator, or combined validator for request and response */
	public void setInputValidator(IValidator inputValidator) {
		this.inputValidator = inputValidator;
	}

	/** Optional pipe to validate the response. Can be specified if the response cannot be validated by the request validator */
	public void setOutputValidator(IValidator outputValidator) {
		this.outputValidator = outputValidator;
	}

	/** Optional pipe to extract the request message from its envelope */
	public void setInputWrapper(IWrapperPipe inputWrapper) {
		this.inputWrapper = inputWrapper;
	}

	/** Optional pipe to wrap the response message in an envelope */
	public void setOutputWrapper(IWrapperPipe outputWrapper) {
		this.outputWrapper = outputWrapper;
	}

	@IbisDoc({ "PipeLine exits. If no exits are specified, a default one is created with name=\""+DEFAULT_SUCCESS_EXIT_NAME+"\" and state=\"SUCCESS\""})
	public void setPipeLineExits(PipeLineExits exits) {
		for(PipeLineExit exit:exits.getExits()) {
			registerPipeLineExit(exit);
		}
	}

	/**
	 * PipeLine exits.
	 */
	@Deprecated
	public void registerPipeLineExit(PipeLineExit exit) {
		if (pipeLineExits.containsKey(exit.getName())) {
			ConfigurationWarnings.add(this, log, "exit named ["+exit.getName()+"] already exists");
		}
		if (exit.getExitCode()>0) {
			for(PipeLineExit item: pipeLineExits.values()) {
				if (item.getExitCode()==exit.getExitCode()) {
					ConfigurationWarnings.add(this, log, getLogPrefix()+"exit ["+exit.getName()+"] has code ["+exit.getExitCode()+"] that is already defined. Only the first exit ["+item.getName()+"] with this code will be represented in OpenAPI schema when it is generated");
					break;
				}
			}
		}
		pipeLineExits.put(exit.getName(), exit);
	}

	/** Global forwards */
	public void setGlobalForwards(PipeForwards forwards){
		for(PipeForward forward:forwards.getForwards()) {
			registerForward(forward);
		}
	}

	@Deprecated
	public void registerForward(PipeForward forward){
		globalForwards.put(forward.getName(), forward);
		log.debug("registered global PipeForward "+forward.toString());
	}

	/**
	 * Optional Locker, to avoid parallel execution of the PipeLine by multiple threads on multiple servers.
	 * The Pipeline is NOT executed (and is considered to have ended successfully) when the lock cannot be obtained,
	 * e.g. in case another thread, may be in another server, holds the lock and does not release it in a timely manner.
	 * If only the number of threads executing this PipeLine needs to be limited, the attribute maxThreads can be set instead, avoiding the database overhead.
	 */
	public void setLocker(Locker locker) {
		this.locker = locker;
	}

	/** Cache of results */
	@Override
	public void setCache(ICache<String,String> cache) {
		this.cache=cache;
	}

	/**
	 * Register an Pipe at this pipeline.
	 * The name is also put in the globalForwards table (with
	 * forward-name=pipename and forward-path=pipename, so that
	 * pipe can look for a specific pipe-name. If already a globalForward
	 * exists under that name, the pipe is NOT added, allowing globalForwards
	 * to prevail.
	 * @see AbstractPipe
	 * @ff.mandatory
	 **/
	public void addPipe(IPipe pipe) throws ConfigurationException {
		if (pipe == null) {
			throw new ConfigurationException("pipe to be added is null, pipelineTable size [" + pipesByName.size() + "]");
		}
		String name = pipe.getName();
		if (StringUtils.isEmpty(name)) {
			throw new ConfigurationException("pipe [" + ClassUtils.nameOf(pipe)+"] to be added has no name, pipelineTable size ["+pipesByName.size()+"]");
		}
		IPipe current = getPipe(name);
		if (current != null) {
			throw new ConfigurationException("pipe [" + name + "] defined more then once");
		}
		pipesByName.put(name, pipe);
		pipes.add(pipe);
		if (pipe.getMaxThreads() > 0) {
			pipeWaitingStatistics.put(name, new StatisticsKeeper(name));
		}
		log.debug("added pipe [" + pipe.toString() + "]");
	}

	/**
	 * Name of the first pipe to execute when a message is to be processed
	 * @ff.default first pipe of the pipeline
	 */
	public void setFirstPipe(String pipeName) {
		firstPipe = pipeName;
	}

	/**
	 * Maximum number of threads that may execute this Pipeline simultaneously, use 0 to disable limit
	 * @ff.default 0
	 */
	public void setMaxThreads(int newMaxThreads) {
		maxThreads = newMaxThreads;
	}

	/**
	 * If set <code>true</code> the original message without namespaces (and prefixes) is stored under the session key originalMessageWithoutNamespaces
	 * @ff.default false
	 */
	public void setStoreOriginalMessageWithoutNamespaces(boolean b) {
		storeOriginalMessageWithoutNamespaces = b;
	}

	/**
	 * If messageSizeWarn>=0 and the size of the input or result pipe message exceeds the value specified a warning message is logged. You can specify the value with the suffixes <code>KB</code>, <code>MB</code> or <code>GB</code>
	 * @ff.default application default (3MB)
	 */
	public void setMessageSizeWarn(String s) {
		messageSizeWarn = Misc.toFileSize(s, messageSizeWarn + 1);
	}
	public long getMessageSizeWarnNum() {
		return messageSizeWarn;
	}

	/** when specified and <code>null</code> is received as a message the message is changed to the specified value */
	public void setTransformNullMessage(String s) {
		transformNullMessage = new Message(s);
	}

	/** when specified and an empty message is received the specified adapter is run before passing the message (response from specified adapter) to the pipeline */
	public void setAdapterToRunBeforeOnEmptyInput(String s) {
		adapterToRunBeforeOnEmptyInput = s;
	}

}
