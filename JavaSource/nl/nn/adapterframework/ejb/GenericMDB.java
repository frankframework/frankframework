/*
 * Created on 28-sep-07
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
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;
import org.apache.log4j.Logger;

/**
 * @author m00035f
 *
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
        } catch (JmsException ex) {
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
