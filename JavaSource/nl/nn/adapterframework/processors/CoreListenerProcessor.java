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
/*
 * $Log: CoreListenerProcessor.java,v $
 * Revision 1.5  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.4  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class CoreListenerProcessor implements ListenerProcessor {
	private Logger log = LogUtil.getLogger(this);

	public String getMessage(ICorrelatedPullingListener listener,
			String correlationID, IPipeLineSession pipeLineSession
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

	protected String getLogPrefix(ICorrelatedPullingListener listener, IPipeLineSession session){
		  StringBuffer sb=new StringBuffer();
		  sb.append("Listener ["+listener.getName()+"] ");
		  if (session!=null) {
			  sb.append("msgId ["+session.getMessageId()+"] ");
		  }
		  return sb.toString();
	}

}
