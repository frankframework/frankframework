/*
 * $Log: ParallelSenders.java,v $
 * Revision 1.1  2008-05-15 15:08:26  europe\L190409
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import java.util.Iterator;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.StatisticsKeeper;

import org.springframework.core.task.TaskExecutor;

/**
 * Collection of Senders, that are executed all at the same time.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class ParallelSenders extends SenderSeries {

	/**
	 * The thread-pool for spawning threads, injected by Spring
	 */
	private TaskExecutor taskExecutor;


	protected String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		Guard guard= new Guard();
		for (Iterator it=getSenderIterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			guard.addResource();
			SenderExecutor se=new SenderExecutor(sender, correlationID, message, prc, guard);
			getTaskExecutor().execute(se);
		}
		try {
			guard.waitForAllResources();
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"was interupted",e);
		}
		return message;
	}



	private class SenderExecutor implements Runnable {

		ISender sender; 
		String correlationID; 
		String message;
		ParameterResolutionContext prc;
		Guard guard;

		String result;
		Throwable t;
		
		public SenderExecutor(ISender sender, String correlationID, String message, ParameterResolutionContext prc, Guard guard) {
			super();
			this.sender=sender;
			this.correlationID=correlationID;
			this.message=message;
			this.prc=prc;
			this.guard=guard;
		}

		public void run() {
			try {
				long t1 = System.currentTimeMillis();
				try {
					if (sender instanceof ISenderWithParameters) {
						result = ((ISenderWithParameters)sender).sendMessage(correlationID,message,prc);
					} else {
						result = sender.sendMessage(correlationID,message);
					}
				} catch (Throwable tr) {
					if (t==null) {
						t=tr;
					} else {
						log.warn(getLogPrefix()+"sender ["+ClassUtils.nameOf(sender)+"] caught additional exception",tr );
					}
				}
				long t2 = System.currentTimeMillis();
				StatisticsKeeper sk = getStatisticsKeeper(sender);
				sk.addValue(t2-t1);
			} finally {
				guard.releaseResource();
			} 
		}
		
		public String getResult() throws SenderException, TimeOutException  {
			if (t!=null) {
				if (t instanceof TimeOutException) {
					throw (TimeOutException)t;
				}
				if (t instanceof SenderException) {
					throw (SenderException)t;
				}
				throw new SenderException(t);
			}
			return result;
		}
	}

	public void setSynchronous(boolean value) {
		if (!isSynchronous()) {
			super.setSynchronous(value); 
		} 
	}

	public void setTaskExecutor(TaskExecutor executor) {
		taskExecutor = executor;
	}
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

}
