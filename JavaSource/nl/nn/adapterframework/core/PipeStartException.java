/*
 * $Log: PipeStartException.java,v $
 * Revision 1.4  2004-03-30 07:29:59  L190409
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception that indicates that the starting of a {@link IPipe Pipe}
 * did not succeed.<br/>

 * @version Id
 * @author Johan Verrips IOS
 * @see nl.nn.adapterframework.pipes.AbstractPipe#start()
 */
public class PipeStartException extends IbisException{
	public static final String version="$Id: PipeStartException.java,v 1.4 2004-03-30 07:29:59 L190409 Exp $";
	
	private String pipeNameInError=null;
/**
 * PipeStartException constructor comment.
 */
public PipeStartException() {
	super();
}
/**
 * PipeStartException constructor comment.
 */
public PipeStartException(String msg) {
	super(msg);
}
public PipeStartException(String msg, Throwable e) {
	super(msg, e);
}
public PipeStartException(Throwable e) {
	super(e);
}
/**
 * The name of the pipe in error.
 * @return java.lang.String Name of the pipe in error
 */
public java.lang.String getPipeNameInError() {
	return pipeNameInError;
}
/**
 * The name of the pipe in error. 
 * @param newPipeNameInError Name of the pipe in error
 */
public void setPipeNameInError(java.lang.String newPipeNameInError) {
	pipeNameInError = newPipeNameInError;
}
}
