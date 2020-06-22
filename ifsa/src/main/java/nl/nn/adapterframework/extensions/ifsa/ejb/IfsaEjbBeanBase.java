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
package nl.nn.adapterframework.extensions.ifsa.ejb;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.exceptions.ServiceException;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.ejb.AbstractListenerConnectingEJB;
import nl.nn.adapterframework.stream.Message;

/**
 *
 * @author Tim van der Leeuw
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
            Message message = listener.extractMessage(request, threadContext);
            String cid = listener.getIdFromRawMessage(request, threadContext);
            String replyText = listener.getHandler().processRequest(listener, cid, request, message, threadContext).asString();
            if (log.isDebugEnabled()) {
                log.debug("processRequest(): ReplyText=[" + replyText + "]");
            }
            return replyText;
        } catch (IOException | ListenerException ex) {
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
