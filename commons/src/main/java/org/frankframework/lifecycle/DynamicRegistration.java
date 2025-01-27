/*
   Copyright 2019 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.lifecycle;

import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;

import org.springframework.stereotype.Component;

/**
 * A Servlet registration wrapper to register {@link jakarta.servlet.Servlet}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addServlet(String, Class)}
 * registration} features provided by {@link ServletContext} but with a friendly design allowing
 * users to change servlet settings before they are initialized by the {@link ServletContextListener} (starting the application).
 * <p>
 * Interface to use in combination with a Spring {@link Component} annotation.
 * Classes that implement the annotation are automatically picked up by Spring,
 * and in combination with the ServletRegisteringPostProcessor the servlets are
 * automatically registered with the ServletManager.
 * <p>
 * The servlet name will be deduced if not specified.
 *
 * @author Niels Meijer
 */
public interface DynamicRegistration {

	String[] ALL_IBIS_ROLES = {"IbisWebService", "IbisObserver", "IbisDataAdmin", "IbisAdmin","IbisTester"};
	String[] ALL_IBIS_USER_ROLES = {"IbisObserver", "IbisDataAdmin","IbisAdmin", "IbisTester"};
	String[] IBIS_FULL_SERVICE_ACCESS_ROLES = {"IbisWebService", "IbisTester"};

	/**
	 * Name of the to-be implemented class
	 */
	default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Order in which to automatically instantiate and load the class.<br/>
	 * <code>0</code> to let the application server determine, <code>-1</code> to disable
	 * This value may be overridden by setting property <code>servlet.servlet-name.loadOnStartup</code>
	 */
	default int loadOnStartUp() {
		return -1;
	}

	interface Servlet extends DynamicRegistration, jakarta.servlet.Servlet {

		/**
		 * The URL the {@link jakarta.servlet.http.HttpServlet Servlet} should be mapped to.
		 * This value may be overridden by setting property <code>servlet.servlet-name.urlMapping</code> to change path the servlet listens to
		 */
		String getUrlMapping();

		/**
		 * The default authorization roles giving access to the {@link jakarta.servlet.http.HttpServlet Servlet}, or <code>null</code> to disable.
		 * This value may be overridden by setting property <code>servlet.servlet-name.securityRoles</code> to the roles that should be granted access.
		 * see ServletManager for more information.
		 */
		String[] getAccessGrantingRoles();

		default boolean isEnabled() {
			return true;
		}
	}

	interface ServletWithParameters extends Servlet {
		/**
		 * {@link jakarta.servlet.http.HttpServlet Servlet} specific init parameters
		 */
		Map<String, String> getParameters();
	}
}
