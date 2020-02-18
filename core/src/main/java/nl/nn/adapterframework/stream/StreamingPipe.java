/*
   Copyright 2019 Integration Partners

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

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
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
		getStreamTarget();
	}

	@Override
	public void stop() {
		super.stop();
	}

	public IPipe getNextPipe() {
		return getPipeLine().getPipe(getForwardName());
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
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, IOutputStreamingSupport nextProvider) throws StreamingException {
		return null;
	}

	/**
	 * Descendants of this class must implement this method. When nextProvider is not null, they can use that to obtain an OutputStream to write their results to. 
	 */
	public abstract PipeRunResult doPipe(Object input, IPipeLineSession session, IOutputStreamingSupport nextProvider) throws PipeRunException;
	
	@Override
	public final PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		return doPipe(input, session, getStreamTarget());
	}

	
	public IOutputStreamingSupport getStreamTarget() {
		if (!determinedStreamTarget) {
			determinedStreamTarget=true;
			PipeLine pipeline=getPipeLine();
			if (!isStreamingActive()) {
				log.debug("Do not try to setup streaming because streamingActive=false");
				return null;
			}
			if (pipeline==null) {
				log.debug("Cannot stream, no pipeline");
				return null;
			}
			PipeForward forward=getForward();
			if (forward==null) {
				log.debug("Forward is null, streaming stops");
				return null;
			}
			String forwardPath=forward.getPath();
			if (forwardPath==null) {
				log.debug("ForwardPath is null, streaming stops");
				return null;
			}
			IPipe nextPipe=pipeline.getPipe(forwardPath);
			if (nextPipe==null) {
				log.debug("Pipeline ends here, streaming stops");
				return null;
			}
			if (!(nextPipe instanceof IOutputStreamingSupport)) {
				log.debug("nextPipe ["+forwardPath+"] type ["+nextPipe.getClass().getSimpleName()+"] does not support streaming");
				return null;
			}
			
			if (nextPipe instanceof StreamingPipe && !((StreamingPipe)nextPipe).isStreamingActive()) {
				log.debug("nextPipe ["+forwardPath+"] has not activated streaming");
				return null;
			}
			streamTarget = (IOutputStreamingSupport)nextPipe;
		}
		return streamTarget;
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
