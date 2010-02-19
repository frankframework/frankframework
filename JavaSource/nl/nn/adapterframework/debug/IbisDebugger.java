/*
 * $Log: IbisDebugger.java,v $
 * Revision 1.5  2010-02-19 13:45:28  m00f069
 * - Added support for (sender) stubbing by debugger
 * - Added reply listener and reply sender to debugger
 * - Use IbisDebuggerDummy by default
 * - Enabling/disabling debugger handled by debugger instead of log level
 * - Renamed messageId to correlationId in debugger interface
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
import nl.nn.adapterframework.core.IReplyListener;
import nl.nn.adapterframework.core.IReplySender;
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

	public Object pipeLineInput(PipeLine pipeLine, String correlationId, Object input);

	public Object pipeLineSessionKey(String correlationId, String sessionKey, Object sessionValue);

	public String pipeLineOutput(PipeLine pipeLine, String correlationId, String output);

	public Throwable pipeLineRollback(PipeLine pipeLine, String correlationId, Throwable throwable);

	public Object pipeInput(PipeLine pipeLine, IPipe pipe, String correlationId, Object input);
	
	public Object pipeOutput(PipeLine pipeLine, IPipe pipe, String correlationId, Object output);

	public Throwable pipeRollback(PipeLine pipeLine, IPipe pipe, String correlationId, Throwable throwable);

	public String senderInput(ISender sender, String correlationId, String input);

	public String senderOutput(ISender sender, String correlationId, String output);

	public Throwable senderAbort(ISender sender, String correlationId, Throwable throwable);

	public String replySenderInput(IReplySender sender, String correlationId, String input);

	public String replySenderOutput(IReplySender sender, String correlationId, String output);

	public Throwable replySenderAbort(IReplySender sender, String correlationId, Throwable throwable);

	public String replyListenerInput(IReplyListener listener, String correlationId, String input);

	public String replyListenerOutput(IReplyListener listener, String correlationId, String output);

	public Throwable replyListenerAbort(IReplyListener listener, String correlationId, Throwable throwable);

	public void createThread(ISender sender, String threadId, String correlationId);

	public Object startThread(ISender sender, String threadId, String correlationId, String input);

	public Object endThread(ISender sender, String correlationId, Object output);

	public Throwable abortThread(ISender sender, String correlationId, Throwable throwable);

	public Object getInputFromSessionKey(String correlationId, String sessionKey, Object sessionValue);

	public Object getInputFromFixedValue(String correlationId, Object fixedValue);

	public Object parameterResolvedTo(Parameter parameter, String correlationId, Object value);
	
	public Object storeResultInSessionKey(String correlationId, String sessionKey, Object result);
	
	public boolean stubSender(ISender sender, String correlationId);
	
	public boolean stubReplySender(IReplySender sender, String correlationId);
	
	public boolean stubReplyListener(IReplyListener listener, String correlationId);

}
