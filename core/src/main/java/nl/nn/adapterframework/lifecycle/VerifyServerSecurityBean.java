/*
   Copyright 2020 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.lifecycle.servlets.SecuritySettings;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

@IbisInitializer
public class VerifyServerSecurityBean implements InitializingBean {

	private Logger log = LogUtil.getLogger(this);

	@Autowired
	public void setServletManager(ServletManager servletManager) {
		//Ensure this bean is loaded after the ServletManager has been instantiated
	}

	@Override
	public void afterPropertiesSet() {
		if(!SecuritySettings.isWebSecurityEnabled()) {
			AppConstants appConstants = AppConstants.getInstance();
			boolean isDtapStageLoc = "LOC".equalsIgnoreCase(appConstants.getProperty("dtap.stage"));
			if(appConstants.getBoolean("security.constraint.warning", !isDtapStageLoc)) {
				ApplicationWarnings.add(log, "unsecure Frank!Application, authentication has been disabled!");
			}
		}
	}
}
