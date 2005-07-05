/*
 * $Log: ISecurityHandler.java,v $
 * Revision 1.1  2005-07-05 09:57:00  europe\L190409
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
