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
package nl.nn.adapterframework.extensions.sap.jco2;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.sap.mw.idoc.IDoc;
import com.sap.mw.idoc.jco.JCoIDoc;
import com.sap.mw.jco.IRepository;
import com.sap.mw.jco.JCO;

import nl.nn.adapterframework.extensions.sap.ISapSystem;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.GlobalListItem;
/**
 * A SapSystem is a provider of repository information and connections to a SAP-system.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the System. SAP-related Ibis objects refer to SapSystems by setting their SystemName-attribute to this value</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>maximum number of connections that may connect simultaneously to the SAP system</td><td>10</td></tr>
 * <tr><td>{@link #setGwhost(String) gwhost}</td><td>name of the SAP-application server to connect to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMandant(String) mandant}</td><td>Mandant i.e. 'client'</td><td>100</td></tr>
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
 * @since 4.1.1
 */
public class SapSystem extends GlobalListItem  implements ISapSystem, JCO.ServerStateChangedListener  {

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

	private IRepository jcoRepository = null;
	private IDoc.Repository idocRepository = null;

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

	private void initSystem() throws SapException {
		try {
			if (log.isDebugEnabled() && getTraceLevel()>0) {
				String logPath=AppConstants.getInstance().getResolvedProperty("logging.path");
				JCO.setTracePath(logPath);
				JCO.setTraceLevel(getTraceLevel());
			}
			clearSystem();
			log.debug(getLogPrefix()+"creating ClientPool");
			
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUserid(), getPasswd());
			
			// Add a connection pool to the specified system
			//    The pool will be saved in the pool list to be used
			//    from other threads by JCO.getClient(SID).
			//    The pool must be explicitely removed by JCO.removeClientPool(SID)
			JCO.addClientPool( getName(),         // Alias for this pool
							   getMaxConnections(),          // Max. number of connections
								getMandant(),
								cf.getUsername(),
								cf.getPassword(),
								getLanguage(),
								getGwhost(),
								getSystemnr());
			// Create a new repository
			//    The repository caches the function and structure definitions
			//    to be used for all calls to the system SID. The creation of
			//    redundant instances cause performance and memory waste.
			log.debug(getLogPrefix()+"creating repository");
			jcoRepository = JCO.createRepository(getName()+"-Repository", getName());
		} catch (Throwable t) {
			throw new SapException(getLogPrefix()+"exception initializing", t);
		}
		if (jcoRepository == null) {
			throw new SapException(getLogPrefix()+"could not create repository");
		}
	}

	@Override
	public void configure() {
		if (log.isDebugEnabled()) {
			JCO.removeServerStateChangedListener(this); // to avoid double logging when restarted
			JCO.addServerStateChangedListener(this);
		}
		log.info(getLogPrefix()+"JCo version ["+JCO.getVersion()+"] on middleware ["+JCO.getMiddlewareLayer()+"] version ["+JCO.getMiddlewareVersion()+"]");
	}

	public static void configureAll() {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
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
		clearCache();
		if (--referenceCount<=0) {
			log.debug(getLogPrefix()+"reference count ["+referenceCount+"], closing system");
			referenceCount=0;
			clearSystem();
			jcoRepository=null;
			log.debug(getLogPrefix()+"closed system");
		} else {
			log.debug(getLogPrefix()+"reference count ["+referenceCount+"], waiting for other references to close");
		}
	}

	public static void openSystems() throws SapException {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName=it.next();
			SapSystem system = getSystem(systemName);
			system.openSystem();
		}
	}

	public static void closeSystems() {
		for(Iterator<String> it = getRegisteredNames(); it.hasNext();) {
			String systemName=it.next();
			SapSystem system = getSystem(systemName);
			system.closeSystem();
		}
	}

	public void clearCache() {
		log.debug("start clearing cache of SapSystem ["+getName()+"]");
		IRepository jcoRepository = getJcoRepository();
		String[] cachedFunctionInterfaces=jcoRepository.getCachedFunctionInterfaces();
		if (cachedFunctionInterfaces!=null) {
			for (int i=0;i<cachedFunctionInterfaces.length;i++) {
				jcoRepository.removeFunctionInterfaceFromCache(cachedFunctionInterfaces[i]);
			}
		}
		String[] cachedStructureDefinitions=jcoRepository.getCachedStructureDefinitions();
		if (cachedStructureDefinitions!=null) {
			for (int i=0;i<cachedStructureDefinitions.length;i++) {
				jcoRepository.removeStructureDefinitionFromCache(cachedStructureDefinitions[i]);
			}
		}
		log.debug("end clearing cache of SapSystem ["+getName()+"]");
	}


  	public JCO.Client getClient() {
		// Get a client from the pool
		return JCO.getClient(getName());
  	}
	// after use client should be released into the pool by calling releaseClient(client);
  	
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

	@Override
	public void serverStateChangeOccurred(JCO.Server server, int old_state, int new_state) {
		log.debug(getLogPrefix()+"a thread of Server [" + server.getProgID() + "] changed state from ["
				+stateToString(old_state)+"] to ["+stateToString(new_state)+"]");
	}


	public IRepository getJcoRepository() {
		return jcoRepository;
	}
	public synchronized IDoc.Repository getIDocRepository() {
		if (idocRepository==null) {
			idocRepository = JCoIDoc.createRepository(getName()+"-IDocRepository", getName());
		}
		return idocRepository;
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
    
	@Override
	public String toString() {
		//return ToStringBuilder.reflectionToString(this);
		return (new ReflectionToStringBuilder(this) {
			@Override
			protected boolean accept(Field f) {
				return super.accept(f) && !f.getName().equals("passwd");
			}
		}).toString();	}

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

	@Override
	public void setGwhost(String string) {
		gwhost = string;
	}

	@Override
	public void setLanguage(String string) {
		language = string;
	}

	@Override
	public void setMandant(String string) {
		mandant = string;
	}



	@Override
	public void setSystemnr(String string) {
		systemnr = string;
	}


	public int getMaxConnections() {
		return maxConnections;
	}

	@Override
	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	public int getServiceOffset() {
		return serviceOffset;
	}

	@Override
	public void setServiceOffset(int i) {
		serviceOffset = i;
	}

	public int getTraceLevel() {
		return traceLevel;
	}

	@Override
	public void setTraceLevel(int i) {
		traceLevel = i;
	}

	public boolean isUnicode() {
		return unicode;
	}

	@Override
	public void setUnicode(boolean b) {
		unicode = b;
	}
}
