package nl.nn.adapterframework.core;

/**
 * Exception thrown when the <code>doPipe()</code> method
 * of a {@link IPipe Pipe} runs in error.
 */
public class PipeRunException extends IbisException {
	public static final String version="$Id: PipeRunException.java,v 1.1 2004-02-04 08:36:12 a1909356#db2admin Exp $";
	
	IPipe pipeInError=null;
public PipeRunException(IPipe pipe, String msg) {
	super(msg);
	setPipeInError(pipe);
}
public PipeRunException(IPipe pipe, String msg, Throwable e) {
	super(msg, e);
	setPipeInError(pipe);
}
/**
 * The name of the pipe in error.
 * @return java.lang.String Name of the pipe in error
 */
public IPipe getPipeInError() {
	return pipeInError;
}
/**
 * The pipe in error. 
 * @param newPipeInError the pipe in error
 */
protected void setPipeInError(IPipe newPipeInError) {
	pipeInError = newPipeInError;
}
}
