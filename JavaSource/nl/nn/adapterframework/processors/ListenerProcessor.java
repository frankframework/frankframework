/*
 * $Log: ListenerProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeOutException;

/**
 * @author Jaco de Groot
 * @version Id
 */
public interface ListenerProcessor {

	public String getMessage(ICorrelatedPullingListener listener,
			String correlationID, PipeLineSession pipeLineSession
			) throws ListenerException, TimeOutException;

}
