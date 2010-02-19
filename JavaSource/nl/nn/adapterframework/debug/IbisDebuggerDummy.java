/*
 * $Log: IbisDebuggerDummy.java,v $
 * Revision 1.1  2010-02-19 13:45:28  m00f069
 * - Added support for (sender) stubbing by debugger
 * - Added reply listener and reply sender to debugger
 * - Use IbisDebuggerDummy by default
 * - Enabling/disabling debugger handled by debugger instead of log level
 * - Renamed messageId to correlationId in debugger interface
 *
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
 * Default IbisDebugger implementation to be able to always have a IbisDebugger
 * available for Spring.
 * 
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.9
 * @version Id
 */
public class IbisDebuggerDummy implements IbisDebugger {

	public Object pipeLineInput(PipeLine pipeLine, String correlationId, Object input) {
		return input;
	}

	public Object pipeLineSessionKey(String correlationId, String sessionKey, Object sessionValue) {
		return sessionValue;
	}

	public String pipeLineOutput(PipeLine pipeLine, String correlationId, String output) {
		return output;
	}

	public Throwable pipeLineRollback(PipeLine pipeLine, String correlationId, Throwable throwable) {
		return throwable;
	}

	public Object pipeInput(PipeLine pipeLine, IPipe pipe, String correlationId, Object input) {
		return input;
	}
	
	public Object pipeOutput(PipeLine pipeLine, IPipe pipe, String correlationId, Object output) {
		return output;
	}

	public Throwable pipeRollback(PipeLine pipeLine, IPipe pipe, String correlationId, Throwable throwable) {
		return throwable;
	}

	public String senderInput(ISender sender, String correlationId, String input) {
		return input;
	}

	public String senderOutput(ISender sender, String correlationId, String output) {
		return output;
	}

	public Throwable senderAbort(ISender sender, String correlationId, Throwable throwable) {
		return throwable;
	}

	public String replySenderInput(IReplySender sender, String correlationId, String input) {
		return input;
	}

	public String replySenderOutput(IReplySender sender, String correlationId, String output) {
		return output;
	}

	public Throwable replySenderAbort(IReplySender sender, String correlationId, Throwable throwable) {
		return throwable;
	}

	public String replyListenerInput(IReplyListener listener, String correlationId, String input) {
		return input;
	}

	public String replyListenerOutput(IReplyListener listener, String correlationId, String output) {
		return output;
	}

	public Throwable replyListenerAbort(IReplyListener listener, String correlationId, Throwable throwable) {
		return throwable;
	}

	public void createThread(ISender sender, String threadId, String correlationId) {
	}

	public Object startThread(ISender sender, String threadId, String correlationId, String input) {
		return input;
	}

	public Object endThread(ISender sender, String correlationId, Object output) {
		return output;
	}

	public Throwable abortThread(ISender sender, String correlationId, Throwable throwable) {
		return throwable;
	}

	public Object getInputFromSessionKey(String correlationId, String sessionKey, Object sessionValue) {
		return sessionValue;
	}

	public Object getInputFromFixedValue(String correlationId, Object fixedValue) {
		return fixedValue;
	}

	public Object parameterResolvedTo(Parameter parameter, String correlationId, Object value) {
		return value;
	}
	
	public Object storeResultInSessionKey(String correlationId, String sessionKey, Object result) {
		return result;
	}
	
	public boolean stubSender(ISender sender, String correlationId) {
		return false;
	}

	public boolean stubReplySender(IReplySender sender, String correlationId) {
		return false;
	}
	
	public boolean stubReplyListener(IReplyListener listener, String correlationId) {
		return false;
	}

}
