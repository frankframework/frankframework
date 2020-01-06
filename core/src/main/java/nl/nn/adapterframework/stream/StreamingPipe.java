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

import nl.nn.adapterframework.core.INamedObject;
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
	
	private List<IOutputStreamingSupport> streamTargets;
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		getStreamTargets();
	}

	@Override
	public void stop() {
		super.stop();
	}

	public IPipe getNextPipe() {
		return getPipeLine().getPipe(getForwardName());
	}
	
	@Override
	public boolean canProvideOutputStream() {
		return StringUtils.isEmpty(getGetInputFromSessionKey());
	}

	@Override
	public boolean requiresOutputStream() {
		return StringUtils.isEmpty(this.getStoreResultInSessionKey());
	}

	@Override
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		return null;
	}

	/**
	 * Descendants of this class must implement this method, and write to the outputStream when it is not null. 
	 */
	public abstract PipeRunResult doPipe(Object input, IPipeLineSession session, MessageOutputStream outputStream) throws PipeRunException;
	
	@Override
	public final PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		List<IOutputStreamingSupport> streamTargets = getStreamTargets();
		if (streamTargets!=null && streamTargets.size()>0) {
			try {
				log.debug(getLogPrefix(session)+"obtaining outputstream");
				MessageOutputStream outputStream = getNextPipesOutputStream(streamTargets, session);
				log.debug(getLogPrefix(session)+"executing pipe with outputstream");
				doPipe(input, session, outputStream);
				PipeForward finalForward=getFinalForward(streamTargets);
				log.debug(getLogPrefix(session)+"obtained forward ["+finalForward+"] from streamtargets");
				return new PipeRunResult(finalForward, outputStream.getResponse());
			} catch (StreamingException e) {
				throw new PipeRunException(this,"Streaming exception", e);
			}
		} else {
			log.debug(getLogPrefix(session)+"cannot stream, streamingActive ["+isStreamingActive()+"]");
			return doPipe(input, session, null);
		}
	}

	
	/**
	 * return a list of down stream {@link IOutputStreamingSupport}s, that can be used to set up the chain of targets.
	 */
	public List<IOutputStreamingSupport> getStreamTargets() {
		if (streamTargets==null) {
			PipeLine pipeline=getPipeLine();
			if (!isStreamingActive()) {
				log.debug("Cannot stream, streamingActive ["+isStreamingActive()+"]");
				return null;
			}
			if (pipeline==null) {
				log.debug("Cannot stream, no pipeline");
				return null;
			}
			List<IOutputStreamingSupport> myStreamTargets = new ArrayList<IOutputStreamingSupport>();
			PipeForward forward=getForward();
			while (true) {
				if (forward==null) {
					log.debug("Forward is null, streaming stops");
					break;
				}
				String forwardPath=forward.getPath();
				if (forwardPath==null) {
					log.debug("ForwardPath is null, streaming stops");
					break;
				}
				IPipe nextPipe=pipeline.getPipe(forwardPath);
				if (nextPipe==null) {
					log.debug("Pipeline ends here, streaming stops");
					break;
				}
				if (!(nextPipe instanceof IOutputStreamingSupport)) {
					log.debug("nextPipe ["+forwardPath+"] type ["+nextPipe.getClass().getSimpleName()+"] does not support streaming");
					break;
				}
				
				if (nextPipe instanceof StreamingPipe && !((StreamingPipe)nextPipe).isStreamingActive()) {
					log.debug("nextPipe ["+forwardPath+"] has not activated streaming");
					break;
				}
				IOutputStreamingSupport streamTarget = (IOutputStreamingSupport)nextPipe;
				if (!streamTarget.canProvideOutputStream()) {
					log.debug("nextPipe ["+forwardPath+"] cannot provide outputstream");
					break;
				}
				log.debug("adding nextPipe ["+forwardPath+"] to list of stream targets");
				myStreamTargets.add(streamTarget);
				
				if (nextPipe instanceof StreamingPipe) {
					log.debug("attach streamTargets of nextPipe ["+forwardPath+"]");
					myStreamTargets.addAll(((StreamingPipe)nextPipe).getStreamTargets());
					break;
				}
				// if next pipe is not a StreamingPipe, than add it's streaming targets manually
				if (!streamTarget.requiresOutputStream() || !(streamTarget instanceof FixedForwardPipe)) {
					log.debug("nextPipe ["+forwardPath+"] cannot provide stream to its successor");
					break;
				}
				forward = ((FixedForwardPipe)nextPipe).getForward();
			}
			streamTargets=myStreamTargets;
			
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

	@IbisDoc({"controls whether output streaming is used. Can be used to switch streaming off for debugging purposes","set by appconstant streaming.auto"})
	public void setStreamingActive(boolean streamingActive) {
		this.streamingActive = streamingActive;
	}
	public boolean isStreamingActive() {
		return streamingActive;
	}


	// TODO: Arrange that this name is used in Ladybug and statistics display. Avoid changing the actual name, that will cause the pipe not being found anymore. 
	public String geDisplayName() {
		if (streamTargets!=null) { // use cached copy of streamTargets, do not generate a new one; it might be too early to do that.
			String result = "Stream: "+super.getName();
			for(IOutputStreamingSupport step:streamTargets) {
				if (step instanceof INamedObject) {
					result+="->"+((INamedObject)step).getName();
				} else {
					result+="->"+step.getClass().getSimpleName();
				}
			}
			return result;
		}
		return super.getName();
	}


}
