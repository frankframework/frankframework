package nl.nn.adapterframework.core;

import java.io.OutputStream;

import nl.nn.adapterframework.doc.IbisDoc;

public interface IOutputStreamConsumer {

	/**
	 * When set, the pipe will not return a String output, but will write its output to the {@link OutputStream} provided in the session variable.
	 */
	@IbisDoc({"When set, no String output will be returned, but the output will be written to the {@link OutputStream} provided in the session variable. The pipe will return its input message", ""})
	public void setStreamToSessionKey(String streamToSessionKey);
	public String getStreamToSessionKey();
	
	/**
	 * When this returns true AND an OutputStream is provided in the session variable referred to by getStreamToSessionKey(), 
	 * then doPipe() should write to this OutputStream, what otherwise would be the result of the pipe. When streaming optimization is used,
	 * the actual result of the doPipe will be ignored then. 
	 */
	public boolean canStreamToOutputStreamAndHappyForwardAlreadyKnown();
	public PipeForward getForwardToOutputStreamProvider(); 
	
}
