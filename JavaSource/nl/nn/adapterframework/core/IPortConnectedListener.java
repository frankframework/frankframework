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
 * $Log: IPortConnectedListener.java,v $
 * Revision 1.6  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2008/01/03 15:41:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework port connected listener interfaces
 *
 * Revision 1.3  2007/11/22 08:37:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.2.2.4  2007/11/15 10:23:37  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add extra methods for allow better configuration via the interface, instead of implementations
 *
 * Revision 1.2.2.3  2007/11/06 13:10:11  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add exception to method signature
 *
 * Revision 1.2.2.2  2007/11/06 12:49:33  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add methods 'populateThreadContext' and 'destroyThreadContext' to interface IPortConnectedListener
 *
 * Revision 1.2.2.1  2007/11/06 09:39:13  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge refactoring/renaming from HEAD
 *
 * Revision 1.2  2007/11/05 13:06:55  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename and redefine methods in interface IListenerConnector to remove 'jms' from names
 *
 * Revision 1.1  2007/11/05 12:15:09  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add new interface for pushing listeners which receive messages from named enpoints, and which require additional glue to make the connection
 *
 * 
 * Created on 5-nov-07
 *
 */
package nl.nn.adapterframework.core;

import java.util.Map;
import javax.jms.Session;

/**
 * Interface extending IPushingListener for listeners which connect to a
 * ListenerPort or other type of named endpoint, from which they receive
 * their messages.
 * 
 * Current implementations are PushingJmsListener and the EJB version of
 * IfsaProviderListener.
 * 
 * @author Tim van der Leeuw
 * @version $Id$
 *
 */
public interface IPortConnectedListener extends IPushingListener {

    public IbisExceptionListener getExceptionListener();
    String getListenerPort();
	IMessageHandler getHandler();

    void setReceiver(IReceiver receiver);
    IReceiver getReceiver();

    IListenerConnector getListenerPortConnector();
}
