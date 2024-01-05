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
package org.frankframework.http;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.NotImplementedException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SecurityHandlerBase;

/**
 * SecurityHandler that delegates its implementation to the corresponding methods in the HttpServlet.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class HttpSecurityHandler extends SecurityHandlerBase {

	HttpServletRequest request;

	public HttpSecurityHandler(HttpServletRequest request) {
		super();
		this.request=request;
	}

	@Override
	public boolean isUserInAnyRole(List<String> roles, PipeLineSession session) throws NotImplementedException {
		for (String s : roles) {
			if (request.isUserInRole(s)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isUserInRole(String role, PipeLineSession session) {
		return request.isUserInRole(role);
	}

	@Override
	public Principal getPrincipal(PipeLineSession session){
		return request.getUserPrincipal();
	}

}
