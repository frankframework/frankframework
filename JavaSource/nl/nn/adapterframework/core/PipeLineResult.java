/*
 * $Log: PipeLineResult.java,v $
 * Revision 1.5  2008-07-14 17:10:00  europe\L190409
 * added serialVersionUID
 *
 * Revision 1.4  2004/03/30 07:29:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

import java.io.Serializable;

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
public class PipeLineResult implements Serializable {
	public static final String version = "$RCSfile: PipeLineResult.java,v $ $Revision: 1.5 $ $Date: 2008-07-14 17:10:00 $";

	static final long serialVersionUID = 1;

	private String result;
	private String state;
	public PipeLineResult() {
		super();
	}
	/**
	 * Get the result of the pipeline processing
	 * @return java.lang.String
	 */
	public String getResult() {
		return result;
	}
	/**
	 * Get the exit-state of the pipeliness
	 * @return java.lang.String
	 */
	public String getState() {
		return state;
	}
	/**
	 * set the result of the PipeLine processing to the specified value.
	 * @param newResult java.lang.String
	 */
	public void setResult(String newResult) {
		result = newResult;
	}
	/**
	 * set the state of the pipeline. 
	 * @param newState java.lang.String
	 */
	public void setState(String newState) {
		state = newState;
	}
	public String toString(){
		return "[result=["+result+"] state=["+state+"] version=["+version+"]]";
	}
}
