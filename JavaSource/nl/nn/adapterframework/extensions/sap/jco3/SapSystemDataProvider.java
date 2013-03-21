/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: SapSystemDataProvider.java,v $
 * Revision 1.2  2012-03-12 15:23:00  m00f069
 * Implemented logon group properties
 *
 * Revision 1.1  2012/02/06 14:33:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3;

import java.util.Properties;

import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.axis.utils.StringUtils;
import org.apache.log4j.Logger;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

/**
 * @author  Jaco de Groot
 * @since   5.0
 * @version $Id$
 */
public class SapSystemDataProvider implements DestinationDataProvider {
	private Logger log = LogUtil.getLogger(this);
	private static SapSystemDataProvider self=null;
	private DestinationDataEventListener destinationDataEventListener;

	private int referenceCount=0;

	private SapSystemDataProvider() {
		super();
	}

	public static synchronized SapSystemDataProvider getInstance() {
		if (self==null) {
			self=new SapSystemDataProvider();
		}
		return self;
	}

	public Properties getDestinationProperties(String destinationName) {
		SapSystem sapSystem = SapSystem.getSystem(destinationName);
		if (sapSystem == null) {
			log.warn("Could not find destination name");
			return null;
		} else {
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
			destinationProperties.setProperty(DestinationDataProvider.JCO_USER, cf.getUsername());
			destinationProperties.setProperty(DestinationDataProvider.JCO_PASSWD, cf.getPassword());
			destinationProperties.setProperty(DestinationDataProvider.JCO_LANG, sapSystem.getLanguage());
			destinationProperties.setProperty(DestinationDataProvider.JCO_PCS, sapSystem.isUnicode()?"2":"1");
			destinationProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, ""+sapSystem.getMaxConnections());
			return destinationProperties;
		}
	}

	public void setDestinationDataEventListener(DestinationDataEventListener destinationDataEventListener) {
		this.destinationDataEventListener = destinationDataEventListener;
	}

	public boolean supportsEvents() {
		return true;
	}

	public synchronized void registerSystem(SapSystem sapSytem) throws SapException {
		if (referenceCount++<=0) {
			referenceCount=1;
			log.debug("Register DestinationDataProvider");
			Environment.registerDestinationDataProvider(SapSystemDataProvider.getInstance());
		} else {
			log.debug("Reference count ["+referenceCount+"], registration already done");
		}
		log.debug("Register " + sapSytem.getName());
		destinationDataEventListener.updated(sapSytem.getName());
	}

	public synchronized void unregisterSystem(SapSystem sapSytem) {
		log.debug("Unregister " + sapSytem.getName());
		destinationDataEventListener.deleted(sapSytem.getName());
		if (--referenceCount<=0) {
			referenceCount=0;
			log.debug("Unregister DestinationDataProvider");
			Environment.unregisterDestinationDataProvider(SapSystemDataProvider.getInstance());
		} else {
			log.debug("Reference count ["+referenceCount+"], waiting for other references to unregister");
		}
	}

}
