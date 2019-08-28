package nl.nn.adapterframework.core;

public interface IOutputStreamConsumerPipe extends IOutputStreamConsumer {

	/**
	 * When this returns true AND an OutputStream is provided in the session variable referred to by getStreamToSessionKey(), 
	 * then doPipe() should write to this OutputStream, what otherwise would be the result of the pipe. When streaming optimization is used,
	 * the actual result of the doPipe will be ignored then. 
	 */
	//public PipeForward getForwardToOutputStreamProvider(); 
	
}
