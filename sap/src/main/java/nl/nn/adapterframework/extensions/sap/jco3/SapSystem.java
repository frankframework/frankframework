/*
   Copyright 2013, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.sap.jco3;

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.extensions.sap.ISapSystemJco3;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.extensions.sap.SapSystemFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.GlobalListItem;
/**
 * A SapSystem is a provider of repository information and connections to a SAP-system.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the System. SAP-related Ibis objects refer to SapSystems by setting their SystemName-attribute to this value</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHost(String) host}</td><td>default value for ashost, gwhost and mshost (i.e. when ashost, gwhost and mshost are all the same, only host needs to be specified)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAshost(String) ashost}</td><td>SAP application server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSystemnr(String) systemnr}</td><td>SAP system nr</td><td>00</td></tr>
 * <tr><td>{@link #setGroup(String) group}</td><td>Group of SAP application servers, when specified logon group will be used and r3name and mshost need to be specified instead of ashost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setR3name(String) r3name}</td><td>System ID of the SAP system</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMshost(String) mshost}</td><td>SAP message server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMsservOffset(int) msservOffset}</td><td>number added to systemNr to find corresponding message server port</td><td>3600</td></tr>
 * <tr><td>{@link #setGwhost(String) gwhost}</td><td>Gateway host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGwservOffset(int) gwservOffset}</td><td>number added to systemNr to find corresponding gateway port</td><td>3300</td></tr>
 * <tr><td>{@link #setMandant(String) mandant}</td><td>Mandant i.e. 'destination'</td><td>100</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias to obtain userid and password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserid(String) userid}</td><td>userid used in the connection</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPasswd(String) passwd}</td><td>passwd used in the connection</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLanguage(String) language}</td><td>Language indicator</td><td>NL</td></tr>
 * <tr><td>{@link #setUnicode(boolean) unicode}</td><td>when set <code>true</code> the SAP system is interpreted as Unicode SAP system, otherwise as non-Unicode (only applies to SapListeners, not to SapSenders)</td><td>false</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>maximum number of connections that may connect simultaneously to the SAP system</td><td>10</td></tr>
 * <tr><td>{@link #setTraceLevel(int) traceLevel}</td><td>trace level (effective only when logging level is debug). 0=none, 10= maximum</td><td>0</td></tr>

 * <tr><td>{@link #setSncEnabled(boolean) sncEnabled}</td><td>Enable or disable SNC</td><td>false</td></tr>
 * <tr><td>{@link #setSncLibrary(String) sncLibPath}</td><td>Path where the SNC library has been installed</td><td></td></tr>
 * <tr><td>{@link #setSncQop(int) qop}</td><td>SNC Quality of Protection. 1: Authentication only, 2: Authentication and integrity protection, 3: Authentication, integrity and privacy protection (encryption), 8: Global default configuration, 9: Maximum protection</td><td>8</td></tr>
 * <tr><td>{@link #setMyName(String) myName}</td><td>Own SNC name of the caller. For example: p:CN=MyUserID, O=ACompany, C=EN</td><td></td></tr>
 * <tr><td>{@link #setPartnerName(String) partnerName}</td><td>SNC name of the communication partner server. For example: p:CN=SID, O=ACompany, C=EN</td><td></td></tr>
 * <tr><td>{@link #setSncAuthMethod(String) sncAuthMethod}</td><td>When using SNC, this specifies if SNC should authenticate via SSO or a username/password combination. 1=SSO, 0=username/password</td><td>0</td></tr>
 * <tr><td>{@link #setSncSSO2(String) sncSSO2}</td><td>Use SAP Cookie Version 2 as logon ticket for SSO based authentication</td><td>1</td></tr>
 * 
 * </table>
 * </p>	
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @author  Niels Meijer
 * @since   5.0
 */
public class SapSystem extends GlobalListItem implements ISapSystemJco3 {

	private String host;
	private String ashost;
	private String systemnr = "00";
	private String group;
	private String r3name;
	private String mshost;
	private int msservOffset = 3600;
	private String gwhost;
	private int gwservOffset = 3300;
	private String mandant = "100";
	private String authAlias= null;
	private String userid = null;
	private String passwd = null;
	private String language = "NL";
	private boolean unicode = false;
	private int maxConnections = 10;
	private int traceLevel = 0;

	private int referenceCount=0;

	//SNC Encryption
	private boolean sncEnabled = false;
	private String sncLibPath;
	private int qop = 8;
	private String myName;
	private String partnerName;
	private String authMethod = "0";
	private String sncSSO2 = "1";

	/**
	 * Retrieve a SapSystem from the list of systems.
	 */
	public static SapSystem getSystem(String name) {
		return (SapSystem)getItem(name);
	}

	private void clearSystem() {
		SapSystemDataProvider.getInstance().deleteSystem(this);
	}

	private void initSystem() throws SapException {
		try {
			SapSystemDataProvider.getInstance().updateSystem(this);
			if (log.isDebugEnabled() && getTraceLevel()>0) {
				String logPath=AppConstants.getInstance().getResolvedProperty("logging.path");
				JCo.setTrace(getTraceLevel(), logPath);
			}
		} catch (Throwable t) {
			throw new SapException(getLogPrefix()+"exception initializing", t);
		}
	}

