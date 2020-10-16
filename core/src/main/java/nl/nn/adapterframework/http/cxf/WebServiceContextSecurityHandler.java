/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.http.cxf;
import java.security.Principal;

import javax.xml.ws.WebServiceContext;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;

/**
 * Securityhandler that delegates its implementation to the corresponding
 * methods in the WebServiceContext.
 * 
 * @author Jaco de Groot
 */
public class WebServiceContextSecurityHandler implements ISecurityHandler {
	WebServiceContext webServiceContext;

	public WebServiceContextSecurityHandler(WebServiceContext webServiceContext) {
		this.webServiceContext = webServiceContext;
	}

	@Override
	public boolean isUserInRole(String role, IPipeLineSession session) {
		return webServiceContext.isUserInRole(role);
	}

	@Override
	public Principal getPrincipal(IPipeLineSession session){
		return webServiceContext.getUserPrincipal();
	}
}
