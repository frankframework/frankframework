/*
 * $Log: PipeLine.java,v $
 * Revision 1.15  2005-06-13 12:52:22  europe\L190409
 * prepare for nested pipelines
 *
 * Revision 1.14  2005/02/10 07:49:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed clearing of pipelinesession a start of pipeline
 *
 * Revision 1.13  2005/01/13 08:55:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Make threadContext-attributes available in PipeLineSession
 *
 * Revision 1.12  2004/08/19 09:07:02  unknown <unknown@ibissource.org>
 * Add not-null validation for message
 *
 * Revision 1.11  2004/07/20 13:04:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Corrected erroneous pipe run statistics (yet another time)
 *
 * Revision 1.10  2004/07/05 09:54:44  Johan Verrips <johan.verrips@ibissource.org>
 * Improved errorhandling mechanism: when a PipeRunException occured, transactions where still committed
 *
 * Revision 1.9  2004/04/28 14:32:42  Johan Verrips <johan.verrips@ibissource.org>
 * Corrected erroneous pipe run statistics
 *
 * Revision 1.8  2004/04/06 12:43:14  Johan Verrips <johan.verrips@ibissource.org>
 * added CommitOnState
 *
 * Revision 1.7  2004/03/31 12:04:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.6  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.5  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.4  2004/03/24 08:29:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed String 'adapterName' to Adapter 'adapter'
 * enabled XA transactions
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.Semaphore;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

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
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>if set to <code>true, messages will be processed under transaction control. (see below)</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setCommitOnState(String) commitOnState}</td><td>If the pipelineResult.getState() equals this value, the transaction is committed.</td><td><code>success</code></td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>&lt;exits&gt;one or more {@link nl.nn.adapterframework.core.PipeLineExit exits}&lt;/exits&gt;</td><td>specifications of exit-paths, in the form &lt;exit path="<i>forwardname</i>" state="<i>statename</i>"/&gt;</td></tr>
 * </table>
 * </p>
 *
 * <p><b>Transaction control</b><br>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true, messages will be processed 
 * under transaction control. Processing by XA-compliant pipes (i.e. Pipes that implement the 
 * IXAEnabled-interface, set their transacted-attribute to <code>true</code> and use XA-compliant 
 * resources) will then either be commited or rolled back in one transaction.
 * 
 * If {@link #setTransacted(boolean) transacted} is set to <code>true, either an existing transaction
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
 * STATUS_ACTIVE (i.e. normal) the transaction will be committed. Otherwise it will be (marked for) 
 * rolled back.
 
 * </p>
 * 
 * @version Id
 * @author  Johan Verrips
 */
public class PipeLine {
	public static final String version = "$RCSfile: PipeLine.java,v $ $Revision: 1.15 $ $Date: 2005-06-13 12:52:22 $";
    private Logger log = Logger.getLogger(this.getClass());
    
	private Adapter adapter;    // for transaction managing
	private INamedObject owner; // for logging purposes
	private boolean transacted=false;
    private Hashtable pipeStatistics = new Hashtable();
    private Hashtable pipeWaitingStatistics = new Hashtable();
    private Hashtable globalForwards = new Hashtable();
    private String firstPipe;
     
    private Hashtable pipelineTable=new Hashtable();
    // set of exits paths with their state
    private Hashtable pipeLineExits=new Hashtable();
	private Hashtable pipeThreadCounts=new Hashtable();
	
	private String commitOnState="success"; // exit state on which receiver will commit XA transactions


	/**
	 * Register an Pipe at this pipeline.
	 * The name is also put in the globalForwards table (with 
	 * forward-name=pipename and forward-path=pipename, so that
	 * pipe can look for a specific pipe-name. If already a globalForward
	 * exists under that name, the pipe is NOT added, allowing globalForwards
	 * to prevail.
	 * @see nl.nn.adapterframework.pipes.AbstractPipe
	 **/
	public void addPipe(IPipe pipe) {
	    pipelineTable.put(pipe.getName(), pipe);
	    pipeStatistics.put(pipe.getName(), new StatisticsKeeper(pipe.getName()));
	    if (pipe.getMaxThreads() > 0) {
	        pipeWaitingStatistics.put(pipe.getName(), new StatisticsKeeper(pipe.getName()));
	    }
	    log.debug("added pipe [" + pipe.toString() + "]");
	    if (globalForwards.get(pipe.getName()) == null) {
	        PipeForward pw = new PipeForward();
	        pw.setName(pipe.getName());
	        pw.setPath(pipe.getName());
	        registerForward(pw);
	    } else {
	        log.info("already had a pipeForward with name ["+ pipe.getName()+ "] skipping this one ["+ pipe.toString()+ "]");
	    }
	}
	
