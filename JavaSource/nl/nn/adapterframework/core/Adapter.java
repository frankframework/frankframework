package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
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
 *
 * @author Johan Verrips
 * @see    nl.nn.adapterframework.core.IReceiver
 * @see    nl.nn.adapterframework.core.PipeLine
 * @see    nl.nn.adapterframework.util.StatisticsKeeper
 * @see    nl.nn.adapterframework.util.DateUtils
 * @see    nl.nn.adapterframework.util.MessageKeeper
 * @see    nl.nn.adapterframework.core.PipeLineResult
 * 
 */

public class Adapter implements Runnable, IAdapter{
	public static final String version="$Id: Adapter.java,v 1.1 2004-02-04 08:36:11 a1909356#db2admin Exp $";
	private Vector receivers=new Vector();
	private long lastMessageDate =0;
    private PipeLine pipeline;
	private String name;
	private Logger log = Logger.getLogger(this.getClass());

	private long numOfMessagesProcessed=0;
	private long numOfMessagesInError=0;
	private StatisticsKeeper statsMessageProcessingDuration=null;
	
	private long statsUpSince=System.currentTimeMillis();
    private IErrorMessageFormatter errorMessageFormatter;

	private RunStateManager runState = new RunStateManager();
	private boolean configurationSucceeded=false;
    private String description;
    private MessageKeeper messageKeeper; //instantiated in configure()
    private int messageKeeperSize=10;    //default length

