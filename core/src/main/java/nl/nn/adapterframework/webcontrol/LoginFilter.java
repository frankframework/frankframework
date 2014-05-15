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
package nl.nn.adapterframework.webcontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

/**
 * Java Servlet Filter as security constraints workaround for non Websphere
 * Application Servers.
 *
 * <p><b>Properties to set:</b>
 * <table border="1">
 * <tr><th>name</th><th>description</th><th>default</th></tr>
 * <tr><td>ldap.auth.mode</td><td>Defines security contraints behaviour. Possible values are:
 *   <table border="1">
 *   <tr><td>None</td><td>no security constraints, all pages are accessible</td></tr>
 *   <tr><td>Simple</td><td>all IbisObserver pages are accessible</td></tr>
 *   <tr><td>Basic</td><td>all IbisObserver pages are accessible, but only when the user has been authenticated</td></tr>
 *   <tr><td>Full</td><td>all IbisObserver and IbisDataAdmin pages are accessible, but only when the user has been authenticated and authorized</td></tr>
 *  </table></td><td>None</td></tr>
 * <tr><td>ldap.auth.url</td><td>URL to the LDAP server</td><td>when <code>otap.stage</code>=PRD then <code>ldaps://cds.insim.biz:636</code>, else <code>ldaps://acceptance.cds.insim.biz:636</code></td></tr>
 * <tr><td>ldap.auth.user.base</td><td>LDAP DN for the username</td><td>uid=%UID%,ou=People,o=ing (where %UID% will be replaced by the entered username)</td></tr>
 * <tr><td>ldap.auth.observer.base</td><td>LDAP DN to authorize user for IbisObserver</td><td>cn=developers,cn=support,ou=IBIS,ou=Services,ou=Groups,o=ing</td></tr>
 * <tr><td>ldap.auth.dataadmin.base</td><td>LDAP DN to authorize user for IbisDataAdmin</td><td>cn=developers,cn=support,ou=IBIS,ou=Services,ou=Groups,o=ing</td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 * @version $Id: LoginFilter.java,v 1.1 2014/05/06 13:21:51 m168309 Exp $
 */

public class LoginFilter implements Filter {
	protected Logger log = LogUtil.getLogger(this);

	protected static final String LDAP_AUTH_MODE_NONE_STR = "None";
	protected static final String LDAP_AUTH_MODE_SIMPLE_STR = "Simple";
	protected static final String LDAP_AUTH_MODE_BASIC_STR = "Basic";
	protected static final String LDAP_AUTH_MODE_FULL_STR = "Full";

	protected static final int LDAP_AUTH_MODE_NONE = 0;
	protected static final int LDAP_AUTH_MODE_SIMPLE = 1;
	protected static final int LDAP_AUTH_MODE_BASIC = 2;
	protected static final int LDAP_AUTH_MODE_FULL = 3;

	protected static final String ldapAuthModes[] = { LDAP_AUTH_MODE_NONE_STR,
			LDAP_AUTH_MODE_SIMPLE_STR, LDAP_AUTH_MODE_BASIC_STR,
			LDAP_AUTH_MODE_FULL_STR };

	protected static final int ldapAuthModeNums[] = { LDAP_AUTH_MODE_NONE,
			LDAP_AUTH_MODE_SIMPLE, LDAP_AUTH_MODE_BASIC, LDAP_AUTH_MODE_FULL };

	protected static final String AUTH_PATH_MODE_OBSERVER = "Observer";
	protected static final String AUTH_PATH_MODE_DATAADMIN = "DataAdmin";

	protected String applicationServerType;
	protected String otapStage;
	protected String instanceName;
	protected int ldapAuthModeNum;
	protected String ldapAuthUrl;
	protected String ldapAuthUserBase;
	protected String ldapAuthObserverBase;
	protected String ldapAuthDataAdminBase;
	protected final List<String> allowedExtentions = new ArrayList<String>();
	protected final List<String> allowedObserverPaths = new ArrayList<String>();
	protected final List<String> allowedDataAdminPaths = new ArrayList<String>();

