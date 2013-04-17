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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.cache.ICacheEnabled;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.processors.PipeLineProcessor;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.SizeStatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;

/**
 * Processor and keeper of a line of {@link IPipe Pipes}.
 * <br/>
 * Pipelines also generate statics information per Pipe and keep forwards, that are registered
 * at individual pipes during the configure fase.
 * <br/>
 * In the AppConstants there may be a property named "log.logIntermediaryResults" (true/false)
 * which indicates wether the intermediary results (between calling pipes) have to be logged.
 *
 * * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>name of the class, mostly a class that extends this class</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFirstPipe(String) firstPipe}</td><td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted} <i>deprecated</i></td><td>if set to <code>true, messages will be processed under transaction control. (see below)</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setCommitOnState(String) commitOnState}</td><td>If the pipelineResult.getState() equals this value, the transaction is committed, otherwise it is rolled back.</td><td><code>success</code></td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are:
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>Supports</td></tr>
 * <tr><td>{@link #setTransactionTimeout(int) transactionTimeout}</td><td>Timeout (in seconds) of transaction started to process a message.</td><td><code>0</code> (use system default)</code></td></tr>
 * <tr><td>{@link #setStoreOriginalMessageWithoutNamespaces(boolean) storeOriginalMessageWithoutNamespaces}</td><td>when set <code>true</code> the original message without namespaces (and prefixes) is stored under the session key originalMessageWithoutNamespaces</td><td>false</td></tr>
 * <tr><td>{@link #setMessageSizeWarn(String) messageSizeWarn}</td><td>if messageSizeWarn>=0 and the size of the input or result pipe message exceeds the value specified a warning message is logged</td><td>application default (1MB)</td></tr>
 * <tr><td>{@link #setMessageSizeError(String) messageSizeError}</td><td>if messageSizeError>=0 and the size of the input or result pipe message exceeds the value specified an error message is logged</td><td>application default (10MB)</td></tr>
 * <tr><td>{@link #setForceFixedForwarding(boolean) forceFixedForwarding}</td><td>forces that each pipe in the pipeline is not automatically added to the globalForwards table</td><td>application default</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>&lt;exits&gt; one or more {@link nl.nn.adapterframework.core.PipeLineExit exits}&lt;/exits&gt;</td><td>specifications of exit-paths, in the form &lt;exit path="<i>forwardname</i>" state="<i>statename</i>"/&gt;</td></tr>
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
 * @version $Id$
 * @author  Johan Verrips
 */
public class PipeLine implements ICacheEnabled, HasStatistics {
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

    private Map<String, IPipe> pipesByName = new Hashtable<String, IPipe>(); // needless synchronization?
    private List<IPipe> pipes              = new ArrayList<IPipe>();
    // set of exits paths with their state
    private Map<String, PipeLineExit> pipeLineExits = new Hashtable<String, PipeLineExit>();

	private String commitOnState = "success"; // exit state on which receiver will commit XA transactions
	private boolean storeOriginalMessageWithoutNamespaces = false;
	private long messageSizeWarn  = Misc.getMessageSizeWarnByDefault();
	private long messageSizeError = Misc.getMessageSizeErrorByDefault();
	private boolean forceFixedForwarding = Misc.isForceFixedForwardingByDefault();

	private List<IPipeLineExitHandler> exitHandlers = new ArrayList<IPipeLineExitHandler>();
	//private CongestionSensorList congestionSensors = new CongestionSensorList();
	private ICacheAdapter cache;


