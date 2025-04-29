/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.processors;

import java.io.IOException;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.CompactSaxHandler;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RestoreMovedElementsHandler;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

/**
 * The InputOutputPipeProcessor class is a subclass of PipeProcessorBase and is responsible for processing pipes in a pipeline.
 * It handles input and output manipulation, including replacing input with session values, replacing input with fixed values,
 * replacing empty input with a fixed value, restoring moved elements from a compacted result, compacting a received message,
 * storing a result in a session key, preserving input, and writing to a secure log.
 *
 * @author Jaco de Groot
 */
public class InputOutputPipeProcessor extends AbstractPipeProcessor {
	private static final Logger SEC_LOG = LogUtil.getLogger("SEC");

	/**
	 * Processes the pipe in the pipeline.
	 * This method is called by the Pipeline to process the given pipe.
	 * It performs various operations on the message and modifies it as required.
	 *
	 * @param pipeLine The PipeLine to which the pipe belongs.
	 * @param pipe The pipe to be processed.
	 * @param message The message to be processed.
	 * @param pipeLineSession The session of the pipeline execution.
	 * @param chain The chain of functions to be executed.
	 * @return The result of processing the pipe.
	 * @throws PipeRunException if there is an error during processing.
	 */
	// Message should not be nullable?
	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable final Message inputMessage, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		Message originalMessage = inputMessage;

