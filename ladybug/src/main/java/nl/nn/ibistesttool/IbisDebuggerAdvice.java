/*
   Copyright 2018 Nationale-Nederlanden

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

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.IsolatedServiceExecutor;
import nl.nn.adapterframework.senders.ParallelSenderExecutor;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */
public class IbisDebuggerAdvice {
	private IbisDebugger ibisDebugger;

	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

	public Object debugPipeLineInputOutputAbort(
			ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine,
			String correlationId, String message,
			IPipeLineSession pipeLineSession) throws Throwable {
		message = (String)ibisDebugger.pipeLineInput(pipeLine, correlationId, message);
		TreeSet keys = new TreeSet(pipeLineSession.keySet());
		Iterator iterator = keys.iterator();
		while (iterator.hasNext()) {
			String sessionKey = (String)iterator.next();
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

	public Object debugPipeInputOutputAbort(
			ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine,
			IPipe pipe, String messageId, Object message,
			IPipeLineSession pipeLineSession) throws Throwable {
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

	public Object debugPipeGetInputFrom(
			ProceedingJoinPoint proceedingJoinPoint, PipeLine pipeLine,
			IPipe pipe, String messageId, Object message,
			IPipeLineSession pipeLineSession) throws Throwable {
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

	public Object debugSenderInputOutputAbort(
			ProceedingJoinPoint proceedingJoinPoint, String correlationId,
			String message) throws Throwable {
		ISender sender = (ISender)proceedingJoinPoint.getTarget();
		if (!sender.isSynchronous() && sender instanceof JmsSender) {
			// Ignore JmsSenders within JmsListeners (calling JmsSender without
			// ParameterResolutionContext) within Receivers.
			return proceedingJoinPoint.proceed();
		} else {
			Object result = debugSenderInputAbort(proceedingJoinPoint, sender, correlationId, message);
			return ibisDebugger.senderOutput(sender, correlationId, result);
		}
	}

	 public Object debugSenderWithParametersInputOutputAbort(
			ProceedingJoinPoint proceedingJoinPoint, String correlationId,
			String message,
			ParameterResolutionContext parameterResolutionContext
			) throws Throwable {
		ISender sender = (ISender)proceedingJoinPoint.getTarget();
		Object preservedObject = message;
		Object result = debugSenderInputAbort(proceedingJoinPoint, sender, correlationId, message);
		if (sender instanceof SenderWrapperBase) {
			SenderWrapperBase senderWrapperBase = (SenderWrapperBase)sender;
			if (senderWrapperBase.isPreserveInput()) {
				result = (String)ibisDebugger.preserveInput(correlationId, preservedObject);
			}
		}
		return ibisDebugger.senderOutput(sender, correlationId, result);
	}

	public Object debugSenderGetInputFrom(
			ProceedingJoinPoint proceedingJoinPoint,
			SenderWrapperBase senderWrapperBase, String correlationId,
			String message,
			ParameterResolutionContext parameterResolutionContext
			) throws Throwable {
		message = (String)debugGetInputFrom(
				parameterResolutionContext.getSession(), correlationId,
				message, senderWrapperBase.getGetInputFromSessionKey(),
				senderWrapperBase.getGetInputFromFixedValue(), null);
		if (ibisDebugger.stubSender(senderWrapperBase, correlationId)) {
			return null;
		} else {
			Object[] args = proceedingJoinPoint.getArgs();
			args[2] = message;
			return proceedingJoinPoint.proceed(args);
		}
	}

	public Object debugReplyListenerInputOutputAbort(
			ProceedingJoinPoint proceedingJoinPoint,
			ICorrelatedPullingListener listener, String correlationId,
			IPipeLineSession pipeLineSession) throws Throwable {
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

	public Object debugThreadCreateStartEndAbort(
			ProceedingJoinPoint proceedingJoinPoint, Runnable runnable
			) throws Throwable {
		if (runnable instanceof ParallelSenderExecutor
				|| runnable instanceof IsolatedServiceExecutor) {
			RequestReplyExecutor requestReplyExecutor = (RequestReplyExecutor)runnable;
			String createThreadId = Misc.createSimpleUUID();
			Executor executor = new Executor();
			executor.setIbisDebugger(ibisDebugger);
			executor.setRequestReplyExecutor(requestReplyExecutor);
			executor.setCreateThreadId(createThreadId);
			ibisDebugger.createThread(requestReplyExecutor, createThreadId, requestReplyExecutor.getCorrelationID());
			Object[] args = proceedingJoinPoint.getArgs();
			args[0] = executor;
			return proceedingJoinPoint.proceed(args);
		} else {
			return proceedingJoinPoint.proceed();
		}
	}

	public Object debugParameterResolvedTo(
			ProceedingJoinPoint proceedingJoinPoint,
			ParameterValueList alreadyResolvedParameters,
			ParameterResolutionContext parameterResolutionContext
			) throws Throwable {
		Object result = proceedingJoinPoint.proceed();
		Parameter parameter = (Parameter)proceedingJoinPoint.getTarget();
		return ibisDebugger.parameterResolvedTo(parameter, parameterResolutionContext.getSession().getMessageId(), result);
	}

	private Object debugGetInputFrom(IPipeLineSession pipeLineSession,
			String correlationId, Object input, String inputFromSessionKey,
			String inputFromFixedValue, String emptyInputReplacement) {
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
	

	private Object debugSenderInputAbort(
			ProceedingJoinPoint proceedingJoinPoint, ISender sender,
			String correlationId, String message) throws Throwable {
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
		IbisDebugger ibisDebugger;
		RequestReplyExecutor requestReplyExecutor;
		String createThreadId;

		public void setIbisDebugger(IbisDebugger ibisDebugger) {
			this.ibisDebugger = ibisDebugger;
		}

		public void setRequestReplyExecutor(RequestReplyExecutor requestReplyExecutor) {
			this.requestReplyExecutor = requestReplyExecutor;
		}
		
		public void setCreateThreadId(String createThreadId) {
			this.createThreadId = createThreadId;
		}

		public void run() {
			ibisDebugger.startThread(requestReplyExecutor, createThreadId, requestReplyExecutor.getCorrelationID(), requestReplyExecutor.getRequest());
			try {
				requestReplyExecutor.run();
			} finally {
				Throwable throwable = requestReplyExecutor.getThrowable();
				if (throwable == null) {
					Object reply = requestReplyExecutor.getReply();
					reply = ibisDebugger.endThread(requestReplyExecutor, requestReplyExecutor.getCorrelationID(), reply);
					requestReplyExecutor.setReply(reply);
				} else {
					throwable = ibisDebugger.abortThread(requestReplyExecutor, requestReplyExecutor.getCorrelationID(), throwable);
					requestReplyExecutor.setThrowable(throwable);
				}
			}
		}
	}

}
