/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.cache.ICacheEnabled;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.processors.PipeLineProcessor;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.SizeStatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

/**
 * Processor and keeper of a line of {@link IPipe Pipes}.
 * <br/>
 * Pipelines also generate statics information per Pipe and keep forwards, that are registered
 * at individual pipes during the configure fase.
 * <br/>
 * In the AppConstants there may be a property named "log.logIntermediaryResults" (true/false)
 * which indicates wether the intermediary results (between calling pipes) have to be logged.
 *
 * <tr><td>{@link #setStoreOriginalMessageWithoutNamespaces(boolean) storeOriginalMessageWithoutNamespaces}</td><td>when set <code>true</code> the original message without namespaces (and prefixes) is stored under the session key originalMessageWithoutNamespaces</td><td>false</td></tr>
 * <tr><td>{@link #setMessageSizeWarn(String) messageSizeWarn}</td><td>if messageSizeWarn>=0 and the size of the input or result pipe message exceeds the value specified a warning message is logged</td><td>application default (3MB)</td></tr>
 * <tr><td>{@link #setForceFixedForwarding(boolean) forceFixedForwarding}</td><td>forces that each pipe in the pipeline is not automatically added to the globalForwards table</td><td>application default</td></tr>
 * <tr><td>{@link #setTransformNullMessage(String) transformNullMessage}</td><td>when specified and <code>null</code> is received as a message the message is changed to the specified value</td><td></td></tr>
 * <tr><td>{@link #setAdapterToRunBeforeOnEmptyInput(String) adapterToRunBeforeOnEmptyInput}</td><td>when specified and an empty message is received the specified adapter is run before passing the message (response from specified adapter) to the pipeline</td><td></td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>&lt;exits&gt; one or more {@link PipeLineExit exits}&lt;/exits&gt;</td><td>specifications of exit-paths, in the form &lt;exit path="<i>forwardname</i>" state="<i>statename</i>"/&gt;</td></tr>
 * <tr><td>&lt;inputValidator&gt;</td><td>specification of Pipe to validate input messages</td></tr>
 * <tr><td>&lt;outputValidator&gt;</td><td>specification of Pipe to validate output messages</td></tr>
 * <tr><td>&lt;inputWrapper&gt;</td><td>specification of Pipe to wrap input messages (after validating)</td></tr>
 * <tr><td>&lt;outputWrapper&gt;</td><td>specification of Pipe to wrap output messages (before validating)</td></tr>
 * <tr><td>&lt;cache ... /&gt;</td><td>optional {@link nl.nn.adapterframework.cache.EhCache cache} definition</td></tr>
 * </table>
 * </p>
 *
 * <p><b>Transaction control</b><br>
 * THE FOLLOWING TO BE UPDATED, attribute 'transacted' replaced by 'transactionAttribute'
 *
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be processed
 * under transaction control. Processing by XA-compliant pipes (i.e. Pipes that implement the
 * IXAEnabled-interface, set their transacted-attribute to <code>true</code> and use XA-compliant
 * resources) will then either be commited or rolled back in one transaction.
 *
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, either an existing transaction
 * (started by a transactional receiver) is joined, or new one is created (if the messsage processing request
 * is not initated by a receiver under transaction control.
 * Messages are only committed or rolled back by the Pipeline if it started the transaction itself. If
 * the pipeline joined an exisiting transaction, the commit or rollback is left to the object that started
 * the transaction, i.e. the receiver. In the latter case the pipeline can indicate to the receiver that the
 * transaction should be rolled back (by calling UserTransaction.setRollBackOnly()).
 *
 * The choice whether to either commit (by Pipeline or Receiver) or rollback (by Pipeline or Receiver)
 * is made as follows:
 *
 * If the processing of the message concluded without exceptions and the status of the transaction is
 * STATUS_ACTIVE (i.e. normal) the transaction will be committed. Otherwise it will be rolled back,
 * or marked for roll back by the calling party.

 * </p>
 * 
 * @author  Johan Verrips
 */
public class PipeLine implements ICacheEnabled<String,String>, HasStatistics {
    private Logger log = LogUtil.getLogger(this);

	private PipeLineProcessor pipeLineProcessor;

	private Adapter adapter;    // for transaction managing
	private INamedObject owner; // for logging purposes

