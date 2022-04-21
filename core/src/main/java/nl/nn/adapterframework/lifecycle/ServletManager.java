/*
   Copyright 2019-2020 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.HttpConstraintElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;

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
 * 
 * @author Niels Meijer
 *
 */
public class ServletManager {

	private ServletContext servletContext = null;
	private List<String> registeredRoles = new ArrayList<>();
	private Logger log = LogUtil.getLogger(this);
	private AppConstants appConstants;
	private boolean webSecurityEnabled = true;
	private static TransportGuarantee defaultTransportGuarantee = TransportGuarantee.CONFIDENTIAL;

	protected static final String AUTH_ENABLED_KEY = "application.security.http.authentication";
	protected static final String HTTPS_ENABLED_KEY = "application.security.http.transportGuarantee";

	protected ServletContext getServletContext() {
		return servletContext;
	}

	public ServletManager(ServletContext servletContext) {
		this.servletContext = servletContext;

		//Add the default IBIS roles
		registeredRoles.addAll(Arrays.asList("IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester", "IbisWebService"));

		appConstants = AppConstants.getInstance();
		boolean isDtapStageLoc = "LOC".equalsIgnoreCase(appConstants.getString("dtap.stage", null));
		webSecurityEnabled = appConstants.getBoolean(AUTH_ENABLED_KEY, !isDtapStageLoc);
		String constraintType = appConstants.getString(HTTPS_ENABLED_KEY, null);
		if (StringUtils.isNotEmpty(constraintType)) {
			try {
				defaultTransportGuarantee = EnumUtils.parse(TransportGuarantee.class, constraintType);
			} catch(IllegalArgumentException e) {
				log.error("unable to set TransportGuarantee", e);
			}
		} else if(isDtapStageLoc) {
			defaultTransportGuarantee = TransportGuarantee.NONE;
		}
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
				log.info("declared role ["+role+"]");
			}
		}
	}

	public void register(DynamicRegistration.Servlet servlet) {
		Map<String, String> parameters = null;
		if(servlet instanceof DynamicRegistration.ServletWithParameters)
			parameters = ((DynamicRegistration.ServletWithParameters) servlet).getParameters();

		registerServlet(servlet, parameters);
	}

	public void register(Servlet servletClass, String servletName, String urlMapping) {
		registerServlet(servletClass, servletName, urlMapping, new String[0], -1, null);
	}

	private void registerServlet(DynamicRegistration.Servlet servlet, Map<String, String> parameters) {
		registerServlet(servlet, servlet.getName(), servlet.getUrlMapping(), servlet.getRoles(), servlet.loadOnStartUp(), parameters);
	}

	private void registerServlet(Servlet servlet, String servletName, String urlMapping, String[] roles, int loadOnStartup, Map<String, String> initParameters) {
		if(servletName.contains(" ")) {
			throw new IllegalArgumentException("unable to instantiate servlet, servlet name may not contain spaces");
		}

		log.info("instantiating IbisInitializer servlet name ["+servletName+"] servletClass ["+servlet+"] loadOnStartup ["+loadOnStartup+"]");
		getServletContext().log("instantiating IbisInitializer servlet ["+servletName+"]");


		String propertyPrefix = "servlet."+servletName+".";
		if(!appConstants.getBoolean(propertyPrefix+"enabled", true))
			return;

		ServletRegistration.Dynamic serv = getServletContext().addServlet(servletName, servlet);

		serv.addMapping(getUrlMapping(propertyPrefix, urlMapping));

		int loadOnStartupCopy = appConstants.getInt(propertyPrefix+"loadOnStartup", loadOnStartup);
		serv.setLoadOnStartup(loadOnStartupCopy);
		serv.setServletSecurity(getServletSecurity(propertyPrefix, roles));

		if(initParameters != null && !initParameters.isEmpty()) {
			//Manually loop through the map as serv.setInitParameters will fail all parameters even if only 1 fails...
			for (String key : initParameters.keySet()) {
				String value = initParameters.get(key);
				if(!serv.setInitParameter(key, value)) {
					log("unable to set init-parameter ["+key+"] with value ["+value+"] for servlet ["+servletName+"]", Level.ERROR);
				}
			}
		}

		if(log.isDebugEnabled()) log.debug("registered servlet ["+servletName+"] class ["+servlet+"] url(s) ["+urlMapping+"] loadOnStartup ["+loadOnStartup+"]");
	}

	private String[] getUrlMapping(String propertyPrefix, String defaultUrlMappings) {
		String[] urlMappingsCopy = defaultUrlMappings.split(",");
		String urlMappingOverride = appConstants.getString(propertyPrefix+"urlMapping", null);
		if(StringUtils.isNotEmpty(urlMappingOverride)) {
			urlMappingsCopy = urlMappingOverride.split(",");
		}

		List<String> mappings = new ArrayList<>();
		for(String urlMapping : urlMappingsCopy) {
			String mapping = urlMapping.trim();
			if(!mapping.startsWith("/") && !mapping.startsWith("*")) {
				mapping = "/"+mapping;
			}
			mappings.add(mapping);
		}

		return mappings.toArray(new String[0]);
	}

	private ServletSecurityElement getServletSecurity(String propertyPrefix, String[] defaultRoles) {
		String[] rolesCopy = new String[0];
		if(defaultRoles != null && webSecurityEnabled) {
			rolesCopy = defaultRoles;
		}

		String roleNames = appConstants.getString(propertyPrefix+"securityroles", null);
		if(StringUtils.isNotEmpty(roleNames)) {
			log.warn("property ["+propertyPrefix+"securityroles] has been replaced with ["+propertyPrefix+"securityRoles"+"]");
		}
		roleNames = appConstants.getString(propertyPrefix+"securityRoles", roleNames);

		if(StringUtils.isNotEmpty(roleNames))
			rolesCopy = roleNames.split(",");
		declareRoles(rolesCopy);

		TransportGuarantee transportGuarantee = getTransportGuarantee(propertyPrefix+"transportGuarantee");
		HttpConstraintElement httpConstraintElement = new HttpConstraintElement(transportGuarantee, rolesCopy);
		return new ServletSecurityElement(httpConstraintElement);
	}

	private void log(String msg, Level level) {
		if(log.isInfoEnabled() )
			getServletContext().log(msg);

		log.log(level, msg);
	}

	public static ServletSecurity.TransportGuarantee getTransportGuarantee(String propertyName) {
		AppConstants appConstants = AppConstants.getInstance();
		String constraintType = appConstants.getString(propertyName, null);
		if (StringUtils.isNotEmpty(constraintType)) {
			return ServletSecurity.TransportGuarantee.valueOf(constraintType);
		}
		return defaultTransportGuarantee;
	}
}
