/*
 * $Log: RunStateEnquiring.java,v $
 * Revision 1.3  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/10/27 08:42:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	public static final String version="$RCSfile: RunStateEnquiring.java,v $ $Revision: 1.3 $ $Date: 2011-11-30 13:51:49 $";

	public void SetRunStateEnquirer(RunStateEnquirer enquirer);
}