    private Map<String, StatisticsKeeper> pipeStatistics = new Hashtable<String, StatisticsKeeper>(); // needless synchronization?
    private Map<String, StatisticsKeeper> pipeWaitingStatistics = new Hashtable<String, StatisticsKeeper>();
	private StatisticsKeeper requestSizeStats;
	private Map<String, StatisticsKeeper> pipeSizeStats = new Hashtable<String, StatisticsKeeper>();

	private Map<String, PipeForward> globalForwards = new Hashtable<String, PipeForward>();
    private String firstPipe;
	private int transactionAttribute = TransactionDefinition.PROPAGATION_SUPPORTS;
	private int transactionTimeout   = 0;

	private Locker locker;

	public final static String INPUT_VALIDATOR_NAME  = "- pipeline inputValidator";
	public final static String OUTPUT_VALIDATOR_NAME = "- pipeline outputValidator";
	public final static String INPUT_WRAPPER_NAME    = "- pipeline inputWrapper";
	public final static String OUTPUT_WRAPPER_NAME   = "- pipeline outputWrapper";

	private IPipe inputValidator  = null;
	private IPipe outputValidator = null;
	private IPipe inputWrapper    = null;
	private IPipe outputWrapper   = null;

	private TransactionDefinition txDef = null;

    private Map<String, IPipe> pipesByName = new LinkedHashMap<String, IPipe>();
    private List<IPipe> pipes              = new ArrayList<IPipe>();
    // set of exits paths with their state
    private Map<String, PipeLineExit> pipeLineExits = new LinkedHashMap<String, PipeLineExit>();

	private String commitOnState = "success"; // exit state on which receiver will commit XA transactions
	private boolean storeOriginalMessageWithoutNamespaces = false;
	private long messageSizeWarn  = Misc.getMessageSizeWarnByDefault();
	private boolean forceFixedForwarding = Misc.isForceFixedForwardingByDefault();
	private Message transformNullMessage = null;
	private String adapterToRunBeforeOnEmptyInput = null;

	private List<IPipeLineExitHandler> exitHandlers = new ArrayList<IPipeLineExitHandler>();
	//private CongestionSensorList congestionSensors = new CongestionSensorList();
	private ICacheAdapter<String,String> cache;


