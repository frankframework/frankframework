/*

   Copyright 2013 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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

import java.util.Map;

import org.jspecify.annotations.NullMarked;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IForwardTarget;
import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlException;
import org.frankframework.util.XmlUtils;

/**
 * @author Jaco de Groot
 */
@Log4j2
@NullMarked
public class CorePipeLineProcessor implements PipeLineProcessor {
	private @Setter PipeProcessor pipeProcessor;

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message input, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {

		// Validate / wrap message and get the first pipe to run
		ProcessingResult<IForwardTarget> preProcessingResult = preProcessInput(pipeLine, input, pipeLineSession, firstPipe);

		long size = preProcessingResult.message.size();
		if (size > 0L) {
			pipeLine.getRequestSizeStats().record(size);
		}

		if (pipeLine.isStoreOriginalMessageWithoutNamespaces()) {
			storeOriginalMessageWithoutNamespaces(preProcessingResult, pipeLineSession);
		}

		ProcessingResult<PipeLineExit> result = runToExit(pipeLine, preProcessingResult.forwardTarget, preProcessingResult.message, pipeLineSession);

		ProcessingResult<PipeLineExit> postProcessingResult = postProcessOutput(pipeLine, result, pipeLineSession, false);
		return createPipeLineResult(pipeLine, messageId, pipeLineSession, postProcessingResult);
	}

	private static PipeLineResult createPipeLineResult(PipeLine pipeLine, String messageId, PipeLineSession pipeLineSession, ProcessingResult<PipeLineExit> result) {
		PipeLineResult pipeLineResult = new PipeLineResult();
		PipeLineExit plExit = result.forwardTarget;
		ExitState state=plExit.getState();
		pipeLineResult.setState(state);
		pipeLineResult.setExitCode(plExit.getExitCode());
		if (!plExit.isEmptyResult() || result.message.isEmpty()) { // If message is empty always store it so meta-data is preserved even when the exit wants an empty result
			pipeLineResult.setResult(result.message);
		} else {
			pipeLineResult.setResult(Message.nullMessage());
		}
		if (log.isDebugEnabled()){  // for performance reasons
			StringBuilder skString = new StringBuilder();
			for (Map.Entry<String, Object> entry: pipeLineSession.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				skString.append("\n ").append(key).append("=[").append(value).append("]");
			}
			log.debug("Available session keys at finishing pipeline of adapter [{}]:{}", pipeLine.getAdapter().getName(), skString);
			log.debug("Pipeline of adapter [{}] finished processing messageId [{}] result: ({}) [{}] with exit-state [{}]", pipeLine.getAdapter()
					.getName(), messageId, result.message.getClass().getSimpleName(), result.message, state);
		}
		return pipeLineResult;
	}

	private static void storeOriginalMessageWithoutNamespaces(ProcessingResult<IForwardTarget> processingResult, PipeLineSession pipeLineSession) throws PipeRunException {
		Message message = processingResult.message;
		if (XmlUtils.isWellFormed(message, null)) {
			IPipe pipe = processingResult.forwardTarget instanceof IPipe ip ? ip : null;
			try {
				Message xsltResult = XmlUtils.removeNamespaces(message);
				pipeLineSession.put("originalMessageWithoutNamespaces", xsltResult);
			} catch (XmlException e) {
				throw new PipeRunException(pipe, "caught XmlException", e);
			}
		} else {
			log.warn("original message is not well-formed");
			pipeLineSession.put("originalMessageWithoutNamespaces", message);
		}
	}

	private ProcessingResult<IForwardTarget> preProcessInput(PipeLine pipeLine, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		// get the first pipe to run
		IForwardTarget forwardTarget = pipeLine.getPipe(firstPipe);

		boolean inputValidateError = false;
		IValidator inputValidator = pipeLine.getInputValidator();
		if (inputValidator != null) {
			log.debug("validating input");
			PipeRunResult validationResult = pipeProcessor.processPipe(pipeLine, inputValidator, message, pipeLineSession);
			if (!validationResult.isSuccessful()) {
				forwardTarget = pipeLine.resolveForward(inputValidator, validationResult.getPipeForward());
				log.warn("forwarding execution flow to [{}] due to validation fault", forwardTarget::getName);
				inputValidateError = true;
			}
			Message validatedMessage = validationResult.getResult();
			if (!validatedMessage.isEmpty()) {
				message=validatedMessage;
			}
		}

		if (!inputValidateError) {
			IPipe inputWrapper = pipeLine.getInputWrapper();
			if (inputWrapper != null) {
				log.debug("wrapping input");
				PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, inputWrapper, message, pipeLineSession);
				if (!wrapResult.isSuccessful()) {
					forwardTarget = pipeLine.resolveForward(inputWrapper, wrapResult.getPipeForward());
					log.warn("forwarding execution flow to [{}] due to wrap fault", forwardTarget::getName);
				} else {
					message = wrapResult.getResult();
				}
				log.debug("input after wrapping [{}]", message);
			}
		}

