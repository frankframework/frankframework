/*
 * $Log: IbisDebugger.java,v $
 * Revision 1.1  2008-07-14 17:07:32  europe\L190409
 * first version of debugger
 *
 */
package nl.nn.adapterframework.debug;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.parameters.Parameter;

/**
 * Interface to be implemented by debuggers of Ibis Adapters. Debuggers are notified of
 * events by a call to the appropriate method for that event.
 * 
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.9
 * @version Id
 */
public interface IbisDebugger {

	public Object pipeLineInput(PipeLine pipeLine, String messageId, Object input);

	public Object pipeInput(PipeLine pipeLine, IPipe pipe, String messageId, Object input);
	
	public Object pipeGetInputFromSessionKey(PipeLine pipeLine, IExtendedPipe pipe, String messageId, Object sessionValue);

	public Object pipeGetInputFromFixedValue(PipeLine pipeLine, IExtendedPipe pipe, String messageId, Object fixedValue);

	public Object parameterResolvedTo(Parameter parameter, String messageId, Object value);
	
	public Object pipeStoreResultInSessionKey(PipeLine pipeLine, IExtendedPipe pipe, String messageId, Object result);
	
	public Object pipeOutput(PipeLine pipeLine, IPipe pipe, String messageId, Object output);

	public String pipeLineOutput(PipeLine pipeLine, String messageId, String output);

	public void pipeLineRollback(PipeLine pipeLine, String messageId, String message);

}
