/*
 * $Log: PipeRunException.java,v $
 * Revision 1.4  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception thrown when the <code>doPipe()</code> method
 * of a {@link IPipe Pipe} runs in error.
 * @version Id
 * @author  Johan Verrips
 */
public class PipeRunException extends IbisException {
	public static final String version="$Id: PipeRunException.java,v 1.4 2004-03-30 07:29:54 L190409 Exp $";
	
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
 * The pipe in error.
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
