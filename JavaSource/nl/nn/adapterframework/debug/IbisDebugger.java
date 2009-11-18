/*
 * $Log: IbisDebugger.java,v $
 * Revision 1.2  2009-11-18 17:28:04  m00f069
 * Added senders to IbisDebugger
 *
 * Revision 1.1  2008/07/14 17:07:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of debugger
 *
 */
package nl.nn.adapterframework.debug;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.IsolatedServiceCaller;

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

	public String pipeLineOutput(PipeLine pipeLine, String messageId, String output);

	public Throwable pipeLineRollback(PipeLine pipeLine, String messageId, Throwable throwable);

	public Object pipeInput(PipeLine pipeLine, IPipe pipe, String messageId, Object input);
	
	public Object pipeOutput(PipeLine pipeLine, IPipe pipe, String messageId, Object output);

	public String senderInput(ISender sender, String messageId, Object input);

	public String senderOutput(ISender sender, String messageId, Object output);

	public void createThread(String threadId, String messageId);

	public Object startThread(String threadId, String messageId, String input);

	public Object endThread(String messageId, Object output);

	public Throwable abortThread(String messageId, Throwable throwable);

	public Object getInputFromSessionKey(String messageId, String sessionKey, Object sessionValue);

	public Object getInputFromFixedValue(String messageId, Object fixedValue);

	public Object parameterResolvedTo(Parameter parameter, String messageId, Object value);
	
	public Object storeResultInSessionKey(String messageId, String sessionKey, Object result);

}
