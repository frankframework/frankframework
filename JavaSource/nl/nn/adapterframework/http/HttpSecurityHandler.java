/*
 * $Log: HttpSecurityHandler.java,v $
 * Revision 1.3  2011-11-30 13:52:01  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/07/05 12:58:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SecurityHandlers
 *
 */
package nl.nn.adapterframework.http;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Securityhandler that delegates its implementation to the corresponding methods in the HttpServlet.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class HttpSecurityHandler implements ISecurityHandler {
	public static final String version = "$RCSfile: HttpSecurityHandler.java,v $ $Revision: 1.3 $ $Date: 2011-11-30 13:52:01 $";

	HttpServletRequest request;
	
	public HttpSecurityHandler(HttpServletRequest request) {
		super();
		this.request=request;
	}

	public boolean isUserInRole(String role, PipeLineSession session) {
		return request.isUserInRole(role);
	}

	public Principal getPrincipal(PipeLineSession session){
		return request.getUserPrincipal();
	}

}
