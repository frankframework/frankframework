/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.http;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;

/**
 * Securityhandler that delegates its implementation to the corresponding methods in the HttpServlet.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version $Id$
 */
public class HttpSecurityHandler implements ISecurityHandler {
	public static final String version = "$RCSfile: HttpSecurityHandler.java,v $ $Revision: 1.4 $ $Date: 2012-06-01 10:52:50 $";

	HttpServletRequest request;
	
	public HttpSecurityHandler(HttpServletRequest request) {
		super();
		this.request=request;
	}

	public boolean isUserInRole(String role, IPipeLineSession session) {
		return request.isUserInRole(role);
	}

	public Principal getPrincipal(IPipeLineSession session){
		return request.getUserPrincipal();
	}

}
