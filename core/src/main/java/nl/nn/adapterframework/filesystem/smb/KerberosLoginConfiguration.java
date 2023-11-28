/*
   Copyright 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.filesystem.smb;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class KerberosLoginConfiguration extends Configuration {

	private static final Logger logger = LogUtil.getLogger(KerberosLoginConfiguration.class);
	private final Map<String, String> loginModuleOptions = new HashMap<>();

	private static final String IBM_LOGIN_MODULE_CLASSNAME = "com.ibm.security.auth.module.Krb5LoginModule";
	private static final String ORACLE_LOGIN_MODULE_CLASSNAME = "com.sun.security.auth.module.Krb5LoginModule";

	public KerberosLoginConfiguration(Map<String, String> options) {
		this.loginModuleOptions.putAll(options);
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		if(ClassUtils.isClassPresent(ORACLE_LOGIN_MODULE_CLASSNAME)) {
			return new AppConfigurationEntry[] { createAppConfigurationEntry(ORACLE_LOGIN_MODULE_CLASSNAME) };
		} else if(ClassUtils.isClassPresent(IBM_LOGIN_MODULE_CLASSNAME)) {
			return new AppConfigurationEntry[] { createAppConfigurationEntry(IBM_LOGIN_MODULE_CLASSNAME) };
		}

		logger.error("neither ({}) nor ({}) class is found, unable to use KRB5 authentication", ORACLE_LOGIN_MODULE_CLASSNAME, IBM_LOGIN_MODULE_CLASSNAME);
		return null; //API requires a null to be returned when no AuthenticationEntry is found.
	}

	private AppConfigurationEntry createAppConfigurationEntry(String loginModule) {
		return new AppConfigurationEntry(loginModule, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, loginModuleOptions);
	}
}
