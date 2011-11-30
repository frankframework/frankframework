/*
 * $Log: ISecurityHandler.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/07/05 09:57:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of ISecurityHandler
 *
 */
package nl.nn.adapterframework.core;

import java.security.Principal;

import org.apache.commons.lang.NotImplementedException;

/**
 * Defines behaviour that can be used to assert identity of callers of a pipeline.
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 * @version Id
 */
public interface ISecurityHandler {
	
	public boolean isUserInRole(String role, PipeLineSession session) throws NotImplementedException;
	public Principal getPrincipal(PipeLineSession session) throws NotImplementedException;

}
