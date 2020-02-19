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
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.ApplicationListener;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.IsolatedServiceExecutor;
import nl.nn.adapterframework.senders.ParallelSenderExecutor;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.IStreamingSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.api.DebuggerStatusChangedEvent;

/**
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */
public class IbisDebuggerAdvice implements ThreadLifeCycleEventListener<Object>, ApplicationListener<DebuggerStatusChangedEvent> {
	protected Logger log = LogUtil.getLogger(this);

	private IbisDebugger ibisDebugger;
	private static boolean enabled=true;
	
	private AtomicInteger threadCounter = new AtomicInteger(0);
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

	public Object debugPipeLineInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, String correlationId, String message, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		message = (String)ibisDebugger.pipeLineInput(pipeLine, correlationId, message);
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

	public Object debugPipeInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, String messageId, Object message, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		Object preservedObject = message;
		message = ibisDebugger.pipeInput(pipeLine, pipe, messageId, message);
		PipeRunResult pipeRunResult = null;
		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[3] = message;
			pipeRunResult = (PipeRunResult)proceedingJoinPoint.proceed(args);
		} catch(Throwable throwable) {
			throw ibisDebugger.pipeAbort(pipeLine, pipe, messageId, throwable);
		}
		if (pipe instanceof IExtendedPipe) {
			IExtendedPipe pe = (IExtendedPipe)pipe;
			if (pe.isPreserveInput()) {
				pipeRunResult.setResult(ibisDebugger.preserveInput(messageId, preservedObject));
			}
		}
		pipeRunResult.setResult(ibisDebugger.pipeOutput(pipeLine, pipe, messageId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	public Object debugPipeGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, String messageId, Object message, IPipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		if (pipe instanceof IExtendedPipe) {
			IExtendedPipe pe = (IExtendedPipe)pipe;
			message = debugGetInputFrom(pipeLineSession, messageId, message,
					pe.getGetInputFromSessionKey(),
					pe.getGetInputFromFixedValue(),
					pe.getEmptyInputReplacement());
		}
		Object[] args = proceedingJoinPoint.getArgs();
		args[3] = message;
		return proceedingJoinPoint.proceed(args);
	}

	/**
	 * Provides advice for {@link ISender#sendMessage(String correlationId, String message)}
	 */
//	@Pointcut("execution( * nl.nn.adapterframework.core.ISender.sendMessage(String, String)) "+
//				"and args(correlationId, message)" )
	public Object debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, String correlationId, String message) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		ISender sender = (ISender)proceedingJoinPoint.getTarget();
		if (log.isDebugEnabled()) log.debug("debugSenderInputOutputAbort thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] correlationId ["+correlationId+"]");
		if (!sender.isSynchronous() && sender instanceof JmsSender) {
			// Ignore JmsSenders within JmsListeners (calling JmsSender without
			// ParameterResolutionContext) within Receivers.
			return proceedingJoinPoint.proceed();
		} else {
			Object result = debugSenderInputAbort(proceedingJoinPoint, sender, correlationId, message);
			return ibisDebugger.senderOutput(sender, correlationId, result);
		}
	}

	/**
	 * Provides advice for {@link ISenderWithParameters#sendMessage(String correlationId, String message, ParameterResolutionContext prc)}
	 */
//	@Pointcut("execution( * nl.nn.adapterframework.core.ISenderWithParameters.sendMessage(String, String, nl.nn.adapterframework.parameters.ParameterResolutionContext)) " +
//				"and args(correlationId, message, parameterResolutionContext)" )
	public Object debugSenderWithParametersInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, String correlationId, String message, ParameterResolutionContext prc) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		ISenderWithParameters sender = (ISenderWithParameters)proceedingJoinPoint.getTarget();
		Object preservedObject = message;
		Object result = debugSenderInputAbort(proceedingJoinPoint, sender, correlationId, message);
		if (ibisDebugger.stubSender(sender, correlationId)) {
			// Resolve parameters so they will be added to the report like when the sender was not stubbed and would
			// resolve parameters itself
			prc.getValues(sender.getParameterList());
		}
		if (sender instanceof SenderWrapperBase) {
			SenderWrapperBase senderWrapperBase = (SenderWrapperBase)sender;
			if (senderWrapperBase.isPreserveInput()) {
				result = (String)ibisDebugger.preserveInput(correlationId, preservedObject);
			}
		}
		return ibisDebugger.senderOutput(sender, correlationId, result);
	}

	/**
	 * Provides advice for {@link IStreamingSender#sendMessage(String correlationID, Message message, ParameterResolutionContext prc, IOutputStreamingSupport next)}
	 */
//	@Pointcut("execution( * nl.nn.adapterframework.stream.IStreamingSender.sendMessage(String, nl.nn.adapterframework.stream.Message, nl.nn.adapterframework.parameters.ParameterResolutionContext, nl.nn.adapterframework.stream.IOutputStreamingSupport)) " +
//				"and args(correlationId, message, prc, next)" )
	public Object debugStreamingSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, String correlationId, Message message, ParameterResolutionContext prc, IOutputStreamingSupport next) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		IStreamingSender sender = (IStreamingSender)proceedingJoinPoint.getTarget();
		String moddedMessage = ibisDebugger.senderInput(sender, correlationId, message.toString()); // TODO: enable Message to be adjusted. Output is now ignored

		PipeRunResult result = null;
		if (!ibisDebugger.stubSender(sender, correlationId)) {
			try {
				result = (PipeRunResult)proceedingJoinPoint.proceed();
			} catch(Throwable throwable) {
				throw ibisDebugger.senderAbort(sender, correlationId, throwable);
			}
		} else {
			// Resolve parameters so they will be added to the report like when the sender was not stubbed and would
			// resolve parameters itself
			prc.getValues(sender.getParameterList());
		}
		ibisDebugger.senderOutput(sender, correlationId, result==null?null:result.getResult());
		return result;
	}
	 
	/**
	 * Provides advice for {@link IOutputStreamingSupport#provideOutputStream(String correlationId, IPipeLineSession session, IOutputStreamingSupport nextProvider)}
	 */
