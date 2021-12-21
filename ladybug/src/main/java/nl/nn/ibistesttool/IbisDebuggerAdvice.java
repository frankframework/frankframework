/*
   Copyright 2018-2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.ibistesttool;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.ApplicationListener;

import nl.nn.adapterframework.core.IBlockEnabledSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.IsolatedServiceExecutor;
import nl.nn.adapterframework.processors.CacheSenderWrapperProcessor;
import nl.nn.adapterframework.processors.CheckSemaphorePipeProcessor;
import nl.nn.adapterframework.processors.CorePipeLineProcessor;
import nl.nn.adapterframework.processors.InputOutputPipeProcessor;
import nl.nn.adapterframework.senders.ParallelSenderExecutor;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.IStreamingSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.api.DebuggerStatusChangedEvent;

/**
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */
public class IbisDebuggerAdvice implements ThreadLifeCycleEventListener<Object>, ApplicationListener<DebuggerStatusChangedEvent> {
	protected Logger log = LogUtil.getLogger(this);

	// Contract for testtool state:
	// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
	// - to get notified of changes, components should listen to DebuggerStatusChangedEvents
	// IbisDebuggerAdvice stores state in appconstants testtool.enabled for use by GUI
	private IbisDebugger ibisDebugger;
	private static boolean enabled=true;
	
	private AtomicInteger threadCounter = new AtomicInteger(0);
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

