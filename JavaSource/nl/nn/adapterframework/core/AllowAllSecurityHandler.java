/*
 * $Log: AllowAllSecurityHandler.java,v $
 * Revision 1.1  2005-07-05 13:31:20  europe\L190409
 * introduction of SecurityHandlers
 *
 */
package nl.nn.adapterframework.core;

import java.security.Principal;

import org.apache.commons.lang.NotImplementedException;

/**
 * Security handler that declares that each role is valid. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class AllowAllSecurityHandler implements ISecurityHandler {
	public static final String version = "$RCSfile: AllowAllSecurityHandler.java,v $ $Revision: 1.1 $ $Date: 2005-07-05 13:31:20 $";

	public boolean isUserInRole(String role, PipeLineSession session) {
		return true;
	}

	public Principal getPrincipal(PipeLineSession session) throws NotImplementedException {
		throw new NotImplementedException("no default user available");
	}

}
