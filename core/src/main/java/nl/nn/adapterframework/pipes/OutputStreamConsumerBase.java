package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IOutputStreamConsumerPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.doc.IbisDoc;

public class OutputStreamConsumerBase extends FixedForwardPipe implements IOutputStreamConsumerPipe {

	private String streamToSessionKey;
	

//	@Override
//	public PipeForward getForwardToOutputStreamProvider() {
//		return getForward();
//	}

	/**
	 * When this returns true AND an OutputStream is provided in the session variable referred to by getStreamToSessionKey(), 
	 * then doPipe() MUST write to this OutputStream, what otherwise would be the result of the pipe. When streaming optimization is used,
	 * the actual result of the doPipe will be ignored then. 
	 */
	@Override
	public boolean isStreamingToOutputStreamPossible() {
		return true;
	}

	@IbisDoc({"When set, no String output will be returned, but the output will be written to the {@link OutputStream} provided in the session variable. The pipe will return its input message", ""})
	@Override
	public void setStreamToSessionKey(String streamToSessionKey) {
		this.streamToSessionKey=streamToSessionKey;
	}
	@Override
	public String getStreamToSessionKey() {
		return streamToSessionKey;
	}

}
