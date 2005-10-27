/*
 * $Log: RunStateEnquirer.java,v $
 * Revision 1.1  2005-10-27 08:42:15  europe\L190409
 * introduced RunStateEnquiries
 *
 */
package nl.nn.adapterframework.util;

/**
 * Interface to support enquiries about the run state.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public interface RunStateEnquirer {
	public static final String version="$RCSfile: RunStateEnquirer.java,v $ $Revision: 1.1 $ $Date: 2005-10-27 08:42:15 $";

	public RunStateEnum getRunState();
	public boolean isInState(RunStateEnum state);	
}
