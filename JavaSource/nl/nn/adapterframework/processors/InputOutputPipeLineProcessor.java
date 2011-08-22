/*
 * $Log: InputOutputPipeLineProcessor.java,v $
 * Revision 1.4  2011-08-22 14:29:58  L190409
 * added first pipe to interface
 *
 * Revision 1.3  2011/08/18 14:41:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now extends PipeLineProcessorBase
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.Misc;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class InputOutputPipeLineProcessor extends PipeLineProcessorBase {
	
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId,
			String message, PipeLineSession pipeLineSession, String firstPipe
			) throws PipeRunException {
		if (pipeLineSession==null) {
			pipeLineSession= new PipeLineSession();
		}
		// reset the PipeLineSession and store the message and its id in the session
		if (messageId==null) {
				messageId=Misc.createSimpleUUID();
				log.error("null value for messageId, setting to ["+messageId+"]");
	
		}
		if (message == null) {
			throw new PipeRunException(null, "Pipeline of adapter ["+ pipeLine.getOwner().getName()+"] received null message");
		}
		// store message and messageId in the pipeLineSession
		pipeLineSession.set(message, messageId);
		return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
	}

}
