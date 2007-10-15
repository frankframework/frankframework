/*
 * $Log: GenericMDB.java,v $
 * Revision 1.3  2007-10-15 13:08:38  europe\L190409
 * EJB updates
 *
 * Revision 1.1.2.6  2007/10/15 11:35:51  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix direct retrieving of Logger w/o using the LogUtil
 *
 * Revision 1.1.2.5  2007/10/12 11:53:42  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add variable to indicate to MDB if it's transactions are container-managed, or bean-managed
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

import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Logger;
import org.springframework.jndi.JndiLookupFailureException;

/**
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class GenericMDB extends AbstractEJBBase implements MessageDrivenBean, MessageListener {
    private final static Logger log = LogUtil.getLogger(GenericMDB.class);
    
    protected MessageDrivenContext ejbContext;
    protected JmsListener listener;
    protected boolean containerManagedTransactions;
    
    public void setMessageDrivenContext(MessageDrivenContext ejbContext) throws EJBException {
        log.info("Received EJB-MDB Context");
        this.ejbContext = ejbContext;
    }
    
    public void ejbCreate() {
        log.info("Creating MDB");
        this.listener = retrieveJmsListener();
        this.containerManagedTransactions = retrieveTransactionType();
    }
    
    public void ejbRemove() throws EJBException {
        log.info("Removing MDB");
    }

    public void onMessage(Message rawMessage) {
        Map threadContext = new HashMap();
        try {
            // Code is not thread-safe but the same instance
            // should be looked up always so there's no point
            // in locking
            if (this.listener == null) {
                this.listener = retrieveJmsListener();
            }

            GenericReceiver receiver = (GenericReceiver) this.listener.getHandler();
            this.listener.populateThreadContext(threadContext, null);
            receiver.processRawMessage(listener, rawMessage, threadContext);
            throw new UnsupportedOperationException("Not supported yet.");
        } catch (ListenerException ex) {
            log.error(ex, ex);
            rollbackTransaction();
        } finally {
            this.listener.destroyThreadContext(threadContext);
        }
    }

    protected boolean retrieveTransactionType() {
        try {
            Boolean txType = (Boolean) getContextVariable("containerTransactions");
            if (txType == null) {
                log.warn("Value of variable 'containerTransactions' in Bean JNDI context is null, assuming bean-managed transactions");
                return false;
            } else {
                return txType.booleanValue();
            }
        } catch (JndiLookupFailureException e) {
            log.error("Cannot look up variable 'containerTransactions' in Bean JNDI context; assuming bean-managed transactions", e);
            return false;
        }
    }

    protected JmsListener retrieveJmsListener() {
        String adapterName = (String) getContextVariable("adapterName");
        String receiverName = (String) getContextVariable("receiverName");
        return retrieveJmsListener(receiverName, adapterName);
    }

    protected JmsListener retrieveJmsListener(String receiverName, String adapterName) {
        IAdapter adapter = config.getRegisteredAdapter(adapterName);
        GenericReceiver receiver = (GenericReceiver) adapter.getReceiverByName(receiverName);
        JmsListener l = (JmsListener) receiver.getListener();
        return l;
    }

    protected void rollbackTransaction() throws IllegalStateException {
        if (containerManagedTransactions) {
            this.ejbContext.setRollbackOnly();
        } else {
            try {
                this.ejbContext.getUserTransaction().setRollbackOnly();
            } catch (Exception ex) {
                log.error("Cannot roll back user-transactions, must be using container-managed transactions without being properly configured for it?", ex);
                // Try the container-maanged way
                try {
                    this.ejbContext.setRollbackOnly();
                } catch (IllegalStateException e) {
                    log.error("After failing to rolll back user-transaction, also failing to roll back container-transaction.", e);
                }
                throw new IllegalStateException("Cannot roll back user-transaction; must be using container-managed transactions? Error-message: ["
                        + ex.getMessage() + "]");
            }
        }
    }
    
    
}
