package nl.nn.adapterframework.lifecycle.servlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

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
//    authentication:
//      type: AD
//      domain: company.org
//      endpoint: 10.1.2.3
public class ServletConfiguration {
	private AppConstants appConstants = AppConstants.getInstance();
	private Logger log = LogUtil.getLogger(this);

	protected static final String AUTH_ENABLED_KEY = "application.security.http.authentication";
	protected static final String HTTPS_ENABLED_KEY = "application.security.http.transportGuarantee";

	private final @Getter String name;
	private @Getter List<String> securityRoles;
	private @Getter List<String> urlMapping;
	private @Getter int loadOnStartup;
	private @Getter boolean enabled = true;
	private @Getter TransportGuarantee transportGuarantee = TransportGuarantee.NONE;
	private @Getter AuthenticationType authentication = AuthenticationType.ANONYMOUS;

	public ServletConfiguration(Servlet servlet) {
		this.name = servlet.getName();
		if(this.name.contains(" ")) {
			throw new IllegalArgumentException("unable to instantiate servlet, servlet name may not contain spaces");
		}

		this.urlMapping = configureUrlMapping(servlet.getUrlMapping());
		this.securityRoles = servlet.getAccessGrantingRoles() == null ? Collections.emptyList() : Arrays.asList(servlet.getAccessGrantingRoles());
		this.loadOnStartup = servlet.loadOnStartUp();
		if(this.name.equals("IAF-API")) authentication = AuthenticationType.AD;

		defaultSecuritySettings();
//		loadYaml();
		loadProperties();

		if(urlMapping.isEmpty()) {
			throw new IllegalStateException("servlet must have an URL to map to");
		}
	}

	public boolean isAuthenticationEnabled() {
		return !securityRoles.isEmpty() && authentication != AuthenticationType.ANONYMOUS;
	}

	private void defaultSecuritySettings() {
		boolean isDtapStageLoc = "LOC".equalsIgnoreCase(appConstants.getProperty("dtap.stage"));
		String isAuthEnabled = appConstants.getProperty(AUTH_ENABLED_KEY);
		boolean webSecurityEnabled = StringUtils.isNotEmpty(isAuthEnabled) ? Boolean.parseBoolean(isAuthEnabled) : !isDtapStageLoc;
		if(!webSecurityEnabled) {
			authentication = AuthenticationType.ANONYMOUS;
		}

		String constraintType = appConstants.getProperty(HTTPS_ENABLED_KEY);
		if (StringUtils.isNotEmpty(constraintType)) {
			try {
				transportGuarantee = EnumUtils.parse(TransportGuarantee.class, constraintType);
			} catch(IllegalArgumentException e) {
				LogUtil.getLogger(ServletManager.class).error("unable to set TransportGuarantee", e);
			}
		} else if(isDtapStageLoc) {
			transportGuarantee = TransportGuarantee.NONE;
		}
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

	public SecurityFilterChain configure(HttpSecurity http) {
		try {
			http.headers().frameOptions().sameOrigin();
			http.csrf().disable();
			http.requestMatcher(getRequestMatcher());
			return authentication.getAuthenticator().configure(this, http);
		} catch (Exception e) {
			throw new IllegalStateException("unable to configure Spring Security", e);
		}
	}

	private RequestMatcher getRequestMatcher() {
		List<RequestMatcher> requestMatchers = new ArrayList<>();
		for(String url : this.getUrlMapping()) {
			String matcherUrl = url;
			if(url.endsWith("*")) {
				matcherUrl = url+"*";
			}

			requestMatchers.add(new AntPathRequestMatcher(matcherUrl, null, false));
		}

		return (requestMatchers.size() == 1) ? requestMatchers.get(0) : new OrRequestMatcher(requestMatchers);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(" servlet ["+name+"]");
		builder.append(" url(s) "+urlMapping);
		builder.append(" loadOnStartup ["+loadOnStartup+"]");

		builder.append(" protocol "+(transportGuarantee==TransportGuarantee.CONFIDENTIAL?"[HTTPS]":"[HTTP]"));
		if(isAuthenticationEnabled()) {
			builder.append(" and roles "+getSecurityRoles());
		} else {
			builder.append(" with no authentication enabled!");
		}
		return builder.toString();
	}
}
