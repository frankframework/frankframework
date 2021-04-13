/*
   Copyright 2019, 2020 WeAreFrank!

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

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.AppConstants;

public abstract class StreamingPipe extends FixedForwardPipe implements IOutputStreamingSupport {

	public final String AUTOMATIC_STREAMING = "streaming.auto";

	private boolean streamingActive=AppConstants.getInstance().getBoolean(AUTOMATIC_STREAMING, false);
	

	public IForwardTarget getNextPipe() {
		if (getPipeLine()==null) {
			return null;
		}
		
		PipeForward forward = getForward();
		try {
			return getPipeLine().resolveForward(this, forward);
		} catch (PipeRunException e) {
			log.warn("no next pipe found",e);
			return null;
		}
	}
	

	/**
	 * returns true when:
	 *  a) the pipe can accept input by providing an OutputStream, and 
	 *  b) there are no side effects configured that prevent handing over its PipeRunResult to the calling pipe.
	 */
	public boolean canProvideOutputStream() {
		return StringUtils.isEmpty(getGetInputFromSessionKey()) && StringUtils.isEmpty(this.getStoreResultInSessionKey());
	}

	/**
	 * returns true when:
	 *  a) the operation needs to have an OutputStream, and 
	 *  b) there are no side effects configured that require the output of this pipe to be available at the return of the doPipe() method.
	 */
	public boolean canStreamToNextPipe() {
		return StringUtils.isEmpty(this.getStoreResultInSessionKey());
	}

	/**
	 * provide the outputstream, or null if a stream cannot be provided.
	 * Implementations should provide a forward target by calling {@link #getNextPipe()}.
	 */
	public MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		return null;
	}

	@Override
	public final MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		return provideOutputStream(session);
	}
	

	/**
	 * Provides a non-null MessageOutputStream, that the caller can use to obtain a Writer, OutputStream or ContentHandler.
	 */
	protected MessageOutputStream getTargetStream(PipeLineSession session) throws StreamingException {
		if (canStreamToNextPipe()) {
			return MessageOutputStream.getTargetStream(this, session, getNextPipe());
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
		// TODO Auto-generated method stub
		return false;
	}


}
