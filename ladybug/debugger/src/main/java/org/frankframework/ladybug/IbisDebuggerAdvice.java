/*
   Copyright 2018-2020 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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
package org.frankframework.ladybug;

import java.io.IOException;
import java.io.Writer;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.xml.sax.ContentHandler;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.IbisManager;
import org.frankframework.core.AbstractRequestReplyExecutor;
import org.frankframework.core.HasName;
import org.frankframework.core.IBlockEnabledSender;
import org.frankframework.core.ICorrelatedPullingListener;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.IValidator;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
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
import org.frankframework.senders.AbstractSenderWrapper;
import org.frankframework.senders.ParallelSenderExecutor;
import org.frankframework.stream.Message;
import org.frankframework.threading.ThreadConnector;
import org.frankframework.threading.ThreadLifeCycleEventListener;
import org.frankframework.util.AppConstants;
import org.frankframework.xml.IXmlDebugger;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.XmlWriter;

/**
 * @author Jaco de Groot
 * @author Niels Meijer
 */
@SuppressWarnings("unchecked")
@Log4j2
public class IbisDebuggerAdvice implements InitializingBean, ThreadLifeCycleEventListener<ThreadDebugInfo>, ApplicationListener<DebuggerStatusChangedEvent>, IXmlDebugger {

	private @Setter @Autowired LadybugReportGenerator reportGenerator;
	private @Setter @Autowired LadybugDebugger debugger;
	protected @Setter @Autowired @Getter IbisManager ibisManager;

	// Contract for testtool state:
	// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
	// - to get notified of changes, components should listen to DebuggerStatusChangedEvents
	// IbisDebuggerAdvice stores state in AppConstants.properties testtool.enabled for use by GUI
	private boolean enabled = true;

	private final AtomicInteger threadCounter = new AtomicInteger();

	@Override
	public void afterPropertiesSet() {
		// As reportGenerator lives in the WebApplicationContext it cannot get wired with ibisManager by Spring
		if (reportGenerator == null) {
			log.info("no Ladybug found on classpath, skipping reportGenerator.");
		} else {
			if(debugger == null) {
				throw new FatalBeanException("missing bean [LadybugDebugger]");
			}
			debugger.setIbisManager(ibisManager);
			log.info("using Ladybug debugger [{}] and reportGenerator [{}]", debugger, reportGenerator);
		}
	}

	/**
	 * Provides advice for {@link CorePipeLineProcessor#processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession session, String firstPipe)}
	 */
	public PipeLineResult debugPipeLineInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (PipeLineResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(pipeLineSession);
		reportGenerator.pipelineInput(pipeLine, correlationId, message);
		TreeSet<String> keys = new TreeSet<>(pipeLineSession.keySet());
		for (String sessionKey : keys) {
			Object sessionValue = reportGenerator.sessionInputPoint(correlationId, sessionKey, pipeLineSession.get(sessionKey));
			pipeLineSession.put(sessionKey, sessionValue);
		}
		PipeLineResult pipeLineResult;
		try {
			PipeLineSession pipeLineSessionDebugger = PipeLineSessionDebugger.newInstance(pipeLineSession, reportGenerator);
			Object[] args = proceedingJoinPoint.getArgs();
			args[3] = pipeLineSessionDebugger;
			pipeLineResult = (PipeLineResult)proceedingJoinPoint.proceed(args);
		} catch (Throwable throwable) {
			throw reportGenerator.pipelineAbort(pipeLine, correlationId, throwable);
		}
		reportGenerator.showOutputValue(correlationId, "exitState", pipeLineResult.getState().name());
		if (pipeLineResult.getExitCode() != null) {
			reportGenerator.showOutputValue(correlationId, "exitCode", Integer.toString(pipeLineResult.getExitCode()));
		}

		if (!pipeLineResult.isSuccessful()) {
			reportGenerator.pipelineAbort(pipeLine, correlationId, pipeLineResult.getResult());
		} else {
			Message result = reportGenerator.pipelineOutput(pipeLine, correlationId, pipeLineResult.getResult());
			if (Message.isNull(result) && !Message.isNull(pipeLineResult.getResult())) {
				log.info("debugger returned NULL, pipeline result was: [{}]", pipeLineResult.getResult());
			}
			pipeLineResult.setResult(result);
		}

		return pipeLineResult;
	}

