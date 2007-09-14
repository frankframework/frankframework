/*
 * Created on 14-sep-07
 *
 */
package nl.nn.adapterframework.unmanaged;

import nl.nn.adapterframework.core.IPipeLineExecutor;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * @author m00035f
 *
 */
public class SimplePipeLineExecutor implements IPipeLineExecutor {

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxRequired(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxRequired(
        PipeLine pipeLine,
        String messageId,
        String message,
        PipeLineSession session)
        throws PipeRunException {
        return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxMandatory(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxMandatory(
        PipeLine pipeLine,
        String messageId,
        String message,
        PipeLineSession session)
        throws PipeRunException {
            return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxRequiresNew(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxRequiresNew(
        PipeLine pipeLine,
        String messageId,
        String message,
        PipeLineSession session)
        throws PipeRunException {
            return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxSupports(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxSupports(
        PipeLine pipeLine,
        String messageId,
        String message,
        PipeLineSession session)
        throws PipeRunException {
            return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxNotSupported(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxNotSupported(
        PipeLine pipeLine,
        String messageId,
        String message,
        PipeLineSession session)
        throws PipeRunException {
            return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxNever(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxNever(
        PipeLine pipeLine,
        String messageId,
        String message,
        PipeLineSession session)
        throws PipeRunException {
            return pipeLine.processPipeLine(messageId, message, session);
    }

}
