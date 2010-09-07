/*
 * $Log: CoreListenerProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class CoreListenerProcessor implements ListenerProcessor {
	private Logger log = LogUtil.getLogger(this);

	public String getMessage(ICorrelatedPullingListener listener,
			String correlationID, PipeLineSession pipeLineSession
			) throws ListenerException, TimeOutException {
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix(listener, pipeLineSession)
					+ "starts listening for return message with correlationID ["+ correlationID	+ "]");
		}
		String result;
		Map threadContext=new HashMap();
		try {
			threadContext = listener.openThread();
			Object msg = listener.getRawMessage(correlationID, threadContext);
			if (msg==null) {	
				log.info(getLogPrefix(listener, pipeLineSession)+"received null reply message");
			} else {
				log.info(getLogPrefix(listener, pipeLineSession)+"received reply message");
			}
			result = listener.getStringFromRawMessage(msg, threadContext);
		} finally {
			try {
				log.debug(getLogPrefix(listener, pipeLineSession)+"is closing");
				listener.closeThread(threadContext);
			} catch (ListenerException le) {
				log.error(getLogPrefix(listener, pipeLineSession)+"got error on closing", le);
			}
		}
		return result;
	}

	protected String getLogPrefix(ICorrelatedPullingListener listener, PipeLineSession session){
		  StringBuffer sb=new StringBuffer();
		  sb.append("Listener ["+listener.getName()+"] ");
		  if (session!=null) {
			  sb.append("msgId ["+session.getMessageId()+"] ");
		  }
		  return sb.toString();
	}

}
