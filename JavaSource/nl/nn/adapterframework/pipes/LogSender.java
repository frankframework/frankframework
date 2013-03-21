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
 * $Log: LogSender.java,v $
 * Revision 1.13  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.11  2009/01/08 17:37:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed NPE in configure()
 *
 * Revision 1.10  2008/12/30 17:01:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.9  2008/11/26 09:38:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Fixed warning message in deprecated classes
 *
 * Revision 1.8  2008/08/07 11:40:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix npe
 *
 * Revision 1.7  2008/08/06 16:38:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved from pipes to senders package
 *
 * Revision 1.6  2007/09/13 09:09:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * return message instead of correlationid
 *
 * Revision 1.5  2007/02/12 14:02:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.4  2006/06/14 09:50:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid null pointer exception when prc==null
 *
 * Revision 1.3  2005/12/28 08:38:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typo in attributename
 *
 * Revision 1.2  2005/10/24 09:59:24  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.1  2005/06/20 09:05:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of LogSender
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Sender that just logs its message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setLogLevel(String) logLevel}</td><td>level on which messages are logged</td><td>info</td></tr>
 * <tr><td>{@link #setLogCategory(String) logCategory}</td><td>category under which messages are logged</td><td>name of the sender</td></tr>
 * </table>
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 * @version $Id$
 * @deprecated Please replace with nl.nn.adapterframework.senders.LogSender
 */
public class LogSender extends nl.nn.adapterframework.senders.LogSender {

	public void configure() throws ConfigurationException {
		super.configure();
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
	}
}