//	@Pointcut("execution( * nl.nn.adapterframework.stream.IOutputStreamingSupport.provideOutputStream(String, nl.nn.adapterframework.core.IPipeLineSession, nl.nn.adapterframework.stream.IOutputStreamingSupport)) " +
//				"and args(correlationId, session, nextProvider)")
	public Object debugProvideOutputStream(ProceedingJoinPoint proceedingJoinPoint, String correlationId, IPipeLineSession session, IOutputStreamingSupport nextProvider) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		if (log.isDebugEnabled()) log.debug("debugProvideOutputStream thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] correlationId ["+correlationId+"]");
		// TODO: provide proper debug entry in Debugger interface.
		if (proceedingJoinPoint.getTarget() instanceof ISender) {
			ISender sender = (ISender)proceedingJoinPoint.getTarget();
			ibisDebugger.senderInput(sender, correlationId, "--> provide outputstream");
			//System.out.println("--> provide outputstream of sender ["+sender.getName()+"]");
			Object result = proceedingJoinPoint.proceed();
			//System.out.println("<-- provide outputstream of sender ["+sender.getName()+"]: ["+result+"]");
			ibisDebugger.senderOutput(sender, correlationId, result==null?null:result.toString());
			return result;
		} else {
			if (proceedingJoinPoint.getTarget() instanceof IPipe) {
				IPipe pipe = (IPipe)proceedingJoinPoint.getTarget();
				//System.out.println("--> provide outputstream of pipe ["+pipe.getName()+"]");
				PipeLine pipeLine = pipe instanceof AbstractPipe ? ((AbstractPipe)pipe).getPipeLine() : new PipeLine();
				ibisDebugger.pipeInput(pipeLine, pipe, correlationId, "--> provide outputstream");
				Object result = proceedingJoinPoint.proceed();
				//System.out.println("<-- provide outputstream of pipe ["+pipe.getName()+"]: ["+result+"]");
				ibisDebugger.pipeOutput(pipeLine, pipe, correlationId, result);
				return result;
			}
		}
		log.warn("Could not identify outputstream provider ["+proceedingJoinPoint.getTarget().getClass().getName()+"] as pipe or sender");
		return proceedingJoinPoint.proceed();
	}
	 
	public Object debugSenderGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, SenderWrapperBase senderWrapperBase, String correlationId, String message, ParameterResolutionContext parameterResolutionContext) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		message = (String)debugGetInputFrom(parameterResolutionContext.getSession(), correlationId, message, senderWrapperBase.getGetInputFromSessionKey(), senderWrapperBase.getGetInputFromFixedValue(), null);
		if (ibisDebugger.stubSender(senderWrapperBase, correlationId)) {
			return null;
		} else {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = message;
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
	
	public Object debugParameterResolvedTo(ProceedingJoinPoint proceedingJoinPoint, ParameterValueList alreadyResolvedParameters, ParameterResolutionContext parameterResolutionContext) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		Object result = proceedingJoinPoint.proceed();
		Parameter parameter = (Parameter)proceedingJoinPoint.getTarget();
		return ibisDebugger.parameterResolvedTo(parameter, parameterResolutionContext.getSession().getMessageId(), result);
	}

	private Object debugGetInputFrom(IPipeLineSession pipeLineSession, String correlationId, Object input, String inputFromSessionKey, String inputFromFixedValue, String emptyInputReplacement) {
		if (StringUtils.isNotEmpty(inputFromSessionKey)) {
			input = pipeLineSession.get(inputFromSessionKey);
			input = ibisDebugger.getInputFromSessionKey(correlationId, inputFromSessionKey, input);
		}
		if (StringUtils.isNotEmpty(inputFromFixedValue)) {
			input = ibisDebugger.getInputFromFixedValue(correlationId, inputFromFixedValue);
		}
		if (input == null || StringUtils.isEmpty(input.toString())) {
			if (StringUtils.isNotEmpty(emptyInputReplacement)) {
				input = ibisDebugger.getEmptyInputReplacement(correlationId, emptyInputReplacement);
			}
		}
		return input;
	}
	

	private Object debugSenderInputAbort(ProceedingJoinPoint proceedingJoinPoint, ISender sender, String correlationId, String message) throws Throwable {
		message = ibisDebugger.senderInput(sender, correlationId, message);
		String result = null;
		// For SenderWrapperBase continue even when it needs to be stubbed
		// because for SenderWrapperBase this will be checked when it calls
		// sendMessage on his senderWrapperProcessor, hence
		// debugSenderGetInputFrom will be called.
		if (!ibisDebugger.stubSender(sender, correlationId) || sender instanceof SenderWrapperBase) {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[1] = message;
				result = (String)proceedingJoinPoint.proceed(args);
			} catch(Throwable throwable) {
				throw ibisDebugger.senderAbort(sender, correlationId, throwable);
			}
		}
		return result;
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
					Object reply = requestReplyExecutor.getReply();
					reply = threadConnector.endThread(reply);
					requestReplyExecutor.setReply(reply);
				} else {
					throwable = threadConnector.abortThread(throwable);
					requestReplyExecutor.setThrowable(throwable);
				}
			}
		}

	}

	public static void setEnabled(boolean enable) {
		enabled = enable;
	}
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void onApplicationEvent(DebuggerStatusChangedEvent event) {
		setEnabled(event.isEnabled());
	}

}
