/*
   Copyright 2017-2021, 2024 WeAreFrank!

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

package org.frankframework.http.rest;

import java.io.IOException;
import java.rmi.server.UID;
import java.util.List;

import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.lifecycle.ServletManager;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.UUIDUtil;

/**
* Pipe to manage the ApiPrincipal handling
*
* @author Niels Meijer
*
*/
public class ApiPrincipalPipe extends FixedForwardPipe {

	private String action = null;
	List<String> allowedActions = List.of("get", "set", "create", "remove");
	private IApiCache cache = null;
	private final int authTTL = AppConstants.getInstance().getInt("api.auth.token-ttl", 60 * 60 * 24 * 7); //Defaults to 7 days
	private String authenticationMethod = "header";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		String action = getAction();
		if (action==null) {
			throw new ConfigurationException("action must be set");
		}
		if (!allowedActions.contains(action)) {
			throw new ConfigurationException("illegal value for action ["+action+"], must be one of " + allowedActions.toString());
		}

		cache = ApiCacheManager.getInstance();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (message==null) {
			throw new PipeRunException(this, "got null input");
		}
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}

		if("get".equals(getAction())) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(PipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, "unable to locate ApiPrincipal");

			return new PipeRunResult(getSuccessForward(), userPrincipal.getData());
		}
		if("set".equals(getAction())) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(PipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, "unable to locate ApiPrincipal");

			userPrincipal.setData(input);
			cache.put(userPrincipal.getToken(), userPrincipal, authTTL);

			return new PipeRunResult(getSuccessForward(), "");
		}
		if("create".equals(getAction())) {
			//TODO type of token? (jwt, saml)
			String uidString = (new UID()).toString();
			String token = UUIDUtil.RANDOM.nextInt() + uidString + Integer.toHexString(uidString.hashCode()) + UUIDUtil.RANDOM.nextInt(8);

			ApiPrincipal userPrincipal = new ApiPrincipal(authTTL);
			userPrincipal.setData(input);
			userPrincipal.setToken(token);
			if("cookie".equals(getAuthenticationMethod())) {
				Cookie cookie = new Cookie(ApiListenerServlet.AUTHENTICATION_COOKIE_NAME, token);
				cookie.setPath("/");
				cookie.setMaxAge(authTTL);
				cookie.setHttpOnly(true);

				ServletSecurity.TransportGuarantee currentGuarantee = ServletManager.getTransportGuarantee("servlet.ApiListenerServlet.transportGuarantee");
				cookie.setSecure(currentGuarantee == ServletSecurity.TransportGuarantee.CONFIDENTIAL);

				HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
				response.addCookie(cookie);
			}

			cache.put(token, userPrincipal, authTTL);

			return new PipeRunResult(getSuccessForward(), token);
		}
		if("remove".equals(getAction())) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(PipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, "unable to locate ApiPrincipal");

			cache.remove(userPrincipal.getToken());
			return new PipeRunResult(getSuccessForward(), "");
		}

		return new PipeRunResult(findForward(PipeForward.EXCEPTION_FORWARD_NAME), "unable to execute action ["+getAction()+"]");
	}

	public void setAction(String string) {
		action = string.toLowerCase();
	}

	public String getAction() {
		return action;
	}

	public void setAuthenticationMethod(String method) throws ConfigurationException {
		if("header".equalsIgnoreCase(method)) {
			this.authenticationMethod = "header";
		}
		else if("cookie".equalsIgnoreCase(method)) {
			this.authenticationMethod = "cookie";
		}
		else
			throw new ConfigurationException("Authentication method not implemented");
	}

	public String getAuthenticationMethod() {
		return authenticationMethod;
	}
}
