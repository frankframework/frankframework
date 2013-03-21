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
/*
 * $Log: CustomIfsaRRReceiverMDB.java,v $
 * Revision 1.4  2011-11-30 13:51:58  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2007/11/22 08:48:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * $Id$
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
 * @version $Id$
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
