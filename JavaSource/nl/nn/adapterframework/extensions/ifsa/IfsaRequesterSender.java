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
 * $Log: IfsaRequesterSender.java,v $
 * Revision 1.32  2011-11-30 13:51:58  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.30  2007/10/16 08:21:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added original javadoc
 *
 * Revision 1.29  2007/10/16 08:15:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced switch class for jms and ejb
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

/**
 * {@link ISender} that sends a message to an IFSA service and, in case the messageprotocol is RR (Request-Reply)
 * it waits for an reply-message.
 * <br>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>the ApplicationID, in the form of "IFSA://<i>AppId</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceId(String) serviceId}</td><td>the ServiceID of the service to be called, in the form of "IFSA://<i>ServiceID</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td><td>&nbsp;</td></td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>must be set <code>true</code> for FF senders in transacted mode</td><td>false</td></tr>
 * </table>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>serviceId</td><td>string</td><td>When a parameter with name serviceId is present, it is used at runtime instead of the serviceId specified by the attribute. A lookup of the service for this serviceId will be done at runtime, while the service for the serviceId specified as an attribute will be done at configuration time. Hence, IFSA configuration problems will be detected at runtime for the serviceId specified as a param and at configuration time for the serviceId specified with an attribute</td></tr>
 * <tr><td>occurrence</td><td>string</td><td>The occurrence part of the serviceId (the part between the fourth and the fifth slash). The occurence part of the specified serviceId (either specified by a parameter or an attribute) will be replaced with the value of this parameter at runtime. From "IFSA - Naming Standards.doc": IFSA://SERVICE/&lt;group&gt;/&lt;occurrence&gt;/&lt;service&gt;/&lt;version&gt;[?&lt;parameter=value&gt;]</td></tr>
 * <tr><td>all other parameters</td><td>string</td><td>All parameters (names and values) (except serviceId and occurrence) are passed to the outgoing UDZ (User Defined Zone) object</td></tr>
 * </table>
 *
 * @author  Gerrit van Brakel
 * @since   4.2, switch class: 4.8
 * @version $Id$
 */
public class IfsaRequesterSender extends nl.nn.adapterframework.extensions.ifsa.jms.IfsaRequesterSender {

}
