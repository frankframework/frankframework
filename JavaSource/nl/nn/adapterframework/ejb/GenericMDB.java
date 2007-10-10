/*
 * $Log: GenericMDB.java,v $
 * Revision 1.1.2.4  2007-10-10 14:30:43  europe\L190409
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
import org.apache.log4j.Logger;

/**
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class GenericMDB extends AbstractEJBBase implements MessageDrivenBean, MessageListener {
    private final static Logger log = Logger.getLogger(GenericMDB.class);
    
    protected MessageDrivenContext ejbContext;
    protected JmsListener listener;
    
    public void setMessageDrivenContext(MessageDrivenContext ejbContext) throws EJBException {
        log.info("Received EJB-MDB Context");
        this.ejbContext = ejbContext;
    }
    
    public void ejbCreate() {
        log.info("Creating MDB");
        this.listener = retrieveJmsListener();
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
            this.ejbContext.setRollbackOnly();
        } finally {
            this.listener.destroyThreadContext(threadContext);
        }
    }

    private JmsListener retrieveJmsListener() {
        String adapterName = getContextVariable("adapterName");
        String receiverName = getContextVariable("receiverName");
        return retrieveJmsListener(receiverName, adapterName);
    }

    private JmsListener retrieveJmsListener(String receiverName, String adapterName) {
        IAdapter adapter = config.getRegisteredAdapter(adapterName);
        GenericReceiver receiver = (GenericReceiver) adapter.getReceiverByName(receiverName);
        JmsListener l = (JmsListener) receiver.getListener();
        return l;
    }
    
    
}
