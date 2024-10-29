/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import java.lang.reflect.Field;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.sap.conn.idoc.IDocRepository;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoRepository;

import lombok.Getter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.extensions.sap.ISapSystem;
import org.frankframework.extensions.sap.SapException;
import org.frankframework.util.AppConstants;
import org.frankframework.util.SapSystemListItem;

/**
 * A SapSystem is a provider of repository information and connections to a SAP-system.
 *
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @author  Niels Meijer
 * @since   5.0
 */
public abstract class SapSystemImpl extends SapSystemListItem implements ISapSystem {

	private String host;
	private String ashost;
	private @Getter String systemnr = "00";
	private @Getter String group;
	private @Getter String r3name;
	private String mshost;
	private @Getter int msservOffset = 3600;
	private String gwhost;
	private @Getter int gwservOffset = 3300;
	private @Getter String mandant = "100";
	private @Getter String authAlias= null;
	private @Getter String userid = null;
	private @Getter String passwd = null;
	private @Getter String language = "NL";
	private @Getter boolean unicode = false;
	private @Getter int maxConnections = 10;
	private @Getter int traceLevel = 0;

	private int referenceCount=0;

	//SNC Encryption
	private @Getter boolean sncEnabled = false;
	private @Getter String sncLibrary;
	private @Getter int sncQop = 8;
	private @Getter String myName;
	private @Getter String partnerName;
	private @Getter String sncAuthMethod = "0";
	private @Getter String sncSSO2 = "1";

	/**
	 * Retrieve a SapSystem from the list of systems.
	 */
	public static SapSystemImpl getSystem(String name) {
		return (SapSystemImpl)getItem(name);
	}

	private void clearSystem() {
		SapSystemDataProvider.getInstance().deleteSystem(this);
	}

	private void initSystem() throws SapException {
		try {
			SapSystemDataProvider.getInstance().updateSystem(this);
			if (log.isDebugEnabled() && getTraceLevel()>0) {
				String logPath=AppConstants.getInstance().getProperty("logging.path");
				JCo.setTrace(getTraceLevel(), logPath);
			}
		} catch (Throwable t) {
			throw new SapException(getLogPrefix()+"exception initializing", t);
		}
	}

