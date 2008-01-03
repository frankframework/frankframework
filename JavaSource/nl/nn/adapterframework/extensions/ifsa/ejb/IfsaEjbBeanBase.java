/*
 * $Log: IfsaEjbBeanBase.java,v $
 * Revision 1.3  2008-01-03 15:44:39  europe\L190409
 * rework port connected listener interfaces
 *
 * Revision 1.2  2007/11/22 08:48:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update from ejb-branch
 *
 * Revision 1.1.2.4  2007/11/15 12:59:51  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add bit more logging
 *
 * Revision 1.1.2.3  2007/11/15 10:27:07  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Add logging of EJB Create / Remove events
 * * Move code up to parent class
 *
 * Revision 1.1.2.2  2007/11/06 12:49:33  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add methods 'populateThreadContext' and 'destroyThreadContext' to interface IPortConnectedListener
 *
 * Revision 1.1.2.1  2007/10/29 12:25:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.ejb.AbstractListenerConnectingEJB;

import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.exceptions.ServiceException;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
abstract public class IfsaEjbBeanBase extends AbstractListenerConnectingEJB implements SessionBean {
    protected SessionContext ejbContext;
    
    public void ejbCreate() throws CreateException {
        log.info("Creating IFSA Handler Session Bean");
        onEjbCreate();
    }
    
    public void ejbRemove() throws EJBException, RemoteException {
        log.info("Removing IFSA Handler Session Bean");
        onEjbRemove();
    }

    protected String processRequest(ServiceRequest request) throws ServiceException {
        log.debug(">>> processRequest() Processing IFSA Request, generic handling");
        Map threadContext = new HashMap();
        try {
//            listener.populateThreadContext(request, threadContext, null);
            String message = listener.getStringFromRawMessage(request, threadContext);
            String id = listener.getIdFromRawMessage(request, threadContext);
            String cid = id;
            String replyText = listener.getHandler().processRequest(listener, cid, message, threadContext);
            if (log.isDebugEnabled()) {
                log.debug("processRequest(): ReplyText=[" + replyText + "]");
            }
            return replyText;
        } catch (ListenerException ex) {
            log.error(ex, ex);
            listener.getExceptionListener().exceptionThrown(listener, ex);
            // Do not invoke rollback, but let IFSA take care of that
            throw new ServiceException(ex);
        } finally {
            log.debug("<<< processRequest() finished generic handling");
//            listener.destroyThreadContext(threadContext);
        }
    }

    public void setSessionContext(SessionContext context) throws EJBException, RemoteException {
        this.ejbContext = context;
    }

    protected EJBContext getEJBContext() {
        return this.ejbContext;
    }

    public void ejbActivate() throws EJBException, RemoteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ejbPassivate() throws EJBException, RemoteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
