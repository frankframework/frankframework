/*
 * $Log: ParallelSenders.java,v $
 * Revision 1.2  2008-05-21 10:54:44  europe\L190409
 * collect results
 *
 * Revision 1.1  2008/05/15 15:08:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.StatisticsKeeper;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.springframework.core.task.TaskExecutor;

/**
 * Collection of Senders, that are executed all at the same time.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.senders.ParallelSenders</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>one or more specifications of senders. Each will receive the same input message, to be processed in parallel</td></tr>
 * </table>
 * </p>

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
		Map executorMap = new HashMap();
		for (Iterator it=getSenderIterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			guard.addResource();
			SenderExecutor se=new SenderExecutor(sender, correlationID, message, prc, guard);
			executorMap.put(sender,se);
			getTaskExecutor().execute(se);
		}
		try {
			guard.waitForAllResources();
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"was interupted",e);
		}
		XmlBuilder resultsXml = new XmlBuilder("results");
		for (Iterator it=getSenderIterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			SenderExecutor se = (SenderExecutor)executorMap.get(sender);
			Object result = se.getResult();
			XmlBuilder resultXml = new XmlBuilder("result");
			resultXml.addAttribute("senderClass",ClassUtils.nameOf(sender));
			resultXml.addAttribute("senderName",sender.getName());
			if (result==null) {
				resultXml.addAttribute("type","null");
			} else {
				resultXml.addAttribute("type",ClassUtils.nameOf(result));
				if (result instanceof Throwable) {
					resultXml.setValue(((Throwable)result).getMessage());
				} else {
					resultXml.setValue(XmlUtils.skipXmlDeclaration(result.toString()),false);
				}
			}
			resultsXml.addSubElement(resultXml); 
		}
		return resultsXml.toXML();
	}



	private class SenderExecutor implements Runnable {

		ISender sender; 
		String correlationID; 
		String message;
		ParameterResolutionContext prc;
		Guard guard;

		Object result;
		
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
					result = tr;
				}
				long t2 = System.currentTimeMillis();
				StatisticsKeeper sk = getStatisticsKeeper(sender);
				sk.addValue(t2-t1);
			} finally {
				guard.releaseResource();
			} 
		}
		
		public Object getResult() throws SenderException, TimeOutException  {
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
