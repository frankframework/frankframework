/*
 * $Log: SapSystem.java,v $
 * Revision 1.1  2004-06-22 06:56:44  L190409
 * First version of SAP package
 *
 *
 */
package nl.nn.adapterframework.sap;

import com.sap.mw.jco.*;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.*;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
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
 * <tr><td>{@link #setServiceOffset(int) serviceOffset}</td><td>number added to systemNr to find corresponding RFC service number</td><td>3300</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 * @since 4.1.1
 */
public class SapSystem extends GlobalListItem {
	public static final String version="$Id: SapSystem.java,v 1.1 2004-06-22 06:56:44 L190409 Exp $";

	private int maxConnections = 10;

	private String gwhost = "10.241.14.4";	// The application server where the RFC-destination is registerd

	private String mandant	= "100";	 	// mandant
	private String userid   = null;
	private String passwd   = null;
	private String language = "NL";
	private String systemnr = "00";

	private int serviceOffset = 3300;

	private IRepository repository = null;

	/**
	 * Retrieve a SapSystem from the list of systems.
	 * @param name
	 * @return
	 */
	public static SapSystem getSystem(String name) {
		return (SapSystem)getItem(name);
	}

	public void configure() {
		try {
			log.debug("creating client pool for SapSystem ["+getName()+"]");
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
			log.debug("creating repository for SapSystem ["+getName()+"]");
			repository = JCO.createRepository(getName()+"-repository", getName());
		} catch (Exception e) {
			log.error("exception configuring SapSystem ["+getName()+"]");
		}
		if (repository == null) {
			throw new NullPointerException("SapSystem ["+getName()+"] cannot create repository");
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


	public IRepository getRepository() {
		return repository;
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

	/**
	 * @return
	 */
	public String getGwhost() {
		return gwhost;
	}

	/**
	 * @return
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @return
	 */
	public String getMandant() {
		return mandant;
	}

	/**
	 * @return
	 */
	public String getPasswd() {
		return passwd;
	}

	/**
	 * @return
	 */
	public String getSystemnr() {
		return systemnr;
	}

	/**
	 * @return
	 */
	public String getUserid() {
		return userid;
	}

	/**
	 * @param string
	 */
	public void setGwhost(String string) {
		gwhost = string;
	}

	/**
	 * @param string
	 */
	public void setLanguage(String string) {
		language = string;
	}

	/**
	 * @param string
	 */
	public void setMandant(String string) {
		mandant = string;
	}


	/**
	 * @param string
	 */
	public void setPasswd(String string) {
		passwd = string;
	}

	/**
	 * @param string
	 */
	public void setSystemnr(String string) {
		systemnr = string;
	}

	/**
	 * @param string
	 */
	public void setUserid(String string) {
		userid = string;
	}

	/**
	 * @return
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * @param i
	 */
	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	/**
	 * @return
	 */
	public int getServiceOffset() {
		return serviceOffset;
	}

	/**
	 * @param i
	 */
	public void setServiceOffset(int i) {
		serviceOffset = i;
	}

}
