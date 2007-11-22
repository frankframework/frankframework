/*
 * $Log: CustomIfsaRRReceiverMDB.java,v $
 * Revision 1.2  2007-11-22 08:48:20  europe\L190409
 * update from ejb-branch
 *
 * Revision 1.1.2.6  2007/11/15 13:44:29  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add logging of success-case
 *
 * Revision 1.1.2.5  2007/11/15 13:01:50  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Remove unused import
 *
 * Revision 1.1.2.4  2007/11/15 13:01:26  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add bit more logging
 *
 * Revision 1.1.2.3  2007/11/14 08:54:33  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Use LogUtil to initialize logging (since this class in in IBIS, not in IFSA, it doesn't use Log4j loaded/initalized from same classloader as IFSA); put logger as protected instance-variable in AbstractBaseMDB class
 *
 * Revision 1.1.2.2  2007/11/02 13:01:09  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add JavaDoc comment
 *
 * Revision 1.1.2.1  2007/11/02 11:47:06  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add custom versions of IFSA MDB Receiver beans, and subclass of IFSA ServiceLocatorEJB
 *
 *
 * $Id: CustomIfsaRRReceiverMDB.java,v 1.2 2007-11-22 08:48:20 europe\L190409 Exp $
 *
 */
package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.provider.RRReceiver;
import com.ing.ifsa.provider.Receiver;
import javax.jms.Message;

/**
 * IfsaReceiverMDB for RequestReply services
 * 
 * @author Tim van der Leeuw
 * @version Id
 */
public class CustomIfsaRRReceiverMDB extends CustomIfsaReceiverMDBAbstractBase {

    public void onMessage(Message msg) {
        if (log.isInfoEnabled()) {
            log.info(">>> onMessage() enter");
        }
        if (!((RRReceiver) receiver).handleMessage(msg)) {
            log.warn("*** onMessage() message was not handled succesfully, rollback transaction");
            getMessageDrivenContext().setRollbackOnly();
        } else {
            if (log.isInfoEnabled()) {
                log.info("Message was handled succesfully");
            }
        }
        if (log.isInfoEnabled()) {
            log.info("<<< onMessage exit");
        }
    }

    protected Receiver createReceiver() {
        return new RRReceiver(serviceLocator);
    }

}