		Message message;
		try (CloseableThreadContext.Instance ignored = CloseableThreadContext.put(LogUtil.MDC_PIPE_KEY, pipe.getName())) {
			// Get the input message for the pipe to be processed, does not return null.
			message = getInputFrom(pipe, inputMessage, pipeLineSession);

			// If the input is empty and the pipe has a replacement value, replace the input with the (fixed) value.
			if (Message.isEmpty(message) && StringUtils.isNotEmpty(pipe.getEmptyInputReplacement())) {
				message.close(); // Cleanup
				log.debug("replacing empty input with fixed value [{}]", pipe::getEmptyInputReplacement);
				message = new Message(pipe.getEmptyInputReplacement());
				message.closeOnCloseOf(pipeLineSession); // Technically not required but prevents the CleanerProvider from complaining.
			}

			// Do the actual pipe processing.
			final PipeRunResult pipeRunResult;
			if (pipe instanceof FixedForwardPipe ffPipe && ffPipe.skipPipe(message, pipeLineSession)) {
				log.info("skipped pipe processing");
				pipeRunResult = new PipeRunResult(ffPipe.getSuccessForward(), message);
			} else {
				pipeRunResult = chain.apply(message);
			}

			if (pipeRunResult == null) { // It is still possible for a PipeProcessor to return NULL?
				throw new PipeRunException(pipe, "received null result from pipe");
			}

			// Post processing.
			try {
				return postProcessPipeResult(pipe, pipeLineSession, pipeRunResult, originalMessage);
			} finally {
				if (pipe.isWriteToSecLog()) {
					SEC_LOG.info("adapter [{}] pipe [{}]{}", () -> pipeLine.getOwner().getName(), pipe::getName, () -> computeSessionKeys(pipeLineSession, pipe));
				}
			}
		}
	}

	private String computeSessionKeys(PipeLineSession pipeLineSession, IPipe pipe) {
		if (pipe.getSecLogSessionKeys() == null) {
			return "";
		}
		return " sessionKeys [" + StringUtil.splitToStream(pipe.getSecLogSessionKeys(), " ,;").map(key -> key + "=" + pipeLineSession.get(key)).collect(
				Collectors.joining(",")) + "]";
	}

	private PipeRunResult postProcessPipeResult(IPipe pipe, PipeLineSession pipeLineSession, PipeRunResult pipeRunResult, Message originalMessage) throws PipeRunException {
		if (pipe.isRestoreMovedElements()) {
			processRestoreMovedElements(pipe, pipeLineSession, pipeRunResult);
		}

		if (pipe.getChompCharSize() != null || pipe.getElementToMove() != null || pipe.getElementToMoveChain() != null) {
			processMessageCompaction(pipe, pipeLineSession, pipeRunResult);
		}

		if (StringUtils.isNotEmpty(pipe.getStoreResultInSessionKey())) {
			log.debug("storing result in session under key [{}]", pipe::getStoreResultInSessionKey);
			Message result = pipeRunResult.getResult();
			pipeLineSession.put(pipe.getStoreResultInSessionKey(), result);
			if (!pipe.isPreserveInput() && !result.isRepeatable()) {
				// When there is a `duplicate use` of the result (in a sessionKey as well as as the result), then message must be repeatable!
				try {
					result.preserve();
				} catch (IOException e) {
					throw new PipeRunException(pipe, "could not preserve output", e);
				}
			}
		}
		if (pipe.isPreserveInput()) {
			pipeRunResult.getResult().closeOnCloseOf(pipeLineSession);
			pipeRunResult.setResult(originalMessage);
		}

		return pipeRunResult;
	}

	/**
	 * Handles getInputFrom -SessionKey and -FixedValue.
	 * Registers the original message to be closed, as we're not using it.
	 */
	@Nonnull
	private Message getInputFrom(@Nonnull final IPipe pipe, @Nullable final Message message, @Nonnull final PipeLineSession pipeLineSession) throws PipeRunException {
		// The order of these two methods has been changed to make it backwards compatible.
		if (StringUtils.isNotEmpty(pipe.getGetInputFromFixedValue())) {
			log.debug("replacing input with fixed value [{}]", pipe::getGetInputFromFixedValue);
			if (!Message.isNull(message)) message.closeOnCloseOf(pipeLineSession);
			Message newMessage = new Message(pipe.getGetInputFromFixedValue());
			newMessage.closeOnCloseOf(pipeLineSession); // Technically not required but prevents the CleanerProvider from complaining.
			return newMessage;
		}

		if (StringUtils.isNotEmpty(pipe.getGetInputFromSessionKey())) {
			log.debug("replacing input with contents of sessionKey [{}]", pipe::getGetInputFromSessionKey);
			if (!Message.isNull(message)) message.closeOnCloseOf(pipeLineSession);
			if (!pipeLineSession.containsKey(pipe.getGetInputFromSessionKey()) && StringUtils.isEmpty(pipe.getEmptyInputReplacement())) {
				boolean throwOnMissingSessionKey;
				if (pipe instanceof FixedForwardPipe ffp) {
					throwOnMissingSessionKey = !ffp.getGetInputFromSessionKey().equals(ffp.getOnlyIfSessionKey());
				} else {
					throwOnMissingSessionKey = true;
				}

				if (throwOnMissingSessionKey) {
					throw new PipeRunException(pipe, "getInputFromSessionKey [" + pipe.getGetInputFromSessionKey() + "] is not present in session");
				}
			} else {
				// Message fetched from session is already in closeables.
				return pipeLineSession.getMessage(pipe.getGetInputFromSessionKey());
			}
		}

		return message == null ? Message.nullMessage() : message;
	}

	private void processMessageCompaction(IPipe pipe, PipeLineSession pipeLineSession, PipeRunResult pipeRunResult) throws PipeRunException {
		Message result = pipeRunResult.getResult();
		if (Message.isEmpty(result)) {
			return;
		}
		log.debug("compacting result message");
		InputSource inputSource = getInputSourceFromResult(result, pipe);

		try {
			result.closeOnCloseOf(pipeLineSession); // Directly closing the result fails, because the message can also exist and used in the session

			MessageBuilder messageBuilder = new MessageBuilder();

			CompactSaxHandler handler = new CompactSaxHandler(messageBuilder.asXmlWriter());
			handler.setChompCharSize(pipe.getChompCharSize());
			handler.setElementToMove(pipe.getElementToMove());
			handler.setElementToMoveChain(pipe.getElementToMoveChain());
			handler.setElementToMoveSessionKey(pipe.getElementToMoveSessionKey());
			handler.setRemoveCompactMsgNamespaces(pipe.isRemoveCompactMsgNamespaces());
			handler.setContext(pipeLineSession);
			XmlUtils.parseXml(inputSource, handler);

			Message compactedResult = messageBuilder.build();
			compactedResult.closeOnCloseOf(pipeLineSession);
			pipeRunResult.setResult(compactedResult);
		} catch (IOException | SAXException e) {
			log.warn("could not compact received message", e);
		}
	}

	private void processRestoreMovedElements(IPipe pipe, PipeLineSession pipeLineSession, PipeRunResult pipeRunResult) throws PipeRunException {
		log.debug("restoring from compacted result");
		Message result = pipeRunResult.getResult();
		if (Message.isEmpty(result)) {
			return;
		}

		result.closeOnCloseOf(pipeLineSession);
		InputSource inputSource = getInputSourceFromResult(result, pipe);

		try {
			MessageBuilder messageBuilder = new MessageBuilder();

			XmlWriter xmlWriter = messageBuilder.asXmlWriter();
			RestoreMovedElementsHandler handler = new RestoreMovedElementsHandler(xmlWriter);
			handler.setSession(pipeLineSession);

			XmlUtils.parseXml(inputSource, handler);

			Message restoredResult = messageBuilder.build();
			restoredResult.closeOnCloseOf(pipeLineSession);
			pipeRunResult.setResult(restoredResult);
		} catch (SAXException | IOException e) {
			throw new PipeRunException(pipe, "could not restore moved elements", e);
		}
	}

	private static InputSource getInputSourceFromResult(Message result, IPipe pipe) throws PipeRunException {
		try {
			// Preserve the message so that it can be read again, in case there was an error during compacting
			result.preserve();
 			return result.asInputSource();
		} catch (IOException e) {
			throw new PipeRunException(pipe, "could not read received message during restoring/compaction of moved elements", e);
		}
	}

	@Override // method needs to be overridden to enable AOP for debugger
	public PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession) throws PipeRunException {
		return super.processPipe(pipeLine, pipe, message, pipeLineSession);
	}

}
