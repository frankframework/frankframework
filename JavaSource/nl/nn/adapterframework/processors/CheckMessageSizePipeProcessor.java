/*
 * $Log: CheckMessageSizePipeProcessor.java,v $
 * Revision 1.5  2011-11-30 13:51:53  europe\m168309
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
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class CheckMessageSizePipeProcessor extends PipeProcessorBase {
	
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, PipeLineSession pipeLineSession
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
