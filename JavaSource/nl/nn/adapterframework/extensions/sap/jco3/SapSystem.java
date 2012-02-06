/*
 * $Log: SapSystem.java,v $
 * Revision 1.1  2012-02-06 14:33:04  m00f069
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 * Revision 1.13  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.11  2010/04/26 14:21:58  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attribute unicode (only for SapListeners, not for SapSenders)
 *
 * Revision 1.10  2008/01/29 15:34:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for idocs and for multiple sap system objects
 *
 * Revision 1.9  2007/05/01 14:20:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved exception handling
 *
 * Revision 1.8  2005/12/19 16:44:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added authentication using authentication-alias
 *
 * Revision 1.7  2005/08/10 12:46:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * underflow-correction of reference count
 *
 * Revision 1.6  2005/08/08 09:42:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked SAP classes to provide better refresh of repository when needed
 *
 * Revision 1.5  2005/08/02 13:04:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configurable trace facility
 *
 * Revision 1.4  2004/10/05 10:41:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.3  2004/08/23 13:11:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.2  2004/07/07 13:56:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove ClientPool before adding it
 *
 * Revision 1.1  2004/07/06 07:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SAP functionality to extensions
 *
 * Revision 1.1  2004/06/22 06:56:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3;

import java.util.Iterator;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.GlobalListItem;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.sap.conn.idoc.IDocRepository;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoRepository;
/**
 * A SapSystem is a provider of repository information and connections to a SAP-system.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the System. SAP-related Ibis objects refer to SapSystems by setting their SystemName-attribute to this value</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>maximum number of connections that may connect simultaneously to the SAP system</td><td>10</td></tr>
 * <tr><td>{@link #setGwhost(String) gwhost}</td><td>name of the SAP-application server to connect to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMandant(String) mandant}</td><td>Mandant i.e. 'destination'</td><td>100</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias to obtain userid and password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserid(String) userid}</td><td>userid used in the connection</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPasswd(String) passwd}</td><td>passwd used in the connection</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLanguage(String) language}</td><td>Language indicator</td><td>NL</td></tr>
 * <tr><td>{@link #setSystemnr(String) systemnr}</td><td>SAP system nr</td><td>00</td></tr>
 * <tr><td>{@link #setTraceLevel(int) traceLevel}</td><td>trace level (effective only when logging level is debug). 0=none, 10= maximum</td><td>0</td></tr>
 * <tr><td>{@link #setServiceOffset(int) serviceOffset}</td><td>number added to systemNr to find corresponding RFC service number</td><td>3300</td></tr>
 * <tr><td>{@link #setUnicode(boolean) unicode}</td><td>when set <code>true</code> the SAP system is interpreted as Unicode SAP system, otherwise as non-Unicode (only applies to SapListeners, not to SapSenders)</td><td>false</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public class SapSystem extends GlobalListItem {
	public static final String version="$RCSfile: SapSystem.java,v $  $Revision: 1.1 $ $Date: 2012-02-06 14:33:04 $";

	private int maxConnections = 10;

	private String gwhost;	// The application server where the RFC-destination is registerd

 	private String authAlias= null;
	private String mandant	= "100";	 	// mandant
	private String userid   = null;
	private String passwd   = null;
	private String language = "NL";
	private String systemnr = "00";
	private int    traceLevel = 0;

	private int serviceOffset = 3300;

	private boolean unicode = false;

	private int referenceCount=0;


	/**
	 * Retrieve a SapSystem from the list of systems.
	 */
	public static SapSystem getSystem(String name) {
		return (SapSystem)getItem(name);
	}

	private void clearSystem() {
		SapSystemDataProvider.getInstance().unregisterSystem(this);
	}

	private void initSystem() throws SapException {
		try {
			SapSystemDataProvider.getInstance().registerSystem(this);
			if (log.isDebugEnabled() && getTraceLevel()>0) {
				String logPath=AppConstants.getInstance().getResolvedProperty("logging.path");
				JCo.setTrace(getTraceLevel(), logPath);
			}
		} catch (Throwable t) {
			throw new SapException(getLogPrefix()+"exception initializing", t);
		}
	}

	public static void configureAll() {
		for(Iterator it = getRegisteredNames(); it.hasNext();) {
			String systemName=(String)it.next();
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
		for(Iterator it = getRegisteredNames(); it.hasNext();) {
			String systemName=(String)it.next();
			SapSystem system = getSystem(systemName);
			system.openSystem();
		}
	}

	public static void closeSystems() {
		for(Iterator it = getRegisteredNames(); it.hasNext();) {
			String systemName=(String)it.next();
			SapSystem system = getSystem(systemName);
			system.closeSystem();
		}
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

	/**
	 * String value of sum of serviceOffset and systemnr.
	 */
	public String getGwserv() {
		return String.valueOf(getServiceOffset() + Integer.parseInt(getSystemnr()));
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);

	}

	public String getGwhost() {
		return gwhost;
	}

	public String getLanguage() {
		return language;
	}

	public String getMandant() {
		return mandant;
	}

	public String getSystemnr() {
		return systemnr;
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setUserid(String string) {
		userid = string;
	}

	public String getUserid() {
		return userid;
	}

	public void setPasswd(String string) {
		passwd = string;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setGwhost(String string) {
		gwhost = string;
	}

	public void setLanguage(String string) {
		language = string;
	}

	public void setMandant(String string) {
		mandant = string;
	}

	public void setSystemnr(String string) {
		systemnr = string;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	public int getServiceOffset() {
		return serviceOffset;
	}

	public void setServiceOffset(int i) {
		serviceOffset = i;
	}

	public int getTraceLevel() {
		return traceLevel;
	}

	public void setTraceLevel(int i) {
		traceLevel = i;
	}

	public boolean isUnicode() {
		return unicode;
	}

	public void setUnicode(boolean b) {
		unicode = b;
	}
}
