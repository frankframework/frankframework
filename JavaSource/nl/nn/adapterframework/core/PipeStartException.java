package nl.nn.adapterframework.core;

/**
 * Exception that indicates that the starting of a {@link IPipe Pipe}
 * did not succeed.<br/>

 * @see nl.nn.adapterframework.pipes.AbstractPipe#start()
 * @author Johan Verrips IOS
 */
public class PipeStartException extends IbisException{
	public static final String version="$Id: PipeStartException.java,v 1.1 2004-02-04 08:36:12 a1909356#db2admin Exp $";
	
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
