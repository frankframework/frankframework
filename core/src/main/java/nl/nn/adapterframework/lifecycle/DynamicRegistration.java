/*
   Copyright 2019 Nationale-Nederlanden, 2021-2022 WeAreFrank!

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

import java.util.Map;

/**
 * Interface to use in combination with the {@link IbisInitializer} annotation.
 * Classes that implement the annotation are automatically picked up by Spring, and allow you to use:
 * <code>
 * public void setServletManager(ServletManager servletManager) {
 *  ServletManager.register(this);
 * }
 * </code>
 *
 * @author Niels Meijer
 *
 */
public interface DynamicRegistration {

	public static final String[] ALL_IBIS_ROLES = {"IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester", "IbisWebService"};
	public static final String[] ALL_IBIS_USER_ROLES = {"IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester"};
	public static final String[] IBIS_FULL_SERVICE_ACCESS_ROLES = {"IbisTester", "IbisWebService"};

	public interface Servlet extends DynamicRegistration, javax.servlet.Servlet {

		/**
		 * The URL the {@link javax.servlet.http.HttpServlet Servlet} should be mapped to.
		 * This value may be overridden by setting property <code>servlet.servlet-name.urlMapping</code> to change path the servlet listens to
		 */
		public String getUrlMapping();

		/**
		 * The default authorization roles giving access to the {@link javax.servlet.http.HttpServlet Servlet}, or <code>null</code> to disable.
		 * This value may be overridden by setting property <code>servlet.servlet-name.securityRoles</code> to the roles that should be granted access.
		 * see {@link ServletManager} for more information.
		 */
		public String[] getAccessGrantingRoles();
	}

	public interface ServletWithParameters extends Servlet {
		/**
		 * {@link javax.servlet.http.HttpServlet Servlet} specific init parameters
		 */
		public Map<String, String> getParameters();
	}

	/**
	 * Name of the to-be implemented class
	 */
	public default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Order in which to automatically instantiate and load the class.<br/>
	 * <code>0</code> to let the application server determine, <code>-1</code> to disable
	 * This value may be overridden by setting property <code>servlet.servlet-name.loadOnStartup</code>
	 */
	public default int loadOnStartUp() {
		return -1;
	}
}