	public void init(FilterConfig filterConfig) throws ServletException {
		applicationServerType = AppConstants.getInstance().getString(
				IbisContext.APPLICATION_SERVER_TYPE, "");
		otapStage = AppConstants.getInstance()
				.getResolvedProperty("otap.stage");
		instanceName = AppConstants.getInstance().getResolvedProperty(
				"instance.name");

		String ldapAuthMode = AppConstants.getInstance().getString(
				"ldap.auth.mode", LDAP_AUTH_MODE_NONE_STR);
		ldapAuthModeNum = getLdapAuthModeNum(ldapAuthMode);
		if (ldapAuthModeNum < 0) {
			log.warn("Unknown ldapAuthMode [" + ldapAuthMode + "], will use ["
					+ LDAP_AUTH_MODE_NONE_STR + "]");
			ldapAuthModeNum = 0;
		}

		if (ldapAuthModeNum >= LDAP_AUTH_MODE_SIMPLE) {
			String allowedExtentionsString = filterConfig
					.getInitParameter("allowedExtentions");
			if (allowedExtentionsString != null) {
				allowedExtentions.addAll(Arrays.asList(allowedExtentionsString
						.split("\\s+")));
			}

			String allowedObserverPathsString = filterConfig
					.getInitParameter("allowedObserverPaths");
			if (allowedObserverPathsString != null) {
				allowedObserverPaths.addAll(Arrays
						.asList(allowedObserverPathsString.split("\\s+")));
			}

			String allowedDataAdminPathsString = filterConfig
					.getInitParameter("allowedDataAdminPaths");
			if (allowedDataAdminPathsString != null) {
				allowedDataAdminPaths.addAll(Arrays
						.asList(allowedDataAdminPathsString.split("\\s+")));
			}

			if (ldapAuthModeNum >= LDAP_AUTH_MODE_BASIC) {
				ldapAuthUrl = AppConstants.getInstance().getResolvedProperty(
						"ldap.auth.url");
				if (ldapAuthUrl == null) {
					String ldapAuthUrlProp = "ldap.auth."
							+ otapStage.toLowerCase() + ".url";
					ldapAuthUrl = AppConstants.getInstance()
							.getResolvedProperty(ldapAuthUrlProp);
				}

				ldapAuthUserBase = AppConstants.getInstance()
						.getResolvedProperty("ldap.auth.user.base");
				if (ldapAuthModeNum >= LDAP_AUTH_MODE_FULL) {
					ldapAuthObserverBase = AppConstants.getInstance()
							.getResolvedProperty("ldap.auth.observer.base");
					if (ldapAuthObserverBase == null) {
						throw new ServletException(
								"property [ldap.auth.observer.base] should be set");
					}
					ldapAuthDataAdminBase = AppConstants.getInstance()
							.getResolvedProperty("ldap.auth.dataadmin.base");
					if (ldapAuthDataAdminBase == null) {
						throw new ServletException(
								"property [ldap.auth.dataadmin.base] should be set");
					}
				}
			}
		}
	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) servletRequest;
		HttpServletResponse res = (HttpServletResponse) servletResponse;

