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

import com.ing.ifsa.exceptions.ConnectionException;
import com.ing.ifsa.provider.Receiver;
import com.ing.ifsa.provider.ServiceLocator;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.jms.Message;
import javax.jms.MessageListener;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;


/**
 * Abstract base class for custom replacement of IFSA FF/RR Receiver MDB.
 * 
 * The reason for the custom subclass is to be able to create an instance
 * of a subclassed ServiceLocatorEJB which looks up a bean of our own
 * naming convention instead of a bean per IFSA Service name.
 * 
 * @author Tim van der Leeuw
 * @version $Id$
 */
public abstract class CustomIfsaReceiverMDBAbstractBase implements MessageDrivenBean, MessageListener {
    protected final Logger log = LogUtil.getLogger(this);
    /**
     * Service Locator instance: Should be per-instance to avoid unwanted
     * EJB aliasing effects.
     */
    protected ServiceLocator serviceLocator = createServiceLocator();
    
    protected MessageDrivenContext ejbContext;
    protected Receiver receiver;
    
    /**
     * Create ServiceLocator instance.
     * 
     * @return new instance of CustomIfsaServiceLocatorEJB
     */
    protected ServiceLocator createServiceLocator() {
        return new CustomIfsaServiceLocatorEJB();
    }
    
    /**
     * Creating MDB
     * 
     * @throws javax.ejb.CreateException
     * @throws javax.ejb.EJBException
     */
    public void ejbCreate() throws CreateException, EJBException {
        if(log.isInfoEnabled()) {
            log.info(">>> ejbCreate()");
        }
        try {
            receiver = createReceiver();
            receiver.connect();
        }
        catch(ConnectionException e) {
            log.fatal("Connection failed", e);
            throw new CreateException(e.toString());
        }
        if(log.isInfoEnabled()) {
            log.info("<<< ejbCreate");
        }
    }

    /**
     * Removing MDB
     * 
     * @throws javax.ejb.EJBException
     */
    public void ejbRemove() throws EJBException {
        if(log.isInfoEnabled()) {
            log.info(">>> ejbRemove()");
        }
    }

    public MessageDrivenContext getMessageDrivenContext() {
        return ejbContext;
    }

    public void setMessageDrivenContext(MessageDrivenContext ctx) throws EJBException {
        ejbContext = ctx;
    }

    /**
     * onMessage is abstract with two identical implementations, simply because
     * the IFSA Receiver abstract class doesn't contain an abstract definition of
     * the method to process the request.
     * 
     * @param msg JMS Message to process as IFSA Request.
     */
    public abstract void onMessage(Message msg);

    /**
     * Abstract method to create Receiver subclass; used in subclasses
     * to create the FFReceiver or FFReceiver.
     * 
     * @return
     */
    protected abstract Receiver createReceiver();

}
