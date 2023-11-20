/*
   Copyright 2020 Nationale-Nederlanden, 2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.util.AppConstants;

@IbisInitializer
public class RedirectIndexProxy extends HttpServletBase {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String consoleLocation = AppConstants.getInstance().getProperty("console.location");
		if(StringUtils.isNotEmpty(consoleLocation)) {
			response.sendRedirect(consoleLocation);
		}
		else {
			response.sendRedirect("iaf/gui/");
		}
	}

	@Override
	public String getUrlMapping() {
		return "/index.html";
	}
}
