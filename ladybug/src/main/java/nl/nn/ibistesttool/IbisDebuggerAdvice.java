/*
   Copyright 2018-2020 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.xml.sax.ContentHandler;

import lombok.Setter;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IBlockEnabledSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.management.bus.DebuggerStatusChangedEvent;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.AbstractPipe;
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
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.stream.xml.XmlTee;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamCaptureUtils;
import nl.nn.adapterframework.xml.IXmlDebugger;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */
public class IbisDebuggerAdvice implements InitializingBean, ThreadLifeCycleEventListener<ThreadDebugInfo>, ApplicationListener<DebuggerStatusChangedEvent>, IXmlDebugger {
	protected Logger log = LogUtil.getLogger(this);

	private @Setter IbisDebugger ibisDebugger;
	private @Setter IbisManager ibisManager;

	// Contract for testtool state:
	// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
	// - to get notified of changes, components should listen to DebuggerStatusChangedEvents
	// IbisDebuggerAdvice stores state in appconstants testtool.enabled for use by GUI
	private static boolean enabled=true;

	private final AtomicInteger threadCounter = new AtomicInteger(0);


	@Override
	public void afterPropertiesSet() throws Exception {
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
		message = ibisDebugger.pipeLineInput(pipeLine, correlationId, message);
		TreeSet<String> keys = new TreeSet<>(session.keySet());
		Iterator<String> iterator = keys.iterator();
		while (iterator.hasNext()) {
			String sessionKey = iterator.next();
			Object sessionValue = session.get(sessionKey);
			sessionValue = ibisDebugger.pipeLineSessionKey(correlationId, sessionKey, sessionValue);
			session.put(sessionKey, sessionValue);
		}
		PipeLineResult pipeLineResult = null;
		try {
			PipeLineSession pipeLineSessionDebugger = PipeLineSessionDebugger.newInstance(session, ibisDebugger);
			Object[] args = proceedingJoinPoint.getArgs();
			args[3] = pipeLineSessionDebugger;
			pipeLineResult = (PipeLineResult)proceedingJoinPoint.proceed(args);
		} catch(Throwable throwable) {
			throw ibisDebugger.pipeLineAbort(pipeLine, correlationId, throwable);
		}
		ibisDebugger.showValue(correlationId, "exitState", pipeLineResult.getState().name());
		if (pipeLineResult.getExitCode()!=0) {
			ibisDebugger.showValue(correlationId, "exitCode", Integer.toString(pipeLineResult.getExitCode()));
		}
		if (!pipeLineResult.isSuccessful()) {
			ibisDebugger.showValue(correlationId, "result", pipeLineResult.getResult());
			ibisDebugger.pipeLineAbort(pipeLine, correlationId, null);
		} else {
			Message result = ibisDebugger.pipeLineOutput(pipeLine, correlationId, pipeLineResult.getResult());
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
		if (pipe instanceof IExtendedPipe && ((IExtendedPipe)pipe).isPreserveInput()) {
			// signal in the debugger that the result of the pipe has been replaced with the original input
			pipeRunResult.setResult(ibisDebugger.preserveInput(correlationId, pipeRunResult.getResult()));
		}
		pipeRunResult.setResult(ibisDebugger.pipeOutput(pipeLine, pipe, correlationId, pipeRunResult.getResult()));
		return pipeRunResult;
	}

	/**
	 * Provides advice for {@link CheckSemaphorePipeProcessor#processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session)}
	 * CheckSemaphorePipeProcessor is just after InputOutputPipeProcessor, so it sees the effect of the replacements made by the latter.
	 */
	public PipeRunResult debugPipeGetInputFrom(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		if (pipe instanceof IExtendedPipe) {
			IExtendedPipe pe = (IExtendedPipe)pipe;
			String correlationId = getCorrelationId(session);
			message = debugGetInputFrom(session, correlationId, message,
					pe.getGetInputFromSessionKey(),
					pe.getGetInputFromFixedValue(),
					pe.getEmptyInputReplacement());
		}
		Object[] args = proceedingJoinPoint.getArgs();
		args[2] = message;
		return (PipeRunResult)proceedingJoinPoint.proceed(args); // the PipeRunResult contains the original result, before replacing via preserveInput
	}

	public PipeRunResult debugValidatorInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine, IValidator validator, Message message, PipeLineSession session, String messageRoot) throws Throwable {
		if (!isEnabled()) {
			return (PipeRunResult)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(session);
		message = ibisDebugger.pipeInput(pipeLine, validator, correlationId, message);
		PipeRunResult pipeRunResult = null;

		if(StringUtils.isNotEmpty(messageRoot)) {
			ibisDebugger.showValue(correlationId, "MessageRoot to be asserted", messageRoot);
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

	private enum SenderReturnType {
		MESSAGE,
		PIPERUNRESULT,
		SENDERRESULT;
	}

	private <M> M debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession session, int messageParamIndex, SenderReturnType returnType) throws Throwable {
		if (!isEnabled()) {
			return (M)proceedingJoinPoint.proceed();
		}
		ISender sender = (ISender)proceedingJoinPoint.getTarget();

		String correlationId = getCorrelationId(session);
		message = ibisDebugger.senderInput(sender, correlationId, message);

		M result = null; // result can be PipeRunResult (for StreamingSenders) or Message (for all other Senders)
		// For SenderWrapperBase continue even when it needs to be stubbed
		// because for SenderWrapperBase this will be checked when it calls
		// sendMessage on his senderWrapperProcessor, hence
		// debugSenderGetInputFrom will be called.
		if (!ibisDebugger.stubSender(sender, correlationId) || sender instanceof SenderWrapperBase) {
			try {
				Object[] args = proceedingJoinPoint.getArgs();
				args[messageParamIndex] = message;
				result = (M)proceedingJoinPoint.proceed(args);
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
		if (sender instanceof SenderWrapperBase && ((SenderWrapperBase)sender).isPreserveInput()) {
			// signal in the debugger that the result of the sender has been replaced with the original input
			if (returnType==SenderReturnType.SENDERRESULT) {
				SenderResult senderResult = (SenderResult)result;
				senderResult.setResult(ibisDebugger.preserveInput(correlationId, senderResult.getResult()));
			} else {
				result = (M)ibisDebugger.preserveInput(correlationId, (Message)result);
			}
		}
		switch (returnType) {
			case MESSAGE:
				return (M)ibisDebugger.senderOutput(sender, correlationId, Message.asMessage(result));
			case PIPERUNRESULT:
				// Create PipeRunResult when streaming sender is stubbed, this will forward to the next pipe and process the
				// message in a streaming.auto=false way (also when at the time of the original report the message was
				// processed with streaming.auto=true)
				PipeRunResult prr = result!=null ? (PipeRunResult)result : new PipeRunResult();
				prr.setResult(ibisDebugger.senderOutput(sender, correlationId, prr.getResult()));
				return (M)prr;
			case SENDERRESULT:
				SenderResult senderResult = result!=null ? (SenderResult)result : new SenderResult(Message.nullMessage());
				ibisDebugger.showValue(correlationId, "success", senderResult.isSuccess());
				if (senderResult.getForwardName()!=null) {
					ibisDebugger.showValue(correlationId, "forwardName", senderResult.getForwardName());
				}
				if (StringUtils.isNotEmpty(senderResult.getErrorMessage())) {
					ibisDebugger.showValue(correlationId, "errorMessage", senderResult.getErrorMessage());
				}
				senderResult.setResult(ibisDebugger.senderOutput(sender, correlationId, senderResult.getResult()));
				return (M)senderResult;
			default:
				throw new IllegalStateException("Unknown ReturnType ["+returnType+"]");
		}
	}

	/**
	 * Provides advice for {@link ISender#sendMessageOrThrow(Message message, PipeLineSession session)}
	 */
	public SenderResult debugSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession session) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 0, SenderReturnType.SENDERRESULT);
	}

	/**
	 * Provides advice for {@link IBlockEnabledSender#sendMessage(Object blockHandle, Message message, PipeLineSession session)}
	 */
	public SenderResult debugBlockEnabledSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Object blockHandle, Message message, PipeLineSession session) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 1, SenderReturnType.SENDERRESULT);
	}

	/**
	 * Provides advice for {@link IStreamingSender#sendMessage(Message message, PipeLineSession session, IForwardTarget next)}
	 */
	public PipeRunResult debugStreamingSenderInputOutputAbort(ProceedingJoinPoint proceedingJoinPoint, Message message, PipeLineSession session, IForwardTarget next) throws Throwable {
		return debugSenderInputOutputAbort(proceedingJoinPoint, message, session, 0, SenderReturnType.PIPERUNRESULT);
	}

	/**
	 * Provides advice for {@link IOutputStreamingSupport#provideOutputStream(PipeLineSession session, IForwardTarget next)}
	 */
	public MessageOutputStream debugProvideOutputStream(ProceedingJoinPoint proceedingJoinPoint, PipeLineSession session) throws Throwable {
		if (!isEnabled()) {
			return (MessageOutputStream)proceedingJoinPoint.proceed();
		}
		String correlationId = getCorrelationId(session);
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
				try (Writer writer = writerPlaceHolder.getWriter()) {
					writer.write("--> Requesting OutputStream from next pipe"); //We already know it failed, but it's a more user-friendly message..
				}
			}
		}
		return resultStream!=null ? "<-- OutputStream provided" : "<-- Request to provide OutputStream could not be honored, no outputstream provided";
	}

	@Override
	public ContentHandler inspectXml(PipeLineSession session, String label, ContentHandler contentHandler, BiConsumer<AutoCloseable,String> closeOnCloseRegister) {
		if (!isEnabled()) {
			return contentHandler;
		}
		String correlationId = getCorrelationId(session);
		WriterPlaceHolder writerPlaceHolder = ibisDebugger.showValue(correlationId, label, new WriterPlaceHolder());
		if (writerPlaceHolder!=null && writerPlaceHolder.getWriter()!=null) {
			Writer writer = writerPlaceHolder.getWriter();
			closeOnCloseRegister.accept(writer, "debugger for inspectXml labeled ["+label+"]");
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
		if (runnable instanceof ParallelSenderExecutor) {
			ParallelSenderExecutorWrapper executor = new ParallelSenderExecutorWrapper((ParallelSenderExecutor) runnable, this);
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
			String nameClause=threadInfo.owner instanceof INamedObject?" name ["+((INamedObject)threadInfo.owner).getName()+"]":"";
			log.debug("announceChildThread thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+threadInfo.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+threadInfo.threadId+"] correlationId ["+threadInfo.correlationId+"]");
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
			String nameClause=threadInfo.owner instanceof INamedObject?" name ["+((INamedObject)threadInfo.owner).getName()+"]":"";
			log.debug("cancelChildThread thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+threadInfo.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+threadInfo.threadId+"] correlationId ["+threadInfo.correlationId+"]");
		}
		ibisDebugger.cancelThread(threadInfo.owner, threadInfo.threadId, threadInfo.correlationId);
	}

	@Override
	public <R> R threadCreated(ThreadDebugInfo ref, R request) {
		if (!isEnabled()) {
			return request;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject?" name ["+((INamedObject)ref.owner).getName()+"]":"";
			log.debug("threadCreated thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+ref.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+ref.threadId+"] correlationId ["+ref.correlationId+"]");
		}
		return (R)ibisDebugger.startThread(ref.owner, ref.threadId, ref.correlationId, request);
	}

	@Override
	public <R> R threadEnded(ThreadDebugInfo ref, R result) {
		if (!isEnabled()) {
			return result;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject?" name ["+((INamedObject)ref.owner).getName()+"]":"";
			log.debug("threadEnded thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+ref.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+ref.threadId+"] correlationId ["+ref.correlationId+"]");
		}
		return (R)ibisDebugger.endThread(ref.owner, ref.correlationId, result);
	}

	@Override
	public Throwable threadAborted(ThreadDebugInfo ref, Throwable t) {
		if (!isEnabled()) {
			return t;
		}
		if (log.isDebugEnabled()) {
			String nameClause=ref.owner instanceof INamedObject?" name ["+((INamedObject)ref.owner).getName()+"]":"";
			log.debug("threadAborted thread id ["+Thread.currentThread().getId()+"] thread name ["+Thread.currentThread().getName()+"] owner ["+ref.owner.getClass().getSimpleName()+"]"+nameClause+" threadId ["+ref.threadId+"] correlationId ["+ref.correlationId+"]");
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
		Parameter parameter = (Parameter)proceedingJoinPoint.getTarget();
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
		if (input == null || input.isEmpty()) {
			if (StringUtils.isNotEmpty(emptyInputReplacement)) {
				input = Message.asMessage(ibisDebugger.getEmptyInputReplacement(correlationId, emptyInputReplacement));
			}
		}
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

	public void setEnabled(boolean enable) {
		enabled = enable;
		AppConstants.getInstance().put("testtool.enabled", String.valueOf(enable));
	}
	public boolean isEnabled() {
		return ibisDebugger != null && enabled;
	}

	@Override
	public void onApplicationEvent(DebuggerStatusChangedEvent event) {
		setEnabled(event.isEnabled());
	}
}
