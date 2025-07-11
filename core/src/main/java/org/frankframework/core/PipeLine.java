/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.context.ApplicationContext;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import lombok.Setter;

import org.frankframework.cache.ICache;
import org.frankframework.cache.ICacheEnabled;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationAware;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.doc.Category;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.doc.Mandatory;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.processors.PipeLineProcessor;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.Locker;
import org.frankframework.util.Misc;
import org.frankframework.util.StringUtil;

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
@Category(Category.Type.BASIC)
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class PipeLine extends TransactionAttributes implements ICacheEnabled<String,String>, FrankElement, ConfigurationAware, ConfigurableLifecycle {
	private @Getter ApplicationContext applicationContext;
	private @Getter @Setter Configuration configuration; // Required for the Ladybug
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	public static final String PIPELINE_NAME = "pipeline";
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

	private @Getter IValidator inputValidator = null;
	private @Getter IValidator outputValidator = null;
	private @Getter IWrapperPipe inputWrapper = null;
	private @Getter IWrapperPipe outputWrapper = null;
	private final Map<String, PipeLineExit> pipeLineExits = new LinkedHashMap<>();
	private final Map<String, PipeForward> globalForwards = new HashMap<>();
	private @Getter Locker locker;
	private @Getter ICache<String,String> cache;

	private final Map<String, IPipe> pipesByName = new LinkedHashMap<>();
	private final @Getter List<IPipe> pipes = new ArrayList<>();

	private @Getter Adapter adapter; // For transaction managing
	private @Setter PipeLineProcessor pipeLineProcessor;

	private @Getter DistributionSummary requestSizeStats;
	private final Map<String, DistributionSummary> pipeStatistics = new ConcurrentHashMap<>();
	private @Getter DistributionSummary pipelineWaitStatistics;
	private final Map<String, DistributionSummary> pipeWaitStatistics = new ConcurrentHashMap<>();
	private final Map<String, DistributionSummary> pipeSizeStats = new ConcurrentHashMap<>();

	private boolean inputMessageConsumedMultipleTimes=false;

	private @Getter String expectsSessionKeys;
	private Set<String> expectsSessionKeysSet;

	private boolean started = false;
	private @Getter boolean configured = false;

	public enum ExitState {
		SUCCESS,
		ERROR,
		REJECTED;

		public static final String SUCCESS_EXIT_STATE = "SUCCESS";
	}

	@Override
	public final void setApplicationContext(@Nonnull ApplicationContext context) {
		if (context instanceof Adapter contextAdapter) {
			this.adapter = contextAdapter;
		} else {
			throw new IllegalArgumentException("ApplicationContext must always be of type Adapter");
		}

		this.applicationContext = context;
	}

	/**
	 * Used by {@link MetricsInitializer} and {@link ConfigurationWarnings}.
	 * When null either the ClassName or nothing is used.
	 * 
	 * See PipeLineTest#testDuplicateExits, which right now does not add a name to the ConfigurationWarnings.
	 * Ideally it copies over the adapter name.
	 */
	@Override
	public String getName() {
		return null;
	}

	public IPipe getPipe(String pipeName) {
		return pipesByName.get(pipeName);
	}
	public IPipe getPipe(int index) {
		return pipes.get(index);
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
			cache.configure();
		}
		if (pipeLineExits.isEmpty()) {
			// if no Exits are configured, then insert a default one, named 'READY', with state 'SUCCESS'
			PipeLineExit defaultExit = new PipeLineExit();
			defaultExit.setName(DEFAULT_SUCCESS_EXIT_NAME);
			defaultExit.setState(ExitState.SUCCESS);
			addPipeLineExit(defaultExit);
			log.debug("Created default Exit named [{}], state [{}]", defaultExit.getName(), defaultExit.getState());
		}
		for (int i=0; i < pipes.size(); i++) {
			IPipe pipe = getPipe(i);

			log.debug("configuring Pipe [{}]", pipe::getName);
			if (pipe instanceof FixedForwardPipe ffPipe) {
				// getSuccessForward will return null if it has not been set. See below configure(pipe)
				if (!ffPipe.hasRegisteredForward(PipeForward.SUCCESS_FORWARD_NAME)) {
					int i2 = i + 1;
					if (i2 < pipes.size()) {
						// Forward to Next Pipe
						String nextPipeName = getPipe(i2).getName();
						PipeForward pf = new PipeForward();
						pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
						pf.setPath(nextPipeName);
						pipe.addForward(pf);
					} else {
						// This is the last pipe, so forwards to a PipeLineExit
						PipeLineExit plExit = findExitByState(ExitState.SUCCESS)
								// if there is no success exit, then apparently only error exits are configured; Just get the first configured one
								.orElseGet( ()-> pipeLineExits.values().iterator().next());

						// By this point there is always at least 1 PipeLineExit, so plExit cannot be NULL.
						PipeForward pf = new PipeForward();
						pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
						pf.setPath(plExit.getName());
						pipe.addForward(pf);
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
			throw new ConfigurationException("no Pipes in Pipeline");
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

		if (StringUtils.isNotBlank(expectsSessionKeys)) {
			expectsSessionKeysSet = StringUtil.splitToStream(expectsSessionKeys).collect(Collectors.toUnmodifiableSet());
		} else {
			expectsSessionKeysSet = Collections.emptySet();
		}

		super.configure();
		log.debug("successfully configured");
		configured = true;
		if (configurationException != null) {
			throw configurationException;
		}
	}

	@Nullable
	private ConfigurationException configureSpecialPipe(@Nonnull final IPipe pipe, @Nonnull final String name, @Nullable final ConfigurationException configurationException) throws ConfigurationException {
		PipeForward pf = new PipeForward();
		pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
		pipe.addForward(pf);
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
			pipe.setPipeLine(this); // Temporary here because of validators and wrappers

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
					PipeLineExit plExit = pipeLineExits.get(path);
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
	public @Nonnull DistributionSummary getPipeStatistics(IPipe pipe) {
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
	 * The {@code process} method does the processing of a message.<br/>
	 * It retrieves the first pipe to execute from the {@code firstPipe} field,
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
		if (!expectsSessionKeysSet.isEmpty()) {
			verifyExpectedSessionKeysPresent(pipeLineSession);
		}
		return pipeLineProcessor.processPipeLine(this, messageId, message, pipeLineSession, firstPipe);
	}

	private void verifyExpectedSessionKeysPresent(PipeLineSession session) throws PipeRunException {
		Set<String> missing = new HashSet<>(expectsSessionKeysSet);
		missing.removeAll(session.keySet());
		if (!missing.isEmpty()) {
			throw new PipeRunException(null, "Adapter [" + getAdapter().getName() + "] called without expected session keys " + missing);
		}
	}


	/**
	 * Find the destination of the forward, i.e. the {@link IForwardTarget object} (Pipe or PipeLineExit) where the forward points to.
	 */
	public IForwardTarget resolveForward(IPipe pipe, PipeForward forward) throws PipeRunException {
		if (forward==null){
			throw new PipeRunException(pipe, "Pipeline of [%s] got a null forward from pipe [%s].".formatted(adapter.getName(), pipe.getName()));
		}
		String path = forward.getPath();
		if (StringUtils.isEmpty(path)){
			throw new PipeRunException(pipe, "Pipeline of [%s] got a forward [%s] with a path that equals null or has a zero-length value from pipe [%s]. Check the configuration, probably forwards are not defined for this pipe.".formatted(adapter.getName(), forward.getName(), pipe.getName()));
		}
		PipeLineExit plExit = pipeLineExits.get(path);
		if (plExit != null ) {
			return plExit;
		}
		IPipe nextPipe=getPipe(path);
		if (nextPipe==null) {
			throw new PipeRunException(pipe, "Pipeline of [%s] got an erroneous definition from pipe [%s]. Target to execute [%s] is not defined as a Pipe or an Exit.".formatted(adapter.getName(), pipe.getName(), path));
		}
		return nextPipe;
	}

	@Override
	public void start() {
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
		started = true;
	}

	@Override
	public boolean isRunning() {
		return started;
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE; // Starts first, stops last
	}

	protected void startPipe(String type, IPipe pipe) {
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
	@Override
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
		started = false;
	}

	// Method may not be called getGlobalForwards, because of the FrankDoc...
	@Nullable
	public PipeForward findGlobalForward(String forward) {
		return globalForwards.get(forward);
	}

	// Method may not be called getPipeLineExits, because of the FrankDoc...
	public Map<String, PipeLineExit> getAllPipeLineExits() {
		return Collections.unmodifiableMap(pipeLineExits);
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

	/**
	 *
	 * @return an enumeration of all pipenames in the pipeline and the
	 * startpipe and endpath
	 * @see #setFirstPipe
	 */
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		result.append("[adapterName=").append(adapter == null ? "-none-" : adapter.getName()).append("]");
		result.append("[startPipe=").append(firstPipe).append("]");
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
			addPipeLineExit(exit);
		}
	}

	/**
	 * PipeLine exits.
	 */
	@Deprecated
	public void addPipeLineExit(PipeLineExit exit) {
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
	// Here for the FrankDoc documentation
	public void setGlobalForwards(PipeForwards forwards){
		for(PipeForward forward: forwards.getForwards()) {
			addForward(forward);
		}
	}

	@Deprecated
	public void addForward(PipeForward forward) {
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
	 * @see AbstractPipe
	 **/
	@Mandatory
	public void addPipe(IPipe pipe) throws ConfigurationException {
		if (pipe == null) {
			throw new ConfigurationException("pipe to be added is null, pipelineTable size [" + pipesByName.size() + "]");
		}
		String name = pipe.getName();
		if (StringUtils.isEmpty(name)) {
			throw new ConfigurationException("pipe [" + ClassUtils.nameOf(pipe) + "] to be added has no name, pipelineTable size [" + pipesByName.size() + "]");
		}
		if (getPipe(name) != null) {
			throw new ConfigurationException("pipe [" + name + "] defined more then once");
		}
		pipesByName.put(name, pipe);
		pipes.add(pipe);
		log.debug("added pipe [{}]", pipe);
	}

	/**
	 * Name of the first pipe to execute when a message is to be processed.
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

	/**
	 * The pipeline of this adapter expects to use the following session keys to be set on call. This
	 * is for adapters that are called as sub-adapters from other adapters. This serves both for documentation,
	 * so callers can see what session keys to set on call, and for verification that those session keys are present.
	 *
	 * @param expectsSessionKeys Session keys to set on call of the pipeline, comma-separated.
	 */
	public void setExpectsSessionKeys(String expectsSessionKeys) {
		this.expectsSessionKeys = expectsSessionKeys;
	}

}