	/**
	 * Configures the pipes of this Pipeline and does some basic checks. It also
	 * registers the <code>PipeLineSession</code> object at the pipes.
	 * @see IPipe
	 */
	public void configurePipes() throws ConfigurationException {
	    Enumeration pipeNames=pipelineTable.keys();
	    while (pipeNames.hasMoreElements()) {
			String pipeName=(String)pipeNames.nextElement();
	        log.debug("Pipeline of ["+owner.getName()+"] configuring "+pipelineTable.get(pipeName).toString());
			IPipe pipe=(IPipe) pipelineTable.get(pipeName);
	
			// register the global forwards at the Pipes
			// the pipe will take care that if a local, pipe-specific
			// forward is defined, it is not overwritten by the globals
			Enumeration globalForwardNames=globalForwards.keys();
			while (globalForwardNames.hasMoreElements()) {
				String gfName=(String)globalForwardNames.nextElement();
				PipeForward pipeForward= (PipeForward) globalForwards.get(gfName);
				pipe.registerForward(pipeForward);
			}
			pipe.configure();
			log.debug("Pipeline of ["+owner.getName()+"]: Pipe ["+pipeName+"] successfully configured");
		}
	    if (pipeLineExits.size()<1) {
		    throw new ConfigurationException("no PipeLine Exits specified");
	    }
	    if (this.firstPipe==null) {
		    throw new ConfigurationException("no firstPipe defined");
	    }
	    if (pipelineTable.get(firstPipe)==null) {
		    throw new ConfigurationException("no pipe found for firstPipe ["+firstPipe+"]");
	    }
		log.debug("Pipeline of ["+owner.getName()+"] successfully configured");
	}
    /**
     * @return the number of pipes in the pipeline
     */
    public int getPipeLineSize(){
        return pipelineTable.size();
    }
    /**
     * @return a Hashtable with in the key the pipenames and in the
     * value a {@link StatisticsKeeper} object with the statistics
     */
	public Hashtable getPipeStatistics(){
		return pipeStatistics;
	}
    /**
     * @return a Hashtable with in the key the pipenames and in the
     * value a {@link StatisticsKeeper} object with the statistics
     */
	public Hashtable getPipeWaitingStatistics(){
		return pipeWaitingStatistics;
	}
	
