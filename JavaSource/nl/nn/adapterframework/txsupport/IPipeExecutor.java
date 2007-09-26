/*
 * Created on 13-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.*;

/**
 * @author m00035f
 *
 */
public interface IPipeExecutor {
    PipeRunResult doPipeTxRequired (IPipe pipe, Object input, PipeLineSession session) throws PipeRunException;
    PipeRunResult doPipeTxMandatory (IPipe pipe, Object input, PipeLineSession session) throws PipeRunException;
    PipeRunResult doPipeTxRequiresNew (IPipe pipe, Object input, PipeLineSession session) throws PipeRunException;
    PipeRunResult doPipeTxSupports (IPipe pipe, Object input, PipeLineSession session) throws PipeRunException;
    PipeRunResult doPipeTxNotSupported (IPipe pipe, Object input, PipeLineSession session) throws PipeRunException;
    PipeRunResult doPipeTxNever (IPipe pipe, Object input, PipeLineSession session) throws PipeRunException;

}
