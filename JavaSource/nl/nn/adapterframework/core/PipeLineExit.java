/*
 * $Log: PipeLineExit.java,v $
 * Revision 1.4  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * The PipeLineExit, that represents a terminator of the PipeLine, provides a placeholder
 * for a path (corresponding to a pipeforward) and a state (that is returned to the receiver). 
 * </p>
 * @version Id
 * @author Johan Verrips
 */
public class PipeLineExit {
	public static final String version="$Id: PipeLineExit.java,v 1.4 2004-03-30 07:29:54 L190409 Exp $";
	
	private String path;
	private String state;

	public String getPath() {
		return path;
	}
	public void setPath(String newPath) {
		path = newPath;
	}
	
	public String getState() {
		return state;
	}
	public void setState(String newState) {
		state = newState;
	}
}
