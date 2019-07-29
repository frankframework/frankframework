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
	
}
