package nl.nn.adapterframework.filesystem;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

public class KerberosLoginConfiguration extends Configuration{

	//define a map of params you wish to pass and fill them up
	Map<String, String> params = new HashMap<String, String>();

	private AppConfigurationEntry configEntry = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
			AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, params);
// For websphere customize this
//	private AppConfigurationEntry configEntry = new AppConfigurationEntry("com.ibm.security.auth.module.Krb5LoginModule",
//			AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, params);

	
	
	public KerberosLoginConfiguration(Map<String, String> params) {
		this.params.putAll(params);
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		return new AppConfigurationEntry[]{configEntry};
	}

}