	private Semaphore getSemaphore(IPipe pipeToRun) {
	    int maxThreads;
	
	    maxThreads = pipeToRun.getMaxThreads();
	    if (maxThreads > 0) {
	
	        Semaphore s;
	
	        synchronized (pipeThreadCounts) {
	            if (pipeThreadCounts.containsKey(pipeToRun)) {
	                s = (Semaphore) pipeThreadCounts.get(pipeToRun);
	            } else {
	                s = new Semaphore(maxThreads);
	                pipeThreadCounts.put(pipeToRun, s);
	            }
	        }
	        return s;
	    }
	    return null;
	
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
	public PipeLineResult process(String messageId, String message, PipeLineSession pipeLineSession) throws PipeRunException {
	
		if (pipeLineSession==null) {
			pipeLineSession= new PipeLineSession();
		}
		// reset the PipeLineSession and store the message and its id in the session
		if (messageId==null) {
				messageId=Misc.createSimpleUUID();
				log.error("null value for messageId, setting to ["+messageId+"]");
	
		}
		if (message == null) {
			throw new PipeRunException(null, "Pipeline of adapter ["+ owner.getName()+"] received null message");
		}
		pipeLineSession.set(message, messageId);
		pipeLineSession.setTransacted(isTransacted());
		UserTransaction utx = null;
		PipeLineResult result;
	
		try {
			if (adapter !=null && isTransacted() && !adapter.inTransaction()) {
				log.debug("Pipeline of adapter ["+ adapter.getName()+"], starting transaction for msgid ["+messageId+"]");
				utx = adapter.getUserTransaction();
				utx.begin();
			}
		} catch (Exception e) {
			throw new PipeRunException(null, "Pipeline of adapter ["+ owner.getName()+"] got exception starting transaction for msgid ["+messageId+"]");
		}
	
		try {
			result = processPipeLine(messageId, message, pipeLineSession);
			// commit or rollback the transaction
			// utx identifies wether the PipeLine instantiated the Transaction. If it did, commit or rollback.
			// If it did not, set the transaction to rollback only.
			if (utx!=null) {
				int txStatus = utx.getStatus();
			
				if  ((txStatus == Status.STATUS_ACTIVE)&&(commitOnState.equals(result.getState()))) {
	
					log.debug("Pipeline of adapter ["+ owner.getName()+"], msgid ["+messageId+"] transaction has status ACTIVE, exitState=["+result.getState()+"], performing commit");
					utx.commit();
				}
				else {
					log.warn("Pipeline of adapter ["+ owner.getName()+"], msgid ["+messageId+"] transaction has status "+JtaUtil.displayTransactionStatus(txStatus)+"exitState=["+result.getState()+"], performing ROLL BACK");
					utx.rollback();
				}
			} else {
				// if the Pipeline did not instantiate the transaction, someone else did, notify that
				// rollback is the only possibility.
				if (adapter!=null && isTransacted() && (!(commitOnState.equals(result.getState())))) {
					log.warn("Pipeline of adapter ["+ owner.getName()+"], msgid ["+messageId+"] exitState=["+result.getState()+"], setting transaction to ROLL BACK ONLY");
					utx = adapter.getUserTransaction();
					utx.setRollbackOnly();
				}
			}
			return result;
		} catch (Exception e) {
			if (utx!=null) {
				log.info("Pipeline of adapter ["+ owner.getName()+"], msgid ["+messageId+"] caught exception, will now perform rollback, (exception will be rethrown, exception message ["+ e.getMessage()+"])");
				try {
					utx.rollback();
				} catch (Exception txe) {
					log.error("Pipeline of adapter ["+ owner.getName()+"], msgid ["+messageId+"] got error rolling back transaction", txe);
				}
			}	else {
				if (adapter!=null && isTransacted())  {
					log.warn("Pipeline of adapter ["+ owner.getName()+"], msgid ["+messageId+"]  setting transaction to ROLL BACK ONLY");
					try {
					utx = adapter.getUserTransaction();
					utx.setRollbackOnly();
					} catch(Exception et) {
						log.error("Pipeline of adapter["+ owner.getName()+"], msgid ["+messageId+"]  got error setting transaction to ROLLBACK ONLY", et);
					}
				}
			}
			if (e instanceof PipeRunException)
				throw (PipeRunException)e;
			else
				throw new PipeRunException(null, "Pipeline of adapter ["+ owner.getName()+"] got error handling transaction", e);
		}
	
	}

	protected PipeLineResult processPipeLine(String messageId, String message, PipeLineSession pipeLineSession) throws PipeRunException {
	    // Object is the object that is passed to and returned from Pipes
	    Object object = (Object) message;
	    PipeRunResult pipeRunResult;
	    // the PipeLineResult 
		PipeLineResult pipeLineResult=new PipeLineResult();   
	
	
		
	    // ready indicates wether the pipeline processing is complete
	    boolean ready=false;
	    
	    // get the first pipe to run
	    IPipe pipeToRun = (IPipe) pipelineTable.get(firstPipe);
	    
	    while (!ready){
	    	
			long pipeStartTime= System.currentTimeMillis();
			
			if (log.isDebugEnabled()){  // for performance reasons
				StringBuffer sb=new StringBuffer();
				sb.append("Pipeline of adapter ["+owner.getName()+"] messageId ["+messageId+"] is about to call pipe ["+ pipeToRun.getName()+"]");
	
				if (AppConstants.getInstance().getProperty("log.logIntermediaryResults")!=null) {
					if (AppConstants.getInstance().getProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
						sb.append(" current result ["+ object.toString()+ "] ");
					}
				}
				log.debug(sb.toString());
			}
	
	        // start it
			long waitingDuration = 0;
	
			Semaphore s = getSemaphore(pipeToRun);
	        if (s != null) {
		        try {
			        // keep waiting statistics for thread-limited pipes
			        long startWaiting = System.currentTimeMillis();
			        s.acquire();
			        waitingDuration = System.currentTimeMillis() - startWaiting;
	
			        StatisticsKeeper sk = (StatisticsKeeper) pipeWaitingStatistics.get(pipeToRun.getName());
	  			    sk.addValue(waitingDuration);
	
	  			    try { 
				        pipeRunResult = pipeToRun.doPipe(object, pipeLineSession);
	  			    } finally {
				        long pipeEndTime = System.currentTimeMillis();
				        long pipeDuration = pipeEndTime - pipeStartTime - waitingDuration;
		
				        sk = (StatisticsKeeper) pipeStatistics.get(pipeToRun.getName());
				        sk.addValue(pipeDuration);
	  			    }
		        } catch (InterruptedException e) {
			        throw new PipeRunException(pipeToRun, "Interrupted waiting for pipe", e);
		        } finally { 
			        s.release();
		        }
	        } else { //no restrictions on the maximum number of threads (s==null)
		        try {
		        	
			        pipeRunResult = pipeToRun.doPipe(object, pipeLineSession);
	 		    } finally {
				        long pipeEndTime = System.currentTimeMillis();
				        long pipeDuration = pipeEndTime - pipeStartTime - waitingDuration;
		
				        StatisticsKeeper sk = (StatisticsKeeper) pipeStatistics.get(pipeToRun.getName());
				        sk.addValue(pipeDuration);
	  			    }
		       }
	        	        
	        object=pipeRunResult.getResult();
	        PipeForward pipeForward=pipeRunResult.getPipeForward();
	
	                
	        if (pipeForward==null){
	            throw new PipeRunException(pipeToRun, "Pipeline of ["+owner.getName()+"] received result from pipe ["+pipeToRun.getName()+"] without a pipeForward");
	        }
	        // get the next pipe to run
	        String nextPath=pipeForward.getPath();
	        if ((null==nextPath) || (nextPath.length()==0)){
	            throw new PipeRunException(pipeToRun, "Pipeline of ["+owner.getName()+"] got an path that equals null or has a zero-length value from pipe ["+pipeToRun.getName()+"]. Check the configuration, probably forwards are not defined for this pipe.");
	        }
	
	        if (null!=pipeLineExits.get(nextPath)){
		        String state=((PipeLineExit)pipeLineExits.get(nextPath)).getState();
		        pipeLineResult.setState(state);
		        pipeLineResult.setResult(object.toString());
	            ready=true;
				if (log.isDebugEnabled()){  // for performance reasons
		        log.debug(
		            "Pipeline of adapter ["+ owner.getName()+ "] finished processing messageId ["+messageId+"] result: ["+ object.toString()+ "] with exit-state ["+state+"]");
				}
	        } else {
		        pipeToRun=(IPipe)pipelineTable.get(pipeForward.getPath());
	        }
	
			if (pipeToRun==null) {
				throw new PipeRunException(null, "Pipeline of adapter ["+ owner.getName()+"] got an erroneous definition. Pipe to execute. ["+pipeForward.getPath()+ "] is not defined.");
			}
			
	    }
	    return pipeLineResult;
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
    /**
     * Register the adapterName of this Pipelineprocessor. 
     * @param adapterName
     */
    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        setOwner(adapter);
    }

	public void setOwner(INamedObject owner) {
		this.owner = owner;
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
    log.info("Pipeline of ["+owner.getName()+"] is starting pipeline");

    Enumeration pipeNames = pipelineTable.keys();
    while (pipeNames.hasMoreElements()) {
        String pipeName = (String) pipeNames.nextElement();

        IPipe pipe = (IPipe) pipelineTable.get(pipeName);
        log.debug("Pipeline of ["+owner.getName()+"] starting " + pipe.getName());
        pipe.start();
        log.debug("Pipeline of ["+owner.getName()+"] successfully started pipe [" + pipe.getName() + "]");
    }
    log.info("Pipeline of ["+owner.getName()+"] is successfully started pipeline");

}
/**
 * Close the pipeline. This will call the <code>stop()</code> method
 * of all registered <code>Pipes</code>
 * @see IPipe#stop
 */
public void stop() {
    log.info("Pipeline of ["+owner.getName()+"] is closing pipeline");
    Enumeration pipeNames = pipelineTable.keys();
    while (pipeNames.hasMoreElements()) {
        String pipeName = (String) pipeNames.nextElement();

        IPipe pipe = (IPipe) pipelineTable.get(pipeName);
        log.debug("Pipeline of ["+owner.getName()+"] is stopping [" + pipe.getName()+"]");
        pipe.stop();
        log.debug("Pipeline of ["+owner.getName()+"] successfully stopped pipe [" + pipe.getName() + "]");
    }
    log.debug("Pipeline of ["+owner.getName()+"] successfully closed pipeline");

}
    /**
     *
     * @return an enumeration of all pipenames in the pipeline and the
     * startpipe and endpath
     * @see #setEndPath
     * @see #setFirstPipe
     */
    public String toString(){
        String result="";
		result+="[ownerName="+(owner==null ? "-none-" : owner.getName())+"]";
        result+="[adapterName="+(adapter==null ? "-none-" : adapter.getName())+"]";
        result+="[startPipe="+firstPipe+"]";
        result+="[transacted="+transacted+"]";
        Enumeration pipeNames=pipelineTable.keys();
        while (pipeNames.hasMoreElements()){
            String pipeName=(String)pipeNames.nextElement();
            result+="["+((IPipe)pipelineTable.get(pipeName)).getName()+"]";
        }
        Enumeration exitKeys=pipeLineExits.keys();
        while (exitKeys.hasMoreElements()){
            String exitPath=(String)exitKeys.nextElement();
            PipeLineExit pe=(PipeLineExit)pipeLineExits.get(exitPath);
            result+="[path:"+pe.getPath()+" state:"+pe.getState()+"]";
        }
        return result;
    }

	public boolean isTransacted() {
		return transacted;
	}

	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
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
}
