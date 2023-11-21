/*
   Copyright 2017-2021 WeAreFrank!

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

package nl.nn.adapterframework.http.rest;

import java.io.IOException;
import java.rmi.server.UID;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.ServletSecurity;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;

/**
* Pipe to manage the ApiPrincipal handling
*
* @author Niels Meijer
*
*/
public class ApiPrincipalPipe extends FixedForwardPipe {

	private String action = null;
	List<String> allowedActions = Arrays.asList("get", "set", "create", "remove");
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

		if(getAction().equals("get")) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(PipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, "unable to locate ApiPrincipal");

			return new PipeRunResult(getSuccessForward(), userPrincipal.getData());
		}
		if(getAction().equals("set")) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(PipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, "unable to locate ApiPrincipal");

			userPrincipal.setData(input);
			cache.put(userPrincipal.getToken(), userPrincipal, authTTL);

			return new PipeRunResult(getSuccessForward(), "");
		}
		if(getAction().equals("create")) {
			//TODO type of token? (jwt, saml)
			String uidString = (new UID()).toString();
			SecureRandom random = new SecureRandom();
			String token = random.nextInt() + uidString + Integer.toHexString(uidString.hashCode()) + random.nextInt(8);

			ApiPrincipal userPrincipal = new ApiPrincipal(authTTL);
			userPrincipal.setData(input);
			userPrincipal.setToken(token);
			if(getAuthenticationMethod().equals("cookie")) {
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
		if(getAction().equals("remove")) {
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
		if(method.equalsIgnoreCase("header")) {
			this.authenticationMethod = "header";
		}
		else if(method.equalsIgnoreCase("cookie")) {
			this.authenticationMethod = "cookie";
		}
		else
			throw new ConfigurationException("Authentication method not implemented");
	}

	public String getAuthenticationMethod() {
		return authenticationMethod;
	}
}
