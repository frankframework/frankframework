/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2021, 2023 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Logger;
import org.frankframework.pipes.FixedForwardPipe;
import org.xml.sax.InputSource;

import org.frankframework.core.IExtendedPipe;
import org.frankframework.core.INamedObject;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;

import org.frankframework.stream.Message;
import org.frankframework.util.CompactSaxHandler;
import org.frankframework.util.LogUtil;
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
public class InputOutputPipeProcessor extends PipeProcessorBase {
	protected Logger secLog = LogUtil.getLogger("SEC");

	private static final String ME_START = "{sessionKey:";
	private static final String ME_END = "}";

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
	protected PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		Object preservedObject = message;
		INamedObject owner = pipeLine.getOwner();

		IExtendedPipe pe=null;

		if (pipe instanceof IExtendedPipe) {
			pe = (IExtendedPipe)pipe;
		}

		try (CloseableThreadContext.Instance ctc = CloseableThreadContext.put("pipe", pipe.getName())) {
			if (pe!=null) {
				if (StringUtils.isNotEmpty(pe.getGetInputFromSessionKey())) {
					log.debug("Pipeline of adapter [{}] replacing input for pipe [{}] with contents of sessionKey [{}]", owner::getName, pe::getName, pe::getGetInputFromSessionKey);
					message.closeOnCloseOf(pipeLineSession, owner);
					if (!pipeLineSession.containsKey(pe.getGetInputFromSessionKey()) && StringUtils.isEmpty(pe.getEmptyInputReplacement())) {
						boolean throwOnMissingSessionKey;
						if (pipe instanceof FixedForwardPipe) {
							FixedForwardPipe ffp = (FixedForwardPipe) pipe;
							throwOnMissingSessionKey = !(ffp.getGetInputFromSessionKey().equals(ffp.getOnlyIfSessionKey()));
						} else {
							throwOnMissingSessionKey = true;
						}

						if (throwOnMissingSessionKey) {
							throw new PipeRunException(pe, "getInputFromSessionKey ["+pe.getGetInputFromSessionKey()+"] is not present in session");
						}
					} else {
						message = pipeLineSession.getMessage(pe.getGetInputFromSessionKey());
					}
				}
				if (StringUtils.isNotEmpty(pe.getGetInputFromFixedValue())) {
					log.debug("Pipeline of adapter [{}] replacing input for pipe [{}] with fixed value [{}]", owner::getName, pe::getName, pe::getGetInputFromFixedValue);
					message.closeOnCloseOf(pipeLineSession, owner);
					message = Message.asMessage(pe.getGetInputFromFixedValue());
				}

				if (Message.isEmpty(message) && StringUtils.isNotEmpty(pe.getEmptyInputReplacement())) {
					log.debug("Pipeline of adapter [{}] replacing empty input for pipe [{}] with fixed value [{}]", owner::getName, pe::getName, pe::getEmptyInputReplacement);
					message = Message.asMessage(pe.getEmptyInputReplacement());
				}
			}

			PipeRunResult pipeRunResult = null;
			if (pipe instanceof FixedForwardPipe) {
				FixedForwardPipe ffPipe = (FixedForwardPipe) pipe;
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

			if (pe !=null) {
				if (pe.isRestoreMovedElements()) {
					log.debug("Pipeline of adapter [{}] restoring from compacted result for pipe [{}]", owner::getName, pe::getName);
					Message result = pipeRunResult.getResult();
					if (!result.isEmpty()) {
						try {
							String resultString = result.asString();
							pipeRunResult.setResult(restoreMovedElements(resultString, pipeLineSession));
						} catch (IOException e) {
							throw new PipeRunException(pipe, "cannot open stream of result", e);
						}
					}
				}

				if (pe.getChompCharSize() != null || pe.getElementToMove() != null || pe.getElementToMoveChain() != null) {
					log.debug("Pipeline of adapter [{}] compact received message", owner::getName);
					Message result = pipeRunResult.getResult();
					if (result!=null && !result.isEmpty()) {
						InputSource inputSource;
						try {
							// Preserve the message so that it can be read again
							// in case there was an error during compacting
							result.preserve();
							inputSource = result.asInputSource();
						} catch (IOException e) {
							throw new PipeRunException(pipe, "Pipeline of ["+pipeLine.getOwner().getName()+"] could not read received message during compacting to more compact format: " + e.getMessage(), e);
						}
						XmlWriter xmlWriter = new XmlWriter();
						CompactSaxHandler handler = new CompactSaxHandler(xmlWriter);
						handler.setChompCharSize(pe.getChompCharSize());
						handler.setElementToMove(pe.getElementToMove());
						handler.setElementToMoveChain(pe.getElementToMoveChain());
						handler.setElementToMoveSessionKey(pe.getElementToMoveSessionKey());
						handler.setRemoveCompactMsgNamespaces(pe.isRemoveCompactMsgNamespaces());
						handler.setContext(pipeLineSession);
						try {
							XmlUtils.parseXml(inputSource, handler);
							pipeRunResult.setResult(xmlWriter.toString());
						} catch (Exception e) {
							log.warn("Pipeline of adapter [{}] could not compact received message: {}", owner.getName(), e.getMessage());
						}
					}
				}

				if (StringUtils.isNotEmpty(pe.getStoreResultInSessionKey())) {
					log.debug("Pipeline of adapter [{}] storing result for pipe [{}] under sessionKey [{}]", owner::getName, pe::getName, pe::getStoreResultInSessionKey);
					Message result = pipeRunResult.getResult();
					pipeLineSession.put(pe.getStoreResultInSessionKey(),result);
					if (!pe.isPreserveInput() && !result.isRepeatable()) {
						// when there is a duplicate use of the result (in a sessionKey as well as as the result), then message must be repeatable
						try {
							result.preserve();
						} catch (IOException e) {
							throw new PipeRunException(pipe, "Pipeline of ["+pipeLine.getOwner().getName()+"] could not preserve output", e);
						}
					}
				}
				if (pe.isPreserveInput()) {
					pipeRunResult.getResult().closeOnCloseOf(pipeLineSession, owner);
					pipeRunResult.setResult(preservedObject);
				}
			}

			if (pe != null && pe.isWriteToSecLog()) {
				String secLogMsg = "adapter [" + owner.getName() + "] pipe [" + pe.getName() + "]";
				if (pe.getSecLogSessionKeys() != null) {
					secLogMsg = secLogMsg + " sessionKeys [" +
							StringUtil.splitToStream(pe.getSecLogSessionKeys(), " ,;")
									.map(key -> key + "=" + pipeLineSession.get(key))
									.collect(Collectors.joining(","))
							+ "]";
				}
				secLog.info(secLogMsg);
			}

			return pipeRunResult;
		}
	}

