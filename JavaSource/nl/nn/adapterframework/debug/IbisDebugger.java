/*
 * $Log: IbisDebugger.java,v $
 * Revision 1.6  2010-03-10 14:30:05  m168309
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.4  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.3  2009/11/27 13:38:20  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Expose available session keys at the beginning of the pipeline to the debugger
 *
 * Revision 1.2  2009/11/18 17:28:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.1  2008/07/14 17:07:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of debugger
 *
 */
package nl.nn.adapterframework.debug;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
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

	public Object pipeLineSessionKey(String messageId, String sessionKey, Object sessionValue);

	public String pipeLineOutput(PipeLine pipeLine, String messageId, String output);

	public Throwable pipeLineRollback(PipeLine pipeLine, String messageId, Throwable throwable);

	public Object pipeInput(PipeLine pipeLine, IPipe pipe, String messageId, Object input);
	
	public Object pipeOutput(PipeLine pipeLine, IPipe pipe, String messageId, Object output);

	public Throwable pipeRollback(PipeLine pipeLine, IPipe pipe, String messageId, Throwable throwable);

	public String senderInput(ISender sender, String messageId, Object input);

	public String senderOutput(ISender sender, String messageId, Object output);

	public Throwable senderAbort(ISender sender, String messageId, Throwable throwable);

	public void createThread(String threadId, String messageId);

	public Object startThread(String threadId, String messageId, String input);

	public Object endThread(String messageId, Object output);

	public Throwable abortThread(String messageId, Throwable throwable);

	public Object getInputFromSessionKey(String messageId, String sessionKey, Object sessionValue);

	public Object getInputFromFixedValue(String messageId, Object fixedValue);

	public Object parameterResolvedTo(Parameter parameter, String messageId, Object value);
	
	public Object storeResultInSessionKey(String messageId, String sessionKey, Object result);

}
