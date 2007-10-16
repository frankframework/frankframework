/*
 * $Log: EjbDelegatingIbisManager.java,v $
 * Revision 1.5  2007-10-16 09:12:27  europe\M00035F
 * Merge with changes from EJB branch in preparation for creating new EJB brance
 *
 * Revision 1.4  2007/10/16 08:31:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed xpath dependency
 * removed loading of configuration name from configuration file
 *
 * Revision 1.3  2007/10/15 13:08:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * EJB updates
 *
 * Revision 1.1.2.9  2007/10/15 11:35:51  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix direct retrieving of Logger w/o using the LogUtil
 *
 * Revision 1.1.2.8  2007/10/15 09:51:57  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add back transaction-management to BrowseExecute action
 *
 * Revision 1.1.2.7  2007/10/15 09:20:16  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Update logging
 *
 * Revision 1.1.2.6  2007/10/12 14:29:31  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Several fixes and improvements to get EJB deployment mode running
 *
 * Revision 1.1.2.5  2007/10/12 09:45:42  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add 'XPathUtil' interface with multiple implementations (both direct XPath API using, and indirect Transform API using) and remove the code from the EjbDelegatingIbisManager
 *
 * Revision 1.1.2.4  2007/10/10 14:30:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/10 09:48:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class EjbDelegatingIbisManager implements IbisManager, BeanFactoryAware {
    private final static Logger log = LogUtil.getLogger(EjbDelegatingIbisManager.class);
    
    private static final String FACTORY_BEAN_ID = "&ibisManagerEjb";
    private static final String JNDI_NAME_PREFIX = "java:comp/ejb/IbisManager";
    
    private IbisManager ibisManager;
    private BeanFactory beanFactory;
    private PlatformTransactionManager transactionManager;
    
    protected synchronized IbisManager getIbisManager() {
        if (this.ibisManager == null) {
        	// TODO: Set JNDI name in Spring Context and retrieve EJB
        	// directly instead of first retrieving factory, tweaking factory,
        	// creating the bean.
        	
            // Look it up via EJB, using JNDI Name based on configuration name
            LocalStatelessSessionProxyFactoryBean factoryBean = 
                    (LocalStatelessSessionProxyFactoryBean) beanFactory.getBean(FACTORY_BEAN_ID);
            String beanJndiName = JNDI_NAME_PREFIX;
            factoryBean.setJndiName(beanJndiName);
            this.ibisManager = (IbisManager) factoryBean.getObject();
            log.info("Looked up IbisManagerEjb at JNDI location '" + beanJndiName + "'");
        }
        return this.ibisManager;
    }
    
    public Configuration getConfiguration() {
        IbisManager mngr = getIbisManager();
        if (mngr == null) {
            log.error("Cannot look up the configuration when the IbisManager is not set");
            return null;
        } else {
            Configuration cfg = mngr.getConfiguration();
            if (cfg == null) {
                log.error("Retrieved null configuration object from real IbisManager");
            } else {
                log.info("Configuration retrieved from real IbisManager: configuration-name '"
                        + cfg.getConfigurationName() + "', nr of adapters: "
                        + cfg.getRegisteredAdapters().size());
            }
            return cfg;
        }
    }

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        getIbisManager().handleAdapter(action, adapterName, receiverName, commandIssuedBy);
    }

    public void startIbis() {
        // Not implemented for this case, since the Ibis will be auto-started from EJB container
    }

    public void startAdapters() {
        getIbisManager().startAdapters();
    }

    public void stopAdapters() {
        getIbisManager().stopAdapters();
    }

    public void startAdapter(IAdapter adapter) {
        getIbisManager().startAdapter(adapter);
    }

    public void stopAdapter(IAdapter adapter) {
        getIbisManager().stopAdapter(adapter);
    }

    public void loadConfigurationFile(String configurationFile) {
    	// Do not delegate to EJB; EJB initializes itself.
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public String getDeploymentModeString() {
        return IbisManager.DEPLOYMENT_MODE_EJB_STRING;
    }

    public int getDeploymentMode() {
        return IbisManager.DEPLOYMENT_MODE_EJB;
    }

    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
