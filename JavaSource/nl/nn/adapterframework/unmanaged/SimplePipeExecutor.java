/*
 * Created on 13-sep-07
 *
 */
package nl.nn.adapterframework.unmanaged;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.txsupport.IPipeExecutor;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Simple implementation of interface IPipeExecutor which leaves it
 * up to container (EJB or Spring) to ensure that each method is
 * executed with the right kind of transaction-context.
 * 
 * @author m00035f
 *
 */
public class SimplePipeExecutor implements IPipeExecutor {

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeExecutor#doPipeTxRequired(nl.nn.adapterframework.core.IPipe, java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeRunResult doPipeTxRequired(
        IPipe pipe,
        Object input,
        PipeLineSession session)
        throws PipeRunException {
        return pipe.doPipe(input, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeExecutor#doPipeTxMandatory(nl.nn.adapterframework.core.IPipe, java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeRunResult doPipeTxMandatory(
        IPipe pipe,
        Object input,
        PipeLineSession session)
        throws PipeRunException {
            return pipe.doPipe(input, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeExecutor#doPipeTxRequiresNew(nl.nn.adapterframework.core.IPipe, java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeRunResult doPipeTxRequiresNew(
        IPipe pipe,
        Object input,
        PipeLineSession session)
        throws PipeRunException {
            return pipe.doPipe(input, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeExecutor#doPipeTxSupports(nl.nn.adapterframework.core.IPipe, java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeRunResult doPipeTxSupports(
        IPipe pipe,
        Object input,
        PipeLineSession session)
        throws PipeRunException {
            return pipe.doPipe(input, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeExecutor#doPipeTxNotSupported(nl.nn.adapterframework.core.IPipe, java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeRunResult doPipeTxNotSupported(
        IPipe pipe,
        Object input,
        PipeLineSession session)
        throws PipeRunException {
            return pipe.doPipe(input, session);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPipeExecutor#doPipeTxNever(nl.nn.adapterframework.core.IPipe, java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
     */
    public PipeRunResult doPipeTxNever(
        IPipe pipe,
        Object input,
        PipeLineSession session)
        throws PipeRunException {
            return pipe.doPipe(input, session);
    }

}
