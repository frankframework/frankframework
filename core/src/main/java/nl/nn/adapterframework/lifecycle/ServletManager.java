package nl.nn.adapterframework.lifecycle;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.HttpConstraintElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.util.AppConstants;

public class ServletManager {

	private IbisApplicationServlet servlet = null;
	private List<String> registeredRoles = new ArrayList<String>();

	public ServletManager(IbisApplicationServlet servlet) {
		this.servlet = servlet;

		declareRoles("IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester", "IbisWebService");
	}

	private ServletContext getServletContext() {
		return servlet.getServletContext();
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
			}
		}
	}

	public void register(String servletName, Servlet servletClass, String urlMapping) {
		register(servletName, servletClass, urlMapping, -1);
	}

	public void register(String servletName, Servlet servletClass, String urlMapping, int loadOnStartup) {
		AppConstants appConstants = AppConstants.getInstance();
		String propertyPrefix = "servlet."+servletName+".";

		ServletRegistration.Dynamic serv = getServletContext().addServlet(servletName, servletClass);

		ServletSecurity.TransportGuarantee transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL;

		String stage = appConstants.getString("otap.stage", null);
		if (StringUtils.isNotEmpty(stage) && stage.equalsIgnoreCase("LOC")) {
			transportGuarantee = ServletSecurity.TransportGuarantee.NONE;
		}

		String constraintType = appConstants.getString(propertyPrefix+"transportGuarantee", null);
		if (StringUtils.isNotEmpty(constraintType)) {
			transportGuarantee = ServletSecurity.TransportGuarantee.valueOf(constraintType);
		}

		String[] roles = new String[0];
		String roleNames = appConstants.getString(propertyPrefix+"securityroles", null);
		if(StringUtils.isNotEmpty(roleNames))
			roles = roleNames.split(",");
		declareRoles(roles);

		HttpConstraintElement httpConstraintElement = new HttpConstraintElement(transportGuarantee, roles);
		ServletSecurityElement constraint = new ServletSecurityElement(httpConstraintElement);

		String urlMappingCopy = appConstants.getString(propertyPrefix+"urlMapping", urlMapping);
		serv.addMapping(urlMappingCopy);

		int loadOnStartupCopy = appConstants.getInt(propertyPrefix+"loadOnStartup", loadOnStartup);
		serv.setLoadOnStartup(loadOnStartupCopy);
		serv.setServletSecurity(constraint);
	}

	public void register(DynamicRegistration.Servlet servlet) {
		register(servlet.getName(), servlet.getServletClass(), servlet.getUrlMapping(), servlet.loadOnStartUp());
	}
}
