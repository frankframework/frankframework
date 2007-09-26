/*
 * Created on 14-sep-07
 *
 */
package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.*;

/**
 * Interface for wrapping execution of a PipeLine in a transaction
 * 
 * @author m00035f
 *
 */
public interface IPipeLineExecutor {
    PipeLineResult doPipeLineTxRequired (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxMandatory (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxRequiresNew (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxSupports (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxNotSupported (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;
    PipeLineResult doPipeLineTxNever (PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException;

}
