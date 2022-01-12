/*
   Copyright 2019-2020 Nationale-Nederlanden

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

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.HttpConstraintElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Enables the use of programmatically adding servlets to the given ServletContext<br/>
 * <br/>
 * The following properties can be set to change the DynamicRegistration.Servlet registration behaviour<br/>
 * <code>servlet.<name>.transportGuarantee</code> - forces https when set to CONFIDENTIAL<br/>
 * <code>servlet.<name>.securityRoles</code> - use the default IBIS roles or create your own<br/>
 * <code>servlet.<name>.urlMapping</code> - path the servlet listens to<br/>
 * <code>servlet.<name>.loadOnStartup</code> - automatically load or use lazyloading<br/>
 * 
 * @author Niels Meijer
 *
 */
public class ServletManager {

	private ServletContext servletContext = null;
	private List<String> registeredRoles = new ArrayList<String>();
	private Logger log = LogUtil.getLogger(this);

	private ServletContext getServletContext() {
		return servletContext;
	}

	public ServletManager(ServletContext servletContext) {
		this.servletContext = servletContext;

		//Add the default IBIS roles
		registeredRoles.addAll(Arrays.asList("IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester", "IbisWebService"));
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

	public void registerServlet(String servletName, Servlet servletClass, String urlMapping) {
		registerServlet(servletName, servletClass, urlMapping, new String[0], -1, null);
	}

	public void registerServlet(String servletName, Servlet servletClass, String urlMapping, String[] roles, int loadOnStartup, Map<String, String> initParameters) {
		log.info("instantiating IbisInitializer servlet name ["+servletName+"] servletClass ["+servletClass+"] loadOnStartup ["+loadOnStartup+"]");
		getServletContext().log("instantiating IbisInitializer servlet ["+servletName+"]");


		AppConstants appConstants = AppConstants.getInstance();
		String propertyPrefix = "servlet."+servletName+".";

		if(!appConstants.getBoolean(propertyPrefix+"enabled", true))
			return;

		ServletRegistration.Dynamic serv = getServletContext().addServlet(servletName, servletClass);
		ServletSecurity.TransportGuarantee transportGuarantee = getTransportGuarantee(propertyPrefix+"transportGuarantee");

		String stage = appConstants.getString("dtap.stage", null);
		String[] rolesCopy = new String[0];
		if(roles != null && stage != null && !stage.equalsIgnoreCase("LOC"))
			rolesCopy = roles;

		String roleNames = appConstants.getString(propertyPrefix+"securityroles", null);
		if(StringUtils.isNotEmpty(roleNames)) {
			log.warn("property ["+propertyPrefix+"securityroles] has been replaced with ["+propertyPrefix+"securityRoles"+"]");
		}
		roleNames = appConstants.getString(propertyPrefix+"securityRoles", roleNames);

		if(StringUtils.isNotEmpty(roleNames))
			rolesCopy = roleNames.split(",");
		declareRoles(rolesCopy);

		HttpConstraintElement httpConstraintElement = new HttpConstraintElement(transportGuarantee, rolesCopy);
		ServletSecurityElement constraint = new ServletSecurityElement(httpConstraintElement);

		String urlMappingCopy = appConstants.getString(propertyPrefix+"urlMapping", urlMapping);
		if(!urlMappingCopy.startsWith("/") && !urlMappingCopy.startsWith("*")) {
			urlMappingCopy = "/"+urlMappingCopy;
		}
		serv.addMapping(urlMappingCopy);

		int loadOnStartupCopy = appConstants.getInt(propertyPrefix+"loadOnStartup", loadOnStartup);
		serv.setLoadOnStartup(loadOnStartupCopy);
		serv.setServletSecurity(constraint);

		if(initParameters != null && !initParameters.isEmpty()) {
			//Manually loop through the map as serv.setInitParameters will fail all parameters even if only 1 fails...
			for (String key : initParameters.keySet()) {
				String value = initParameters.get(key);
				if(!serv.setInitParameter(key, value)) {
					log("unable to set init-parameter ["+key+"] with value ["+value+"] for servlet ["+servletName+"]", Level.ERROR);
				}
			}
		}

		if(log.isDebugEnabled()) log.debug("registered servlet ["+servletName+"] class ["+servletClass+"] url ["+urlMapping+"] loadOnStartup ["+loadOnStartup+"]");
	}

	private void log(String msg, Level level) {
		if(log.isInfoEnabled() )
			getServletContext().log(msg);

		log.log(level, msg);
	}

	public void register(DynamicRegistration.Servlet servlet) {
		Map<String, String> parameters = null;
		if(servlet instanceof DynamicRegistration.ServletWithParameters)
			parameters = ((DynamicRegistration.ServletWithParameters) servlet).getParameters();

		registerServlet(servlet.getName(), servlet.getServlet(), servlet.getUrlMapping(), servlet.getRoles(), servlet.loadOnStartUp(), parameters);
	}

	public static ServletSecurity.TransportGuarantee getTransportGuarantee(String propertyName) {
		AppConstants appConstants = AppConstants.getInstance();
		String constraintType = appConstants.getString(propertyName, null);
		if (StringUtils.isNotEmpty(constraintType))
			return ServletSecurity.TransportGuarantee.valueOf(constraintType);

		String stage = appConstants.getString("dtap.stage", null);
		if (StringUtils.isNotEmpty(stage) && stage.equalsIgnoreCase("LOC")) {
			return ServletSecurity.TransportGuarantee.NONE;
		}
		return ServletSecurity.TransportGuarantee.CONFIDENTIAL;

	}
}
