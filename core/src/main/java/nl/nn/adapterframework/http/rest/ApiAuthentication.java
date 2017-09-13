package nl.nn.adapterframework.http.rest;

import nl.nn.adapterframework.core.ListenerException;

public class ApiAuthentication {

	public static final String BASIC = "BASIC";
	public static final String TOKEN = "TOKEN";

	private IApiCache cache = ApiCacheManager.getInstance();
	private String authenticationMethod = null;
	private String uriPattern = null;
	private ApiPrincipal userPrincipal = null;

	ApiAuthentication(ApiListener listener) {
		this.uriPattern = listener.getUriPattern();
		this.authenticationMethod = listener.getAuthenticationMethod();
	}

	public ApiPrincipal findUser(String authorizationHeader) throws ListenerException {
		if(authenticationMethod.equalsIgnoreCase(BASIC) && authorizationHeader.startsWith("BASIC")) {
			//TODO elke keer checken of ingelogd?
			String key = authorizationHeader.substring(6);
			System.out.println(key);

			userPrincipal = (ApiPrincipal) cache.get(key);
		}
		else if(authenticationMethod.equalsIgnoreCase(TOKEN) && authorizationHeader.startsWith("token")) {
			String key = authorizationHeader.substring(6);
			System.out.println(key);

			userPrincipal = (ApiPrincipal) cache.get(key);
		}
		else {
			throw new ListenerException("AuthenticationMethod ["+authenticationMethod+"] not found!");
		}

		return userPrincipal;
	}

	public boolean isMethodAllowed(String method) {
		return isMethodAllowed(method, userPrincipal);
	}

	public boolean isMethodAllowed(String method, ApiPrincipal userPrincipal) {
		if(uriPattern.equalsIgnoreCase("companies/a/b"))
			return false;
		return true;
	}

}
