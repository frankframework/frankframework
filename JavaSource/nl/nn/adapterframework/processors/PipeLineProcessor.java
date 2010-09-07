/*
 * $Log: PipeLineProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * @author Jaco de Groot
 * @version Id
 */
public interface PipeLineProcessor {

	PipeLineResult processPipeLine(PipeLine pipeLine, String messageId,
			String message, PipeLineSession pipeLineSession
			) throws PipeRunException;

}