	/**
	 * Provides advice for {@link InputOutputPipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session)}
	 */
	public PipeRunResult debugPipeInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(pipeLineSession);
		Message result = reportGenerator.pipeInput(pipeLine, pipe, correlationId, message);
		PipeRunResult pipeRunResult;
		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = result;
			pipeRunResult = (PipeRunResult)proceedingJoinPoint.proceed(args); // in case of 'preserveInput', this result is already replaced with the preserved input
		} catch(Throwable throwable) {
			throw reportGenerator.pipeAbort(pipeLine, pipe, correlationId, throwable);
		}
		if (pipe.isPreserveInput()) {
			// signal in the debugger that the result of the pipe has been replaced with the original input
			pipeRunResult.setResult(reportGenerator.preserveInput(correlationId, pipeRunResult.getResult()));
		}
		pipeRunResult.setResult(reportGenerator.pipeOutput(pipeLine, pipe, correlationId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	/**
	 * Provides advice for {@link LimitingParallelExecutionPipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session)}
	 * LimitingParallelExecutionPipeProcessor is just after InputOutputPipeProcessor, so it sees the effect of the replacements made by the latter.
	 */
	public PipeRunResult debugPipeGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult) proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(pipeLineSession);
		Message result = debugGetInputFrom(
				pipeLineSession, correlationId, message,
				pipe.getGetInputFromSessionKey(),
				pipe.getGetInputFromFixedValue(),
				pipe.getDefaultValue());
		Object[] args = proceedingJoinPoint.getArgs();
		args[2] = result;
		return (PipeRunResult) proceedingJoinPoint.proceed(args); // the PipeRunResult contains the original result, before replacing via preserveInput
	}

	public PipeRunResult debugValidatorInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IValidator validator, Message message, PipeLineSession pipeLineSession, String messageRoot) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(pipeLineSession);
		Message result = reportGenerator.pipeInput(pipeLine, validator, correlationId, message);
		PipeRunResult pipeRunResult;

		if(StringUtils.isNotEmpty(messageRoot)) {
			reportGenerator.showInfoValue(correlationId, "MessageRoot to be asserted", messageRoot);
		}

		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = result;
			pipeRunResult = (PipeRunResult)proceedingJoinPoint.proceed(args); // in case of 'preserveInput', this result is already replaced with the preserved input
		} catch (Throwable throwable) {
			throw reportGenerator.pipeAbort(pipeLine, validator, correlationId, throwable);
		}
		pipeRunResult.setResult(reportGenerator.pipeOutput(pipeLine, validator, correlationId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	private SenderResult debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession pipeLineSession, int messageParamIndex) throws Throwable {
		if (!isEnabled()) {
			return (SenderResult) proceedingJoinPoint.proceed();
		}
		ISender sender = (ISender) proceedingJoinPoint.getTarget();

		String correlationId = getCorrelationId(pipeLineSession);
		Message result = reportGenerator.senderInput(sender, correlationId, message);

		SenderResult senderResult;
		// For SenderWrapperBase continue even when it needs to be stubbed
		// because for SenderWrapperBase this will be checked when it calls
		// sendMessage on his senderWrapperProcessor, hence
		// debugSenderGetInputFrom will be called.
		if (!debugger.stubSender(sender, correlationId) || sender instanceof AbstractSenderWrapper) {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[messageParamIndex] = result;
				Object resultObject = proceedingJoinPoint.proceed(args);
				if(resultObject instanceof Message message1) {
					senderResult = new SenderResult(message1);
				} else {
					senderResult = (SenderResult) resultObject;
				}
			} catch(Throwable throwable) {
				throw reportGenerator.senderAbort(sender, correlationId, throwable);
			}

			if (sender instanceof AbstractSenderWrapper base && base.isPreserveInput()) {
				// signal in the debugger that the result of the sender has been replaced with the original input
				senderResult.setResult(reportGenerator.preserveInput(correlationId, senderResult.getResult()));
			}
		} else {
			// Resolve parameters so they will be added to the report like when the sender was not stubbed and would
			// resolve parameters itself
			if (sender instanceof IWithParameters parameters) {
				ParameterList parameterList = parameters.getParameterList();
				parameterList.getValues(result, pipeLineSession);
			}

			senderResult = new SenderResult(true, Message.nullMessage(), null, "stub");
		}

		reportGenerator.showOutputValue(correlationId, "success", senderResult.isSuccess());
		if (senderResult.getForwardName()!=null) {
			reportGenerator.showOutputValue(correlationId, "forwardName", senderResult.getForwardName());
		}
		if (StringUtils.isNotEmpty(senderResult.getErrorMessage())) {
			reportGenerator.showOutputValue(correlationId, "errorMessage", senderResult.getErrorMessage());
		}

		Message capturedResult = reportGenerator.senderOutput(sender, correlationId, senderResult.getResult());
		senderResult.setResult(capturedResult);
		return senderResult;
	}

	/**
	 * Provides advice for {@link ISender#sendMessageOrThrow(Message message, PipeLineSession session)}
	 */
	public Message debugSenderSendMessageOrThrow(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled() || proceedingJoinPoint.getTarget() instanceof SendMessageJobSender) {
			return (Message) proceedingJoinPoint.proceed();
		}

		SenderResult result = debugSenderInputOutputAbort(proceedingJoinPoint, message, pipeLineSession, 0);
		return result.getResult();
	}

	/**
	 * Provides advice for {@link ISender#sendMessage(Message message, PipeLineSession session)}
	 */
	public SenderResult debugSenderSendMessage(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession pipeLineSession) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, pipeLineSession, 0);
	}

	/**
	 * Provides advice for {@link IBlockEnabledSender#sendMessage(Object blockHandle, Message message, PipeLineSession session)}
	 */
	public SenderResult debugBlockEnabledSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Object blockHandle, Message message, PipeLineSession pipeLineSession) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, pipeLineSession, 1);
	}

	@Override
	public ContentHandler inspectXml(PipeLineSession session, String label, ContentHandler contentHandler) {
		if (!isEnabled()) {
			return contentHandler;
		}
		String correlationId = getCorrelationId(session);
		WriterPlaceHolder writerPlaceHolder = reportGenerator.showInfoValue(correlationId, label, new WriterPlaceHolder());
		if (writerPlaceHolder!=null && writerPlaceHolder.getWriter()!=null) {
			Writer writer = writerPlaceHolder.getWriter();
			session.scheduleCloseOnSessionExit(writer);
			XmlWriter xmlWriter = new XmlWriter(limitSize(writer, writerPlaceHolder.getSizeLimit()), true);
			return new XmlTee(contentHandler, new PrettyPrintFilter(xmlWriter));
		}
		return contentHandler;
	}

	public static Writer limitSize(Writer writer, int maxSize) {
		return new Writer() {

			private long written;

			@Override
			public void write(char @NonNull [] buffer, int offset, int length) throws IOException {
				if (written <= maxSize) {
					writer.write(buffer, offset, length);
					written += length;
					if (written > maxSize) {
						writer.close();
					}
				}
			}

			@Override
			public void flush() throws IOException {
				writer.flush();
			}

			@Override
			public void close() throws IOException {
				if (written<=maxSize) {
					writer.close();
				}
			}
		};
	}


	/**
	 * Provides advice for {@link CacheSenderWrapperProcessor#sendMessage(AbstractSenderWrapper senderWrapperBase, Message message, PipeLineSession session)}
	 */
	public SenderResult debugSenderGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, AbstractSenderWrapper senderWrapperBase, Message message, PipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (SenderResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(pipeLineSession);
		Message result = debugGetInputFrom(
				pipeLineSession, correlationId, message,
				senderWrapperBase.getGetInputFromSessionKey(),
				senderWrapperBase.getGetInputFromFixedValue(),
				null);
		if (debugger.stubSender(senderWrapperBase, correlationId)) {
			return null;
		}
		Object[] args = proceedingJoinPoint.getArgs();
		args[1] = result;
		return (SenderResult)proceedingJoinPoint.proceed(args); // this message contains the original result, before replacing via preserveInput
	}

	public <M> M debugReplyListenerInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, ICorrelatedPullingListener<M> listener, String correlationId, PipeLineSession pipeLineSession) throws Throwable {
		if (!isEnabled()) {
			return (M)proceedingJoinPoint.proceed();
		}
		String updatedCorrelationId = reportGenerator.replyListenerInput(listener, pipeLineSession.getMessageId(), correlationId);
		if (debugger.stubReplyListener(listener, updatedCorrelationId)) {
			return null;
		}
		M result;
		try {
			Object[] args = proceedingJoinPoint.getArgs();
			args[1] = updatedCorrelationId;
			result = (M)proceedingJoinPoint.proceed(args);
		} catch (Throwable throwable) {
			throw reportGenerator.replyListenerAbort(listener, pipeLineSession.getMessageId(), throwable);
		}
		return reportGenerator.replyListenerOutput(listener, pipeLineSession.getMessageId(), result);
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
			String nameClause=threadInfo.owner instanceof HasName ino?" name ["+ino.getName()+"]":"";
			log.debug("announceChildThread thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.threadId(), Thread.currentThread().getName(), threadInfo.owner.getClass()
					.getSimpleName(), nameClause, threadInfo.threadId, threadInfo.correlationId);
		}
		reportGenerator.createThread(threadInfo.owner, threadInfo.threadId, threadInfo.correlationId);
		return threadInfo;
	}

	@Override
	public void cancelChildThread(ThreadDebugInfo threadInfo) {
		if (!isEnabled()) {
			return;
		}
		if (log.isDebugEnabled()) {
			String nameClause=threadInfo.owner instanceof HasName ino?" name ["+ino.getName()+"]":"";
			log.debug("cancelChildThread thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.threadId(), Thread.currentThread().getName(), threadInfo.owner.getClass()
					.getSimpleName(), nameClause, threadInfo.threadId, threadInfo.correlationId);
		}
		reportGenerator.cancelThread(threadInfo.owner, threadInfo.threadId, threadInfo.correlationId);
	}

	@Override
	public <R> R threadCreated(ThreadDebugInfo ref, R request) {
		if (!isEnabled()) {
			return request;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof HasName ino?" name ["+ino.getName()+"]":"";
			log.debug("threadCreated thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.threadId(), Thread.currentThread().getName(), ref.owner.getClass().getSimpleName(), nameClause, ref.threadId, ref.correlationId);
		}
		return (R)reportGenerator.startThread(ref.owner, ref.threadId, ref.correlationId, request);
	}

	@Override
	public <R> R threadEnded(ThreadDebugInfo ref, R result) {
		if (!isEnabled()) {
			return result;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof HasName ino?" name ["+ino.getName()+"]":"";
			log.debug("threadEnded thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.threadId(), Thread.currentThread().getName(), ref.owner.getClass().getSimpleName(), nameClause, ref.threadId, ref.correlationId);
		}
		return (R)reportGenerator.endThread(ref.owner, ref.correlationId, result);
	}

	@Override
	public Throwable threadAborted(ThreadDebugInfo ref, Throwable t) {
		if (!isEnabled()) {
			return t;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof HasName ino?" name ["+ino.getName()+"]":"";
			log.debug("threadAborted thread id [{}] thread name [{}] owner [{}]{} threadId [{}] correlationId [{}]", Thread.currentThread()
					.threadId(), Thread.currentThread().getName(), ref.owner.getClass().getSimpleName(), nameClause, ref.threadId, ref.correlationId);
		}
		return reportGenerator.abortThread(ref.owner, ref.correlationId, t);
	}

	/**
	 * Provides advice for {@link Parameter#getValue(Message message, PipeLineSession session)}
	 */
	public Object debugParameterResolvedTo(ProceedingJoinPoint proceedingJoinPoint, ParameterValueList alreadyResolvedParameters, Message message, PipeLineSession session, boolean namespaceAware) throws Throwable {
		if (!isEnabled()) {
			return proceedingJoinPoint.proceed();
		}
		Object result = proceedingJoinPoint.proceed();
		IParameter parameter = (IParameter)proceedingJoinPoint.getTarget();
		return reportGenerator.parameterResolvedTo(parameter, getCorrelationId(session), result); // session is null in afterMessageProcessed()
	}

	private Message debugGetInputFrom(PipeLineSession pipeLineSession, String correlationId, Message input, String inputFromSessionKey, String inputFromFixedValue, String defaultValue) {
		Message result;
		if (StringUtils.isNotEmpty(inputFromFixedValue)) {
			result =  Message.asMessage(reportGenerator.getInputFromFixedValue(correlationId, inputFromFixedValue));
		} else if (StringUtils.isNotEmpty(inputFromSessionKey)) {
			result = (Message)reportGenerator.getInputFromSessionKey(correlationId, inputFromSessionKey, Message.asMessage(pipeLineSession.get(inputFromSessionKey)));
		} else {
			result = input;
		}

		if (StringUtils.isNotEmpty(defaultValue) && Message.isEmpty(result)) {
			return Message.asMessage(reportGenerator.getDefaultValue(correlationId, defaultValue));
		}
		return result;
	}

	static class ParallelSenderExecutorWrapper implements Runnable {
		private final AbstractRequestReplyExecutor requestReplyExecutor;
		private final ThreadConnector<ThreadDebugInfo> threadConnector;

		ParallelSenderExecutorWrapper(ParallelSenderExecutor parallelSenderExecutor, ThreadLifeCycleEventListener<ThreadDebugInfo> threadLifeCycleEventListener) {
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
		AppConstants.setGlobalProperty("testtool.enabled", enable);
	}
	private boolean isEnabled() {
		return reportGenerator != null && enabled;
	}

	@Override
	public void onApplicationEvent(@NonNull DebuggerStatusChangedEvent event) {
		log.debug("received DebuggerStatusChangedEvent [{}]", event);
		setEnabled(event.isEnabled());
	}
}