	/**
	 * Register an Pipe at this pipeline.
	 * The name is also put in the globalForwards table (with
	 * forward-name=pipename and forward-path=pipename, so that
	 * pipe can look for a specific pipe-name. If already a globalForward
	 * exists under that name, the pipe is NOT added, allowing globalForwards
	 * to prevail.
	 * @see nl.nn.adapterframework.pipes.AbstractPipe
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
		pipeStatistics.put(name, new StatisticsKeeper(name));
		if (pipe instanceof MessageSendingPipe) {
			MessageSendingPipe messageSendingPipe = (MessageSendingPipe)pipe;
			if (messageSendingPipe.getInputValidator() != null) {
				String subName = messageSendingPipe.getInputValidator().getName();
				pipeStatistics.put(subName, new StatisticsKeeper(subName));
			}
			if (messageSendingPipe.getOutputValidator() != null) {
				String subName = messageSendingPipe.getOutputValidator().getName();
				pipeStatistics.put(subName, new StatisticsKeeper(subName));
			}
			if (messageSendingPipe.getInputWrapper() != null) {
				String subName = messageSendingPipe.getInputWrapper().getName();
				pipeStatistics.put(subName, new StatisticsKeeper(subName));
			}
			if (messageSendingPipe.getOutputWrapper() != null) {
				String subName = messageSendingPipe.getOutputWrapper().getName();
				pipeStatistics.put(subName, new StatisticsKeeper(subName));
			}
			if (messageSendingPipe.getMessageLog() != null) {
				String subName = messageSendingPipe.getMessageLog().getName();
				pipeStatistics.put(subName, new StatisticsKeeper(subName));
			}
		}
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

			String pipeName=pipe.getName();
			log.debug("Pipeline of [" + owner.getName() + "] configuring Pipe ["+pipeName+"]");
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

			try {
				if (pipe instanceof IExtendedPipe) {
					IExtendedPipe epipe=(IExtendedPipe)pipe;
					epipe.configure(this);
					if (epipe.getDurationThreshold() >= 0) {
						epipe.registerEvent(IExtendedPipe.LONG_DURATION_MONITORING_EVENT);
					}
					epipe.registerEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
					if (getMessageSizeErrorNum() >= 0) {
						epipe.registerEvent(IExtendedPipe.MESSAGE_SIZE_MONITORING_EVENT);
					}
					if (epipe.hasSizeStatistics()) {
						pipeSizeStats.put(pipe.getName(), new SizeStatisticsKeeper(pipe.getName()));
					}
				} else {
					pipe.configure();
				}
				//congestionSensors.addSensor(pipe);
			} catch (Throwable t) {
				if (t instanceof ConfigurationException) {
					throw (ConfigurationException)t;
				}
				throw new ConfigurationException("Exception configuring Pipe ["+pipeName+"]",t);
			}
			if (log.isDebugEnabled()) {
                log.debug("Pipeline of [" + owner.getName() + "]: Pipe ["+pipeName+"] successfully configured: ["+pipe.toString()+"]");
            }

		}
	    if (pipeLineExits.size() < 1) {
		    throw new ConfigurationException("no PipeLine Exits specified");
	    }
	    if (this.firstPipe == null) {
		    throw new ConfigurationException("no firstPipe defined");
	    }
	    if (getPipe(firstPipe) == null) {
		    throw new ConfigurationException("no pipe found for firstPipe [" + firstPipe + "]");
	    }

		if (getInputValidator() != null) {
			log.debug("Pipeline of [" + owner.getName() + "] configuring InputValidator");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			getInputValidator().registerForward(pf);
			getInputValidator().setName(INPUT_VALIDATOR_NAME);
			if (getInputValidator() instanceof IExtendedPipe) {
				((IExtendedPipe)getInputValidator()).configure(this);
			} else {
				getInputValidator().configure();
			}
		    pipeStatistics.put(getInputValidator().getName(), new StatisticsKeeper(getInputValidator().getName()));
		}
		if (getOutputValidator()!=null) {
			log.debug("Pipeline of [" + owner.getName() + "] configuring OutputValidator");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			getOutputValidator().registerForward(pf);
			getOutputValidator().setName(OUTPUT_VALIDATOR_NAME);
			if (adapter!=null && getOutputValidator() instanceof IExtendedPipe) {
				((IExtendedPipe)getOutputValidator()).configure(this);
			} else {
				getOutputValidator().configure();
			}
		    pipeStatistics.put(getOutputValidator().getName(), new StatisticsKeeper(getOutputValidator().getName()));
		}

		if (getInputWrapper()!=null) {
			log.debug("Pipeline of [" + owner.getName() + "] configuring InputWrapper");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			getInputWrapper().registerForward(pf);
			getInputWrapper().setName(INPUT_WRAPPER_NAME);
			if (getInputWrapper() instanceof IExtendedPipe) {
				((IExtendedPipe)getInputWrapper()).configure(this);
			} else {
				getInputWrapper().configure();
			}
		    pipeStatistics.put(getInputWrapper().getName(), new StatisticsKeeper(getInputWrapper().getName()));
		}
		if (getOutputWrapper()!=null) {
			log.debug("Pipeline of [" + owner.getName() + "] configuring OutputWrapper");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			getOutputWrapper().registerForward(pf);
			getOutputWrapper().setName(OUTPUT_WRAPPER_NAME);
			if (adapter!=null && getOutputWrapper() instanceof IExtendedPipe) {
				((IExtendedPipe)getOutputWrapper()).configure(this);
			} else {
				getOutputWrapper().configure();
			}
		    pipeStatistics.put(getOutputWrapper().getName(), new StatisticsKeeper(getOutputWrapper().getName()));
		}

		requestSizeStats = new SizeStatisticsKeeper("- pipeline in");

		int txOption = this.getTransactionAttributeNum();
		if (log.isDebugEnabled()) log.debug("creating TransactionDefinition for transactionAttribute ["+getTransactionAttribute()+"], timeout ["+getTransactionTimeout()+"]");
		txDef = SpringTxManagerProxy.getTransactionDefinition(txOption,getTransactionTimeout());
		log.debug("Pipeline of [" + owner.getName() + "] successfully configured");
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
			handlePipeStat(pipe, pipeSizeStats, sizeStatsData, hski, false, action);
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
	public PipeLineResult process(String messageId, String message, IPipeLineSession pipeLineSession) throws PipeRunException {
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
	    pipeLineExits.put(exit.getPath(), exit);
    }

	public void setPipeLineProcessor(PipeLineProcessor pipeLineProcessor) {
		this.pipeLineProcessor = pipeLineProcessor;
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
    * The indicator for the end of the processing, with default state "undefined".
    * @deprecated since v 3.2 this functionality is superseded by the use of {@link nl.nn.adapterframework.core.PipeLineExit PipeLineExits}.
    * @see PipeLineExit
    */
    public void setEndPath(String endPath){
	    PipeLineExit te=new PipeLineExit();
	    te.setPath(endPath);
	    te.setState("undefined");
		registerPipeLineExit(te);
    }
    /**
     * set the name of the first pipe to execute when a message is to be
     * processed
     * @param pipeName the name of the pipe
     * @see nl.nn.adapterframework.pipes.AbstractPipe
     */
    public void setFirstPipe(String pipeName){
        firstPipe=pipeName;
    }
	public void start() throws PipeStartException {
	    log.info("Pipeline of [" + owner.getName() + "] is starting pipeline");

		if (cache!=null) {
		    log.debug("Pipeline of [" + owner.getName() + "] starting cache");
			cache.open();
		}

		for (int i=0; i<pipes.size(); i++) {
			IPipe pipe = getPipe(i);
			String pipeName = pipe.getName();

			log.debug("Pipeline of [" + owner.getName() + "] starting pipe [" + pipeName+"]");
			pipe.start();
			log.debug("Pipeline of [" + owner.getName() + "] successfully started pipe [" + pipeName + "]");
		}
	    log.info("Pipeline of [" + owner.getName() + "] is successfully started pipeline");

	}

