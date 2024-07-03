/*
   Copyright 2013, 2017 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.extensions.sap.jco3;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

/**
 * @author  Jaco de Groot
 * @author  Niels Meijer
 * @since   5.0
 */
public class SapSystemDataProvider implements DestinationDataProvider {
	private static final Logger log = LogUtil.getLogger(SapSystemDataProvider.class);
	private static SapSystemDataProvider self = null;
	private DestinationDataEventListener destinationDataEventListener;

	private SapSystemDataProvider() {
		super();
	}

	public static synchronized SapSystemDataProvider getInstance() {
		if (self == null) {
			self = new SapSystemDataProvider();
			log.debug("Register DestinationDataProvider");
			// Method registerDestinationDataProvider can only be called once,
			// see JCo javadoc: Throws: java.lang.IllegalStateException - if a provider is already registered
			Environment.registerDestinationDataProvider(self);
		}
		return self;
	}

	@Override
	public Properties getDestinationProperties(String destinationName) {
		SapSystemImpl sapSystem = SapSystemImpl.getSystem(destinationName);
		if (sapSystem == null) {
			log.warn("Could not find destination name");
			return null;
		}
		CredentialFactory cf = new CredentialFactory(sapSystem.getAuthAlias(), sapSystem.getUserid(), sapSystem.getPasswd());
		Properties destinationProperties = new Properties();
		// See Javadoc DestinationDataProvider for available properties and their description.
		if (StringUtils.isEmpty(sapSystem.getGroup())) {
			destinationProperties.setProperty(DestinationDataProvider.JCO_ASHOST, sapSystem.getAshost());
			destinationProperties.setProperty(DestinationDataProvider.JCO_SYSNR, sapSystem.getSystemnr());
		} else {
			destinationProperties.setProperty(DestinationDataProvider.JCO_R3NAME, sapSystem.getR3name());
			destinationProperties.setProperty(DestinationDataProvider.JCO_MSHOST, sapSystem.getMshost());
			destinationProperties.setProperty(DestinationDataProvider.JCO_MSSERV, sapSystem.getMsserv());
			destinationProperties.setProperty(DestinationDataProvider.JCO_GROUP, sapSystem.getGroup());
		}
		destinationProperties.setProperty(DestinationDataProvider.JCO_CLIENT, sapSystem.getMandant());
		if(cf.getUsername() != null) {
			if (cf.getPassword()==null) {
				throw new IllegalArgumentException("no password specified for sapSystem ["+sapSystem.getName()+"]");
			}
			destinationProperties.setProperty(DestinationDataProvider.JCO_USER, cf.getUsername());
			destinationProperties.setProperty(DestinationDataProvider.JCO_PASSWD, cf.getPassword());
		}
		destinationProperties.setProperty(DestinationDataProvider.JCO_LANG, sapSystem.getLanguage());
		destinationProperties.setProperty(DestinationDataProvider.JCO_PCS, sapSystem.isUnicode()?"2":"1");
		destinationProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, ""+sapSystem.getMaxConnections());

		if(sapSystem.isSncEnabled()) {
			destinationProperties.setProperty(DestinationDataProvider.JCO_SNC_MODE, "1");
			destinationProperties.setProperty(DestinationDataProvider.JCO_SNC_LIBRARY, sapSystem.getSncLibrary());
			destinationProperties.setProperty(DestinationDataProvider.JCO_SNC_QOP, sapSystem.getSncQop()+"");
			destinationProperties.setProperty(DestinationDataProvider.JCO_SNC_SSO, sapSystem.getSncAuthMethod());
			destinationProperties.setProperty(DestinationDataProvider.JCO_SNC_PARTNERNAME, sapSystem.getPartnerName());
			destinationProperties.setProperty(DestinationDataProvider.JCO_SNC_MYNAME, sapSystem.getMyName());
			if(sapSystem.getSncAuthMethod().equals("1")) {
				destinationProperties.setProperty(DestinationDataProvider.JCO_GETSSO2, sapSystem.getSncAuthMethod()); //Automatically order a SSO ticket after logon
				if(sapSystem.getSncSSO2().equals("1"))
					destinationProperties.setProperty(DestinationDataProvider.JCO_MYSAPSSO2, sapSystem.getSncSSO2());
			}
		}

		return destinationProperties;
	}

	@Override
	public void setDestinationDataEventListener(DestinationDataEventListener destinationDataEventListener) {
		this.destinationDataEventListener = destinationDataEventListener;
	}

	@Override
	public boolean supportsEvents() {
		return true;
	}

	public synchronized void updateSystem(SapSystemImpl sapSystem) {
		log.debug("Update {}", sapSystem.getName());
		destinationDataEventListener.updated(sapSystem.getName());
	}

	public synchronized void deleteSystem(SapSystemImpl sapSystem) {
		log.debug("Delete {}", sapSystem.getName());
		destinationDataEventListener.deleted(sapSystem.getName());
	}
}
