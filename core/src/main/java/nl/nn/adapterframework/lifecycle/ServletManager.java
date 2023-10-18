/*
   Copyright 2019-2020 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

import lombok.Setter;
import nl.nn.adapterframework.lifecycle.servlets.AuthenticationType;
import nl.nn.adapterframework.lifecycle.servlets.IAuthenticator;
import nl.nn.adapterframework.lifecycle.servlets.JeeAuthenticator;
import nl.nn.adapterframework.lifecycle.servlets.SecuritySettings;
import nl.nn.adapterframework.lifecycle.servlets.ServletAuthenticatorBase;
import nl.nn.adapterframework.lifecycle.servlets.ServletConfiguration;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.StringUtil;

/**
 * <p>
 * Enables the use of programmatically adding servlets to the given ServletContext.<br/>
 * Run during the ApplicationServers {@link ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent) contextInitialized} phase, before starting servlets.
 * This ensures that all (dynamic) {@link DynamicRegistration.Servlet servlets} are created, before servlets are being created.
 * This in turn avoids a ConcurrentModificationException if this where to be done during a {@link javax.servlet.http.HttpServlet servlet} init phase.
 * </p>
 * <p>
 * When <code>dtap.stage</code> is set to LOC, the default behaviour of each servlet is not-secured (no authentication) on HTTP.<br/>
 * When <code>dtap.stage</code> is NOT set to LOC, the default behaviour of each servlet is secured (authentication enforced) on HTTPS.
 * </p>
 * <p>
 * To change this behaviour the following properties can be used;
 * <code>servlet.servlet-name.transportGuarantee</code> - forces HTTPS when set to CONFIDENTIAL, or HTTP when set to NONE<br/>
 * <code>servlet.servlet-name.securityRoles</code> - use the default IBIS roles or create your own<br/>
 * <code>servlet.servlet-name.urlMapping</code> - path the servlet listens to<br/>
 * <code>servlet.servlet-name.loadOnStartup</code> - automatically load or use lazy-loading (affects application startup time)<br/>
 * </p>
 * NOTE:
 * Both CONTAINER and NONE are non-configurable default authenticators.
 *
 * @author Niels Meijer
 *
 */
public class ServletManager implements ApplicationContextAware, InitializingBean {

	private ServletContext servletContext = null;
	private List<String> registeredRoles = new ArrayList<>();
	private Logger log = LogUtil.getLogger(this);
	private Map<String, ServletConfiguration> servlets = new HashMap<>();
	private Map<String, IAuthenticator> authenticators = new HashMap<>();
	private @Setter ApplicationContext applicationContext;
	private boolean allowUnsecureOptionsRequest = false;

	protected ServletContext getServletContext() {
		return servletContext;
	}

	public ServletManager(ServletContext servletContext) {
		this.servletContext = servletContext;

		//Add the default IBIS roles
		registeredRoles.addAll(ServletAuthenticatorBase.DEFAULT_IBIS_ROLES);
	}

	@Override // After initialization but before other servlets are wired
	public void afterPropertiesSet() throws Exception {
		Environment env = applicationContext.getEnvironment();
		SecuritySettings.setupDefaultSecuritySettings(env);
		allowUnsecureOptionsRequest = env.getProperty(ServletAuthenticatorBase.ALLOW_OPTIONS_REQUESTS_KEY, boolean.class, false);

		addDefaultAuthenticator(AuthenticationType.CONTAINER);
		addDefaultAuthenticator(AuthenticationType.NONE);

		resolveAuthenticators();
		log.info("found Authenticators {}", authenticators::values);
	}

	private void addDefaultAuthenticator(AuthenticationType type) {
		if(!authenticators.containsKey(type.name())) {
			Class<? extends IAuthenticator> clazz = type.getAuthenticator();
			IAuthenticator authenticator = SpringUtils.createBean(applicationContext, clazz);
			authenticators.put(type.name(), authenticator);
		}
	}

	public void startAuthenticators() {
		log.info("starting Authenticators {}", authenticators::values);

		for(IAuthenticator authenticator : authenticators.values()) {
			authenticator.build();
		}
	}

	private void resolveAuthenticators() {
		AppConstants.getInstance()
				.getListProperty("application.security.http.authenticators")
				.forEach(this::resolveAndConfigureAuthenticator);
	}

	private void resolveAndConfigureAuthenticator(String authenticatorName) {
		AppConstants appConstants = AppConstants.getInstance();
		String properyPrefix = "application.security.http.authenticators."+authenticatorName+".";
		String type = AppConstants.getInstance().getProperty(properyPrefix+"type");
		AuthenticationType auth = null;
		try {
			auth = EnumUtils.parse(AuthenticationType.class, type);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("invalid authenticator type", e);
		}
		Class<? extends IAuthenticator> clazz = auth.getAuthenticator();
		IAuthenticator authenticator = SpringUtils.createBean(applicationContext, clazz);

		for(Method method: clazz.getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			String setter = StringUtil.lcFirst(method.getName().substring(3));
			String value = appConstants.getProperty(properyPrefix+setter);
			if(StringUtils.isEmpty(value))
				continue;

			ClassUtils.invokeSetter(authenticator, method, value);
		}

		authenticators.put(authenticatorName, authenticator);
	}

