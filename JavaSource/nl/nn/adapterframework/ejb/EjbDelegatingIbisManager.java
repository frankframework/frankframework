/*
 * EjbIbisManager.java
 * 
 * Created on 2-okt-2007, 10:06:39
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.ejb;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author m00035f
 */
public class EjbDelegatingIbisManager implements IbisManager, BeanFactoryAware {
    private final static Logger log = Logger.getLogger(EjbDelegatingIbisManager.class);
    
    private static final String FACTORY_BEAN_ID = "@ibisManagerEjb";
    private static final String JNDI_NAME_PREFIX = "ejb/ibis/IbisManager/";
    
    private final static String CONFIG_NAME_XPATH = "/child::*/@configurationName";
    
    private String configurationName;
    private IbisManager ibisManager;
    private BeanFactory beanFactory;
    
    protected synchronized IbisManager getIbisManager() {
        if (this.ibisManager == null) {
            // Look it up via EJB, using JNDI Name based on configuration name
            LocalStatelessSessionProxyFactoryBean factoryBean = 
                    (LocalStatelessSessionProxyFactoryBean) beanFactory.getBean(FACTORY_BEAN_ID);
            String beanJndiName = JNDI_NAME_PREFIX + configurationName.replace(' ', '-');
            factoryBean.setJndiName(beanJndiName);
            this.ibisManager = (IbisManager) factoryBean.getObject();
            log.info("Looked up IbisManagerEjb at JNDI location '" + beanJndiName + "'");
        }
        return this.ibisManager;
    }
    
    public Configuration getConfiguration() {
        return getIbisManager().getConfiguration();
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
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            InputSource inputSource = new InputSource(configurationFile);
            NodeList nodes = (NodeList) xpath.evaluate(CONFIG_NAME_XPATH, inputSource, XPathConstants.NODESET);
            Node item = nodes.item(0);
            setConfigurationName(item.getNodeValue());
            log.info("Extracted configuration-name '" + getConfigurationName()
                    + "' from configuration-file '" + configurationFile + "'");
        } catch (Exception ex) {
            log.error("Error retrieving configuration-name from configuration file '" +
                    configurationFile + "'", ex);
        }
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
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

}
