/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.ejb;

import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * EJB layer around the IbisManager implementation which is defined in the
 * Spring context.
 * 
 * The base-class {@link AbstractEJBBase} takes care of initializing the
 * Spring context in it's static class initialization.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class IbisManagerEjbBean extends AbstractEJBBase implements SessionBean, IbisManager {
    private final static Logger log = LogUtil.getLogger(IbisManagerEjbBean.class);
    
    SessionContext sessionContext;
    
    public IbisManagerEjbBean() {
        super();
        log.info("Created IbisManagerEjbBean instance");
    }
    
    public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException {
        log.info("Set session context for IbisManagerEjb");
        this.sessionContext = sessionContext;
    }
    
    public void ejbCreate() throws CreateException {
        log.info("Creating IbisManagerEjb");
    }
    
    public void ejbRemove() throws EJBException, RemoteException {
        log.info("Removing IbisManagerEjb");
    }

    public void ejbActivate() throws EJBException, RemoteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ejbPassivate() throws EJBException, RemoteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Configuration getConfiguration() {
        return ibisManager.getConfiguration();
    }

    public List<Configuration> getConfigurations() {
        return ibisManager.getConfigurations();
    }

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
		ibisManager.handleAdapter(action, adapterName, receiverName, commandIssuedBy);
    }

    public void shutdownIbis() {
		ibisManager.shutdownIbis();
		ibisContext.destroyConfig();
    }
    
    public void startAdapters(Configuration configuration) {
		ibisManager.startAdapters(configuration);
    }

    public void stopAdapters(Configuration configuration) {
		ibisManager.stopAdapters(configuration);
    }

    public void startAdapter(IAdapter adapter) {
		ibisManager.startAdapter(adapter);
    }

    public void stopAdapter(IAdapter adapter) {
		ibisManager.stopAdapter(adapter);
    }

    public void loadConfigurationFile(ClassLoader classLoader, String basePath, String configurationFile) throws ConfigurationException {
		ibisManager.loadConfigurationFile(classLoader, basePath, configurationFile);
    }

    public PlatformTransactionManager getTransactionManager() {
        return ibisManager.getTransactionManager();
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.ejb.AbstractEJBBase#getEJBContext()
     */
    protected EJBContext getEJBContext() {
        return this.sessionContext;
    }

	public void startConfiguration(Configuration configuration) {
		ibisManager.startConfiguration(configuration);
		
	}

	public Configuration getConfiguration(String configurationName) {
		return ibisManager.getConfiguration(configurationName);
	}

	public IAdapter getRegisteredAdapter(String name) {
		return ibisManager.getRegisteredAdapter(name);
	}

	public List<IAdapter> getRegisteredAdapters() {
		return ibisManager.getRegisteredAdapters();
	}

	public void dumpStatistics(int action) {
		ibisManager.dumpStatistics(action);
	}

	public List<String> getSortedStartedAdapterNames() {
		return ibisManager.getSortedStartedAdapterNames();
	}

}
