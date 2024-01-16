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
package org.frankframework.extensions.cmis.servlets;

import org.frankframework.lifecycle.IbisInitializer;

@IbisInitializer
public class WebServices11 extends WebServicesServletBase {

	private static final long serialVersionUID = 1L;

	@Override
	public String getUrlMapping() {
		return "/cmis/webservices/*";
	}

	@Override
	protected String getCmisVersion() {
		return "1.1";
	}
}