	/**
	 * Register a new role
	 * @param roleNames String or multiple strings of roleNames to register
	 */
	public void declareRoles(String... roleNames) {
		for (String role : roleNames) {
			if(StringUtils.isNotEmpty(role) && !registeredRoles.contains(role)) {
				registeredRoles.add(role);

				getServletContext().declareRoles(role);
				log.info("declared role [{}]", role);
			}
		}
	}

	public void register(ServletConfiguration config) {
		if(!config.isEnabled()) {
			log.info("skip instantiating servlet name [{}] not enabled", config::getName);
			return;
		}

		registerServlet(config);

		String authenticatorName = config.getAuthenticatorName();
		if(StringUtils.isNotEmpty(authenticatorName)) {
			IAuthenticator authenticator = authenticators.get(authenticatorName);
			if(authenticator == null) {
				throw new IllegalStateException("unable to configure servlet security, authenticator ["+authenticatorName+"] does not exist");
			}
			authenticator.registerServlet(config);
		}
	}

	private void registerServlet(ServletConfiguration config) {
		String servletName = config.getName();
		if(servlets.containsKey(servletName)) {
			throw new IllegalArgumentException("unable to instantiate servlet ["+servletName+"], servlet name must be unique");
		}

		log.info("instantiating IbisInitializer servlet name [{}] servletClass [{}]", servletName, config.getServlet());

		ServletRegistration.Dynamic serv = getServletContext().addServlet(servletName, config.getServlet());

		serv.setLoadOnStartup(config.getLoadOnStartup());
		serv.addMapping(getEndpoints(config.getUrlMapping()));
		serv.setServletSecurity(getServletSecurity(config));

		if(!config.getInitParameters().isEmpty()) {
			//Manually loop through the map as serv.setInitParameters will fail all parameters even if only 1 fails...
			for(Entry<String, String> entry : config.getInitParameters().entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if(!serv.setInitParameter(key, value)) {
					log("unable to set init-parameter ["+key+"] with value ["+value+"] for servlet ["+servletName+"]", Level.ERROR);
				}
			}
		}

		servlets.put(servletName, config);
		logServletInfo(serv, config);
	}

	// Remove all endpoint excludes
	private String[] getEndpoints(List<String> list) {
		return list.stream()
				.filter(e -> e.charAt(0) != '!')
				.toArray(String[]::new);
	}

	private void logServletInfo(Dynamic serv, ServletConfiguration config) {
		StringBuilder builder = new StringBuilder("registered");
		builder.append(" servlet ["+serv.getName()+"]");
		builder.append(" configuration ");
		builder.append(config);

		getServletContext().log(builder.toString());

		if(log.isDebugEnabled()) builder.append(" class ["+serv.getClassName()+"]");
		log.info(builder::toString);
	}

	private ServletSecurityElement getServletSecurity(ServletConfiguration config) {
		String[] roles = new String[0];
		if(config.isAuthenticationEnabled() && authenticators.get(config.getAuthenticatorName()) instanceof JeeAuthenticator) {// Only add roles when using  Container Based Authentication
			roles = config.getSecurityRoles().toArray(new String[0]);
			declareRoles(roles);
		}
		HttpConstraintElement httpConstraintElement = new HttpConstraintElement(config.getTransportGuarantee(), roles);

		List<HttpMethodConstraintElement> methodConstraints = new ArrayList<>();
		if(allowUnsecureOptionsRequest) {
			methodConstraints.add(new HttpMethodConstraintElement("OPTIONS"));
		}
		return new ServletSecurityElement(httpConstraintElement, methodConstraints);
	}

	private void log(String msg, Level level) {
		if(log.isInfoEnabled() )
			getServletContext().log(msg);

		log.log(level, msg);
	}

	public ServletConfiguration getServlet(String name) {
		return servlets.get(name);
	}

	public Collection<ServletConfiguration> getServlets() {
		return Collections.unmodifiableCollection(servlets.values());
	}

	public static ServletSecurity.TransportGuarantee getTransportGuarantee(String propertyName) {
		AppConstants appConstants = AppConstants.getInstance();
		String constraintType = appConstants.getString(propertyName, null);
		if (StringUtils.isNotEmpty(constraintType)) {
			return EnumUtils.parse(TransportGuarantee.class, constraintType);
		}
		return SecuritySettings.getDefaultTransportGuarantee();
	}
}
