/*
 * $Log: RunStateEnquiring.java,v $
 * Revision 1.1  2005-10-27 08:42:14  europe\L190409
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
public interface RunStateEnquiring {
	public static final String version="$RCSfile: RunStateEnquiring.java,v $ $Revision: 1.1 $ $Date: 2005-10-27 08:42:14 $";

	public void SetRunStateEnquirer(RunStateEnquirer enquirer);
}
