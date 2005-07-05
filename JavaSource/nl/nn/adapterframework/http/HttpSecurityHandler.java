/*
 * $Log: HttpSecurityHandler.java,v $
 * Revision 1.1  2005-07-05 12:58:32  europe\L190409
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
	public static final String version = "$RCSfile: HttpSecurityHandler.java,v $ $Revision: 1.1 $ $Date: 2005-07-05 12:58:32 $";

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
