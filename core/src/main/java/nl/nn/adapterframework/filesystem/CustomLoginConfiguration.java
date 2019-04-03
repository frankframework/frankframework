package nl.nn.adapterframework.filesystem;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

public class CustomLoginConfiguration extends Configuration{

	//define a map of params you wish to pass and fill them up
	Map<String, String> params = new HashMap<String, String>();

	private AppConfigurationEntry configEntry = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
			AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, params);

	public CustomLoginConfiguration(Map<String, String> params) {
		this.params.putAll(params);
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		return new AppConfigurationEntry[]{configEntry};
	}

}
