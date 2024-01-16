/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.ibistesttool.web;

import java.util.Map;

import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.IbisInitializer;

/**
 * @author Jaco de Groot
 */
@IbisInitializer
public class ApiServlet extends nl.nn.testtool.web.ApiServlet implements DynamicRegistration.ServletWithParameters {
	private static final long serialVersionUID = 1L;

	@Override
	public String getUrlMapping() {
		return "/iaf" + getDefaultMapping();
	}

	@Override
	public Map<String, String> getParameters() {
		return getDefaultInitParameters();
	}

	@Override
	public String getName() {
		return "Ladybug-" + this.getClass().getSimpleName();
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return DynamicRegistration.Servlet.ALL_IBIS_USER_ROLES;
	}
}
