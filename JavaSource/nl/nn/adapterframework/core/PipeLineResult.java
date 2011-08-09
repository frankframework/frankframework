/*
 * $Log: PipeLineResult.java,v $
 * Revision 1.6  2011-08-09 07:42:19  L190409
 * simplified toString()
 *
 * Revision 1.5  2008/07/14 17:10:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added serialVersionUID
 *
 * Revision 1.4  2004/03/30 07:29:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
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

	private String result;
	private String state;

	public String toString(){
		return "result=["+result+"] state=["+state+"]";
	}

	/**
	 * Get the result of the pipeline processing
	 * @return java.lang.String
	 */
	public String getResult() {
		return result;
	}
	/**
	 * set the result of the PipeLine processing to the specified value.
	 */
	public void setResult(String newResult) {
		result = newResult;
	}

	/**
	 * Get the exit-state of the pipeline
	 */
	public String getState() {
		return state;
	}
	/**
	 * set the state of the pipeline. 
	 */
	public void setState(String newState) {
		state = newState;
	}
}
