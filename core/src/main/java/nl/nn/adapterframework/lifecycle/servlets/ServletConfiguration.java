/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle.servlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.lifecycle.DynamicRegistration.Servlet;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;

//servlets:
//  IAF-API:
//    transportGuarantee: NONE
//    securityRoles:
//      - IbisObserver
//      - IbisDataAdmin
//      - IbisAdmin
//      - IbisTester
//    urlMapping: iaf/api/*
//    loadOnStartup: 0
//    authenticator: myAuthenticatorID
public class ServletConfiguration {
	private AppConstants appConstants = AppConstants.getInstance();
	private Logger log = LogUtil.getLogger(this);

	private final @Getter String name;
	private @Getter List<String> securityRoles;
	private @Getter List<String> urlMapping;
	private @Getter int loadOnStartup = -1;
	private @Getter boolean enabled = true;
	private @Getter TransportGuarantee transportGuarantee;
	private @Getter String authenticatorName = null;

	public ServletConfiguration(Servlet servlet) {
		this.name = servlet.getName();
		if(this.name.contains(" ")) {
			throw new IllegalArgumentException("unable to instantiate servlet, servlet name may not contain spaces");
		}

		this.urlMapping = configureUrlMapping(servlet.getUrlMapping());
		this.securityRoles = servlet.getAccessGrantingRoles() == null ? Collections.emptyList() : Arrays.asList(servlet.getAccessGrantingRoles());
		this.loadOnStartup = servlet.loadOnStartUp();
		this.enabled = servlet.isEnabled();

		defaultSecuritySettings();
		loadProperties();

		if(urlMapping.isEmpty()) {
			throw new IllegalStateException("servlet must have an URL to map to");
		}
	}

	public boolean isAuthenticationEnabled() {
		return !securityRoles.isEmpty() && !"NONE".equals(authenticatorName);
	}

	private void defaultSecuritySettings() {
		transportGuarantee = ServletManager.getDefaultTransportGuarantee();
		AuthenticationType defaultType = ServletManager.isWebSecurityEnabled() ? AuthenticationType.CONTAINER : AuthenticationType.NONE;
		authenticatorName = defaultType.name();
	}

	private void loadProperties() {
		String propertyPrefix = "servlet."+name+".";

		this.enabled = appConstants.getBoolean(propertyPrefix+"enabled", enabled);
		configureServletSecurity(propertyPrefix);
		String constraintType = appConstants.getString(propertyPrefix+"transportGuarantee", null);
		if(StringUtils.isNotEmpty(constraintType)) {
			this.transportGuarantee = EnumUtils.parse(TransportGuarantee.class, constraintType);
		}
		this.loadOnStartup = appConstants.getInt(propertyPrefix+"loadOnStartup", loadOnStartup);
		String mapping = appConstants.getString(propertyPrefix+"urlMapping", null);
		if(StringUtils.isNotEmpty(mapping)) {
			this.urlMapping = configureUrlMapping(mapping);
		}
		this.authenticatorName = appConstants.getString(propertyPrefix+"authenticator", authenticatorName);
	}

	private void configureServletSecurity(String propertyPrefix) {
		String roleNames = appConstants.getString(propertyPrefix+"securityroles", null);
		if(StringUtils.isNotEmpty(roleNames)) {
			log.warn("property ["+propertyPrefix+"securityroles] has been replaced with ["+propertyPrefix+"securityRoles"+"]");
		}
		roleNames = appConstants.getString(propertyPrefix+"securityRoles", roleNames);

		if(StringUtils.isNotEmpty(roleNames)) {
			String[] rolesCopy = roleNames.split(",");
			securityRoles = Arrays.asList(rolesCopy);
		}
	}

	private List<String> configureUrlMapping(String urlMappingArray) {
		String[] urlMappingsCopy = new String[0];
		if(StringUtils.isNotEmpty(urlMappingArray)) {
			urlMappingsCopy = urlMappingArray.split(",");
		}

		List<String> mappings = new ArrayList<>();
		for(String rawMapping : urlMappingsCopy) {
			String mapping = rawMapping.trim();
			if(!mapping.startsWith("/") && !mapping.startsWith("*")) {
				mapping = "/"+mapping;
			}
			mappings.add(mapping);
		}

		return mappings;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(" servlet ["+name+"]");
		builder.append(" url(s) "+urlMapping);
		builder.append(" loadOnStartup ["+loadOnStartup+"]");
		builder.append(" protocol "+(transportGuarantee==TransportGuarantee.CONFIDENTIAL?"[HTTPS]":"[HTTP]"));
		builder.append(" authenticatior ["+authenticatorName+"]");

		if(isAuthenticationEnabled()) {
			builder.append(" roles "+getSecurityRoles());
		} else {
			builder.append(" with no authentication enabled!");
		}
		return builder.toString();
	}
}
