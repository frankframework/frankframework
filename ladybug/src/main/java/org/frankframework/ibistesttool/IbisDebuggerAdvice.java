/*
   Copyright 2018-2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.ibistesttool;

import java.io.Writer;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.IBlockEnabledSender;
import org.frankframework.core.ICorrelatedPullingListener;
import org.frankframework.core.INamedObject;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.IValidator;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.RequestReplyExecutor;
import org.frankframework.core.SenderResult;
import org.frankframework.documentbuilder.xml.XmlTee;
import org.frankframework.management.bus.DebuggerStatusChangedEvent;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.processors.CacheSenderWrapperProcessor;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.InputOutputPipeProcessor;
import org.frankframework.processors.LimitingParallelExecutionPipeProcessor;
import org.frankframework.scheduler.job.SendMessageJob.SendMessageJobSender;
import org.frankframework.senders.ParallelSenderExecutor;
import org.frankframework.senders.SenderWrapperBase;
import org.frankframework.stream.Message;
import org.frankframework.threading.ThreadConnector;
import org.frankframework.threading.ThreadLifeCycleEventListener;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamCaptureUtils;
import org.frankframework.xml.IXmlDebugger;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.XmlWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.xml.sax.ContentHandler;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * @author  Jaco de Groot (jaco@dynasol.nl)
 * @author Niels Meijer
 */
@Log4j2
public class IbisDebuggerAdvice implements InitializingBean, ThreadLifeCycleEventListener<ThreadDebugInfo>, ApplicationListener<DebuggerStatusChangedEvent>, IXmlDebugger {

	private static final String REQUESTER = "TestTool";

	private @Setter IbisDebugger ibisDebugger;
	private @Setter IbisManager ibisManager;

	// Contract for testtool state:
	// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
	// - to get notified of changes, components should listen to DebuggerStatusChangedEvents
	// IbisDebuggerAdvice stores state in appconstants testtool.enabled for use by GUI
	private boolean enabled = true;

	private final AtomicInteger threadCounter = new AtomicInteger();


	@Override
	public void afterPropertiesSet() {
		if(ibisDebugger == null) {
			return;
		}
		// As ibisDebugger lives in the WebApplicationContext it cannot get wired with ibisManager by Spring
		ibisDebugger.setIbisManager(ibisManager);
	}

	/**
	 * Provides advice for {@link CorePipeLineProcessor#processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession session, String firstPipe)}
	 */
	public PipeLineResult debugPipeLineInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, String messageId, Message message, PipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (PipeLineResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(session);
		message = ibisDebugger.pipelineInput(pipeLine, correlationId, message);
		TreeSet<String> keys = new TreeSet<>(session.keySet());
		Iterator<String> iterator = keys.iterator();
		while (iterator.hasNext()) {
			String sessionKey = iterator.next();
			Object sessionValue = session.get(sessionKey);
			sessionValue = ibisDebugger.pipelineSessionKey(correlationId, sessionKey, sessionValue);
			session.put(sessionKey, sessionValue);
		}
		PipeLineResult pipeLineResult = null;
		try {
			PipeLineSession pipeLineSessionDebugger = PipeLineSessionDebugger.newInstance(session, ibisDebugger);
			Object[] args = proceedingJoinPoint.getArgs();
			args[3] = pipeLineSessionDebugger;
			pipeLineResult = (PipeLineResult)proceedingJoinPoint.proceed(args);
		} catch(Throwable throwable) {
			throw ibisDebugger.pipelineAbort(pipeLine, correlationId, throwable);
		}
		ibisDebugger.showOutputValue(correlationId, "exitState", pipeLineResult.getState().name());
		if (pipeLineResult.getExitCode()!=0) {
			ibisDebugger.showOutputValue(correlationId, "exitCode", Integer.toString(pipeLineResult.getExitCode()));
		}

		if (!pipeLineResult.isSuccessful()) {
			ibisDebugger.pipelineAbort(pipeLine, correlationId, pipeLineResult.getResult());
		} else {
			Message result = ibisDebugger.pipelineOutput(pipeLine, correlationId, pipeLineResult.getResult());
			if(Message.isNull(result)) {
				log.error("debugger returned NULL, pipeline result was: [{}]", pipeLineResult.getResult());
			}
			pipeLineResult.setResult(result);
		}

		return pipeLineResult;
	}

