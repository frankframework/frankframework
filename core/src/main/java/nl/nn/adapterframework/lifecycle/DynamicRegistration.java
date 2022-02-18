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

import javax.servlet.http.HttpServlet;

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

	public interface Servlet extends DynamicRegistration, javax.servlet.Servlet {

		public static final String[] ALL_IBIS_ROLES = {"IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester", "IbisWebService"};
		public static final String[] ALL_IBIS_USER_ROLES = {"IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester"};

		/** @return The {@link javax.servlet.http.HttpServlet Servlet} to register using the {@link ServletManager} */
		public HttpServlet getServlet();

		/** @return The URL the {@link javax.servlet.http.HttpServlet Servlet} should be mapped to. */
		public String getUrlMapping();

		/**
		 * Not used when dtap.stage == LOC. See {@link ServletManager} for more information.
		 * @return The default roles the {@link javax.servlet.http.HttpServlet Servlet} has, or <code>null</code> to disable.
		 */
		public String[] getRoles();
	}

	public interface ServletWithParameters extends Servlet {
		/**
		 * @return {@link javax.servlet.http.HttpServlet Servlet} specific init parameters
		 */
		public Map<String, String> getParameters();
	}

	/**
	 * @return Name of the to-be implemented class
	 */
	public default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Order in which to automatically instantiate and load the class.</br>
	 * @return <code>0</code> to let the application server determine, <code>-1</code> to disable
	 */
	public default int loadOnStartUp() {
		return 1;
	}
}