	/**
	 * Close the pipeline. This will call the <code>stop()</code> method
	 * of all registered <code>Pipes</code>
	 * @see IPipe#stop
	 */
	public void stop() {
	    log.info("Pipeline of [" + owner.getName() + "] is closing pipeline");
		for (int i=0; i<pipes.size(); i++) {
			IPipe pipe = getPipe(i);
			String pipeName = pipe.getName();

			log.debug("Pipeline of [" + owner.getName() + "] is stopping [" + pipeName+"]");
			pipe.stop();
			log.debug("Pipeline of [" + owner.getName() + "] successfully stopped pipe [" + pipeName + "]");
		}

		if (cache!=null) {
		    log.debug("Pipeline of [" + owner.getName() + "] closing cache");
			cache.close();
		}
	    log.debug("Pipeline of [" + owner.getName() + "] successfully closed pipeline");

	}

    /**
     *
     * @return an enumeration of all pipenames in the pipeline and the
     * startpipe and endpath
     * @see #setEndPath
     * @see #setFirstPipe
     */
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


	public void setTransacted(boolean transacted) {
//		this.transacted = transacted;
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		if (transacted) {
			String msg = "Pipeline of [" + owner.getName() + "] implementing setting of transacted=true as transactionAttribute=Required";
			configWarnings.add(log, msg);
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_REQUIRED);
		} else {
			String msg = "Pipeline of [" + owner.getName() + "] implementing setting of transacted=false as transactionAttribute=Supports";
			configWarnings.add(log, msg);
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_SUPPORTS);
		}
	}
	/**
	 * the exit state of the pipeline on which the receiver will commit the transaction.
	 */
	public void setCommitOnState(String string) {
		commitOnState = string;
	}
	public String getCommitOnState() {
		return commitOnState;
	}


	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			throw new ConfigurationException("illegal value for transactionAttribute ["+attribute+"]");
		}
	}
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}

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

	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
	public int getTransactionTimeout() {
		return transactionTimeout;
	}

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
	public void setMessageSizeWarn(String s) {
		messageSizeWarn = Misc.toFileSize(s, messageSizeWarn + 1);
	}
	public long getMessageSizeWarnNum() {
		return messageSizeWarn;
	}

	/**
	 * The <b>MessageSizeError</b> option takes a long
	 * integer in the range 0 - 2^63. You can specify the value
	 * with the suffixes "KB", "MB" or "GB" so that the integer is
	 * interpreted being expressed respectively in kilobytes, megabytes
	 * or gigabytes. For example, the value "10KB" will be interpreted
	 * as 10240.
	 */
	public void setMessageSizeError(String s) {
		messageSizeError = Misc.toFileSize(s, messageSizeError + 1);
	}
	public long getMessageSizeErrorNum() {
		return messageSizeError;
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

	public void registerCache(ICacheAdapter cache) {
		this.cache=cache;
	}
	public ICacheAdapter getCache() {
		return cache;
	}

	public void setForceFixedForwarding(boolean b) {
		forceFixedForwarding = b;
	}
	public boolean isForceFixedForwarding() {
		return forceFixedForwarding;
	}

	public StatisticsKeeper getRequestSizeStats() {
		return requestSizeStats;
	}

}
