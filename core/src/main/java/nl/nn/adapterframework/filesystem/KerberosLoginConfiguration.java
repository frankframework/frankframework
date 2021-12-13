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
package nl.nn.adapterframework.filesystem;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class KerberosLoginConfiguration extends Configuration{

	private static final Logger logger = LogUtil.getLogger(KerberosLoginConfiguration.class);
	//define a map of params you wish to pass and fill them up
	private Map<String, String> params = new HashMap<>();

	private String ibmJavaKrb5LoginModuleClass = "com.ibm.security.auth.module.Krb5LoginModule";
	private String oracleJavaKrb5LoginModuleClass = "com.sun.security.auth.module.Krb5LoginModule";

	public KerberosLoginConfiguration(Map<String, String> params) {
		this.params.putAll(params);
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		AppConfigurationEntry configEntry = null;
		try {
			ClassUtils.loadClass(oracleJavaKrb5LoginModuleClass);
			configEntry = new AppConfigurationEntry(oracleJavaKrb5LoginModuleClass,
						AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, params);
			return new AppConfigurationEntry[]{configEntry};
		} catch (ClassNotFoundException e) {
			try {
				ClassUtils.loadClass(ibmJavaKrb5LoginModuleClass);
				configEntry = new AppConfigurationEntry(ibmJavaKrb5LoginModuleClass,
						AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, params);
				return new AppConfigurationEntry[]{configEntry};
			} catch (ClassNotFoundException e2) {
				String errorMessage = "Neither (" + oracleJavaKrb5LoginModuleClass + ") nor (" + ibmJavaKrb5LoginModuleClass + ") class is found";
				logger.error(errorMessage, e2);
			}
		}
		return null;
	}
}
