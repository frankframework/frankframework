package nl.nn.adapterframework.core;

/**
 * The PipeLineExit, that represents a terminator of the PipeLine, provides a placeholder
 * for a path (corresponding to a pipeforward) and a state (that is returned to the receiver). 
 * </p>
 * <p>$Id: PipeLineExit.java,v 1.2 2004-02-04 10:01:57 a1909356#db2admin Exp $</p>
 * @author Johan Verrips
 */
public class PipeLineExit {
	public static final String version="$Id: PipeLineExit.java,v 1.2 2004-02-04 10:01:57 a1909356#db2admin Exp $";
	
	private String path;
	private String state;
/**
 * PipeLineExit constructor comment.
 */
public PipeLineExit() {
	super();
}
/**
 * Insert the method's description here.
 * Creation date: (06-06-2003 8:34:28)
 * @return java.lang.String
 */
public java.lang.String getPath() {
	return path;
}
/**
 * Insert the method's description here.
 * Creation date: (06-06-2003 8:34:28)
 * @return java.lang.String
 */
public java.lang.String getState() {
	return state;
}
/**
 * Insert the method's description here.
 * Creation date: (06-06-2003 8:34:28)
 * @param newPath java.lang.String
 */
public void setPath(java.lang.String newPath) {
	path = newPath;
}
/**
 * Insert the method's description here.
 * Creation date: (06-06-2003 8:34:28)
 * @param newState java.lang.String
 */
public void setState(java.lang.String newState) {
	state = newState;
}
}