		if (ldapAuthModeNum >= LDAP_AUTH_MODE_SIMPLE) {
			String path = req.getServletPath();
			if (hasAllowedExtension(path)) {
				filterChain.doFilter(servletRequest, servletResponse);
			} else {
				boolean allowedObserverPath = isAllowedObserverPath(path);
				boolean allowedDataAdminPath = false;
				String authorizePathMode = null;
				if (ldapAuthModeNum >= LDAP_AUTH_MODE_FULL) {
					allowedDataAdminPath = isAllowedDataAdminPath(path);
					if (allowedObserverPath) {
						authorizePathMode = AUTH_PATH_MODE_OBSERVER;
					} else if (allowedDataAdminPath) {
						authorizePathMode = AUTH_PATH_MODE_DATAADMIN;
					}
				}
				if (allowedObserverPath || allowedDataAdminPath) {
					if (ldapAuthModeNum >= LDAP_AUTH_MODE_BASIC) {
						String authenticated = askUsername(req, res,
								authorizePathMode);
						if (authenticated == null) {
							res.getWriter().write(
									"<html>Not Allowed (" + path + ")</html>");
						} else {
							filterChain.doFilter(servletRequest,
									servletResponse);
						}
					} else {
						filterChain.doFilter(servletRequest, servletResponse);
					}
				} else {
					res.getWriter().write(
							"<html>Not Allowed (" + path + ")</html>");
				}
			}
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private boolean hasAllowedExtension(String path) {
		for (String allowedExtension : allowedExtentions) {
			if (FileUtils.extensionEqualsIgnoreCase(path, allowedExtension)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAllowedObserverPath(String path) {
		for (String allowedPath : allowedObserverPaths) {
			if (path.equals(allowedPath)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAllowedDataAdminPath(String path) {
		for (String allowedPath : allowedDataAdminPaths) {
			if (path.equals(allowedPath)) {
				return true;
			}
		}
		return false;
	}

	private String askUsername(HttpServletRequest req, HttpServletResponse res,
			String authorizePathMode) {
		String username = null;
		String header = req.getHeader("Authorization");
		if (header == null) {
			log.debug("no Authorization header found yet, getting credentials");
		} else {
			String usernpassw = new String(Base64.decodeBase64(header
					.substring(6)));
			if (usernpassw != null) {
				String uname = usernpassw.substring(0, usernpassw.indexOf(":"));
				String pword = usernpassw
						.substring(usernpassw.indexOf(":") + 1);
				if (checkUsernamePassword(uname, pword, authorizePathMode)) {
					username = uname;
				}
			}
		}
		if (header == null || username == null) {
			res.setHeader("WWW-Authenticate", "BASIC realm=\"" + instanceName
					+ "\"");
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
		return username;
	}

	private boolean checkUsernamePassword(String username, String password,
			String authorizePathMode) {
		String dnUser = Misc.replace(ldapAuthUserBase, "%UID%", username);

		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapAuthUrl);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, dnUser);
		env.put(Context.SECURITY_CREDENTIALS, password);

		DirContext ctx = null;
		try {
			try {
				ctx = new InitialDirContext(env);
			} catch (CommunicationException e) {
				log.info("cannot create constructor for DirContext ("
						+ e.getMessage()
						+ "], will try again with dummy SocketFactory");
				env.put("java.naming.ldap.factory.socket",
						DummySSLSocketFactory.class.getName());
				ctx = new InitialLdapContext(env, null);
			}

			if (authorizePathMode == null) {
				return true;
			} else {
				if (authorizePathMode.equals(AUTH_PATH_MODE_OBSERVER)) {
					if (isMemberOf(ctx, dnUser, ldapAuthObserverBase)) {
						return true;
					}
					if (isMemberOf(ctx, dnUser, ldapAuthDataAdminBase)) {
						return true;
					}
				}
				if (authorizePathMode.equals(AUTH_PATH_MODE_DATAADMIN)) {
					if (isMemberOf(ctx, dnUser, ldapAuthDataAdminBase)) {
						return true;
					}
				}
			}
		} catch (AuthenticationException e) {
			return false;
		} catch (Exception e) {
			log.warn("LoginFilter caught Exception", e);
			return false;
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (Exception e) {
					log.warn("LoginFilter caught Exception", e);
				}
			}
		}
		return false;
	}

	private boolean isMemberOf(DirContext ctx, String dnUser, String dnGroup)
			throws NamingException {
		DirContext lookedContext = (DirContext) (ctx.lookup(dnGroup));
		Attribute attrs = lookedContext.getAttributes("").get("member");
		for (int i = 0; i < attrs.size(); i++) {
			String foundMember = (String) attrs.get(i);
			if (foundMember.equalsIgnoreCase(dnUser)) {
				return true;
			}
		}
		return false;
	}

	private static int getLdapAuthModeNum(String ldapAuthMode) {
		int i = ldapAuthModes.length - 1;
		while (i >= 0 && !ldapAuthModes[i].equalsIgnoreCase(ldapAuthMode))
			i--; // try next
		if (i >= 0) {
			return ldapAuthModeNums[i];
		} else {
			return i;
		}
	}

	public void destroy() {
	}
}
