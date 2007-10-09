/*
 * $Log: IPipeLineExecutor.java,v $
 * Revision 1.2  2007-10-09 15:54:43  europe\L190409
 * Direct copy from Ibis-EJB:
 * first version in HEAD of txSupport classes
 *
 */
package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * Interface for wrapping execution of a PipeLine in a transaction.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IPipeLineExecutor {
    PipeLineResult doPipeLineTxRequired (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxMandatory (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxRequiresNew (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxSupports (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxNotSupported (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxNever (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;

}