	public static void configureAll() {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName = it.next();
			SapSystem system = getSystem(systemName);
			system.configure();
		}
	}

	public synchronized void openSystem() throws SapException {
		if (referenceCount++<=0) {
			referenceCount=1;
			log.debug(getLogPrefix()+"opening system");
			initSystem(); 
			log.debug(getLogPrefix()+"opened system");
		}
	}

	public synchronized void closeSystem() {
		clearCache();
		if (--referenceCount<=0) {
			log.debug(getLogPrefix()+"reference count ["+referenceCount+"], closing system");
			referenceCount=0;
			clearSystem();
			log.debug(getLogPrefix()+"closed system");
		} else {
			log.debug(getLogPrefix()+"reference count ["+referenceCount+"], waiting for other references to close");
		}
	}

	public static void openSystems() throws SapException {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName = it.next();
			SapSystem system = getSystem(systemName);
			system.openSystem();
		}
	}

	public static void closeSystems() {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName = it.next();
			SapSystem system = getSystem(systemName);
			system.closeSystem();
		}
	}

	@Override
	public void registerItem(Object dummyParent) {
		super.registerItem(dummyParent);
		SapSystemFactory.getInstance().registerSapSystem(this, getName());
	}

	public void clearCache() {
		log.debug("start clearing cache of SapSystem ["+getName()+"]");
		try {
			JCoRepository jcoRepository = getJcoRepository();
			jcoRepository.clear();
		} catch (JCoException e) {
			log.warn("cannot clear function template cache",e);
		}
		log.debug("end clearing cache of SapSystem ["+getName()+"]");
	}

	public JCoDestination getDestination() throws JCoException {
		JCoDestination destination = JCoDestinationManager.getDestination(getName());
		return destination;
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
				return super.accept(f) && !f.getName().equals("passwd");
			}
		}).toString();
	}

	@Override
	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public void setAshost(String ashost) {
		this.ashost = ashost;
	}

	public String getAshost() {
		if (StringUtils.isEmpty(ashost)) {
			return host;
		} else {
			return ashost;
		}
	}

	@Override
	public void setSystemnr(String string) {
		systemnr = string;
	}

	public String getSystemnr() {
		return systemnr;
	}

	@Override
	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	@Override
	public void setR3name(String r3name) {
		this.r3name = r3name;
	}

	public String getR3name() {
		return r3name;
	}

	@Override
	public void setMshost(String mshost) {
		this.mshost = mshost;
	}

	public String getMshost() {
		if (StringUtils.isEmpty(mshost)) {
			return host;
		} else {
			return mshost;
		}
	}

	/**
	 * String value of sum of msservOffset and systemnr.
	 */
	public String getMsserv() {
		return String.valueOf(getMsservOffset() + Integer.parseInt(getSystemnr()));
	}

	public int getMsservOffset() {
		return msservOffset;
	}

	@Override
	public void setMsservOffset(int i) {
		msservOffset = i;
	}

	@Override
	public void setGwhost(String string) {
		gwhost = string;
	}

	public String getGwhost() {
		if (StringUtils.isEmpty(gwhost)) {
			return host;
		} else {
			return gwhost;
		}
	}

	/**
	 * String value of sum of gwservOffset and systemnr.
	 */
	public String getGwserv() {
		return String.valueOf(getGwservOffset() + Integer.parseInt(getSystemnr()));
	}

	@Override
	public void setGwservOffset(int i) {
		gwservOffset = i;
	}

	public int getGwservOffset() {
		return gwservOffset;
	}

	@Override
	public void setMandant(String string) {
		mandant = string;
	}

	public String getMandant() {
		return mandant;
	}

	@Override
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	@Override
	public void setUserid(String string) {
		userid = string;
	}

	public String getUserid() {
		return userid;
	}

	@Override
	public void setPasswd(String string) {
		passwd = string;
	}

	public String getPasswd() {
		return passwd;
	}

	public String getLanguage() {
		return language;
	}

	@Override
	public void setLanguage(String string) {
		language = string;
	}

	@Override
	public void setUnicode(boolean b) {
		unicode = b;
	}

	public boolean isUnicode() {
		return unicode;
	}

	@Override
	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	@Override
	public void setTraceLevel(int i) {
		traceLevel = i;
	}

	public int getTraceLevel() {
		return traceLevel;
	}

	@Override
	public void setSncEnabled(boolean sncEnabled) {
		this.sncEnabled = sncEnabled;
	}
	public boolean isSncEncrypted() {
		return sncEnabled;
	}

	@Override
	public void setSncLibrary(String sncLibPath) {
		this.sncLibPath = sncLibPath;
	}
	public String getSncLibrary() {
		return sncLibPath;
	}

	@Override
	public void setSncQop(int qop) throws ConfigurationException {
		if(qop < 1 || qop > 9)
			throw new ConfigurationException("SNC QOP cannot be smaller then 0 or larger then 9");

		this.qop = qop;
	}
	public int getSncQop() {
		return qop;
	}

	@Override
	public void setMyName(String myName) {
		this.myName = myName;
	}
	public String getMyName() {
		return myName;
	}

	@Override
	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}
	public String getPartnerName() {
		return partnerName;
	}

	@Override
	public void setSncAuthMethod(String sncAuthMethod) {
		this.authMethod = sncAuthMethod;
	}
	public String getSncAuthMethod() {
		return authMethod;
	}

	@Override
	public void setSncSSO2(String sncSSO2) {
		this.sncSSO2 = sncSSO2;
	}
	public String getSncSSO2() {
		return sncSSO2;
	}

	@Override
	public void setServiceOffset(int i) {
		log.warn("setServiceOffset not used in JCo3");
	}
}
