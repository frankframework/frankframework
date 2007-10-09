/*
 * $Log: IbisManagerEjbBean.java,v $
 * Revision 1.2  2007-10-09 16:07:37  europe\L190409
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import org.apache.log4j.Logger;

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
    private final static Logger log = Logger.getLogger(IbisManagerEjbBean.class);
    
    SessionContext sessionContext;
    
    public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException {
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
        return manager.getConfiguration();
    }

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        manager.handleAdapter(action, adapterName, receiverName, commandIssuedBy);
    }

    public void startIbis() {
        manager.startIbis();
    }

    public void startAdapters() {
        manager.startAdapters();
    }

    public void stopAdapters() {
        manager.stopAdapters();
    }

    public void startAdapter(IAdapter adapter) {
        manager.startAdapter(adapter);
    }

    public void stopAdapter(IAdapter adapter) {
        manager.stopAdapter(adapter);
    }

    public void loadConfigurationFile(String configurationFile) {
        manager.loadConfigurationFile(configurationFile);
    }

    public String getDeploymentModeString() {
        return manager.getDeploymentModeString();
    }

    public int getDeploymentMode() {
        return manager.getDeploymentMode();
    }

}
