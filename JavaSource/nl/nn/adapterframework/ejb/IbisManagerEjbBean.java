/*
 * $Log: IbisManagerEjbBean.java,v $
 * Revision 1.8  2011-11-30 13:51:57  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2010/09/07 15:55:14  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.5  2007/11/22 08:47:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update from ejb-branch
 *
 * Revision 1.4.2.3  2007/10/29 10:37:25  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix method visibility error
 *
 * Revision 1.4.2.2  2007/10/29 10:29:13  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Refactor: pullup a number of methods to abstract base class so they can be shared with new IFSA Session EJBs
 *
 * Revision 1.4.2.1  2007/10/25 08:36:58  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add shutdown method for IBIS which shuts down the scheduler too, and which unregisters all EjbJmsConfigurators from the ListenerPortPoller.
 * Unregister JmsListener from ListenerPortPoller during ejbRemove method.
 * Both changes are to facilitate more proper shutdown of the IBIS adapters.
 *
 * Revision 1.4  2007/10/16 09:12:27  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge with changes from EJB branch in preparation for creating new EJB brance
 *
 * Revision 1.3  2007/10/15 13:08:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * EJB updates
 *
 * Revision 1.1.2.4  2007/10/15 11:35:51  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix direct retrieving of Logger w/o using the LogUtil
 *
 * Revision 1.1.2.3  2007/10/15 09:51:57  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add back transaction-management to BrowseExecute action
 *
 * Revision 1.1.2.2  2007/10/10 14:30:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/09 16:07:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import nl.nn.adapterframework.configuration.Configuration;
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
 * @version Id
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

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
		ibisManager.handleAdapter(action, adapterName, receiverName, commandIssuedBy);
    }

    public void startIbis() {
		ibisManager.startIbis();
    }

    public void shutdownIbis() {
		ibisManager.shutdownIbis();
		ibisContext.destroyConfig();
    }
    
    public void startAdapters() {
		ibisManager.startAdapters();
    }

    public void stopAdapters() {
		ibisManager.stopAdapters();
    }

    public void startAdapter(IAdapter adapter) {
		ibisManager.startAdapter(adapter);
    }

    public void stopAdapter(IAdapter adapter) {
		ibisManager.stopAdapter(adapter);
    }

    public void loadConfigurationFile(String configurationFile) {
		ibisManager.loadConfigurationFile(configurationFile);
    }

    public String getDeploymentModeString() {
        return ibisManager.getDeploymentModeString();
    }

    public int getDeploymentMode() {
        return ibisManager.getDeploymentMode();
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

}
