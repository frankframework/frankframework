package nl.nn.adapterframework.core;

/**
 * The PipeLineResult is a type to store both the
 * result of the PipeLine processing as well as an exit state.
 * <br/>
 * The exit state is returned to the Adapter that hands it to the <code>Receiver</code>,
 * so that the receiver knows whether or not the request was successfully
 * processed, and might -for instance- not commit a received message.
 * <br/>
 * @version Id
 * @author Johan Verrips
 */
public class PipeLineResult {
	public static final String version="$Id: PipeLineResult.java,v 1.3 2004-03-26 10:42:50 NNVZNL01#L180564 Exp $";

	private String result;
	private String state;
	public PipeLineResult() {
		super();
	}
/**
 * Get the result of the pipeline processing
 * Creation date: (06-06-2003 8:25:02)
 * @return java.lang.String
 */
public java.lang.String getResult() {
	return result;
}
/**
 * Get the exit-state of the pipeliness
 * @return java.lang.String
 */
public java.lang.String getState() {
	return state;
}
/**
 * set the result of the PipeLine processing to the specified value.
 * @param newResult java.lang.String
 */
public void setResult(java.lang.String newResult) {
	result = newResult;
}
/**
 * set the state of the pipeline. 
 * @param newState java.lang.String
 */
public void setState(java.lang.String newState) {
	state = newState;
}
	public String toString(){
		return "[result=["+result+"] state=["+state+"] version=["+version+"]]";
	}
}
