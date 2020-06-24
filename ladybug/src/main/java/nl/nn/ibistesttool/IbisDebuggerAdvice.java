/*
   Copyright 2018-2020 Nationale-Nederlanden

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
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
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
	// - to get notified of canges, components should listen to DebuggerStatusChangedEvents
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
			PipeLineSessionDebugger pipeLineSessionDebugger = new PipeLineSessionDebugger(pipeLineSession);
			pipeLineSessionDebugger.setIbisDebugger(ibisDebugger);
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
		boolean preserveInput = pipe instanceof IExtendedPipe && ((IExtendedPipe)pipe).isPreserveInput();
		Message preservedInput=null;
		if (preserveInput) {
			message.preserve();
			preservedInput = message;
		}
		String messageId = pipeLineSession.getMessageId();
		message = ibisDebugger.pipeInput(pipeLine, pipe, messageId, message);
		PipeRunResult pipeRunResult = null;
		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = message;
			pipeRunResult = (PipeRunResult)proceedingJoinPoint.proceed(args);
		} catch(Throwable throwable) {
			throw ibisDebugger.pipeAbort(pipeLine, pipe, messageId, throwable);
		}
		if (preserveInput) {
			pipeRunResult.setResult(ibisDebugger.preserveInput(messageId, preservedInput));
		}
		pipeRunResult.setResult(ibisDebugger.pipeOutput(pipeLine, pipe, messageId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	/**
	 * Provides advice for {@link CheckSemaphorePipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession)}
	 */
	public Object debugPipeGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		if (pipe instanceof IExtendedPipe) {
			IExtendedPipe pe = (IExtendedPipe)pipe;
			String messageId = pipeLineSession.getMessageId();
			message = (Message)debugGetInputFrom(pipeLineSession, messageId, message,
					pe.getGetInputFromSessionKey(),
					pe.getGetInputFromFixedValue(),
					pe.getEmptyInputReplacement());
		}
		Object[] args = proceedingJoinPoint.getArgs();
		args[2] = message;
		return proceedingJoinPoint.proceed(args);
	}

	/**
	 * Provides advice for {@link ISender#sendMessage(Message message, IPipeLineSession session)}
	 */
	public Message debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, IPipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (Message)proceedingJoinPoint.proceed();
		}
		String correlationId = session==null ? null : session.getMessageId();
		ISender sender = (ISender)proceedingJoinPoint.getTarget();
		if (!sender.isSynchronous() && sender instanceof JmsSender) {
			// Ignore JmsSenders within JmsListeners (calling JmsSender without
			// ParameterResolutionContext) within Receivers.
			return (Message)proceedingJoinPoint.proceed();
		} 
		boolean preserveInput = sender instanceof SenderWrapperBase && ((SenderWrapperBase)sender).isPreserveInput();
		Message preservedInput=null;
		if (preserveInput) {
			message.preserve();
			preservedInput = message;
		}
		Message result = debugSenderInputAbort(proceedingJoinPoint, sender, correlationId, message);
		if (sender instanceof ISenderWithParameters && ibisDebugger.stubSender(sender, correlationId)) {
			ISenderWithParameters psender = (ISenderWithParameters)sender;
			// Resolve parameters so they will be added to the report like when the sender was not stubbed and would
			// resolve parameters itself
			ParameterList parameterList = psender.getParameterList();
			if (parameterList!=null) {
				parameterList.getValues(message, session);
			}
		}
		if (preserveInput) {
			result = ibisDebugger.preserveInput(correlationId, preservedInput);
		}
		return ibisDebugger.senderOutput(sender, correlationId, result);
	}
	
	private Message debugSenderInputAbort(ProceedingJoinPoint proceedingJoinPoint, ISender sender, String correlationId, Message message) throws Throwable {
		message = ibisDebugger.senderInput(sender, correlationId, message);
		Message result = null;
		// For SenderWrapperBase continue even when it needs to be stubbed
		// because for SenderWrapperBase this will be checked when it calls
		// sendMessage on his senderWrapperProcessor, hence
		// debugSenderGetInputFrom will be called.
		if (!ibisDebugger.stubSender(sender, correlationId) || sender instanceof SenderWrapperBase) {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[0] = message;
				result = (Message)proceedingJoinPoint.proceed(args);
			} catch(Throwable throwable) {
				throw ibisDebugger.senderAbort(sender, correlationId, throwable);
			}
		}
		return result;
	}


	private Object debugSimpleSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, IPipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		String correlationId = session == null ? null : session.getMessageId();
		ISender sender = (ISender)proceedingJoinPoint.getTarget();
		message = ibisDebugger.senderInput(sender, correlationId, message); 

		Object result = null;
		if (!ibisDebugger.stubSender(sender, correlationId)) {
			try {
				result = proceedingJoinPoint.proceed();
			} catch(Throwable throwable) {
				throw ibisDebugger.senderAbort(sender, correlationId, throwable);
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
		if (result!=null && result instanceof PipeRunResult) {
			PipeRunResult prr = (PipeRunResult)result;
			prr.setResult(ibisDebugger.senderOutput(sender, correlationId, prr.getResult()));
			return result;
		}
		return ibisDebugger.senderOutput(sender, correlationId, Message.asMessage(result));
	}
	
	/**
	 * Provides advice for {@link IBlockEnabledSender#sendMessage(Object blockHandle, Message message, IPipeLineSession session)}
	 */
	public Object debugBlockEnabledSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Object blockHandle, Message message, IPipeLineSession session) throws Throwable {
		return debugSimpleSenderInputOutputAbort(proceedingJoinPoint, message, session);
	}

	/**
	 * Provides advice for {@link IStreamingSender#sendMessage(Message message, IPipeLineSession session, IForwardTarget next)}
	 */
	public Object debugStreamingSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, IPipeLineSession session, IForwardTarget nexte) throws Throwable {
		return debugSimpleSenderInputOutputAbort(proceedingJoinPoint, message, session);
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
		// TODO: provide proper debug entry in Debugger interface.
		if (proceedingJoinPoint.getTarget() instanceof ISender) {
			ISender sender = (ISender)proceedingJoinPoint.getTarget();
			ibisDebugger.senderInput(sender, correlationId, new Message("--> provide outputstream"));
			//System.out.println("--> provide outputstream of sender ["+sender.getName()+"]");
			MessageOutputStream result = (MessageOutputStream)proceedingJoinPoint.proceed();
			//System.out.println("<-- provide outputstream of sender ["+sender.getName()+"]: ["+result+"]");
			ibisDebugger.senderOutput(sender, correlationId, new Message(result==null?null:result.toString()));
			return result;
		} else {
			if (proceedingJoinPoint.getTarget() instanceof IPipe) {
				IPipe pipe = (IPipe)proceedingJoinPoint.getTarget();
				//System.out.println("--> provide outputstream of pipe ["+pipe.getName()+"]");
				PipeLine pipeLine = pipe instanceof AbstractPipe ? ((AbstractPipe)pipe).getPipeLine() : new PipeLine();
				ibisDebugger.pipeInput(pipeLine, pipe, correlationId, new Message("--> provide outputstream"));
				MessageOutputStream result = (MessageOutputStream)proceedingJoinPoint.proceed();
				//System.out.println("<-- provide outputstream of pipe ["+pipe.getName()+"]: ["+result+"]");
				ibisDebugger.pipeOutput(pipeLine, pipe, correlationId, new Message(result==null?null:result.toString()));
				return result;
			}
		}
		log.warn("Could not identify outputstream provider ["+proceedingJoinPoint.getTarget().getClass().getName()+"] as pipe or sender");
		return (MessageOutputStream)proceedingJoinPoint.proceed();
	}

	/**
	 * Provides advice for {@link CacheSenderWrapperProcessor#sendMessage(SenderWrapperBase senderWrapperBase, Message message, IPipeLineSession session)}
	 */
	public Object debugSenderGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, SenderWrapperBase senderWrapperBase, Message message, IPipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		String correlationId = session == null ? null : session.getMessageId();
		message = (Message)debugGetInputFrom(session, correlationId, message, senderWrapperBase.getGetInputFromSessionKey(), senderWrapperBase.getGetInputFromFixedValue(), null);
		if (ibisDebugger.stubSender(senderWrapperBase, correlationId)) {
			return null;
		} else {
			Object[] args = proceedingJoinPoint.getArgs();
			args[1] = message;
			return proceedingJoinPoint.proceed(args);
		}
	}

	public Object debugReplyListenerInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, ICorrelatedPullingListener<?> listener, String correlationId, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		correlationId = ibisDebugger.replyListenerInput(listener, pipeLineSession.getMessageId(), correlationId);
		String result = null;
		if (ibisDebugger.stubReplyListener(listener, correlationId)) {
			return null;
		} else {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[1] = correlationId;
				result = (String)proceedingJoinPoint.proceed(args);
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
		return ibisDebugger.parameterResolvedTo(parameter, session.getMessageId(), result);
	}

	/**
	 * provides advice for {@link CacheSenderWrapperProcessor#sendMessage(String correlationID, Message message, IPipeLineSession session)}
	 */
	private Object debugGetInputFrom(IPipeLineSession pipeLineSession, String correlationId, Message input, String inputFromSessionKey, String inputFromFixedValue, String emptyInputReplacement) {
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
