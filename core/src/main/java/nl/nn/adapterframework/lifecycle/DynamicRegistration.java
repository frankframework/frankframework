/*
   Copyright 2019 Nationale-Nederlanden

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

public interface DynamicRegistration {

	public interface Servlet extends DynamicRegistration, javax.servlet.Servlet {
		public HttpServlet getServlet();
		public String getUrlMapping();
		public String[] getRoles();
	}
	public interface ServletWithParameters extends Servlet {
		public Map<String, String> getParameters();
	}

	/**
	 * @return Name of the to-be implemented class
	 */
	public String getName();

	/**
	 * Order in which to automatically instantiate and load the class.</br>
	 * @return <code>0</code> to let the ibis determine, <code>-1</code> to disable
	 */
	public int loadOnStartUp();
}