	// state to put in PipeLineResult when a PipeRunException occurs;
	private String errorState = "ERROR";

    
    /**
	 * The nummer of message currently in process
	 */
    private int numOfMessagesInProcess=0;
    
/**
 * Indicates wether the configuration succeeded.
 * Creation date: (24-02-2003 8:48:49)
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
    messageKeeper = new MessageKeeper(messageKeeperSize);
    statsMessageProcessingDuration = new StatisticsKeeper(getName());
    pipeline.setAdapterName(getName());
    pipeline.configurePipes();

    messageKeeper.add("pipeline successfully configured");
    Iterator it = receivers.iterator();
    while (it.hasNext()) {
        IReceiver receiver = (IReceiver) it.next();

        log.info(
            "Adapter [" + name + "] is initializing receiver [" + receiver.getName() + "]");
        receiver.setAdapter(this);
        try {
            receiver.configure();
		    messageKeeper.add("receiver [" + receiver.getName() + "] successfully configured");
        } catch (ConfigurationException e) {
	        String msg = "Adapter [" + getName() + "] got error initializing receiver ["+ receiver.getName() +"]";
	        log.error(msg,e);
            messageKeeper.add(msg+": "+e.getMessage());
        }
    }
    configurationSucceeded = true;
}
/**
 * Decrease the number of messages in process
 */
private synchronized void  decNumOfMessagesInProcess(long duration) {
	synchronized (statsMessageProcessingDuration) {
	  numOfMessagesInProcess--;
      numOfMessagesProcessed++;
      statsMessageProcessingDuration.addValue(duration);
      notifyAll();
	}
}
    public synchronized String formatErrorMessage(
	    String errorMessage, 
	    Throwable t,
	    String originalMessage,
	    String messageID,
	    INamedObject objectInError,
	    long receivedTime 
	    ) {
        if (errorMessageFormatter == null) {
	        errorMessageFormatter = new ErrorMessageFormatter();
        } 
        // you never can trust an implementation, so try/catch!
        try {
            return errorMessageFormatter.format(errorMessage,t,objectInError,originalMessage,messageID,receivedTime);
        } catch (Exception e) {
	        String msg = "got error while formatting errormessage, original errorMessage ["+errorMessage+"]";
	        msg=msg+" from ["+(objectInError==null? "unknown-null": objectInError.getName())+"]";
            log.error(msg, e);
            messageKeeper.add(msg+": " + e.getMessage());
            return errorMessage;
	    }
    }
  /**
   * @return some functional description of the <code>Adapter</code>
   */
  public String getDescription() {
  	return this.description;
  }
 /**
 * state to put in PipeLineResult when a PipeRunException occurs
 * Creation date: (06-06-2003 8:31:57)
 * @return java.lang.String
 */
public java.lang.String getErrorState() {
	return errorState;
}
 	/**
 	 * retrieve the date and time of the last message
 	 */ 
	public String getLastMessageDate() {
	    String result="";
	    if (lastMessageDate!=0)
		   result= DateUtils.format(new Date(lastMessageDate), DateUtils.FORMAT_GENERICDATETIME);
		else
			result="-";
	    return result;
    }
 	/**
 	 * the MessageKeeper is for keeping the last <code>messageKeeperSize</code>
 	 * messages available, for instance for displaying it in the webcontrol
     * @see nl.nn.adapterframework.util.MessageKeeper
 	 */ 
	public MessageKeeper getMessageKeeper() {
	    return this.messageKeeper;
    }
  /**
   * the functional name of this adapter
   * @return  the name of the adapter
   */
  public String getName(){
  	return name;
  }
/**
 * The number of messages for which processing ended unsuccessfully.
 */
public long getNumOfMessagesInError() {
	synchronized (statsMessageProcessingDuration) {
	return numOfMessagesInError;
	}
}
public int getNumOfMessagesInProcess() {
	synchronized (statsMessageProcessingDuration) {
		return numOfMessagesInProcess;
	}
}
/**
 * Total of messages processed
 * Creation date: (19-02-2003 12:16:53)
 * @return long total messages processed
 */
public long getNumOfMessagesProcessed() {
	synchronized (statsMessageProcessingDuration) {
	return numOfMessagesProcessed;
	}
}
  public Hashtable getPipeLineStatistics() {
  	return pipeline.getPipeStatistics();
  }
	public IReceiver getReceiverByName(String receiverName) {
           Iterator it=receivers.iterator();
           while (it.hasNext()){
	           IReceiver receiver=(IReceiver) it.next();
	           if (receiver.getName().equalsIgnoreCase(receiverName)){
		           return receiver;
	           }
	           
           }
           return null;
	}
	public Iterator getReceiverIterator() {
		return receivers.iterator();
	}
public RunStateEnum getRunState() {
	return runState.getRunState();
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
	return DateUtils.format(new Date(statsUpSince), DateUtils.FORMAT_GENERICDATETIME);
}
/**
 * Retrieve the waiting statistics as a <code>Hashtable</code>
 */ 
public Hashtable getWaitingStatistics() {
  	return pipeline.getPipeWaitingStatistics();
  }
/**
 * The number of messages for which processing ended unsuccessfully.
 */
private void incNumOfMessagesInError() {
	synchronized (statsMessageProcessingDuration) {
		numOfMessagesInError++;
	}
}
/**
 * Increase the number of messages in process
 */
private void incNumOfMessagesInProcess(long startTime) {
	synchronized (statsMessageProcessingDuration) {
	  numOfMessagesInProcess++;
      lastMessageDate = startTime;
  	}
}
/**
 *
 * Process the receiving of a message
 * After all Pipes have been run in the PipeLineProcessor, the Object.toString() function
 * is called. The result is returned to the Receiver.
 *
 */
public PipeLineResult processMessage(
    String messageId,
    String message) {
    PipeLineResult result = new PipeLineResult();

    long startTime = System.currentTimeMillis();
    // prevent executing a stopped adapter
    // the receivers should implement this, but you never now....
    RunStateEnum currentRunState = getRunState();
    if (!currentRunState.equals(RunStateEnum.STARTED) &&
	    !currentRunState.equals(RunStateEnum.STOPPING)) {
	    
		String msgAdapterNotOpen = "Adapter [" + getName() + "] in state ["+currentRunState+"], cannot process message";
		ManagedStateException e = new ManagedStateException(msgAdapterNotOpen);

       result.setResult(formatErrorMessage(null, e, message, messageId, this, startTime));
       result.setState(getErrorState());
       return result;
    }

    incNumOfMessagesInProcess(startTime);

    if (log.isDebugEnabled()) { // for performance reasons
        log.debug("Adapter [" + name + "] received message [" + message + "] with messageId ["+messageId+"]");
    } else  {
      log.info("Adapter [" + name + "] received message with messageId ["+messageId+"]");
    }

    try {
        result = pipeline.process(messageId, message);
        if (log.isDebugEnabled()) {
	        log.debug("Adapter ["+getName()+"] messageId["+messageId+ "] got exit-state ["+result.getState()+"] and result ["+result.toString()+"] from PipeLine");
        }

    } catch (PipeRunException pre) {
	    incNumOfMessagesInError();
		// fill the PipeRunException with the name of the last pipe
//        if (pre.getPipeInError() == null)
//            pre.setPipeInError(pipeline.getLastExecutedPipe());
        result.setResult(formatErrorMessage("error during pipeline processing", pre, message, messageId, pre.getPipeInError(), startTime));
		result.setState(getErrorState());
        log.error("Adapter [" + name + "] got error during pipeline processing messageId["+ messageId+"]: ["+pre.toString()+"]", pre);
        //notify messageKeeper        
        messageKeeper.add("error during pipeline processing: " + pre.getMessage());
    } catch (Throwable undefError) {
	    incNumOfMessagesInError();
	    
        messageKeeper.add(
            "illegal exception thrown ["
                + undefError.getClass().getName()
                + "] with message ["
                + undefError.getMessage()
                + "] on message ["
                + message
                + "] messageId ["
                + messageId
                + "]");
        log.error(
            "Illegal exception found in processMessage of adapter [" + name + "]",
            undefError);

        result.setResult(formatErrorMessage("Illegal exception found in processMessage of adapter [" + name + "]", undefError, message, messageId, this, startTime));
		result.setState(getErrorState());
        
    }
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    if (log.isDebugEnabled()) { // for performance reasons
        log.debug(
            "Adapter: ["
                + getName()
                + "] STAT: Finished processing message with messageId ["
                + messageId
                + "] exit-state ["
                + result.getState()
                + "] started "
                + DateUtils.format(new Date(startTime), DateUtils.FORMAT_GENERICDATETIME)
                + " finished "
                + DateUtils.format(new Date(endTime), DateUtils.FORMAT_GENERICDATETIME)
                + " total duration: "
                + (endTime - startTime)
                + " msecs");
    } else log.info("Adapter completed message with messageId ["+messageId+"] with exit-state ["+result.getState()+"]");

    //reset the InProcess fields, and increase processedMessagesCount
    decNumOfMessagesInProcess(duration);
    return result;
}
  /**
   * Register a PipeLine at this adapter. On registering, the adapter performs
   * a <code>Pipeline.configurePipes()</code>, as to configure the individual pipes.
    * @param pipeline
   * @throws ConfigurationException
   * @see PipeLine
   */
  public void registerPipeLine (PipeLine pipeline) throws ConfigurationException{
  	this.pipeline=pipeline;
  	log.debug("Adapter ["+name+"] registered pipeline ["+pipeline.toString()+"]");

  }
    /**
     * Register a receiver for this Adapter
     * @param receiver
     * @see IReceiver
     */
public void registerReceiver(IReceiver receiver) {
    receivers.add(receiver);
    log.debug(
        "Adapter ["+name+"] registered receiver ["
            + receiver.getName()
            + "] with properties ["
            + receiver.toString()
            + "]");
}
/**
 * do not call this method! start an adapter by the <code>start()</code>
 * method. This method starts the adapter, synchronizing the adapter itself.
 * This is done to prevent simultaenous starting and stopping of the adapter
 * by the web-application.
 */
public void run() {
    try {

        if (!configurationSucceeded) {
                log.warn(
                    "configuration of adapter ["
                        + getName()
                        + "] did not succeed, therefore starting the adapter is not possible");
                messageKeeper.add(
                    "configuration did not succeed. Starting the adapter is not possible");
                return;
        }
            
        synchronized (runState) {
            RunStateEnum currentRunState = getRunState();
            if (!currentRunState.equals(RunStateEnum.STOPPED)) {
	            String msg = "Adapter [" + getName() + "] is currently in state ["+ currentRunState +"], ignoring start() command";
                log.warn(msg);
                messageKeeper.add(msg);
                return;
            }
            // start the pipeline
            runState.setRunState(RunStateEnum.STARTING);
        }
        try {
                log.debug("Adapter [" + getName() + "] is starting pipeline");
                pipeline.start();

                // as from version 3.0 the adapter is started,
                // regardless of receivers are correctly started.
	            runState.setRunState(RunStateEnum.STARTED);
            } catch (PipeStartException pre) {
                log.error(pre);
                messageKeeper.add(
                    "Adapter [" + getName() + "] got error starting PipeLine: " + pre.getMessage());
	            runState.setRunState(RunStateEnum.ERROR);
            }
            // start receivers only if adapter is started.
            if (runState.isInState(RunStateEnum.STARTED)) {
                Iterator it = receivers.iterator();
                while (it.hasNext()) {
                    IReceiver receiver = (IReceiver) it.next();
                    log.info("Adapter [" + getName() + "] is starting receiver [" + receiver.getName() + "]");
                    receiver.startRunning();
                } //while

		        messageKeeper.add("Adapter up and running");
		        log.info("Adapter [" + getName() + "] up and running");
            
		        waitForRunState(RunStateEnum.STOPPING);

		        it = receivers.iterator();
                while (it.hasNext()) {
                    IReceiver receiver = (IReceiver) it.next();
                    receiver.waitForRunState(RunStateEnum.STOPPED);
                    log.info("Adapter [" + getName() + "] stopped [" + receiver.getName() + "]");
                }

                int currentNumOfMessagesInProcess = getNumOfMessagesInProcess();
				if (currentNumOfMessagesInProcess>0) {
					String msg = "Adapter ["+name+"] is being stopped while still processing "+currentNumOfMessagesInProcess+" messages, waiting for them to finish";
					log.warn(msg);
					messageKeeper.add(msg);
				}
				waitForNoMessagesInProcess();
			  	log.debug("Adapter ["+name+"] is stopping pipeline");	
			  	pipeline.stop();
				runState.setRunState(RunStateEnum.STOPPED);
		 		messageKeeper.add("Adapter stopped");

  			    
            }// if isstarted

    } catch (Throwable e) {
	    	log.error("error running adapter ["+getName()+"] ["+ToStringBuilder.reflectionToString(e)+"]", e);
			runState.setRunState(RunStateEnum.ERROR);
    }

}
  /**
   *  some functional description of the <code>Adapter</code>
   */
  public void setDescription(String description){
  	this.description=description;
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
	 * Creation date: (06-06-2003 8:31:57)
	 * @param newErrorState java.lang.String
	 * @see PipeLineResult
	 */
	public void setErrorState(java.lang.String newErrorState) {
		errorState = newErrorState;
	}
    /**
     * Set the number of messages that are kept on the screen.
     * @param size
     * @see nl.nn.adapterframework.util.MessageKeeper
     */
    public void setMessageKeeperSize (int size) {
	    this.messageKeeperSize=size;
    }
  /**
   * the functional name of this adapter
   */
  public void setName(String name){
  	this.name=name;
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
    Thread t = new Thread(this, getName());
    t.start();
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
  public void stopRunning(){

	synchronized (runState) {
		RunStateEnum currentRunState = getRunState();
		
		if (!currentRunState.equals(RunStateEnum.STARTED) && 
			!currentRunState.equals(RunStateEnum.ERROR)) {
			String msg = "Adapter ["+name+"] in state ["+ currentRunState +"] while stopAdapter() command is issued, ignoring command";
			log.warn(msg);
			messageKeeper.add(msg);
			return;
		}
		
	  	log.debug("Adapter ["+name+"] is stopping receivers");
	  	Iterator it=receivers.iterator();
	  	while (it.hasNext()) {
		  	IReceiver receiver=(IReceiver)it.next();
		  	try{
			  	receiver.stopRunning();
		  		log.info("Adapter ["+name+"] successfully stopped receiver ["+receiver.getName()+"]");

		  	} catch (Exception e) {
			  	log.error("Adapter ["+name+"] received error while stopping, ignoring this, so watch out.",e);
		  		messageKeeper.add("received error stopping receiver ["+receiver.getName()+"] : "+e.getMessage());
		  	}
	  	}

		runState.setRunState(RunStateEnum.STOPPING);
	}
  }
  public String toString(){
	  StringBuffer sb=new StringBuffer();
	  sb.append("[name="+name+"]");
	  sb.append("[version="+version+"]");
	  Iterator it=receivers.iterator();
	  sb.append("[receivers=");
	  while (it.hasNext()){
		  IReceiver receiver=(IReceiver) it.next();
		  sb.append(" "+receiver.getName());
	  
	  }
	  sb.append("]");
  	  sb.append("[pipeLine="+((pipeline!=null)?pipeline.toString():"none registered")+"]"+
  			"[started="+getRunState()+"]");
	  
  	return sb.toString();
  }
public void waitForNoMessagesInProcess() throws InterruptedException {
	synchronized (statsMessageProcessingDuration) {
		while (getNumOfMessagesInProcess()>0) {
			wait();
		}
	}
}
public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
	runState.waitForRunState(requestedRunState);
}
}
