/*
 * $Log: IPipeExecutor.java,v $
 * Revision 1.3  2008-01-11 10:06:05  europe\L190409
 * some rework
 *
 * Revision 1.2  2007/10/09 15:54:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD of txSupport classes
 *
 */
package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Interface for wrapping execution of a Pipe in a transaction.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IPipeExecutor {
	
	public PipeRunResult doPipeTransactional(int propagation, IPipe pipe, Object input, PipeLineSession session) throws PipeRunException;

}
