/*
Copyright 2017 Integration Partners

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

import java.rmi.server.UID;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
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
	private int authTTL = AppConstants.getInstance().getInt("api.auth.token-ttl", 60 * 60 * 24 * 7); //Defaults to 7 days
	private String authenticationMethod = "header";

	public void configure() throws ConfigurationException {
		super.configure();

		String action = getAction();
		if (action==null) {
			throw new ConfigurationException(getLogPrefix(null)+"action must be set");
		}
		if (!allowedActions.contains(action)) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for action ["+action+"], must be one of " + allowedActions.toString());
		}

		cache = ApiCacheManager.getInstance();
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this, getLogPrefix(session)+"got null input");
		}
		if (!(input instanceof String)) {
			throw new PipeRunException(this, getLogPrefix(session)+"got an invalid type as input, expected String, got "+ input.getClass().getName());
		}

		if(getAction().equals("get")) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(IPipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, getLogPrefix(session) + "unable to locate ApiPrincipal");

			return new PipeRunResult(getForward(), userPrincipal.getData());
		}
		if(getAction().equals("set")) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(IPipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, getLogPrefix(session) + "unable to locate ApiPrincipal");

			userPrincipal.setData((String) input);
			cache.put(userPrincipal.getToken(), userPrincipal, authTTL);

			return new PipeRunResult(getForward(), "");
		}
		if(getAction().equals("create")) {
			//TODO type of token? (jwt, saml)
			String uidString = (new UID()).toString();
			Random random = new Random();
			String token = random.nextInt() + uidString + Integer.toHexString(uidString.hashCode()) + random.nextInt(8);

			ApiPrincipal userPrincipal = new ApiPrincipal(authTTL);
			userPrincipal.setData((String) input);
			userPrincipal.setToken(token);

			if(getAuthenticationMethod().equals("cookie")) {
				Cookie cookie = new Cookie("authenticationToken", token);
				cookie.setPath("/");
				cookie.setMaxAge(authTTL);
				HttpServletResponse response = (HttpServletResponse) session.get(IPipeLineSession.HTTP_RESPONSE_KEY);
				response.addCookie(cookie);
			}

			cache.put(token, userPrincipal, authTTL);

			return new PipeRunResult(getForward(), token);
		}
		if(getAction().equals("remove")) {
			ApiPrincipal userPrincipal = (ApiPrincipal) session.get(IPipeLineSession.API_PRINCIPAL_KEY);
			if(userPrincipal == null)
				throw new PipeRunException(this, getLogPrefix(session) + "unable to locate ApiPrincipal");

			cache.remove(userPrincipal.getToken());
			return new PipeRunResult(getForward(), "");
		}

		return new PipeRunResult(findForward("exception"), "this is not supposed to happen... like ever!");
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