		return new ProcessingResult<>(forwardTarget, message);
	}

	private ProcessingResult<PipeLineExit> postProcessOutput(PipeLine pipeLine, ProcessingResult<PipeLineExit> processingResult, PipeLineSession pipeLineSession, boolean outputValidationFailedPreviously) throws PipeRunException {
		PipeLineExit plExit = processingResult.forwardTarget;
		Message result = processingResult.message;
		if (plExit.isEmptyResult()) {
			// No validation or wrapping on empty results
			return new ProcessingResult<>(plExit, result);
		}

		boolean outputWrapError = false;
		IForwardTarget forwardTarget = plExit;
		Message message = result;

		IPipe outputWrapper = pipeLine.getOutputWrapper();
		if (!plExit.isSkipWrapping() && outputWrapper != null) {
			log.debug("wrapping PipeLineResult");
			PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, outputWrapper, message, pipeLineSession);
			if (!wrapResult.isSuccessful()) {
				forwardTarget = pipeLine.resolveForward(outputWrapper, wrapResult.getPipeForward());
				log.warn("forwarding execution flow to [{}] due to wrap fault", forwardTarget::getName);
				outputWrapError = true;
			} else {
				log.debug("wrap succeeded");
				message = wrapResult.getResult();
			}
			log.debug("PipeLineResult after wrapping: ({}) [{}]", message.getClass().getSimpleName(), message);
		}

		IValidator outputValidator = pipeLine.getOutputValidator();
		if (!outputWrapError && !plExit.isSkipValidation() &&  outputValidator != null) {
			if (outputValidationFailedPreviously) {
				log.debug("validating error message after PipeLineResult validation failed");
			} else {
				log.debug("validating PipeLineResult");
			}
			String exitSpecificResponseRoot = plExit.getResponseRoot();
			PipeRunResult validationResult = pipeProcessor.validate(pipeLine, outputValidator, message, pipeLineSession, exitSpecificResponseRoot);
			if (!validationResult.isSuccessful()) {
				if (!outputValidationFailedPreviously) {
					forwardTarget = pipeLine.resolveForward(outputValidator, validationResult.getPipeForward());
					log.warn("forwarding execution flow to [{}] due to validation fault", forwardTarget::getName);
				} else {
					log.warn("validation of error message by validator [{}] failed, returning result anyhow", outputValidator::getName); // to avoid endless looping
					message = validationResult.getResult();
				}
			} else {
				log.debug("validation succeeded");
				message = validationResult.getResult();
			}
		}

		if (forwardTarget instanceof PipeLineExit pipeLineExit) {
			// If forwarding to an exit, return results as they now are
			return new  ProcessingResult<>(pipeLineExit, message);
		} else {
			// Forwarding to a pipe that handles output-validation errors
			ProcessingResult<PipeLineExit> errorHandlerResult = runToExit(pipeLine, forwardTarget, message, pipeLineSession);
			if (outputValidationFailedPreviously) {
				// If output validation already failed previously, do not again try to apply post-processing
				return errorHandlerResult;
			}
			// Recursive call to do post-processing of the output from error-handling pipe
			return postProcessOutput(pipeLine, errorHandlerResult, pipeLineSession, true);
		}
	}

	private ProcessingResult<PipeLineExit> runToExit(PipeLine pipeLine, IForwardTarget startAtTarget, Message input, PipeLineSession pipeLineSession) throws PipeRunException {
		Message message = input;
		IForwardTarget forwardTarget = startAtTarget;
		while (forwardTarget instanceof IPipe pipeToRun) {
			PipeRunResult pipeRunResult = pipeProcessor.processPipe(pipeLine, pipeToRun, message, pipeLineSession);
			message = pipeRunResult.getResult();

			PipeForward pipeForward = pipeRunResult.getPipeForward();
			// get the next pipe to run
			forwardTarget = pipeLine.resolveForward(pipeToRun, pipeForward);

			if (forwardTarget instanceof PipeLineExit pipeLineExit) {
				return new ProcessingResult<>(pipeLineExit, message);
			}
		}
		throw new IllegalStateException("Processing loop exited with forwardTarget [" + forwardTarget + "] that appears to be neither a Pipe nor a PipeLineExit");
	}

	private record ProcessingResult<T extends IForwardTarget>(T forwardTarget, Message message) {}
}
