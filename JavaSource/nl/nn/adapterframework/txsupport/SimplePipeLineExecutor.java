/*
 * $Log: SimplePipeLineExecutor.java,v $
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
 * Simple implementation of interface IPipeLineExecutor which leaves it
 * up to container (EJB or Spring) to ensure that each method is
 * executed with the right kind of transaction-context.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class SimplePipeLineExecutor implements IPipeLineExecutor {

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxRequired(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxRequired(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
		return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxMandatory(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxMandatory(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
		return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxRequiresNew(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxRequiresNew( PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
		return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxSupports(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxSupports(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
		return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxNotSupported(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxNotSupported(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
		return pipeLine.processPipeLine(messageId, message, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeLineExecutor#doPipeLineTxNever(nl.nn.adapterframework.core.PipeLine, java.lang.String, java.lang.String, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeLineResult doPipeLineTxNever(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
		return pipeLine.processPipeLine(messageId, message, session);
    }

}
