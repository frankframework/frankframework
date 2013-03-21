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
 * $Log: CheckMessageSizePipeProcessor.java,v $
 * Revision 1.6  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.5  2011/11/30 13:51:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2010/09/13 13:53:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now extends baseclass
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class CheckMessageSizePipeProcessor extends PipeProcessorBase {
	
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, IPipeLineSession pipeLineSession
			) throws PipeRunException {
		checkMessageSize(message, pipeLine, pipe, true);
		PipeRunResult pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		Object result = pipeRunResult.getResult();
		checkMessageSize(result, pipeLine, pipe, false);
		return pipeRunResult;
	}

	private void checkMessageSize(Object message, PipeLine pipeLine, IPipe pipe, boolean input) {
		String logMessage = null;
		if (pipeLine.getMessageSizeErrorNum()>=0) {
			if (message instanceof String) {
				int messageLength = message.toString().length();
				if (messageLength>=pipeLine.getMessageSizeErrorNum()) {
					logMessage = "pipe [" + pipe.getName() + "] of adapter [" + pipeLine.getOwner().getName() + "], " + (input ? "input" : "result") + " message size [" + Misc.toFileSize(messageLength) + "] exceeds [" + Misc.toFileSize(pipeLine.getMessageSizeErrorNum()) + "]";
					log.error(logMessage);
					if (pipe instanceof IExtendedPipe) {
						IExtendedPipe pe = (IExtendedPipe)pipe;
						pe.throwEvent(IExtendedPipe.MESSAGE_SIZE_MONITORING_EVENT);
					}
				}
			}
		}
		if (logMessage == null) {
			if (pipeLine.getMessageSizeWarnNum()>=0) {
				if (message instanceof String) {
					int messageLength = message.toString().length();
					if (messageLength>=pipeLine.getMessageSizeWarnNum()) {
						logMessage = "pipe [" + pipe.getName() + "] of adapter [" + pipeLine.getOwner().getName() + "], " + (input ? "input" : "result") + " message size [" + Misc.toFileSize(messageLength) + "] exceeds [" + Misc.toFileSize(pipeLine.getMessageSizeWarnNum()) + "]";
						log.warn(logMessage);
					}
				}
			}
		}
	}

}