	/**
	 * Provides advice for {@link CorePipeLineProcessor#processPipeLine(PipeLine pipeLine, String messageId, Message message, IPipeLineSession pipeLineSession, String firstPipe)}
	 */
	public PipeLineResult debugPipeLineInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, String correlationId, Message message, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (PipeLineResult)proceedingJoinPoint.proceed();
		}
		message = ibisDebugger.pipeLineInput(pipeLine, correlationId, message);
		TreeSet<String> keys = new TreeSet<String>(pipeLineSession.keySet());
		Iterator<String> iterator = keys.iterator();
		while (iterator.hasNext()) {
			String sessionKey = iterator.next();
			Object sessionValue = pipeLineSession.get(sessionKey);
			sessionValue = ibisDebugger.pipeLineSessionKey(correlationId, sessionKey, sessionValue);
			pipeLineSession.put(sessionKey, sessionValue);
		}
		PipeLineResult pipeLineResult = null;
		try {
			PipeLineSessionBase pipeLineSessionDebugger = PipeLineSessionDebugger.newInstance((PipeLineSessionBase)pipeLineSession, ibisDebugger);
			Object[] args = proceedingJoinPoint.getArgs();
			args[3] = pipeLineSessionDebugger;
			pipeLineResult = (PipeLineResult)proceedingJoinPoint.proceed(args);
		} catch(Throwable throwable) {
			throw ibisDebugger.pipeLineAbort(pipeLine, correlationId, throwable);
		}
		pipeLineResult.setResult(ibisDebugger.pipeLineOutput(pipeLine, correlationId, pipeLineResult.getResult()));
		return pipeLineResult;
	}

	/**
	 * Provides advice for {@link InputOutputPipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession)}
	 */
	public PipeRunResult debugPipeInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		String messageId = pipeLineSession.getMessageId();
		message = ibisDebugger.pipeInput(pipeLine, pipe, messageId, message);
		PipeRunResult pipeRunResult = null;
		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = message;
			pipeRunResult = (PipeRunResult)proceedingJoinPoint.proceed(args); // in case of 'preserveInput', this result is already replaced with the preserved input
		} catch(Throwable throwable) {
			throw ibisDebugger.pipeAbort(pipeLine, pipe, messageId, throwable);
		}
		if (pipe instanceof IExtendedPipe && ((IExtendedPipe)pipe).isPreserveInput()) {
			// signal in the debugger that the result of the pipe has been replaced with the original input
			pipeRunResult.setResult(ibisDebugger.preserveInput(messageId, pipeRunResult.getResult()));
		}
		pipeRunResult.setResult(ibisDebugger.pipeOutput(pipeLine, pipe, messageId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	/**
	 * Provides advice for {@link CheckSemaphorePipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession)}
	 * CheckSemaphorePipeProcessor is just after InputOutputPipeProcessor, so it sees the effect of the replacements made by the latter.
	 */
	public PipeRunResult debugPipeGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		if (pipe instanceof IExtendedPipe) {
			IExtendedPipe pe = (IExtendedPipe)pipe;
			String messageId = pipeLineSession.getMessageId();
			message = debugGetInputFrom(pipeLineSession, messageId, message,
					pe.getGetInputFromSessionKey(),
					pe.getGetInputFromFixedValue(),
					pe.getEmptyInputReplacement());
		}
		Object[] args = proceedingJoinPoint.getArgs();
		args[2] = message;
		return (PipeRunResult)proceedingJoinPoint.proceed(args); // the PipeRunResult contains the original result, before replacing via preserveInput
	}

	private <M> M debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, IPipeLineSession session, int messageParamIndex, boolean expectPipeRunResult) throws Throwable {
		if (!isEnabled()) {
			return (M)proceedingJoinPoint.proceed();
		}
		ISender sender = (ISender)proceedingJoinPoint.getTarget();
		if (!sender.isSynchronous() && sender instanceof JmsSender) {
			// Ignore JmsSenders within JmsListeners (calling JmsSender without ParameterResolutionContext) within Receivers.
			return (M)proceedingJoinPoint.proceed();
		} 

		String messageId = session == null ? null : session.getMessageId();
		message = ibisDebugger.senderInput(sender, messageId, message); 

		M result = null; // result can be PipeRunResult (for StreamingSenders) or Message (for all other Senders)
		// For SenderWrapperBase continue even when it needs to be stubbed
		// because for SenderWrapperBase this will be checked when it calls
		// sendMessage on his senderWrapperProcessor, hence
		// debugSenderGetInputFrom will be called.
		if (!ibisDebugger.stubSender(sender, messageId) || sender instanceof SenderWrapperBase) {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[messageParamIndex] = message;
				result = (M)proceedingJoinPoint.proceed(args);
			} catch(Throwable throwable) {
				throw ibisDebugger.senderAbort(sender, messageId, throwable);
			}
		} else {
			// Resolve parameters so they will be added to the report like when the sender was not stubbed and would
			// resolve parameters itself
			if (sender instanceof IWithParameters) {
				ParameterList parameterList = ((IWithParameters)sender).getParameterList();
				if (parameterList!=null) {
					parameterList.getValues(message, session);
				}
			}
		}
		if (sender instanceof SenderWrapperBase && ((SenderWrapperBase)sender).isPreserveInput()) {
			// signal in the debugger that the result of the sender has been replaced with the original input
			result = (M)ibisDebugger.preserveInput(messageId, (Message)result);
		}
		if (expectPipeRunResult) {
			// Create PipeRunResult when streaming sender is stubbed, this will forward to the next pipe and process the
			// message in a streaming.auto=false way (also when at the time of the original report the message was
			// processed with streaming.auto=true)
			PipeRunResult prr = result!=null ? (PipeRunResult)result : new PipeRunResult();
			prr.setResult(ibisDebugger.senderOutput(sender, messageId, prr.getResult()));
			return (M)prr;
		}
		return (M)ibisDebugger.senderOutput(sender, messageId, Message.asMessage(result));
	}
	
	/**
	 * Provides advice for {@link ISender#sendMessage(Message message, IPipeLineSession session)}
	 */
	public Message debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, IPipeLineSession session) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 0, false);
	}

	/**
	 * Provides advice for {@link IBlockEnabledSender#sendMessage(Object blockHandle, Message message, IPipeLineSession session)}
	 */
	public Message debugBlockEnabledSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Object blockHandle, Message message, IPipeLineSession session) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 1, false);
	}

	/**
	 * Provides advice for {@link IStreamingSender#sendMessage(Message message, IPipeLineSession session, IForwardTarget next)}
	 */
	public PipeRunResult debugStreamingSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, IPipeLineSession session, IForwardTarget next) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 0, true);
	}
	 
	/**
	 * Provides advice for {@link IOutputStreamingSupport#provideOutputStream(IPipeLineSession session, IForwardTarget next)}
	 * Provides advice for {@link StreamingPipe#provideOutputStream(IPipeLineSession session)}
	 */
	public MessageOutputStream debugProvideOutputStream(ProceedingJoinPoint proceedingJoinPoint, IPipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (MessageOutputStream)proceedingJoinPoint.proceed();
		}
		String correlationId = session == null ? null : session.getMessageId();
		if (log.isDebugEnabled()) log.debug("debugProvideOutputStream thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] correlationId ["+correlationId+"]");
		if (proceedingJoinPoint.getTarget() instanceof ISender) {
			ISender sender = (ISender)proceedingJoinPoint.getTarget();
			// Use WriterPlaceHolder to make the contents that is later written to the MessageOutputStream appear as input of the Sender
			WriterPlaceHolder writerPlaceHolder = ibisDebugger.senderInput(sender, correlationId, new WriterPlaceHolder());
			MessageOutputStream resultStream = (MessageOutputStream)proceedingJoinPoint.proceed();
			String resultMessage = handleMessageOutputStream(writerPlaceHolder, resultStream);
			ibisDebugger.senderOutput(sender, correlationId, resultMessage);
			return resultStream;
		}
		if (proceedingJoinPoint.getTarget() instanceof IPipe) {
			IPipe pipe = (IPipe)proceedingJoinPoint.getTarget();
			PipeLine pipeLine = pipe instanceof AbstractPipe ? ((AbstractPipe)pipe).getPipeLine() : new PipeLine();
			// Use WriterPlaceHolder to make the contents that is later written to the MessageOutputStream appear as input of the Pipe
			WriterPlaceHolder writerPlaceHolder = ibisDebugger.pipeInput(pipeLine, pipe, correlationId, new WriterPlaceHolder());
			MessageOutputStream resultStream = (MessageOutputStream)proceedingJoinPoint.proceed();
			String resultMessage = handleMessageOutputStream(writerPlaceHolder, resultStream);
			ibisDebugger.pipeOutput(pipeLine, pipe, correlationId, resultMessage);
			return resultStream;
		}
		log.warn("Could not identify outputstream provider ["+proceedingJoinPoint.getTarget().getClass().getName()+"] as pipe or sender");
		return (MessageOutputStream)proceedingJoinPoint.proceed();
	}

	private String handleMessageOutputStream(WriterPlaceHolder writerPlaceHolder, MessageOutputStream resultStream) throws IOException {
		if (writerPlaceHolder!=null && writerPlaceHolder.getWriter()!=null) {
			if (resultStream!=null) {
				resultStream.captureCharacterStream(writerPlaceHolder.getWriter(), writerPlaceHolder.getSizeLimit());
			} else {
				try (Writer writer = writerPlaceHolder.getWriter()){ 
					writer.write("<--> request to provide outputstream could not be honored");
					writer.close();
				}
			}
		} 
		return resultStream!=null ? "<-- outputstream provided" : "<-- no outputstream could be provided";
	}
	
	/**
	 * Provides advice for {@link CacheSenderWrapperProcessor#sendMessage(SenderWrapperBase senderWrapperBase, Message message, IPipeLineSession session)}
	 */
	public Message debugSenderGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, SenderWrapperBase senderWrapperBase, Message message, IPipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (Message)proceedingJoinPoint.proceed();
		}
		String messageId = session == null ? null : session.getMessageId();
		message = debugGetInputFrom(session, messageId, message, 
				senderWrapperBase.getGetInputFromSessionKey(), 
				senderWrapperBase.getGetInputFromFixedValue(), 
				null);
		if (ibisDebugger.stubSender(senderWrapperBase, messageId)) {
			return null;
		} else {
			Object[] args = proceedingJoinPoint.getArgs();
			args[1] = message;
			return (Message)proceedingJoinPoint.proceed(args); // this message contains the original result, before replacing via preserveInput
		}
	}

	public <M> M debugReplyListenerInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, ICorrelatedPullingListener<M> listener, String correlationId, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (M)proceedingJoinPoint.proceed();
		}
		correlationId = ibisDebugger.replyListenerInput(listener, pipeLineSession.getMessageId(), correlationId);
		M result = null;
		if (ibisDebugger.stubReplyListener(listener, correlationId)) {
			return null;
		} else {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[1] = correlationId;
				result = (M)proceedingJoinPoint.proceed(args);
			} catch(Throwable throwable) {
				throw ibisDebugger.replyListenerAbort(listener, pipeLineSession.getMessageId(), throwable);
			}
			return ibisDebugger.replyListenerOutput(listener, pipeLineSession.getMessageId(), result);
		}
	}

	public Object debugThreadCreateStartEndAbort(ProceedingJoinPoint proceedingJoinPoint, Runnable runnable) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		if (runnable instanceof ParallelSenderExecutor || runnable instanceof IsolatedServiceExecutor) {
			Executor executor = new Executor((RequestReplyExecutor)runnable,(ThreadLifeCycleEventListener<Object>)this);
			Object[] args = proceedingJoinPoint.getArgs();
			args[0] = executor;
			return proceedingJoinPoint.proceed(args);
		} else {
			return proceedingJoinPoint.proceed();
		}
	}

	@Override
	public ThreadDebugInfo announceChildThread(Object owner, String correlationId) {
		if (!isEnabled()) {
			return null;
		}
		ThreadDebugInfo threadInfo = new ThreadDebugInfo();
		threadInfo.owner = owner;
		threadInfo.correlationId = correlationId;
		threadInfo.threadId = Integer.toString(threadCounter.incrementAndGet());
		if (log.isDebugEnabled()) {
			String nameClause=threadInfo.owner instanceof INamedObject?" name ["+((INamedObject)threadInfo.owner).getName()+"]":"";
			log.debug("announceChildThread thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+threadInfo.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+threadInfo.threadId+"] correlationId ["+threadInfo.correlationId+"]");
		}
		ibisDebugger.createThread(threadInfo.owner, threadInfo.threadId, threadInfo.correlationId);
		return threadInfo;
	}

	@Override
	public void cancelChildThread(Object handle) {
		if (!isEnabled()) {
			return;
		}
		ThreadDebugInfo ref = (ThreadDebugInfo)handle;
		ibisDebugger.cancelThread(ref.owner, ref.threadId, ref.correlationId);
	}

	@Override
	public Object threadCreated(Object handle, Object request) {
		if (!isEnabled()) {
			return null;
		}
		ThreadDebugInfo ref = (ThreadDebugInfo)handle;
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject?" name ["+((INamedObject)ref.owner).getName()+"]":"";
			log.debug("threadCreated thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+ref.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+ref.threadId+"] correlationId ["+ref.correlationId+"]");
		}
		return ibisDebugger.startThread(ref.owner, ref.threadId, ref.correlationId, request);
	}

	@Override
	public Object threadEnded(Object handle, Object result) {
		if (!isEnabled()) {
			return null;
		}
		ThreadDebugInfo ref = (ThreadDebugInfo)handle;
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject?" name ["+((INamedObject)ref.owner).getName()+"]":"";
			log.debug("threadEnded thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+ref.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+ref.threadId+"] correlationId ["+ref.correlationId+"]");
		}
		return ibisDebugger.endThread(ref.owner, ref.correlationId, result);
	}

	@Override
	public Throwable threadAborted(Object handle, Throwable t) {
		if (!isEnabled()) {
			return null;
		}
		ThreadDebugInfo ref = (ThreadDebugInfo)handle;
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject?" name ["+((INamedObject)ref.owner).getName()+"]":"";
			log.debug("threadAborted thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+ref.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+ref.threadId+"] correlationId ["+ref.correlationId+"]");
		}
		return ibisDebugger.abortThread(ref.owner, ref.correlationId, t);
	}
	
	/**
	 * Provides advice for {@link Parameter#getValue(ParameterValueList alreadyResolvedParameters, Message message, IPipeLineSession session, boolean namespaceAware)}
	 */
	public Object debugParameterResolvedTo(ProceedingJoinPoint proceedingJoinPoint, ParameterValueList alreadyResolvedParameters, Message message, IPipeLineSession session, boolean namespaceAware) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		Object result = proceedingJoinPoint.proceed();
		Parameter parameter = (Parameter)proceedingJoinPoint.getTarget();
		return ibisDebugger.parameterResolvedTo(parameter, session==null?null:session.getMessageId(), result); // session is null in afterMessageProcessed()
	}

	private Message debugGetInputFrom(IPipeLineSession pipeLineSession, String correlationId, Message input, String inputFromSessionKey, String inputFromFixedValue, String emptyInputReplacement) {
		if (StringUtils.isNotEmpty(inputFromSessionKey)) {
			input = Message.asMessage(pipeLineSession.get(inputFromSessionKey));
			input = (Message)ibisDebugger.getInputFromSessionKey(correlationId, inputFromSessionKey, input);
		}
		if (StringUtils.isNotEmpty(inputFromFixedValue)) {
			input =  Message.asMessage(ibisDebugger.getInputFromFixedValue(correlationId, inputFromFixedValue));
		}
		if (input == null || input.isEmpty()) {
			if (StringUtils.isNotEmpty(emptyInputReplacement)) {
				input = Message.asMessage(ibisDebugger.getEmptyInputReplacement(correlationId, emptyInputReplacement));
			}
		}
		return input;
	}
	

	public class Executor implements Runnable {
		private RequestReplyExecutor requestReplyExecutor;
		private ThreadConnector threadConnector;

		public Executor(RequestReplyExecutor requestReplyExecutor, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener) {
			this.requestReplyExecutor=requestReplyExecutor;
			this.threadConnector = new ThreadConnector(requestReplyExecutor, threadLifeCycleEventListener, requestReplyExecutor.getCorrelationID());
		}
		
		@Override
		public void run() {
			threadConnector.startThread(requestReplyExecutor.getRequest());
			try {
				requestReplyExecutor.run();
			} finally {
				Throwable throwable = requestReplyExecutor.getThrowable();
				if (throwable == null) {
					Message reply = requestReplyExecutor.getReply();
					reply = (Message)threadConnector.endThread(reply);
					requestReplyExecutor.setReply(reply);
				} else {
					throwable = threadConnector.abortThread(throwable);
					requestReplyExecutor.setThrowable(throwable);
				}
			}
		}

	}

	public void setEnabled(boolean enable) {
		enabled = enable;
		AppConstants.getInstance().put("testtool.enabled", ""+enable);
	}
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void onApplicationEvent(DebuggerStatusChangedEvent event) {
		setEnabled(event.isEnabled());
	}

}
