package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.Semaphore;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Processor and keeper of a line of {@link IPipe Pipes}.
 * <br/>
 * Pipelines also generate statics information per Pipe and keep forwards, that are registered
 * at individual pipes during the configure fase.
 * <br/>
 * In the AppConstants there may be a property named "log.logIntermediaryResults" (true/false)
 * which indicates wether the intermediary results (between calling pipes) have to be logged.
 * 
 * @author  Johan Verrips
 */
public class PipeLine {
	public static final String version="$Id: PipeLine.java,v 1.1 2004-02-04 08:36:12 a1909356#db2admin Exp $";
    private Logger log = Logger.getLogger(this.getClass());
	private String adapterName; // for logging purposes
    private Hashtable pipeStatistics = new Hashtable();
    private Hashtable pipeWaitingStatistics = new Hashtable();
    private Hashtable globalForwards = new Hashtable();
    private String firstPipe;
     
    private Hashtable pipelineTable=new Hashtable();
    // set of exits paths with their state
    private Hashtable pipeLineExits=new Hashtable();
	private Hashtable pipeThreadCounts=new Hashtable();

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
        log.debug("Pipeline of ["+adapterName+"] configuring "+pipelineTable.get(pipeName).toString());
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
		log.debug("Pipeline of ["+adapterName+"]: Pipe ["+pipeName+"] successfully configured");
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
public PipeLineResult process(String messageId, String message) throws PipeRunException {
    // Object is the object that is passed to and returned from Pipes
    Object object = (Object) message;
    IPipe lastExecutedPipe;
    PipeRunResult pipeRunResult;
    
    long pipeStartTime = System.currentTimeMillis();

    PipeLineSession pipeLineSession= new PipeLineSession();
    // the PipeLineResult 
	PipeLineResult pipeLineResult=new PipeLineResult();   
	// reset the PipeLineSession and store the message and its id in the session
	if (messageId==null) {
			messageId=Misc.createSimpleUUID();
			log.error("null value for messageId, setting to ["+messageId+"]");

	} 
	pipeLineSession.reset(message, messageId);
	
    // ready indicates wether the pipeline processing is complete
    boolean ready=false;
    
    // get the first pipe to run
    IPipe pipeToRun = (IPipe) pipelineTable.get(firstPipe);
    
    while (!ready){
		if (log.isDebugEnabled()){  // for performance reasons
			StringBuffer sb=new StringBuffer();
			sb.append("Pipeline of adapter ["+adapterName+"] messageId ["+messageId+"] is about to call pipe ["+ pipeToRun.getName()+"]");

			if (AppConstants.getInstance().getProperty("log.logIntermediaryResults")!=null) {
				if (AppConstants.getInstance().getProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
					sb.append(" current result ["+ object.toString()+ "] ");
				}
			}
			log.debug(sb.toString());
		}

		lastExecutedPipe=pipeToRun;
        // start it
		long waitingDuration = 0;

		Semaphore s = getSemaphore(pipeToRun);
        if (s != null) {
	        try {
		        // keep waiting statistics for thread-limited pipes
		        long startWaiting = System.currentTimeMillis();
		        s.acquire();
		        waitingDuration = startWaiting - System.currentTimeMillis();

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
            throw new PipeRunException(pipeToRun, "Pipeline of ["+adapterName+"] received result from pipe ["+pipeToRun.getName()+"] without a pipeForward");
        }
        // get the next pipe to run
        String nextPath=pipeForward.getPath();
        if ((null==nextPath) || (nextPath.length()==0)){
            throw new PipeRunException(pipeToRun, "Pipeline of ["+adapterName+"] got an path that equals null or has a zero-length value from pipe ["+pipeToRun.getName()+"]. Check the configuration, probably forwards are not defined for this pipe.");
        }

        if (null!=pipeLineExits.get(nextPath)){
	        String state=((PipeLineExit)pipeLineExits.get(nextPath)).getState();
	        pipeLineResult.setState(state);
	        pipeLineResult.setResult(object.toString());
            ready=true;
			if (log.isDebugEnabled()){  // for performance reasons
	        log.debug(
	            "Pipeline of adapter ["+ adapterName+ "] finished processing messageId ["+messageId+"] result: ["+ object.toString()+ "] with exit-state ["+state+"]");
			}
        } else {
	        pipeToRun=(IPipe)pipelineTable.get(pipeForward.getPath());
        }

		if (pipeToRun==null) {
			throw new PipeRunException(null, "Pipeline of adapter ["+ adapterName+"] got an erroneous definition. Pipe to execute. ["+pipeForward.getPath()+ "] is not defined.");
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
    public void setAdapterName(String adapterName) {
        this.adapterName = adapterName;
    }
   /**
    * The indicator for the end of the processing, with default state "undefined".
    * @deprecated since v 3.2 this functionality is superseded by the use of PipeLineExits.
    * @see PipeLineExit
    */
    public  void setEndPath(String endPath){
	    PipeLineExit te=new PipeLineExit();
	    te.setPath(endPath);
	    te.setState("undefined");
        pipeLineExits.put(endPath, te);
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
    log.info("Pipeline of ["+adapterName+"] is starting pipeline");

    Enumeration pipeNames = pipelineTable.keys();
    while (pipeNames.hasMoreElements()) {
        String pipeName = (String) pipeNames.nextElement();

        IPipe pipe = (IPipe) pipelineTable.get(pipeName);
        log.debug("Pipeline of ["+adapterName+"] starting " + pipe.getName());
        pipe.start();
        log.debug("Pipeline of ["+adapterName+"] successfully started pipe [" + pipe.getName() + "]");
    }
    log.info("Pipeline of ["+adapterName+"] is successfully started pipeline");

}
/**
 * Close the pipeline. This will call the <code>stop()</code> method
 * of all registered <code>Pipes</code>
 * @see IPipe#stop
 */
public void stop() {
    log.info("Pipeline of ["+adapterName+"] is closing pipeline");
    Enumeration pipeNames = pipelineTable.keys();
    while (pipeNames.hasMoreElements()) {
        String pipeName = (String) pipeNames.nextElement();

        IPipe pipe = (IPipe) pipelineTable.get(pipeName);
        log.debug("Pipeline of ["+adapterName+"] is stopping [" + pipe.getName()+"]");
        pipe.stop();
        log.debug("Pipeline of ["+adapterName+"] successfully stopped pipe [" + pipe.getName() + "]");
    }
    log.debug("Pipeline of ["+adapterName+"] successfully closed pipeline");

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
        result+="[adapterName="+adapterName+"]";
        Enumeration pipeNames=pipelineTable.keys();
        while (pipeNames.hasMoreElements()){
            String pipeName=(String)pipeNames.nextElement();
            result+="["+((IPipe)pipelineTable.get(pipeName)).getName()+"]";
        }
        result+="[startPipe="+firstPipe+"]";
        Enumeration exitKeys=pipeLineExits.keys();
        while (exitKeys.hasMoreElements()){
            String exitPath=(String)exitKeys.nextElement();
            PipeLineExit pe=(PipeLineExit)pipeLineExits.get(exitPath);
            result+="[path:"+pe.getPath()+" state:"+pe.getState()+"]";
        }
        return result;
    }
}
