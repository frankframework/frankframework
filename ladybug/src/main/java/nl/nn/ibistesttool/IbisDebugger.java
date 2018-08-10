package nl.nn.ibistesttool;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.parameters.Parameter;

/**
 * Interface to be implemented by an Ibis debugger. The debugger will be
 * notified of events by a call to the appropriate method for that event.
 * 
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */
public interface IbisDebugger {

	public Object pipeLineInput(PipeLine pipeLine, String correlationId, Object input);

	public Object pipeLineSessionKey(String correlationId, String sessionKey, Object sessionValue);

	public String pipeLineOutput(PipeLine pipeLine, String correlationId, String output);

	public Throwable pipeLineAbort(PipeLine pipeLine, String correlationId, Throwable throwable);

	public Object pipeInput(PipeLine pipeLine, IPipe pipe, String correlationId, Object input);
	
	public Object pipeOutput(PipeLine pipeLine, IPipe pipe, String correlationId, Object output);

	public Throwable pipeAbort(PipeLine pipeLine, IPipe pipe, String correlationId, Throwable throwable);

	public String senderInput(ISender sender, String correlationId, Object input);

	public String senderOutput(ISender sender, String correlationId, Object output);

	public Throwable senderAbort(ISender sender, String correlationId, Throwable throwable);

	public String replyListenerInput(IListener listener, String correlationId, String input);

	public String replyListenerOutput(IListener listener, String correlationId, String output);

	public Throwable replyListenerAbort(IListener listener, String correlationId, Throwable throwable);

	public void createThread(Object sourceObject, String threadId, String correlationId);

	public Object startThread(Object sourceObject, String threadId, String correlationId, Object input);

	public Object endThread(Object sourceObject, String correlationId, Object output);

	public Throwable abortThread(Object sourceObject, String correlationId, Throwable throwable);

	public Object getInputFromSessionKey(String correlationId, String sessionKey, Object sessionValue);

	public Object getInputFromFixedValue(String correlationId, Object fixedValue);

	public Object getEmptyInputReplacement(String correlationId, Object replacementValue);

	public Object parameterResolvedTo(Parameter parameter, String correlationId, Object value);
	
	public Object storeInSessionKey(String correlationId, Object sessionKey, Object result);

	public Object preserveInput(String correlationId, Object input);
	
	public boolean stubSender(ISender sender, String correlationId);
	
	public boolean stubReplyListener(IListener listener, String correlationId);

}
