/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import org.frankframework.core.HasName;
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
	protected Logger secLog = LogUtil.getLogger("SEC");

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
	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		Object preservedObject = message;
		HasName owner = pipeLine.getOwner();

		try (CloseableThreadContext.Instance ignored = CloseableThreadContext.put("pipe", pipe.getName())) {
			if (StringUtils.isNotEmpty(pipe.getGetInputFromSessionKey())) {
				log.debug("Pipeline of adapter [{}] replacing input for pipe [{}] with contents of sessionKey [{}]", owner::getName, pipe::getName, pipe::getGetInputFromSessionKey);
				message.closeOnCloseOf(pipeLineSession, owner);
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
					message = pipeLineSession.getMessage(pipe.getGetInputFromSessionKey());
				}
			}
			if (StringUtils.isNotEmpty(pipe.getGetInputFromFixedValue())) {
				log.debug("Pipeline of adapter [{}] replacing input for pipe [{}] with fixed value [{}]", owner::getName, pipe::getName, pipe::getGetInputFromFixedValue);
				message.closeOnCloseOf(pipeLineSession, owner);
				message = new Message(pipe.getGetInputFromFixedValue());
			}

			if (Message.isEmpty(message) && StringUtils.isNotEmpty(pipe.getEmptyInputReplacement())) {
				log.debug("Pipeline of adapter [{}] replacing empty input for pipe [{}] with fixed value [{}]", owner::getName, pipe::getName, pipe::getEmptyInputReplacement);
				message = new Message(pipe.getEmptyInputReplacement());
			}

			PipeRunResult pipeRunResult = null;
			if (pipe instanceof FixedForwardPipe ffPipe) {
				if (ffPipe.skipPipe(message, pipeLineSession)) {
					log.info("skipped pipe processing");
					pipeRunResult = new PipeRunResult(ffPipe.getSuccessForward(), message);
				}
			}

			if (pipeRunResult == null) {
				pipeRunResult = chain.apply(message);
			}
			if (pipeRunResult == null) {
				throw new PipeRunException(pipe, "Pipeline of [" + pipeLine.getOwner().getName() + "] received null result from pipe [" + pipe.getName() + "]d");
			}

			if (pipe.isRestoreMovedElements()) {
				processRestoreMovedElements(pipe, pipeLineSession, owner, pipeRunResult);
			}

			if (pipe.getChompCharSize() != null || pipe.getElementToMove() != null || pipe.getElementToMoveChain() != null) {
				processMessageCompaction(pipe, pipeLineSession, owner, pipeRunResult);
			}

			if (StringUtils.isNotEmpty(pipe.getStoreResultInSessionKey())) {
				log.debug("Pipeline of adapter [{}] storing result for pipe [{}] under sessionKey [{}]", owner::getName, pipe::getName, pipe::getStoreResultInSessionKey);
				Message result = pipeRunResult.getResult();
				pipeLineSession.put(pipe.getStoreResultInSessionKey(), result);
				if (!pipe.isPreserveInput() && !result.isRepeatable()) {
					// when there is a duplicate use of the result (in a sessionKey as well as as the result), then message must be repeatable
					try {
						result.preserve();
					} catch (IOException e) {
						throw new PipeRunException(pipe, "Pipeline of [" + pipeLine.getOwner().getName() + "] could not preserve output", e);
					}
				}
			}
			if (pipe.isPreserveInput()) {
				pipeRunResult.getResult().closeOnCloseOf(pipeLineSession, owner);
				pipeRunResult.setResult(preservedObject);
			}

			if (pipe.isWriteToSecLog()) {
				String secLogMsg = "adapter [" + owner.getName() + "] pipe [" + pipe.getName() + "]";
				if (pipe.getSecLogSessionKeys() != null) {
					secLogMsg = secLogMsg + " sessionKeys [" +
							StringUtil.splitToStream(pipe.getSecLogSessionKeys(), " ,;")
									.map(key -> key + "=" + pipeLineSession.get(key))
									.collect(Collectors.joining(","))
							+ "]";
				}
				secLog.info(secLogMsg);
			}

			return pipeRunResult;
		}
	}

	private void processMessageCompaction(IPipe pipe, PipeLineSession pipeLineSession, HasName owner, PipeRunResult pipeRunResult) throws PipeRunException {
		log.debug("Pipeline of adapter [{}] compact received message", owner::getName);
		Message result = pipeRunResult.getResult();
		if (Message.isEmpty(result)) {
			return;
		}
		InputSource inputSource = getInputSourceFromResult(result, pipe, owner);

		try {
			MessageBuilder messageBuilder = new MessageBuilder();

			CompactSaxHandler handler = new CompactSaxHandler(messageBuilder.asXmlWriter());
			handler.setChompCharSize(pipe.getChompCharSize());
			handler.setElementToMove(pipe.getElementToMove());
			handler.setElementToMoveChain(pipe.getElementToMoveChain());
			handler.setElementToMoveSessionKey(pipe.getElementToMoveSessionKey());
			handler.setRemoveCompactMsgNamespaces(pipe.isRemoveCompactMsgNamespaces());
			handler.setContext(pipeLineSession);
			XmlUtils.parseXml(inputSource, handler);
			result.closeOnCloseOf(pipeLineSession, owner); // Directly closing the result fails, because the message can also exist and used in the session
			pipeRunResult.setResult(messageBuilder.build());
		} catch (IOException | SAXException e) {
			log.warn("Pipeline of adapter [{}] could not compact received message", owner.getName(), e);
		}
	}

	private void processRestoreMovedElements(IPipe pipe, PipeLineSession pipeLineSession, HasName owner, PipeRunResult pipeRunResult) throws PipeRunException {
		log.debug("Pipeline of adapter [{}] restoring from compacted result for pipe [{}]", owner::getName, pipe::getName);
		Message result = pipeRunResult.getResult();
		if (Message.isEmpty(result)) {
			return;
		}

		result.closeOnCloseOf(pipeLineSession, owner);
		InputSource inputSource = getInputSourceFromResult(result, pipe, owner);

		try {
			MessageBuilder messageBuilder = new MessageBuilder();

			XmlWriter xmlWriter = messageBuilder.asXmlWriter();
			RestoreMovedElementsHandler handler = new RestoreMovedElementsHandler(xmlWriter);
			handler.setSession(pipeLineSession);

			XmlUtils.parseXml(inputSource, handler);

			Message restoredResult = messageBuilder.build();
			restoredResult.closeOnCloseOf(pipeLineSession, owner);
			pipeRunResult.setResult(restoredResult);
		} catch (SAXException | IOException e) {
			throw new PipeRunException(pipe, "could not restore moved elements", e);
		}
	}

	private static InputSource getInputSourceFromResult(Message result, IPipe pipe, HasName owner) throws PipeRunException {
		try {
			// Preserve the message so that it can be read again, in case there was an error during compacting
			result.preserve();
			return result.asInputSource();
		} catch (IOException e) {
			throw new PipeRunException(pipe, "Pipeline of [" + owner.getName() + "] could not read received message during restoring/compaction of moved elements: " + e.getMessage(), e);
		}
	}

	@Override // method needs to be overridden to enable AOP for debugger
	public PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession) throws PipeRunException {
		return super.processPipe(pipeLine, pipe, message, pipeLineSession);
	}

}
