/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.processors;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

/**
 * @author Jaco de Groot
 */
public class CoreListenerProcessor implements ListenerProcessor {
	private Logger log = LogUtil.getLogger(this);

	public Message getMessage(ICorrelatedPullingListener listener, String correlationID, IPipeLineSession pipeLineSession) throws ListenerException, TimeOutException {
		if (log.isDebugEnabled()) log.debug(getLogPrefix(listener, pipeLineSession) + "starts listening for return message with correlationID ["+ correlationID	+ "]");
		Message result;
		Map threadContext=new HashMap();
		try {
			threadContext = listener.openThread();
			Object msg = listener.getRawMessage(correlationID, threadContext);
			if (msg==null) {	
				log.info(getLogPrefix(listener, pipeLineSession)+"received null reply message");
			} else {
				log.info(getLogPrefix(listener, pipeLineSession)+"received reply message");
			}
			result = listener.extractMessage(msg, threadContext);
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
