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
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.AppConstants;

public abstract class StreamingPipe extends FixedForwardPipe implements IOutputStreamingSupport {

	public final String AUTOMATIC_STREAMING = "streaming.auto";

	private boolean streamingActive=AppConstants.getInstance().getBoolean(AUTOMATIC_STREAMING, false);
	
	private boolean determinedStreamTarget=false;
	private IOutputStreamingSupport streamTarget;


	@Override
	public void start() throws PipeStartException {
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
	}

	public IForwardTarget getNextPipe() {
		if (getPipeLine()==null) {
			return null;
		}
		
		PipeForward forward = getForward();
		try {
			return getPipeLine().getForward(this, forward.getPath());
		} catch (PipeRunException e) {
			log.warn("no next pipe found",e);
			return null;
		}
	}
	
	public boolean canProvideOutputStream() {
		return StringUtils.isEmpty(getGetInputFromSessionKey());
	}

	public boolean requiresOutputStream() {
		return StringUtils.isEmpty(this.getStoreResultInSessionKey());
	}

	/**
	 * provide the outputstream, or null if a stream cannot be provided.
	 * If nextProvider is null, then descendants must replace it with getStreamTarget().
	 */
	@Override
	public MessageOutputStream provideOutputStream(IPipeLineSession session, IForwardTarget next) throws StreamingException {
		return null;
	}


	@IbisDoc({"controls whether output streaming is used. Can be used to switch streaming off for debugging purposes","set by appconstant streaming.auto"})
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
