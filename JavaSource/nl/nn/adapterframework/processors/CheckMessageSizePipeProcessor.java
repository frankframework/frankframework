/*
 * $Log: CheckMessageSizePipeProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
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
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class CheckMessageSizePipeProcessor implements PipeProcessor {
	private Logger log = LogUtil.getLogger(this);
	private PipeProcessor pipeProcessor;

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}
	
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
