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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
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

	private boolean streamingActive=AppConstants.getInstance().getBoolean(AUTOMATIC_STREAMING, false);;
//	private IPipe nextPipe;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
//		if (nextPipe!=null) {
//			nextPipe.configure();
//		}
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
//		if (nextPipe!=null) {
//			nextPipe.start();
//		}
	}

	@Override
	public void stop() {
//		if (nextPipe!=null) {
//			nextPipe.stop();
//		}
		super.stop();
	}

//	/**
//	 * Registers a pipe as next pipe, and as potential streaming target.
//	 */
//	public void addPipe(IPipe pipe) throws ConfigurationException {
//		nextPipe=pipe;
//	}
	
	@Override
	public boolean canProvideOutputStream() {
		return StringUtils.isEmpty(getGetInputFromSessionKey());
	}

	@Override
	public boolean canStreamToTarget() {
		return StringUtils.isEmpty(this.getStoreResultInSessionKey());
	}

	public IPipe getNextPipe() {
		return getPipeLine().getPipe(getForwardName());
	}
	/**
	 * Descendants of this class must implement this method, and write to the outputStream when it is not null. 
	 * @param input
	 * @param session
	 * @param outputStream
	 */
	public abstract PipeRunResult doPipe(Object input, IPipeLineSession session, MessageOutputStream outputStream) throws PipeRunException;
	
	@Override
	public final PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		List<IOutputStreamingSupport> streamTargets = getStreamTargets(session);
		if (streamTargets!=null && streamTargets.size()>0) {
			try {
				MessageOutputStream outputStream;
					outputStream = getNextPipesOutputStream(streamTargets, session);
				doPipe(input, session, outputStream);
				PipeForward finalForward=getFinalForward(streamTargets);
				return new PipeRunResult(finalForward, outputStream.getResponse());
			} catch (StreamingException e) {
				throw new PipeRunException(this,"Streaming exception", e);
			}
		} else {
			return doPipe(input, session, null);
		}
	}

	private List<IOutputStreamingSupport> getStreamTargets(IPipeLineSession session) {
		PipeLine pipeline=getPipeLine();
		if (!isStreamingActive() || pipeline==null) {
			return null;
		}
		List<IOutputStreamingSupport> streamTargets = new ArrayList<IOutputStreamingSupport>();
		String forwardName=getForwardName();
		while (true) {
			IPipe nextPipe=pipeline.getPipe(forwardName);
			if (!(nextPipe instanceof IOutputStreamingSupport)) {
				break;
			}
			if (nextPipe instanceof StreamingPipe && !((StreamingPipe)nextPipe).isStreamingActive()) {
				break;
			}
			IOutputStreamingSupport streamTarget = (IOutputStreamingSupport)nextPipe;
			if (!streamTarget.canProvideOutputStream()) {
				break;
			}
			streamTargets.add(streamTarget);
			if (!streamTarget.canStreamToTarget() || !(streamTarget instanceof FixedForwardPipe)) {
				break;
			}
			forwardName = ((FixedForwardPipe)nextPipe).getForwardName();
		}
		return streamTargets;
	}
	
	private PipeForward getFinalForward(List<IOutputStreamingSupport> streamTargets) {
		FixedForwardPipe lastPipe = (FixedForwardPipe)streamTargets.get(streamTargets.size()-1);
		return lastPipe.findForward(lastPipe.getForwardName());
	}
	
	private MessageOutputStream getNextPipesOutputStream(List<IOutputStreamingSupport> streamTargets, IPipeLineSession session) throws StreamingException {
		String correlationID=session.getMessageId();
		MessageOutputStream result=null;
		for(int i=streamTargets.size()-1; i>=0; i--) {
			result = streamTargets.get(i).provideOutputStream(correlationID, session, result);
		}
		return result;
	}

	@IbisDoc({"controls whether output streaming is used. Can be used to switch streaming off for debugging purposes","true"})
	public void setStreamingActive(boolean streamingActive) {
		this.streamingActive = streamingActive;
	}
	public boolean isStreamingActive() {
		return streamingActive;
	}


}
