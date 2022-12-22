/*
   Copyright 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.esb;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Some utilities for working with Esb.
 *
 * @author Peter Leeuwenburgh
 */
public class EsbUtils {
	protected static Logger log = LogUtil.getLogger(EsbUtils.class);

	private static EsbConnectionFactoryInfo getEsbConnectionFactoryInfo(EsbJmsListener esbJmsListener) {
		String cfUrl = null;
		String cfUserName = null;
		String cfPassword = null;

		Object managedConnectionFactory = null;
		try {
			managedConnectionFactory = esbJmsListener
					.getManagedConnectionFactory();
		} catch (JmsException e) {
			log.warn("error occured during getting managed connection factory: "
					+ e.getMessage());
		}
		if (managedConnectionFactory == null) {
			log.warn("could not get managed connection factory");
		} else {
			String contextFactoryClassname = getContextFactoryClassname(managedConnectionFactory);
			if (contextFactoryClassname == null) {
				log.warn("could not get context factory");
			} else {
				cfUrl = getProviderURL(managedConnectionFactory);
				String authDataAlias = getAuthDataAlias(managedConnectionFactory);
				if (authDataAlias != null) {
					CredentialFactory cf = new CredentialFactory(authDataAlias);
					cfUserName = cf.getUsername();
					cfPassword = cf.getPassword();
				}
				if (!contextFactoryClassname
						.equals("com.tibco.tibjms.naming.TibjmsInitialContextFactory")) {
					log.warn("did not expect context factory of type ["
							+ contextFactoryClassname + "]");
				} else {
					return new EsbConnectionFactoryInfo(
							managedConnectionFactory, contextFactoryClassname,
							cfUrl, cfUserName, cfPassword);
				}
			}
		}
		return null;
	}

	private static String getContextFactoryClassname(
			Object managedConnectionFactory) {
		try {
			return (String) ClassUtils.invokeGetter(managedConnectionFactory,
					"getContextFactoryClassname", true);
		} catch (Exception e) {
			// log.debug("Caught Exception: "+e.getMessage());
			return null;
		}
	}

	private static String getProviderURL(Object managedConnectionFactory) {
		try {
			return (String) ClassUtils.invokeGetter(managedConnectionFactory,
					"getProviderURL", true);
		} catch (Exception e) {
			// log.debug("Caught Exception: "+e.getMessage());
			return null;
		}
	}

	private static String getAuthDataAlias(Object managedConnectionFactory) {
		try {
			return (String) ClassUtils.invokeGetter(managedConnectionFactory,
					"getAuthDataAlias", true);
		} catch (Exception e) {
			// log.debug("Caught Exception: "+e.getMessage());
			return null;
		}
	}

	public static String getQueueMessageCount(EsbJmsListener esbJmsListener) {
		EsbConnectionFactoryInfo ecfi = getEsbConnectionFactoryInfo(esbJmsListener);
		if (ecfi != null) {
			return getQueueMessageCount(ecfi.getUrl(), null,
					ecfi.getUserName(), ecfi.getPassword(),
					esbJmsListener.getPhysicalDestinationShortName(),
					esbJmsListener.getMessageSelector());
		}
		return null;
	}

	public static String getQueueMessageCount(String provUrl, String authAlias,
			String userName, String password, String queueName,
			String messageSelector) {
		try {
			Class<?>[] args_types = new Class<?>[6];
			args_types[0] = String.class;
			args_types[1] = String.class;
			args_types[2] = String.class;
			args_types[3] = String.class;
			args_types[4] = String.class;
			args_types[5] = String.class;
			Object[] args = new Object[6];
			args[0] = provUrl;
			args[1] = authAlias;
			args[2] = userName;
			args[3] = password;
			args[4] = queueName;
			args[5] = messageSelector;
			long messageCount = (Long) Class
					.forName(
							"nl.nn.adapterframework.extensions.tibco.TibcoUtils")
					.getMethod("getQueueMessageCount", args_types)
					.invoke(null, args);
			return String.valueOf(messageCount);
		} catch (Exception e) {
			log.warn("error occured during getting queue message count: "
					+ e.getMessage());
		}
		return null;
	}
}
