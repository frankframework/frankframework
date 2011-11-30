/*
 * $Log: AllowAllSecurityHandler.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/07/05 13:31:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	public static final String version = "$RCSfile: AllowAllSecurityHandler.java,v $ $Revision: 1.3 $ $Date: 2011-11-30 13:51:55 $";

	public boolean isUserInRole(String role, PipeLineSession session) {
		return true;
	}

	public Principal getPrincipal(PipeLineSession session) throws NotImplementedException {
		throw new NotImplementedException("no default user available");
	}

}