	/**
	 * Register an Pipe at this pipeline.
	 * The name is also put in the globalForwards table (with
	 * forward-name=pipename and forward-path=pipename, so that
	 * pipe can look for a specific pipe-name. If already a globalForward
	 * exists under that name, the pipe is NOT added, allowing globalForwards
	 * to prevail.
	 * @see AbstractPipe
	 **/
	public void addPipe(IPipe pipe) throws ConfigurationException {
		if (pipe == null) {
			throw new ConfigurationException("pipe to be added is null, pipelineTable size [" + pipesByName.size() + "]");
		}
		if (pipe instanceof IExtendedPipe && !((IExtendedPipe)pipe).isActive()) {
			log.debug("Pipe [" + pipe.getName() + "] is not active, therefore not included in configuration");
			return;
		}
		String name = pipe.getName();
		if (StringUtils.isEmpty(name)) {
			throw new ConfigurationException("pipe [" + pipe.getClass().getName()+"] to be added has no name, pipelineTable size ["+pipesByName.size()+"]");
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
		if (!isForceFixedForwarding())
		{
			if (globalForwards.get(name) == null) {
				PipeForward pw = new PipeForward();
				pw.setName(name);
				pw.setPath(name);
				registerForward(pw);
			} else {
				log.info("already had a pipeForward with name ["+ name+ "] skipping the implicit one to Pipe ["+ pipe.getName()+ "]");
			}
		}
	}

	public IPipe getPipe(String pipeName) {
		return pipesByName.get(pipeName);
	}
	public IPipe getPipe(int index) {
		return pipes.get(index);
	}

	public List<IPipe> getPipes() {
		return pipes;
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
	public void configure() throws ConfigurationException {
		INamedObject owner = getOwner();
		IAdapter adapter = null;
		if (owner instanceof IAdapter) {
			adapter = (IAdapter)owner;
		}
		if (cache != null) {
			cache.configure(owner.getName() + "-Pipeline");
		}
		for (int i=0; i < pipes.size(); i++) {
			IPipe pipe = getPipe(i);

			log.debug(getLogPrefix()+"configuring Pipe ["+pipe.getName()+"]");
			// register the global forwards at the Pipes
			// the pipe will take care that if a local, pipe-specific
			// forward is defined, it is not overwritten by the globals
			for (String gfName : globalForwards.keySet()) {
				PipeForward pipeForward = globalForwards.get(gfName);
				pipe.registerForward(pipeForward);
			}

			if (pipe instanceof FixedForwardPipe) {
				FixedForwardPipe ffpipe = (FixedForwardPipe)pipe;
				if (ffpipe.findForward("success") == null) {
					int i2 = i + 1;
					if (i2 < pipes.size()) {
						String nextPipeName = getPipe(i2).getName();
						PipeForward pf = new PipeForward();
						pf.setName("success");
						pf.setPath(nextPipeName);
						pipe.registerForward(pf);
					} else {
						PipeLineExit plexit = findExitByState("success");
						if (plexit != null) {
							PipeForward pf = new PipeForward();
							pf.setName("success");
							pf.setPath(plexit.getPath());
							pipe.registerForward(pf);
						}
					}
				}
			}
			configure(pipe);
		}
		if (pipeLineExits.size() < 1) {
			throw new ConfigurationException("no PipeLine Exits specified");
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

		IPipe inputValidator = getInputValidator();
		IPipe outputValidator = getOutputValidator();
		if (inputValidator!=null && outputValidator==null && inputValidator instanceof IDualModeValidator) {
			outputValidator=((IDualModeValidator)inputValidator).getResponseValidator();
			setOutputValidator(outputValidator);
		}
		if (inputValidator != null) {
			log.debug(getLogPrefix()+"configuring InputValidator");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			inputValidator.registerForward(pf);
			inputValidator.setName(INPUT_VALIDATOR_NAME);
			configure(inputValidator);
		}
		if (outputValidator!=null) {
			log.debug(getLogPrefix()+"configuring OutputValidator");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			outputValidator.registerForward(pf);
			outputValidator.setName(OUTPUT_VALIDATOR_NAME);
			configure(outputValidator);
		}

		if (getInputWrapper()!=null) {
			log.debug(getLogPrefix()+"configuring InputWrapper");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			getInputWrapper().registerForward(pf);
			getInputWrapper().setName(INPUT_WRAPPER_NAME);
			configure(getInputWrapper());
		}
		if (getOutputWrapper()!=null) {
			log.debug(getLogPrefix()+"configuring OutputWrapper");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			if (getOutputWrapper() instanceof AbstractPipe && adapter instanceof Adapter) {
				((AbstractPipe) getOutputWrapper()).setRecoverAdapter(((Adapter) adapter).isRecover());
			}
			getOutputWrapper().registerForward(pf);
			getOutputWrapper().setName(OUTPUT_WRAPPER_NAME);
			if (getOutputWrapper() instanceof EsbSoapWrapperPipe) {
				EsbSoapWrapperPipe eswPipe = (EsbSoapWrapperPipe)getOutputWrapper();
				boolean stop = false;
				Iterator<IReceiver> recIt = adapter.getReceiverIterator();
				if (recIt.hasNext()) {
					while (recIt.hasNext() && !stop) {
						IReceiver receiver = recIt.next();
						if (receiver instanceof ReceiverBase ) {
							ReceiverBase rb = (ReceiverBase) receiver;
							IListener listener = rb.getListener();
							try {
								if (eswPipe.retrievePhysicalDestinationFromListener(listener)) {
									stop = true;
								}
							} catch (JmsException e) {
							    throw new ConfigurationException(e);
							}
						}
					}
				}
			}
			configure(getOutputWrapper());
		}

		requestSizeStats = new SizeStatisticsKeeper("- pipeline in");

		if (isTransacted() && getTransactionTimeout()>0) {
			String systemTransactionTimeout = Misc.getSystemTransactionTimeout();
			if (systemTransactionTimeout!=null && StringUtils.isNumeric(systemTransactionTimeout)) {
				int stt = Integer.parseInt(systemTransactionTimeout);
				if (getTransactionTimeout()>stt) {
					ConfigurationWarnings.add(null, log, getLogPrefix()+"has a transaction timeout ["+getTransactionTimeout()+"] which exceeds the system transaction timeout ["+stt+"]");
				}
			}
		}

		int txOption = this.getTransactionAttributeNum();
		if (log.isDebugEnabled()) log.debug("creating TransactionDefinition for transactionAttribute ["+getTransactionAttribute()+"], timeout ["+getTransactionTimeout()+"]");
		txDef = SpringTxManagerProxy.getTransactionDefinition(txOption,getTransactionTimeout());
		log.debug(getLogPrefix()+"successfully configured");
	}

	public void configure(IPipe pipe) throws ConfigurationException {
		try {
			if (pipe instanceof IExtendedPipe) {
				IExtendedPipe epipe=(IExtendedPipe)pipe;
				epipe.configure(this);
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
			} else {
				pipe.configure();
			}
			if (pipe instanceof MessageSendingPipe) {
				MessageSendingPipe messageSendingPipe = (MessageSendingPipe) pipe;
				if (messageSendingPipe.getInputValidator() != null) {
					configure(messageSendingPipe.getInputValidator());
				}
				if (messageSendingPipe.getOutputValidator() != null) {
					configure(messageSendingPipe.getOutputValidator());
				}
				if (messageSendingPipe.getInputWrapper() != null) {
					configure(messageSendingPipe.getInputWrapper());
				}
				if (messageSendingPipe.getOutputWrapper() != null) {
					configure(messageSendingPipe.getOutputWrapper());
				}
				if (messageSendingPipe.getMessageLog() != null) {
					pipeStatistics.put(messageSendingPipe.getMessageLog().getName(), new StatisticsKeeper(messageSendingPipe.getMessageLog().getName()));
				}
			}
			pipeStatistics.put(pipe.getName(), new StatisticsKeeper(pipe.getName()));
			//congestionSensors.addSensor(pipe);
		} catch (Throwable t) {
			if (t instanceof ConfigurationException) {
				throw (ConfigurationException)t;
			}
			throw new ConfigurationException("Exception configuring Pipe ["+pipe.getName()+"]",t);
		}
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"pipe ["+pipe.getName()+"] successfully configured: ["+pipe.toString()+"]");
		}
	}

	public PipeLineExit findExitByState(String state) {
		for (String exitPath : pipeLineExits.keySet()) {
			PipeLineExit pe = pipeLineExits.get(exitPath);
			if (pe.getState().equals(state)) {
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
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		Object pipeStatsData = hski.openGroup(data, null, "pipeStats");
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
		Object sizeStatsData = hski.openGroup(data, null,"sizeStats");
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

	private void handlePipeStat(INamedObject pipe, Map<String, StatisticsKeeper> pipelineStatistics, Object pipeStatsData, StatisticsKeeperIterationHandler handler, boolean deep, int action) throws SenderException {
		if (pipe == null) {
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
	public StatisticsKeeper getPipeSizeStatistics(DummyNamedObject dno){
		return pipeSizeStats.get(dno.getName());
	}


//	public boolean isCongestionSensing() {
//		return congestionSensors.isCongestionSensing();
//	}
//
//	public INamedObject isCongested() throws SenderException {
//		return congestionSensors.isCongested();
//	}


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
	public PipeLineResult process(String messageId, Message message, IPipeLineSession pipeLineSession) throws PipeRunException {
		if (transformNullMessage != null && message.isEmpty()) {
			message = transformNullMessage;
		}
		return pipeLineProcessor.processPipeLine(this, messageId, message, pipeLineSession, firstPipe);
	}

	/**
	 * Register global forwards.
	 */
	public void registerForward(PipeForward forward){
		globalForwards.put(forward.getName(), forward);
		log.debug("registered global PipeForward "+forward.toString());
	}

	public void registerPipeLineExit(PipeLineExit exit) {
		if (pipeLineExits.containsKey(exit.getPath())) {
			ConfigurationWarnings.add(null, log, getLogPrefix()+"exit named ["+exit.getPath()+"] already exists");
		}
		pipeLineExits.put(exit.getPath(), exit);
	}

	public void setPipeLineProcessor(PipeLineProcessor pipeLineProcessor) {
		this.pipeLineProcessor = pipeLineProcessor;
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
			throw new PipeRunException(pipe, "Pipeline of ["+getOwner().getName()+"] got a path that equals null or has a zero-length value from pipe ["+pipe.getName()+"]. Check the configuration, probably forwards are not defined for this pipe.");
		}
		PipeLineExit plExit= getPipeLineExits().get(path);
		if (plExit != null ) {
			return plExit;
		}
		IPipe nextPipe=getPipe(path);
		if (nextPipe==null) {
			throw new PipeRunException(pipe, "Pipeline of adapter ["+ getOwner().getName()+"] got an erroneous definition from pipe ["+pipe.getName()+"]. Pipe to execute ["+path+ "] is not defined.");
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
	public Adapter getAdapter() {
		return adapter;
	}

	public void setOwner(INamedObject owner) {
		this.owner = owner;
	}
	public INamedObject getOwner() {
		return owner;
	}

	/**
	 * set the name of the first pipe to execute when a message is to be processed
	 * 
	 * @param pipeName the name of the pipe
	 * @see AbstractPipe
	 */
	@IbisDoc({ "name of the first pipe to execute when a message is to be processed", "" })
	public void setFirstPipe(String pipeName) {
		firstPipe = pipeName;
	}

	public void start() throws PipeStartException {
		log.info(getLogPrefix()+"is starting pipeline");

		if (cache!=null) {
			log.debug(getLogPrefix()+"starting cache");
			cache.open();
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
//        result+="[transacted="+transacted+"]";
		result+="[transactionAttribute="+getTransactionAttribute()+"]";
		for (int i=0; i<pipes.size(); i++) {
			result+="pipe"+i+"=["+getPipe(i).getName()+"]";
		}
        for (String exitPath : pipeLineExits.keySet()) {
            PipeLineExit pe = pipeLineExits.get(exitPath);
            result += "[path:" + pe.getPath() + " state:" + pe.getState() + "]";
        }
        return result;
    }


	@IbisDoc({"if set to <code>true, messages will be processed under transaction control. (see below)</code>", "<code>false</code>"})
	public void setTransacted(boolean transacted) {
//		this.transacted = transacted;
		if (transacted) {
			ConfigurationWarnings.add(null, log, getLogPrefix()+"implementing setting of transacted=true as transactionAttribute=Required");
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_REQUIRED);
		} else {
			ConfigurationWarnings.add(null, log, getLogPrefix()+"implementing setting of transacted=false as transactionAttribute=Supports");
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_SUPPORTS);
		}
	}

	public boolean isTransacted() {
//		return transacted;
		int txAtt = getTransactionAttributeNum();
		return  txAtt==TransactionDefinition.PROPAGATION_REQUIRED || 
				txAtt==TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				txAtt==TransactionDefinition.PROPAGATION_MANDATORY;
	}

	/**
	 * the exit state of the pipeline on which the receiver will commit the transaction.
	 */
	@IbisDoc({"if the pipelineresult.getstate() equals this value, the transaction is committed, otherwise it is rolled back.", "<code>success</code>"})
	public void setCommitOnState(String string) {
		commitOnState = string;
	}
	public String getCommitOnState() {
		return commitOnState;
	}

	@IbisDoc({"The <code>transactionAttribute</code> declares transactional behavior of pipeline execution. It "
		+ "applies both to database transactions and XA transactions."
        + "The pipeline uses this to start a new transaction or suspend the current one when required. "
		+ "For developers: it is equal"
        + "to <a href=\"http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494\">EJB transaction attribute</a>. "
        + "Possible values for transactionAttribute:"
        + "  <table border=\"1\">"
        + "    <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>"
        + "    <tr><td colspan=\"1\" rowspan=\"2\">Required</td>    <td>none</td><td>T2</td></tr>"
        + "											      <tr><td>T1</td>  <td>T1</td></tr>"
        + "    <tr><td colspan=\"1\" rowspan=\"2\">RequiresNew</td> <td>none</td><td>T2</td></tr>"
        + "											      <tr><td>T1</td>  <td>T2</td></tr>"
        + "    <tr><td colspan=\"1\" rowspan=\"2\">Mandatory</td>   <td>none</td><td>error</td></tr>"
        + "											      <tr><td>T1</td>  <td>T1</td></tr>"
        + "    <tr><td colspan=\"1\" rowspan=\"2\">NotSupported</td><td>none</td><td>none</td></tr>"
        + "											      <tr><td>T1</td>  <td>none</td></tr>"
        + "    <tr><td colspan=\"1\" rowspan=\"2\">Supports</td>    <td>none</td><td>none</td></tr>"
        + " 										      <tr><td>T1</td>  <td>T1</td></tr>"
        + "    <tr><td colspan=\"1\" rowspan=\"2\">Never</td>       <td>none</td><td>none</td></tr>"
        + "											      <tr><td>T1</td>  <td>error</td></tr>"
        + "  </table>", "Supports"})
	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			throw new ConfigurationException("illegal value for transactionAttribute ["+attribute+"]");
		}
	}
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}

    @IbisDoc({"Like <code>transactionAttribute</code>, but the chosen "
	    + "option is represented with a number. The numbers mean:"
	    + "<table>"
	    + "<tr><td>0</td><td>Required</td></tr>"
	    + "<tr><td>1</td><td>Supports</td></tr>"
	    + "<tr><td>2</td><td>Mandatory</td></tr>"
	    + "<tr><td>3</td><td>RequiresNew</td></tr>"
	    + "<tr><td>4</td><td>NotSupported</td></tr>"
	    + "<tr><td>5</td><td>Never</td></tr>"
	    + "</table>", "1"})
	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	public void setLocker(Locker locker) {
		this.locker = locker;
	}
	public Locker getLocker() {
		return locker;
	}

	public void setInputValidator(IPipe inputValidator) {
		this.inputValidator = inputValidator;
	}
	public IPipe getInputValidator() {
		return inputValidator;
	}

	public void setOutputValidator(IPipe outputValidator) {
		this.outputValidator = outputValidator;
	}
	public IPipe getOutputValidator() {
		return outputValidator;
	}

	public void setInputWrapper(IPipe inputWrapper) {
		this.inputWrapper = inputWrapper;
	}
	public IPipe getInputWrapper() {
		return inputWrapper;
	}

	public void setOutputWrapper(IPipe outputWrapper) {
		this.outputWrapper = outputWrapper;
	}
	public IPipe getOutputWrapper() {
		return outputWrapper;
	}

	@IbisDoc({"timeout (in seconds) of transaction started to process a message.", "<code>0</code> (use system default)"})
	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
	public int getTransactionTimeout() {
		return transactionTimeout;
	}

	@IbisDoc({"when set <code>true</code> the original message without namespaces (and prefixes) is stored under the session key originalmessagewithoutnamespaces", "false"})
	public void setStoreOriginalMessageWithoutNamespaces(boolean b) {
		storeOriginalMessageWithoutNamespaces = b;
	}
	public boolean isStoreOriginalMessageWithoutNamespaces() {
		return storeOriginalMessageWithoutNamespaces;
	}

	/**
	 * The <b>MessageSizeWarn</b> option takes a long
	 * integer in the range 0 - 2^63. You can specify the value
	 * with the suffixes "KB", "MB" or "GB" so that the integer is
	 * interpreted being expressed respectively in kilobytes, megabytes
	 * or gigabytes. For example, the value "10KB" will be interpreted
	 * as 10240.
	 */
	@IbisDoc({"if messagesizewarn>=0 and the size of the input or result pipe message exceeds the value specified a warning message is logged", "application default (3mb)"})
	public void setMessageSizeWarn(String s) {
		messageSizeWarn = Misc.toFileSize(s, messageSizeWarn + 1);
	}
	public long getMessageSizeWarnNum() {
		return messageSizeWarn;
	}

	public TransactionDefinition getTxDef() {
		return txDef;
	}

	public String getFirstPipe() {
		return firstPipe;
	}

	public Map<String, PipeLineExit> getPipeLineExits() {
		return pipeLineExits;
	}

	public List<IPipeLineExitHandler> getExitHandlers() {
		return exitHandlers;
	}

	@Override
	public void registerCache(ICacheAdapter<String,String> cache) {
		this.cache=cache;
	}
	@Override
	public ICacheAdapter<String,String> getCache() {
		return cache;
	}

	@IbisDoc({"forces that each pipe in the pipeline is not automatically added to the globalforwards table", "application default"})
	public void setForceFixedForwarding(boolean b) {
		forceFixedForwarding = b;
	}
	public boolean isForceFixedForwarding() {
		return forceFixedForwarding;
	}

	@IbisDoc({"when specified and <code>null</code> is received as a message the message is changed to the specified value", ""})
	public void setTransformNullMessage(String s) {
		transformNullMessage = new Message(s);
	}

	public StatisticsKeeper getRequestSizeStats() {
		return requestSizeStats;
	}

	@IbisDoc({"when specified and an empty message is received the specified adapter is run before passing the message (response from specified adapter) to the pipeline", ""})
	public void setAdapterToRunBeforeOnEmptyInput(String s) {
		adapterToRunBeforeOnEmptyInput = s;
	}
	public String getAdapterToRunBeforeOnEmptyInput() {
		return adapterToRunBeforeOnEmptyInput;
	}

	private String getLogPrefix() {
		String prefix = "PipeLine";
		if(owner != null) {
			prefix += " of [" + owner.getName() + "]";
		}
		return prefix + " ";
	}
}
