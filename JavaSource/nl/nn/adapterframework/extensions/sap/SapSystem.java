/*
 * $Log: SapSystem.java,v $
 * Revision 1.7  2005-08-10 12:46:27  europe\L190409
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
package nl.nn.adapterframework.extensions.sap;

import com.sap.mw.jco.*;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.GlobalListItem;

import org.apache.commons.lang.builder.ToStringBuilder;
/**
 * A SapSystem is a provider of repository information and connections to a SAP-system.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the System. SAP-related Ibis objects refer to SapSystems by setting their SystemName-attribute to this value</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>maximum number of connections that may connect simultaneously to the SAP system</td><td>10</td></tr>
 * <tr><td>{@link #setGwhost(String) gwhost}</td><td>name of the SAP-application server to connect to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMandant(String) mandant}</td><td>Mandant i.e. 'client'</td><td>100</td></tr>
 * <tr><td>{@link #setUserid(String) userid}</td><td>userid used in the connection</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPasswd(String) passwd}</td><td>passwd used in the connection</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLanguage(String) language}</td><td>Language indicator</td><td>NL</td></tr>
 * <tr><td>{@link #setSystemnr(String) systemnr}</td><td>SAP system nr</td><td>00</td></tr>
 * <tr><td>{@link #setTraceLevel(int) traceLevel}</td><td>trace level (effective only when logging level is debug). 0=none, 10= maximum</td><td>0</td></tr>
 * <tr><td>{@link #setServiceOffset(int) serviceOffset}</td><td>number added to systemNr to find corresponding RFC service number</td><td>3300</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 * @since 4.1.1
 */
public class SapSystem extends GlobalListItem  implements JCO.ServerStateChangedListener  {
	public static final String version="$RCSfile: SapSystem.java,v $  $Revision: 1.7 $ $Date: 2005-08-10 12:46:27 $";

	private int maxConnections = 10;

	private String gwhost;	// The application server where the RFC-destination is registerd

	private String mandant	= "100";	 	// mandant
	private String userid   = null;
	private String passwd   = null;
	private String language = "NL";
	private String systemnr = "00";
	private int    traceLevel = 0;

	private int serviceOffset = 3300;

	private IRepository repository = null;

	private int referenceCount=0;


	/**
	 * Retrieve a SapSystem from the list of systems.
	 */
	public static SapSystem getSystem(String name) {
		return (SapSystem)getItem(name);
	}

	private void clearSystem() {
		try {
			log.debug(getLogPrefix()+"removing possible existing ClientPool");
			JCO.removeClientPool(getName());
		} catch (Exception e) {
			log.debug(getLogPrefix()+"exception removing ClientPool, probably because it didn't exist",e);
		}
	}

	private void initSystem() {
		try {
			if (log.isDebugEnabled() && getTraceLevel()>0) {
				String logPath=AppConstants.getInstance().getResolvedProperty("logging.path");
				JCO.setTracePath(logPath);
				JCO.setTraceLevel(getTraceLevel());
			}
			clearSystem();
			log.debug(getLogPrefix()+"creating ClientPool");
			// Add a connection pool to the specified system
			//    The pool will be saved in the pool list to be used
			//    from other threads by JCO.getClient(SID).
			//    The pool must be explicitely removed by JCO.removeClientPool(SID)
			JCO.addClientPool( getName(),         // Alias for this pool
							   getMaxConnections(),          // Max. number of connections
								getMandant(),
								getUserid(),
								getPasswd(),
								getLanguage(),
								getGwhost(),
								getSystemnr());
			// Create a new repository
			//    The repository caches the function and structure definitions
			//    to be used for all calls to the system SID. The creation of
			//    redundant instances cause performance and memory waste.
			log.debug(getLogPrefix()+"creating repository");
			repository = JCO.createRepository(getName()+"-repository", getName());
		} catch (Throwable t) {
			log.error(getLogPrefix()+"exception initializing", t);
		}
		if (repository == null) {
			log.error(getLogPrefix()+"could not create repository");
		}
	}

	public void configure() {
		if (log.isDebugEnabled()) {
			JCO.removeServerStateChangedListener(this); // to avoid double logging when restarted
			JCO.addServerStateChangedListener(this);
		}
		log.info(getLogPrefix()+"JCo version ["+JCO.getVersion()+"] on middleware ["+JCO.getMiddlewareLayer()+"] version ["+JCO.getMiddlewareVersion()+"]");
	}
  
  	public synchronized void openSystem() {
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
			repository=null;
			log.debug(getLogPrefix()+"closed system");
		} else {
			log.debug(getLogPrefix()+"reference count ["+referenceCount+"], waiting for other references to close");
		}
	}

  	public JCO.Client getClient() {
		// Get a client from the pool
		return JCO.getClient(getName());
  	}
	// after use client should be relaese into the pool by calling releaseClient(client);
  	
	public void releaseClient(JCO.Client client) {
		// Release the client into the pool
		JCO.releaseClient(client);
	}

	public String stateToString(int state) {
		String result="";
		if ((state & JCO.STATE_STOPPED    ) != 0) result += " STOPPED ";
		if ((state & JCO.STATE_STARTED    ) != 0) result += " STARTED ";
		if ((state & JCO.STATE_LISTENING  ) != 0) result += " LISTENING ";
		if ((state & JCO.STATE_TRANSACTION) != 0) result += " TRANSACTION ";
		if ((state & JCO.STATE_BUSY       ) != 0) result += " BUSY ";
		return result;
	}

	public void serverStateChangeOccurred(JCO.Server server, int old_state, int new_state) {
		log.debug("Server [" + server.getProgID() + "] changed state from ["
				+stateToString(old_state)+"] to ["+stateToString(new_state)+"]");
	}


	public IRepository getRepository() {
		return repository;
	}

	protected String getLogPrefix() {
		return "SapSystem ["+getName()+"] "; 
	}
  
  	/**
  	 * String value of sum of serviceOffset and systemnr.
  	 */
    public String getGwserv() {
    	return String.valueOf(getServiceOffset() + Integer.parseInt(getSystemnr()));
    }
    
	public String toString() {
	  return  ToStringBuilder.reflectionToString(this);

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

	public String getPasswd() {
		return passwd;
	}

	public String getSystemnr() {
		return systemnr;
	}

	public String getUserid() {
		return userid;
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


	public void setPasswd(String string) {
		passwd = string;
	}

	public void setSystemnr(String string) {
		systemnr = string;
	}

	public void setUserid(String string) {
		userid = string;
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

}
