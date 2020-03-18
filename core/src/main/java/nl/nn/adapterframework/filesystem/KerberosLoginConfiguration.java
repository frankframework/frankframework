package nl.nn.adapterframework.filesystem;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class KerberosLoginConfiguration extends Configuration{

	private final static Logger logger = LogUtil.getLogger(KerberosLoginConfiguration.class);
	//define a map of params you wish to pass and fill them up
	private Map<String, String> params = new HashMap<String, String>();
	
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
