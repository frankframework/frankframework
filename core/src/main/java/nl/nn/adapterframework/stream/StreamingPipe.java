/*
   Copyright 2019, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.stream;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.AppConstants;

public abstract class StreamingPipe extends FixedForwardPipe implements IOutputStreamingSupport {

	public static final String AUTOMATIC_STREAMING = "streaming.auto";

	private boolean streamingActive=AppConstants.getInstance().getBoolean(AUTOMATIC_STREAMING, false);
	private boolean canProvideOutputStream;
	private boolean canStreamToNextPipe;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		boolean outputSideEffectsPresent = StringUtils.isNotEmpty(getStoreResultInSessionKey()) || isPreserveInput() || isRestoreMovedElements()
				|| StringUtils.isNotEmpty(getElementToMove()) || StringUtils.isNotEmpty(getChompCharSize()) || StringUtils.isNotEmpty(getElementToMoveChain());

		canProvideOutputStream = StringUtils.isEmpty(getGetInputFromSessionKey()) && StringUtils.isEmpty(getGetInputFromFixedValue())
				&& StringUtils.isEmpty(getEmptyInputReplacement()) && !isSkipOnEmptyInput()
				&& getLocker()==null && StringUtils.isEmpty(getIfParam())
				&& (getParameterList()==null || !getParameterList().isInputValueOrContextRequiredForResolution())
				&& !outputSideEffectsPresent;
		canStreamToNextPipe = !outputSideEffectsPresent && !isWriteToSecLog();
	}

	/**
	 * Find the target to receive this pipes output, i.e that could provide an output stream
	 */
	protected IForwardTarget getNextPipe() {
		if (getPipeLine()==null) {
			return null;
		}

		PipeForward forward = getSuccessForward();
		try {
			return getPipeLine().resolveForward(this, forward);
		} catch (PipeRunException e) {
			log.warn("no next pipe found",e);
			return null;
		}
	}


	/**
	 * returns true when:
	 *  a) the pipe might be able to accept an input by providing an OutputStream, and
	 *  b) there are no side effects configured that prevent handing over its PipeRunResult to the calling pipe (e.g. storeResultInSessionKey)
	 *  c) there are no side effects that require the input to be available at the end of the pipe (e.g. preserveInput=true)
	 *  d) there are no parameters that require the input value or context
	 *  (b and c are implemented by PipeProcessors, that do not support outputStreaming)
	 */
	protected boolean canProvideOutputStream() {
		return canProvideOutputStream;
	}

	/**
	 * called if the pipe implementation requests an OutputStream, to determine if there are side effects configured
	 * that require the output of this pipe to be available at the return of the doPipe() method.
	 * All side effects that are handled by PipeProcessors (storeResultInSessionKey, preserveInput) and
	 * move and chop actions (restoreMovedElements, elementToMove, chompCharSize, ElementToMoveChain) inhibit streaming to the next pipe.
	 */
	protected boolean canStreamToNextPipe() {
		return canStreamToNextPipe;
	}

	/**
	 * provide the outputstream, or null if a stream cannot be provided.
	 * Implementations should provide a forward target by calling {@link #getNextPipe()}.
	 */
	protected MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		log.debug("pipe [{}] has no implementation to provide an outputstream", this::getName);
		return null;
	}

	/**
	 * Don't override unless you're absolutely sure what you're doing!
	 */
	@Override //Can't make AOP'd methods final
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		if (!isStreamingActive() || !canProvideOutputStream()) {
			log.debug("pipe [{}] cannot provide outputstream", this::getName);
			return null;
		}
		log.debug("pipe [{}] creating outputstream", this::getName);
		return provideOutputStream(session);
	}


	/**
	 * Provides a non-null MessageOutputStream, that the caller can use to obtain a Writer, OutputStream or ContentHandler.
	 */
	protected MessageOutputStream getTargetStream(PipeLineSession session) throws StreamingException {
		if (canStreamToNextPipe()) {
			IForwardTarget nextPipe = getNextPipe();
			if (log.isDebugEnabled()) {
				if (nextPipe != null) {
					log.debug("pipe [{}] can stream to next pipe [{}]", getName(), nextPipe.getName());
				} else {
					log.debug("pipe [{}] can stream, but no target to stream to", getName());
				}
			}
			return MessageOutputStream.getTargetStream(this, session, nextPipe);
		}
		return new MessageOutputStreamCap(this, getNextPipe());
	}


	@IbisDoc({"If true, then this pipe can provide an OutputStream to the previous pipe, to write its output to. Can be used to switch this streaming off for debugging purposes","set by appconstant streaming.auto"})
	public void setStreamingActive(boolean streamingActive) {
		this.streamingActive = streamingActive;
	}
	public boolean isStreamingActive() {
		return streamingActive;
	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return false;
	}


}
