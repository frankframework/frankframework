/*
   Copyright 2019-2020 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.http;

import jakarta.servlet.http.HttpServlet;

import org.frankframework.lifecycle.DynamicRegistration;

/**
 * Base class for @IbisInitializer capable servlets
 *
 * @author Niels Meijer
 */
public abstract class AbstractHttpServlet extends HttpServlet implements DynamicRegistration.Servlet {

	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return null;
	}
}