	public static void configureAll() {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName = it.next();
			SapSystemImpl system = getSystem(systemName);
			system.configure();
		}
	}

	public synchronized void openSystem() throws SapException {
		if (referenceCount++<=0) {
			referenceCount=1;
			log.debug("{}opening system", getLogPrefix());
			initSystem();
			log.debug("{}opened system", getLogPrefix());
		}
	}

	public synchronized void closeSystem() {
		clearCache();
		if (--referenceCount<=0) {
			log.debug("{}reference count [{}], closing system", getLogPrefix(), referenceCount);
			referenceCount=0;
			clearSystem();
			log.debug("{}closed system", getLogPrefix());
		} else {
			log.debug("{}reference count [{}], waiting for other references to close", getLogPrefix(), referenceCount);
		}
	}

	public static void openSystems() throws SapException {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName = it.next();
			SapSystemImpl system = getSystem(systemName);
			system.openSystem();
		}
	}

	public static void closeSystems() {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName = it.next();
			SapSystemImpl system = getSystem(systemName);
			system.closeSystem();
		}
	}

	public void clearCache() {
		log.debug("start clearing cache of SapSystem [{}]", getName());
		try {
			JCoRepository jcoRepository = getJcoRepository();
			jcoRepository.clear();
		} catch (JCoException e) {
			log.warn("cannot clear function template cache",e);
		}
		log.debug("end clearing cache of SapSystem [{}]", getName());
	}

	public JCoDestination getDestination() throws JCoException {
		JCoDestination destination = JCoDestinationManager.getDestination(getName());
		return destination;
	}

	@Override
	public String getDestinationAsString() {
		try {
			return getDestination().toString();
		} catch (JCoException exception) {
			return null;
		}
	}

	public JCoRepository getJcoRepository() throws JCoException {
		return getDestination().getRepository();
	}

	public synchronized IDocRepository getIDocRepository() throws JCoException {
		return JCoIDoc.getIDocRepository(getDestination());
	}

	public String getLogPrefix() {
		return "SapSystem ["+getName()+"] ";
	}

	@Override
	public String toString() {
		//return ToStringBuilder.reflectionToString(this);
		return (new ReflectionToStringBuilder(this) {
			@Override
			protected boolean accept(Field f) {
				return super.accept(f) && !"passwd".equals(f.getName());
			}
		}).toString();
	}

	/** Default value for ashost, gwhost and mshost (i.e. when ashost, gwhost and mshost are all the same, only host needs to be specified) */
	@Override
	public void setHost(String host) {
		this.host = host;
	}

	/** SAP application server */
	@Override
	public void setAshost(String ashost) {
		this.ashost = ashost;
	}

	public String getAshost() {
		if (StringUtils.isEmpty(ashost)) {
			return host;
		}
		return ashost;
	}

	/**
	 * SAP system nr
	 * @ff.default 00
	 */
	@Override
	public void setSystemnr(String string) {
		systemnr = string;
	}

	/** Group of SAP application servers, when specified logon group will be used and r3name and mshost need to be specified instead of ashost */
	@Override
	public void setGroup(String group) {
		this.group = group;
	}

	/** System ID of the SAP system */
	@Override
	public void setR3name(String r3name) {
		this.r3name = r3name;
	}

	/** SAP message server */
	@Override
	public void setMshost(String mshost) {
		this.mshost = mshost;
	}

	public String getMshost() {
		if (StringUtils.isEmpty(mshost)) {
			return host;
		}
		return mshost;
	}

	/**
	 * String value of sum of msservOffset and systemnr.
	 */
	public String getMsserv() {
		return String.valueOf(getMsservOffset() + Integer.parseInt(getSystemnr()));
	}

	/**
	 * Number added to systemNr to find corresponding message server port
	 * @ff.default 3600
	 */
	@Override
	public void setMsservOffset(int i) {
		msservOffset = i;
	}

	/** Gateway host */
	@Override
	public void setGwhost(String string) {
		gwhost = string;
	}

	public String getGwhost() {
		if (StringUtils.isEmpty(gwhost)) {
			return host;
		}
		return gwhost;
	}

	/**
	 * String value of sum of gwservOffset and systemnr.
	 */
	public String getGwserv() {
		return String.valueOf(getGwservOffset() + Integer.parseInt(getSystemnr()));
	}

	/**
	 * Number added to systemNr to find corresponding gateway port
	 * @ff.default 3300
	 */
	@Override
	public void setGwservOffset(int i) {
		gwservOffset = i;
	}

	/**
	 * Mandant i.e. 'destination'
	 * @ff.default 100
	 */
	@Override
	public void setMandant(String string) {
		mandant = string;
	}

	/** Alias to obtain userid and password */
	@Override
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	/** Userid used in the connection */
	@Override
	public void setUserid(String string) {
		userid = string;
	}

	/** Passwd used in the connection */
	@Override
	public void setPasswd(String string) {
		passwd = string;
	}

	/**
	 * Language indicator
	 * @ff.default NL
	 */
	@Override
	public void setLanguage(String string) {
		language = string;
	}

	/**
	 * If set <code>true</code> the SAP system is interpreted as Unicode SAP system, otherwise as non-Unicode (only applies to SapListeners, not to SapSenders)
	 * @ff.default false
	 */
	@Override
	public void setUnicode(boolean b) {
		unicode = b;
	}

	/**
	 * Maximum number of connections that may connect simultaneously to the SAP system
	 * @ff.default 10
	 */
	@Override
	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	/**
	 * Trace level (effective only when logging level is debug). 0=none, 10= maximum
	 * @ff.default 0
	 */
	@Override
	public void setTraceLevel(int i) {
		traceLevel = i;
	}

	/**
	 * Enable or disable SNC
	 * @ff.default false
	 */
	@Override
	public void setSncEnabled(boolean sncEnabled) {
		this.sncEnabled = sncEnabled;
	}

	/** Path where the SNC library has been installed */
	@Override
	public void setSncLibrary(String sncLibPath) {
		this.sncLibrary = sncLibPath;
	}

	/**
	 * SNC Quality of Protection. 1: Authentication only, 2: Authentication and integrity protection, 3: Authentication, integrity and privacy protection (encryption), 8: Global default configuration, 9: Maximum protection
	 * @ff.default 8
	 */
	@Override
	public void setSncQop(int qop) throws ConfigurationException {
		if(qop < 1 || qop > 9) {
			throw new ConfigurationException("SNC QOP cannot be smaller then 0 or larger then 9");
		}
		this.sncQop = qop;
	}

	/** Own SNC name of the caller. For example: p:CN=MyUserID, O=ACompany, C=EN */
	@Override
	public void setMyName(String myName) {
		this.myName = myName;
	}

	/** SNC name of the communication partner server. For example: p:CN=SID, O=ACompany, C=EN */
	@Override
	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}

	/**
	 * When using SNC, this specifies if SNC should authenticate via SSO or a username/password combination. 1=SSO, 0=username/password
	 * @ff.default 0
	 */
	@Override
	public void setSncAuthMethod(String sncAuthMethod) {
		this.sncAuthMethod = sncAuthMethod;
	}

	/**
	 * Use SAP Cookie Version 2 as logon ticket for SSO based authentication
	 * @ff.default 1
	 */
	@Override
	public void setSncSSO2(String sncSSO2) {
		this.sncSSO2 = sncSSO2;
	}

	@Override
	@Deprecated(forRemoval = true, since = "7.8.0")
	@ConfigurationWarning("setServiceOffset not used in JCo3")
	public void setServiceOffset(int i) {
		log.warn("setServiceOffset not used in JCo3");
	}
}
