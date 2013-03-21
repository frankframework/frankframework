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
package nl.nn.adapterframework.ejb;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;

import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.GenericReceiver;

/**
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
public class GenericMDB extends AbstractListenerConnectingEJB implements MessageDrivenBean, MessageListener {
    
    protected MessageDrivenContext ejbContext;
    
    public void setMessageDrivenContext(MessageDrivenContext ejbContext) throws EJBException {
        log.info("Received EJB-MDB Context");
        this.ejbContext = ejbContext;
    }
    
    public void ejbCreate() {
        log.info("Creating MDB");
        onEjbCreate();
    }
    
    public void ejbRemove() throws EJBException {
        log.info("Removing MDB");
        onEjbRemove();
    }

    public void onMessage(Message message) {
        try {
            // Code is not thread-safe but the same instance
            // should be looked up always so there's no point
            // in locking
            if (this.listener == null) {
                this.listener = retrieveListener();
            }

            IMessageHandler handler = this.listener.getHandler();
            handler.processRawMessage(listener,message);
        } catch (ListenerException ex) {
            log.error(ex, ex);
            listener.getExceptionListener().exceptionThrown(listener, ex);
            rollbackTransaction();
        }
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.ejb.AbstractEJBBase#getEJBContext()
     */
    protected EJBContext getEJBContext() {
        return this.ejbContext;
    }
    
    
}
