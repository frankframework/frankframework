/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.DistributionSummary;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.frankframework.cache.ICache;
import org.frankframework.cache.ICacheEnabled;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.doc.Category;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.processors.PipeLineProcessor;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.HasStatistics;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.Locker;
import org.frankframework.util.Misc;
import org.springframework.context.ApplicationContext;

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
	private @Getter final ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	public static final String INPUT_VALIDATOR_NAME  = "- pipeline inputValidator";
	public static final String OUTPUT_VALIDATOR_NAME = "- pipeline outputValidator";
	public static final String INPUT_WRAPPER_NAME    = "- pipeline inputWrapper";
	public static final String OUTPUT_WRAPPER_NAME   = "- pipeline outputWrapper";

	private @Setter MetricsInitializer configurationMetrics;

	public static final String PIPELINE_DURATION_STATS  = "duration";
	public static final String PIPELINE_WAIT_STATS  = "wait";
	public static final String PIPELINE_SIZE_STATS  = "msgsize";

	public static final String DEFAULT_SUCCESS_EXIT_NAME = "READY";

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
	private @Getter final Map<String, PipeLineExit> pipeLineExits = new LinkedHashMap<>();
	private @Getter final Map<String, PipeForward> globalForwards = new HashMap<>();
	private @Getter Locker locker;
	private @Getter ICache<String,String> cache;

	private final Map<String, IPipe> pipesByName = new LinkedHashMap<>();
	private @Getter final List<IPipe> pipes	  = new ArrayList<>();

	private @Getter Adapter adapter;    // for transaction managing
	private @Setter @Getter INamedObject owner; // for logging purposes
	private @Setter PipeLineProcessor pipeLineProcessor;

	private @Getter DistributionSummary requestSizeStats;
	private final Map<String, DistributionSummary> pipeStatistics = new ConcurrentHashMap<>();
	private @Getter DistributionSummary pipelineWaitStatistics;
	private final Map<String, DistributionSummary> pipeWaitStatistics = new ConcurrentHashMap<>();
	private final Map<String, DistributionSummary> pipeSizeStats = new ConcurrentHashMap<>();


	private @Getter final List<IPipeLineExitHandler> exitHandlers = new ArrayList<>();

	private boolean configurationSucceeded = false;
	private boolean inputMessageConsumedMultipleTimes=false;

	public enum ExitState {
		SUCCESS,
		ERROR,
		REJECTED;

		public static final String SUCCESS_EXIT_STATE = "SUCCESS";
	}

	public IPipe getPipe(String pipeName) {
		return pipesByName.get(pipeName);
	}
	public IPipe getPipe(int index) {
		return pipes.get(index);
	}

	public void registerExitHandler(IPipeLineExitHandler exitHandler) {
		exitHandlers.add(exitHandler);
		log.info("registered exithandler [{}]", exitHandler.getName());
	}

	/**
	 * Configures the pipes of this Pipeline and does some basic checks. It also
	 * registers the <code>PipeLineSession</code> object at the pipes.
	 * @see IPipe
	 */
	@Override
	public void configure() throws ConfigurationException {
		ConfigurationException configurationException = null;
		if (cache != null) {
			cache.configure(owner.getName() + "-Pipeline");
		}
		if (pipeLineExits.isEmpty()) {
			// if no Exits are configured, then insert a default one, named 'READY', with state 'SUCCESS'
			PipeLineExit defaultExit = new PipeLineExit();
			defaultExit.setName(DEFAULT_SUCCESS_EXIT_NAME);
			defaultExit.setState(ExitState.SUCCESS);
			registerPipeLineExit(defaultExit);
			log.debug("Created default Exit named [{}], state [{}]", defaultExit.getName(), defaultExit.getState());
		}
		for (int i=0; i < pipes.size(); i++) {
			IPipe pipe = getPipe(i);

			log.debug("configuring Pipe [{}]", pipe::getName);
			if (pipe instanceof FixedForwardPipe ffPipe) {
				// getSuccessForward will return null if it has not been set. See below configure(pipe)
				if (ffPipe.findForward(PipeForward.SUCCESS_FORWARD_NAME) == null) {
					int i2 = i + 1;
					if (i2 < pipes.size()) {
						// Forward to Next Pipe
						String nextPipeName = getPipe(i2).getName();
						PipeForward pf = new PipeForward();
						pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
						pf.setPath(nextPipeName);
						pipe.registerForward(pf);
					} else {
						// This is the last pipe, so forwards to a PipeLineExit
						PipeLineExit plExit = findExitByState(ExitState.SUCCESS)
								// if there is no success exit, then apparently only error exits are configured; Just get the first configured one
								.orElseGet( ()-> pipeLineExits.values().iterator().next());

						// By this point there is always at least 1 PipeLineExit, so plExit cannot be NULL.
						PipeForward pf = new PipeForward();
						pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
						pf.setPath(plExit.getName());
						pipe.registerForward(pf);
					}
				}
			}
			try {
				configure(pipe);
			} catch (ConfigurationException e) {
				configurationException = suppressException(configurationException, e);
			}
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
		if (outputValidator == null && inputValidator instanceof IDualModeValidator validator) {
			outputValidator = validator.getResponseValidator();
			setOutputValidator(outputValidator);
		}
		if (inputValidator != null) {
			log.debug("configuring InputValidator");
			configurationException = configureSpecialPipe(inputValidator, INPUT_VALIDATOR_NAME, configurationException);
		}
		if (outputValidator != null) {
			log.debug("configuring OutputValidator");
			configurationException = configureSpecialPipe(outputValidator, OUTPUT_VALIDATOR_NAME, configurationException);
		}

		IWrapperPipe inputWrapper = getInputWrapper();
		if (inputWrapper != null) {
			log.debug("configuring InputWrapper");
			configurationException = configureSpecialPipe(inputWrapper, INPUT_WRAPPER_NAME, configurationException);
		}
		IWrapperPipe outputWrapper = getOutputWrapper();
		if (outputWrapper != null) {
			log.debug("configuring OutputWrapper");
			if (outputWrapper instanceof DestinationValidator validator) {
				validator.validateListenerDestinations(this);
			}
			configurationException = configureSpecialPipe(outputWrapper, OUTPUT_WRAPPER_NAME, configurationException);
		}
		if (getLocker() != null) {
			log.debug("configuring Locker");
			getLocker().configure();
		}

		requestSizeStats = configurationMetrics.createDistributionSummary(this, FrankMeterType.PIPELINE_SIZE);
		pipelineWaitStatistics = configurationMetrics.createDistributionSummary(this, FrankMeterType.PIPELINE_WAIT_TIME);

		inputMessageConsumedMultipleTimes |= pipes.stream()
				.anyMatch(p -> p.consumesSessionVariable(PipeLineSession.ORIGINAL_MESSAGE_KEY));

		super.configure();
		log.debug("successfully configured");
		configurationSucceeded = true;
		if (configurationException != null) {
			throw configurationException;
		}
	}

	@Nullable
	private ConfigurationException configureSpecialPipe(@Nonnull final IPipe pipe, @Nonnull final String name, @Nullable final ConfigurationException configurationException) throws ConfigurationException {
		PipeForward pf = new PipeForward();
		pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
		pipe.registerForward(pf);
		pipe.setName(name);

		try {
			configure(pipe);
		} catch (ConfigurationException e) {
			return suppressException(configurationException, e);
		}
		return configurationException;
	}

	@Nonnull
	private static ConfigurationException suppressException(@Nullable final ConfigurationException configurationException, @Nonnull final ConfigurationException e) {
		if (configurationException == null) {
			return e;
		} else {
			configurationException.addSuppressed(e);
			return configurationException;
		}
	}

	public void configure(IPipe pipe) throws ConfigurationException {
		try (CloseableThreadContext.Instance ctc = CloseableThreadContext.put("pipe", pipe.getName())) {
			pipe.setPipeLine(this); //Temporary here because of validators and wrappers

			if (pipe.getDurationThreshold() >= 0) {
				pipe.registerEvent(IPipe.LONG_DURATION_MONITORING_EVENT);
			}
			pipe.registerEvent(IPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			if (getMessageSizeWarnNum() >= 0) {
				pipe.registerEvent(IPipe.MESSAGE_SIZE_MONITORING_EVENT);
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

		} catch (Throwable t) {
			ConfigurationException e = new ConfigurationException("Exception configuring "+ ClassUtils.nameOf(pipe),t);
			getAdapter().getMessageKeeper().error("Error initializing adapter ["+ getAdapter().getName()+"]: " +e.getMessage());
			throw e;
		}
		log.debug("Pipe successfully configured");
	}

	public boolean configurationSucceeded() {
		return configurationSucceeded;
	}

	public Optional<PipeLineExit> findExitByState(ExitState state) {
		return pipeLineExits.values()
				.stream()
				.filter(pe -> pe.getState() == state)
				.findFirst();
	}

	/**
	 * @return the number of pipes in the pipeline
	 */
	public int getPipeLineSize(){
		return pipesByName.size();
	}

	/*
	 * All pipe related statistics
	 */
	public @Nonnull DistributionSummary getPipeStatistics(IConfigurationAware pipe) {
		return pipeStatistics.computeIfAbsent(pipe.getName(), name -> configurationMetrics.createDistributionSummary(pipe, FrankMeterType.PIPE_DURATION));
	}
	public @Nonnull DistributionSummary getPipeWaitStatistics(IPipe pipe){
		return pipeWaitStatistics.computeIfAbsent(pipe.getName(), name -> configurationMetrics.createDistributionSummary(pipe, FrankMeterType.PIPE_WAIT_TIME));
	}
	public @Nonnull DistributionSummary getPipeSizeInStatistics(IPipe pipe) {
		return pipeSizeStats.computeIfAbsent(pipe.getName() + " (in)", name -> configurationMetrics.createDistributionSummary(pipe, FrankMeterType.PIPE_SIZE_IN));
	}
	public @Nonnull DistributionSummary getPipeSizeOutStatistics(IPipe pipe) {
		return pipeSizeStats.computeIfAbsent(pipe.getName() + " (out)", name -> configurationMetrics.createDistributionSummary(pipe, FrankMeterType.PIPE_SIZE_OUT));
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


	public void start() throws PipeStartException {
		log.info("starting pipeline");

		if (cache!=null) {
			log.debug("starting cache");
			cache.open();
		}

		startPipe("InputWrapper",getInputWrapper());
		startPipe("InputValidator",getInputValidator());
		startPipe("OutputValidator",getOutputValidator());
		startPipe("OutputWrapper",getOutputWrapper());

		for (int i=0; i<pipes.size(); i++) {
			startPipe("Pipe", getPipe(i));
		}

		log.info("successfully started pipeline");
	}

	protected void startPipe(String type, IPipe pipe) throws PipeStartException {
		if (pipe!=null) {
			try (CloseableThreadContext.Instance ctc = CloseableThreadContext.put("pipe", pipe.getName())) {
				log.debug("starting {}", type);
				pipe.start();
				log.debug("successfully started {}", type);
			}
		}
	}

	/**
	 * Close the pipeline. This will call the <code>stop()</code> method
	 * of all registered <code>Pipes</code>
	 * @see IPipe#stop
	 */
	public void stop() {
		log.info("is closing pipeline");

		stopPipe("InputWrapper", getInputWrapper());
		stopPipe("InputValidator", getInputValidator());
		stopPipe("OutputValidator", getOutputValidator());
		stopPipe("OutputWrapper", getOutputWrapper());

		for (int i=0; i<pipes.size(); i++) {
			stopPipe("Pipe", getPipe(i));
		}

		if (cache!=null) {
			log.debug("closing cache");
			cache.close();
		}
		log.debug("successfully closed pipeline");

	}

	protected void stopPipe(String type, IPipe pipe) {
		if (pipe!=null) {
			try (CloseableThreadContext.Instance ctc = CloseableThreadContext.put("pipe", pipe.getName())) {
				log.debug("stopping {}", type);
				pipe.stop();
				log.debug("successfully stopped {}", type);
			}
		}
	}

	@Override
	public String getName() {
		String name = "PipeLine";
		if(owner != null) {
			name += " of [" + owner.getName() + "]";
		}
		return name;
	}

	/**
	 *
	 * @return an enumeration of all pipenames in the pipeline and the
	 * startpipe and endpath
	 * @see #setFirstPipe
	 */
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		result.append("[ownerName=").append(owner == null ? "-none-" : owner.getName()).append("]");
		result.append("[adapterName=").append(adapter == null ? "-none-" : adapter.getName()).append("]");
		result.append("[startPipe=").append(firstPipe).append("]");
//		result+="[transacted="+transacted+"]";
		result.append("[transactionAttribute=").append(getTransactionAttribute()).append("]");
		for (int i=0; i<pipes.size(); i++) {
			result.append("pipe").append(i).append("=[").append(getPipe(i).getName()).append("]");
		}
		for (String exitName : pipeLineExits.keySet()) {
			PipeLineExit pe = pipeLineExits.get(exitName);
			result.append("[name:").append(pe.getName()).append(" state:").append(pe.getState()).append("]");
		}
		return result.toString();
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

	/** PipeLine exits. If no exits are specified, a default one is created with name={@value #DEFAULT_SUCCESS_EXIT_NAME} and state={@value PipeLine.ExitState#SUCCESS_EXIT_STATE} */
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
					ConfigurationWarnings.add(this, log, "exit ["+exit.getName()+"] has code ["+exit.getExitCode()+"] that is already defined. Only the first exit ["+item.getName()+"] with this code will be represented in OpenAPI schema when it is generated");
					break;
				}
			}
		}
		pipeLineExits.put(exit.getName(), exit);
	}

	/**
	 * Optional global forwards that will be added to every pipe, when the forward name has not been explicitly set.
	 * For example the <code>&lt;forward name="exception" path="error_exception" /&gt;</code>, which will add the <code>exception</code> forward to every pipe in the pipeline.
	 */
	public void setGlobalForwards(PipeForwards forwards){
		for(PipeForward forward: forwards.getForwards()) {
			registerForward(forward);
		}
	}

	@Deprecated
	public void registerForward(PipeForward forward) {
		globalForwards.put(forward.getName(), forward);
		log.debug("registered global PipeForward {}", forward);
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
	public void setCache(ICache<String, String> cache) {
		this.cache = cache;
	}

	/**
	 * Register a Pipe at this pipeline.
	 * The name is also put in the globalForwards table (with
	 * forward-name=pipename and forward-path=pipename, so that
	 * pipe can look for a specific pipe-name. If already a globalForward
	 * exists under that name, the pipe is NOT added, allowing globalForwards
	 * to prevail.
	 *
	 * @ff.mandatory
	 * @see AbstractPipe
	 **/
	public void addPipe(IPipe pipe) throws ConfigurationException {
		if (pipe == null) {
			throw new ConfigurationException("pipe to be added is null, pipelineTable size [" + pipesByName.size() + "]");
		}
		String name = pipe.getName();
		if (StringUtils.isEmpty(name)) {
			throw new ConfigurationException("pipe [" + ClassUtils.nameOf(pipe) + "] to be added has no name, pipelineTable size [" + pipesByName.size() + "]");
		}
		IPipe current = getPipe(name);
		if (current != null) {
			throw new ConfigurationException("pipe [" + name + "] defined more then once");
		}
		pipesByName.put(name, pipe);
		pipes.add(pipe);
		log.debug("added pipe [{}]", pipe);
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
	 * @ff.default application default (30MB)
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
	@Deprecated
	@ConfigurationWarning("Please use an XmlIf-pipe and call a sub-adapter to retrieve a new/different response")
	public void setAdapterToRunBeforeOnEmptyInput(String s) {
		adapterToRunBeforeOnEmptyInput = s;
	}

}
