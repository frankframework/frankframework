/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.lifecycle.servlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.DynamicRegistration.Servlet;
import org.frankframework.util.EnumUtils;

/**
 * Yaml config example:
 * <pre>{@code
 *  servlets:
 *    IAF-API:
 *      transportGuarantee: NONE
 *      securityRoles:
 *        - IbisObserver
 *        - IbisDataAdmin
 *        - IbisAdmin
 *        - IbisTester
 *      urlMapping: iaf/api/*
 *      loadOnStartup: 0
 *      authenticator: myAuthenticatorID
 * }</pre>
 */
@Log4j2
public class ServletConfiguration implements InitializingBean, EnvironmentAware {

	private static final char SLASH = '/';
	private @Getter String name;
	private List<String> securityRoles = Collections.emptyList();
	private @Getter List<String> urlMapping;
	private @Getter @Setter int loadOnStartup = -1;
	private @Getter @Setter boolean enabled = true;
	private @Getter TransportGuarantee transportGuarantee;
	private @Getter String authenticatorName = null;
	private @Getter jakarta.servlet.Servlet servlet;
	private @Setter Environment environment;
	private final @Getter Map<String, String> initParameters = new LinkedHashMap<>();

	@Override
	public void afterPropertiesSet() {
		transportGuarantee = SecuritySettings.getDefaultTransportGuarantee();
		String defaultAuthenticatorName = environment.getProperty("application.security.http.authenticator");
		authenticatorName = SecuritySettings.isWebSecurityEnabled() ? defaultAuthenticatorName : AuthenticationType.NONE.name();
	}

	public void setSecurityRoles(String[] accessGrantingRoles) {
		if(accessGrantingRoles != null) {
			this.securityRoles = Arrays.asList(accessGrantingRoles);
		}
	}

	@Nonnull
	public List<String> getSecurityRoles() {
		return securityRoles;
	}

	public void setName(String servletName) {
		this.name = servletName;
		if(this.name.contains(" ")) {
			throw new IllegalArgumentException("unable to instantiate servlet, servlet name may not contain spaces");
		}
	}

	/** Convenience method to easily register a dynamic Frank servlet. */
	public void fromServlet(Servlet servlet) {
		setName(servlet.getName());
		setUrlMapping(servlet.getUrlMapping());
		setSecurityRoles(servlet.getAccessGrantingRoles());
		setLoadOnStartup(servlet.loadOnStartUp());
		setEnabled(servlet.isEnabled());
		setServlet(servlet);

		if(servlet instanceof DynamicRegistration.ServletWithParameters parameters) {
			Map<String, String> initParams = parameters.getParameters();
			initParams.entrySet().stream().forEach(e -> addInitParameter(e.getKey(), e.getValue()));
		}

		loadProperties();
	}

	public void setServlet(jakarta.servlet.Servlet servlet) {
		this.servlet = servlet;
	}

	public void addInitParameter(String name, String value) {
		this.initParameters.put(name, value);
	}

	public boolean isAuthenticationEnabled() {
		return !securityRoles.isEmpty() && !"NONE".equals(authenticatorName);
	}

	/**
	 * Overwrites servlet defaults with properties.
	 */
	public void loadProperties() {
		String propertyPrefix = "servlet."+name+".";

		this.enabled = environment.getProperty(propertyPrefix+"enabled", boolean.class, enabled);
		configureServletSecurity(propertyPrefix);
		String constraintType = environment.getProperty(propertyPrefix+"transportGuarantee");
		if(StringUtils.isNotEmpty(constraintType)) {
			this.transportGuarantee = EnumUtils.parse(TransportGuarantee.class, constraintType);
		}
		this.loadOnStartup = environment.getProperty(propertyPrefix+"loadOnStartup", int.class, loadOnStartup);
		String mapping = environment.getProperty(propertyPrefix+"urlMapping");
		if(StringUtils.isNotEmpty(mapping)) {
			setUrlMapping(mapping);
		}
		this.authenticatorName = environment.getProperty(propertyPrefix+"authenticator", authenticatorName);
	}

	private void configureServletSecurity(String propertyPrefix) {
		String roleNames = environment.getProperty(propertyPrefix+"securityRoles");
		if(environment.containsProperty(propertyPrefix+"securityroles")) { // Deprecated warning
			log.warn("property [{}securityroles] has been replaced with [{}securityRoles]", propertyPrefix, propertyPrefix);
			roleNames = environment.getProperty(propertyPrefix+"securityroles");
		}

		if(StringUtils.isNotEmpty(roleNames)) {
			String[] rolesCopy = roleNames.split(",");
			securityRoles = Arrays.asList(rolesCopy);
		}
	}

	private void setUrlMapping(List<String> urlMappings) {
		if(urlMappings == null || urlMappings.isEmpty()) {
			throw new IllegalStateException("servlet must have an URL to map to");
		}

		this.urlMapping = urlMappings.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
	}

	public void setUrlMapping(String urlMappingArray) {
		String[] urlMappingsCopy = new String[0];
		if(StringUtils.isNotEmpty(urlMappingArray)) {
			urlMappingsCopy = urlMappingArray.split(",");
		}

		List<String> mappings = new ArrayList<>();
		for(final String rawMapping : urlMappingsCopy) {
			String mapping = rawMapping.trim();
			if(StringUtils.isBlank(mapping)) continue;
			if("*".equals(mapping)) mapping = "/*";

			char firstChar = mapping.charAt(0);
			if(firstChar == '!' && (mapping.charAt(1) != SLASH || mapping.charAt(mapping.length()-1) == '*')) {
				throw new IllegalStateException("when excluding an URL you it must start with '!/' and may not end with a wildcard");
			}
			if(firstChar != SLASH && firstChar != '*' && firstChar != '!') { // Add a conditional slash
				mapping = "/"+mapping;
			}
			log.debug("converted raw mapping [{}] to [{}]", rawMapping, mapping);
			mappings.add(mapping);
		}

		setUrlMapping(mappings);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(" servlet ["+name+"]");
		builder.append(" url(s) ").append(urlMapping);
		builder.append(" loadOnStartup [").append(loadOnStartup).append("]");
		builder.append(" protocol ").append(transportGuarantee==TransportGuarantee.CONFIDENTIAL?"[HTTPS]":"[HTTP]");
		builder.append(" authenticatior [").append(authenticatorName).append("]");

		if(isAuthenticationEnabled()) {
			builder.append(" roles ").append(getSecurityRoles());
		} else {
			builder.append(" with no authentication enabled!");
		}
		return builder.toString();
	}
}