	/**
	 * Provides advice for {@link InputOutputPipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session)}
	 */
	public PipeRunResult debugPipeInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(session);
		message = ibisDebugger.pipeInput(pipeLine, pipe, correlationId, message);
		PipeRunResult pipeRunResult = null;
		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = message;
			pipeRunResult = (PipeRunResult)proceedingJoinPoint.proceed(args); // in case of 'preserveInput', this result is already replaced with the preserved input
		} catch(Throwable throwable) {
			throw ibisDebugger.pipeAbort(pipeLine, pipe, correlationId, throwable);
		}
		if (pipe.isPreserveInput()) {
			// signal in the debugger that the result of the pipe has been replaced with the original input
			pipeRunResult.setResult(ibisDebugger.preserveInput(correlationId, pipeRunResult.getResult()));
		}
		pipeRunResult.setResult(ibisDebugger.pipeOutput(pipeLine, pipe, correlationId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	/**
	 * Provides advice for {@link LimitingParallelExecutionPipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session)}
	 * LimitingParallelExecutionPipeProcessor is just after InputOutputPipeProcessor, so it sees the effect of the replacements made by the latter.
	 */
	public PipeRunResult debugPipeGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult) proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(session);
		message = debugGetInputFrom(session, correlationId, message,
				pipe.getGetInputFromSessionKey(),
				pipe.getGetInputFromFixedValue(),
				pipe.getEmptyInputReplacement());
		Object[] args = proceedingJoinPoint.getArgs();
		args[2] = message;
		return (PipeRunResult) proceedingJoinPoint.proceed(args); // the PipeRunResult contains the original result, before replacing via preserveInput
	}

	public PipeRunResult debugValidatorInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IValidator validator, Message message, PipeLineSession session, String messageRoot) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(session);
		message = ibisDebugger.pipeInput(pipeLine, validator, correlationId, message);
		PipeRunResult pipeRunResult = null;

		if(StringUtils.isNotEmpty(messageRoot)) {
			ibisDebugger.showInputValue(correlationId, "MessageRoot to be asserted", messageRoot);
		}

		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = message;
			pipeRunResult = (PipeRunResult)proceedingJoinPoint.proceed(args); // in case of 'preserveInput', this result is already replaced with the preserved input
		} catch(Throwable throwable) {
			throw ibisDebugger.pipeAbort(pipeLine, validator, correlationId, throwable);
		}
		pipeRunResult.setResult(ibisDebugger.pipeOutput(pipeLine, validator, correlationId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	private SenderResult debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession session, int messageParamIndex) throws Throwable {
		if (!isEnabled()) {
			return (SenderResult) proceedingJoinPoint.proceed();
		}
		ISender sender = (ISender) proceedingJoinPoint.getTarget();

		String correlationId = getCorrelationId(session);
		message = ibisDebugger.senderInput(sender, correlationId, message);

		SenderResult senderResult;
		// For SenderWrapperBase continue even when it needs to be stubbed
		// because for SenderWrapperBase this will be checked when it calls
		// sendMessage on his senderWrapperProcessor, hence
		// debugSenderGetInputFrom will be called.
		if (!ibisDebugger.stubSender(sender, correlationId) || sender instanceof SenderWrapperBase) {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[messageParamIndex] = message;
				Object result = proceedingJoinPoint.proceed(args);
				if(result instanceof Message message1) {
					senderResult = new SenderResult(message1);
				} else {
					senderResult = (SenderResult) result;
				}
			} catch(Throwable throwable) {
				throw ibisDebugger.senderAbort(sender, correlationId, throwable);
			}

			if (sender instanceof SenderWrapperBase base && base.isPreserveInput()) {
				// signal in the debugger that the result of the sender has been replaced with the original input
				senderResult.setResult(ibisDebugger.preserveInput(correlationId, senderResult.getResult()));
			}
		} else {
			// Resolve parameters so they will be added to the report like when the sender was not stubbed and would
			// resolve parameters itself
			if (sender instanceof IWithParameters parameters) {
				ParameterList parameterList = parameters.getParameterList();
				if (parameterList!=null) {
					parameterList.getValues(message, session);
				}
			}

			senderResult = new SenderResult(true, Message.nullMessage(), null, "stub");
		}

		ibisDebugger.showOutputValue(correlationId, "success", senderResult.isSuccess());
		if (senderResult.getForwardName()!=null) {
			ibisDebugger.showOutputValue(correlationId, "forwardName", senderResult.getForwardName());
		}
		if (StringUtils.isNotEmpty(senderResult.getErrorMessage())) {
			ibisDebugger.showOutputValue(correlationId, "errorMessage", senderResult.getErrorMessage());
		}

		Message capturedResult = ibisDebugger.senderOutput(sender, correlationId, senderResult.getResult());
		senderResult.setResult(capturedResult);
		session.scheduleCloseOnSessionExit(capturedResult, REQUESTER); //The Ladybug may change the result (when stubbed).
		return senderResult;
	}

	/**
	 * Provides advice for {@link ISender#sendMessageOrThrow(Message message, PipeLineSession session)}
	 */
	public Message debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession session) throws Throwable {
		if (!isEnabled() || proceedingJoinPoint.getTarget() instanceof SendMessageJobSender) {
			return (Message) proceedingJoinPoint.proceed();
		}

		SenderResult result = debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 0);
		return result.getResult();
	}

	/**
	 * Provides advice for {@link ISender#sendMessage(Message message, PipeLineSession session)}
	 */
	public SenderResult debugSenderWithForwardInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession session) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 0);
	}

	/**
	 * Provides advice for {@link IBlockEnabledSender#sendMessage(Object blockHandle, Message message, PipeLineSession session)}
	 */
	public SenderResult debugBlockEnabledSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Object blockHandle, Message message, PipeLineSession session) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 1);
	}

	@Override
	public ContentHandler inspectXml(PipeLineSession session, String label, ContentHandler contentHandler) {
		if (!isEnabled()) {
			return contentHandler;
		}
		String correlationId = getCorrelationId(session);
		WriterPlaceHolder writerPlaceHolder = ibisDebugger.showInputValue(correlationId, label, new WriterPlaceHolder());
		if (writerPlaceHolder!=null && writerPlaceHolder.getWriter()!=null) {
			Writer writer = writerPlaceHolder.getWriter();
			session.scheduleCloseOnSessionExit(writer, REQUESTER);
			XmlWriter xmlWriter = new XmlWriter(StreamCaptureUtils.limitSize(writer, writerPlaceHolder.getSizeLimit()), true);
			contentHandler = new XmlTee(contentHandler, new PrettyPrintFilter(xmlWriter));
		}
		return contentHandler;
	}


	/**
	 * Provides advice for {@link CacheSenderWrapperProcessor#sendMessage(SenderWrapperBase senderWrapperBase, Message message, PipeLineSession session)}
	 */
	public SenderResult debugSenderGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, SenderWrapperBase senderWrapperBase, Message message, PipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (SenderResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(session);
		message = debugGetInputFrom(session, correlationId, message,
				senderWrapperBase.getGetInputFromSessionKey(),
				senderWrapperBase.getGetInputFromFixedValue(),
				null);
		if (ibisDebugger.stubSender(senderWrapperBase, correlationId)) {
			return null;
		}
		Object[] args = proceedingJoinPoint.getArgs();
		args[1] = message;
		return (SenderResult)proceedingJoinPoint.proceed(args); // this message contains the original result, before replacing via preserveInput
	}

	public <M> M debugReplyListenerInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, ICorrelatedPullingListener<M> listener, String correlationId, PipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (M)proceedingJoinPoint.proceed();
		}
		correlationId = ibisDebugger.replyListenerInput(listener, pipeLineSession.getMessageId(), correlationId);
		M result;
		if (ibisDebugger.stubReplyListener(listener, correlationId)) {
			return null;
		}
		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[1] = correlationId;
			result = (M)proceedingJoinPoint.proceed(args);
		} catch(Throwable throwable) {
			throw ibisDebugger.replyListenerAbort(listener, pipeLineSession.getMessageId(), throwable);
		}
		return ibisDebugger.replyListenerOutput(listener, pipeLineSession.getMessageId(), result);
	}

	public Object debugThreadCreateStartEndAbort(ProceedingJoinPoint proceedingJoinPoint, Runnable runnable) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		if (runnable instanceof ParallelSenderExecutor senderExecutor) {
			ParallelSenderExecutorWrapper executor = new ParallelSenderExecutorWrapper(senderExecutor, this);
			Object[] args = proceedingJoinPoint.getArgs();
			args[0] = executor;
			return proceedingJoinPoint.proceed(args);
		}
		return proceedingJoinPoint.proceed();
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
			String nameClause=threadInfo.owner instanceof INamedObject ino?" name ["+ino.getName()+"]":"";
			log.debug("announceChildThread thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.getId(), Thread.currentThread().getName(), threadInfo.owner.getClass()
					.getSimpleName(), nameClause, threadInfo.threadId, threadInfo.correlationId);
		}
		ibisDebugger.createThread(threadInfo.owner, threadInfo.threadId, threadInfo.correlationId);
		return threadInfo;
	}

	@Override
	public void cancelChildThread(ThreadDebugInfo threadInfo) {
		if (!isEnabled()) {
			return;
		}
		if (log.isDebugEnabled()) {
			String nameClause=threadInfo.owner instanceof INamedObject ino?" name ["+ino.getName()+"]":"";
			log.debug("cancelChildThread thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.getId(), Thread.currentThread().getName(), threadInfo.owner.getClass()
					.getSimpleName(), nameClause, threadInfo.threadId, threadInfo.correlationId);
		}
		ibisDebugger.cancelThread(threadInfo.owner, threadInfo.threadId, threadInfo.correlationId);
	}

	@Override
	public <R> R threadCreated(ThreadDebugInfo ref, R request) {
		if (!isEnabled()) {
			return request;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject ino?" name ["+ino.getName()+"]":"";
			log.debug("threadCreated thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.getId(), Thread.currentThread().getName(), ref.owner.getClass().getSimpleName(), nameClause, ref.threadId, ref.correlationId);
		}
		return (R)ibisDebugger.startThread(ref.owner, ref.threadId, ref.correlationId, request);
	}

	@Override
	public <R> R threadEnded(ThreadDebugInfo ref, R result) {
		if (!isEnabled()) {
			return result;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject ino?" name ["+ino.getName()+"]":"";
			log.debug("threadEnded thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.getId(), Thread.currentThread().getName(), ref.owner.getClass().getSimpleName(), nameClause, ref.threadId, ref.correlationId);
		}
		return (R)ibisDebugger.endThread(ref.owner, ref.correlationId, result);
	}

	@Override
	public Throwable threadAborted(ThreadDebugInfo ref, Throwable t) {
		if (!isEnabled()) {
			return t;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject ino?" name ["+ino.getName()+"]":"";
			log.debug("threadAborted thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.getId(), Thread.currentThread().getName(), ref.owner.getClass().getSimpleName(), nameClause, ref.threadId, ref.correlationId);
		}
		return ibisDebugger.abortThread(ref.owner, ref.correlationId, t);
	}

	/**
	 * Provides advice for {@link Parameter#getValue(ParameterValueList alreadyResolvedParameters, Message message, PipeLineSession session, boolean namespaceAware)}
	 */
	public Object debugParameterResolvedTo(ProceedingJoinPoint proceedingJoinPoint, ParameterValueList alreadyResolvedParameters, Message message, PipeLineSession session, boolean namespaceAware) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		Object result = proceedingJoinPoint.proceed();
		IParameter parameter = (IParameter)proceedingJoinPoint.getTarget();
		return ibisDebugger.parameterResolvedTo(parameter, getCorrelationId(session), result); // session is null in afterMessageProcessed()
	}

	private Message debugGetInputFrom(PipeLineSession pipeLineSession, String correlationId, Message input, String inputFromSessionKey, String inputFromFixedValue, String emptyInputReplacement) {
		if (StringUtils.isNotEmpty(inputFromSessionKey)) {
			input = Message.asMessage(pipeLineSession.get(inputFromSessionKey));
			input = (Message)ibisDebugger.getInputFromSessionKey(correlationId, inputFromSessionKey, input);
		}
		if (StringUtils.isNotEmpty(inputFromFixedValue)) {
			input =  Message.asMessage(ibisDebugger.getInputFromFixedValue(correlationId, inputFromFixedValue));
		}
		if (StringUtils.isNotEmpty(emptyInputReplacement) && Message.isEmpty(input)) {
			input = Message.asMessage(ibisDebugger.getEmptyInputReplacement(correlationId, emptyInputReplacement));
		}

		pipeLineSession.scheduleCloseOnSessionExit(input, REQUESTER); //If we're pointcutting and manipulating the Message, we need to schedule it to be closed...
		return input;
	}

public static class ParallelSenderExecutorWrapper implements Runnable {
		private RequestReplyExecutor requestReplyExecutor;
		private ThreadConnector<ThreadDebugInfo> threadConnector;

		public ParallelSenderExecutorWrapper(ParallelSenderExecutor parallelSenderExecutor, ThreadLifeCycleEventListener<ThreadDebugInfo> threadLifeCycleEventListener) {
			this.requestReplyExecutor=parallelSenderExecutor;
			this.threadConnector = new ThreadConnector<>(parallelSenderExecutor, "Debugger", threadLifeCycleEventListener, null, parallelSenderExecutor.getSession());
		}

		@Override
		public void run() {
			threadConnector.startThread(requestReplyExecutor.getRequest());
			try {
				requestReplyExecutor.run();
			} finally {
				Throwable throwable = requestReplyExecutor.getThrowable();
				if (throwable == null) {
					SenderResult reply = requestReplyExecutor.getReply();
					reply = threadConnector.endThread(reply);
					requestReplyExecutor.setReply(reply);
				} else {
					throwable = threadConnector.abortThread(throwable);
					requestReplyExecutor.setThrowable(throwable);
				}
			}
		}
	}

	public String getCorrelationId(PipeLineSession session) {
		return session==null?null:session.getCorrelationId();
	}

	private void setEnabled(boolean enable) {
		enabled = enable;
		AppConstants.getInstance().put("testtool.enabled", String.valueOf(enable));
	}
	private boolean isEnabled() {
		return ibisDebugger != null && enabled;
	}

	@Override
	public void onApplicationEvent(DebuggerStatusChangedEvent event) {
		log.debug("received DebuggerStatusChangedEvent [{}]", event);
		setEnabled(event.isEnabled());
	}
}
