/*
 * $Log: PushingReceiverBase.java,v $
 * Revision 1.1  2004-06-22 12:12:52  L190409
 * introduction of MessagePushers and PushingReceivers
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IMessagePusher;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.StatisticsKeeper;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IReceiver Receiver},
 * that enable <code>IAdapters</code> to receive messages received by pushing listeners.
 * <p>Note: at this moment the state of the adapter is not handled. Only the result
 * from calling the adapter (<code>PipeLineResult.getResult()</code> is retrieved,
 * not <code>PipeLineResult.getState()</code>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.ServiceListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnIfStopped(String) returnIfStopped}</td><td>message to be returned in case the listener is closed</td><td>continue</td></tr>
 * </table>
 * <p>
 * <table border="1">
 * <tr><th>nested elements (accessible in descender-classes)</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.IMessagePusher listener}</td><td>the pushing listener used to receive messages from</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Gerrit van Brakel
 * @see nl.nn.adapterframework.core.IAdapter
 * @see nl.nn.adapterframework.core.IReceiver
 * @see nl.nn.adapterframework.core.PipeLineResult
 */
public class PushingReceiverBase  implements IReceiver, IReceiverStatistics, HasSender,
        ServiceClient, Serializable {
	public static final String version="$Id: PushingReceiverBase.java,v 1.1 2004-06-22 12:12:52 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;
        	
	private String name;
	private String returnIfStopped="";

	private IMessagePusher listener;
	private ISender sender;

	private IAdapter adapter;

	private Counter msgReceived= new Counter(0);
	private StatisticsKeeper processStatistics = new StatisticsKeeper("general");

	private RunStateManager runState = new RunStateManager();

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		IMessagePusher listener = getListener();
		if (listener==null) {
			throw new ConfigurationException(getLogPrefix()+"has no listener");
		}
		listener.setHandler(this);
		listener.configure();
		
		ISender sender = getSender();
		if (sender !=null) {
			sender.configure();
		}
	}

	/**
	 * Start the receiver. The thread-name will be set to the receiver name
	 */
	public void startRunning() {
		if (adapter != null) {
			RunStateEnum currentRunState = adapter.getRunState();
			if (!currentRunState.equals(RunStateEnum.STARTED)) {
				log.warn(getLogPrefix()
				        +" on adapter ["
						+ adapter.getName()
						+ "] was tried to start, but the adapter is in state ["+currentRunState+"]. Ignoring command.");
				adapter.getMessageKeeper().add(
					"ignored start command on [" + getName() + "]; adapter is in state ["+currentRunState+"]");
				return;
			}
		}
	   	try {				   		
			getListener().open();
			if (getSender()!=null) {
				getSender().open();
			}
			runState.setRunState(RunStateEnum.STARTED);

			String msg = getLogPrefix()+"started listening";
			if (adapter != null) {
				 adapter.getMessageKeeper().add(msg);
			log.info(msg);
			}
	   	} catch (Exception e) {
			runState.setRunState(RunStateEnum.ERROR);
			String msg = getLogPrefix()+"could not start";
			if (adapter != null) {
				adapter.getMessageKeeper().add(msg+": "+e.getMessage());
		   	}
		   	log.error(msg,e);
		}
	}
	 /**
		 * The receiver is ordered to stop listening by someone.
		 */
	public void stopRunning() {
		try {				   		
			if (getSender()!=null) {
				getSender().close();
			}
		} catch (Exception e) {
			String msg = getLogPrefix()+"has problem stopping sender";
			if (adapter != null) {
				 adapter.getMessageKeeper().add(msg+": "+e.getMessage());
			}
			log.error(msg,e);
		}
		try {				   		
			getListener().close();
		} catch (Exception e) {
			String msg = getLogPrefix()+"has problem stopping listener";
			if (adapter != null) {
				 adapter.getMessageKeeper().add(msg+": "+e.getMessage());
			}
			log.error(msg,e);
		}
		
		runState.setRunState(RunStateEnum.STOPPED);
		if (adapter!=null) {
			adapter.getMessageKeeper().add(getLogPrefix()+"stopped");
		}

	}

	/**
	 * Process the received message with {@link #processRequest(String, String)}.
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID
	 */
	public String processRequest(String request) {
		String correlationId=getName()+"-"+Misc.createSimpleUUID();
			if (log.isDebugEnabled()) 
				log.debug(getLogPrefix()+"generating correlationId ["+correlationId+"] for request ["+request+"]");
		return processRequest(correlationId, request);
	}
	
	/**
	 * Process the received message.
	 * @since 4.0
	 */	
	public String processRequest(String correlationId, String request) {
		String answer = "";
		String state = "";
	
		long startProcessingTimestamp = System.currentTimeMillis();
		msgReceived.increase();
	    
		if (getRunState().equals(RunStateEnum.STARTED)) {
			PipeLineResult pipeLineResult=adapter.processMessage(correlationId, request);
			answer = pipeLineResult.getResult();
			state = pipeLineResult.getState();
			if (log.isDebugEnabled()) 
				log.debug(getLogPrefix()+"correlationId ["+correlationId+"] execution of pipeline returned result ["+answer+"] with exit-state ["+state+"]");
		}
		else
			answer = returnIfStopped;
	
		if (getSender() != null) {
			try {
				getSender().sendMessage(correlationId, answer);
			} catch (Exception se) {
				log.error(
					getLogPrefix()+"Error occured on sendMessage. closing down adapter",se);
				adapter.getMessageKeeper().add(
					"Error occured while sending message. Closing adapter.");
				if (adapter != null) {
					try {
						adapter.stopRunning();
					} catch (Exception e) {
						log.error(getLogPrefix()+"failed to close adapter [" + adapter.getName() + "] afte errors on sender.", e);
					}
				}
				log.error(getLogPrefix()+"closing after errors on sender"); 
				stopRunning();
	
			}
		}
		if (answer==null) {
			log.warn(getLogPrefix()+"has null as a answer, setting to empty string");
			answer="";
		}
		long duration = System.currentTimeMillis() - startProcessingTimestamp;
	
		synchronized (processStatistics) {
			processStatistics.addValue(duration);
		}
	
		return answer;	
	}


	public Iterator getIdleStatisticsIterator() {
	    return null;
	}
	public long getMessagesReceived() {
	    return msgReceived.getValue();
	}
	public Iterator getProcessStatisticsIterator() {
	
		class StatisticsIterator implements Iterator {
			private Object p;
	
			StatisticsIterator(Object p) {
				super();
				this.p = p;
			}
	
			public boolean hasNext() {
				return p!=null;
			}
	
			public Object next() {
				Object result = p;
				p=null;
				return result;
			}
			
			public void remove() {
				p=null;
			}
		}
		
	    return new StatisticsIterator(processStatistics);
	}
	public RunStateEnum getRunState() {
		return runState.getRunState();
	}
    public ISender getSender(){
      return sender;
    }
	/**
     * The processing of messages must be delegated to the <code>Adapter</code>
     * object.
     * @see nl.nn.adapterframework.core.IAdapter
     */
	public void setAdapter(IAdapter adapter) {
	     this.adapter=adapter;
	}
	/**
   	 * The <code>toString()</code> method retrieves its value
     * by reflection.
     * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
     *
     **/
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
		runState.waitForRunState(requestedRunState);
	}
	
	protected String getLogPrefix() {
		return "Receiver ["+getClass().getName()+"] named ["+getName()+"] ";
	}
	
	/**
	 * @return
	 */
	public IMessagePusher getListener() {
		return listener;
	}
	/**
	 * @param pusher
	 */
	protected void setListener(IMessagePusher listener) {
		this.listener = listener;
	}
	/**
	 * Returns the name of the Listener. 
	 */
	public String getName() {
		return name;
	}
	/**
	 * Sets the name of the Listener.
	 */
	public void setName(String name) {
		this.name=name;
	}
	
	/**
	 * Return this value when this receiver is stopped.
	 */
	public String getReturnIfStopped() {
		return returnIfStopped;
	}
	/**
	 * Return this value when this receiver is stopped.
	 */
	public void setReturnIfStopped (String returnIfStopped){
		this.returnIfStopped=returnIfStopped;
	}

}