	@Override // method needs to be overridden to enable AOP for debugger
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
		return super.processPipe(pipeLine, pipe, message, pipeLineSession);
	}

	private String restoreMovedElements(String inputString, PipeLineSession pipeLineSession) {
		StringBuilder buffer = new StringBuilder();
		int startPos = inputString.indexOf(ME_START);
		if (startPos == -1) {
			return inputString;
		}
		char[] inputChars = inputString.toCharArray();
		int copyFrom = 0;
		while (startPos != -1) {
			buffer.append(inputChars, copyFrom, startPos - copyFrom);
			int nextStartPos = inputString.indexOf(ME_START, startPos + ME_START.length());
			if (nextStartPos == -1) {
				nextStartPos = inputString.length();
			}
			int endPos = inputString.indexOf(ME_END, startPos + ME_START.length());
			if (endPos == -1 || endPos > nextStartPos) {
				log.warn("Found a start delimiter without an end delimiter while restoring from compacted result at position [{}] in [{}]", startPos, inputString);
				buffer.append(inputChars, startPos, nextStartPos - startPos);
				copyFrom = nextStartPos;
			} else {
				String movedElementSessionKey = inputString.substring(startPos + ME_START.length(),endPos);
				if (pipeLineSession.containsKey(movedElementSessionKey)) {
					String movedElementValue = pipeLineSession.getString(movedElementSessionKey);
					buffer.append(movedElementValue);
					copyFrom = endPos + ME_END.length();
				} else {
					log.warn("Did not find sessionKey [{}] while restoring from compacted result", movedElementSessionKey);
					buffer.append(inputChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = inputString.indexOf(ME_START, copyFrom);
		}
		buffer.append(inputChars, copyFrom, inputChars.length - copyFrom);
		return buffer.toString();
	}
}
